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
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Covid;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
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
public class TimelineTests extends DashboardDataItemLogic implements TimelineFunctionalities {

  private Set<MiiObservation> diseasePositiveObservations;

  /**
   * To create a {@link TimestampedListPair} for each day since the qualifying date to determine the
   * frequency of laboratory findings per day
   *
   * @return ListNumberPair with all tests held from the qualifying date up to today
   */
  public TimestampedListPair createTimelineTestsMap(
      DataItemContext dataItemContext,
      List<MiiObservation> observations,
      InputCodeSettings inputCodeSettings) {
    log.debug("started createTimelineTestsMap");
    Instant startTimer = TimerTools.startTimer();
    Map<Long, Long> valueDateMap = getDateMapWithoutValues(dataItemContext);
    List<Long> valueList;

    // Checking the loinc pcr codes in the observation to detect pcr findings.
    if (diseasePositiveObservations == null) {
      diseasePositiveObservations =
          ObservationFilter.getObservationsByContext(
              observations, inputCodeSettings, dataItemContext);
    }


    // Initialization of the map with the date entries to keep the order ascending
    List<Long> labEffectiveDates =
        diseasePositiveObservations.parallelStream()
            .filter(MiiObservation::hasEffectiveDateTimeType)
            .map(MiiObservation::getEffectiveDateTimeType)
            .map(x -> DateTools.dateToUnixTime(x.getValue()))
            .toList();

    // Each lab date falls into exactly one day of the timeline, so the day is computed directly
    // instead of walking day by day until a match is found.
    final long firstDayUnix = getKickOffDateInSeconds(dataItemContext);
    labEffectiveDates.parallelStream()
        .forEach(
            effective -> addLabTestToTimeline(effective, firstDayUnix, valueDateMap));

    // order them by key ascending (just needed if we want to parallelize it; the first tries
    // were not really promising tho because of too many write/read ops probably block each other)
    valueList = divideMapValuesToLists(valueDateMap);

    TimerTools.stopTimerAndLog(startTimer, "finished createTimelineTestsMap");
    return new TimestampedListPair(getDatesOutputList(dataItemContext), valueList);
  }

  /**
   * Create a ListNumberPair containing all positive lab results for each day, since the qualifying
   * date
   *
   * @return ListNumberPair with all positive labor results up until today
   */
  public TimestampedListPair createTimelineTestPositiveMap(
      DataItemContext dataItemContext,
      List<MiiObservation> observations,
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings) {
    log.debug("started createTimelineTestPositiveMap");
    Instant startTimer = TimerTools.startTimer();
    Map<Long, Long> valueDateMap = getDateMapWithoutValues(dataItemContext);
    List<Long> valueList;
    if (diseasePositiveObservations == null) {
      diseasePositiveObservations =
          ObservationFilter.getObservationsByContext(
              observations, inputCodeSettings, dataItemContext);
    }

    if (diseasePositiveObservations == null) {
      log.warn("No positive observations found for data item context: {}", dataItemContext);
      return null;
    }

    // Creation of a sublist with all disease-related positive observations
    // and reduce it to the effective dates of the funding's to make the data retrieval more
    // efficient
    // 1) Detection by Observation value.
    List<Long> labEffectiveDatesOfPositives =
        new ArrayList<>(
            ObservationFilter.getObservationsByValue(
                    diseasePositiveObservations, POSITIVE, qualitativeLabCodesSettings)
                .parallelStream()
                .filter(MiiObservation::hasEffectiveDateTimeType)
                // Caution with using getEffectiveDateTimeType since the default (if its null) will
                // be a date object of the current time.
                .map(MiiObservation::getEffectiveDateTimeType)
                .map(x -> DateTools.dateToUnixTime(x.getValue()))
                .toList());

    // 2) Detection by Observation interpretation.
    labEffectiveDatesOfPositives.addAll(
        ObservationFilter.getObservationsByInterpretation(diseasePositiveObservations, POSITIVE)
            .parallelStream()
            .filter(MiiObservation::hasEffectiveDateTimeType)
            // Caution with using getEffectiveDateTimeType since the default (if its null) will
            // be a date object of the current time.
            .map(MiiObservation::getEffectiveDateTimeType)
            .map(x -> DateTools.dateToUnixTime(x.getValue()))
            .toList());

    try {
      // Same direct day computation as in createTimelineTestsMap. Note that this item intentionally
      // starts at the covid qualifying date, which is kept as-is.
      final long firstDayUnix = Covid.QUALIFYING_DATE_SECONDS;
      labEffectiveDatesOfPositives.parallelStream()
          .forEach(labEffective -> addLabTestToTimeline(labEffective, firstDayUnix, valueDateMap));
    } catch (Exception ex) {
      log.error("Error while running createTimelineTestPositiveMap", ex);
    }
    valueList = divideMapValuesToLists(valueDateMap);
    TimerTools.stopTimerAndLog(startTimer, "finished createTimelineTestPositiveMap");
    return new TimestampedListPair(getDatesOutputList(dataItemContext), valueList);
  }

  /**
   * Initializes a map that holds a list with all the midnight timestamps in the relevant time
   * period and initializes them with sum value 0.
   */
  protected static Map<Long, Long> getDateMapWithoutValues(DataItemContext dataItemContext) {
    Map<Long, Long> valueDateMap = new ConcurrentHashMap<>();
    for (int i = 0; i < getDatesOutputList(dataItemContext).size(); i++) {
      valueDateMap.put(getDatesOutputList(dataItemContext).get(i), 0L);
    }
    return valueDateMap;
  }

  /**
   * Increments the timeline entry of the day the given laboratory result falls into [interval: day
   * &lt;-&gt; day+24h].
   *
   * <p>The day is computed arithmetically instead of searching it day by day. Results before the
   * first day of the timeline, or in a day that is not part of the timeline (e.g. a future date),
   * are not counted - which is exactly what the day-by-day search this method replaces did.
   *
   * @param labFundDate Date of the laboratory result [unix time]
   * @param firstDayUnix Unix time of the first day of the timeline [unix time]
   * @param resultMap Map that maps a frequency value to a date (unix time)
   */
  static void addLabTestToTimeline(
      Long labFundDate, long firstDayUnix, Map<Long, Long> resultMap) {
    if (labFundDate == null || labFundDate < firstDayUnix) {
      return;
    }
    long dayStart =
        firstDayUnix
            + ((labFundDate - firstDayUnix) / NumDashboardConstants.DAY_IN_SECONDS)
                * NumDashboardConstants.DAY_IN_SECONDS;
    // compute() leaves the map untouched if the day is not part of the timeline
    resultMap.compute(dayStart, (k, v) -> v != null ? v + 1 : null);
  }
}
