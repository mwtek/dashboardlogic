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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay;

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.KJP_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.logic.DashboardData.LENGTH_OF_STAY_STACKS;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraData.createLabelList;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineFunctionalities;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Generation of the 'cumulative.diags.lengthofstay' items for kjp and rsv.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraLengthOfStay extends DashboardDataItemLogic implements TimelineFunctionalities {

  /**
   * Creates a stacked bar chart showing the average length of stay for each group of cases. The
   * data is filtered to exclude outpatient cases and those lasting only one day.
   *
   * @param kidsRadarDataItemContext context defining which set of cases (e.g., KJP or RSV) should
   *     be analyzed
   * @param coreCaseDataByGroups map where the key is a group identifier (e.g., diagnosis type), and
   *     the value is a map of case data keyed by patient ID
   * @return a {@link StackedBarChartsItem} containing the bar chart data, including labels and
   *     values
   */
  public static StackedBarChartsItem createStackedBarCharts(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {
    log.debug("started KiraLengthOfStayDisorders.createStackedBarCharts");
    Instant startTimer = TimerTools.startTimer();

    String chartsLabel = "";
    switch (kidsRadarDataItemContext) {
      case KJP -> chartsLabel = KJP_DIAGNOSES_ALL;
      case RSV -> chartsLabel = RSV_DIAGNOSES_ALL;
    }
    // Setting chart, stack, and bar labels
    StackedBarChartsItem result = new StackedBarChartsItem();
    result.setCharts(new ArrayList<>(List.of(chartsLabel)));
    result.setStacks(List.of(LENGTH_OF_STAY_STACKS));
    result.setBars(List.of(createLabelList(coreCaseDataByGroups.keySet())));

    List<List<? extends Number>> resultValues = new ArrayList<>();

    coreCaseDataByGroups.forEach(
        (groupKey, coreCaseDataItem) -> {
          double meanByScope = 0;
          List<Long> lengthOfStaysByScope = new ArrayList<>();
          coreCaseDataItem
              .values()
              .forEach(
                  dataItem -> {
                    // Calculate whole days between start and end dates; if the end is null,
                    // the current datetime will be taken
                    // since it should be an active case then.
                    Long daysBetween =
                        DateTools.calcLengthOfStayBetweenDates(
                            dataItem.getAdmissionDate(), dataItem.getDischargeDate());
                    // Filter out one-day cases and outpatient cases based on the definition
                    if (daysBetween > 1) {
                      lengthOfStaysByScope.add(daysBetween);
                      // Print information about the length of stay
                      log.debug(
                          "{} days between discharge + admission {} [pid: {}] on scope: {}",
                          daysBetween,
                          dataItem.getFacilityEncounterId(),
                          dataItem.getPatientId(),
                          groupKey);
                    }
                  });
          // Calculation of the arithmetical mean
          if (!lengthOfStaysByScope.isEmpty()) {
            meanByScope =
                lengthOfStaysByScope.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElseThrow(() -> new IllegalArgumentException("The list must not be empty"));
          }
          resultValues.add(List.of(meanByScope));
        });

    result.setValues(List.of(resultValues));
    TimerTools.stopTimerAndLog(
        startTimer, "finished KiraLengthOfStayDisorders.createStackedBarCharts");

    // order ascending regarding the specification
    return result;
  }
}
