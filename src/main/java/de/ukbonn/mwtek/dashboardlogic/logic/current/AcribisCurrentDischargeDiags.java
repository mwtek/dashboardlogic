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
package de.ukbonn.mwtek.dashboardlogic.logic.current;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Creation of the "timeline.diags.occurrence" items for kjp and rsv.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class AcribisCurrentDischargeDiags extends DashboardDataItemLogic {

  public static final String ACR_ALL_COHORTS = "acr_all_cohorts";
  public static final String ACR_NUMBER_PATIENTS_COHORT = "acr_number_patients_cohort";
  private final PidTimestampCohortMap cohort1Map;
  private final PidTimestampCohortMap cohort2Map;
  private final PidTimestampCohortMap cohort3Map;

  public AcribisCurrentDischargeDiags(
      PidTimestampCohortMap cohort1Map,
      PidTimestampCohortMap cohort2Map,
      PidTimestampCohortMap cohort3Map) {
    this.cohort1Map = cohort1Map;
    this.cohort2Map = cohort2Map;
    this.cohort3Map = cohort3Map;
  }

  /**
   * Creates a StackedBarChartsItem object containing stacked bar chart data for the Acribis current
   * discharge dignosis cohort.
   */
  public StackedBarChartsItem createStackedBarCharts() {

    log.debug("started AcribisCurrentDischargeDiags.createStackedBarCharts");
    Instant startTimer = TimerTools.startTimer();

    StackedBarChartsItem result = new StackedBarChartsItem();
    result.setCharts(new ArrayList<>(List.of(ACR_ALL_COHORTS)));
    result.setBars(List.of(List.of(ACR_COHORT_K_1, ACR_COHORT_K_2, ACR_COHORT_K_3)));
    result.setStacks(List.of(List.of(ACR_NUMBER_PATIENTS_COHORT)));
    List<List<? extends Number>> resultList = new ArrayList<>();
    resultList.add(List.of(cohort1Map.size()));
    resultList.add(List.of(cohort2Map.size()));
    resultList.add(List.of(cohort3Map.size()));
    result.setValues(List.of(resultList));
    TimerTools.stopTimerAndLog(
        startTimer, "finished AcribisCurrentDischargeDiags.createStackedBarCharts");

    // Order ascending regarding the specification.
    return result;
  }

  public Map<String, List<String>> getDebugData() {
    Map<String, List<String>> output = new LinkedHashMap<>();
    output.put(ACR_COHORT_K_1, new ArrayList<>(cohort1Map.keySet()));
    output.put(ACR_COHORT_K_2, new ArrayList<>(cohort2Map.keySet()));
    output.put(ACR_COHORT_K_3, new ArrayList<>(cohort3Map.keySet()));
    return output;
  }
}
