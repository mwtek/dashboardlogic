/*
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;


/**
 * This class is used for generating the data item
 * {@link CoronaDataItem cumulative.maxtreatmentlevel}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeMaxTreatmentLevel {

  public List<UkbEncounter> listEncounters;

  public CumulativeMaxTreatmentLevel(List<UkbEncounter> listEncounter) {
    listEncounters = listEncounter;
  }

  /**
   * Returns a list containing encounter which have or had ambulant or stationary as their highest
   * treatmentlevel
   *
   * @param mapIcu                      Map that assigns a list of case numbers to an ICU treatment
   *                                    level class
   * @param treatmentLevel              Treatmentlevel (e.g. {@link CoronaFixedValues#ICU_ECMO}) as
   *                                    separation criterion
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @return List with all encounters that have the given MaxTreatmentLevel
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCumulativeByClass(
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel,
      Map<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    Set<String> ambulantPidSet = new HashSet<>();
    Set<String> stationaryPidSet = new HashSet<>();
    List<UkbEncounter> listResult = new ArrayList<>();
    log.debug("started getCumulativeByClass");
    Instant startTime = TimerTools.startTimer();

    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      List<UkbEncounter> value = entry.getValue();
      switch (treatmentLevel) {
        case OUTPATIENT_ITEM -> { // check which case is ambulant and does not have the twelve Days Logic
          for (UkbEncounter encounter : value) {
            if (CoronaResultFunctionality.isCaseClassOutpatient(
                encounter) && !encounter.hasExtension(
                CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue())) {
              ambulantPidSet.add(encounter.getPatientId());
            }
          }
        }
        case INPATIENT_ITEM -> { // check if stationary and is not any kind of ICU
          for (UkbEncounter e : value) {
            if (CoronaResultFunctionality.isCaseClassInpatient(e)) {
              // check if icu
              if (!mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue()).contains(e) && !mapIcu.get(
                  CoronaFixedValues.ICU_VENTILATION.getValue()).contains(e) && !mapIcu.get(
                  CoronaFixedValues.ICU.getValue()).contains(e)) {
                stationaryPidSet.add(e.getPatientId());
              }
            }
          }
        }
      }
    }
    // print out only the most recent cases in case a patient has more than one case with same
    // treatmentlevel
    if (treatmentLevel.equals(CoronaFixedValues.OUTPATIENT_ITEM)) {
      listResult = youngestCaseLogic(treatmentLevel.getValue(), mapPositiveEncounterByClass,
          ambulantPidSet);
    } else if (treatmentLevel.equals(CoronaFixedValues.INPATIENT_ITEM)) {
      listResult = youngestCaseLogic(treatmentLevel.getValue(), mapPositiveEncounterByClass,
          stationaryPidSet);
    }

    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByClass");
    return listResult;
  }

  /**
   * Creates a list of encounters who are the youngest created encounter for the patient
   *
   * @param treatmentLevel              Treatmentlevel (e.g. {@link CoronaFixedValues#ICU_ECMO}) as
   *                                    separation criterion
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @param setInpatientPids            Set with all the pids of inpatient patients
   * @return List of encounters that all have different pids
   */
  private List<UkbEncounter> youngestCaseLogic(String treatmentLevel,
      Map<String, List<UkbEncounter>> mapPositiveEncounterByClass,
      Set<String> setInpatientPids) {
    Map<String, List<UkbEncounter>> pidEncounterMap = new HashMap<>();
    try {
      for (UkbEncounter encounter : mapPositiveEncounterByClass.get(treatmentLevel)) {

        if (setInpatientPids.contains(encounter.getPatientId())) {
          if (pidEncounterMap.containsKey(encounter.getPatientId())) {

            List<UkbEncounter> tempEncounterList =
                new ArrayList<>(pidEncounterMap.get(encounter.getPatientId()));

            UkbEncounter youngestCase = encounter;
            if (!tempEncounterList.isEmpty()) {
              // go through each encounter, if encounter was created after youngest case, than
              // replace the youngest case with encounter
              for (UkbEncounter enc : tempEncounterList) {
                // check whether the current youngest case, was created before the checked enc
                if (enc.isPeriodStartExistent() && youngestCase.isPeriodStartExistent()) {
                  if (youngestCase.getPeriod().getStart().before(enc.getPeriod().getStart())) {
                    // if created earlier, remove from map if already contained, and declare current
                    // enc
                    // as youngest case
                    pidEncounterMap.get(youngestCase.getPatientId()).remove(youngestCase);
                    youngestCase = enc;
                  }
                } // if
              } // for
              pidEncounterMap.get(youngestCase.getPatientId()).add(youngestCase);
            } else {
              // if patientIdCheckList is empty
              List<UkbEncounter> temp = new ArrayList<>();
              temp.add(encounter);
              pidEncounterMap.put(encounter.getPatientId(), temp);
            }
          } else {
            // in case the listEncounter is empty (probably first Object
            List<UkbEncounter> temp = new ArrayList<>();
            temp.add(encounter);
            pidEncounterMap.put(encounter.getPatientId(), temp);
          }
        }
      }
    } catch (Exception ex) {
      log.error("Error in the 'youngest case' determination logic: ", ex);
    }
    List<UkbEncounter> listEndResult = new ArrayList<>();
    // check if some pids still have multiple encounter assigned to them
    // Only save the youngest ones.
    for (Map.Entry<String, List<UkbEncounter>> entry : pidEncounterMap.entrySet()) {
      if (entry.getValue().size() >= 2) {
        UkbEncounter newestCase = entry.getValue().get(0);
        for (UkbEncounter encounter : entry.getValue()) {
          if (!newestCase.equals(encounter)) {
            if (encounter.isPeriodStartExistent() && newestCase.isPeriodStartExistent()) {
              if (newestCase.getPeriod().getStart().before(encounter.getPeriod().getStart())) {
                newestCase = encounter;
              }
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

  // Multiple required lists to project larger FHIR resource object lists to the needed attributes
  private Set<String> setVentilationPids;
  private Set<String> setEcmoPids;

  /**
   * Creates a list which contains encounter who have or had icu, ventilation or ecmo as highest
   * treatmentlevel. [needed in data item: cumulative.maxtreatmentlevel]
   *
   * @param mapIcu         Map that assigns a list of case numbers to an ICU treatment level class.
   * @param treatmentLevel The treatmentlevel (e.g. {@link CoronaFixedValues#ICU}) which is going to
   *                       be checked as separation criterion.
   * @return List of all encounter that have the given treatment level as maximum treatment level.
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCumulativeByIcuLevel(
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel) {
    List<UkbEncounter> resultList = new ArrayList<>();
    HashMap<String, List<UkbEncounter>> mapPidCases = new HashMap<>();
    log.debug("started getCumulativeByIcuLevel [" + treatmentLevel + "]");
    Instant startTime = TimerTools.startTimer();

    List<UkbEncounter> listIcuEncounter = mapIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter = mapIcu.get(
        CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue());
    // Initial flattening of the patient ids by treatment level
    if (setVentilationPids == null || setEcmoPids == null) {
      setVentilationPids = listVentilationEncounter.stream()
          .map(UkbEncounter::getPatientId).collect(
              Collectors.toSet());
      setEcmoPids = listEcmoEncounter.stream()
          .map(UkbEncounter::getPatientId).collect(
              Collectors.toSet());
    }
    // PIDs of the patients that had no ventilation and no ecmo.
    Set<String> setIcuOnlyPids = new HashSet<>();
    Set<UkbEncounter> setEncounterToBeRemoved = new HashSet<>();

    switch (treatmentLevel) {
      case ICU -> {
        // checks if the encounter has a higher treatmentlevel
        for (UkbEncounter icuEncounter : listIcuEncounter) {
          String currentPid = icuEncounter.getPatientId();
          if (!setEcmoPids.contains(currentPid) && !setVentilationPids.contains(
              currentPid)) {
            // Sort cases by pids; example: Map<Pid,Cases> Pid[123456,7891234]
            addPidToMap(mapPidCases, icuEncounter);
            setIcuOnlyPids.add(icuEncounter.getPatientId());
          }
        }

        // Figuring out the first covid case by patient
        List<UkbEncounter> listTempEncounter = getPatientsFirstCovidEncounter(
            mapPidCases);
        resultList.addAll(listTempEncounter);
        // Checks if encounter.getPatientId is in an encounter with a higher treatmentlevel
        // Starting with checking if there is a ventilation encounter
        for (UkbEncounter ventEncounter : listVentilationEncounter) {
          if (ventEncounter != null) {
            if (setIcuOnlyPids.contains(ventEncounter.getPatientId())) {
              setEncounterToBeRemoved.add(ventEncounter);
            }
          }
        }
        // And checking if there is an ecmo encounter aswell
        for (UkbEncounter ecmoEncounter : listEcmoEncounter) {
          if (ecmoEncounter != null) {
            if (setIcuOnlyPids.contains(ecmoEncounter.getPatientId())) {
              setEncounterToBeRemoved.add(ecmoEncounter);
            }
          }
        }
        // Removing the cases that have a higher treatmentlevel than ICU
        resultList.removeAll(setEncounterToBeRemoved);
      }
      case ICU_VENTILATION -> {
        // checks if the encounter has a higher treatmentlevel
        for (UkbEncounter encounter : listVentilationEncounter) {
          if (!listEcmoEncounter.contains(encounter)) {
            // Sort cases by pids; example: Map<Pid,Cases> Pid[123456,7891234]
            addPidToMap(mapPidCases, encounter);
          }
        }
        // If there are multiple cases with the same pid and same treatmentlevel, then only the first one should be counted
        resultList.addAll(getPatientsFirstCovidEncounter(
            mapPidCases));
      }
      case ICU_ECMO -> {
        for (UkbEncounter encounter : listEcmoEncounter) {
          addPidToMap(mapPidCases, encounter);
        }
        // check if there are two or more cases which belong to one pid
        resultList.addAll(getPatientsFirstCovidEncounter(
            mapPidCases));
      }
    }

    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByIcuLevel");
    return resultList;
  }

  /**
   * Identify patients' first positive corona cases without considering any subsequent positive
   * corona cases.
   *
   * @param mapPidCase A map that assigns all associated positive covid cases (0:n) to a pid.
   * @return List of {@link UkbEncounter} resources of first positive covid cases of transferred
   * patients.
   */
  private List<UkbEncounter> getPatientsFirstCovidEncounter(
      Map<String, List<UkbEncounter>> mapPidCase) {
    List<UkbEncounter> encounters = new ArrayList<>();
    for (Map.Entry<String, List<UkbEncounter>> mapEncountersByPid : mapPidCase.entrySet()) {
      encounters.add(getFirstCovidPositiveCase(mapEncountersByPid));
    }
    return encounters;
  }

  /**
   * Returns the case from an encounter list with the lowest admission date.
   * ({@link UkbEncounter#getPeriod()})
   *
   * @param mapEncounterByPid Map of all encounters per pid.
   * @return The oldest positive C19 case of a patient.
   */
  private UkbEncounter getFirstCovidPositiveCase(
      Map.Entry<String, List<UkbEncounter>> mapEncounterByPid) {
    return mapEncounterByPid.getValue().stream().filter(UkbEncounter::isPeriodStartExistent)
        .min(Comparator.comparing(x -> x.getPeriod().getStart())).orElse(null);
  }

  /**
   * Add specific encounter to a map.
   *
   * @param mapPidAndCase Map consisting of encounter sorted by their pids.
   * @param encounter     The {@link UkbEncounter} that has to be added.
   */
  private void addPidToMap(Map<String, List<UkbEncounter>> mapPidAndCase,
      UkbEncounter encounter) {
    if (mapPidAndCase.containsKey(encounter.getPatientId())) {
      mapPidAndCase.get(encounter.getPatientId()).add(encounter);
    } else {
      // Initialization of the encounter list for the given patient
      mapPidAndCase.put(encounter.getPatientId(),
          new ArrayList<>(Collections.singletonList(encounter)));
    }
  }
}
