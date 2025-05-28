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

import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.YEAR_MONTH_FORMAT;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraData.createLabelList;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.generateDateList;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Creation of the "timeline.diags.occurrence" items for kjp and rsv.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraTimelineDisorders extends DashboardDataItemLogic
    implements TimelineFunctionalities {
  // Debug output
  static Map<String, Map<String, Set<String>>> patientIdsByPeriod = new LinkedHashMap<>();

  /**
   * Creates a StackedBarChartsItem object containing stacked bar chart data for the KidsRadar
   * timeline.
   *
   * <p>This method processes case data grouped by categories and months, and returns a stacked bar
   * chart item containing the counts of unique patients per month. The resulting chart is used in
   * the KidsRadar timeline to visualize patient encounters across different case groups.
   *
   * @param kidsRadarDataItemContext the context containing metadata and settings for the radar
   *     chart like {@link KidsRadarDataItemContext#KJP} or {@link KidsRadarDataItemContext#RSV}.
   * @param coreCaseDataByGroups a map where the key represents a group/category (e.g., disorders),
   *     and the value is another map, mapping case IDs to CoreCaseData objects.
   * @return StackedBarChartsItem containing chart data, bars (months), stacks (groups), and values
   *     (counts).
   */
  public StackedBarChartsItem createStackedBarCharts(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    log.debug("started KiraTimelineDisorders.createStackedBarCharts");
    Instant startTimer = TimerTools.startTimer();

    StackedBarChartsItem result = new StackedBarChartsItem();
    result.setCharts(
        new ArrayList<>(List.of(determineKiRaChartsAllLabelByContext(kidsRadarDataItemContext))));

    // Generate valid date list using the determined format.
    var validPeriods = generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_MONTH_FORMAT);
    result.setBars(List.of(validPeriods));

    result.setStacks(List.of(createLabelList(coreCaseDataByGroups.keySet())));
    List<List<? extends Number>> resultList = new ArrayList<>();

    for (String period : validPeriods) {
      List<Integer> resultByPeriod = new ArrayList<>();

      coreCaseDataByGroups.forEach(
          (group, caseDataItem) -> {
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

            // Creation of a map with patients that got more than 1 case by month as debug info.
            logPatientsWithMultipleCases(patientIdCaseIdsMap, period, group);

            // Adding patient once.
            resultByPeriod.add(patientIdCaseIdsMap.size());
          });

      resultList.add(resultByPeriod);
    }

    result.setValues(List.of(resultList));
    TimerTools.stopTimerAndLog(startTimer, "finished KiraTimelineDisorders.createStackedBarCharts");

    // Order ascending regarding the specification.
    return result;
  }

  public Map<String, Map<String, Set<String>>> getDebugData() {
    return patientIdsByPeriod;
  }

  // Method to log patients with multiple cases for readability
  private void logPatientsWithMultipleCases(
      Map<String, Set<String>> patientIdCaseIdsMap, String yearAndMonth, String group) {

    Map<String, Set<String>> patientIdsWithMultipleCases =
        patientIdCaseIdsMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (!patientIdsWithMultipleCases.isEmpty()) {
      patientIdsWithMultipleCases.forEach(
          (patientId, caseIds) -> {
            log.info(
                "Patient {} had multiple encounters in the month: {} [group: {}; encounters: {}]",
                patientId,
                yearAndMonth,
                group,
                caseIds);
          });
    }
  }
}
