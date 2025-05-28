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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.DAY_IN_SECONDS;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Creation of the "timeline.diags.occurrence" items for kjp and rsv.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class AcribisTimelineDischargeDiags extends DashboardDataItemLogic
    implements TimelineFunctionalities {
  // Debug output
  static Map<String, Map<String, Set<String>>> patientIdsByPeriod = new LinkedHashMap<>();

  /**
   * Generates a timeline of daily cohort counts based on consent dates.
   *
   * <p>Each cohort contains consent timestamps, which are aggregated by day into counts. The method
   * returns a map of daily labels and per-day counts for each cohort.
   *
   * @param cohort1 the first cohort map of consent dates
   * @param cohort2 the second cohort map of consent dates
   * @param cohort3 the third cohort map of consent dates
   * @return a map containing daily timestamps and daily consent counts for each cohort
   */
  public Map<String, List<Long>> generateDailyCohortTimeline(
      PidTimestampCohortMap cohort1, PidTimestampCohortMap cohort2, PidTimestampCohortMap cohort3) {

    log.debug("started AcribisTimelineDischargeDiags.generateDailyCohortTimeline");
    Instant startTimer = TimerTools.startTimer();

    Map<String, List<Long>> resultMap;
    try {
      // Prepare and sort consent timestamps
      var cohort1ConsentTimestamps =
          cohort1.values().stream().map(DateTools::dateToUnixTime).sorted().toList();

      var cohort2ConsentTimestamps =
          cohort2.values().stream().map(DateTools::dateToUnixTime).sorted().toList();

      var cohort3ConsentTimestamps =
          cohort3.values().stream().map(DateTools::dateToUnixTime).sorted().toList();

      long currentUnixTime = DateTools.getCurrentUnixTime();
      long startDateUnix = getKickOffDateInSeconds(ACRIBIS);

      // Count daily consents using sliding window logic
      var cohort1DailyCounts =
          countPerDayLinear(cohort1ConsentTimestamps, startDateUnix, currentUnixTime);
      var cohort2DailyCounts =
          countPerDayLinear(cohort2ConsentTimestamps, startDateUnix, currentUnixTime);
      var cohort3DailyCounts =
          countPerDayLinear(cohort3ConsentTimestamps, startDateUnix, currentUnixTime);

      var cohort1Values = divideMapValuesToLists(cohort1DailyCounts);
      var cohort2Values = divideMapValuesToLists(cohort2DailyCounts);
      var cohort3Values = divideMapValuesToLists(cohort3DailyCounts);

      resultMap =
          Map.of(
              DATE, getDatesOutputList(ACRIBIS),
              ACR_COHORT_K_1, cohort1Values,
              ACR_COHORT_K_2, cohort2Values,
              ACR_COHORT_K_3, cohort3Values);

    } catch (Exception e) {
      log.error("Error generating consent timeline: ", e);
      resultMap = Map.of();
    }

    TimerTools.stopTimerAndLog(
        startTimer, "finished AcribisTimelineDischargeDiags.generateDailyCohortTimeline");
    return resultMap;
  }

  public Map<String, Map<String, Set<String>>> getDebugData() {
    return patientIdsByPeriod;
  }

  /**
   * Counts the number of consent events per day in a sorted list of timestamps.
   *
   * @param timestamps a sorted list of Unix timestamps (consent dates)
   * @param startUnix the start of the timeline (inclusive)
   * @param endUnix the end of the timeline (inclusive)
   * @return a map of day start timestamps to consent counts for that day
   */
  private LinkedHashMap<Long, Long> countPerDayLinear(
      List<Long> timestamps, long startUnix, long endUnix) {
    var result = new LinkedHashMap<Long, Long>();
    int index = 0;
    int size = timestamps.size();

    while (startUnix <= endUnix) {
      final long dayStart = startUnix;
      final long dayEnd = dayStart + DAY_IN_SECONDS;
      long count = 0;

      while (index < size && timestamps.get(index) < dayEnd) {
        if (timestamps.get(index) >= dayStart) {
          count++;
        }
        index++;
      }

      result.put(dayStart, count);
      startUnix = dayEnd;
    }

    return result;
  }
}
