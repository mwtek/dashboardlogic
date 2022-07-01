package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;

/**
 * This class is used for generating the data item {@link CoronaDataItem cumulative.maxtreatmentlevel.age.* subitems}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j public class CumulativeMaxTreatmentLevelAge {
  public List<UkbPatient> listPatients = new ArrayList<>();

  public CumulativeMaxTreatmentLevelAge(List<UkbPatient> listPatients) {
    this.listPatients = listPatients;
  }

  public List<Integer> createMaxTreatmentLevelAgeMap(
          Map<String, List<UkbEncounter>> mapPositiveEncounterByClass,
          Map<String, List<UkbEncounter>> mapIcu, String treatmentlevel) {
    log.debug("started createMaxTreatmentLevelAgeMap");
    Instant startTimer = TimerTools.startTimer();

    List<Integer> resultList = new ArrayList<>();
    Map<String, UkbEncounter> pidAdmissionMap = new HashMap<>();

    // Identify when the positive cases were first recorded.
    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        CoronaResultFunctionality.sortingFirstAdmissionDateToPid(encounter, pidAdmissionMap);
      }
    }
    // Calculation of patient age and sorting by their maxtreatmentlevel.
    for (Map.Entry<String, UkbEncounter> entry : pidAdmissionMap.entrySet()) {
      boolean isNormal =
              mapPositiveEncounterByClass.get(CoronaFixedValues.STATIONARY_ITEM.getValue())
                      .contains(entry.getValue());
      boolean isIcu = mapIcu.get(CoronaFixedValues.ICU.getValue()).contains(entry.getValue());
      boolean isVent =
              mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue()).contains(entry.getValue());
      boolean isEcmo = mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue()).contains(entry.getValue());
      UkbPatient patient =
              listPatients.stream().filter(p -> p.getId().equals(entry.getKey())).findAny()
                      .orElse(null);

      if (entry.getValue().isPeriodStartExistent() && patient != null) {
        Date validAdmissionDate = entry.getValue().getPeriod().getStart();
        if (isNormal && !isIcu && !isVent && !isEcmo && treatmentlevel.equals(
                CoronaFixedValues.NORMAL_WARD.getValue())) {
          if(patient.hasBirthDate()) {
            int age = CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
                    validAdmissionDate);
            resultList.add(CoronaResultFunctionality.checkAgeGroup(age));
          }
          else {
            log.warn("Could not find a birthday in the resource of patient " + patient.getId());
          }
        } else if (isIcu && !isVent && !isEcmo && treatmentlevel.equals(
                CoronaFixedValues.ICU.getValue())) {
          if(patient.hasBirthDate()) {
            int age = CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
                    validAdmissionDate);
            resultList.add(CoronaResultFunctionality.checkAgeGroup(age));
          }
          else {
            log.warn("Could not find a birthday in the resource of patient " + patient.getId());
          }
        } else if (isVent && !isEcmo && treatmentlevel.equals(
                CoronaFixedValues.ICU_VENTILATION.getValue())) {
          if(patient.hasBirthDate()) {
            if(patient.hasBirthDate()) {
              int age = CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
                      validAdmissionDate);
              resultList.add(CoronaResultFunctionality.checkAgeGroup(age));
            }
            else {
              log.warn("Could not find a birthday in the resource of patient " + patient.getId());
            }
          }
          else {
            log.warn("Could not find a birthday in the resource of patient " + patient.getId());
          }
        } else if (isEcmo && treatmentlevel.equals(CoronaFixedValues.ICU_ECMO.getValue())) {
          if(patient.hasBirthDate()) {
            int age = CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
                    validAdmissionDate);
            resultList.add(CoronaResultFunctionality.checkAgeGroup(age));
          }
          else {
            log.warn("Could not find a birthday in the resource of patient " + patient.getId());
          }
        }
      } else if (patient == null) {
        log.error("Patient " + entry.getKey() + " not found in pidAdmissionMap.");
      }
    }
    Collections.sort(resultList);
    TimerTools.stopTimerAndLog(startTimer, "finished createMaxTreatmentLevelAgeMap");
    return resultList;
  }
}
