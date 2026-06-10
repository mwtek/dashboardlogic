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
 */ package de.ukbonn.mwtek.dashboardlogic.logic;

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.CASE_MERGED;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.THRESHOLD_DAYS_CASE_MERGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.UPPER_AGE_BORDER;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.filterEncountersByIds;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isEncounterValidByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.getIcdCodesAsString;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools.getEncounterIdsByIcdCodes;
import static de.ukbonn.mwtek.utilities.generic.time.DateTools.calcWholeDaysBetweenDates;

import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData.AdmissionStatus;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityContactIcuLocationMap;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;

@Slf4j
public class KiraHandlingLogic {

  public static final int DAYS_BETWEEN_READMISSION = 182;

  public static MergeResult mergeEncounterAndFilterByAge(
      List<MiiEncounter> facilityContactEncountersPreFiltered,
      List<MiiCondition> conditionsFiltered,
      List<MiiProcedure> proceduresFiltered,
      List<MiiPatient> patientsFiltered,
      InputCodeSettings inputCodeSettings) {

    Set<String> casesDeletedByDiagnosis = new HashSet<>();

    // Canonical registry used by mergeCases to mutate CoreCaseData
    Map<String, CoreCaseData> registryByEncounterId =
        generateCoreCaseDataByEncounterIdMap(
            conditionsFiltered,
            facilityContactEncountersPreFiltered,
            patientsFiltered,
            inputCodeSettings);

    // Group by patient and merge (MUST mutate registryByEncounterId entries)
    Map<String, List<Map.Entry<String, CoreCaseData>>> byPatient =
        registryByEncounterId.entrySet().stream()
            .collect(Collectors.groupingBy(e -> e.getValue().getPatientId(), Collectors.toList()));

    for (var entry : byPatient.entrySet()) {
      List<Map.Entry<String, CoreCaseData>> cases = new ArrayList<>(entry.getValue());
      if (cases.size() > 1) {
        mergeCases(
            conditionsFiltered,
            proceduresFiltered,
            facilityContactEncountersPreFiltered,
            cases,
            casesDeletedByDiagnosis,
            registryByEncounterId); // <-- mutate canonical instances + delete merged keys
      }
    }

    // Build merged encounter list consistent with registry keys
    List<MiiEncounter> mergedEncounters =
        facilityContactEncountersPreFiltered.stream()
            .filter(x -> registryByEncounterId.containsKey(x.getId()))
            .filter(x -> !casesDeletedByDiagnosis.contains(x.getId()))
            .filter(x -> isEncounterValidByAge(registryByEncounterId, x, UPPER_AGE_BORDER))
            .toList();

    return new MergeResult(mergedEncounters, registryByEncounterId);
  }

  public record MergeResult(
      List<MiiEncounter> mergedEncounters, Map<String, CoreCaseData> registryByEncounterId) {}

