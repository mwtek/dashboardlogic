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

package de.ukbonn.mwtek.dashboardlogic.logic.cumulative;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import lombok.extern.slf4j.Slf4j;

/**
 * All logic concerning the cumulative logic {@link CoronaDataItem data items}
 * 
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CoronaCumulativeLogic {

  /**
   * Determination of the number of patients per gender and case status (e.g. inpatient)
   * 
   * @param listPatients a list with {@link UkbPatient} resources
   * @param listEncounters a list with {@link UkbEncounter} resources
   * @param gender the gender type (e.g. male) to be counted
   * @param caseClass the case class (e.g. inpatient) to be counted
   * @return number of genders per gender and case status
   */
  public static Number getGenderCountByCaseClass(List<UkbPatient> listPatients,
      List<UkbEncounter> listEncounters, String gender, String caseClass) {
    log.debug(
        "started getGenderCountByCaseClass for class: " + caseClass + " and gender: " + gender);
    Instant startTimer = TimerTools.startTimer();
    List<UkbEncounter> filteredEncounterList = new ArrayList<>();
    List<UkbEncounter> listEncounterByClass = new ArrayList<>();

    boolean isAmbulant = caseClass == CoronaFixedValues.CASECLASS_OUTPATIENT.getValue();
    boolean isStationary = caseClass == CoronaFixedValues.CASECLASS_INPATIENT.getValue();

    if (isAmbulant)
      listEncounterByClass = listEncounters.parallelStream()
          .filter(x -> CoronaResultFunctionality.isCaseClassOutpatient(x))
          .collect(Collectors.toList());

    else if (isStationary)
      listEncounterByClass = listEncounters.parallelStream()
          .filter(x -> CoronaResultFunctionality.isCaseClassInpatient(x))
          .collect(Collectors.toList());

    for (UkbEncounter encounter : listEncounterByClass) {
      if (isAmbulant) {
        filteredEncounterList.add(encounter);
      } else if (isStationary) {
        filteredEncounterList.add(encounter);
      }
    }

    TimerTools.stopTimerAndLog(startTimer, "finished getGenderCountByCaseClass");
    return getGenderCount(filteredEncounterList, listPatients, gender);
  }


  /**
   * Count number of gender of the covid patients
   * 
   * @param listEncounters a list with {@link UkbEncounter} resources
   * @param listPatients a list with {@link UkbPatient} resources
   * @param gender the gender type (e.g. male) to be counted
   * @return amount of the searched gender, of the patients who are covid positive
   */
  public static Number getGenderCount(List<UkbEncounter> listEncounters,
      List<UkbPatient> listPatients, String gender) {
    log.debug("started genderCounting for gender: " + gender);
    Instant startTimer = TimerTools.startTimer();

    // get all the pids from the positive marked encounters
    Set<String> positivePids = listEncounters.parallelStream()
        .filter(x -> x.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue()))
        .map(UkbEncounter::getPatientId)
        .collect(Collectors.toSet());

    // collect all the positive patient ids
    Set<String> resultSet = listPatients.parallelStream()
        .filter(x -> x.getGender()
            .toCode()
            .equalsIgnoreCase(gender))
        .filter(x -> positivePids.contains(x.getId()))
        .map(UkbPatient::getId)
        .collect(Collectors.toSet());

    TimerTools.stopTimerAndLog(startTimer, "finished genderCounting");
    return resultSet.size();
  }

  /**
   * Get the ages of all c19 positive patients by a given class
   * 
   * @param caseClass the class of an encounter (e.g. {@link CoronaFixedValues#INPATIENT})
   * @param listEncounters a list with {@link UkbEncounter} resources
   * @param listPatients a list with {@link UkbPatient} resources
   * @return returns list of age of all positive patients, who fulfill the caseStatus criteria
   */
  public static List<Integer> getAgeDistributionsByCaseClass(String caseClass,
      List<UkbEncounter> listEncounters, List<UkbPatient> listPatients) {
    log.debug("started getAgeDistributionsByCaseClass");
    Instant startTimer = TimerTools.startTimer();
    List<Integer> resultList = new ArrayList<>();
    Set<String> pidSet = new HashSet<>();
    Set<String> stationaryPidSet = new HashSet<>();
    Set<String> ambulantPidSet = new HashSet<>();

    // get the age of each patient at the admission date from the first c19-positive case
    List<UkbEncounter> listEncounterPositive = listEncounters.parallelStream()
        .filter(x -> x.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue()))
        .collect(Collectors.toList());

    HashMap<String, List<UkbEncounter>> mapEncounterAll = new HashMap<>();
    mapEncounterAll.put(CoronaFixedValues.ALL.getValue(), listEncounters);
    Map<String, Date> pidAgeMap = createPidAgeMap(VitalStatus.ALL, mapEncounterAll);

    for (UkbEncounter encounter : listEncounterPositive) {

      if (CoronaResultFunctionality.isCaseClassInpatient(encounter)) {
        stationaryPidSet.add(encounter.getPatientId());
      }

      else if (CoronaResultFunctionality.isCaseClassOutpatient(encounter)
          && !encounter.hasExtension(CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue())) {
        ambulantPidSet.add(encounter.getPatientId());
      }
    }

    // age.Inpatient
    if (caseClass.equalsIgnoreCase(CoronaFixedValues.INPATIENT.getValue())) {
      pidSet.addAll(stationaryPidSet);
    }

    // outpatient.age
    else if (caseClass.equalsIgnoreCase(CoronaFixedValues.OUTPATIENT.getValue())) {
      pidSet.addAll(ambulantPidSet);
    }

    // age
    else if (caseClass.equalsIgnoreCase(CoronaFixedValues.ALL.getValue())) {
      pidSet.addAll(stationaryPidSet);
      pidSet.addAll(ambulantPidSet);
    }

    // calculates age
    for (UkbPatient patient : listPatients) {
      if (pidSet.contains(patient.getId())) {
        if (patient.getBirthDate() != null && pidAgeMap.get(patient.getId()) != null)
          resultList.add(CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
              pidAgeMap.get(patient.getId())));
      }
    }
    TimerTools.stopTimerAndLog(startTimer, "finished getAgeDistributionsByCaseClass");

    // order ascending regarding the specification

    List<Integer> cohortedAgeList = new ArrayList<>();
    for (int age : resultList) {
      Long cohortedAge = CoronaResultFunctionality.checkAgeGroup(age);
      cohortedAgeList.add(cohortedAge.intValue());
    }
    Collections.sort(cohortedAgeList);
    return cohortedAgeList;
  }

  /**
   * Creates a map with the time in days that patients have spent as inpatients in hospital
   * 
   * @param listEncounters a list with {@link UkbEncounter} resources
   * @return A map with the pid as key and the value is a map containing the information on how many
   *         days a patient has spent in the hospital, and how often he was there, shown by the
   *         number of casesIds
   */
  public static HashMap<String, Map<Long, List<String>>> createMapDaysHospitalList(
      List<UkbEncounter> listEncounters) {
    log.debug("started createMapDaysHospitalList");
    Instant startTimer = TimerTools.startTimer();

    HashMap<String, Map<Long, List<String>>> mapResult = new HashMap<>();
    for (UkbEncounter e : listEncounters) {
      String pid = e.getPatientId();
      if (CoronaResultFunctionality.isCaseClassInpatient(e)
          && e.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())) {
        if (e.isPeriodStartExistent()) {
          LocalDate start = e.getPeriod()
              .getStart()
              .toInstant()
              .atZone(ZoneId.systemDefault())
              .toLocalDate();

          LocalDate end;
          if (e.getPeriod()
              .hasEnd()) {
            end = e.getPeriod()
                .getEnd()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
          } else {
            end = DateTools.getCurrentDateTime()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
          }

          Long days = ChronoUnit.DAYS.between(start, end);
          // check if there is already in entry for this patient
          // If that is the case, remove the old values and replace them with the new ones
          if (mapResult.containsKey(pid)) {
            Map<Long, List<String>> mapDaysCase = mapResult.get(pid);
            Long key = mapDaysCase.keySet()
                .stream()
                .findFirst()
                .get();
            Long keyNew = key + days;
            List<String> listCases = mapDaysCase.get(key);
            listCases.add(e.getId());
            mapDaysCase.remove(key);
            mapDaysCase.put(keyNew, listCases);
            mapResult.replace(pid, mapDaysCase);
          }

          else {
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
   * Calculate age of patients who fulfill the searched criteria
   * 
   * @param vitalStatus vitalstatus of a patient (e.g. {@link VitalStatus#ALIVE})
   * @param listEncounters a list with {@link UkbEncounter} resources
   * @param listPatients a list with {@link UkbPatient} resources
   * @param mapPositiveEncounterByClass Map with the c19-positive encounters separated by CaseClass
   * @return List with the ages of the c19-positive patients for the respective {@link VitalStatus}
   */
  public static List<Integer> getAgeCountByVitalStatus(VitalStatus vitalStatus,
      List<UkbEncounter> listEncounters, List<UkbPatient> listPatients,
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    log.debug("started getAgeCountByVitalStatus [vitalStatus: " + vitalStatus + "]");
    Instant startTime = TimerTools.startTimer();

    List<Integer> resultList = new ArrayList<>();

    Map<String, Date> pidAgeMap = createPidAgeMap(vitalStatus, mapPositiveEncounterByClass);
    // calculate age
    for (UkbPatient patient : listPatients) {
      if (pidAgeMap.containsKey(patient.getId())) {
        if (patient.getBirthDate() != null && pidAgeMap.get(patient.getId()) != null) {
          resultList.add(CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
              pidAgeMap.get(patient.getId())));
        }
      }
    }
    // order ascending regarding the specification
    List<Integer> cohortedAgeList = new ArrayList<>();
    for (int age : resultList) {
      Long cohortedAge = CoronaResultFunctionality.checkAgeGroup(age);
      cohortedAgeList.add(cohortedAge.intValue());
    }
    Collections.sort(cohortedAgeList);

    TimerTools.stopTimerAndLog(startTime, "finished getAgeCountByVitalStatus");
    return cohortedAgeList;
  }

  /**
   * Create a map that identifies the first admission date of a patient's first positive Covid case
   * and assigns it to the PID. This is the reference point for calculating the age.
   * 
   * @param vitalStatus vital status of a patient (e.g. {@link VitalStatus#ALIVE})
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @return Map that assigns the admission date of the patient's first c19 positive case to a pid
   */
  private static Map<String, Date> createPidAgeMap(VitalStatus vitalStatus,
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    Map<String, Date> pidMap = new HashMap<>();
    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      List<UkbEncounter> tempEncounterListByClass = entry.getValue();
      for (UkbEncounter encounter : tempEncounterListByClass) {
        String currentPid = encounter.getPatientId();
        // get the coding of the encounter if he is discharged
        List<Coding> dischargeCoding = encounter.getHospitalization()
            .getDischargeDisposition()
            .getCoding();

        switch (vitalStatus) {
          case ALIVE:
            // check dischargeCoding whether it is empty or does not have "07" as code
            // add pid to set and map id criterias are fulfilled
            if (dischargeCoding.isEmpty() || !dischargeCoding.get(0)
                .getCode()
                .equals(CoronaFixedValues.DEATH_CODE.getValue())) {
              pidMap.put(currentPid, checkIfEncounterHasEarlierCase(pidMap, encounter));
            }
            break;
          case DEAD:
            // same here just reversed
            if (!dischargeCoding.isEmpty()) {
              if (dischargeCoding.get(0)
                  .getCode()
                  .equals(CoronaFixedValues.DEATH_CODE.getValue())) {
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
   * Creates a list which contains encounter who have or had icu, ventilation or ecmo as highest
   * treatmentlevel
   * 
   * @param listEncounters a list with {@link UkbEncounter} resources
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @param treatmentLevel TreatmentLevel as separation criterion
   * @return List of all encounter that have the given {@link TreatmentLevel} as MaxTreatmentLevel
   */
  @SuppressWarnings("incomplete-switch")
  public static List<UkbEncounter> getCumulativeByIcuLevel(List<UkbEncounter> listEncounters,
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel) {
    List<UkbEncounter> resultList = new ArrayList<>();
    HashMap<String, List<UkbEncounter>> mapPidAndCase = new HashMap<>();
    log.debug("started getCumulativeByIcuLevel [" + treatmentLevel + "]");
    Instant startTime = TimerTools.startTimer();

    String icu = CoronaFixedValues.ICU.getValue();
    String ventilation = CoronaFixedValues.ICU_VENTILATION.getValue();
    String ecmo = CoronaFixedValues.ICU_ECMO.getValue();

    List<UkbEncounter> listIcuEncounter = mapIcu.get(icu);
    List<UkbEncounter> listVentilationEncounter = mapIcu.get(ventilation);
    List<UkbEncounter> listEcmoEncounter = mapIcu.get(ecmo);
    Set<String> setIcuPids = new HashSet<>();
    Set<String> setRemoveCasePid = new HashSet<>();
    List<UkbEncounter> listRemoveEncounter = new ArrayList<>();

    switch (treatmentLevel) {
      case ICU:
        // checks if the encounter has a higher treatmentlevel
        for (UkbEncounter encounter : listIcuEncounter) {
          if (!listEcmoEncounter.contains(encounter)
              && !listVentilationEncounter.contains(encounter)) {
            // Sort cases by pids; example: Map<Pid,Cases> Pid[123456,7891234]
            addPidToMap(mapPidAndCase, encounter);
            setIcuPids.add(encounter.getPatientId());
          }
        }
        break;

      case ICU_VENTILATION:
        // checks if the encounter has a higher treatmentlevel
        for (UkbEncounter encounter : listVentilationEncounter) {
          if (!listEcmoEncounter.contains(encounter)) {
            // Sort cases by pids; example: Map<Pid,Cases> Pid[123456,7891234]
            addPidToMap(mapPidAndCase, encounter);
          }
        }
        break;

      case ICU_ECMO:
        for (UkbEncounter encounter : listEcmoEncounter) {
          addPidToMap(mapPidAndCase, encounter);
        }
    }
    if (treatmentLevel == CoronaFixedValues.ICU) {
      List<UkbEncounter> listTempEncounter = new ArrayList<>();
      for (Map.Entry<String, List<UkbEncounter>> mapEncountersByPid : mapPidAndCase.entrySet()) {
        listTempEncounter.add(getFirstCovidPositiveCase(mapEncountersByPid));
      }
      resultList.addAll(listTempEncounter);
      // checks if encounter.getPatientid is in a encounter with a higher treatmentlevel
      for (UkbEncounter stationaryEncounter : listVentilationEncounter) {

        if (stationaryEncounter != null) {
          String stationaryPid = stationaryEncounter.getPatientId();
          if (setIcuPids.contains(stationaryPid)) {
            setRemoveCasePid.add(stationaryPid);
          }
        }
      }

      for (UkbEncounter stationaryEncounter : listEcmoEncounter) {
        String stationaryPid = stationaryEncounter.getPatientId();
        if (setIcuPids.contains(stationaryPid)) {
          setRemoveCasePid.add(stationaryPid);
        }
      }
      // removes the cases that have a higher treatmentlevel than ICU
      for (UkbEncounter resultEncounter : resultList) {
        if (setRemoveCasePid.contains(resultEncounter.getPatientId())) {
          listRemoveEncounter.add(resultEncounter);
        }
      }
      resultList.removeAll(listRemoveEncounter);
    }
    // If there are multiple cases with the same pid and same treatmentlevel, then only the youngest
    // should be counted for
    else if (treatmentLevel == CoronaFixedValues.ICU_VENTILATION) {
      List<UkbEncounter> listTempEncounter = new ArrayList<>();
      for (Map.Entry<String, List<UkbEncounter>> mapEncountersByPid : mapPidAndCase.entrySet()) {
        listTempEncounter.add(getFirstCovidPositiveCase(mapEncountersByPid));
      }
      resultList.addAll(listTempEncounter);

      // check if any encounter in ecmo has the same pid as one of the cases in Ventilation
      for (UkbEncounter stationaryEncounter : listEcmoEncounter) {
        String stationaryPid = stationaryEncounter.getPatientId();
        if (setIcuPids.contains(stationaryPid)) {
          setRemoveCasePid.add(stationaryPid);
        }

        // removes the cases that have a higher treatmentlevel
        for (UkbEncounter resultEncounter : resultList) {
          if (setRemoveCasePid.contains(resultEncounter.getPatientId())) {
            listRemoveEncounter.add(resultEncounter);
          }
        }
        resultList.removeAll(listRemoveEncounter);
      }
    } else if (treatmentLevel == CoronaFixedValues.ICU_ECMO) {
      // check if caseValue has two or more cases which belong to one pid
      List<UkbEncounter> listTempEncounter = new ArrayList<>();
      for (Map.Entry<String, List<UkbEncounter>> mapEncountersByPid : mapPidAndCase.entrySet()) {
        listTempEncounter.add(getFirstCovidPositiveCase(mapEncountersByPid));
      }
      resultList.addAll(listTempEncounter);
    }
    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByIcuLevel");
    return resultList;
  }

  /**
   * Add specific encounter to a map
   * 
   * @param mapPidAndCase map consisting of encounter sorted by their pids
   * @param encounter the encounter that has to be added
   */
  private static void addPidToMap(HashMap<String, List<UkbEncounter>> mapPidAndCase,
      UkbEncounter encounter) {
    if (mapPidAndCase.containsKey(encounter.getPatientId())) {
      mapPidAndCase.get(encounter.getPatientId())
          .add(encounter);
    } else {
      List<UkbEncounter> temp = new ArrayList<>();
      temp.add(encounter);
      mapPidAndCase.put(encounter.getPatientId(), temp);
    }
  }

  /**
   * If a patient got multiple positive covid cases, we need to calculate the age dependent on the
   * age compared to the admission date of his first case
   * 
   * @param pidMap map that links a pid to an admission date (not ensuring it is the oldest date)
   * @param encounter Encounter against which the previous admission date is checked
   * @return Admission date of the oldest of the two encounter examined
   */
  private static Date checkIfEncounterHasEarlierCase(Map<String, Date> pidMap,
      UkbEncounter encounter) {
    String pid = encounter.getPatientId();
    if (encounter.isPeriodStartExistent()) {
      Date admissionDateEncounter = encounter.getPeriod()
          .getStart();
      Date admissionDateExisting = null;

      // pid alrdy existing in map
      if (pidMap.containsKey(pid))
        admissionDateExisting = pidMap.get(pid);

      // if no admission date is alrdy connected to the pid -> get the one from the actual
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
    } else
      return pidMap.get(pid) != null ? pidMap.get(pid) : null;
  }

  /**
   * Returns the case from an encounter list with with the lowest admission date
   * ({@link UkbEncounter#getPeriod()})
   * 
   * @param mapEncounterByPid Map of all encounters per pid
   * @return The oldest positive C19 case of a patient
   */
  private static UkbEncounter getFirstCovidPositiveCase(
      Map.Entry<String, List<UkbEncounter>> mapEncounterByPid) {
    return mapEncounterByPid.getValue()
        .stream()
        .filter(x -> x.isPeriodStartExistent())
        .sorted((x1, x2) -> x1.getPeriod()
            .getStart()
            .compareTo(x2.getPeriod()
                .getStart()))
        .findFirst()
        .get();
  }

  /**
   * Returns a list containing the postcode of each patient from Germany.
   * 
   * @param listEncounters a list with {@link UkbEncounter} resources
   * @param listPatients a list with {@link UkbPatient} resources
   * @return List with all zipcodes of the given c19-positive patients
   */
  public static List<String> createZipCodeList(List<UkbEncounter> listEncounters,
      List<UkbPatient> listPatients) {
    log.debug("started createZipCodeList");
    Instant startTime = TimerTools.startTimer();
    Set<String> tempPidSet = new HashSet<>();
    List<String> listResult = new ArrayList<>();
    // get pids from all positive cases
    listEncounters.forEach(encounter -> {
      if (encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())) {
        tempPidSet.add(encounter.getPatientId());
      }
    });
    // save addresses from all positive cases
    for (UkbPatient patient : listPatients) {
      if (tempPidSet.contains(patient.getId())) {
        if (patient.getAddress()
            .get(0)
            .getPostalCode() != null) {
          if (patient.getAddress()
              .get(0)
              .getCountry()
              .equals(CoronaFixedValues.COUNTRY_CODE.getValue())) {
            listResult.add(patient.getAddress()
                .get(0)
                .getPostalCode());
          } else {
            listResult.add("null");
          }
        } else {
          listResult.add("null");
        }
      }
    }
    Collections.sort(listResult);
    TimerTools.stopTimerAndLog(startTime, "finished createZipCodeList");
    return listResult;
  }

  /**
   * Returns a list containing encounter which have or had ambulant or stationary as their highest
   * treatmentlevel
   * 
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @param treatmentLevel {@link TreatmentLevel TreatmentLevel.class} as separation criterion
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @return List with all encounters that have the given MaxTreatmentLevel
   */
  @SuppressWarnings("incomplete-switch")
  public static List<UkbEncounter> getCumulativeByClass(List<UkbEncounter> listEncounters,
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel,
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    Set<String> ambulantPidSet = new HashSet<>();
    Set<String> stationaryPidSet = new HashSet<>();
    List<UkbEncounter> listResult = new ArrayList<>();
    log.debug("started getCumulativeByClass");
    Instant startTime = TimerTools.startTimer();

    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      List<UkbEncounter> value = entry.getValue();
      switch (treatmentLevel) {
        case AMBULANT_ITEM: // check which case is ambulant and does not have the twelve Days Logic
          for (UkbEncounter encounter : value) {
            if (CoronaResultFunctionality.isCaseClassOutpatient(encounter)
                && !encounter.hasExtension(CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue())) {
              ambulantPidSet.add(encounter.getPatientId());
            }
          }
          break;
        case STATIONARY_ITEM: // check if stationary and is not any kind of ICU
          for (UkbEncounter e : value) {
            if (CoronaResultFunctionality.isCaseClassInpatient(e)) {
              // check if icu
              if (!mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue())
                  .contains(e)
                  && !mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue())
                      .contains(e)
                  && !mapIcu.get(CoronaFixedValues.ICU.getValue())
                      .contains(e)) {
                stationaryPidSet.add(e.getPatientId());
              }
            }
          }
          break;
      }
    }
    // print out only the most recent cases in case a patient has more than one case with same
    // treatmentlevel
    if (treatmentLevel.equals(CoronaFixedValues.AMBULANT_ITEM)) {
      listResult =
          youngestCaseLogic(treatmentLevel.getValue(), mapPositiveEncounterByClass, ambulantPidSet);
    }

    else if (treatmentLevel.equals(CoronaFixedValues.STATIONARY_ITEM)) {
      listResult = youngestCaseLogic(treatmentLevel.getValue(), mapPositiveEncounterByClass,
          stationaryPidSet);
    }

    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByClass");
    return listResult;
  }

  /**
   * Creates a list of encounters who are the youngest created encounter for the patient
   * 
   * @param treatmentLevel {@link TreatmentLevel TreatmentLevel.class} as separation criterion
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @param setInpatientPids Set with all the pids of inpatient patients
   * @return List of encounters that all have different pids
   */
  private static List<UkbEncounter> youngestCaseLogic(String treatmentLevel,
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass,
      Set<String> setInpatientPids) {
    Map<String, List<UkbEncounter>> pidEncounterMap = new HashMap<>();
    try {
      for (UkbEncounter encounter : mapPositiveEncounterByClass.get(treatmentLevel)) {

        Map<String, List<UkbEncounter>> tempPidEncounterMap = pidEncounterMap;

        if (setInpatientPids.contains(encounter.getPatientId())) {
          if (pidEncounterMap.containsKey(encounter.getPatientId())) {

            List<UkbEncounter> tempEncounterList = new ArrayList<>();
            tempEncounterList.addAll(tempPidEncounterMap.get(encounter.getPatientId()));

            UkbEncounter youngestCase = encounter;
            if (!tempEncounterList.isEmpty()) {
              // go through each encounter, if encounter was created after youngest case, than
              // replace youngest case with encounter
              for (UkbEncounter e : tempEncounterList) {
                // check whether the current youngest case, was created before the checked encounter
                if (encounter.isPeriodStartExistent() && youngestCase.isPeriodStartExistent()) {
                  if (youngestCase.getPeriod()
                      .getStart()
                      .before(e.getPeriod()
                          .getStart())) {
                    // if created earlier, remove from map if already contained, and declare current
                    // encounter
                    // as youngest case
                    if (tempPidEncounterMap.get(youngestCase.getPatientId())
                        .contains(youngestCase))
                      tempPidEncounterMap.get(youngestCase.getPatientId())
                          .remove(youngestCase);
                    youngestCase = e;
                  }
                } // if
              } // for
              tempPidEncounterMap.get(youngestCase.getPatientId())
                  .add(youngestCase);
            }

            else {
              // if patientIdCheckList is empty
              List<UkbEncounter> temp = new ArrayList<>();
              temp.add(encounter);
              tempPidEncounterMap.put(encounter.getPatientId(), temp);
            }
          } else {
            // in case the listEncounter is empty (probably first Object
            List<UkbEncounter> temp = new ArrayList<>();
            temp.add(encounter);
            tempPidEncounterMap.put(encounter.getPatientId(), temp);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    List<UkbEncounter> listEndResult = new ArrayList<>();
    // check if some pids still have multiple encounter assigned to them
    // Only save the youngest ones.
    for (Map.Entry<String, List<UkbEncounter>> entry : pidEncounterMap.entrySet()) {
      if (entry.getValue()
          .size() >= 2) {
        UkbEncounter newestCase = entry.getValue()
            .get(0);
        for (UkbEncounter encounter : entry.getValue()) {
          if (!newestCase.equals(encounter)) {
            if (encounter.isPeriodStartExistent() && newestCase.isPeriodStartExistent())
              if (newestCase.getPeriod()
                  .getStart()
                  .before(encounter.getPeriod()
                      .getStart())) {
                newestCase = encounter;
              }
          }
        }
        listEndResult.add(newestCase);
      } else {
        listEndResult.addAll(entry.getValue());
      }
    }

    return listEndResult;
  }

  /**
   * Creates a map containing the length of stay in hours for every patient/encounter who was in
   * intensive care
   * 
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @param listLocation A list with {@link UkbLocation} resources, to figure out which location is
   *          an icu location
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @return A Map that linking a patientid to a map that containing the length of stay in the
   *         hospital and all the caseids from which this total was calculated
   */
  public static HashMap<String, Map<Long, Set<String>>> createIcuLengthOfStayList(
      List<UkbEncounter> listEncounters, List<UkbLocation> listLocation,
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
        .filter(x -> x.hasPhysicalType() && x.getPhysicalType()
            .getCoding()
            .get(0)
            .getCode()
            .equals(CoronaFixedValues.WARD.getValue()))
        .filter(x -> x.hasType() && x.getType()
            .get(0)
            .getCoding()
            .get(0)
            .getCode()
            .equals(CoronaFixedValues.ICU.getValue()))
        .map(UkbLocation::getId)
        .collect(Collectors.toList());

    // iterate through every encounter and calculates amount of time spent in icu
    for (UkbEncounter encounter : icuEncounterSet) {
      Long hours = 0l;
      String pid = encounter.getPatientId();

      // get all the locations that are ICU Locations, by comparing the id with the icu location ids
      List<EncounterLocationComponent> listIcuEncounterLocation = new ArrayList<>();
      try {
        for (EncounterLocationComponent location : encounter.getLocation()) {
          if (location.getLocation() != null || !location.getLocation()
              .isEmpty()) {
            if (listIcuLocationIds
                .contains(CoronaResultFunctionality.splitReference(location.getLocation()
                    .getReference()))) {
              listIcuEncounterLocation.add(location);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      // go through each Location and calculate the time the spend in ICU
      for (EncounterLocationComponent location : listIcuEncounterLocation) {
        if (encounter.isPeriodStartExistent()) {
          LocalDateTime start = location.getPeriod()
              .getStart()
              .toInstant()
              .atZone(ZoneId.systemDefault())
              .toLocalDateTime();

          if (location.getPeriod()
              .hasStart()
              && location.getPeriod()
                  .hasEnd()) {
            LocalDateTime end = location.getPeriod()
                .getEnd()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            hours = CoronaResultFunctionality.calculateDaysInBetweenInHours(start, end);
            addIcuHours(mapResult, pid, hours, encounter);
          }

          else if (!location.getPeriod()
              .hasEnd()) {
            // calulate with current Date
            hours =
                CoronaResultFunctionality.calculateDaysInBetweenInHours(start, LocalDateTime.now());
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
   * @param mapResult A map that assigns a map with number of Icu Hours and the underlying caseids
   *          to a patient Id
   * @param patientId patient resource id
   * @param hoursToAdd Number of hours of Icu stay to be assigned to the respective patient ID or to
   *          be added to the previous total.
   * @param encounter {@link UkbEncounter} that is considered
   */
  private static void addIcuHours(HashMap<String, Map<Long, Set<String>>> mapResult,
      String patientId, Long hoursToAdd, UkbEncounter encounter) {
    // checks if there is already a time saved for the patient
    if (mapResult.containsKey(patientId)) {
      // getting the hours and the set of cases for this patient
      Map<Long, Set<String>> mapHourCase = mapResult.get(patientId);
      // getting the already saved hours and added together with the new hours
      Long key = mapHourCase.keySet()
          .stream()
          .findFirst()
          .get();
      Long keyNew = key + hoursToAdd;
      Set<String> listCases = mapHourCase.get(key);
      listCases.add(encounter.getId());
      // replace old time and set of cases, with the new ones
      mapHourCase.remove(key);
      mapHourCase.put(keyNew, listCases);
      mapResult.replace(patientId, mapHourCase);
    }
    // if patient was not added before, than create new key-value pair for it
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
   * 
   * @param vitalStatus Vital status of a patient (e.g. {@link VitalStatus#ALIVE})
   * @param mapIcuLengthList Map with the number of Icu stay hours per patientId
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @return Map with the number of Icu stay hours of all non-deceased patients per PatientId
   */
  public static HashMap<String, Map<Long, Set<String>>> createIcuLengthListByVitalstatus(
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
        if (encounter.getHospitalization()
            .hasDischargeDisposition()) {
          if (encounter.getHospitalization()
              .getDischargeDisposition()
              .getCoding()
              .get(0)
              .getCode()
              .equals(CoronaFixedValues.DEATH_CODE.getValue())) {
            Map<Long, Set<String>> mapTimeAndCaseNrs = mapIcuLengthList.get(pid);
            mapResult.put(pid, mapTimeAndCaseNrs);
          }
        }
      } else if (vitalStatus == VitalStatus.ALIVE) {
        /* check if patient is still under treatment */
        if (!encounter.getHospitalization()
            .hasDischargeDisposition()) {
          Map<Long, Set<String>> mapTimeAndCaseNrs = mapIcuLengthList.get(pid);
          mapResult.put(pid, mapTimeAndCaseNrs);
        }
        /* check if the patient was not discharged due to being deceased */
        else if (!encounter.getHospitalization()
            .getDischargeDisposition()
            .getCoding()
            .get(0)
            .getCode()
            .equals(CoronaFixedValues.DEATH_CODE.getValue())) {
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

  /**
   * Calculates the amount of time in days, a patient stayed in the hospital
   * 
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @param mapDays An already created map that assigns a length of stay to patient Ids
   * @param vitalStatus Criteria whether it should be searched for details of deceased/alive
   *          patients
   * @return A Map linking a patientid to a map that containing the length of stay in the hospital
   *         and all the caseids from which this total was calculated
   */
  public static HashMap<String, Map<Long, List<String>>> createLengthOfStayHospitalByVitalstatus(
      List<UkbEncounter> listEncounters, HashMap<String, Map<Long, List<String>>> mapDays,
      String vitalStatus) {
    log.debug("started createLengthOfStayHospitalByVitalstatus");
    Instant startTimer = TimerTools.startTimer();

    // just the c19 positive stays needs to be checked
    List<UkbEncounter> listEncountersPositive = listEncounters.parallelStream()
        .filter(x -> x.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue()))
        .collect(Collectors.toList());

    HashMap<String, Map<Long, List<String>>> resultMap = new HashMap<>();
    /* mapDays: Amount of days spent by all Patients, regardless of their condition */
    for (Map.Entry<String, Map<Long, List<String>>> entry : mapDays.entrySet()) {
      /* Map<Time, caseNrs> */
      Map<Long, List<String>> mapTimeAndCaseNrs = entry.getValue();

      for (Map.Entry<Long, List<String>> timeAndCaseNrs : mapTimeAndCaseNrs.entrySet()) {
        List<String> caseNrs = timeAndCaseNrs.getValue();
        List<UkbEncounter> listFilteredEncounter = new ArrayList<>();

        if (vitalStatus.equals(CoronaFixedValues.ALIVE.getValue())) {
          /* Get Encounter who got an discharged without being the reason "deceased" */
          listFilteredEncounter = listEncountersPositive.stream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(encounter -> encounter.getHospitalization()
                  .hasDischargeDisposition()
                  && !encounter.getHospitalization()
                      .getDischargeDisposition()
                      .getCoding()
                      .get(0)
                      .getCode()
                      .equals(CoronaFixedValues.DEATH_CODE.getValue()))
              .collect(Collectors.toList());

          /* Encounter without discharge */
          List<UkbEncounter> listNoDischargeEncounter = listEncountersPositive.stream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(encounter -> !encounter.getHospitalization()
                  .hasDischargeDisposition())
              .collect(Collectors.toList());

          listFilteredEncounter.addAll(listNoDischargeEncounter);
          /* if there is no encounter with any discharge than just add Encounter in */
          if (!listFilteredEncounter.isEmpty()) {
            resultMap.put(entry.getKey(), mapTimeAndCaseNrs);
          }
        } else if (vitalStatus.equals(CoronaFixedValues.DEAD.getValue())) {
          /* Check if encounter was discharged with the reason being "deceased" */
          listFilteredEncounter = listEncountersPositive.stream()
              .filter(encounter -> caseNrs.contains(encounter.getId()))
              .filter(encounter -> encounter.getHospitalization()
                  .hasDischargeDisposition()
                  && encounter.getHospitalization()
                      .getDischargeDisposition()
                      .getCoding()
                      .get(0)
                      .getCode()
                      .equals(CoronaFixedValues.DEATH_CODE.getValue()))
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

  public static List<Long> createMaxtreatmentAgeMap(List<UkbPatient> listPatients,
      Map<String, List<UkbEncounter>> mapPositiveEncounterByClass,
      Map<String, List<UkbEncounter>> mapIcu, String treatmentlevel) {
    log.debug("started createMaxtreatmentAgeMap");
    Instant startTimer = TimerTools.startTimer();

    List<Long> resultList = new ArrayList<>();
    Map<String, UkbEncounter> pidAdmissionMap = new HashMap<>();

    // find out the first admissiontime of the positive cases
    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        CoronaResultFunctionality.sortingFirstAdmissionDateToPid(encounter, pidAdmissionMap);
      }
    }
    // calculate patients age and sorting them to their maxtreatment
    for (Map.Entry<String, UkbEncounter> entry : pidAdmissionMap.entrySet()) {
      boolean isNormal =
          mapPositiveEncounterByClass.get(CoronaFixedValues.STATIONARY_ITEM.getValue())
              .contains(entry.getValue());
      boolean isIcu = mapIcu.get(CoronaFixedValues.ICU.getValue())
          .contains(entry.getValue());
      boolean isVent = mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue())
          .contains(entry.getValue());
      boolean isEcmo = mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue())
          .contains(entry.getValue());
      UkbPatient patient = listPatients.stream()
          .filter(p -> p.getId()
              .equals(entry.getKey()))
          .findAny()
          .get();

      if (entry.getValue()
          .isPeriodStartExistent()) {
        Date validAdmissionDate = entry.getValue()
            .getPeriod()
            .getStart();
        if (isNormal && !isIcu && !isVent && !isEcmo
            && treatmentlevel.equals(CoronaFixedValues.NORMALSTATION.getValue())) {
          int age =
              CoronaResultFunctionality.calculateAge(patient.getBirthDate(), validAdmissionDate);
          long cohortedAge = CoronaResultFunctionality.checkAgeGroup(age);
          resultList.add(Long.valueOf(cohortedAge));
        } else if (isIcu && !isVent && !isEcmo
            && treatmentlevel.equals(CoronaFixedValues.ICU.getValue())) {
          int age =
              CoronaResultFunctionality.calculateAge(patient.getBirthDate(), validAdmissionDate);
          long cohortedAge = CoronaResultFunctionality.checkAgeGroup(age);
          resultList.add(Long.valueOf(cohortedAge));
        } else if (isVent && !isEcmo
            && treatmentlevel.equals(CoronaFixedValues.ICU_VENTILATION.getValue())) {
          int age =
              CoronaResultFunctionality.calculateAge(patient.getBirthDate(), validAdmissionDate);
          long cohortedAge = CoronaResultFunctionality.checkAgeGroup(age);
          resultList.add(Long.valueOf(cohortedAge));
        } else if (isEcmo && treatmentlevel.equals(CoronaFixedValues.ICU_ECMO.getValue())) {
          int age =
              CoronaResultFunctionality.calculateAge(patient.getBirthDate(), validAdmissionDate);
          long cohortedAge = CoronaResultFunctionality.checkAgeGroup(age);
          resultList.add(Long.valueOf(cohortedAge));
        }
      }
    }
    Collections.sort(resultList);
    TimerTools.stopTimerAndLog(startTimer, "finished createMaxtreatmentAgeMap");
    return resultList;
  }
}