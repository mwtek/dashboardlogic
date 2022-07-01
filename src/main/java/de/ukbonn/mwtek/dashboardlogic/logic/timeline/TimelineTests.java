/*
 *
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 *
 */
package de.ukbonn.mwtek.dashboardlogic.logic.timeline;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.ListNumberPair;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;

/**
 * This class is used for generating the data item {@link CoronaDataItem timeline.tests}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class TimelineTests extends TimelineFunctionalities {

  public List<UkbObservation> listLabObservations;

  public TimelineTests(List<UkbObservation> listLabObservation) {
    super();
    this.listLabObservations = listLabObservation;
  }

  /**
   * To create a {@link ListNumberPair} for each day since the qualifying date to determine the
   * frequency of laboratory findings per day
   *
   * @return ListNumberPair with all tests held from the qualifying date up to today
   */
  public ListNumberPair createTimelineTestsMap() {
    log.debug("started createTimelineTestsMap");
    Instant startTimer = TimerTools.startTimer();
    Map<Long, Long> valueDateMap = new ConcurrentHashMap<>();
    List<Long> dateList = new ArrayList<>();
    List<Long> valueList = new ArrayList<>();

    long endUnixTime = DateTools.getCurrentUnixTime();

    // Initialization of the map with the date entries to keep the order ascending
    long startDate = CoronaDashboardConstants.qualifyingDate;
    while (startDate <= endUnixTime) {
      // Initialize key (unix date) if not existent (important, since we have no values for some days )
      valueDateMap.putIfAbsent(startDate, 0L);
      startDate += CoronaDashboardConstants.dayInSeconds;
    }
    List<Long> labEffectiveDates =
        listLabObservations.parallelStream().map(UkbObservation::getEffectiveDateTimeType)
            .map(x -> DateTools.dateToUnixTime(x.getValue())).toList();

    labEffectiveDates.parallelStream().forEach(effective -> {
      // Reset of the starting date
      long tempDate = CoronaDashboardConstants.qualifyingDate;
      // If value was found once in time window, can be cancelled
      Boolean labValueFound = false;

      while (tempDate <= endUnixTime && !labValueFound) {
        labValueFound = addLabTestToTimeline(effective, tempDate, valueDateMap);

        // check the next day
        tempDate += CoronaDashboardConstants.dayInSeconds; // add one day
      }
    });

    // order them by key ascending (just needed if we want to parallelize it; the first tries
    // were
    // not really promising tho because of too many write/read ops probably block each other)
    divideMapValuesToLists(valueDateMap, dateList, valueList);

    TimerTools.stopTimerAndLog(startTimer, "finished createTimelineTestsMap");
    return new ListNumberPair(dateList, valueList);
  }

  /**
   * Create a ListNumberPair containing all positive lab results for each day, since the qualifying
   * date
   *
   * @return ListNumberPair with all positive labor results up until today
   */
  public ListNumberPair createTimelineTestPositiveMap() {
    log.debug("started createTimelineTestPositiveMap");
    Instant startTimer = TimerTools.startTimer();
    Map<Long, Long> tempMap = new ConcurrentHashMap<>();
    ArrayList<Long> dateList = new ArrayList<>();
    ArrayList<Long> valueList = new ArrayList<>();
    long currentUnixTime = DateTools.getCurrentUnixTime();

    // initialization of the map with the date entries to keep the order ascending
    long startDate = CoronaDashboardConstants.qualifyingDate;
    while (startDate <= currentUnixTime) {
      // initialize key (unix date) if not existent (important, since we have 0 values for some days)
      tempMap.putIfAbsent(startDate, 0L);
      startDate += CoronaDashboardConstants.dayInSeconds;
    }

    // Creation of a sublist with all positive covid observations
    // and reduce it to the effective dates of the funding's to make the data retrieval more efficient
    List<Long> labEffectiveDatesPositiveFundings = listLabObservations.parallelStream()
        .filter(x -> (CoronaFixedValues.COVID_LOINC_CODES.contains(
            x.getCode().getCoding().get(0).getCode())))
        .filter(x -> ((CodeableConcept) x.getValue()).getCoding().get(0).getCode()
            .equals(CoronaFixedValues.POSITIVE_CODE.getValue()))
        .map(UkbObservation::getEffectiveDateTimeType)
        .map(x -> DateTools.dateToUnixTime(x.getValue())).toList();

    try {
      labEffectiveDatesPositiveFundings.parallelStream().forEach(labEffective -> {
        Boolean obsFound = false;
        long checkingDateUnix = CoronaDashboardConstants.qualifyingDate;
        while (checkingDateUnix <= currentUnixTime && !obsFound) {

          obsFound = addLabTestToTimeline(labEffective, checkingDateUnix, tempMap);
          checkingDateUnix += CoronaDashboardConstants.dayInSeconds; // add one day
        }
      });
    } catch (Exception ex) {
      log.debug("issue while running createTimelineTestPositiveMap");
      ex.printStackTrace();
    }
    divideMapValuesToLists(tempMap, dateList, valueList);
    TimerTools.stopTimerAndLog(startTimer, "finished createTimelineTestPositiveMap");
    return new ListNumberPair(dateList, valueList);
  }

  /**
   * Check whether a laboratory result belongs to the supplied date {@literal [Interval: day <->
   * day+24h]} and subsequent incrementing if so.
   *
   * @param labFundDate  Date of the laboratory result
   * @param tempDateUnix Current day [unix time] which is checked and incremented if the reporting
   *                     date fits into the corresponding time window [day + 24 hours]
   * @param resultMap    Map that maps a frequency value to a date (unix time)
   * @return Status information on whether the time of the laboratory test was within the reference
   * range and the resultMap was incremented at this point.
   */
  private static Boolean addLabTestToTimeline(Long labFundDate, long tempDateUnix,
      Map<Long, Long> resultMap) {
    long increasedUnix = tempDateUnix + CoronaDashboardConstants.dayInSeconds - 1;
    boolean obsFound = false;

    // if the labResult day is after the checked date and if the difference
    if (tempDateUnix <= labFundDate && increasedUnix >= labFundDate) {
      obsFound = true;
      resultMap.compute(tempDateUnix, (k, v) -> v != null ? v + 1 : null);
    }

    return obsFound;
  }
}
