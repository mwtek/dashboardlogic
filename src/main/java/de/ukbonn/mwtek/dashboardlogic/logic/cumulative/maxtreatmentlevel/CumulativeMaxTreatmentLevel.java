package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;


/**
 * This class is used for generating the data item {@link CoronaDataItem
 * cumulative.maxtreatmentlevel}
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
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    Set<String> ambulantPidSet = new HashSet<>();
    Set<String> stationaryPidSet = new HashSet<>();
    List<UkbEncounter> listResult = new ArrayList<>();
    log.debug("started getCumulativeByClass");
    Instant startTime = TimerTools.startTimer();

    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      List<UkbEncounter> value = entry.getValue();
      switch (treatmentLevel) {
        case OUTPATIENT_ITEM: // check which case is ambulant and does not have the twelve Days Logic
          for (UkbEncounter encounter : value) {
            if (CoronaResultFunctionality.isCaseClassOutpatient(
                encounter) && !encounter.hasExtension(
                CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue())) {
              ambulantPidSet.add(encounter.getPatientId());
            }
          }
          break;
        case INPATIENT_ITEM: // check if stationary and is not any kind of ICU
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
          break;
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
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass,
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
    } catch (Exception e) {
      e.printStackTrace();
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

  /**
   * Creates a list which contains encounter who have or had icu, ventilation or ecmo as highest
   * treatmentlevel
   *
   * @param mapIcu         Map that assigns a list of case numbers to an ICU treatment level class
   * @param treatmentLevel TreatmentLevel as separation criterion
   * @return List of all encounter that have the given treatment level as maximum treatment level
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCumulativeByIcuLevel(
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
          if (!listEcmoEncounter.contains(encounter) && !listVentilationEncounter.contains(
              encounter)) {
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
      // checks if encounter.getPatientId is in an encounter with a higher treatmentlevel
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
   * Returns the case from an encounter list with the lowest admission date ({@link
   * UkbEncounter#getPeriod()})
   *
   * @param mapEncounterByPid Map of all encounters per pid
   * @return The oldest positive C19 case of a patient
   */
  private UkbEncounter getFirstCovidPositiveCase(
      Map.Entry<String, List<UkbEncounter>> mapEncounterByPid) {
    return mapEncounterByPid.getValue().stream().filter(UkbEncounter::isPeriodStartExistent)
        .min(Comparator.comparing(x -> x.getPeriod().getStart())).orElse(null);
  }

  /**
   * Add specific encounter to a map
   *
   * @param mapPidAndCase map consisting of encounter sorted by their pids
   * @param encounter     the encounter that has to be added
   */
  private void addPidToMap(HashMap<String, List<UkbEncounter>> mapPidAndCase,
      UkbEncounter encounter) {
    if (mapPidAndCase.containsKey(encounter.getPatientId())) {
      mapPidAndCase.get(encounter.getPatientId()).add(encounter);
    } else {
      List<UkbEncounter> temp = new ArrayList<>();
      temp.add(encounter);
      mapPidAndCase.put(encounter.getPatientId(), temp);
    }
  }
}
