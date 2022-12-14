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

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getCovidObservations;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getPatientIdsByObsInterpretation;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getPatientIdsByObsValue;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCoding;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodesystem;

import de.ukbonn.mwtek.dashboardlogic.CoronaDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaTreatmentLevelExport;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;
import org.hl7.fhir.r4.model.Reference;

/**
 * This class contains all the functions that could be important in several of the sub-logics
 * (current, cumulative and temporal)
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CoronaResultFunctionality {

  /**
   * Create a map sorting the encounters according to their icu treatmentlevel, only for the current
   * logic
   *
   * @param listEncounters    A list with {@linkplain UkbEncounter} resources.
   * @param listLocations     A list with {@linkplain UkbLocation} resources, to figure out which
   *                          location is an icu location.
   * @param listIcuProcedures The {@link UkbProcedure} resources, which include information about
   *                          ECMO / artificial ventilation periods.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return Map Containing encounter which are currently in icu
   */
  public static Map<String, List<UkbEncounter>> createCurrentIcuMap(
      List<UkbEncounter> listEncounters, List<UkbLocation> listLocations,
      List<UkbProcedure> listIcuProcedures, InputCodeSettings inputCodeSettings) {
    log.debug("started createCurrentIcuMap");
    Instant startTimer = TimerTools.startTimer();

    // just inpatient encounter needed to check (ambulant cases can't have icu data)
    List<UkbEncounter> listEncounterInpatients =
        listEncounters.parallelStream().filter(CoronaResultFunctionality::isCaseClassInpatient)
            .toList();

    // get All Icu Locations
    List<UkbLocation> listIcuLocations = listLocations.stream().filter(x -> !x.getType().isEmpty())
        .filter(LocationFilter::isLocationWard).filter(LocationFilter::isLocationIcu)
        .collect(Collectors.toList());

    Map<String, List<UkbEncounter>> resultMap = new ConcurrentHashMap<>();
    resultMap.put(CoronaFixedValues.ICU.getValue(), new ArrayList<>());
    resultMap.put(CoronaFixedValues.ICU_VENTILATION.getValue(), new ArrayList<>());
    resultMap.put(CoronaFixedValues.ICU_ECMO.getValue(), new ArrayList<>());
    List<UkbEncounter> listRemovedEncounter = new ArrayList<>();

    // sort encounter to map accordingly (icu, Icu_Vent, Icu_Ecmo)
    try {
      listEncounterInpatients.parallelStream().forEach(encounter -> {
        boolean isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
        if (isPositive) {
          List<UkbProcedure> listIcuCurrentPatient =
              listIcuProcedures.stream().filter(x -> x.getCaseId() != null)
                  .filter(x -> x.getCaseId().equals(encounter.getId()))
                  .toList();

          if (listIcuCurrentPatient.isEmpty()) {
            checkCurrentLocationWithCaseLocation(encounter, listIcuLocations, resultMap);
          } else {
            listIcuCurrentPatient.forEach(icu -> {
              // if encounter is icu_vent or icu_Ecmo
              if (icu.getStatus().equals(ProcedureStatus.INPROGRESS)) {
                if (icu.hasCode() && icu.getCode().hasCoding()) {
                  // No hard system check as some providers may have the need to use ops instead of snomed.
                  String icuCode = getCodeOfFirstCoding(icu.getCode().getCoding());
                  sortIcuToMap(icuCode, resultMap, encounter, inputCodeSettings);
                } else {
                  log.warn(logMissingProcedureCode(icu.getId()));
                }
              }
              // check if the current location of the encounter is icu
              else {
                checkCurrentLocationWithCaseLocation(encounter, listIcuLocations, resultMap);
              }
            });
          }
        }
      });

      // Checking if one of the ICU encounters still has a running ventilation or ecmo is connected
      for (UkbEncounter encounter : resultMap.get(CoronaFixedValues.ICU.getValue())) {
        if (resultMap.get(CoronaFixedValues.ICU_VENTILATION.getValue())
            .contains(encounter) || resultMap.get(CoronaFixedValues.ICU_ECMO.getValue())
            .contains(encounter)) {
          for (UkbProcedure icu : listIcuProcedures) {
            if (icu.getCaseId().equals(encounter.getId())) {
              if (icu.getStatus().equals(ProcedureStatus.INPROGRESS)) {
                listRemovedEncounter.add(encounter);
              }
            }
          }
        }
      }
      resultMap.get(CoronaFixedValues.ICU.getValue()).removeAll(listRemovedEncounter);
    } catch (Exception e) {
      e.printStackTrace();
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createCurrentIcuMap");
    return resultMap;
  }

  /**
   * same procedure as createCurrentIcuMap, just for everything besides the current logic
   *
   * @param listEncounters    A list with {@linkplain UkbEncounter} resources.
   * @param listLocations     A list with {@linkplain UkbLocation} resources, to figure out which
   *                          location is an icu location.
   * @param listIcuProcedures The {@link UkbProcedure} resources, which include information about
   *                          ECMO / artificial ventilation periods.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return Map on which ICU cases are separated according to ICU, ventilation and Ecmo
   */
  public static Map<String, List<UkbEncounter>> createIcuMap(List<UkbEncounter> listEncounters,
      List<UkbLocation> listLocations, List<UkbProcedure> listIcuProcedures,
      InputCodeSettings inputCodeSettings) {
    log.debug("started createIcuMap");

    // List of stationary Cases
    List<UkbEncounter> listInpatientEncounter =
        listEncounters.parallelStream().filter(CoronaResultFunctionality::isCaseClassInpatient)
            .toList();

    Instant start = TimerTools.startTimer();
    // get Icu Locations
    List<UkbLocation> listIcuLocations = listLocations.stream().filter(x -> !x.getType().isEmpty())
        .filter(LocationFilter::isLocationWard).filter(LocationFilter::isLocationIcu)
        .collect(Collectors.toList());

    // Filter procedure resources to usable ones (e.g. encounter id must be set)
    List<UkbProcedure> listIcuProceduresWithEncRef =
        listIcuProcedures.stream().filter(x -> x.getCaseId() != null).toList();

    Map<String, List<UkbEncounter>> resultMap = new ConcurrentHashMap<>();
    resultMap.put(CoronaFixedValues.ICU.getValue(),
        Collections.synchronizedList(new ArrayList<>()));
    resultMap.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
        Collections.synchronizedList(new ArrayList<>()));
    resultMap.put(CoronaFixedValues.ICU_ECMO.getValue(),
        Collections.synchronizedList(new ArrayList<>()));

    Map<String, Object> lockMap = new ConcurrentHashMap<>();

    listInpatientEncounter.parallelStream().forEach(encounter -> {

      lockMap.putIfAbsent(encounter.getPatientId(), new Object());

      // make sure that encounter of a patient are not handled simultaneously
      synchronized (lockMap.get(encounter.getPatientId())) {
        boolean isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
        String caseId = encounter.getId();

        if (isPositive) {
          // check whether encounter has connection to ventilation or ecmo procedure

          // get all procedures of an encounter
          List<UkbProcedure> listIcuByEncounter =
              listIcuProceduresWithEncRef.stream().filter(x -> x.getCaseId().equals(caseId))
                  .toList();

          for (UkbProcedure icu : listIcuByEncounter) {
            if (icu.hasCode() && icu.getCode().hasCoding()) {
              // The procedure retrieval is not fixed to a system at the moment since some providers are using ops instead of snomed.
              String icuCode = getCodeOfFirstCoding(icu.getCode().getCoding());
              sortIcuToMap(icuCode, resultMap, encounter, inputCodeSettings);
            } else {
              log.warn(logMissingProcedureCode(icu.getId()));
            }
          }
          // check if encounter is only lying in icu without any procedure
          checkIfEncounterIsIcu(encounter, listIcuLocations, resultMap);
        }
      }
    });

    TimerTools.stopTimerAndLog(start, "finished createIcuMap");
    return resultMap;
  }

  private static String logMissingProcedureCode(String procedureId) {
    return "Procedure with id " + procedureId
        + " got no code, though its mandatory [1..1].";
  }

  /**
   * Adds the given case to a map as an ICU case if no higher treatment level is found for this
   * case.
   *
   * @param encounter        The encounter that is going to be checked
   * @param listIcuLocations A list with the ICU {@linkplain UkbLocation} resources
   * @param resultMap        Map that links the maxtreatmentlevel with {@link UkbEncounter}
   */
  private static void checkIfEncounterIsIcu(UkbEncounter encounter,
      List<UkbLocation> listIcuLocations, Map<String, List<UkbEncounter>> resultMap) {
    // checking location = icu location via location id
    List<String> listIcuLocationIds = listIcuLocations.stream().map(UkbLocation::getId).toList();

    for (EncounterLocationComponent location : encounter.getLocation()) {
      // check if location id or reference should be used
      if (!location.getLocation().hasReference()) {
        if (listIcuLocationIds.contains(location.getId())) {
          if (!resultMap.get(CoronaFixedValues.ICU_VENTILATION.getValue())
              .contains(encounter) && !resultMap.get(CoronaFixedValues.ICU_ECMO.getValue())
              .contains(encounter)) {
            if (!resultMap.get(CoronaFixedValues.ICU.getValue()).contains(encounter)) {
              resultMap.get(CoronaFixedValues.ICU.getValue()).add(encounter);
            }
          }
        }
      } else {
        if (listIcuLocationIds.contains(
            extractIdFromResourceReference(location.getLocation().getReference()))) {
          if (!resultMap.get(CoronaFixedValues.ICU_VENTILATION.getValue())
              .contains(encounter) && !resultMap.get(CoronaFixedValues.ICU_ECMO.getValue())
              .contains(encounter)) {
            if (!resultMap.get(CoronaFixedValues.ICU.getValue()).contains(encounter)) {
              resultMap.get(CoronaFixedValues.ICU.getValue()).add(encounter);
            }
          }
        }
      }
    }
  }

  /**
   * Extracts the ID from a resource reference (e.g.: {@literal "Patient/123" -> 123}).
   *
   * @param resourceReference Resource reference (e.g. "Patient/123").
   * @return The id of the reference.
   */
  private static String extractIdFromResourceReference(String resourceReference) {
    String[] parts = resourceReference.split("/");
    return parts[parts.length - 1];
  }

  /**
   * Used to identify whether the encounter is located in icu (for currentlogic)
   *
   * @param encounter        The encounter that is going to be checked
   * @param listIcuLocations A list with the ICU {@link UkbLocation} resources
   * @param resultMap        Map that links the maxtreatmentlevel with {@link UkbEncounter}
   */
  private static void checkCurrentLocationWithCaseLocation(UkbEncounter encounter,
      List<UkbLocation> listIcuLocations, Map<String, List<UkbEncounter>> resultMap) {
    List<String> locationIds = listIcuLocations.stream().map(UkbLocation::getId).toList();

    for (EncounterLocationComponent caseLocation : encounter.getLocation()) {
      if (locationIds.contains(
          extractIdFromResourceReference(caseLocation.getLocation().getReference()))) {
        if (!resultMap.get(CoronaFixedValues.ICU.getValue())
            .contains(encounter) && !caseLocation.getPeriod().hasEnd()) {
          resultMap.get(CoronaFixedValues.ICU.getValue()).add(encounter);
        }
      }
    }
  }

  /**
   * Sorting the icu cases whether they have an ecmo or a ventilation procedure
   *
   * @param procedureCode     The value of an {@link UkbProcedure procedure.code} most likely a
   *                          snomed ct code.
   * @param resultMap         Map that links the maxtreatmentlevel with {@link UkbEncounter}.
   * @param encounter         The encounter that is going to be checked.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   */
  private static void sortIcuToMap(String procedureCode, Map<String, List<UkbEncounter>> resultMap,
      UkbEncounter encounter, InputCodeSettings inputCodeSettings) {
    // check if it is an ecmo case
    if (inputCodeSettings.getProcedureEcmoCodes().contains(procedureCode)) {
      if (!resultMap.get(CoronaFixedValues.ICU_ECMO.getValue()).contains(encounter)) {
        addIcuEncounter(resultMap, encounter, CoronaFixedValues.ICU_ECMO.getValue());
      }
    }

    // check if it is a ventilation case
    else if (inputCodeSettings.getProcedureVentilationCodes().contains(procedureCode)) {
      if (!resultMap.get(CoronaFixedValues.ICU_VENTILATION.getValue()).contains(encounter)) {
        addIcuEncounter(resultMap, encounter, CoronaFixedValues.ICU_VENTILATION.getValue());
      }
    }
  }

  /**
   * Adds Encounter to either the ECMO or Ventilation lists inside the map
   *
   * @param resultMap containing the treatmentlevel and a list of encounter with that
   *                  treatmentlevel
   * @param encounter Case Resource
   * @param icuType   Defines whether the encounter Resource is a ECMO or Ventilation Case
   */
  private static void addIcuEncounter(Map<String, List<UkbEncounter>> resultMap,
      UkbEncounter encounter, String icuType) {
    if (icuType.equals(CoronaFixedValues.ICU_ECMO.getValue())) {
      resultMap.get(CoronaFixedValues.ICU_ECMO.getValue()).add(encounter);
    } else {
      resultMap.computeIfPresent(CoronaFixedValues.ICU_VENTILATION.getValue(), (k, v) -> {
        v.add(encounter);
        return v;
      });
    }
  }

  /**
   * Create a map sorting the encounter after whether they are inpatient or outpatient
   *
   * @param listEncounters A list with {@linkplain UkbEncounter} resources
   * @return map Where encounter are sorted after stationary and ambulant
   */
  public static HashMap<String, List<UkbEncounter>> createEncounterMap(
      List<UkbEncounter> listEncounters) {
    HashMap<String, List<UkbEncounter>> encounterMap = new HashMap<>();

    log.debug("started createEncounterMap");

    Instant startTimer = TimerTools.startTimer();

    encounterMap.put(CoronaFixedValues.OUTPATIENT_ITEM.getValue(), new ArrayList<>());
    encounterMap.put(CoronaFixedValues.INPATIENT_ITEM.getValue(), new ArrayList<>());
    // Iterate through each encounter,
    // check if they have positive flag
    // and if they are ambulant or stationary cases
    listEncounters.forEach(e -> {
      // retrieve the contact type (Encounter.type.kontaktart) which is needed to figure out if the
      // inpatient case is a prestationary or normalstationary one

      if (e.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())) {
        // check if encounter is stationary (without the prestationary ones!)
        if (CoronaResultFunctionality.isCaseClassInpatient(e)) {
          encounterMap.get(CoronaFixedValues.INPATIENT_ITEM.getValue()).add(e);
        }
        // check if encounter is ambulant
        else if (CoronaResultFunctionality.isCaseClassOutpatient(e)) {
          encounterMap.get(CoronaFixedValues.OUTPATIENT_ITEM.getValue()).add(e);
        }
      }
    });
    TimerTools.stopTimerAndLog(startTimer, "finished createEncounterMap");
    return encounterMap;
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

  /**
   * Creation of a cross table listing the current patients at the UKB site according to current
   * level of care and place of residence in Bonn or outside Bonn.
   *
   * @param mapCumulativeMaxTreatments Map with the current inpatient c19 positive cases and their
   *                                   maxtreatment level
   * @param listPatient                List with the patient data for the given encounter
   * @return List with the crosstab states and their values
   */
  public static List<String[]> generateCrosstabList(
      LinkedHashMap<CoronaFixedValues, List<UkbEncounter>> mapCumulativeMaxTreatments,
      List<UkbPatient> listPatient) {
    log.debug("started generateCrosstabList");
    Instant startTimer = TimerTools.startTimer();
    List<String[]> resultList = new ArrayList<>();
    List<String> listOnlyBonn = new ArrayList<>();
    List<String> listBonnAndIcu = new ArrayList<>();
    List<String> listBonnAndVent = new ArrayList<>();
    List<String> listBonnAndEcmo = new ArrayList<>();
    List<String> listNoBonn = new ArrayList<>();
    List<String> listNoBonnAndIcu = new ArrayList<>();
    List<String> listNoBonnAndVent = new ArrayList<>();
    List<String> listNoBonnAndEcmo = new ArrayList<>();
    Map<String, Boolean> mapIsBonnPatient = new HashMap<>();
    // go through each patient check if they are living in Bonn
    for (UkbPatient patient : listPatient) {
      try {
        Address address = patient.getAddressFirstRep();
        mapIsBonnPatient.put(patient.getId(),
            address.getCity() != null && address.getCity()
                .equals(CoronaFixedValues.CITY_BONN.getValue()));
      } catch (Exception e) {
        log.debug("Patient: " + patient.getId() + " got no address/city");
        e.printStackTrace();
      }
    }
    // iterate through each encounter, and sort them to the right list
    for (Map.Entry<CoronaFixedValues, List<UkbEncounter>> entry : mapCumulativeMaxTreatments.entrySet()) {
      CoronaFixedValues key = entry.getKey();
      for (UkbEncounter encounter : entry.getValue()) {
        if (mapIsBonnPatient.containsKey(encounter.getPatientId())) {
          boolean isBonn = mapIsBonnPatient.get(encounter.getPatientId());
          switch (key) {
            case INPATIENT_ITEM:
              if (isBonn) {
                listOnlyBonn.add(encounter.getId());
              } else {
                listNoBonn.add(encounter.getId());
              }
              break;
            case ICU:
              if (isBonn) {
                listBonnAndIcu.add(encounter.getId());
              } else {
                listNoBonnAndIcu.add(encounter.getId());
              }
              break;
            case ICU_VENTILATION:
              if (isBonn) {
                listBonnAndVent.add(encounter.getId());
              } else {
                listNoBonnAndVent.add(encounter.getId());
              }
              break;
            case ICU_ECMO:
              if (isBonn) {
                listBonnAndEcmo.add(encounter.getId());
              } else {
                listNoBonnAndEcmo.add(encounter.getId());
              }
              break;
          }
        }
      }
    }
    resultList.add(listNoBonn.toArray(new String[0]));
    resultList.add(listNoBonnAndIcu.toArray(new String[0]));
    resultList.add(listNoBonnAndVent.toArray(new String[0]));
    resultList.add(listNoBonnAndEcmo.toArray(new String[0]));
    resultList.add(listOnlyBonn.toArray(new String[0]));
    resultList.add(listBonnAndIcu.toArray(new String[0]));
    resultList.add(listBonnAndVent.toArray(new String[0]));
    resultList.add(listBonnAndEcmo.toArray(new String[0]));
    TimerTools.stopTimerAndLog(startTimer, "finished generateCrosstabList");

    return resultList;
  }


  /**
   * Calculates the age of the patient at the time of the admission date of a case.
   *
   * @param birthDate     The birthdate of the patient.
   * @param admissionDate Date when the patient was hospitalised.
   * @return Age at the time of the admission.
   */
  public static Integer calculateAge(Date birthDate, Date admissionDate) {
    if (birthDate != null && admissionDate != null) {
      long birthDateInSec = DateTools.dateToUnixTime(birthDate);
      long caseDateInSec = DateTools.dateToUnixTime(admissionDate);
      LocalDate birthDateLocal =
          Instant.ofEpochSecond(birthDateInSec).atZone(ZoneId.systemDefault()).toLocalDate();
      LocalDate casePeriodDateLocal =
          Instant.ofEpochSecond(caseDateInSec).atZone(ZoneId.systemDefault()).toLocalDate();
      return Period.between(birthDateLocal, casePeriodDateLocal).getYears();
    } else {
      return null;
    }
  }

  /**
   * Filling the given maxtreatmentlevel casenrs map for debug purposes.
   *
   * @param listCumulativeEncounter   Sublist with encounters that have already been used in the
   *                                  "cumulative.treatmentlevel" data item.
   * @param treatmentLevel            TreatmentLevel (e.g. {@link CoronaFixedValues#OUTPATIENT_ITEM}
   *                                  ).
   * @param resultMaxTreatmentCaseNrs Map with the output.
   */
  public static void createCumulativeMaxDebug(List<UkbEncounter> listCumulativeEncounter,
      String treatmentLevel,
      HashMap<String, Map<String, List<String>>> resultMaxTreatmentCaseNrs) {
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

  private static List<Long> datesOutput;

  private static List<Long> createDateList() {
    // Initialization of the output list
    datesOutput = new ArrayList<>();
    long currentDate = CoronaDashboardConstants.qualifyingDate;
    long currentDayUnix = DateTools.getCurrentUnixTime();

    while (currentDate <= currentDayUnix) {
      datesOutput.add(currentDate);
      currentDate += CoronaDashboardConstants.dayInSeconds;
    }
    return datesOutput;
  }

  /**
   * Generation of a list of all 24-hour timestamps from the start of covid data collection to the
   * query time.
   *
   * @return List of 24-hour timestamps for the entries in the data items with "date" attribute.
   */
  public static List<Long> getDatesOutputList() {
    // Just generate the dates output list if needed (initially or when its outdated since the server ran over 2 days)
    if (datesOutput == null || (DateTools.getCurrentUnixTime() > datesOutput.get(
        datesOutput.size() - 1))) {
      datesOutput = createDateList();
    }

    return datesOutput;
  }

  /**
   * Finds the first recorded encounter resource the patient had, and saves it together with the
   * patient resource.
   *
   * @param encounter       The {@link UkbEncounter} to be checked.
   * @param pidAdmissionMap Map containing patient ids and the first encounter resource attached to
   *                        it.
   */
  public static void assignFirstAdmissionDateToPid(UkbEncounter encounter,
      Map<String, UkbEncounter> pidAdmissionMap) {
    if (encounter.isPeriodStartExistent()) {
      LocalDateTime encounterAdmission =
          encounter.getPeriod().getStart().toInstant().atZone(ZoneId.systemDefault())
              .toLocalDateTime();
      String pid = encounter.getPatientId();
      if (!pidAdmissionMap.containsKey(encounter.getPatientId())) {
        pidAdmissionMap.put(pid, encounter);
      } else {
        UkbEncounter prevEncounter = pidAdmissionMap.get(pid);
        LocalDateTime prevAdmission =
            prevEncounter.getPeriod().getStart().toInstant().atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        if (prevAdmission.isAfter(encounterAdmission)) {
          pidAdmissionMap.replace(pid, encounter);
        }
      }
    }
  }

  /**
   * Sort age to the corresponding age group.
   *
   * @param age Age for checking
   * @return Lowest value of the age group.
   */
  public static int checkAgeGroup(int age) {
    if (age <= 19) {
      return 0;
    } else if (age >= 90) {
      return 90;
    } else {
      if (age < 50) {
        for (int i = 20; i <= 50; i += 5) {
          if (age >= i && age <= i + 4) {
            return i;
          }
        }
      }
      // else: if age >= 50
      else {
        for (int i = 50; i <= 90; i += 5) {
          if (age >= i && age <= i + 4) {
            return i;
          }
        }
      }

    }
    // this return should not be possible to happen
    return 4000;
  }


  /**
   * Retrieval of the value that is part of a slice in {@link Encounter#getType() Encounter.type}.
   *
   * @param listType List of {@link Encounter#getType() Encounter.types}.
   * @return The contact-type of a german value set (e.g. "vorstationär").
   */
  private static String getContactType(List<CodeableConcept> listType) {

    StringBuilder contactType = new StringBuilder();
    listType.forEach(ccType -> ccType.getCoding().forEach(codingType -> {
      if (codingType.hasSystem() && codingType.getSystem()
          .equals(CoronaFixedValues.CASETYPE_KONTAKTART_SYSTEM.getValue())) {
        contactType.append(codingType.getCode());
      }
    }));

    return contactType.length() > 0 ? contactType.toString() : null;
  }

  /**
   * A simple check if the given contact type got the value "prestationary".
   *
   * @param listType A list of {@link Encounter#getType() Encounter.types}.
   * @return <code>True</code> if the case type equals "prestationary".
   */
  public static boolean isCaseTypePrestationary(List<CodeableConcept> listType) {
    String contactType = getContactType(listType);
    return contactType != null && contactType.equals(
        CoronaFixedValues.CASETYPE_PRESTATIONARY.getValue());
  }

  /**
   * is the case class counted as "inpatient" regarding the json data specification (without
   * pre-stationary cases)
   *
   * @param encounter An instance of an {@link UkbEncounter} object.
   * @return <code>True</code>, if the case class of the encounter is "inpatient"
   */
  public static boolean isCaseClassInpatient(UkbEncounter encounter) {
    return encounter.hasClass_() && isCodeInCodesystem(encounter.getClass_().getCode(),
        CoronaFixedValues.ENCOUNTER_CLASS_INPATIENT_CODES) && !isCaseTypePrestationary(
        encounter.getType());
  }

  /**
   * is the case class counted as "outpatient" regarding the json data specification (plus
   * pre-stationary cases that are counted as "outpatient" logic-wise in the workflow aswell)
   *
   * @param encounter An instance of an {@link UkbEncounter} object.
   * @return <code>True</code>, if the case class of the encounter is "outpatient".
   */
  public static boolean isCaseClassOutpatient(UkbEncounter encounter) {
    return encounter.hasClass_() && isCodeInCodesystem(encounter.getClass_().getCode(),
        CoronaFixedValues.ENCOUNTER_CLASS_OUTPATIENT_CODES) || isCaseTypePrestationary(
        encounter.getType());
  }

  /**
   * Splits the system and resource part from the id in a fhir resource reference (e.g.
   * {@literal http://fhirserver.com/r4/Location/123 -> 123})
   *
   * @param fhirResourceReference A fhir resource reference
   * @return the plain id of the resource
   */
  public static String extractIdFromReference(Reference fhirResourceReference) {
    // Split reference into segments
    String[] segments = fhirResourceReference.getReference().split("/");
    // Grab the last segment
    return segments[segments.length - 1];
  }

  /**
   * Export of a csv file that displays a list of case/encounter numbers of active cases separated
   * by treatment level when run through.
   *
   * @param mapCurrentTreatmentlevelCasenrs {@link
   *                                        CoronaDataItemGenerator#getMapCurrentTreatmentlevelCasenrs()
   *                                        Map} with the current case/encounter ids by treatment
   *                                        level.
   * @param exportDirectory                 The directory to export to (e.g.:
   *                                        "C:\currentTreatmentlevelExport").
   * @param fileBaseName                    The base file name of the generated file (e.g.:
   *                                        "Caseids_inpatient_covid19_patients").
   */
  @SuppressWarnings("unused")
  public static void generateCurrentTreatmentLevelList(
      HashMap<String, List<String>> mapCurrentTreatmentlevelCasenrs, String exportDirectory,
      String fileBaseName) {

    CoronaTreatmentLevelExport treatmentLevelExport = new CoronaTreatmentLevelExport(
        mapCurrentTreatmentlevelCasenrs.get(CoronaFixedValues.NORMAL_WARD.getValue()),
        mapCurrentTreatmentlevelCasenrs.get(CoronaFixedValues.ICU.getValue()),
        mapCurrentTreatmentlevelCasenrs.get(CoronaFixedValues.ICU_VENTILATION.getValue()),
        mapCurrentTreatmentlevelCasenrs.get(CoronaFixedValues.ICU_ECMO.getValue()));

    String currentDate = new SimpleDateFormat("yyyy-MM-dd-HHmm").format(new Date());
    try (PrintWriter out = new PrintWriter(
        exportDirectory + "\\" + fileBaseName + "_" + currentDate + ".csv")) {
      out.println(treatmentLevelExport.toCsv());
    } catch (FileNotFoundException fnf) {
      log.error(
          "Unable to export file with the current treatment levels, probably because the target directory cant be created: "
              + fnf.getMessage());
    }
  }


  /**
   * Determination of all patient Ids that have at least one case with a positive SARS-CoV-2 PCR
   * laboratory result and/or SARS-CoV-2 diagnosis (U07.1 and U07.2).
   *
   * @param listUkbObservations List with the {@link UkbObservation SARS-CoV-2 lab findings}.
   * @param listUkbConditions   List with the {@link UkbCondition SARS-CoV-2 conditions}.
   * @param inputCodeSettings   The configuration of the parameterizable codes such as the
   *                            observation codes or procedure codes.
   * @return {@link HashSet} with patient Ids that have at least one case with a positive SARS-CoV-2
   * PCR laboratory result and/or SARS-CoV-2 diagnosis.
   */
  public static Set<String> getPidsPosFinding(List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions, InputCodeSettings inputCodeSettings) {
    Set<String> listPidsPosFinding = new HashSet<>();
    // Get all patient ids where the patient got at least one positive SARS-CoV-2 lab finding
    listPidsPosFinding.addAll(getPidsWithPosCovidLabResult(listUkbObservations, inputCodeSettings));
    // Get all patient ids where the patient got at least one SARS-CoV-2 related condition
    listPidsPosFinding.addAll(getPidsWithCovidDiagnosis(listUkbConditions, inputCodeSettings));

    return listPidsPosFinding;
  }

  /**
   * Identification of all patients who have at least one case with a positive SARS-CoV-2 pcr test.
   *
   * @param ukbObservations   List with the {@link UkbObservation SARS-CoV-2 lab findings}.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return List with the patients ids of all patients who have at least one case with a positive
   * SARS-CoV-2 pcr tests
   */
  public static Collection<String> getPidsWithPosCovidLabResult(
      List<UkbObservation> ukbObservations, InputCodeSettings inputCodeSettings) {

    // The extraction is based on a subset of the observations, namely just the covid related ones.
    Set<UkbObservation> covidObservations = getCovidObservations(ukbObservations,
        inputCodeSettings);

    // 1) Identification by Observation.value
    Set<String> positivePatientIds = getPatientIdsByObsValue(covidObservations, POSITIVE);

    // 2) Identification by Observation.interpretation
    positivePatientIds.addAll(
        getPatientIdsByObsInterpretation(covidObservations, POSITIVE));

    return positivePatientIds;
  }

  /**
   * Identification of all patients who have at least one case with a U07.1 or U07.2 diagnosis.
   *
   * @param listUkbConditions List with the {@link UkbCondition SARS-CoV-2 icd conditions}.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return List with the patients ids of all patients who have at least one case with a U07.1 or
   * U07.2 diagnosis
   */
  public static Set<String> getPidsWithCovidDiagnosis(List<UkbCondition> listUkbConditions,
      InputCodeSettings inputCodeSettings) {
    Set<String> pidsWithCovidDiag = new HashSet<>();
    listUkbConditions.forEach(condition -> {
      if (condition.hasCode() && isCodingValid(condition.getCode(),
          CoronaFixedValues.ICD_SYSTEM.getValue(), inputCodeSettings.getConditionIcdCodes())) {
        pidsWithCovidDiag.add(condition.getPatientId());
      }
    });
    return pidsWithCovidDiag;
  }

  /**
   * Checking if the given {@link CodeableConcept#getCoding() coding} of a
   * <code>CodeableConcept</code> got a code that is part of the given value set.
   *
   * @param codeableConcept The codings that need to be checked (e.g.
   *                        {@link UkbObservation#getCode()}).
   * @param system          The system that should be checked.
   * @param validCodes      The codes that should be checked.
   * @return <code>True</code> if the system and a valid code is part of the coding.
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

  /**
   * Determine via the discharge disposition which is part of
   * {@link UkbEncounter#getHospitalization()} whether the patient is deceased within the scope of
   * the case under review.
   *
   * @param enc An instance of an {@link UkbEncounter} object
   * @return <code>false</code>, if no valid discharge disposition can be found in the {@link
   * UkbEncounter#getHospitalization()} instance and <code>true</code> if the discharge code ("07")
   * was found
   */
  public static boolean isPatientDeceased(UkbEncounter enc) {

    Encounter.EncounterHospitalizationComponent hospComp = enc.getHospitalization();
    // check if encounter resource got a discharge disposition with a certain extension url
    if (hospComp != null && hospComp.hasDischargeDisposition() && hospComp.getDischargeDisposition()
        .hasExtension(CoronaFixedValues.DISCHARGE_DISPOSITION_EXT_URL.getValue())) {
      Extension extDischargeDisp = hospComp.getDischargeDisposition()
          .getExtensionByUrl(CoronaFixedValues.DISCHARGE_DISPOSITION_EXT_URL.getValue());
      Extension extPosFirstAndSec = extDischargeDisp.getExtensionByUrl(
          CoronaFixedValues.DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_EXT_URL.getValue());
      if (extPosFirstAndSec != null) {
        // the extension always contains a coding as value
        try {
          Coding coding = (Coding) extPosFirstAndSec.getValue();
          // If system is valid check the code right after
          if (coding.hasSystem() && coding.getSystem()
              .equals(
                  CoronaFixedValues.DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_SYSTEM.getValue())) {
            // the code must be "07" (Death)
            if (coding.hasCode() && coding.getCode()
                .equals(CoronaFixedValues.DEATH_CODE.getValue())) {
              return true;
            }
          } else {
            return false;
          }
        } catch (ClassCastException cce) {
          log.error(
              "Encounter.hospitalization.dischargeDisposition.EntlassungsgrundErsteUndZweiteStelle.value must be from type Coding but found: "
                  + extPosFirstAndSec.getValue()
                  .getClass());
        }
      }
    }

    return false;
  }

  /**
   * Increase the passed covid variant frequency by one.
   *
   * @param variantMap  Map that assigns the variant names, their (current) frequency.
   * @param variantName Name of the variant (As defined in the ValueSet of the corresponding Data
   *                    item).
   */
  public static void incrementVariantCount(Map<String, Integer> variantMap, String variantName) {
    // Merge has the advantage that the fields with the numbers in the map do not have to be initialized and the map is called only once.
    variantMap.merge(variantName, 1, Integer::sum);
  }

}