  public static Map<String, CoreCaseData> mergeCoreCaseDataLists(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByKjpDiagnosis,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByRsvDiagnosis) {
    {
      return Stream.concat(
              coreCaseDataByKjpDiagnosis.values().stream()
                  .flatMap(innerMap -> innerMap.entrySet().stream()),
              coreCaseDataByRsvDiagnosis.values().stream()
                  .flatMap(innerMap -> innerMap.entrySet().stream()))
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  // No need to find special handling for replicates
                  (existing, replacement) -> replacement));
    }
  }

  public static Map<String, CoreCaseData> mergeCoreCaseDataList(
      Map<String, Map<String, CoreCaseData>> coreCaseData) {
    {
      return coreCaseData.values().stream()
          .flatMap(innerMap -> innerMap.entrySet().stream())
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> replacement));
    }
  }

  /**
   * Merges multiple cases for a single patient.
   *
   * @param conditions Condition resources where the references may need to be updated.
   * @param procedures Procedure resources where the references may need to be updated.
   * @param facilityEncounters Facility encounters where the update procedure should take part.
   * @param caseDataEntries List of case data entries for a single patient.
   * @param casesDeletedByDiagnosis Set of IDs of cases that have been merged.
   * @param coreCaseDataByEncounterIdMap Map of encounter IDs to CoreCaseData.
   */
  public static void mergeCases(
      List<MiiCondition> conditions,
      List<MiiProcedure> procedures,
      List<MiiEncounter> facilityEncounters,
      List<Entry<String, CoreCaseData>> caseDataEntries,
      Set<String> casesDeletedByDiagnosis,
      Map<String, CoreCaseData> coreCaseDataByEncounterIdMap) {
    // Sort the case data entries by admission date
    caseDataEntries.sort(Comparator.comparing(e -> e.getValue().getAdmissionDate()));

    // Currently merging is just necessary for the disorders-only encounters
    List<CoreCaseData> sortedCoreCaseData =
        caseDataEntries.stream()
            //            .filter(x -> x.getValue().isKjpDiagnosis())
            .map(Entry::getValue)
            .collect(Collectors.toList());

    Set<String> encountersToBeDeleted = new HashSet<>();
    int i = 0;

    // Iterate through the sorted list and perform the merge checks
    while (i < sortedCoreCaseData.size() - 1) {
      CoreCaseData currentCase = sortedCoreCaseData.get(i);
      CoreCaseData nextCase = sortedCoreCaseData.get(i + 1);

      if (currentCase.getDischargeDate() == null) {
        log.warn(
            "No discharge date found for case {}. Case merging aborted.",
            currentCase.getFacilityEncounterId());
        // Stop merging completely
        return;
      }

      // Calculate the number of days between the discharge date of the current case and the
      // admission date of the next case
      long daysBetween =
          DateTools.calcWholeDaysBetweenDates(
              currentCase.getDischargeDate(), nextCase.getAdmissionDate());
      if (daysBetween <= THRESHOLD_DAYS_CASE_MERGE) {
        // Merge the two cases
        mergeTwoCases(
            conditions,
            procedures,
            facilityEncounters,
            currentCase,
            nextCase,
            encountersToBeDeleted,
            coreCaseDataByEncounterIdMap);

        // Remove the merged case from the list
        sortedCoreCaseData.remove(i + 1);
        caseDataEntries.remove(i + 1);

        // Restart the loop to ensure all merges are handled
        i = 0;
      } else {
        i++;
      }
    }

    if (!encountersToBeDeleted.isEmpty()) {
      casesDeletedByDiagnosis.addAll(encountersToBeDeleted);
      log.info(
          "{} got {} encounter(s) removed",
          caseDataEntries.get(0).getValue().getPatientId(),
          encountersToBeDeleted.size());
    }
  }

  /**
   * Merges two specific cases.
   *
   * @param conditions Condition resources where the reference needs to be updated.
   * @param procedures Procedure resources where the reference needs to be updated.
   * @param currentCase The current case to be merged.
   * @param nextCase The next case to be merged.
   * @param encountersToBeDeleted Set of IDs of cases that have been merged.
   * @param coreCaseDataByEncounterIdMap Map of encounter IDs to CoreCaseData.
   */
  private static void mergeTwoCases(
      List<MiiCondition> conditions,
      List<MiiProcedure> procedures,
      List<MiiEncounter> encounters,
      CoreCaseData currentCase,
      CoreCaseData nextCase,
      Set<String> encountersToBeDeleted,
      Map<String, CoreCaseData> coreCaseDataByEncounterIdMap) {

    // Null checks for essential inputs
    if (currentCase == null || nextCase == null) {
      log.error("Current case or next case is null.");
      return;
    }

    // Mark the next case as merged by adding its ID to the deleted set
    encountersToBeDeleted.add(nextCase.getFacilityEncounterId());

    // Log the discharge date update (even if it doesn't happen)
    log.info(
        "Discharge date updated for case id {} from {} to {}",
        currentCase.getFacilityEncounterId(),
        currentCase.getDischargeDate(),
        nextCase.getDischargeDate());

    // Try to find the encounter and update it
    updateUkbEncounter(encounters, currentCase, nextCase);

    // Update references and discharge date of the current case
    updateCaseAndReferences(
        conditions, procedures, currentCase, nextCase, coreCaseDataByEncounterIdMap);
  }

  /** Updates the corresponding UkbEncounter's discharge date and adds a merged extension. */
  private static void updateUkbEncounter(
      List<MiiEncounter> encounters, CoreCaseData currentCase, CoreCaseData nextCase) {
    Optional<MiiEncounter> encounter =
        encounters.stream()
            .filter(x -> x.getId().equals(currentCase.getFacilityEncounterId()))
            .findFirst();

    encounter.ifPresentOrElse(
        enc -> {
          Date currentEncounterEnd = enc.getPeriod().getEnd();

          // Only update if the next case's discharge date is later
          if (shouldUpdateDate(currentEncounterEnd, nextCase.getDischargeDate())) {
            enc.getPeriod().setEnd(nextCase.getDischargeDate());
          }

          // Add the extension indicating this case was merged
          enc.addExtension(CASE_MERGED);
        },
        () ->
            log.warn(
                "Encounter with id {} not found in facilityContactEncounters",
                currentCase.getFacilityEncounterId()));
  }

  /** Updates the encounter references and possibly the discharge date for the merged case. */
  private static void updateCaseAndReferences(
      List<MiiCondition> conditions,
      List<MiiProcedure> procedures,
      CoreCaseData currentCase,
      CoreCaseData nextCase,
      Map<String, CoreCaseData> coreCaseDataByEncounterIdMap) {

    // Update encounter references
    updateConditionEncounterReferences(
        conditions, nextCase.getFacilityEncounterId(), currentCase.getFacilityEncounterId());
    updateProcedureEncounterReferences(
        procedures, nextCase.getFacilityEncounterId(), currentCase.getFacilityEncounterId());

    // Update the discharge date if necessary
    if (shouldUpdateDate(currentCase.getDischargeDate(), nextCase.getDischargeDate())) {
      // Store the gap period between both cases since we need to exclude the gaps in some charts
      currentCase
          .getGapPeriods()
          .add(
              new Period()
                  .setStart(currentCase.getDischargeDate())
                  .setEnd(nextCase.getAdmissionDate()));
      log.trace(
          "Gap period: {}_{}",
          currentCase.getFacilityEncounterId(),
          currentCase.getGapPeriods().size());

      currentCase.setDischargeDate(nextCase.getDischargeDate());
    }

    // Remove the next case from the map
    coreCaseDataByEncounterIdMap.remove(nextCase.getFacilityEncounterId());
  }

  /** Helper method to determine if the discharge date should be updated. */
  private static boolean shouldUpdateDate(Date current, Date proposed) {
    return proposed != null && current.before(proposed);
  }

  /**
   * Updates the encounter references for conditions of the cases that get merged.
   *
   * @param oldEncounterId the old encounter ID to be replaced, must not be null
   * @param newEncounterId the new encounter ID to replace with, must not be null
   * @throws IllegalArgumentException if either oldEncounterId or newEncounterId is null
   */
  public static void updateConditionEncounterReferences(
      List<MiiCondition> conditions, String oldEncounterId, String newEncounterId) {
    if (oldEncounterId == null || newEncounterId == null) {
      throw new IllegalArgumentException("Encounter IDs must not be null");
    }
    conditions.stream()
        .filter(condition -> condition.getCaseId().equals(oldEncounterId))
        .forEach(
            condition -> {
              condition.setCaseId(newEncounterId);
              log.info(
                  "Updated condition {} encounter id from {} to {}",
                  condition.getId(),
                  oldEncounterId,
                  newEncounterId);
            });
  }

  /**
   * Updates the encounter references for procedures of the cases that get merged.
   *
   * @param procedures the list of procedures to update; may be empty but not null-safe if method is
   *     called with null
   * @param oldEncounterId the old encounter ID to be replaced, must not be null
   * @param newEncounterId the new encounter ID to replace with, must not be null
   */
  public static void updateProcedureEncounterReferences(
      List<MiiProcedure> procedures, String oldEncounterId, String newEncounterId) {

    if (oldEncounterId == null || newEncounterId == null) {
      throw new IllegalArgumentException("Encounter IDs must not be null");
    }
    if (procedures == null || procedures.isEmpty()) return;

    procedures.stream()
        .filter(Objects::nonNull)
        .filter(p -> oldEncounterId.equals(p.getCaseId()))
        .forEach(
            p -> {
              p.setCaseId(newEncounterId);
              log.info(
                  "Updated procedure {} encounter id from {} to {}",
                  p.getId(),
                  oldEncounterId,
                  newEncounterId);
            });
  }

  private static Map<String, CoreCaseData> generateCoreCaseDataByEncounterIdMap(
      List<MiiCondition> conditions,
      List<MiiEncounter> facilityContactEncountersFiltered,
      List<MiiPatient> patientsFiltered,
      InputCodeSettings inputCodeSettings) {

    Map<String, CoreCaseData> caseDataByPatientId = new HashMap<>();
    prepareCasesByDiagnosisType(
        caseDataByPatientId,
        conditions,
        facilityContactEncountersFiltered,
        patientsFiltered,
        inputCodeSettings.getKidsRadarConditionKjpIcdCodes());
    return caseDataByPatientId;
  }

  public static void processDiagnosisType(
      Map<String, Map<String, CoreCaseData>> result,
      Map<String, CoreCaseData> registry,
      List<MiiCondition> conditions,
      List<MiiEncounter> facilityContactEncountersFiltered,
      FacilityContactIcuLocationMap facilityContactIcuLocationMap,
      List<MiiPatient> patientsFiltered,
      Map<String, List<String>> icdCodesByDiag) {

    if (icdCodesByDiag == null) {
      Map<MiiEncounter, MiiPatient> allEncounters =
          mapEncountersToPatients(
              new HashSet<>(facilityContactEncountersFiltered), patientsFiltered);

      Map<String, CoreCaseData> allCases = new HashMap<>();
      allEncounters.forEach(
          (encounter, patient) -> {
            List<Encounter.EncounterLocationComponent> icuLocationList =
                facilityContactIcuLocationMap.get(encounter.getFacilityContactId());
            addCaseViaRegistry(registry, allCases, encounter, icuLocationList, patient);
          });
      result.put("ALL", allCases);
      return;
    }

    icdCodesByDiag.forEach(
        (name, icdCodes) -> {
          Set<String> encounterIds = getEncounterIdsByIcdCodes(conditions, icdCodes);
          Set<MiiEncounter> relevantEncounters =
              filterEncountersByIds(facilityContactEncountersFiltered, encounterIds);

          Map<MiiEncounter, MiiPatient> encounterPatientMap =
              mapEncountersToPatients(relevantEncounters, patientsFiltered);

          Map<String, CoreCaseData> casesByDiagnosis = new HashMap<>();
          encounterPatientMap.forEach(
              (encounter, patient) ->
                  addCaseViaRegistry(registry, casesByDiagnosis, encounter, null, patient));

          result.put(name, casesByDiagnosis);
        });
  }

  private static void prepareCasesByDiagnosisType(
      Map<String, CoreCaseData> result,
      List<MiiCondition> conditions,
      List<MiiEncounter> facilityContactEncountersFiltered,
      List<MiiPatient> patientsFiltered,
      Map<String, List<String>> icdCodes) {

    Set<String> encounterIds = getEncounterIdsByIcdCodes(conditions, getIcdCodesAsString(icdCodes));
    Set<MiiEncounter> encountersByDiagnosisType =
        filterEncountersByIds(facilityContactEncountersFiltered, encounterIds);
    Map<MiiEncounter, MiiPatient> encounterIdPatientMap =
        mapEncountersToPatients(encountersByDiagnosisType, patientsFiltered);
    encounterIdPatientMap.forEach(
        (encounter, patient) -> addCaseDataIfAbsent(result, encounter, null, patient));
  }

  private static void addCaseViaRegistry(
      Map<String, CoreCaseData> registry,
      Map<String, CoreCaseData> target,
      MiiEncounter encounter,
      List<Encounter.EncounterLocationComponent> icuLocationsOrNull,
      MiiPatient patient) {

    String key = encounter.getFacilityContactId();
    CoreCaseData ccd = registry.get(key);

    if (ccd == null) {
      Date admissionDate = encounter.getPeriod().getStart();
      int ageYears = DateTools.calcYearsBetweenDates(admissionDate, patient.getBirthDate());
      int ageMonths = DateTools.calcMonthsBetweenDates(admissionDate, patient.getBirthDate());

      ccd =
          new CoreCaseData(
              admissionDate,
              encounter.getPeriod().getEnd(),
              key,
              encounter.getPatientId(),
              patient,
              ageYears,
              ageMonths,
              icuLocationsOrNull,
              AdmissionStatus.NEW_ADMISSION,
              new ArrayList<>(),
              new ArrayList<>());
      registry.put(key, ccd);
    } else {
      // Merging optional infos
      if (icuLocationsOrNull != null
          && (ccd.getLocationComponentList() == null || ccd.getLocationComponentList().isEmpty())) {
        ccd.setLocationComponentList(icuLocationsOrNull);
      }
    }
    target.putIfAbsent(key, ccd);
  }

  /**
   * Maps encounters to corresponding patients.
   *
   * @param encounters The set of encounters.
   * @param patients The list of patients.
   * @return A map of encounters to patients.
   */
  public static Map<MiiEncounter, MiiPatient> mapEncountersToPatients(
      Set<MiiEncounter> encounters, List<MiiPatient> patients) {
    return encounters.parallelStream()
        .collect(
            Collectors.toMap(
                encounter -> encounter,
                encounter ->
                    patients.stream()
                        .filter(patient -> patient.getId().equals(encounter.getPatientId()))
                        .findFirst()
                        .orElse(null)));
  }

  /**
   * Populates case data with admission details if not already present in the result map.
   *
   * @param result The map to store CoreCaseData, keyed by encounter ID.
   * @param encounter The encounter associated with the case.
   * @param patient The patient associated with the encounter.
   */
  public static void addCaseDataIfAbsent(
      Map<String, CoreCaseData> result,
      MiiEncounter encounter,
      List<Encounter.EncounterLocationComponent> icuLocations,
      MiiPatient patient) {
    Date admissionDate = encounter.getPeriod().getStart();
    int ageAtAdmission = DateTools.calcYearsBetweenDates(admissionDate, patient.getBirthDate());
    int ageAtAdmissionInMonths =
        DateTools.calcMonthsBetweenDates(admissionDate, patient.getBirthDate());

    if (!result.containsKey(encounter.getId())) {
      result.put(
          encounter.getId(),
          new CoreCaseData(
              admissionDate,
              encounter.getPeriod().getEnd(),
              encounter.getId(),
              encounter.getPatientId(),
              patient,
              ageAtAdmission,
              ageAtAdmissionInMonths,
              icuLocations,
              AdmissionStatus.NEW_ADMISSION,
              new ArrayList<>(),
              new ArrayList<>()));
    }
  }

  public static Map<String, Map<String, CoreCaseData>> createCasesByDiag(
      List<MiiCondition> conditions,
      List<MiiEncounter> facilityContactEncountersFiltered,
      FacilityContactIcuLocationMap facilityContactIcuLocationMap,
      List<MiiPatient> patientsFiltered,
      Map<String, List<String>> icdCodeMap) {

    Map<String, Map<String, CoreCaseData>> result = new HashMap<>();
    // central registry to make sure there is just ONE core case data object per case
    Map<String, CoreCaseData> registry = new HashMap<>();

    processDiagnosisType(
        result,
        registry,
        conditions,
        facilityContactEncountersFiltered,
        facilityContactIcuLocationMap,
        patientsFiltered,
        icdCodeMap);

    return result;
  }

  public static Map<String, Map<String, CoreCaseData>> createCasesByDiag(
      List<MiiCondition> conditions,
      List<MiiEncounter> facilityContactEncountersFiltered,
      FacilityContactIcuLocationMap facilityContactIcuLocationMap,
      List<MiiPatient> patientsFiltered,
      Map<String, List<String>> icdCodeMap,
      Map<String, CoreCaseData> registryByEncounterId) {

    Map<String, Map<String, CoreCaseData>> result = new HashMap<>();

    processDiagnosisType(
        result,
        registryByEncounterId,
        conditions,
        facilityContactEncountersFiltered,
        facilityContactIcuLocationMap,
        patientsFiltered,
        icdCodeMap);

    return result;
  }

  /**
   * Updates the status of each CoreCaseData according to the rule: - RE_ADMISSION if the most
   * recent previous case for the same patient has a discharge date within 182 days (inclusive)
   * before the current admission date.
   */
  public static void updateAdmissionStatuses(Map<String, CoreCaseData> coreCaseDataAll) {

    if (coreCaseDataAll == null || coreCaseDataAll.isEmpty()) return;

    // Group all cases by patientId and keep only those patients with more than one case
    Map<String, List<CoreCaseData>> byPatient =
        coreCaseDataAll.values().stream()
            .filter(c -> c != null && c.getPatientId() != null)
            .collect(Collectors.groupingBy(CoreCaseData::getPatientId))
            .entrySet()
            .stream()
            // only keep patients with more than one case
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Process each patient group
    for (Map.Entry<String, List<CoreCaseData>> e : byPatient.entrySet()) {
      List<CoreCaseData> cases = e.getValue();

      // Sort ascending by admissionDate
      cases.sort(
          Comparator.comparing(
              CoreCaseData::getAdmissionDate, Comparator.nullsLast(Comparator.naturalOrder())));

      Date lastDischarge = null;

      for (CoreCaseData c : cases) {
        Date admission = c.getAdmissionDate();

        if (admission == null) {
          // no admission date available set default to NEW_ADMISSION
          c.setStatus(CoreCaseData.AdmissionStatus.NEW_ADMISSION);
        } else if (lastDischarge == null) {
          // no previous case set new admission
          c.setStatus(CoreCaseData.AdmissionStatus.NEW_ADMISSION);
        } else {
          Long days = calcWholeDaysBetweenDates(lastDischarge, admission);

          if (days != null && days >= 0 && days <= DAYS_BETWEEN_READMISSION) {
            // set readmission if ≤ 182 days since last discharge
            c.setStatus(CoreCaseData.AdmissionStatus.RE_ADMISSION);
            log.debug(
                "Updating the admission status of case {} to readmission",
                c.getFacilityEncounterId());
          } else {
            c.setStatus(CoreCaseData.AdmissionStatus.NEW_ADMISSION);
          }
        }

        // update lastDischarge if the current case has a later discharge date
        if (c.getDischargeDate() != null) {
          if (lastDischarge == null || c.getDischargeDate().after(lastDischarge)) {
            lastDischarge = c.getDischargeDate();
          }
        }
      }
    }
  }

  /**
   * Updates ICU day information on {@link CoreCaseData} records using performed dates from
   * procedures.
   *
   * @param coreCaseDataAll container of all case-data entries keyed by (any) id; values are updated
   *     in place
   * @param procedures list of procedures contributing ICU day dates; may be empty
   */
  public static void updateIntensiveCareDays(
      Map<String, CoreCaseData> coreCaseDataAll, List<MiiProcedure> procedures) {

    if (coreCaseDataAll == null || coreCaseDataAll.isEmpty()) return;
    if (procedures == null || procedures.isEmpty()) return;

    // 1) Build index: caseId -> List<Date> (performed dates)
    Map<String, List<Date>> datesByCaseId =
        procedures.stream()
            .filter(Objects::nonNull)
            .filter(p -> p.getCaseId() != null && p.getPerformed() != null)
            .map(
                p ->
                    new AbstractMap.SimpleEntry<>(
                        p.getCaseId(),
                        // convert performed to Date; skip if value is null
                        p.getPerformed().dateTimeValue() == null
                            ? null
                            : p.getPerformed().dateTimeValue().getValue()))
            .filter(e -> e.getValue() != null)
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

    // Ensure distinct & sorted per caseId (keeps earliest -> latest)
    datesByCaseId.replaceAll(
        (caseId, list) -> {
          // Sort and distinct
          return list.stream()
              .filter(Objects::nonNull)
              .sorted()
              .distinct()
              .collect(Collectors.toCollection(ArrayList::new));
        });
    // 2) Build an index: caseId -> CoreCaseData (or List<CoreCaseData> if not unique)
    // If caseId is UNIQUE per CoreCaseData:
    Map<String, CoreCaseData> ccdByCaseId =
        coreCaseDataAll.values().stream()
            .filter(Objects::nonNull)
            .filter(ccd -> ccd.getFacilityEncounterId() != null)
            .collect(
                Collectors.toMap(CoreCaseData::getFacilityEncounterId, ccd -> ccd, (a, b) -> a));

    // 3) Update only matching entries
    for (Map.Entry<String, List<Date>> e : datesByCaseId.entrySet()) {
      CoreCaseData ccd = ccdByCaseId.get(e.getKey());
      if (ccd != null && !e.getValue().isEmpty()) {
        ccd.setIntensiveCareDays(e.getValue());
      }
    }
  }
}
