/*
 *
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 *
 */
package de.ukbonn.mwtek.dashboardlogic.logic;

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.LOINC_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getCaseIdsByObsInterpretation;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getCaseIdsByObsValue;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getCovidObservations;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodesystem;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

/**
 * Class predominantly used for annotation and detection of cases/patients as SARS-CoV-2 positive
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class CoronaLogic {

  /**
   * Marks all encounters in the given list as SARS-CoV-2-positive or /-borderline or /-negative
   * (Exclusion via negative diagnostic code), as far as this information can be determined from the
   * corresponding observation and condition resources.
   *
   * @param listEncounters        List of all {@linkplain UkbEncounter} resources that could be
   *                              flagged.
   * @param listConditions        List of all {@linkplain UkbCondition} resources with U07.* ICD
   *                              codes.
   * @param listLaborObservations List of all observation resources with a SARS-CoV-2 PCR code.
   * @param inputCodeSettings     The configuration of the parameterizable codes such as the
   *                              observation codes or procedure codes.
   * @return The origin encounter list, with flags about the corona status in the extension.
   */
  public static List<UkbEncounter> flagCases(List<UkbEncounter> listEncounters,
      List<UkbCondition> listConditions, List<UkbObservation> listLaborObservations,
      InputCodeSettings inputCodeSettings) {
    Set<String> labSet = getCaseIdsWithPositiveLabObs(listLaborObservations, inputCodeSettings);
    // create maps <DiagnoseCodes, Set<CaseId>>
    HashMap<String, Set<String>> u071CaseId =
        getCaseIdsByDiagReliability(listConditions, CoronaFixedValues.U071.getValue());
    HashMap<String, Set<String>> u072CaseId =
        getCaseIdsByDiagReliability(listConditions, CoronaFixedValues.U072.getValue());

    // Flag the encounters and store them in a map, separated according to their SARS-CoV-2 characteristics
    HashMap<String, Set<UkbEncounter>> flaggedEncounter =
        flagEncounter(listEncounters, labSet, u071CaseId, u072CaseId);

    // 12-days-logic and flagging the encounter if the prerequisites are fulfilled
    ambulantStationaryLogic(flaggedEncounter, listEncounters);

    return listEncounters;
  }

  /**
   * Used to flag the cases who fall under the twelve days Logic. Stationary cases who are negative,
   * but fall under the twelve days logic are considered as positive
   *
   * @param mapFlaggedEncounter Map containing all encounter with SARS-CoV-2-positive flag
   * @param listEncounterAll    List of all encounters (including the non flagged ones) that may now
   *                            be flagged due to the 12-day logic.
   */
  public static void ambulantStationaryLogic(HashMap<String, Set<UkbEncounter>> mapFlaggedEncounter,
      List<UkbEncounter> listEncounterAll) {
    List<UkbEncounter> listEncounterOutpatients = new ArrayList<>();
    Set<String> patientIds = new HashSet<>();

    // Retrieval of the positive outpatient cases
    Set<UkbEncounter> setPositiveOutpatientEncounter =
        mapFlaggedEncounter.get(CoronaFixedValues.POSITIVE.getValue()).parallelStream()
            .filter(CoronaResultFunctionality::isCaseClassOutpatient)
            .collect(Collectors.toSet());
    // Storing the pids and cases in separate collections
    for (UkbEncounter encounter : setPositiveOutpatientEncounter) {
      patientIds.add(encounter.getPatientId());
      listEncounterOutpatients.add(encounter);
    }

    // Search for stationary Cases
    List<UkbEncounter> listEncounterInpatients = listEncounterAll.stream()
        .filter(encounter -> patientIds.contains(encounter.getPatientId()))
        .filter(CoronaResultFunctionality::isCaseClassInpatient).toList();

    for (UkbEncounter outpatientEncounter : listEncounterOutpatients) {

      // Filter the stationary cases who have the same pid as the positive ambulant cases
      List<UkbEncounter> listInpatientEncounter = listEncounterInpatients.stream()
          .filter(x -> x.getPatientId().equals(outpatientEncounter.getPatientId())).toList();

      // Checking 12-days-logic for every stationary encounter
      listInpatientEncounter.forEach(inpatientEncounter -> {

        if (outpatientEncounter.isPeriodStartExistent()
            && inpatientEncounter.isPeriodStartExistent()) {
          Date outpatientStart = outpatientEncounter.getPeriod().getStart();
          Date inpatientStart = inpatientEncounter.getPeriod().getStart();

          // 12-days-logic
          if (outpatientStart != null && inpatientStart != null) {
            if (outpatientStart.before(inpatientStart)) {
              Instant ambuInstant = outpatientStart.toInstant();
              Instant stationInstant = inpatientStart.toInstant();
              ZonedDateTime ambuZone = ambuInstant.atZone(ZoneId.systemDefault());
              ZonedDateTime stationZone = stationInstant.atZone(ZoneId.systemDefault());
              LocalDate begin = ambuZone.toLocalDate();
              LocalDate end = stationZone.toLocalDate();

              long days = ChronoUnit.DAYS.between(begin, end);

              if (days <= CoronaDashboardConstants.daysAfterOutpatientStay) {
                Extension extension = new Extension();
                extension.setValue(new BooleanType(true));
                extension.setUrl(CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue());
                inpatientEncounter.addExtension(extension);

                // to prevent two possible positive flagging
                if (!inpatientEncounter.hasExtension(
                    CoronaFixedValues.POSITIVE_RESULT.getValue())) {
                  log.debug(
                      "Inpatient case " + inpatientEncounter.getCaseId()
                          + " was marked as positive because a previous outpatient case not older than 12 days was positive.");
                  inpatientEncounter.addExtension(markPositive());
                }
              }
            } // if days
          }
        } // if outpatientStart and inpatientStart
      });
    } // for listEncounterOutpatients
  }


  /**
   * Flag the encounters and store them in a map, separated according to their SARS-CoV-2
   * characteristics (positive/borderline/negative)
   *
   * @param listEncounters              List of all Encounters that could be flagged
   * @param setPositiveLabResultCaseIds Set of all case numbers that have a positive SARS-CoV-2
   *                                    laboratory result
   * @param mapCaseIdsU071              Map with case numbers of all cases with U07.1 diagnosis
   * @param mapCaseIdsU072              Map with case numbers of all cases with U07.2 diagnosis
   * @return Map that assigns a list of cases to a flag (positive/borderline/negative)
   */
  // String: positive, borderline, negative
  public static HashMap<String, Set<UkbEncounter>> flagEncounter(List<UkbEncounter> listEncounters,
      Set<String> setPositiveLabResultCaseIds, HashMap<String, Set<String>> mapCaseIdsU071,
      HashMap<String, Set<String>> mapCaseIdsU072) {

    HashMap<String, Set<UkbEncounter>> mapResult = new HashMap<>();
    Set<UkbEncounter> setBorderlineEncounter = new HashSet<>();
    Set<UkbEncounter> setNegativeEncounter = new HashSet<>();

    Set<UkbEncounter> setPositiveEncounter =
        flagEncounterByPositiveLabResult(listEncounters, setPositiveLabResultCaseIds);

    // create case id sets for the specific cases
    Set<String> setU072CaseId = getCaseIdsByDiagReliability(mapCaseIdsU072,
        CoronaFixedValues.DIAG_RELIABILITY_MISSING.getValue()); // U072
    // borderline
    Set<String> setU072ACaseId = getCaseIdsByDiagReliability(mapCaseIdsU072,
        CoronaFixedValues.DIAG_RELIABILITY_A.getValue()); // U072A negative
    Set<String> setU072VCaseId = getCaseIdsByDiagReliability(mapCaseIdsU072,
        CoronaFixedValues.DIAG_RELIABILITY_V.getValue()); // U072V
    Set<String> setU072GCaseId = getCaseIdsByDiagReliability(mapCaseIdsU072,
        CoronaFixedValues.DIAG_RELIABILITY_G.getValue()); // U072G
    Set<String> setU072ZCaseId = getCaseIdsByDiagReliability(mapCaseIdsU072,
        CoronaFixedValues.DIAG_RELIABILITY_Z.getValue()); // U072Z
    Set<String> setU071CaseId = getCaseIdsByDiagReliability(mapCaseIdsU071,
        CoronaFixedValues.DIAG_RELIABILITY_MISSING.getValue()); // 071
    // positive
    Set<String> setU071GCaseId = getCaseIdsByDiagReliability(mapCaseIdsU071,
        CoronaFixedValues.DIAG_RELIABILITY_G.getValue()); // U071G = "Gesichert" is positive
    Set<String> setU071VCaseId = getCaseIdsByDiagReliability(mapCaseIdsU071,
        CoronaFixedValues.DIAG_RELIABILITY_V.getValue()); // U071V = "Verdacht"
    Set<String> setU071ACaseId = getCaseIdsByDiagReliability(mapCaseIdsU071,
        CoronaFixedValues.DIAG_RELIABILITY_A.getValue()); // U071A negative
    Set<String> setU071ZCaseId = getCaseIdsByDiagReliability(mapCaseIdsU071,
        CoronaFixedValues.DIAG_RELIABILITY_Z.getValue()); // U071Z

    for (UkbEncounter encounter : listEncounters) {
      // Positive flagging
      boolean hasEncounterU071 =
          addExtensionToEncounterIfSetContainsCaseId(encounter, setPositiveEncounter,
              setU071CaseId, markPositive());
      boolean hasEncounterU071G =
          addExtensionToEncounterIfSetContainsCaseId(encounter, setPositiveEncounter,
              setU071GCaseId, markPositive());
      boolean hasEncounterU071Z =
          addExtensionToEncounterIfSetContainsCaseId(encounter, setPositiveEncounter,
              setU071ZCaseId, markPositive());
      if (!hasEncounterU071 && !hasEncounterU071G && !hasEncounterU071Z) {
        // Borderline flagging
        boolean hasEncounterU072 =
            addExtensionToEncounterIfSetContainsCaseId(encounter, setBorderlineEncounter,
                setU072CaseId, markBorderline());
        boolean hasEncounterU072G =
            addExtensionToEncounterIfSetContainsCaseId(encounter, setBorderlineEncounter,
                setU072GCaseId, markBorderline());
        boolean hasEncounterU072V =
            addExtensionToEncounterIfSetContainsCaseId(encounter, setBorderlineEncounter,
                setU072VCaseId, markBorderline());
        boolean hasEncounterU072Z =
            addExtensionToEncounterIfSetContainsCaseId(encounter, setBorderlineEncounter,
                setU072ZCaseId, markBorderline());
        boolean hasEncounterU071V =
            addExtensionToEncounterIfSetContainsCaseId(encounter, setBorderlineEncounter,
                setU071VCaseId, markBorderline());
        if (!hasEncounterU072 && !hasEncounterU071V && !hasEncounterU072G && !hasEncounterU072V
            && !hasEncounterU072Z) {
          // Negative flagging, simply used to flag the encounter
          addExtensionToEncounterIfSetContainsCaseId(encounter, setNegativeEncounter,
              setU072ACaseId, markNegative());
          addExtensionToEncounterIfSetContainsCaseId(encounter, setNegativeEncounter,
              setU071ACaseId, markNegative());
        }
      }
    }

    // adding the results to a map
    mapResult.put(CoronaFixedValues.POSITIVE.getValue(), setPositiveEncounter);
    mapResult.put(CoronaFixedValues.BORDERLINE.getValue(), setBorderlineEncounter);
    mapResult.put(CoronaFixedValues.NEGATIVE.getValue(), setNegativeEncounter);
    return mapResult;
  }

  /**
   * Used to flag a given {@linkplain UkbEncounter encounter resource} If the case id is not
   * contained in the specified given case id set (e.g. all case ids with a certain diagnostic
   * certainty on an U07.1 code), false is returned.
   *
   * @param encounter    A given encounter resource that may be flagged
   * @param setEncounter A set of encounters that meet a certain criterion and to which the given
   *                     encounter should be added if the extension is set.
   * @param setCaseIds   Set of case numbers that meet a certain criterion
   * @param flag         Extension flag to be set
   * @return true/false if the extension was set to the given encounter
   */
  public static boolean addExtensionToEncounterIfSetContainsCaseId(UkbEncounter encounter,
      Set<UkbEncounter> setEncounter, Set<String> setCaseIds, Extension flag) {
    if (setCaseIds != null) {
      if (setCaseIds.contains(encounter.getId())) {
        encounter.getExtension().add(flag);
        setEncounter.add(encounter);
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Creation of case id sets for the specific diagnosis codes with a given diagnostic reliability
   *
   * @param mapCaseIdU07                Map of all case numbers for a specific U07.* ICD code
   * @param icdDiagnosisReliabilityCode ICD diagnosis confidence code (see
   *                                    {@link CoronaFixedValues#DIAGNOSIS_SECURITY_BORDERLINE} for
   *                                    example)
   * @return Set of case numbers that have a specific diagnosis code and a given diagnostic
   * certainty
   */
  public static Set<String> getCaseIdsByDiagReliability(HashMap<String, Set<String>> mapCaseIdU07,
      String icdDiagnosisReliabilityCode) {
    Set<String> setResult = new HashSet<>();
    try {
      if (mapCaseIdU07.containsKey(icdDiagnosisReliabilityCode)) {
        if (mapCaseIdU07.get(icdDiagnosisReliabilityCode) != null || !mapCaseIdU07.get(
            icdDiagnosisReliabilityCode).isEmpty()) {
          setResult = mapCaseIdU07.get(icdDiagnosisReliabilityCode);
        }
      }
    } catch (NullPointerException ex) {
      ex.printStackTrace();
    }
    return setResult;
  }

  /**
   * Creation of an {@linkplain Extension} which is used as flag for SARS-CoV-2 borderline cases
   *
   * @return SARS-CoV-2 borderline extension
   */
  public static Extension markBorderline() {
    Extension conditionU072 = new Extension();
    conditionU072.setUrl(CoronaFixedValues.BORDERLINE_RESULT.getValue());
    conditionU072.setValue(new StringType(CoronaFixedValues.BORDERLINE.getValue()));
    return conditionU072;
  }

  /**
   * Creation of an {@linkplain Extension} which is used as flag for SARS-CoV-2 positive cases
   *
   * @return SARS-CoV-2 positive extension
   */
  public static Extension markPositive() {
    Extension lab = new Extension();
    lab.setUrl(CoronaFixedValues.POSITIVE_RESULT.getValue());
    lab.setValue(new BooleanType(true));
    return lab;
  }

  /**
   * Creation of an {@linkplain Extension} which is used as flag for SARS-CoV-2 negative cases
   *
   * @return SARS-CoV-2 positive Extension
   */
  public static Extension markNegative() {
    Extension lab = new Extension();
    lab.setUrl(CoronaFixedValues.NEGATIVE_RESULT.getValue());
    lab.setValue(new BooleanType(false));
    return lab;
  }

  /**
   * Creation of a map that assigns the respective case numbers for a given ICD code to a diagnosis
   * certainty.
   *
   * @param listConditions List of all condition resources with U07.* ICD codes
   * @param icdCode        The ICD diagnosis code to be checked (e.g. U07.1)
   * @return Map with diagnosis code and a list of case numbers per diagnosis code
   */
  public static HashMap<String, Set<String>> getCaseIdsByDiagReliability(
      List<UkbCondition> listConditions, String icdCode) {

    // Create a map that assigns a list of case numbers to an ICD code.
    HashMap<String, Set<String>> mapResultDiagnoseCaseIds = new HashMap<>();

    for (UkbCondition condition : listConditions) {
      // Check if the condition contains an ICD-10-GM code system and if it contains the given icd code
      if (condition.hasCode() && condition.getCode()
          .hasCoding(CoronaFixedValues.ICD_SYSTEM.getValue(), icdCode)) {

        condition.getCode().getCoding().forEach(coding -> {

          if (coding.getSystem().equals(CoronaFixedValues.ICD_SYSTEM.getValue())) {

            Set<String> caseIds = new HashSet<>();
            String icdDiagReliabilityCode = null;

            // Detect the diagnosis reliability which is part of an extension
            if (coding.hasExtension(CoronaFixedValues.ICD_DIAG_RELIABILITY_EXT_URL.getValue())) {
              Extension extDiagReliability = coding.getExtensionByUrl(
                  CoronaFixedValues.ICD_DIAG_RELIABILITY_EXT_URL.getValue());
              if (extDiagReliability.getValue() instanceof Coding codingExtDiagReliability) {
                // The Coding got a fixed url as system
                if (codingExtDiagReliability.getSystem()
                    .equals(CoronaFixedValues.ICD_DIAG_RELIABILITY_CODING_SYSTEM.getValue())
                    && codingExtDiagReliability.hasCode()) {
                  // check if ICD diagnosis reliability code (usually a letter) is available
                  icdDiagReliabilityCode = codingExtDiagReliability.getCode();
                } // if codingExtDiagReliability.getSystem()
              } // if extDiagReliability.getValue()
            } // if coding.hasExtension
            if (icdDiagReliabilityCode == null) {
              icdDiagReliabilityCode = CoronaFixedValues.DIAG_RELIABILITY_MISSING.getValue();
            }

            String caseId = condition.getCaseId();
            if (caseId != null) {
              caseIds.add(caseId);
              // If there is already an entry for the given reliability cody exists -> add the case id to the existing set
              if (mapResultDiagnoseCaseIds.containsKey(icdDiagReliabilityCode)) {
                mapResultDiagnoseCaseIds.computeIfPresent(icdDiagReliabilityCode, (k, v) -> {
                  v.add(caseId);
                  return v;
                });
              } else {
                mapResultDiagnoseCaseIds.put(icdDiagReliabilityCode, caseIds);
              }
            } else {
              log.debug("No case id found in the condition resource with ID: " + condition.getId());
            }
          }
        });
      }
    }

    return mapResultDiagnoseCaseIds;
  }

  /**
   * Creation of a set of caseIds that have a SARS-CoV-2 positive lab result.
   *
   * @param labObservations   A list with {@link UkbObservation observation resources}.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return set of case ids that have a SARS-CoV-2 positive lab result
   */
  public static Set<String> getCaseIdsWithPositiveLabObs(List<UkbObservation> labObservations,
      InputCodeSettings inputCodeSettings) {
    Set<String> positiveCaseIds;
    Set<UkbObservation> covidObservations = getCovidObservations(labObservations,
        inputCodeSettings);

    // For logging purposes we look for observations that contain a covid coding but don't have any values
    Set<String> covidObservationsWithoutResult = labObservations.parallelStream()
        .filter(x -> x.hasCode() && x.getCode().hasCoding())
        .filter(x -> isCodeInCodesystem(x.getCode().getCoding(),
            inputCodeSettings.getObservationPcrLoincCodes(), LOINC_SYSTEM))
        .filter(x -> !(x.hasValueCodeableConcept() || x.hasInterpretation())).map(Resource::getId)
        .collect(Collectors.toSet());

    covidObservationsWithoutResult.forEach(x -> {
      log.warn("The observation resource with id " + x
          + " that describes a covid pcr finding doesn't contain a valueCodeableConcept or an expected interpretation coding.");
    });

    // Adding the ones with positive value and then the ones with positive interpretation code.
    positiveCaseIds = getCaseIdsByObsValue(covidObservations, POSITIVE);
    positiveCaseIds.addAll(getCaseIdsByObsInterpretation(covidObservations, POSITIVE));

    return positiveCaseIds;
  }

  /**
   * Create a set of encounters extended with a SARS-CoV-2 positive extension if positive lab
   * results were found on the encounters.
   *
   * @param listEncounters              List of all Encounters that could be flagged
   * @param setPositiveLabResultCaseIds Set with case numbers of cases in which a positive
   *                                    SARS-CoV-2 laboratory finding could be found
   * @return Set of encounters extended by SARS-CoV-2 positive extensions
   */
  public static Set<UkbEncounter> flagEncounterByPositiveLabResult(
      List<UkbEncounter> listEncounters, Set<String> setPositiveLabResultCaseIds) {
    Set<UkbEncounter> setResult = new HashSet<>();
    try {
      // flag all encounter
      for (UkbEncounter encounter : listEncounters) {
        if (setPositiveLabResultCaseIds.contains(encounter.getId())) {
          encounter.addExtension(markPositive());
          setResult.add(encounter);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return setResult;
  }
}
