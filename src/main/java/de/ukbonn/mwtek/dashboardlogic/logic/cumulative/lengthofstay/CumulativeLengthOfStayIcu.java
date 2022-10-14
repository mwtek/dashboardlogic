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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay;

import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeOfFirstCodeableConceptEquals;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeOfFirstCodingEquals;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;

/**
 * This class is used for generating the data items
 * {@link CoronaDataItem cumulative.lengthofstay.icu} and the sub items.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeLengthOfStayIcu {

  public List<UkbEncounter> listEncounters;
  public List<UkbLocation> listLocation;

  public CumulativeLengthOfStayIcu(List<UkbEncounter> listEncounters,
      List<UkbLocation> listLocation) {
    this.listEncounters = listEncounters;
    this.listLocation = listLocation;
  }

  /**
   * Creates a map containing the length of stay in hours for every patient/encounter who was in
   * intensive care
   * <p>
   * Used for cumulative.lengthofstay.icu
   *
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @return A Map that links a patient id to a map that containing the length of stay in the
   * hospital and all the case ids from which this total was calculated
   */
  public HashMap<String, Map<Long, Set<String>>> createIcuLengthOfStayList(
      Map<String, List<UkbEncounter>> mapIcu) {

    log.debug("started createIcuLengthOfStayList");
    Instant startTimer = TimerTools.startTimer();
    HashMap<String, Map<Long, Set<String>>> mapResult = new HashMap<>();

    // Set containing all icu Encounter
    Set<UkbEncounter> icuEncounterSet = new HashSet<>();
    icuEncounterSet.addAll(mapIcu.get(CoronaFixedValues.ICU.getValue()));
    icuEncounterSet.addAll(mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue()));
    icuEncounterSet.addAll(mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue()));

    // Determination of the location IDs of all intensive care units. Only the wards are considered,
    // since in the location components within an Encounter resource, at best ward/room and bed are
    // listed with identical time periods and the stay should only be evaluated once. The highest of
    // these hierarchy levels should be sufficient.

    List<String> listIcuLocationIds = listLocation.stream()
        .filter(
            x -> x.hasPhysicalType() && isCodeOfFirstCodingEquals(x.getPhysicalType().getCoding(),
                CoronaFixedValues.WARD.getValue()))
        .filter(x -> x.hasType() && isCodeOfFirstCodeableConceptEquals(x.getType(),
            CoronaFixedValues.ICU.getValue()))
        .map(UkbLocation::getId).toList();

    // iterate through every encounter and calculates amount of time spent in icu
    for (UkbEncounter encounter : icuEncounterSet) {
      Long hours;
      String pid = encounter.getPatientId();

      // get all the locations that are ICU Locations, by comparing the id with the icu location ids
      List<Encounter.EncounterLocationComponent> listIcuEncounterLocation = new ArrayList<>();
      try {
        for (Encounter.EncounterLocationComponent location : encounter.getLocation()) {
          if (location.getLocation() != null || !location.getLocation().isEmpty()) {
            if (listIcuLocationIds.contains(
                CoronaResultFunctionality.extractIdFromReference(location.getLocation()))) {
              listIcuEncounterLocation.add(location);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      // go through each Location and calculate the time the spend in ICU
      for (Encounter.EncounterLocationComponent location : listIcuEncounterLocation) {
        if (location.hasPeriod() && location.getPeriod().hasStart()) {
          LocalDateTime start =
              location.getPeriod().getStart().toInstant().atZone(ZoneId.systemDefault())
                  .toLocalDateTime();

          if (location.getPeriod().hasStart() && location.getPeriod().hasEnd()) {
            LocalDateTime end =
                location.getPeriod().getEnd().toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            hours = CoronaResultFunctionality.calculateDaysInBetweenInHours(start, end);
            addIcuHours(mapResult, pid, hours, encounter);
          } else if (!location.getPeriod().hasEnd()) {
            // Calculate with current Date
            hours = CoronaResultFunctionality.calculateDaysInBetweenInHours(start,
                LocalDateTime.now());
            addIcuHours(mapResult, pid, hours, encounter);
          }
        }
      }
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createIcuLengthOfStayList");
    return mapResult;
  }

  /**
   * Add a number of Icu hours to a PatientId including a check if there is already an entry for
   * this patient. If this is the case, the old value is increased by the specified number of hours.
   * [ Used by: createIcuHoursList ]
   *
   * @param mapResult  A map that assigns a map with number of Icu Hours and the underlying caseids
   *                   to a patient Id
   * @param patientId  patient resource id
   * @param hoursToAdd Number of hours of Icu stay to be assigned to the respective patient ID or to
   *                   be added to the previous total.
   * @param encounter  {@link UkbEncounter} that is considered
   */
  private void addIcuHours(HashMap<String, Map<Long, Set<String>>> mapResult, String patientId,
      Long hoursToAdd, UkbEncounter encounter) {
    // checks if there is already a time saved for the patient
    if (mapResult.containsKey(patientId)) {
      // getting the hours and the set of cases for this patient
      Map<Long, Set<String>> mapHourCase = mapResult.get(patientId);
      // getting the already saved hours and added together with the new hours
      Long key = mapHourCase.keySet().stream().findFirst().get();
      Long keyNew = key + hoursToAdd;
      Set<String> listCases = mapHourCase.get(key);
      listCases.add(encounter.getId());
      // replace old time and set of cases, with the new ones
      mapHourCase.remove(key);
      mapHourCase.put(keyNew, listCases);
      mapResult.replace(patientId, mapHourCase);
    }
    // if patient was not added before, then create new key-value pair for it
    else {
      Set<String> listCaseId = new HashSet<>();
      listCaseId.add(encounter.getId());
      Map<Long, Set<String>> mapTemp = new HashMap<>();
      mapTemp.put(hoursToAdd, listCaseId);
      mapResult.put(patientId, mapTemp);
    }
  }

  /**
   * Create a map containing the length of stay in hours for icu cases, where the patients did not
   * deceased
   * <p>
   * used by cumulative.lengthofstay.icu.alive and dead
   *
   * @param vitalStatus      Vital status of a patient (e.g. {@link VitalStatus#ALIVE})
   * @param mapIcuLengthList Map with the number of Icu stay hours per patientId
   * @param mapIcu           Map that assigns a list of case numbers to an ICU treatment level
   *                         class
   * @return Map with the number of Icu stay hours of all non-deceased patients per PatientId
   */
  public HashMap<String, Map<Long, Set<String>>> createIcuLengthListByVitalstatus(
      VitalStatus vitalStatus, Map<String, Map<Long, Set<String>>> mapIcuLengthList,
      Map<String, List<UkbEncounter>> mapIcu) {
    log.debug("started createIcuLengthListByVitalstatus");
    Instant startTime = TimerTools.startTimer();
    HashMap<String, Map<Long, Set<String>>> mapResult = new HashMap<>();

    /* All icu Encounter in one set */
    Set<UkbEncounter> icuSet = new HashSet<>();
    icuSet.addAll(mapIcu.get(CoronaFixedValues.ICU.getValue()));
    icuSet.addAll(mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue()));
    icuSet.addAll(mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue()));

    for (UkbEncounter encounter : icuSet) {
      String pid = encounter.getPatientId();
      /* check if patient is deceased */
      if (vitalStatus == VitalStatus.DEAD) {
        if (CoronaResultFunctionality.isPatientDeceased(encounter)) {
          Map<Long, Set<String>> mapTimeAndCaseNrs = mapIcuLengthList.get(pid);
          mapResult.put(pid, mapTimeAndCaseNrs);
        }
      } else if (vitalStatus == VitalStatus.ALIVE) {
        /* check if patient is still under treatment */
        if (!encounter.getHospitalization().hasDischargeDisposition()) {
          Map<Long, Set<String>> mapTimeAndCaseNrs = mapIcuLengthList.get(pid);
          mapResult.put(pid, mapTimeAndCaseNrs);
        }
        /* check if the patient was not discharged due to being deceased */
        else if (!CoronaResultFunctionality.isPatientDeceased(encounter)) {
          Map<Long, Set<String>> mapTimeAndCaseNrs = mapIcuLengthList.get(pid);
          mapResult.put(pid, mapTimeAndCaseNrs);
        }
      } else if (vitalStatus == null) {
        Map<Long, Set<String>> mapTimeAndCaseNrs = mapIcuLengthList.get(pid);
        mapResult.put(pid, mapTimeAndCaseNrs);
      }
    }
    TimerTools.stopTimerAndLog(startTime, "finished createIcuLengthListByVitalstatus");
    return mapResult;
  }

}
