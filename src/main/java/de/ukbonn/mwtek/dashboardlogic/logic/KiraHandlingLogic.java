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

import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KiraHandlingLogic {

  /**
   * Merges encounters based on certain criteria.
   *
   * @return List of UkbEncounter objects after merging.
   */
  public static List<UkbEncounter> mergeEncounterAndFilterByAge(
      List<UkbEncounter> facilityContactEncountersPreFiltered,
      List<UkbCondition> conditionsFiltered,
      List<UkbPatient> patientsFiltered,
      InputCodeSettings inputCodeSettings) {
    Set<String> casesDeletedByDiagnosis = new HashSet<>();

    Map<String, CoreCaseData> coreCaseDataByEncounterIdMap =
        generateCoreCaseDataByEncounterIdMap(
            conditionsFiltered,
            facilityContactEncountersPreFiltered,
            patientsFiltered,
            inputCodeSettings);

    // Group the CoreCaseData entries by patientId
    Map<String, List<Entry<String, CoreCaseData>>> patientIdToCaseDataEntriesMap =
        coreCaseDataByEncounterIdMap.entrySet().stream()
            .collect(
                Collectors.groupingBy(
                    entry -> entry.getValue().getPatientId(), Collectors.toList()));

    // Iterate over each patient's case data entries
    for (Entry<String, List<Entry<String, CoreCaseData>>> entry :
        patientIdToCaseDataEntriesMap.entrySet()) {
      List<Entry<String, CoreCaseData>> caseDataEntries = new ArrayList<>(entry.getValue());

      // If the patient has more than one encounter, attempt to merge them
      if (caseDataEntries.size() > 1) {
        mergeCases(
            conditionsFiltered,
            facilityContactEncountersPreFiltered,
            caseDataEntries,
            casesDeletedByDiagnosis,
            coreCaseDataByEncounterIdMap);
      }
    }

    log.info("{} cases got removed due to case merging", casesDeletedByDiagnosis.size());

    // Return the list of UkbEncounter objects, excluding those that were merged
    return facilityContactEncountersPreFiltered.parallelStream()
        .filter(x -> coreCaseDataByEncounterIdMap.containsKey(x.getId()))
        .filter(x -> !casesDeletedByDiagnosis.contains(x.getId()))
        .filter(x -> isEncounterValidByAge(coreCaseDataByEncounterIdMap, x, UPPER_AGE_BORDER))
        .toList();
  }

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
                  (existing, replacement) ->
                      replacement // No need to find special handling for replicates
                  ));
    }
  }

  /**
   * Merges multiple cases for a single patient.
   *
   * @param conditions Condition resources where the references may need to be updated.
   * @param facilityEncounters Facility encounters where the update procedure should take part.
   * @param caseDataEntries List of case data entries for a single patient.
   * @param casesDeletedByDiagnosis Set of IDs of cases that have been merged.
   * @param coreCaseDataByEncounterIdMap Map of encounter IDs to CoreCaseData.
   */
  public static void mergeCases(
      List<UkbCondition> conditions,
      List<UkbEncounter> facilityEncounters,
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
        log.warn("No discharge date found for case {}", currentCase.getFacilityEncounterId());
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
   * @param currentCase The current case to be merged.
   * @param nextCase The next case to be merged.
   * @param encountersToBeDeleted Set of IDs of cases that have been merged.
   * @param coreCaseDataByEncounterIdMap Map of encounter IDs to CoreCaseData.
   */
  private static void mergeTwoCases(
      List<UkbCondition> conditions,
      List<UkbEncounter> encounters,
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
    updateCaseAndReferences(conditions, currentCase, nextCase, coreCaseDataByEncounterIdMap);
  }

  /** Updates the corresponding UkbEncounter's discharge date and adds a merged extension. */
  private static void updateUkbEncounter(
      List<UkbEncounter> encounters, CoreCaseData currentCase, CoreCaseData nextCase) {
    Optional<UkbEncounter> encounter =
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
      List<UkbCondition> conditions,
      CoreCaseData currentCase,
      CoreCaseData nextCase,
      Map<String, CoreCaseData> coreCaseDataByEncounterIdMap) {

    // Update encounter references
    updateConditionEncounterReferences(
        conditions, nextCase.getFacilityEncounterId(), currentCase.getFacilityEncounterId());

    // Update the discharge date if necessary
    if (shouldUpdateDate(currentCase.getDischargeDate(), nextCase.getDischargeDate())) {
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
      List<UkbCondition> conditions, String oldEncounterId, String newEncounterId) {
    if (oldEncounterId == null || newEncounterId == null) {
      throw new IllegalArgumentException("Encounter IDs must not be null");
    }
    conditions.stream()
        .filter(condition -> condition.getCaseId().equals(oldEncounterId))
        .forEach(
            condition -> {
              condition.setCaseId(newEncounterId);
              log.info(
                  String.format(
                      "Updated condition %s encounter id from %s to %s",
                      condition.getId(), oldEncounterId, newEncounterId));
            });
  }

  private static Map<String, CoreCaseData> generateCoreCaseDataByEncounterIdMap(
      List<UkbCondition> conditions,
      List<UkbEncounter> facilityContactEncountersFiltered,
      List<UkbPatient> patientsFiltered,
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
      List<UkbCondition> conditions,
      List<UkbEncounter> facilityContactEncountersFiltered,
      List<UkbPatient> patientsFiltered,
      Map<String, List<String>> icdCodesByDiag) {

    icdCodesByDiag.forEach(
        (name, icdCodes) -> {
          Map<String, CoreCaseData> casesByDiagnosis = new HashMap<>();
          Set<String> encounterIds = getEncounterIdsByIcdCodes(conditions, icdCodes);
          Set<UkbEncounter> relevantEncounterIds =
              filterEncountersByIds(facilityContactEncountersFiltered, encounterIds);
          Map<UkbEncounter, UkbPatient> encounterIdPatientMap =
              mapEncountersToPatients(relevantEncounterIds, patientsFiltered);
          encounterIdPatientMap.forEach(
              (encounter, patient) -> addCaseDataIfAbsent(casesByDiagnosis, encounter, patient));
          result.put(name, casesByDiagnosis);
        });
  }

  private static void prepareCasesByDiagnosisType(
      Map<String, CoreCaseData> result,
      List<UkbCondition> conditions,
      List<UkbEncounter> facilityContactEncountersFiltered,
      List<UkbPatient> patientsFiltered,
      Map<String, List<String>> icdCodes) {

    Set<String> encounterIds = getEncounterIdsByIcdCodes(conditions, getIcdCodesAsString(icdCodes));
    Set<UkbEncounter> encountersByDiagnosisType =
        filterEncountersByIds(facilityContactEncountersFiltered, encounterIds);
    Map<UkbEncounter, UkbPatient> encounterIdPatientMap =
        mapEncountersToPatients(encountersByDiagnosisType, patientsFiltered);
    encounterIdPatientMap.forEach(
        (encounter, patient) -> addCaseDataIfAbsent(result, encounter, patient));
  }

  /**
   * Maps encounters to corresponding patients.
   *
   * @param encounters The set of encounters.
   * @param patients The list of patients.
   * @return A map of encounters to patients.
   */
  public static Map<UkbEncounter, UkbPatient> mapEncountersToPatients(
      Set<UkbEncounter> encounters, List<UkbPatient> patients) {
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
      Map<String, CoreCaseData> result, UkbEncounter encounter, UkbPatient patient) {
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
              ageAtAdmissionInMonths));
    }
  }

  public static Map<String, Map<String, CoreCaseData>> createCasesByDiag(
      List<UkbCondition> conditions,
      List<UkbEncounter> facilityContactEncountersFiltered,
      List<UkbPatient> patientsFiltered,
      Map<String, List<String>> icdCodeMap) {

    Map<String, Map<String, CoreCaseData>> result = new HashMap<>();
    processDiagnosisType(
        result, conditions, facilityContactEncountersFiltered, patientsFiltered, icdCodeMap);
    return result;
  }
}
