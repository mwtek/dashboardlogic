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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Coding;

/**
 * This class is used for generating the data items {@link CoronaDataItem cumulative.age} including
 * the sub items (*.alive, *.dead)
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeAge {

  List<UkbEncounter> listEncounters;
  List<UkbPatient> listPatients;

  public CumulativeAge(List<UkbEncounter> listEncounters, List<UkbPatient> listPatients) {
    this.listEncounters = listEncounters;
    this.listPatients = listPatients;
  }

  /**
   * Get the ages of all c19 positive patients by a given class
   * <p>
   * Called by cumulative.age
   *
   * @param caseClass the class of an encounter (e.g. {@link CoronaFixedValues#CASESTATUS_INPATIENT})
   * @return returns list of age of all positive patients, who fulfill the caseStatus criteria
   */
  public List<Integer> getAgeDistributionsByCaseClass(String caseClass) {
    log.debug("started getAgeDistributionsByCaseClass");
    Instant startTimer = TimerTools.startTimer();
    List<Integer> resultList = new ArrayList<>();
    Set<String> pidSet = new HashSet<>();
    Set<String> stationaryPidSet = new HashSet<>();
    Set<String> ambulantPidSet = new HashSet<>();

    // get the age of each patient at the admission date from the first c19-positive case
    List<UkbEncounter> listEncounterPositive = listEncounters.parallelStream()
        .filter(x -> x.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())).toList();

    HashMap<String, List<UkbEncounter>> mapEncounterAll = new HashMap<>();
    mapEncounterAll.put(CoronaFixedValues.CASESTATUS_ALL.getValue(), listEncounters);
    Map<String, Date> pidAgeMap = createPidAgeMap(VitalStatus.ALL, mapEncounterAll);

    for (UkbEncounter encounter : listEncounterPositive) {

      if (CoronaResultFunctionality.isCaseClassInpatient(encounter)) {
        stationaryPidSet.add(encounter.getPatientId());
      } else if (CoronaResultFunctionality.isCaseClassOutpatient(
          encounter) && !encounter.hasExtension(
          CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue())) {
        ambulantPidSet.add(encounter.getPatientId());
      }
    }

    // age.Inpatient
    if (caseClass.equalsIgnoreCase(CoronaFixedValues.CASESTATUS_INPATIENT.getValue())) {
      pidSet.addAll(stationaryPidSet);
    }

    // outpatient.age
    else if (caseClass.equalsIgnoreCase(CoronaFixedValues.CASESTATUS_OUTPATIENT.getValue())) {
      pidSet.addAll(ambulantPidSet);
    }

    // age
    else if (caseClass.equalsIgnoreCase(CoronaFixedValues.CASESTATUS_ALL.getValue())) {
      pidSet.addAll(stationaryPidSet);
      pidSet.addAll(ambulantPidSet);
    }

    // calculates age
    for (UkbPatient patient : listPatients) {
      if (pidSet.contains(patient.getId())) {
        if (patient.hasBirthDate() && pidAgeMap.get(patient.getId()) != null) {
          resultList.add(CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
              pidAgeMap.get(patient.getId())));
        } else {
          log.warn("Could not find a birthday in the resource of patient " + patient.getId());
        }
      }
    }
    TimerTools.stopTimerAndLog(startTimer, "finished getAgeDistributionsByCaseClass");

    // order ascending regarding the specification
    return createCohortAgeList(resultList);
  }

  /**
   * Calculate age of patients who fulfill the searched criteria
   * <p>
   * called by <code>cumulative.age.alive</code> and <code>.dead</code>
   *
   * @param vitalStatus                 The vital-status of a patient (e.g. {@link
   *                                    VitalStatus#ALIVE})
   * @param mapPositiveEncounterByClass Map with the c19-positive encounters separated by CaseClass
   * @return List with the ages of the c19-positive patients for the respective {@link VitalStatus}
   */
  @Deprecated
  public List<Integer> getAgeCountByVitalStatus(VitalStatus vitalStatus,
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    log.debug("started getAgeCountByVitalStatus [vitalStatus: " + vitalStatus + "]");
    Instant startTime = TimerTools.startTimer();

    List<Integer> resultList = new ArrayList<>();

    Map<String, Date> pidAgeMap = createPidAgeMap(vitalStatus, mapPositiveEncounterByClass);
    // calculate age
    for (UkbPatient patient : listPatients) {
      if (pidAgeMap.containsKey(patient.getId())) {
        if (patient.hasBirthDate() && pidAgeMap.get(patient.getId()) != null) {
          resultList.add(CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
              pidAgeMap.get(patient.getId())));
        } else {
          log.warn(
              "Could not find a birthday in the resource or the resource itself in the pidAgeMap for the patient "
                  + patient.getId());
        }
      }
    }
    // order ascending regarding the specification
    TimerTools.stopTimerAndLog(startTime, "finished getAgeCountByVitalStatus");
    return createCohortAgeList(resultList);
  }


  /**
   * Create a map that identifies the first admission date of a patient's first positive Covid case
   * and assigns it to the PID. This is the reference point for calculating the age.
   *
   * @param vitalStatus                 The vital-status of a patient (e.g. {@link
   *                                    VitalStatus#ALIVE})
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @return Map that assigns the admission date of the patient's first c19 positive case to a pid
   */
  private Map<String, Date> createPidAgeMap(VitalStatus vitalStatus,
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    Map<String, Date> pidMap = new HashMap<>();
    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      List<UkbEncounter> tempEncounterListByClass = entry.getValue();
      for (UkbEncounter encounter : tempEncounterListByClass) {
        String currentPid = encounter.getPatientId();
        // get the coding of the encounter if he is discharged
        List<Coding> dischargeCoding =
            encounter.getHospitalization().getDischargeDisposition().getCoding();

        switch (vitalStatus) {
          // The split between live and dead cohorts is no longer needed and has been deprecated.
          case ALIVE:
            // Check dischargeCoding whether it is empty or does not have "07" as code
            // add pid to set and map id criteria are fulfilled
            if (!CoronaResultFunctionality.isPatientDeceased(encounter)) {
              pidMap.put(currentPid, checkIfEncounterHasEarlierCase(pidMap, encounter));
            }
            break;
          case DEAD:
            // Same here just reversed
            if (!dischargeCoding.isEmpty()) {
              if (CoronaResultFunctionality.isPatientDeceased(encounter)) {
                pidMap.put(currentPid, checkIfEncounterHasEarlierCase(pidMap, encounter));
              }
            }
            break;
          case ALL:
            pidMap.put(currentPid, checkIfEncounterHasEarlierCase(pidMap, encounter));
            break;
        }
      }
    }
    return pidMap;
  }

  /**
   * If a patient got multiple positive covid cases, we need to calculate the age dependent on the
   * age compared to the admission date of his first case
   *
   * @param pidMap    Map that links a pid to an admission date (not ensuring it is the oldest
   *                  date)
   * @param encounter Encounter against which the previous admission date is checked
   * @return Admission date of the oldest of the two encounter examined
   */
  private Date checkIfEncounterHasEarlierCase(Map<String, Date> pidMap, UkbEncounter encounter) {
    String pid = encounter.getPatientId();
    if (encounter.isPeriodStartExistent()) {
      Date admissionDateEncounter = encounter.getPeriod().getStart();
      Date admissionDateExisting = null;

      // pid already existing in map
      if (pidMap.containsKey(pid)) {
        admissionDateExisting = pidMap.get(pid);
      }

      // if no admission date is already connected to the pid -> get the one from the actual
      // encounter,
      // otherwise compare the dates and get the earlier one
      if (admissionDateExisting == null) {
        return admissionDateEncounter;
      } else {
        if (admissionDateEncounter.before(admissionDateExisting)) {
          return admissionDateEncounter;
        } else {
          return admissionDateExisting;
        }
      }
    } else {
      return pidMap.get(pid) != null ? pidMap.get(pid) : null;
    }
  }

  private List<Integer> createCohortAgeList(List<Integer> resultList) {
    List<Integer> cohortAgeList = new ArrayList<>();
    for (int age : resultList) {
      int cohortAge = CoronaResultFunctionality.checkAgeGroup(age);
      cohortAgeList.add(cohortAge);
    }
    Collections.sort(cohortAgeList);
    return cohortAgeList;
  }
}

