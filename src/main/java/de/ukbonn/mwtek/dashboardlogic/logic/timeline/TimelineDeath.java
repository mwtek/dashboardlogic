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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.DAY_IN_SECONDS;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem timeline.deaths}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class TimelineDeath extends DashboardDataItemLogic implements TimelineFunctionalities {

  /**
   * Creates a ListNumberPair that contains the number of deceased patients for each day since
   * Qualifying date
   *
   * @return ListNumberPair Containing dates and number of deceased people
   */
  public TimestampedListPair createTimelineDeathMap(
      List<MiiEncounter> facilityContacts, DataItemContext dataItemContext) {
    log.debug("started createTimelineDeathMap");
    Instant startTimer = TimerTools.startTimer();
    LinkedHashMap<Long, Long> dateResultMap = new LinkedHashMap<>();
    List<Long> valueList;
    TimestampedListPair resultPair = new TimestampedListPair();

    // Retrieval of the current date
    long currentUnixTime = DateTools.getCurrentUnixTime();
    // Retrieval of the start date
    long tempDateUnix = getKickOffDateInSeconds(dataItemContext);

    try {
      // subset with positive and completed encounters needed with discharge disposition: dead
      // (07 on pos 1 and 2 in the Encounter.dischargeDisposition)
      List<MiiEncounter> listPositiveDeceasedCases =
          facilityContacts.parallelStream()
              .filter(x -> x.hasExtension(POSITIVE_RESULT.getValue()))
              // just finished non-outpatient cases can hold a discharge disposition
              .filter(x -> x.getPeriod().hasEnd() && !x.isCaseClassOutpatient())
              .filter(MiiEncounter::isPatientDeceased)
              .toList();
      // Bucket the deceased cases by the calendar day of their discharge date. The previous
      // implementation compared every single case against every single day since the qualifying
      // date, which is quadratic over the days and the case list.
      Map<LocalDate, Long> deathsByDay = new HashMap<>();
      for (MiiEncounter encounter : listPositiveDeceasedCases) {
        LocalDate deathDay =
            encounter.getPeriod().getEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        deathsByDay.merge(deathDay, 1L, Long::sum);
      }

      // Loop through each day
      while (tempDateUnix <= currentUnixTime) {
        LocalDate checkedDay =
            DateTools.unixTimeSecondsToDate(tempDateUnix)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        // A case counts on the day its discharge date falls on (see the removed
        // checkDaysDifference: dayDifference < 1 plus the same day/month/year check is exactly
        // "same local date")
        dateResultMap.put(tempDateUnix, deathsByDay.getOrDefault(checkedDay, 0L));
        tempDateUnix += DAY_IN_SECONDS; // add one day
      }
      valueList = divideMapValuesToLists(dateResultMap);
      resultPair = new TimestampedListPair(getDatesOutputList(dataItemContext), valueList);
    } catch (Exception e) {
      log.debug("Error is calculating the timeline death: {}", e.getMessage());
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createTimeLineDeathMap");
    return resultPair;
  }
}
