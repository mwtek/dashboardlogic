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

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.IN_GROUP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.OUT_GROUP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.YEAR_MONTH_FORMAT;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraData.createLabelList;
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.getRsvOnlyCoreCaseDataByGroups;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.generateDateList;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.KiraInteger;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsUniformItem;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates the grouped bar chart data item for the KidsRadar timeline, used to visualize
 * diagnostic group occurrences across monthly periods.
 */
@Slf4j
public class KiraTimelineDisorders extends DashboardDataItemLogic
    implements TimelineFunctionalities {

  // Debug map: holds patients and case IDs per period and group
  static Map<String, Map<String, Set<String>>> patientIdsByPeriod = new LinkedHashMap<>();

  /**
   * Generates a GroupedBarChartsItem with patient counts per diagnostic group and monthly period.
   *
   * <p>For each group (e.g., diagnostic category), this method creates one chart. Each chart
   * contains a bar per month (x-axis) and two stacked bars: one for patients who had this diagnosis
   * in that month ("in_group") and one for patients who had encounters in that month but did not
   * have this diagnosis ("out_group").
   *
   * @param kidsRadarDataItemContext The context (e.g., KJP or RSV) determining date reference
   * @param coreCaseDataByGroups Map: group → Map: case ID → CoreCaseData
   * @return a GroupedBarChartsItem formatted for rendering in the KidsRadar dashboard
   */
  public StackedBarChartsUniformItem<KiraInteger> createStackedBarChartsUniform(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    log.debug("Started KiraTimelineDisorders.createGroupedBarCharts");
    Instant startTimer = TimerTools.startTimer();

    StackedBarChartsUniformItem<KiraInteger> result = new StackedBarChartsUniformItem<>();
    result.setCharts(createLabelList(coreCaseDataByGroups.keySet()));

    List<String> validPeriods =
        generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_MONTH_FORMAT, true);
    result.setBars(validPeriods);
    result.setStacks(List.of(IN_GROUP, OUT_GROUP));

    // Precompute: all patients per period (across all diagnostic groups)
    Map<String, Set<String>> allPatientsByPeriod = new HashMap<>();
    for (String period : validPeriods) {
      Set<String> allPatients =
          coreCaseDataByGroups.values().stream()
              .flatMap(groupCases -> groupCases.values().stream())
              .filter(x -> isDateInPeriod(kidsRadarDataItemContext, x.getAdmissionDate(), period))
              .map(CoreCaseData::getPatientId)
              .collect(Collectors.toSet());
      allPatientsByPeriod.put(period, allPatients);
    }

    // Final result structure: charts x months x [in_group, out_group]
    List<List<List<KiraInteger>>> values = new ArrayList<>();

    coreCaseDataByGroups.forEach(
        (group, caseDataItem) -> {
          if (caseDataItem == null) return;

          List<List<KiraInteger>> valuesPerPeriod = new ArrayList<>();

          for (String period : validPeriods) {
            // Get all cases with this diagnosis group in the current period
            List<CoreCaseData> caseDataItemsByPeriod =
                caseDataItem.values().stream()
                    .filter(
                        x -> isDateInPeriod(kidsRadarDataItemContext, x.getAdmissionDate(), period))
                    .toList();

            Map<String, Set<String>> patientIdCaseIdsMap =
                caseDataItemsByPeriod.stream()
                    .collect(
                        Collectors.groupingBy(
                            CoreCaseData::getPatientId,
                            Collectors.mapping(
                                CoreCaseData::getFacilityEncounterId, Collectors.toSet())));

            patientIdsByPeriod.put(period + "_" + group, patientIdCaseIdsMap);
            logPatientsWithMultipleCases(patientIdCaseIdsMap, period, group);

            int inGroup = patientIdCaseIdsMap.size();
            Set<String> allPatientIdsInPeriod = allPatientsByPeriod.getOrDefault(period, Set.of());
            int outGroup =
                (int)
                    allPatientIdsInPeriod.stream()
                        .filter(id -> !patientIdCaseIdsMap.containsKey(id))
                        .count();

            valuesPerPeriod.add(List.of(new KiraInteger(inGroup), new KiraInteger(outGroup)));
          }

          values.add(valuesPerPeriod);
        });

    result.setValues(values);

    TimerTools.stopTimerAndLog(startTimer, "Finished KiraTimelineDisorders.createGroupedBarCharts");
    return result;
  }

  /**
   * Generates a StackedBarChartsItem with patient counts per diagnostic group and monthly period.
   * Currently used for RSV only.
   *
   * @param kidsRadarDataItemContext The context (e.g., RSV) determining date reference
   * @param coreCaseDataByGroups Map: group → Map: case ID → CoreCaseData
   * @return a StackedBarChartsItem formatted for rendering in the KidsRadar dashboard
   */
  public StackedBarChartsItem createStackBarCharts(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    Map<String, Map<String, CoreCaseData>> rsvOnly =
        getRsvOnlyCoreCaseDataByGroups(coreCaseDataByGroups);
    log.debug("Started KiraTimelineDisorders.createStackBarCharts");
    Instant startTimer = TimerTools.startTimer();

    StackedBarChartsItem result = new StackedBarChartsItem();
    result.setCharts(List.of(RSV_DIAGNOSES_ALL));
    result.setStacks(List.of(createLabelList(rsvOnly.keySet())));

    List<String> validPeriods = generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_MONTH_FORMAT);
    result.setBars(List.of(validPeriods));

    // Precompute: all patients per period (across all diagnostic groups)
    List<String> stackKeysOrdered = new ArrayList<>(rsvOnly.keySet());

    // 3) Build values: values[chartIndex][barIndex][stackIndex]
    List<List<List<? extends Number>>> values = new ArrayList<>(1);
    List<List<? extends Number>> valuesForSingleChart = new ArrayList<>(validPeriods.size());

    for (String period : validPeriods) {
      // one row per month (bar): counts per stack in the same order as stackKeysOrdered
      List<Integer> countsPerStackThisMonth = new ArrayList<>(stackKeysOrdered.size());

      for (String groupKey : stackKeysOrdered) {
        Map<String, CoreCaseData> groupCases = rsvOnly.get(groupKey);

        // 1) Filter cases by period
        List<CoreCaseData> caseDataItemsByPeriod =
            (groupCases == null ? Stream.<CoreCaseData>empty() : groupCases.values().stream())
                .filter(c -> isDateInPeriod(kidsRadarDataItemContext, c.getAdmissionDate(), period))
                .toList();

        // 2) Group by patientId and collect set of encounterIds
        Map<String, Set<String>> patientIdCaseIdsMap =
            caseDataItemsByPeriod.stream()
                .collect(
                    Collectors.groupingBy(
                        CoreCaseData::getPatientId,
                        Collectors.mapping(
                            CoreCaseData::getFacilityEncounterId, Collectors.toSet())));

        // 3) Put into debug map under key "<period>_<group>"
        patientIdsByPeriod.put(period + "_" + groupKey, patientIdCaseIdsMap);

        // 4) Use the size of the map as the unique patient count for this stack this month
        int uniquePatientsThisMonth = patientIdCaseIdsMap.size();

        countsPerStackThisMonth.add(uniquePatientsThisMonth);
      }

      valuesForSingleChart.add(countsPerStackThisMonth);
    }

    values.add(valuesForSingleChart);
    result.setValues(values);
    TimerTools.stopTimerAndLog(startTimer, "Finished KiraTimelineDisorders.createStackBarCharts");
    return result;
  }

  /** Returns internal debug data mapping periods + group to patient-case information. */
  public Map<String, Map<String, Set<String>>> getDebugData() {
    return patientIdsByPeriod;
  }

  /**
   * Logs information about patients who had multiple encounters in a given month and group.
   *
   * @param patientIdCaseIdsMap map of patient ID → set of encounter IDs
   * @param period the period (yyyy-MM)
   * @param group the diagnosis group name
   */
  private void logPatientsWithMultipleCases(
      Map<String, Set<String>> patientIdCaseIdsMap, String period, String group) {

    long count =
        patientIdCaseIdsMap.values().stream().filter(encounters -> encounters.size() > 1).count();

    if (count > 0) {
      log.debug(
          "In period {} for group '{}': {} patients had multiple encounters.",
          period,
          group,
          count);
    }
  }
}
