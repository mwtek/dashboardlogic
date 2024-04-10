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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.ListNumberPair;
import de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem timeline.tests}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class TimelineTests extends DashboardDataItemLogics implements TimelineFunctionalities {

  public List<UkbObservation> listLabObservations;

  private Set<UkbObservation> diseasePositiveObservations;

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
  public ListNumberPair createTimelineTestsMap(
      DataItemContext dataItemContext) {
    log.debug("started createTimelineTestsMap");
    Instant startTimer = TimerTools.startTimer();
    Map<Long, Long> valueDateMap = getDateMapWithoutValues();
    List<Long> valueList;

    // Checking the loinc pcr codes in the observation to detect pcr findings.
    if (diseasePositiveObservations == null) {
      diseasePositiveObservations = ObservationFilter.getObservationsByContext(
          listLabObservations, getInputCodeSettings(), dataItemContext);
    }

    long endUnixTime = DateTools.getCurrentUnixTime();

    // Initialization of the map with the date entries to keep the order ascending
    List<Long> labEffectiveDates =
        diseasePositiveObservations.parallelStream()
            .filter(UkbObservation::hasEffectiveDateTimeType)
            .map(UkbObservation::getEffectiveDateTimeType)
            .map(x -> DateTools.dateToUnixTime(x.getValue())).toList();

    labEffectiveDates.parallelStream().forEach(effective -> {
      // Reset of the starting date
      long tempDate = CoronaDashboardConstants.qualifyingDate;
      // If value was found once in time window, can be cancelled
      Boolean labValueFound = false;

      while (tempDate <= endUnixTime && !labValueFound) {
        labValueFound = addLabTestToTimeline(effective, tempDate, valueDateMap);

        // check the next day
        tempDate += CoronaDashboardConstants.DAY_IN_SECONDS; // add one day
      }
    });

    // order them by key ascending (just needed if we want to parallelize it; the first tries
    // were not really promising tho because of too many write/read ops probably block each other)
    valueList = divideMapValuesToLists(valueDateMap);

    TimerTools.stopTimerAndLog(startTimer, "finished createTimelineTestsMap");
    return new ListNumberPair(getDatesOutputList(), valueList);
  }

  /**
   * Create a ListNumberPair containing all positive lab results for each day, since the qualifying
   * date
   *
   * @return ListNumberPair with all positive labor results up until today
   */
  public ListNumberPair createTimelineTestPositiveMap(DataItemContext dataItemContext) {
    log.debug("started createTimelineTestPositiveMap");
    Instant startTimer = TimerTools.startTimer();
    Map<Long, Long> valueDateMap = getDateMapWithoutValues();
    List<Long> valueList;
    long currentUnixTime = DateTools.getCurrentUnixTime();
    if (diseasePositiveObservations == null) {
      diseasePositiveObservations = ObservationFilter.getObservationsByContext(listLabObservations,
          getInputCodeSettings(), dataItemContext);
    }

    // Creation of a sublist with all positive covid observations
    // and reduce it to the effective dates of the funding's to make the data retrieval more
    // efficient
    // 1) Detection by Observation value.
    List<Long> labEffectiveDatesOfPositives = new ArrayList<>(
        ObservationFilter.getObservationsByValue(
                diseasePositiveObservations, POSITIVE).parallelStream()
            .filter(UkbObservation::hasEffectiveDateTimeType)
            // Caution with using getEffectiveDateTimeType since the default (if its null) will
            // be a date object of the current time.
            .map(UkbObservation::getEffectiveDateTimeType)
            .map(x -> DateTools.dateToUnixTime(x.getValue())).toList());

    // 2) Detection by Observation interpretation.
    labEffectiveDatesOfPositives.addAll(
        ObservationFilter.getObservationsByInterpretation(diseasePositiveObservations, POSITIVE)
            .parallelStream().filter(UkbObservation::hasEffectiveDateTimeType)
            // Caution with using getEffectiveDateTimeType since the default (if its null) will
            // be a date object of the current time.
            .map(UkbObservation::getEffectiveDateTimeType)
            .map(x -> DateTools.dateToUnixTime(x.getValue())).toList());

    try {
      labEffectiveDatesOfPositives.parallelStream().forEach(labEffective -> {
        Boolean obsFound = false;
        long checkingDateUnix = CoronaDashboardConstants.qualifyingDate;
        while (checkingDateUnix <= currentUnixTime && !obsFound) {
          obsFound = addLabTestToTimeline(labEffective, checkingDateUnix, valueDateMap);
          checkingDateUnix += CoronaDashboardConstants.DAY_IN_SECONDS; // add one day
        }
      });
    } catch (Exception ex) {
      log.error("Error while running createTimelineTestPositiveMap", ex);
    }
    valueList = divideMapValuesToLists(valueDateMap);
    TimerTools.stopTimerAndLog(startTimer, "finished createTimelineTestPositiveMap");
    return new ListNumberPair(getDatesOutputList(), valueList);
  }

  /**
   * Initializes a map that holds a list with all the midnight timestamps in the relevant time
   * period and initializes them with a sum value 0.
   */
  private static Map<Long, Long> getDateMapWithoutValues() {
    Map<Long, Long> valueDateMap = new ConcurrentHashMap<>();
    for (int i = 0; i < getDatesOutputList().size(); i++) {
      valueDateMap.put(getDatesOutputList().get(i), 0L);
    }
    return valueDateMap;
  }

  /**
   * Check whether a laboratory result belongs to the supplied date
   * {@literal [Interval: day <-> day+24h]} and subsequent incrementing if so.
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
    long increasedUnix = tempDateUnix + CoronaDashboardConstants.DAY_IN_SECONDS - 1;
    boolean obsFound = false;

    // if the labResult day is after the checked date and if the difference
    if (tempDateUnix <= labFundDate && increasedUnix >= labFundDate) {
      obsFound = true;
      resultMap.compute(tempDateUnix, (k, v) -> v != null ? v + 1 : null);
    }

    return obsFound;
  }
}
