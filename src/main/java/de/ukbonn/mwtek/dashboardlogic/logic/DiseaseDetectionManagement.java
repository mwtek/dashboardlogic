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
package de.ukbonn.mwtek.dashboardlogic.logic;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.ICD_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.U071;
import static de.ukbonn.mwtek.dashboardlogic.enums.FlaggingExtension.POSITIVE_EXTENSION;
import static de.ukbonn.mwtek.dashboardlogic.enums.FlaggingExtension.TWELVE_DAYS_EXTENSION;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getCaseIdsByObsInterpretation;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getCaseIdsByObsValue;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByContext;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.LOINC;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodesystem;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirTools.flagEncountersByIdentifierValue;

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools;
import de.ukbonn.mwtek.utilities.fhir.misc.FhirTools;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Resource;

/**
 * Class predominantly used for annotation and detection of cases/patients as SARS-CoV-2 / influenza
 * positive
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class DiseaseDetectionManagement {

  /**
   * Marks all encounters in the given list as SARS-CoV-2-positive or /-borderline or /-negative
   * (Exclusion via negative diagnostic code), as far as this information can be determined from the
   * corresponding observation and condition resources.
   *
   * @param ukbEncounters List of all {@linkplain UkbEncounter} resources that could be flagged.
   * @param ukbConditions List of all {@linkplain UkbCondition} resources with U07.* ICD codes.
   * @param ukbObservations List of all observation resources with a disease-related PCR code.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *     codes or procedure codes.
   */
  public static void flagEncounters(
      List<UkbEncounter> ukbEncounters,
      List<UkbCondition> ukbConditions,
      List<UkbObservation> ukbObservations,
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      DataItemContext dataItemContext) {
    log.debug("started flagCases");
    Instant startTimer = TimerTools.startTimer();
    // The encounter ids of the disease-positive encounters
    Set<String> positiveEncounterIds = new HashSet<>();
    Set<UkbEncounter> flaggedEncounters;
    // create maps <DiagnoseCodes, Set<CaseId>>
    switch (dataItemContext) {
      case COVID -> {
        positiveEncounterIds =
            getEncounterIdsWithPositiveLabObs(
                ukbObservations, inputCodeSettings, dataItemContext, qualitativeLabCodesSettings);
        Set<String> caseIdsWithIcdCode =
            FhirConditionTools.getEncounterIdsByIcdCodes(ukbConditions, U071.getValue());
        positiveEncounterIds.addAll(caseIdsWithIcdCode);
      }
      case INFLUENZA -> {
        positiveEncounterIds =
            getEncounterIdsWithPositiveLabObs(
                ukbObservations, inputCodeSettings, dataItemContext, qualitativeLabCodesSettings);
        Set<String> positiveIcdCodes = Set.of("J10.0", "J10.1", "J10.8", "J09");
        // The suspected handling is part of the data set description but got no impact on any
        // data item at the moment
        Set<String> encounterIdsWithPositiveIcdCode =
            FhirConditionTools.getEncounterIdsByIcdCodes(ukbConditions, positiveIcdCodes);
        positiveEncounterIds.addAll(encounterIdsWithPositiveIcdCode);
      }
    }
    // Identify the facility contact <-> supply contact connection via 'encounter.identifier
    // .aufnahmenummer' and flag them as disease-positive if needed
    Set<String> positiveVisitNumbers =
        FhirTools.getVisitNumberIdentifiers(positiveEncounterIds, ukbEncounters);
    flaggedEncounters =
        flagEncountersByIdentifierValue(positiveVisitNumbers, ukbEncounters, POSITIVE_EXTENSION);

    // 12-days-logic and flagging the encounter if the prerequisites are fulfilled
    detectPositiveInpatientEncountersByPreviousEncounters(flaggedEncounters, ukbEncounters);
    TimerTools.stopTimerAndLog(startTimer, "finished flagCases");
  }

  /**
   * Used to flag the cases who fall under the twelve-day Logic. Stationary cases who are negative,
   * but fall under the twelve-day logic are considered as positive
   *
   * @param flaggedEncounter Set containing (minimum) all encounters with a positive disease marker
   * @param encountersAll List of all encounters (including the non-flagged ones) that may now be
   *     flagged due to the 12-day logic.
   */
  public static void detectPositiveInpatientEncountersByPreviousEncounters(
      Set<UkbEncounter> flaggedEncounter, List<UkbEncounter> encountersAll) {

    // Start logging
    log.debug("started detectPositiveInpatientEncountersByPreviousEncounters");
    Instant startTimer = TimerTools.startTimer();

    Set<UkbEncounter> positiveOutpatientEncounter =
        flaggedEncounter.parallelStream()
            .filter(UkbEncounter::isCaseClassOutpatient)
            .collect(Collectors.toSet());

    // Extract patient IDs from flagged encounters marked as outpatient
    Set<String> positiveOutpatientPatientIds =
        positiveOutpatientEncounter.stream()
            .map(UkbEncounter::getPatientId)
            .collect(Collectors.toSet());

    // Filter encounters for inpatients using positive outpatient patient IDs
    Map<String, List<UkbEncounter>> inpatientEncountersByPatientId =
        encountersAll.stream()
            .filter(encounter -> positiveOutpatientPatientIds.contains(encounter.getPatientId()))
            .filter(UkbEncounter::isCaseClassInpatient)
            .collect(Collectors.groupingBy(UkbEncounter::getPatientId));

    // Loop through flagged encounters
    for (UkbEncounter outpatientEncounter : positiveOutpatientEncounter) {
      // Skip encounters without start dates
      if (!outpatientEncounter.isPeriodStartExistent()) {
        continue;
      }
      // Get start date of outpatient encounter
      Date outpatientStart = outpatientEncounter.getPeriod().getStart();

      // Get inpatient encounters for the same patient ID
      List<UkbEncounter> inpatientEncounters =
          inpatientEncountersByPatientId.get(outpatientEncounter.getPatientId());
      if (inpatientEncounters == null) {
        continue; // No inpatient encounters for this patient
      }

      // Iterate through inpatient encounters for the same patient ID
      for (UkbEncounter inpatientEncounter : inpatientEncounters) {
        // Skip encounters without start dates
        if (!inpatientEncounter.isPeriodStartExistent()) {
          continue;
        }

        // Get start date of inpatient encounter
        Date inpatientStart = inpatientEncounter.getPeriod().getStart();

        // Check if outpatient encounter is before inpatient encounter
        if (outpatientStart.before(inpatientStart)) {
          // Calculate the number of days between outpatient and inpatient encounters
          long days =
              ChronoUnit.DAYS.between(
                  outpatientStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                  inpatientStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

          // Check if days fall within the defined period
          if (days <= NumDashboardConstants.DAYS_AFTER_OUTPATIENT_STAY) {
            // Add an extension to inpatient encounter
            inpatientEncounter.addExtension(TWELVE_DAYS_EXTENSION);
            // Log if inpatient encounter is marked as positive
            if (!inpatientEncounter.hasExtension(POSITIVE_RESULT.getValue())) {
              log.debug(
                  "The encounter with id "
                      + inpatientEncounter.getId()
                      + " was marked as positive because a previous outpatient case not older"
                      + " than 12 days was positive.");
            }
          }
        }
      }
    }
    // Stop timer and log
    TimerTools.stopTimerAndLog(
        startTimer, "finished detectPositiveInpatientEncountersByPreviousEncounters");
  }

  /**
   * Creation of a map that assigns the respective case numbers for a given ICD code to a diagnosis
   * certainty.
   *
   * @param listConditions List of all condition resources with U07.* ICD codes
   * @param icdCode The ICD diagnosis code to be checked (e.g. U07.1)
   * @return Map with diagnosis code and a list of case numbers per diagnosis code
   */
  @Deprecated
  public static Map<String, Set<String>> getCaseIdsByDiagReliability(
      List<UkbCondition> listConditions, String icdCode) {

    // Create a map that assigns a list of case numbers to an ICD code.
    Map<String, Set<String>> mapResultDiagnoseCaseIds = new HashMap<>();

    for (UkbCondition condition : listConditions) {
      // Check if the condition contains an ICD-10-GM code system and if it contains the given
      // icd code
      if (condition.hasCode() && condition.getCode().hasCoding(ICD_SYSTEM.getValue(), icdCode)) {
        condition
            .getCode()
            .getCoding()
            .forEach(
                coding -> {
                  if (coding.getSystem().equals(ICD_SYSTEM.getValue())) {
                    Set<String> caseIds = new HashSet<>();
                    String icdDiagReliabilityCode = null;
                    // Detect the diagnosis reliability which is part of an extension
                    if (coding.hasExtension(
                        DashboardLogicFixedValues.ICD_DIAG_RELIABILITY_EXT_URL.getValue())) {
                      Extension extDiagReliability =
                          coding.getExtensionByUrl(
                              DashboardLogicFixedValues.ICD_DIAG_RELIABILITY_EXT_URL.getValue());
                      if (extDiagReliability.getValue()
                          instanceof Coding codingExtDiagReliability) {
                        // The Coding got a fixed url as a system
                        if (codingExtDiagReliability
                                .getSystem()
                                .equals(
                                    DashboardLogicFixedValues.ICD_DIAG_RELIABILITY_CODING_SYSTEM
                                        .getValue())
                            && codingExtDiagReliability.hasCode()) {
                          // check if ICD diagnosis reliability code (usually a letter) is available
                          icdDiagReliabilityCode = codingExtDiagReliability.getCode();
                        } // if codingExtDiagReliability.getSystem()
                      } // if extDiagReliability.getValue()
                    } // if coding.hasExtension
                    if (icdDiagReliabilityCode == null) {
                      icdDiagReliabilityCode =
                          DashboardLogicFixedValues.DIAG_RELIABILITY_MISSING.getValue();
                    }

                    String caseId = condition.getCaseId();
                    if (caseId != null) {
                      caseIds.add(caseId);
                      // If there is already an entry for the given reliability code exists -> add
                      // the
                      // case id to the existing set
                      if (mapResultDiagnoseCaseIds.containsKey(icdDiagReliabilityCode)) {
                        mapResultDiagnoseCaseIds.computeIfPresent(
                            icdDiagReliabilityCode,
                            (k, v) -> {
                              v.add(caseId);
                              return v;
                            });
                      } else {
                        mapResultDiagnoseCaseIds.put(icdDiagReliabilityCode, caseIds);
                      }
                    } else {
                      log.warn(
                          "No case id found in the condition resource with ID: "
                              + condition.getId());
                    }
                  }
                });
      }
    }

    return mapResultDiagnoseCaseIds;
  }

  /**
   * Creation of a set of caseIds that have a disease positive lab result.
   *
   * @param labObservations A list with {@link UkbObservation observation resources}.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *     codes or procedure codes.
   * @return set of case ids that have a disease positive lab result
   */
  public static Set<String> getEncounterIdsWithPositiveLabObs(
      List<UkbObservation> labObservations,
      InputCodeSettings inputCodeSettings,
      DataItemContext dataItemContext,
      QualitativeLabCodesSettings qualitativeLabCodesSettings) {
    Set<String> positiveEncounterIds;
    Set<UkbObservation> positiveObservations =
        getObservationsByContext(labObservations, inputCodeSettings, dataItemContext);
    // For logging purposes, we look for observations that contain a loinc coding,
    // but don't have any values
    List<String> loincCodes =
        switch (dataItemContext) {
          case COVID -> inputCodeSettings.getCovidObservationPcrLoincCodes();
          case INFLUENZA -> inputCodeSettings.getInfluenzaObservationPcrLoincCodes();
          case KIDS_RADAR -> null;
          case KIDS_RADAR_KJP -> null;
          case KIDS_RADAR_RSV -> null;
        };

    Set<String> observationsWithoutResult =
        labObservations.parallelStream()
            .filter(x -> x.hasCode() && x.getCode().hasCoding())
            .filter(x -> isCodeInCodesystem(x.getCode().getCoding(), loincCodes, LOINC))
            .filter(x -> !(x.hasValueCodeableConcept() || x.hasInterpretation()))
            .map(Resource::getId)
            .collect(Collectors.toSet());

    observationsWithoutResult.forEach(
        x ->
            log.warn(
                "The observation resource with id {} that describes a covid/influenza pcr finding doesn't contain a valueCodeableConcept or an expected interpretation coding.",
                x));

    // Adding the ones with positive value and then the ones with positive interpretation code.
    positiveEncounterIds =
        getCaseIdsByObsValue(positiveObservations, POSITIVE, qualitativeLabCodesSettings);
    positiveEncounterIds.addAll(getCaseIdsByObsInterpretation(positiveObservations, POSITIVE));

    return positiveEncounterIds;
  }
}
