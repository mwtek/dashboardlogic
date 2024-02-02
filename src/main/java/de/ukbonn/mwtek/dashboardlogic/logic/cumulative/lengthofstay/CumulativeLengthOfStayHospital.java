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

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items
 * {@link CoronaDataItem cumulative.lengthofstay.hospital} and the sub items.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeLengthOfStayHospital {

  List<UkbEncounter> listEncounters;

  public CumulativeLengthOfStayHospital(List<UkbEncounter> listEncounter) {
    this.listEncounters = listEncounter;
  }

  /**
   * Creates a map with the time in days that patients have spent as inpatients in hospital
   * <p>
   * Used for cumulative.lengthofstay.hospital
   *
   * @return A map with the pid as key and the value is a map containing the information on how many
   * days a patient has spent in the hospital, and how often he was there, shown by the number of
   * casesIds
   */
  public Map<String, Map<Long, List<String>>> createMapDaysHospitalList() {
    log.debug("started createMapDaysHospitalList");
    Instant startTimer = TimerTools.startTimer();

    HashMap<String, Map<Long, List<String>>> mapResult = new HashMap<>();
    for (UkbEncounter e : listEncounters) {
      String pid = e.getPatientId();
      if (CoronaResultFunctionality.isCaseClassInpatient(e) && e.hasExtension(
          CoronaFixedValues.POSITIVE_RESULT.getValue())) {
        if (e.isPeriodStartExistent()) {
          LocalDate start =
              e.getPeriod().getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

          LocalDate end;
          if (e.getPeriod().hasEnd()) {
            end = e.getPeriod().getEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
          } else {
            end = DateTools.getCurrentDateTime().toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate();
          }

          Long days = ChronoUnit.DAYS.between(start, end);
          // check if there is already in entry for this patient
          // If that is the case, remove the old values and replace them with the new ones
          if (mapResult.containsKey(pid)) {
            Map<Long, List<String>> mapDaysCase = mapResult.get(pid);
            Long key = mapDaysCase.keySet().stream().findFirst().get();
            Long keyNew = key + days;
            List<String> listCases = mapDaysCase.get(key);
            listCases.add(e.getId());
            mapDaysCase.remove(key);
            mapDaysCase.put(keyNew, listCases);
            mapResult.replace(pid, mapDaysCase);
          } else {
            List<String> listCaseId = new ArrayList<>();
            listCaseId.add(e.getId());
            Map<Long, List<String>> mapTemp = new HashMap<>();
            mapTemp.put(days, listCaseId);
            mapResult.put(pid, mapTemp);
          }
        }
      }
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createMapDaysHospitalList");
    return mapResult;
  }

  /**
   * Calculates the amount of time in days, a patient stayed in the hospital
   * <p>
   * Used for cumulative.lengthofstay.hospital.alive and cumulative.lengthofstay.hospital.dead
   *
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @param mapDays        An already created map that assigns a length of stay to patient Ids
   * @param vitalStatus    Criteria whether it should be searched for details of deceased/alive
   *                       patients
   * @return A Map linking a patient id to a map that containing the length of stay in the hospital
   * and all the case ids from which this total was calculated
   */
  public Map<String, Map<Long, List<String>>> createLengthOfStayHospitalByVitalstatus(
      List<UkbEncounter> listEncounters, Map<String, Map<Long, List<String>>> mapDays,
      String vitalStatus) {
    log.debug("started createLengthOfStayHospitalByVitalstatus");
    Instant startTimer = TimerTools.startTimer();

    // just the c19 positive stays needs to be checked
    List<UkbEncounter> listEncountersPositive = listEncounters.parallelStream()
        .filter(x -> x.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())).toList();

    HashMap<String, Map<Long, List<String>>> resultMap = new HashMap<>();
    /* mapDays: Amount of days spent by all Patients, regardless of their condition */
    for (Map.Entry<String, Map<Long, List<String>>> entry : mapDays.entrySet()) {
      /* Map<Time, caseNrs> */
      Map<Long, List<String>> mapTimeAndCaseNrs = entry.getValue();

      for (Map.Entry<Long, List<String>> timeAndCaseNrs : mapTimeAndCaseNrs.entrySet()) {
        List<String> caseNrs = timeAndCaseNrs.getValue();
        List<UkbEncounter> listFilteredEncounter;

        if (vitalStatus.equals(CoronaFixedValues.ALIVE.getValue())) {
          /* Get Encounter who got a discharged without being the reason "deceased" */
          listFilteredEncounter = listEncountersPositive.stream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(encounter -> !CoronaResultFunctionality.isPatientDeceased(encounter))
              .collect(Collectors.toList());

          /* Encounter without discharge */
          List<UkbEncounter> listNoDischargeEncounter = listEncountersPositive.stream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(encounter -> !encounter.getHospitalization().hasDischargeDisposition())
              .toList();

          listFilteredEncounter.addAll(listNoDischargeEncounter);
          /* if there is no encounter with any discharge than just add Encounter in */
          if (!listFilteredEncounter.isEmpty()) {
            resultMap.put(entry.getKey(), mapTimeAndCaseNrs);
          }
        } else if (vitalStatus.equals(CoronaFixedValues.DEAD.getValue())) {
          /* Check if encounter was discharged with the reason being "deceased" */
          listFilteredEncounter = listEncountersPositive.stream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(CoronaResultFunctionality::isPatientDeceased)
              .collect(Collectors.toList());

          if (!listFilteredEncounter.isEmpty()) {
            resultMap.put(entry.getKey(), mapTimeAndCaseNrs);
          }
        }
      }
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createLengthOfStayHospitalByVitalstatus");
    return resultMap;
  }
}
