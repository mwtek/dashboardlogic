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

package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel;

import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.checkAgeGroup;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.calculateAge;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem
 * cumulative.maxtreatmentlevel.age.* subitems}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeMaxTreatmentLevelAge extends DashboardDataItemLogic {

  public CumulativeMaxTreatmentLevelAge() {}

  private Set<String> outpatientPatientIds;
  private Set<String> inpatientPatientIds;
  private Set<String> icuPatientIds;
  private Set<String> icuVentPatientIds;
  private Set<String> ecmoPatientIds;
  Map<String, UkbEncounter> pidAdmissionMap = new ConcurrentHashMap<>();
  Set<UkbEncounter> encountersOverall = ConcurrentHashMap.newKeySet();
  Map<String, UkbPatient> patientMap = null;
  private boolean initialized = false;

  public List<Integer> createMaxTreatmentLevelAgeMap(
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass,
      Map<TreatmentLevels, List<UkbEncounter>> mapIcuOverall,
      List<UkbPatient> patients,
      TreatmentLevels treatmentLevel) {

    log.debug("Started createMaxTreatmentLevelAgeMap");
    Instant startTimer = TimerTools.startTimer();

    initializeIfNeeded(mapPositiveEncounterByClass, mapIcuOverall, patients);

    List<Integer> resultList = new ArrayList<>();

    for (Map.Entry<String, UkbEncounter> entry : pidAdmissionMap.entrySet()) {
      UkbPatient patient = patientMap.get(entry.getKey());

      if (patient == null) {
        log.debug("Skipping unknown patient with ID: " + entry.getKey());
        continue;
      }

      if (!patient.hasBirthDate()) {
        log.warn("No birthdate for patient ID: " + patient.getId());
        continue;
      }

      UkbEncounter encounter = entry.getValue();
      Date validAdmissionDate =
          encounter.getPeriod() != null ? encounter.getPeriod().getStart() : null;

      if (validAdmissionDate == null) {
        log.error("No admission date for patient ID: " + patient.getId());
        continue;
      }

      if (!hasHigherTreatmentLevel(patient.getId(), treatmentLevel)) {
        addPatientIfEligible(patient, validAdmissionDate, resultList, treatmentLevel);
      }
    }

    Collections.sort(resultList);
    TimerTools.stopTimerAndLog(startTimer, "Finished createMaxTreatmentLevelAgeMap");
    return resultList;
  }

  private void initializeIfNeeded(
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass,
      Map<TreatmentLevels, List<UkbEncounter>> mapIcuOverall,
      List<UkbPatient> patients) {
    if (!initialized) {
      pidAdmissionMap = new ConcurrentHashMap<>();
      encountersOverall = new HashSet<>();

      outpatientPatientIds = getPatientIds(mapPositiveEncounterByClass, OUTPATIENT);
      inpatientPatientIds = getPatientIds(mapPositiveEncounterByClass, INPATIENT);
      icuPatientIds = getPatientIds(mapIcuOverall, ICU);
      icuVentPatientIds = getPatientIds(mapIcuOverall, ICU_VENTILATION);
      ecmoPatientIds = getPatientIds(mapIcuOverall, ICU_ECMO);

      Stream.of(
              mapPositiveEncounterByClass.getOrDefault(OUTPATIENT, Collections.emptyList()),
              mapPositiveEncounterByClass.getOrDefault(INPATIENT, Collections.emptyList()),
              mapIcuOverall.getOrDefault(ICU, Collections.emptyList()),
              mapIcuOverall.getOrDefault(ICU_VENTILATION, Collections.emptyList()),
              mapIcuOverall.getOrDefault(ICU_ECMO, Collections.emptyList()))
          .forEach(encountersOverall::addAll);

      encountersOverall.forEach(
          encounter ->
              CoronaResultFunctionality.assignFirstAdmissionDateToPid(encounter, pidAdmissionMap));

      patientMap =
          patients.stream().collect(Collectors.toMap(UkbPatient::getId, Function.identity()));
      initialized = true;
    }
  }

  private boolean hasHigherTreatmentLevel(String patientId, TreatmentLevels level) {
    return switch (level) {
      case OUTPATIENT ->
          inpatientPatientIds.contains(patientId)
              || icuPatientIds.contains(patientId)
              || icuVentPatientIds.contains(patientId)
              || ecmoPatientIds.contains(patientId);
      case NORMAL_WARD ->
          icuPatientIds.contains(patientId)
              || icuVentPatientIds.contains(patientId)
              || ecmoPatientIds.contains(patientId);
      case ICU -> icuVentPatientIds.contains(patientId) || ecmoPatientIds.contains(patientId);
      case ICU_VENTILATION -> ecmoPatientIds.contains(patientId);
      default -> false;
    };
  }

  // 5. Kapselung der Patientenprüfung in eine eigene Methode
  private void addPatientIfEligible(
      UkbPatient patient,
      Date admissionDate,
      List<Integer> resultList,
      TreatmentLevels treatmentLevel) {
    boolean isEligible =
        switch (treatmentLevel) {
          case INPATIENT -> false;
          case OUTPATIENT -> outpatientPatientIds.contains(patient.getId());
          case NORMAL_WARD -> inpatientPatientIds.contains(patient.getId());
          case ICU -> icuPatientIds.contains(patient.getId());
          case ICU_VENTILATION -> icuVentPatientIds.contains(patient.getId());
          case ICU_ECMO -> ecmoPatientIds.contains(patient.getId());
          case ALL -> false;
        };

    if (isEligible) {
      addCohortAgeToList(patient, admissionDate, resultList);
    }
  }

  private static Set<String> getPatientIds(
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass,
      TreatmentLevels treatmentLevel) {
    return mapPositiveEncounterByClass.get(treatmentLevel).stream()
        .map(UkbEncounter::getPatientId)
        .collect(Collectors.toSet());
  }

  private Integer addCohortAgeToList(
      UkbPatient patient, Date validAdmissionDate, List<Integer> resultList) {
    if (patient.hasBirthDate()) {
      int age = calculateAge(patient.getBirthDate(), validAdmissionDate);
      resultList.add(checkAgeGroup(age));
      return age;
    } else {
      log.warn("Could not find a birthday in the resource of patient " + patient.getId());
    }
    return null;
  }
}
