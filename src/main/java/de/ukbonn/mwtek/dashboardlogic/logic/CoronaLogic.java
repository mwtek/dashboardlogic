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

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import lombok.extern.slf4j.Slf4j;

/**
 * Class predominantly used for annotation and detection of cases/patients as c19 positive
 * 
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 *
 */
@Slf4j
public class CoronaLogic {

  /**
   * Marks all encounters in the given list as C19-positive or /-borderline or /-negative (Exclusion
   * via negative diagnostic code), as far as this information can be determined from the
   * corresponding observation and condition resources.
   * 
   * @param listEncounters List of all {@linkplain UkbEncounter} resources that could be flagged
   * @param listConditions List of all {@linkplain UkbCondition} resources with U07.* ICD codes
   * @param listLaborObservations List of all observation resources with a c19 PCR code
   * @return The origin encounter list, with flags about the corona status in the extension
   */
  public static List<UkbEncounter> flagCases(List<UkbEncounter> listEncounters,
      List<UkbCondition> listConditions, List<UkbObservation> listLaborObservations) {
    Set<String> labSet = getCaseIdsWithPositiveLabObs(listLaborObservations);
    // create maps <DiagnoseCodes, Set<CaseId>>
    HashMap<String, Set<String>> u071CaseId =
        getCaseIdsByDiagnosisCode(listConditions, CoronaFixedValues.U071.getValue());
    HashMap<String, Set<String>> u072CaseId =
        getCaseIdsByDiagnosisCode(listConditions, CoronaFixedValues.U072.getValue());

    // Flag the encounters and store them in a map, separated according to their C19 characteristics
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
   * @param mapFlaggedEncounter Map containing all encounter with C19-positive flag
   * @param listEncounterAll List of all encounters (including the unflagged ones) that may now be
   *          flagged due to the 12-day logic.
   */
  public static void ambulantStationaryLogic(HashMap<String, Set<UkbEncounter>> mapFlaggedEncounter,
      List<UkbEncounter> listEncounterAll) {
    List<UkbEncounter> listResults = new ArrayList<>();
    List<UkbEncounter> listAmbulant = new ArrayList<>();
    Set<String> setPatientIds = new HashSet<>();
    Set<UkbEncounter> setPositiveEncounter =
        mapFlaggedEncounter.get(CoronaFixedValues.POSITIVE.getValue());

    // Search for positive ambulant cases
    for (UkbEncounter encounter : setPositiveEncounter) {
      if (CoronaResultFunctionality.isCaseClassOutpatient(encounter)) {
        if (encounter.hasExtension() && ((BooleanType) encounter.getExtension()
            .get(0)
            .getValue()).getValue()) {
          setPatientIds.add(encounter.getPatientId());
          listAmbulant.add(encounter);
        }
      }
    }

    // Search for stationary Cases
    List<UkbEncounter> listEncounterInpatients = listEncounterAll.stream()
        .filter(encounter -> setPatientIds.contains(encounter.getPatientId()))
        .filter(x -> CoronaResultFunctionality.isCaseClassInpatient(x))
        .collect(Collectors.toList());

    for (UkbEncounter ambuEncounter : listAmbulant) {


      // Filter the stationary cases who have the same pid as the positive ambulant cases
      List<UkbEncounter> listStationaryCases = listEncounterInpatients.stream()
          .filter(x -> x.getPatientId()
              .equals(ambuEncounter.getPatientId()))
          .collect(Collectors.toList());

      // check twelve days logic for every stationary encounter
      listStationaryCases.forEach(encounter -> {
        Date ambu = null;
        Date station = null;

        if (ambuEncounter.isPeriodStartExistent() && encounter.isPeriodStartExistent()) {
          ambu = ambuEncounter.getPeriod()
              .getStart();
          station = encounter.getPeriod()
              .getStart();

          // twelve Days logic
          if (ambu != null && station != null)
            if (ambu.before(station)) {
              Instant ambuInstant = ambu.toInstant();
              Instant stationInstant = station.toInstant();
              ZonedDateTime ambuZone = ambuInstant.atZone(ZoneId.systemDefault());
              ZonedDateTime stationZone = stationInstant.atZone(ZoneId.systemDefault());
              LocalDate begin = ambuZone.toLocalDate();
              LocalDate end = stationZone.toLocalDate();

              long days = ChronoUnit.DAYS.between(begin, end);

              if (days <= CoronaDashboardConstants.daysAfterOutpatientStay) {
                Extension extension = new Extension();
                extension.setValue(new StringType(CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue()));
                extension.setUrl(CoronaFixedValues.TWELVE_DAYS_LOGIC.getValue());
                encounter.addExtension(extension);

                // to prevent two possible positive flaggings
                if (!encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())) {
                  encounter.addExtension(markPositive());
                }
                listResults.add(encounter);
              }
            } // if days
        } // if ambu and station
      });
    } // for listAmbulant
  }


