package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel;

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.INPATIENT_ITEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.OUTPATIENT_ITEM;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link CoronaDataItem
 * cumulative.maxtreatmentlevel.age.* subitems}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeMaxTreatmentLevelAge {

  public List<UkbPatient> listPatients;

  public CumulativeMaxTreatmentLevelAge(List<UkbPatient> listPatients) {
    this.listPatients = listPatients;
  }

  public List<Integer> createMaxTreatmentLevelAgeMap(
      Map<String, List<UkbEncounter>> mapPositiveEncounterByClass,
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel) {
    log.debug("started createMaxTreatmentLevelAgeMap");
    Instant startTimer = TimerTools.startTimer();

    List<Integer> resultList = new ArrayList<>();
    Map<String, UkbEncounter> pidAdmissionMap = new HashMap<>();

    // Reducing the complexity of the records to the pid, since the calculation is done across cases.
    Set<String> outpatientPatientIds = mapPositiveEncounterByClass.get(OUTPATIENT_ITEM.getValue())
        .stream()
        .map(UkbEncounter::getPatientId).collect(Collectors.toSet());
    Set<String> inpatientPatientIds = mapPositiveEncounterByClass.get(INPATIENT_ITEM.getValue())
        .stream()
        .map(UkbEncounter::getPatientId).collect(Collectors.toSet());
    Set<String> icuPatientIds = mapIcu.get(CoronaFixedValues.ICU.getValue()).stream()
        .map(UkbEncounter::getPatientId).collect(Collectors.toSet());
    Set<String> icuVentPatientIds = mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue())
        .stream()
        .map(UkbEncounter::getPatientId).collect(Collectors.toSet());
    Set<String> ecmoPatientIds = mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue()).stream()
        .map(UkbEncounter::getPatientId).collect(Collectors.toSet());

    // Identify when the positive cases were first recorded.
    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        CoronaResultFunctionality.assignFirstAdmissionDateToPid(encounter, pidAdmissionMap);
      }
    }
    // Calculation of patient age and sorting by their maxtreatmentlevel.
    for (Map.Entry<String, UkbEncounter> entry : pidAdmissionMap.entrySet()) {
      boolean isOutpatient =
          outpatientPatientIds
              .contains(entry.getValue().getPatientId());
      boolean isNormal =
          inpatientPatientIds.contains(entry.getValue().getPatientId());
      boolean isIcu = icuPatientIds.contains(entry.getValue().getPatientId());
      boolean isVent =
          icuVentPatientIds.contains(entry.getValue().getPatientId());
      boolean isEcmo = ecmoPatientIds.contains(entry.getValue().getPatientId());
      // Identification of the patient resource of the current encounter for the retrieval of the birthdate.
      UkbPatient patient =
          listPatients.stream().filter(p -> p.getId().equals(entry.getKey())).findAny()
              .orElse(null);

      if (entry.getValue().isPeriodStartExistent() && patient != null) {
        Date validAdmissionDate = entry.getValue().getPeriod().getStart();
        if (isOutpatient && !isNormal && !isIcu && !isVent && !isEcmo
            && treatmentLevel == OUTPATIENT_ITEM) {
          addCohortAgeToList(patient, validAdmissionDate, resultList);
        } else if (isNormal && !isIcu && !isVent && !isEcmo && treatmentLevel == NORMAL_WARD) {
          addCohortAgeToList(patient, validAdmissionDate, resultList);
        } else if (isIcu && !isVent && !isEcmo && treatmentLevel == ICU) {
          addCohortAgeToList(patient, validAdmissionDate, resultList);
        } else if (isVent && !isEcmo && treatmentLevel == ICU_VENTILATION) {
          if (patient.hasBirthDate()) {
            addCohortAgeToList(patient, validAdmissionDate, resultList);
          } else {
            log.warn("Could not find a birthday in the resource of patient " + patient.getId());
          }
        } else if (isEcmo && treatmentLevel == ICU_ECMO) {
          addCohortAgeToList(patient, validAdmissionDate, resultList);
        }
      } else if (patient == null) {
        log.error(
            "Unable to find an encounter with an admission date for patient " + entry.getKey());
      }
    }
    Collections.sort(resultList);
    TimerTools.stopTimerAndLog(startTimer, "finished createMaxTreatmentLevelAgeMap");
    return resultList;
  }

  private void addCohortAgeToList(UkbPatient patient, Date validAdmissionDate,
      List<Integer> resultList) {
    if (patient.hasBirthDate()) {
      int age = CoronaResultFunctionality.calculateAge(patient.getBirthDate(),
          validAdmissionDate);
      resultList.add(CoronaResultFunctionality.checkAgeGroup(age));
    } else {
      log.warn("Could not find a birthday in the resource of patient " + patient.getId());
    }
  }
}
