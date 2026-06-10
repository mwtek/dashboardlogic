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

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.YEAR_MONTH_FORMAT;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.generateDateList;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeKjpCluster;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.KiraInteger;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items containing age timelines.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraKjpTimelineAge extends DashboardDataItemLogic implements TimelineFunctionalities {

  /** Creates the kjp age timeline that separates age cluster by month. */
  public StackedBarChartsItem<KiraInteger> createKjpAgeTimeline(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {
    return createKjpAgeTimelineCore(coreCaseDataByGroups);
  }

  /**
   * Builds the pediatric age timeline for a set of encounters and case IDs.
   *
   * <p>The method normalizes all encounter periods to midnight-based days, assigns encounters to
   * age buckets (based on {@link CoreCaseData}), and counts the number of unique encounters per day
   * per bucket.
   *
   * @param coreCaseDataByGroups case data grouped by keys, must include "ALL" for full mapping
   */
  private StackedBarChartsItem<KiraInteger> createKjpAgeTimelineCore(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // Build the month bars (e.g., ["2020-01", "2020-02", ...]) using the same helper as elsewhere
    List<String> validPeriods =
        generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_MONTH_FORMAT, true);

    // Prepare result container
    StackedBarChartsItem<KiraInteger> result = new StackedBarChartsItem<>();
    result.setCharts(List.of(KJP_PATIENT));
    result.setBars(List.of(validPeriods));
    List<String> stackLabels = KiraAgeKjpCluster.BARS;
    result.setStacks(List.of(stackLabels));

    Map<String, CoreCaseData> allCases = flattenKjpCoreCases(coreCaseDataByGroups);

    // Prepare an index: period -> (ageLabel -> distinct facilityEncounterIds)
    Map<String, Map<String, Set<String>>> periodAgeToIds = new LinkedHashMap<>();
    for (String period : validPeriods) {
      Map<String, Set<String>> ageMap = new LinkedHashMap<>();
      for (String stack : stackLabels) {
        ageMap.put(stack, new HashSet<>());
      }
      periodAgeToIds.put(period, ageMap);
    }

    // Fill counts: for each case in scope, use its admission date to place it into a period
    // and its age bucket to place it into a stack. Count each case once.
    for (CoreCaseData ccd : allCases.values()) {
      String encId = ccd.getFacilityEncounterId();
      // Determine the period (bar) this case belongs to (based on your KidsRadar semantics)
      for (String period : validPeriods) {
        if (isDateInPeriod(KJP, ccd.getAdmissionDate(), period)) {
          String ageLabel = resolveAgeLabel(ccd, KJP);
          // If resolveAgeLabel can return unknown labels, guard with containsKey:
          if (periodAgeToIds.get(period).containsKey(ageLabel)) {
            periodAgeToIds.get(period).get(ageLabel).add(encId);
          }
          break;
        }
      }
    }

    // Build values: one chart -> list over bars; each bar -> list of stacks
    List<List<KiraInteger>> valuesForChart = new ArrayList<>(validPeriods.size());
    for (String period : validPeriods) {
      Map<String, Set<String>> perAge = periodAgeToIds.get(period);
      List<KiraInteger> perBar = new ArrayList<>(stackLabels.size());
      for (String stack : stackLabels) {
        int count = perAge.getOrDefault(stack, Collections.emptySet()).size();
        perBar.add(new KiraInteger(count));
      }
      valuesForChart.add(perBar);
    }

    result.setValues(List.of(valuesForChart));
    return result;
  }

  public Map<String, List<String>> getDebugData() {
    Map<String, List<String>> output = new LinkedHashMap<>();
    return output;
  }
}
