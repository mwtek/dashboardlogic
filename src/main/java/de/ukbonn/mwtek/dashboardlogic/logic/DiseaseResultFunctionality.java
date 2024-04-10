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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isCaseClassInpatient;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isCaseClassOutpatient;
import static de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter.getIcuLocationIds;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByContext;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getPatientIdsByObsInterpretation;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getPatientIdsByObsValue;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityEncounterToIcuSupplyContactsMap;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Reference;

/**
 * This class contains all the functions that are used in different disease context.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class DiseaseResultFunctionality {

  /**
   * same procedure as createCurrentIcuMap, just for everything besides the current logic
   *
   * @param encounters        A list with {@linkplain UkbEncounter} resources.
   * @param locations         A list with {@linkplain UkbLocation} resources, to figure out which
   *                          location is an icu location.
   * @param icuProcedures     The {@link UkbProcedure} resources, which include information about
   *                          ECMO / artificial ventilation periods.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return Map on which ICU cases are separated according to ICU, ventilation and Ecmo
   */
  public static Map<TreatmentLevels, List<UkbEncounter>> createIcuMap(
      List<UkbEncounter> encounters, List<UkbEncounter> supplyContactEncounters,
      List<UkbLocation> locations, List<UkbProcedure> icuProcedures,
      InputCodeSettings inputCodeSettings) {
    log.debug("started createIcuMap");

    // List of stationary Cases
    List<UkbEncounter> inpatientPositiveEncounters =
        encounters.parallelStream().filter(EncounterFilter::isDiseasePositive)
            .filter(EncounterFilter::isCaseClassInpatient)
            .toList();

    List<UkbEncounter> supplyContactEncountersPositive = supplyContactEncounters.stream()
        .filter(EncounterFilter::isDiseasePositive).toList();

    Instant start = TimerTools.startTimer();

    // checking location = icu location via location id
    Set<String> listIcuLocationIds = getIcuLocationIds(locations);

    // Filter procedure resources to usable ones (e.g. encounter id must be set)
    List<UkbProcedure> icuProceduresWithEncRef =
        icuProcedures.stream().filter(x -> x.getCaseId() != null).toList();
    Set<String> facilityContactsOnIcu = supplyContactEncountersPositive.stream()
        .filter(x -> x.isIcuCase(listIcuLocationIds)).map(UkbEncounter::getFacilityContactId)
        .collect(
            Collectors.toSet());

    List<UkbEncounter> icuEncounters = inpatientPositiveEncounters.stream()
        .filter(x -> facilityContactsOnIcu.contains(x.getId())).toList();

    Set<String> facilityContactsWithVent = icuProceduresWithEncRef.stream()
        .filter(
            x -> x.isCodeExistingInFirstCoding(inputCodeSettings.getProcedureVentilationCodes()))
        .map(
            UkbProcedure::getCaseId).collect(
            Collectors.toSet());

    List<UkbEncounter> ventEncounters = inpatientPositiveEncounters.stream()
        .filter(x -> facilityContactsWithVent.contains(x.getId())).toList();

    Set<String> facilityContactsWithEcmo = icuProceduresWithEncRef.stream()
        .filter(x -> x.isCodeExistingInFirstCoding(inputCodeSettings.getProcedureEcmoCodes())).map(
            UkbProcedure::getCaseId).collect(
            Collectors.toSet());

    List<UkbEncounter> ecmoEncounters = inpatientPositiveEncounters.stream()
        .filter(x -> facilityContactsWithEcmo.contains(x.getId())).toList();

    Map<TreatmentLevels, List<UkbEncounter>> resultMap = new ConcurrentHashMap<>();
    resultMap.put(ICU, icuEncounters);
    resultMap.put(ICU_VENTILATION, ventEncounters);
    resultMap.put(ICU_ECMO, ecmoEncounters);

    TimerTools.stopTimerAndLog(start, "finished createIcuMap");
    return resultMap;
  }

  public static FacilityEncounterToIcuSupplyContactsMap assignSupplyEncountersToFacilityEncounter(
      List<UkbEncounter> icuSupplyContactEncounters, List<UkbEncounter> inpatientEncounters) {

    // Initialize output map
    FacilityEncounterToIcuSupplyContactsMap output = new FacilityEncounterToIcuSupplyContactsMap();

    // Group ICU supply contacts by official identifier value
    Map<String, List<UkbEncounter>> icuSupplyContactsMap = icuSupplyContactEncounters.stream()
        .collect(Collectors.groupingBy(UkbEncounter::getVisitNumberIdentifierValue));

    List<UkbEncounter> facilityContacts = inpatientEncounters.parallelStream()
        .filter(EncounterFilter::isFacilityContact).toList();

    // Iterate over facility contacts and map supply contacts
    for (UkbEncounter facilityContact : facilityContacts) {
      try {
        String visitNumber = facilityContact.getVisitNumberIdentifierValue();
        // Get supply contacts for the facility contact
        List<UkbEncounter> supplyContacts = icuSupplyContactsMap.getOrDefault(
            visitNumber, Collections.emptyList());
        // Add the mapping to the output, using an empty list if no supply contacts are present
        output.computeIfAbsent(facilityContact.getId(), k -> new ArrayList<>())
            .addAll(supplyContacts);
      } catch (Exception ex) {
        log.error("Unable to determine the supply contacts for facility contact "
            + facilityContact.getId());
      }
    }
    return output;
  }

  /**
   * This method generates a mapping from supply contacts to facility contacts. The input sets are
   * lists of UkbEncounter objects.
   *
   * @param supplyContactEncounters The list of supply contacts.
   * @param facilityEncounters      The list of facility contacts.
   * @return A mapping from supply contacts to facility contacts.
   */
  public static Map<String, String> generateSupplyContactToFacilityContactMap(
      List<UkbEncounter> supplyContactEncounters, List<UkbEncounter> facilityEncounters) {

    // Initialize output map
    Map<String, String> output = new ConcurrentHashMap<>();

    // Index facility encounters by officialIdentifierValue
    Map<String, UkbEncounter> facilityEncounterMap = new ConcurrentHashMap<>();
    facilityEncounters.parallelStream().forEach(facilityEncounter ->
    {
      String visitNumber = facilityEncounter.getVisitNumberIdentifierValue();
      if (visitNumber != null) {
        facilityEncounterMap.put(facilityEncounter.getVisitNumberIdentifierValue(),
            facilityEncounter);
      } else {
        log.warn("No identifier with slice 'Aufnahmenummer' was found for encounter with id "
            + facilityEncounter.getId());
      }
    });
// TODO Implement a feature that checks .partOf recursively if Encounter.identifier is null
    if (facilityEncounterMap.isEmpty()) {
      String logMessage =
          "Not a single encounter with an identifier.slice 'Aufnahmenummer' was found. Thus, no "
              + "hierarchical determination of supply contact -> facility contact is possible!";
      throw new RuntimeException(logMessage);
    }

    // Map supply contacts to facility contacts and add a references to the supply contact resource
    supplyContactEncounters.parallelStream().forEach(supplyContactEncounter -> {
      try {
        String visitNumber = supplyContactEncounter.getVisitNumberIdentifierValue();
        if (visitNumber != null) {
          UkbEncounter facilityEncounter = facilityEncounterMap.get(visitNumber);
          if (facilityEncounter != null) {
            supplyContactEncounter.setFacilityContactId(facilityEncounter.getId());
            output.put(supplyContactEncounter.getId(), facilityEncounter.getId());
          }
        }
      } catch (Exception ex) {
        // Log statement can be inserted here if necessary
        log.error("");
      }
    });

    return output;
  }

  /**
   * Create a map that sorts the encounters according to whether they are an inpatient or
   * outpatient.
   *
   * @param encounters A list with {@linkplain UkbEncounter} resources
   * @return A map where encounter is sorted after stationary and ambulant
   */
  public static Map<TreatmentLevels, List<UkbEncounter>> createEncounterMapByClass(
      List<UkbEncounter> encounters) {
    Map<TreatmentLevels, List<UkbEncounter>> encounterMap = new HashMap<>();

    log.debug("started createEncounterMapByClass");
    Instant startTimer = TimerTools.startTimer();

    List<UkbEncounter> positiveEncounters = encounters.stream().filter(
            EncounterFilter::isDiseasePositive)
        .toList();

    encounterMap.put(OUTPATIENT, new ArrayList<>());
    encounterMap.put(INPATIENT, new ArrayList<>());
    // Iterate through each encounter,
    // check if they have a positive flag
    // and if they are ambulant or stationary cases
    positiveEncounters.forEach(e -> {
      // retrieve the contact type (Encounter.type.kontaktart) which is needed to figure out if the
      // inpatient case is a prestationary or normalstationary one
      // check if encounter is stationary (without the prestationary ones!)
      if (isCaseClassInpatient(e)) {
        encounterMap.get(INPATIENT).add(e);
      }
      // check if encounter is ambulant
      else if (isCaseClassOutpatient(e)) {
        encounterMap.get(OUTPATIENT).add(e);
      }
    });
    TimerTools.stopTimerAndLog(startTimer, "finished createEncounterMapByClass");
    return encounterMap;
  }

  /**
   * Create a map sorting the encounters according to their icu treatmentlevel, only for the current
   * logic.
   * <p>
   * If an icu patient got an active ventilation AND an ECMO, he will appear in all three lists. The
   * sorting by hierarchy part will be done later in the data item generation
   *
   * @param mapIcuOverall The map generated in the {@link DiseaseResultFunctionality#createIcuMap}
   *                      method.
   * @return Map Containing encounter which are currently in icu
   */
  public static Map<TreatmentLevels, List<UkbEncounter>> createCurrentIcuMap(
      Map<TreatmentLevels, List<UkbEncounter>> mapIcuOverall) {
    log.debug("started createCurrentIcuMap");
    Instant startTimer = TimerTools.startTimer();

    // Pre-filtering: Figure out if the facility contact encounter is in-progress (NOT the
    // procedure/icu situation!)
    List<UkbEncounter> currentActiveIcuEncounters = mapIcuOverall.get(ICU).stream()
        .filter(EncounterFilter::isActive).filter(EncounterFilter::isDiseasePositive).toList();

    List<UkbEncounter> currentActiveIcuVentEncounters = mapIcuOverall.get(ICU_VENTILATION).stream()
        .filter(EncounterFilter::isActive).filter(EncounterFilter::isDiseasePositive).toList();

    List<UkbEncounter> currentActiveIcuEcmoEncounters = mapIcuOverall.get(ICU_ECMO).stream()
        .filter(EncounterFilter::isActive).filter(EncounterFilter::isDiseasePositive).toList();

    Map<TreatmentLevels, List<UkbEncounter>> resultMap = new ConcurrentHashMap<>();
    resultMap.put(ICU, currentActiveIcuEncounters);
    resultMap.put(ICU_VENTILATION, currentActiveIcuVentEncounters);
    resultMap.put(ICU_ECMO, currentActiveIcuEcmoEncounters);

    TimerTools.stopTimerAndLog(startTimer, "finished createCurrentIcuMap");
    return resultMap;
  }

  /**
   * Fills the given map for debugging purposes, associating encounter case IDs with patients and
   * treatment levels.
   *
   * @param listCumulativeEncounter   Sublist with encounters that have already been used in the
   *                                  "cumulative.treatmentlevel" data item.
   * @param treatmentLevel            TreatmentLevel (e.g., {@link TreatmentLevels#OUTPATIENT}).
   * @param resultMaxTreatmentCaseNrs Map with the output, associating treatment levels with
   *                                  patients and their encounter case IDs. This map is modified
   *                                  and populated during the execution of the method.
   */
  public static void createCumulativeMaxDebug(List<UkbEncounter> listCumulativeEncounter,
      String treatmentLevel,
      Map<String, Map<String, List<String>>> resultMaxTreatmentCaseNrs) {
    listCumulativeEncounter.forEach(e -> {
      String pid = e.getPatientId();
      if (resultMaxTreatmentCaseNrs.get(treatmentLevel).containsKey(pid)) {
        resultMaxTreatmentCaseNrs.get(treatmentLevel).get(pid).add(e.getId());
      } else {
        List<String> caseList = new ArrayList<>();
        caseList.add(e.getId());
        resultMaxTreatmentCaseNrs.get(treatmentLevel).put(pid, caseList);
      }
    });
  }

  /**
   * Calculates the patient's age (in years) at the time of case admission.
   *
   * @param birthDate     The birthdate of the patient as a {@link java.util.Date} object.
   * @param admissionDate Date when the patient was hospitalized as a {@link java.util.Date}
   *                      object.
   * @return The age of the patient in years at the time of admission, or null if either date is
   * null.
   */
  public static Integer calculateAge(Date birthDate, Date admissionDate) {
    if (birthDate == null || admissionDate == null) {
      return null;
    } else {
      long birthDateInSec = DateTools.dateToUnixTime(birthDate);
      long caseDateInSec = DateTools.dateToUnixTime(admissionDate);
      LocalDate birthDateLocal =
          Instant.ofEpochSecond(birthDateInSec).atZone(ZoneId.systemDefault()).toLocalDate();
      LocalDate casePeriodDateLocal =
          Instant.ofEpochSecond(caseDateInSec).atZone(ZoneId.systemDefault()).toLocalDate();
      return Period.between(birthDateLocal, casePeriodDateLocal).getYears();
    }
  }

  /**
   * Identification of all patients who have at least one case with a positive lab result.
   *
   * @param ukbObservations   List with the {@link UkbObservation lab findings}.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @param dataItemContext   The given data item context (e.g. {@link DataItemContext#COVID}.
   * @return List with the patients ids of all patients who have at least one case with a positive
   * SARS-CoV-2 pcr tests
   */
  public static Collection<String> getPidsWithPosLabResult(
      List<UkbObservation> ukbObservations, InputCodeSettings inputCodeSettings,
      DataItemContext dataItemContext) {

    // The extraction is based on a subset of the observations, namely just the covid related ones.
    Set<UkbObservation> positiveObservations = getObservationsByContext(ukbObservations,
        inputCodeSettings, dataItemContext);

    // 1) Identification by Observation.value
    Set<String> positivePatientIds = getPatientIdsByObsValue(positiveObservations, POSITIVE);

    // 2) Identification by Observation.interpretation
    positivePatientIds.addAll(
        getPatientIdsByObsInterpretation(positiveObservations, POSITIVE));

    return positivePatientIds;
  }

  /**
   * Difference between two days in hours
   *
   * @param start The start date
   * @param end   The end date
   * @return Difference in hours
   */
  public static Long calculateDaysInBetweenInHours(LocalDateTime start, LocalDateTime end) {
    return ChronoUnit.HOURS.between(start, end);
  }


  private static List<Long> datesOutput;

  private static List<Long> createDateList() {
    // Initialization of the output list
    datesOutput = new ArrayList<>();
    long currentDate = CoronaDashboardConstants.qualifyingDate;
    long currentDayUnix = DateTools.getCurrentUnixTime();

    while (currentDate <= currentDayUnix) {
      datesOutput.add(currentDate);
      currentDate += CoronaDashboardConstants.DAY_IN_SECONDS;
    }
    return datesOutput;
  }

  /**
   * Generation of a list of all 24-hour timestamps from the start of disease-positive-data
   * collection to the query time.
   *
   * @return List of 24-hour timestamps for the entries in the data items with "date" attribute.
   */
  public static List<Long> getDatesOutputList() {
    // Generate the date output list if needed (initially or when its outdated since the
    // server ran over 2 days)
    if (datesOutput == null || (DateTools.getCurrentUnixTime() > datesOutput.get(
        datesOutput.size() - 1))) {
      datesOutput = createDateList();
    }

    return datesOutput;
  }


  /**
   * Determination of all patient Ids that have at least one case with a disease-positive PCR
   * laboratory result and/or disease-context-related diagnosis (e.g. U07.1 and U07.2).
   *
   * @param ukbObservations   List with
   *                          {@link UkbObservation disease-context-related lab findings}.
   * @param ukbConditions     List with {@link UkbCondition disease-context-related conditions}.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return {@link HashSet} with patient Ids that have at least one case with a positive
   * disease-context-related PCR laboratory result and/or disease-context-related diagnosis.
   */
  public static Set<String> getPidsPosFinding(List<UkbObservation> ukbObservations,
      List<UkbCondition> ukbConditions, InputCodeSettings inputCodeSettings,
      DataItemContext dataItemContext) {
    Set<String> listPidsPosFinding = new HashSet<>();
    // Get all patient ids where the patient got at least one disease-positive lab finding
    listPidsPosFinding.addAll(
        getPidsWithPosLabResult(ukbObservations, inputCodeSettings, dataItemContext));
    // Get all patient ids where the patient got at least one disease-positive related condition
    listPidsPosFinding.addAll(
        getPidsWithGivenIcdCodes(ukbConditions, inputCodeSettings, dataItemContext));
    return listPidsPosFinding;
  }


  /**
   * Identification of all patients who have at least one case with a given icd condition resource.
   *
   * @param ukbConditions     List with the {@link UkbCondition icd condition resources}.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @param dataItemContext   The given data item context (e.g. {@link DataItemContext#COVID}.
   * @return A set with the patient ids of all patients who have at least one case with a one of the
   * given diagnoses.
   */
  public static Set<String> getPidsWithGivenIcdCodes(List<UkbCondition> ukbConditions,
      InputCodeSettings inputCodeSettings, DataItemContext dataItemContext) {
    Set<String> pidsWithGivenIcdCodes = new HashSet<>();
    List<String> inputCodes = switch (dataItemContext) {
      case COVID -> inputCodeSettings.getCovidConditionIcdCodes();
      case INFLUENZA -> inputCodeSettings.getInfluenzaConditionIcdCodes();
      default -> Collections.emptyList(); // this branch never should get reached
    };
    ukbConditions.forEach(condition -> {
      if (condition.hasCode() && isCodingValid(condition.getCode(),
          DashboardLogicFixedValues.ICD_SYSTEM.getValue(), inputCodes)) {
        pidsWithGivenIcdCodes.add(condition.getPatientId());
      }
    });
    return pidsWithGivenIcdCodes;
  }

  /**
   * Finds the first recorded encounter resource the patient had, and saves it together with the
   * patient resource.
   *
   * @param encounter       The {@link UkbEncounter} to be checked.
   * @param pidAdmissionMap Map containing patient ids and the first encounter resource attached to
   *                        it.
   */
  public static void assignFirstAdmissionDateToPid(
      UkbEncounter encounter,
      Map<String, UkbEncounter> pidAdmissionMap) {
    String pid = encounter.getPatientId();
    if (encounter.isPeriodStartExistent()) {
      Date encounterStartDate = encounter.getPeriod().getStart();
      Instant instant = encounterStartDate.toInstant();
      LocalDateTime encounterAdmission = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
      // If the key doesn't exist, add the object directly
      pidAdmissionMap.putIfAbsent(pid, encounter);
      // If the key already exists and the current encounter is earlier,
      // update the encounter in the map
      pidAdmissionMap.computeIfPresent(pid, (key, prevEncounter) -> {
        Date prevStartDate = prevEncounter.getPeriod().getStart();
        Instant prevInstant = prevStartDate.toInstant();
        LocalDateTime prevAdmission = LocalDateTime.ofInstant(prevInstant, ZoneId.systemDefault());
        if (prevAdmission.isAfter(encounterAdmission)) {
          return encounter;
        }
        return prevEncounter;
      });
    }
  }


  /**
   * Sort age into the corresponding age group.
   *
   * @param age Age for checking
   * @return Lowest value of the age group.
   */
  public static int checkAgeGroup(int age) {
    // Handle edge cases
    if (age < 0) {
      throw new IllegalArgumentException("Age cannot be negative.");
    } else if (age <= 19) {
      return 0; // Age group 0-19
    } else if (age >= 90) {
      return 90; // Age group 90 and above
    }

    // For ages between 20 and 49
    if (age < 50) {
      for (int i = 20; i <= 45; i += 5) {
        if (age <= i + 4) {
          return i; // Return the lowest value of the age group
        }
      }
    }
    // For ages between 50 and 89
    else {
      for (int i = 50; i <= 85; i += 5) {
        if (age <= i + 4) {
          return i; // Return the lowest value of the age group
        }
      }
    }
    // Default return (should not be reached)
    throw new IllegalStateException("Unexpected age value: " + age);
  }


  /**
   * Extracts the plain ID from a FHIR resource reference.
   *
   * <p>
   * For example, given a FHIR resource reference like "http://fhirserver.com/r4/Location/123", this
   * method will extract "123" as the plain ID of the resource.
   * </p>
   *
   * @param fhirResourceReference The FHIR resource reference to extract the ID from.
   * @return The plain ID of the resource.
   * @throws IllegalArgumentException If the provided FHIR resource reference is null or empty.
   */
  public static String extractIdFromReference(Reference fhirResourceReference) {
    if (fhirResourceReference == null || fhirResourceReference.getReference().isEmpty()) {
      throw new IllegalArgumentException("FHIR resource reference must not be null or empty.");
    }

    // Split the reference into segments using "/"
    String[] segments = fhirResourceReference.getReference().split("/");

    // The ID is typically the last segment
    return segments[segments.length - 1];
  }


  /**
   * Checking if the given {@link CodeableConcept#getCoding() coding} of a
   * <code>CodeableConcept</code> got a code that is part of the given value set.
   *
   * @param codeableConcept The codings that need to be checked (e.g.
   *                        {@link UkbObservation#getCode()}).
   * @param system          The system that should be checked.
   * @param validCodes      The codes that should be checked.
   * @return <code>True</code> if the system and a valid code are part of the coding.
   */
  public static boolean isCodingValid(CodeableConcept codeableConcept, String system,
      List<String> validCodes) {
    boolean isCodingValid = false;
    for (String code : validCodes) {
      if (codeableConcept.hasCoding(system, code)) {
        isCodingValid = true;
      }
    }
    return isCodingValid;
  }
}