  /**
   * Flag the encounters and store them in a map, separated according to their C19 characteristics
   * (positive/borderline/negative)
   * 
   * @param listEncounters List of all Encounters that could be flagged
   * @param setPositiveLabResultCaseIds Set of all case numbers that have a positive C19 laboratory
   *          result
   * @param mapCaseIdsU071 Map with case numbers of all cases with U07.1 diagnosis
   * @param mapCaseIdsU072 Map with case numbers of all cases with U07.2 diagnosis
   * @return Map that assigns a list of cases to a flag (positive/borderline/negative)
   */
  // String: positive, borderline, negative
  public static HashMap<String, Set<UkbEncounter>> flagEncounter(List<UkbEncounter> listEncounters,
          Set<String> setPositiveLabResultCaseIds,
      HashMap<String, Set<String>> mapCaseIdsU071, HashMap<String, Set<String>> mapCaseIdsU072) {

    HashMap<String, Set<UkbEncounter>> mapResult = new HashMap<>();
    Set<UkbEncounter> setBorderlineEncounter = new HashSet<>();
    Set<UkbEncounter> setNegativeEncounter = new HashSet<>();

    Set<UkbEncounter> setPositiveEncounter =
        flagEncounterByPositiveLabResult(listEncounters, setPositiveLabResultCaseIds);

    // create Case Id Sets for the specific cases
    Set<String> setU072CaseId =
        getCaseIdsByDiagnosisCode(mapCaseIdsU072, CoronaFixedValues.DIAGNOSECODE_EMPTY.getValue()); // U072
    // grenzwertig
    Set<String> setU072ACaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU072,
        CoronaFixedValues.DIAGNOSIS_SECURITY_NEGATIVE.get(0)); // U072A negative
    Set<String> setU072VCaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU072,
        CoronaFixedValues.DIAGNOSIS_SECURITY_BORDERLINE.get(0)); // U072V
    Set<String> setU072GCaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU072,
        CoronaFixedValues.DIAGNOSIS_SECURITY_BORDERLINE.get(1)); // U072G
    Set<String> setU072ZCaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU072,
        CoronaFixedValues.DIAGNOSIS_SECURITY_BORDERLINE.get(2)); // U072Z
    Set<String> setU071CaseId =
        getCaseIdsByDiagnosisCode(mapCaseIdsU071, CoronaFixedValues.DIAGNOSECODE_EMPTY.getValue()); // 071
    // Positive
    Set<String> setU071GCaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU071,
        CoronaFixedValues.DIAGNOSIS_SECURITY_BORDERLINE.get(1)); // U071G is positive but uses the
                                                                 // letter "g" from the Borderline
                                                                 // List
    Set<String> setU071VCaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU071,
        CoronaFixedValues.DIAGNOSIS_SECURITY_BORDERLINE.get(0)); // U071V grenzwertig
    Set<String> setU071ACaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU071,
        CoronaFixedValues.DIAGNOSIS_SECURITY_NEGATIVE.get(0)); // U071A negative
    Set<String> setU071ZCaseId = getCaseIdsByDiagnosisCode(mapCaseIdsU071,
        CoronaFixedValues.DIAGNOSIS_SECURITY_BORDERLINE.get(2)); // U071Z

    for (UkbEncounter encounter : listEncounters) {
      // Positive flagging
      boolean hasEncounterU071 = addExtensionToEncounterIfSetContainsCaseId(encounter,
          setPositiveEncounter, setU071CaseId, markPositive());
      boolean hasEncounterU071G = addExtensionToEncounterIfSetContainsCaseId(encounter,
          setPositiveEncounter, setU071GCaseId, markPositive());
      boolean hasEncounterU071Z = addExtensionToEncounterIfSetContainsCaseId(encounter,
          setPositiveEncounter, setU071ZCaseId, markPositive());
      if (!hasEncounterU071 && !hasEncounterU071G && !hasEncounterU071Z) {
        // Borderline flagging
        boolean hasEncounterU072 = addExtensionToEncounterIfSetContainsCaseId(encounter,
            setBorderlineEncounter, setU072CaseId, markBorderline());
        boolean hasEncounterU072G = addExtensionToEncounterIfSetContainsCaseId(encounter,
            setBorderlineEncounter, setU072GCaseId, markBorderline());
        boolean hasEncounterU072V = addExtensionToEncounterIfSetContainsCaseId(encounter,
            setBorderlineEncounter, setU072VCaseId, markBorderline());
        boolean hasEncounterU072Z = addExtensionToEncounterIfSetContainsCaseId(encounter,
            setBorderlineEncounter, setU072ZCaseId, markBorderline());
        boolean hasEncounterU071V = addExtensionToEncounterIfSetContainsCaseId(encounter,
            setBorderlineEncounter, setU071VCaseId, markBorderline());
        if (!hasEncounterU072 && !hasEncounterU071V && !hasEncounterU072G && !hasEncounterU072V
            && !hasEncounterU072Z) {
          // Negative flagging, simply used to flag the encounter
          // TODO check if this part is rly needed anymore
          boolean hasEncounterU072ACaseId = addExtensionToEncounterIfSetContainsCaseId(encounter,
              setNegativeEncounter, setU072ACaseId, markNegative());
          boolean hasEncounterU071A = addExtensionToEncounterIfSetContainsCaseId(encounter,
              setNegativeEncounter, setU071ACaseId, markNegative());
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
   * Used to flag a given {@linkplain UkbEncounter encounter resource} If the caseid is not
   * contained in the specified given caseid set (e.g. all caseids with a certain diagnostic
   * certainty on an U07.1 code), false is returned.
   * 
   * @param encounter An given encounter resource that may be flagged
   * @param setEncounter A set of encounters that meet a certain criterion and to which the given
   *          encounter should be added if the extension is set.
   * @param setCaseIds Set of case numbers that meet a certain criterion
   * @param flag Extension flag to be set
   * @return true/false if the extension was set to the given encounter
   */
  public static boolean addExtensionToEncounterIfSetContainsCaseId(UkbEncounter encounter,
      Set<UkbEncounter> setEncounter, Set<String> setCaseIds, Extension flag) {
    if (setCaseIds != null) {
      if (setCaseIds.contains(encounter.getId())) {
        encounter.getExtension()
            .add(flag);
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
   * @param mapCaseIdU07 Map of all case numbers for a specific U07.* ICD code
   * @param icdDiagnosisReliabilityCode ICD diagnosis confidence code (see
   *          {@link CoronaFixedValues#DIAGNOSIS_SECURITY_BORDERLINE} for example)
   * @return Set of case numbers that have a specific diagnosis code and a given diagnostic
   *         certainty
   */
  public static Set<String> getCaseIdsByDiagnosisCode(HashMap<String, Set<String>> mapCaseIdU07,
      String icdDiagnosisReliabilityCode) {
    Set<String> setResult = new HashSet<>();
    try {
      if (mapCaseIdU07.containsKey(icdDiagnosisReliabilityCode)) {
        if (mapCaseIdU07.get(icdDiagnosisReliabilityCode) != null
            || !mapCaseIdU07.get(icdDiagnosisReliabilityCode)
                .isEmpty()) {
          setResult = mapCaseIdU07.get(icdDiagnosisReliabilityCode);
        }
      }
    } catch (NullPointerException ex) {
      ex.printStackTrace();
    }
    return setResult;
  }

  /**
   * Creation of an {@linkplain Extension} which is used as flag for c19 borderline cases
   * 
   * @return c19 borderline Extension
   */
  public static Extension markBorderline() {
    Extension conditionU072 = new Extension();
    conditionU072.setUrl(CoronaFixedValues.BORDERLINE_RESULT.getValue());
    conditionU072.setValue(new StringType(CoronaFixedValues.BORDERLINE.getValue()));
    return conditionU072;
  }

  /**
   * Creation of an {@linkplain Extension} which is used as flag for c19 positive cases
   * 
   * @return c19 positive Extension
   */
  public static Extension markPositive() {
    Extension lab = new Extension();
    lab.setUrl(CoronaFixedValues.POSITIVE_RESULT.getValue());
    lab.setValue(new BooleanType(true));
    return lab;
  }

  /**
   * Creation of an {@linkplain Extension} which is used as flag for c19 negative cases
   * 
   * @return c19 positive Extension
   */
  public static Extension markNegative() {
    Extension lab = new Extension();
    lab.setUrl(CoronaFixedValues.NEGATIVE_RESULT.getValue());
    lab.setValue(new BooleanType(false));
    return lab;
  }

  /**
   * Creation of a map for Diagnose codes U07.1 or .2 including their caseIds
   * 
   * @param listConditions List of all condition resources with U07.* ICD codes
   * @param diagnosisCode The ICD diagnosis code to be checked (e.g. U07.1)
   * @return Map with diagnosis code and a list of case numbers per diagnosis code
   */
  public static HashMap<String, Set<String>> getCaseIdsByDiagnosisCode(
      List<UkbCondition> listConditions, String diagnosisCode) {

    // Create a map that assigns a list of case numbers to an ICD code.
    HashMap<String, Set<String>> mapResultDiagnoseCaseIds = new HashMap<>();

    for (UkbCondition condition : listConditions) {
      String code = condition.getCode()
          .getCoding()
          .get(0)
          .getCode();
      if (code.equals(diagnosisCode) || code.matches(".*\\b" + diagnosisCode + "\\b.*")) {

        Coding conditionCoding = condition.getCode()
            .getCoding()
            .get(0);
        Set<String> conditionCaseId = new HashSet<>();
        String icdSecurityCode = "";

        // check if the condition does have any extensions which might contain the searched diagnose
        // code
        if (conditionCoding.hasExtension(CoronaFixedValues.PRIMAERCODE.getValue())
            || conditionCoding.hasExtension(CoronaFixedValues.ICD.getValue())
            || conditionCoding.hasExtension(CoronaFixedValues.ICD_SECURITY.getValue())) {

          List<Extension> conditionExtension = conditionCoding.getExtension();

          for (Extension extension : conditionExtension) {
            Coding valueCoding = (Coding) extension.getValue();
            // check if icd security code (usually a letter) is available
            if (valueCoding.getSystem()
                .equals(CoronaFixedValues.ICD_SECURITY_SYSTEM.getValue())) {

              // check if the icd Security letter/code is a borderline or negative letter/code
              if (CoronaFixedValues.DIAGNOSIS_SECURITY_BORDERLINE.contains(valueCoding.getCode())
                  || CoronaFixedValues.DIAGNOSIS_SECURITY_NEGATIVE
                      .contains(valueCoding.getCode())) {
                icdSecurityCode = valueCoding.getCode();
              }
            }
          }
        }
        // in case icdSecurityCode is not available
        if (icdSecurityCode.equals("")) {
          icdSecurityCode = CoronaFixedValues.DIAGNOSECODE_EMPTY.getValue();
        }

        if (condition.getCaseId() != null) {
          conditionCaseId.add(condition.getCaseId());
          if (condition.getCaseId() == null) {
            log.debug("condition without caseid found");
          }
          // check if there already is a DiagnoseCode within the map
          if (mapResultDiagnoseCaseIds.containsKey(icdSecurityCode)) {
            Set<String> oldSet = mapResultDiagnoseCaseIds.remove(icdSecurityCode);
            conditionCaseId.addAll(oldSet);
            mapResultDiagnoseCaseIds.put(icdSecurityCode, conditionCaseId);
          } else {
            mapResultDiagnoseCaseIds.put(icdSecurityCode, conditionCaseId);
          }
        }
      }
    }
    return mapResultDiagnoseCaseIds;
  }

  /**
   * Creation of a set of caseIds that have a c19 positive lab result
   * 
   * @param listLabObservations list with {@link UkbObservation observation resoures}
   * @return set of caseids that have a c19 positive lab result
   */
  public static Set<String> getCaseIdsWithPositiveLabObs(List<UkbObservation> listLabObservations) {
    Set<String> labSet = new HashSet<>();
    for (UkbObservation labObs : listLabObservations) {
      if (CoronaFixedValues.COVID_LOINC_CODES.contains(labObs.getCode()
          .getCoding()
          .get(0)
          .getCode())) {
        if (((CodeableConcept) labObs.getValue()).getCoding()
            .get(0)
            .getCode()
            .equals(CoronaFixedValues.POSITIVE_CODE.getValue()))
          labSet.add(labObs.getCaseId());
      }
    }
    return labSet;
  }

  /**
   * Create a set of encounters extended with a c19 positive extension if positive lab results were
   * found on the encounters.
   * 
   * @param listEncounters List of all Encounters that could be flagged
   * @param setPositiveLabResultCaseIds Set with case numbers of cases in which a positive c19
   *          laboratory finding could be found
   * @return Set of encounters extended by c19 positive extensions
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
