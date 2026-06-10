/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 * YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 * COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 * OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 * this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */
package de.ukbonn.mwtek.dashboardlogic.logic.timeline;

import static de.ukbonn.mwtek.dashboardlogic.logic.KiraData.createLabelList;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.YEAR_QUARTER_FORMAT;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.generateDateList;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.generateYearListToLastFullYear;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData.AdmissionStatus;
import de.ukbonn.mwtek.dashboardlogic.models.KiraInteger;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsUniformItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items containing admission timelines.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraKjpTimelineAdmission extends DashboardDataItemLogic
    implements TimelineFunctionalities {

  public static final String NCS_NEW_ADMISSION = "ncs_newadmission";
  public static final String NCS_RE_ADMISSION = "ncs_readmission";

  /** Creates the kjp admission timeline that separates admission status by quartal. */
  public StackedBarChartsItem<KiraInteger> createKjpTimelineAdmission(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {
    return createKjpTimelineAdmissionCore(coreCaseDataByGroups);
  }

  /** Creates the kjp admission timeline that separates admission status by quartal. */
  public StackedBarChartsUniformItem<KiraInteger> createKjpTimelineDiagsAdmission(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {
    return createKjpTimelineDiagsAdmissionCore(coreCaseDataByGroups);
  }

  /**
   * Builds the pediatric age (re/new)admission timeline for a set of encounters and case IDs.
   *
   * @param coreCaseDataByGroups case data grouped by keys, must include "ALL" for full mapping
   */
  private StackedBarChartsItem<KiraInteger> createKjpTimelineAdmissionCore(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // Build the month bars (e.g., ["2020-01", "2020-02", ...]) using the same helper as elsewhere
    List<String> validQuarters = generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_QUARTER_FORMAT);

    // Prepare result container
    StackedBarChartsItem<KiraInteger> result = new StackedBarChartsItem<>();
    result.setCharts(List.of(KJP_PATIENT));
    result.setBars(List.of(validQuarters));
    List<String> stackLabels = List.of(NCS_RE_ADMISSION, NCS_NEW_ADMISSION);
    result.setStacks(List.of(stackLabels));

    Map<String, CoreCaseData> allCases = flattenKjpCoreCases(coreCaseDataByGroups);

    // Prepare an index: period -> (quartal -> distinct encounter ids)
    Map<String, Map<String, Set<String>>> resultPairByQuarter = new LinkedHashMap<>();
    for (String period : validQuarters) {
      Map<String, Set<String>> resultMap = new LinkedHashMap<>();
      for (String stack : stackLabels) {
        resultMap.put(stack, new HashSet<>());
      }
      resultPairByQuarter.put(period, resultMap);
    }

    // Fill counts: for each case in scope, use its admission date to place it into a period
    // and its age bucket to place it into a stack. Count each case once.
    for (CoreCaseData ccd : allCases.values()) {
      String encId = ccd.getFacilityEncounterId();
      // Determine the period (bar) this case belongs to (based on your KidsRadar semantics)
      for (String quarter : validQuarters) {
        if (isDateInQuarter(ccd.getAdmissionDate(), quarter)) {
          String labelGroup;
          if (ccd.getStatus() == AdmissionStatus.NEW_ADMISSION) labelGroup = NCS_NEW_ADMISSION;
          else labelGroup = NCS_RE_ADMISSION;
          // If resolveAgeLabel can return unknown labels, guard with containsKey:
          if (resultPairByQuarter.get(quarter).containsKey(labelGroup)) {
            resultPairByQuarter.get(quarter).get(labelGroup).add(encId);
          }
          break;
        }
      }
    }

    // values for one chart: list over bars; each bar -> list over stacks
    List<List<KiraInteger>> valuesForChart = new ArrayList<>(validQuarters.size());
    for (String quarter : validQuarters) {
      Map<String, Set<String>> pairByQuarter = resultPairByQuarter.get(quarter);
      List<KiraInteger> perBar = new ArrayList<>(stackLabels.size());
      for (String stack : stackLabels) {
        int count = pairByQuarter.getOrDefault(stack, Collections.emptySet()).size();
        perBar.add(new KiraInteger(count));
      }
      valuesForChart.add(perBar); // e.g. [220, 520] (als KiraInteger)
    }

    // Wrap the single chart's values
    List<List<List<KiraInteger>>> values = List.of(valuesForChart);
    result.setValues(values);
    return result;
  }

  /** Creates the KJP admission timeline per diagnosis group, separated by calendar quarter. */
  private StackedBarChartsUniformItem<KiraInteger> createKjpTimelineDiagsAdmissionCore(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // Build quarter bars from QUALIFYING_DATE up to the last fully completed quarter
    List<String> validYears = generateYearListToLastFullYear(KidsRadar.QUALIFYING_DATE);

    StackedBarChartsUniformItem<KiraInteger> result = new StackedBarChartsUniformItem<>();
    result.setCharts(createLabelList(coreCaseDataByGroups.keySet()));
    // bars = years
    result.setBars(validYears);
    List<String> groupLabels = List.of(NCS_RE_ADMISSION, NCS_NEW_ADMISSION);
    result.setStacks(groupLabels);

    // Final payload: charts x quarters x groups
    List<List<List<KiraInteger>>> values = new ArrayList<>();

    // For each diagnosis group, count distinct encounters per quarter and admission status
    coreCaseDataByGroups.forEach(
        (diagGroup, casesMap) -> {
          if (casesMap == null || casesMap.isEmpty()) {
            // Produce an all-zero slice to keep shape consistent
            List<List<KiraInteger>> zeros = new ArrayList<>(validYears.size());
            for (int i = 0; i < validYears.size(); i++) {
              zeros.add(List.of(new KiraInteger(0), new KiraInteger(0)));
            }
            values.add(zeros);
            return;
          }

          // quarter -> status -> distinct encounter ids
          Map<String, Map<String, Set<String>>> quarterToStatusIds = new LinkedHashMap<>();
          for (String q : validYears) {
            Map<String, Set<String>> statusMap = new LinkedHashMap<>();
            for (String gl : groupLabels) statusMap.put(gl, new HashSet<>());
            quarterToStatusIds.put(q, statusMap);
          }

          // Place each case into its admission quarter and status bucket once
          for (CoreCaseData ccd : casesMap.values()) {
            if (ccd == null || ccd.getAdmissionDate() == null) continue;

            // Find the quarter that contains the admission date (exactly one)
            for (String year : validYears) {
              if (isDateInYear(ccd.getAdmissionDate(), year)) {
                String labelGroup =
                    (ccd.getStatus() == AdmissionStatus.NEW_ADMISSION)
                        ? NCS_NEW_ADMISSION
                        : NCS_RE_ADMISSION;

                quarterToStatusIds.get(year).get(labelGroup).add(ccd.getFacilityEncounterId());
                break; // count in exactly one quarter
              }
            }
          }

          // Build the values slice for this diagnosis group in the required order
          List<List<KiraInteger>> valuesPerQuarter = new ArrayList<>(validYears.size());
          for (String q : validYears) {
            Map<String, Set<String>> perStatus = quarterToStatusIds.get(q);
            // Order must match 'groupLabels'
            int reCount = perStatus.getOrDefault(NCS_RE_ADMISSION, Set.of()).size();
            int newCount = perStatus.getOrDefault(NCS_NEW_ADMISSION, Set.of()).size();
            valuesPerQuarter.add(List.of(new KiraInteger(reCount), new KiraInteger(newCount)));
          }
          values.add(valuesPerQuarter);
        });

    result.setValues(values);
    return result;
  }

  public Map<String, List<String>> getDebugData() {
    Map<String, List<String>> output = new LinkedHashMap<>();
    return output;
  }
}
