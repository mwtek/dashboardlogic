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

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants.DAY_IN_SECONDS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
public class TimelineDeath extends DashboardDataItemLogics implements TimelineFunctionalities {

  /**
   * Creates a ListNumberPair that contains the number of deceased patients for each day since
   * Qualifying date
   *
   * @return ListNumberPair Containing dates and number of deceased people
   */
  public TimestampedListPair createTimelineDeathMap() {
    log.debug("started createTimelineDeathMap");
    Instant startTimer = TimerTools.startTimer();
    LinkedHashMap<Long, Long> dateResultMap = new LinkedHashMap<>();
    List<Long> valueList;
    TimestampedListPair resultPair = new TimestampedListPair();

    //current date
    long currentUnixTime = DateTools.getCurrentUnixTime();
    //start date
    long tempDateUnix = CoronaDashboardConstants.QUALIFYING_DATE;

    try {
      // subset with positive and completed encounters needed with discharge disposition: dead
      // (07 on pos 1 and 2 in the Encounter.dischargeDisposition)
      List<UkbEncounter> listPositiveDeceasedCases = getFacilityContactEncounters().parallelStream()
          .filter(x -> x.hasExtension(POSITIVE_RESULT.getValue()))
          // just finished non-outpatient cases can hold a discharge disposition
          .filter(x -> x.getPeriod()
              .hasEnd() && !x.isCaseClassOutpatient())
          .filter(UkbEncounter::isPatientDeceased).toList();
      // Loop through each day
      while (tempDateUnix <= currentUnixTime) {

        long countDeceased = 0;
        // needs to be filled beforehand for daysDifferenceCheck to work
        dateResultMap.put(tempDateUnix, countDeceased);

        for (UkbEncounter encounter : listPositiveDeceasedCases) {
          checkDaysDifference(tempDateUnix, dateResultMap, encounter);
        }
        tempDateUnix += DAY_IN_SECONDS; // add one day
      }
      valueList = divideMapValuesToLists(dateResultMap);
      resultPair = new TimestampedListPair(getDatesOutputList(), valueList);
    } catch (Exception e) {
      log.debug("Error is calculating the timeline death: " + e.getMessage());
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createTimeLineDeathMap");
    return resultPair;
  }

  /**
   * Calculation of the difference between two dates and, if the difference is less than or equal to
   * 1, increments the value written to the map for the date checked [used by
   * createTimelineDeathMap]
   *
   * @param tempDateUnix  Current date [unixtime] which is checked and incremented if the reporting
   *                      date fits into the corresponding time window [date + 24 hours]
   * @param dateResultMap Map with the result per date [unixtime]
   * @param encounter     Current {@link UkbEncounter} that is going to be checked
   */
  private static void checkDaysDifference(long tempDateUnix,
      Map<Long, Long> dateResultMap, UkbEncounter encounter) {
    Date checkDate = DateTools.unixTimeSecondsToDate(tempDateUnix);
    Date caseDate = encounter.getPeriod().getEnd();
    LocalDate localCheckDate = checkDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    LocalDate localCaseDate = caseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    long dayDifference = Math.abs(ChronoUnit.DAYS.between(localCheckDate, localCaseDate));
    // if the dayDifference is smaller than one than the value of the day can be increased by
    // one
    if (dayDifference < 1) {
      // check if the dates have the same year and month
      if (localCheckDate.getDayOfMonth() == localCaseDate.getDayOfMonth()
          && localCheckDate.getYear() == localCaseDate.getYear()) {
        dateResultMap.replace(tempDateUnix, dateResultMap.get(tempDateUnix) + 1);
      }
    }
  }
}
