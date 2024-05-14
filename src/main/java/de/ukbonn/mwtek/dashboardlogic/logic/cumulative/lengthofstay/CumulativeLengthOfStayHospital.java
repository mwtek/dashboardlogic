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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus.ALIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus.DEAD;

import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items
 * {@link DiseaseDataItem cumulative.lengthofstay.hospital} and the subitems.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeLengthOfStayHospital extends DashboardDataItemLogics {

  /**
   * Creates a map with the time in days that patients have spent as inpatients in the hospital
   * <p>
   * Used for cumulative.lengthofstay.hospital
   *
   * @return A map with the pid as key and the value is a map containing the information on how many
   * days a patient has spent in the hospital, and how often he was there, shown by the number of
   * caseIds
   */
  public static Map<String, Map<Long, Set<String>>> createMapDaysHospitalList() {
    log.debug("started createMapDaysHospitalList");
    Instant startTimer = TimerTools.startTimer();
    // Filter encounters to include only inpatient cases with positive disease status
    Map<String, Map<Long, Set<String>>> mapResult = getFacilityContactEncounters().stream()
        .filter(UkbEncounter::isCaseClassInpatient)
        .filter(EncounterFilter::isDiseasePositive)
        .filter(UkbEncounter::isPeriodStartExistent)
        // Group encounters by patient ID
        .collect(Collectors.groupingBy(
            UkbEncounter::getPatientId,
            // Group encounters by the number of days
            Collectors.groupingBy(
                e -> {
                  // Calculate the number of days of the encounter
                  Instant startInstant = e.getPeriod().getStart().toInstant();
                  Instant endInstant =
                      e.getPeriod().hasEnd() ? e.getPeriod().getEnd().toInstant() : Instant.now();
                  LocalDate startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                  LocalDate endDate = endInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                  return ChronoUnit.DAYS.between(startDate, endDate);
                },
                Collectors.mapping(UkbEncounter::getId, Collectors.toSet())
            )
        ));
    // Stop the timer and log the finish message
    TimerTools.stopTimerAndLog(startTimer, "finished createMapDaysHospitalList");
    return mapResult;
  }

  /**
   * Calculates the amount of time in days that a patient stayed in the hospital.
   * <p>
   * Used for {@code cumulative.lengthofstay.hospital.alive} and
   * {@code cumulative.lengthofstay.hospital.dead}.
   *
   * @param mapDays     An already created map that assigns a length of stay to patient ids.
   * @param vitalStatus Criteria whether it should be searched for details of deceased/alive
   *                    patients.
   * @return A Map linking a patient id to a map containing the length of stay in the hospital and
   * all the case ids from which this total was calculated.
   */
  public static Map<String, Map<Long, Set<String>>> createLengthOfStayHospitalByVitalstatus(
      Map<String, Map<Long, Set<String>>> mapDays,
      VitalStatus vitalStatus) {
    log.debug("started createLengthOfStayHospitalByVitalstatus");
    Instant startTimer = TimerTools.startTimer();

    // Filter encounters based on disease-positive results
    List<UkbEncounter> filteredEncounters = getFacilityContactEncounters().parallelStream()
        .filter(x -> x.hasExtension(POSITIVE_RESULT.getValue())).toList();

    Map<String, Map<Long, Set<String>>> resultMap = new ConcurrentHashMap<>();
    mapDays.entrySet().parallelStream().forEach(entry -> {
      Map<Long, Set<String>> mapTimeAndCaseNrs = entry.getValue();

      mapTimeAndCaseNrs.forEach((key, caseNrs) -> {
        List<UkbEncounter> listFilteredEncounter = new ArrayList<>();

        if (vitalStatus == ALIVE) {
          listFilteredEncounter = filteredEncounters.parallelStream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(encounter -> !encounter.isPatientDeceased()).toList();
        } else if (vitalStatus == DEAD) {
          listFilteredEncounter = filteredEncounters.parallelStream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(UkbEncounter::isPatientDeceased).toList();
        } else if (vitalStatus == null) {
          listFilteredEncounter = filteredEncounters.parallelStream()
              .filter(encounter -> caseNrs.contains(encounter.getId())).toList();
        }

        if (!listFilteredEncounter.isEmpty()) {
          resultMap.computeIfAbsent(entry.getKey(), k -> new ConcurrentHashMap<>())
              .put(key, caseNrs);
        }
      });
    });

    TimerTools.stopTimerAndLog(startTimer, "finished createLengthOfOfStayHospitalByVitalstatus");
    return resultMap;
  }

}

