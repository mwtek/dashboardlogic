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
package de.ukbonn.mwtek.dashboardlogic;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.BORDERLINE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.CITY_BONN;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DIVERSE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.FEMALE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.MALE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.NEGATIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_AGGREGATED;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.SUBITEMTYPE_DATE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CASENRS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_INPATIENT_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_INPATIENT_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_HOSPITAL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_ICU_ALIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_ICU_DEAD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_OUTPATIENT_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_OUTPATIENT_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_RESULTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_VARIANTTESTRESULTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL_CROSSTAB;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.INFLUENZA_PREFIX;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DEATHS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_TESTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_TEST_POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_VARIANTTESTRESULTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.Gender.DIVERSE;
import static de.ukbonn.mwtek.dashboardlogic.enums.Gender.FEMALE;
import static de.ukbonn.mwtek.dashboardlogic.enums.Gender.MALE;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus.ALIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus.DEAD;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.generateCrosstabList;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.assignSupplyEncountersToFacilityEncounter;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.createCumulativeMaxDebug;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.createCurrentIcuMap;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.createEncounterMapByClass;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.createIcuMap;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.generateSupplyContactToFacilityContactMap;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGender.getGenderCount;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGenderByClass.getGenderCountByCaseClass;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayHospital.createLengthOfStayHospitalByVitalstatus;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayHospital.createMapDaysHospitalList;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayIcu.createIcuLengthListByVitalstatus;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayIcu.createIcuLengthOfStayList;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentTreatmentLevel.getCurrentEncounterByIcuLevel;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.logic.DiseaseDetectionManagement;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CumulativeZipCode;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age.CumulativeAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.results.CumulativeResult;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.results.CumulativeVariantTestResults;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.age.CurrentMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineDeath;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineTests;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineVariantTestResults;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityEncounterToIcuSupplyContactsMap;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the Json specification are
 * generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class DataItemGenerator {

  // Initialization of the fhir resource lists
  List<UkbCondition> conditions;
  List<UkbObservation> observations;
  List<UkbPatient> patients;
  List<UkbEncounter> encounters;
  List<UkbEncounter> facilityContactEncounters;
  List<UkbProcedure> icuProcedures;
  List<UkbLocation> locations;

  // map with all inpatient-disease-positive cases, which is needed for internal reports in the UKB
  Map<String, List<String>> mapCurrentTreatmentlevelCaseNrs = new HashMap<>();

  /**
   * Initialization of the CoronaResults object with the required FhirRessource lists.
   *
   * @param conditions    List of FHIR condition resources for U07.1 and U07.2 diagnoses
   * @param observations  List of FHIR Observation Resources based on the Corona Lab Codes
   * @param patients      List of FHIR patient resources based on the Condition and Observation
   *                      inputs
   * @param encounters    List of FHIR encounter resources based on the Condition and Observation
   *                      inputs
   * @param icuProcedures List of FHIR ICU procedures resources based on the Patient/Encounter
   *                      inputs
   * @param locations     List of all FHIR location resources based on the encounter inputs
   */
  public DataItemGenerator(List<UkbCondition> conditions,
      List<UkbObservation> observations,
      List<UkbPatient> patients, List<UkbEncounter> encounters,
      List<UkbProcedure> icuProcedures, List<UkbLocation> locations) {
    super();
    this.conditions = conditions;
    this.observations = observations;
    this.patients = patients;
    this.encounters = encounters;
    this.icuProcedures = icuProcedures;
    this.locations = locations;
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems          Map with data items to be excluded from the output (e.g.
   *                                     "current.treatmentlevel").
   * @param debug                        Flag to provide debug information (e.g. casenrs) in the
   *                                     output.
   * @param variantSettings              {@link VariantSettings Configuration} of extended covid-19
   *                                     variants for the definition of not yet known/captured
   *                                     variants.
   * @param inputCodeSettings            The configuration of the parameterizable codes such as the
   *                                     observation codes or procedure codes.
   * @param usePartOfInsteadOfIdentifier Should the Encounter.partOf value be used additionally of
   *                                     the visit-number in {@link UkbEncounter#getIdentifier()} to
   *                                     assign 'Versorgungsstellenkontakt' to
   *                                     'Einrichtungskontakt'?
   * @return List with all the {@link DiseaseDataItem data items} that are defined in the corona
   * dashboard json specification
   */
  @SuppressWarnings("unused")
  public List<DiseaseDataItem> getDataItems(Map<String, Boolean> mapExcludeDataItems,
      Boolean debug, VariantSettings variantSettings, InputCodeSettings inputCodeSettings,
      DataItemContext dataItemContext, Boolean usePartOfInsteadOfIdentifier) {
    List<DiseaseDataItem> currentDataList = new ArrayList<>();
    if (mapExcludeDataItems == null) {
      mapExcludeDataItems = new HashMap<>();
    }

    if (debug == null) {
      debug = false;
    }

    // If there are resources with unfilled mandatory attributes, report them immediately (may give
    // partially reduced result sets)
    reportMissingFields(encounters);

    // Group the encounter resources by facility type
    facilityContactEncounters = encounters.parallelStream()
        .filter(UkbEncounter::isFacilityContact).toList();

    // Logging of unexpected attribute assignments within the resources.
    reportAttributeArtifacts(facilityContactEncounters);

    // To obtain the case transfer history of an encounter, we need to check the supply level
    List<UkbEncounter> supplyContactEncounters = encounters.parallelStream()
        .filter(UkbEncounter::isSupplyContact).toList();

    // Department contacts are just needed if we need to determine encounter hierarchy via .partOf
    List<UkbEncounter> departmentContactEncounters = encounters.parallelStream()
        .filter(UkbEncounter::isDepartmentContact).toList();

    Map<String, String> supplyContactIdFacilityContactId =
        generateSupplyContactToFacilityContactMap(
            supplyContactEncounters, departmentContactEncounters, facilityContactEncounters,
            usePartOfInsteadOfIdentifier);

    // Marking encounter resources as positive via setting of extensions
    DiseaseDetectionManagement.flagEncounters(encounters, conditions,
        observations, inputCodeSettings, dataItemContext);

    Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass =
        createEncounterMapByClass(facilityContactEncounters);

    // the icu information is part of the supply contact
    List<UkbEncounter> icuSupplyContactEncounters =
        supplyContactEncounters.stream()
            .filter(x -> x.isIcuCase(LocationFilter.getIcuLocationIds(locations)))
            .toList();

    // List of stationary Cases
    List<UkbEncounter> inpatientEncounters =
        encounters.parallelStream().filter(UkbEncounter::isCaseClassInpatient)
            .toList();

    FacilityEncounterToIcuSupplyContactsMap facilityEncounterIdToIcuSupplyContactsMap =
        assignSupplyEncountersToFacilityEncounter(
            icuSupplyContactEncounters, inpatientEncounters);

    Map<TreatmentLevels, List<UkbEncounter>> mapIcuDiseasePositiveOverall =
        createIcuMap(encounters, icuSupplyContactEncounters, locations,
            icuProcedures, inputCodeSettings);

    /* used for current logic */
    Map<TreatmentLevels, List<UkbEncounter>> mapCurrentIcuDiseasePositive =
        createCurrentIcuMap(mapIcuDiseasePositiveOverall);

    // CurrentLogic Classes
    DashboardDataItemLogics.initializeData(inputCodeSettings, encounters, patients, observations,
        locations, icuProcedures, dataItemContext);

    // Partial lists of current cases broken down by case status
    List<UkbEncounter> currentStandardWardEncounters = new ArrayList<>();
    List<UkbEncounter> currentIcuEncounters = new ArrayList<>();
    List<UkbEncounter> currentVentEncounters = new ArrayList<>();
    List<UkbEncounter> currentEcmoEncounters = new ArrayList<>();

    // Lists of current maxtreatmentlevels
    List<UkbEncounter> currentMaxStationary = new ArrayList<>();
    List<UkbEncounter> currentMaxIcu;
    List<UkbEncounter> currentMaxIcuVent;
    List<UkbEncounter> currentMaxIcuEcmo;

    // Lists of current maxtreatmentlevels with ages
    List<Long> currentMaxStationaryAgeList;
    List<Long> currentMaxIcuAgeList;
    List<Long> currentMaxIcuVentAgeList;
    List<Long> currentMaxIcuEcmoAgeList;

    // Inpatients = NORMAL_WARD + ICU + ICU_VENT + ECMO
    // needed later for the splitting cumulative.gender into
    // cumulative.inpatient/outpatient.gender

    // same for cumulative results
    List<UkbEncounter> cumulativeOutpatientEncounters =
        new CumulativeMaxTreatmentLevel().getCumulativeByClass(mapIcuDiseasePositiveOverall,
            OUTPATIENT, mapPositiveEncounterByClass);

    List<UkbEncounter> cumulativeStandardWardEncounters =
        new CumulativeMaxTreatmentLevel().getCumulativeByClass(mapIcuDiseasePositiveOverall,
            INPATIENT, mapPositiveEncounterByClass);
    // initialize ICU data
    List<UkbEncounter> cumulativeIcuEncounters =
        new CumulativeMaxTreatmentLevel().getCumulativeByIcuLevel(mapIcuDiseasePositiveOverall,
            ICU);
    // initialize ICU_VENT data
    List<UkbEncounter> cumulativeIcuVentEncounters =
        new CumulativeMaxTreatmentLevel().getCumulativeByIcuLevel(mapIcuDiseasePositiveOverall,
            ICU_VENTILATION);
    // initialize ECMO data
    List<UkbEncounter> cumulativeIcuEcmoEncounters =
        new CumulativeMaxTreatmentLevel().getCumulativeByIcuLevel(mapIcuDiseasePositiveOverall,
            ICU_ECMO);

    removeDuplicatePids(cumulativeOutpatientEncounters, cumulativeStandardWardEncounters,
        cumulativeIcuEncounters, cumulativeIcuVentEncounters,
        cumulativeIcuEcmoEncounters);

    DiseaseDataItem cd;
    // current treatmentlevel
    String currentTreatmentlevelItemName = determineLabel(dataItemContext,
        CURRENT_TREATMENTLEVEL);
    if (isItemNotExcluded(mapExcludeDataItems, currentTreatmentlevelItemName, false)) {
      Map<String, Number> mapCurrent = new LinkedHashMap<>();
      currentStandardWardEncounters = getCurrentEncounterByIcuLevel(
          mapCurrentIcuDiseasePositive, NORMAL_WARD, icuSupplyContactEncounters);
      currentIcuEncounters = getCurrentEncounterByIcuLevel(
          mapCurrentIcuDiseasePositive, ICU, icuSupplyContactEncounters);
      currentVentEncounters = getCurrentEncounterByIcuLevel(
          mapCurrentIcuDiseasePositive, ICU_VENTILATION, icuSupplyContactEncounters);
      currentEcmoEncounters = getCurrentEncounterByIcuLevel(
          mapCurrentIcuDiseasePositive, ICU_ECMO, icuSupplyContactEncounters);
      mapCurrent.put(NORMAL_WARD.getValue(), currentStandardWardEncounters.size());
      mapCurrent.put(ICU.getValue(), currentIcuEncounters.size());
      mapCurrent.put(ICU_VENTILATION.getValue(), currentVentEncounters.size());
      mapCurrent.put(ICU_ECMO.getValue(), currentEcmoEncounters.size());

      currentDataList.add(
          new DiseaseDataItem(currentTreatmentlevelItemName, ITEMTYPE_AGGREGATED, mapCurrent));

      // Storing the case ids of the current treatmentlevel.
      // This list will be used in coming item generations.
      Map<String, List<String>> mapCurrentTreatmentlevelCaseIds =
          createMapCurrentTreatmentlevelCaseIds(currentStandardWardEncounters,
              currentIcuEncounters, currentVentEncounters, currentEcmoEncounters);
      this.setMapCurrentTreatmentlevelCaseIds(mapCurrentTreatmentlevelCaseIds);

      if (debug) {
        currentDataList.add(
            new DiseaseDataItem(addCaseNrsToLabel(currentTreatmentlevelItemName), ITEMTYPE_DEBUG,
                mapCurrentTreatmentlevelCaseIds));
      }
    }

    String currentMaxtreatmentlevelLabel = determineLabel(dataItemContext,
        CURRENT_MAXTREATMENTLEVEL);
    // current maxtreatmentlevel
    if (isItemNotExcluded(mapExcludeDataItems, currentMaxtreatmentlevelLabel, false)) {
      Map<String, Number> mapCurrentMax = new LinkedHashMap<>();
      currentMaxStationary =
          new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
              mapIcuDiseasePositiveOverall,
              INPATIENT);
      currentMaxIcu = new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
          mapIcuDiseasePositiveOverall, ICU);
      currentMaxIcuVent =
          new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
              mapIcuDiseasePositiveOverall,
              ICU_VENTILATION);
      currentMaxIcuEcmo =
          new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
              mapIcuDiseasePositiveOverall,
              ICU_ECMO);
      mapCurrentMax.put(NORMAL_WARD.getValue(), currentMaxStationary.size());
      mapCurrentMax.put(ICU.getValue(), currentMaxIcu.size());
      mapCurrentMax.put(ICU_VENTILATION.getValue(), currentMaxIcuVent.size());
      mapCurrentMax.put(ICU_ECMO.getValue(), currentMaxIcuEcmo.size());
      currentDataList.add(
          new DiseaseDataItem(currentMaxtreatmentlevelLabel, ITEMTYPE_AGGREGATED, mapCurrentMax));

      if (debug) {
        List<String> listStationaryCaseNrs = new ArrayList<>();
        List<String> listIcuCaseNrs = new ArrayList<>();
        List<String> listVentCaseNrs = new ArrayList<>();
        List<String> listEcmoCaseNrs = new ArrayList<>();
        Map<String, List<String>> resultCurrentTreatmentCaseNrs = new LinkedHashMap<>();

        currentMaxStationary.forEach(encounter -> listStationaryCaseNrs.add(encounter.getId()));
        currentMaxIcu.forEach(encounter -> listIcuCaseNrs.add(encounter.getId()));
        currentMaxIcuVent.forEach(encounter -> listVentCaseNrs.add(encounter.getId()));
        currentMaxIcuEcmo.forEach(encounter -> listEcmoCaseNrs.add(encounter.getId()));

        resultCurrentTreatmentCaseNrs.put(NORMAL_WARD.getValue(), listStationaryCaseNrs);
        resultCurrentTreatmentCaseNrs.put(ICU.getValue(), listIcuCaseNrs);
        resultCurrentTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), listVentCaseNrs);
        resultCurrentTreatmentCaseNrs.put(ICU_ECMO.getValue(), listEcmoCaseNrs);

        currentDataList.add(
            new DiseaseDataItem(addCaseNrsToLabel(currentMaxtreatmentlevelLabel), ITEMTYPE_DEBUG,
                resultCurrentTreatmentCaseNrs));
      }
    }

    String currentAgeMaxtreatmentlevelNormalWard = determineLabel(dataItemContext,
        CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
    // current.age.maxtreatmentlevel.normal_ward
    if (isItemNotExcluded(mapExcludeDataItems, currentAgeMaxtreatmentlevelNormalWard, false)) {

      if (mapExcludeDataItems.getOrDefault(currentMaxtreatmentlevelLabel, false)) {
        currentMaxStationary =
            new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
                mapIcuDiseasePositiveOverall, INPATIENT);
      }
      CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
          new CurrentMaxTreatmentLevelAge(patients, currentMaxStationary);
      currentDataList.add(new DiseaseDataItem(currentAgeMaxtreatmentlevelNormalWard, ITEMTYPE_LIST,
          currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass)));
    }
    // current.age.maxtreatmentlevel.icu
    String currentAgeMaxtreatmentlevelIcuLabel = determineLabel(dataItemContext,
        CURRENT_AGE_MAXTREATMENTLEVEL_ICU);
    if (isItemNotExcluded(mapExcludeDataItems, currentAgeMaxtreatmentlevelIcuLabel, false)) {
      // This item is entirely depending on current.maxtreatmentlevel
      if (isItemNotExcluded(mapExcludeDataItems, currentMaxtreatmentlevelLabel, false)) {
        currentMaxIcu =
            new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
                mapIcuDiseasePositiveOverall, ICU);
        CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
            new CurrentMaxTreatmentLevelAge(patients, currentMaxIcu);
        currentDataList.add(new DiseaseDataItem(currentAgeMaxtreatmentlevelIcuLabel, ITEMTYPE_LIST,
            currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass)));
      }

    }
    // current.age.maxtreatmentlevel.icu_with_ventilation
    String currentAgeMaxtreatmentlevelIcuVent = determineLabel(dataItemContext,
        CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION);
    if (isItemNotExcluded(mapExcludeDataItems, currentAgeMaxtreatmentlevelIcuVent, false)) {
      // This item is entirely depending on current.maxtreatmentlevel
      if (isItemNotExcluded(mapExcludeDataItems, currentMaxtreatmentlevelLabel, false)) {
        currentMaxIcuVent = new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
            mapIcuDiseasePositiveOverall,
            ICU_VENTILATION);
        CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
            new CurrentMaxTreatmentLevelAge(patients, currentMaxIcuVent);
        currentDataList.add(new DiseaseDataItem(currentAgeMaxtreatmentlevelIcuVent, ITEMTYPE_LIST,
            currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass)));
      }
    }
    // current.age.maxtreatmentlevel.icu_with_ecmo
    String currentAgeMaxtreatmentlevelIcuEcmo = determineLabel(dataItemContext,
        CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO);
    if (isItemNotExcluded(mapExcludeDataItems, currentAgeMaxtreatmentlevelIcuEcmo, false)) {
      // This item is entirely depending on current.maxtreatmentlevel
      if (isItemNotExcluded(mapExcludeDataItems, currentMaxtreatmentlevelLabel, false)) {
        currentMaxIcuEcmo =
            new CurrentMaxTreatmentLevel().getNumberOfCurrentMaxTreatmentLevel(
                mapIcuDiseasePositiveOverall, ICU_ECMO);
        CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
            new CurrentMaxTreatmentLevelAge(patients, currentMaxIcuEcmo);
        currentDataList.add(new DiseaseDataItem(currentAgeMaxtreatmentlevelIcuEcmo, ITEMTYPE_LIST,
            currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass)));
      }
    }

    // cumulative.results
    String cumulativeResultsLabel = determineLabel(dataItemContext, CUMULATIVE_RESULTS);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeResultsLabel, false)) {
      Set<UkbObservation> setPositiveObservations =
          new CumulativeResult().getObservationsByResult(POSITIVE, dataItemContext);
      Set<UkbObservation> setNegativeObservations =
          new CumulativeResult().getObservationsByResult(NEGATIVE, dataItemContext);
      Set<UkbObservation> setBorderlineLabCaseNrs =
          new CumulativeResult().getObservationsByResult(BORDERLINE, dataItemContext);
      Map<String, Number> cumulativeResultMap = new LinkedHashMap<>();
      cumulativeResultMap.put(POSITIVE.getValue(), setPositiveObservations.size());
      cumulativeResultMap.put(BORDERLINE.getValue(), setBorderlineLabCaseNrs.size());
      cumulativeResultMap.put(NEGATIVE.getValue(), setNegativeObservations.size());
      currentDataList.add(
          new DiseaseDataItem(cumulativeResultsLabel, ITEMTYPE_AGGREGATED, cumulativeResultMap));
    }

    String cumulativeGenderLabel = determineLabel(dataItemContext, CUMULATIVE_GENDER);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeGenderLabel, false)) {
      // cumulative.gender
      Map<String, Number> cumulativeGenderMap = new LinkedHashMap<>();

      cumulativeGenderMap.put(MALE_SPECIFICATION.getValue(),
          getGenderCount(facilityContactEncounters, MALE));
      cumulativeGenderMap.put(FEMALE_SPECIFICATION.getValue(),
          getGenderCount(facilityContactEncounters, FEMALE));
      cumulativeGenderMap.put(DIVERSE_SPECIFICATION.getValue(),
          getGenderCount(facilityContactEncounters, DIVERSE));
      currentDataList.add(
          new DiseaseDataItem(cumulativeGenderLabel, ITEMTYPE_AGGREGATED, cumulativeGenderMap));
    }

    // cumulative.age
    String cumulativeAgeLabel = determineLabel(dataItemContext, CUMULATIVE_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeAgeLabel, false)) {
      currentDataList.add(new DiseaseDataItem(cumulativeAgeLabel, ITEMTYPE_LIST,
          CumulativeAge.getAgeDistributionsByCaseClass(TreatmentLevels.ALL).toArray()));
    }

    // cumulative.maxtreatmentlevel
    String cumulativeMaxTreatmentLevelLabel = determineLabel(dataItemContext,
        CUMULATIVE_MAXTREATMENTLEVEL);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeMaxTreatmentLevelLabel, false)) {
      Map<String, Number> mapCumulativeMaxtreatmentlevel = new LinkedHashMap<>();

      mapCumulativeMaxtreatmentlevel.put(OUTPATIENT.getValue(),
          cumulativeOutpatientEncounters.size());
      mapCumulativeMaxtreatmentlevel.put(NORMAL_WARD.getValue(),
          cumulativeStandardWardEncounters.size());
      mapCumulativeMaxtreatmentlevel.put(ICU.getValue(), cumulativeIcuEncounters.size());
      mapCumulativeMaxtreatmentlevel.put(ICU_VENTILATION.getValue(),
          cumulativeIcuVentEncounters.size());
      mapCumulativeMaxtreatmentlevel.put(ICU_ECMO.getValue(), cumulativeIcuEcmoEncounters.size());

      currentDataList.add(new DiseaseDataItem(cumulativeMaxTreatmentLevelLabel, ITEMTYPE_AGGREGATED,
          mapCumulativeMaxtreatmentlevel));

      // adding case ids on demand
      if (debug) {
        Map<String, Map<String, List<String>>> resultMaxTreatmentCaseNrs =
            new LinkedHashMap<>();

        resultMaxTreatmentCaseNrs.put(OUTPATIENT.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(NORMAL_WARD.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(ICU.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(ICU_ECMO.getValue(), new HashMap<>());

        createCumulativeMaxDebug(cumulativeOutpatientEncounters, OUTPATIENT.getValue(),
            resultMaxTreatmentCaseNrs);
        createCumulativeMaxDebug(cumulativeStandardWardEncounters, NORMAL_WARD.getValue(),
            resultMaxTreatmentCaseNrs);
        createCumulativeMaxDebug(cumulativeIcuEncounters, ICU.getValue(),
            resultMaxTreatmentCaseNrs);
        createCumulativeMaxDebug(cumulativeIcuVentEncounters, ICU_VENTILATION.getValue(),
            resultMaxTreatmentCaseNrs);
        createCumulativeMaxDebug(cumulativeIcuEcmoEncounters, ICU_ECMO.getValue(),
            resultMaxTreatmentCaseNrs);

        currentDataList.add(
            new DiseaseDataItem(addCaseNrsToLabel(cumulativeMaxTreatmentLevelLabel), ITEMTYPE_DEBUG,
                resultMaxTreatmentCaseNrs));
      }
    }
    String cumulativeAgeMaxTreatmentlevelOutpatientLabel = determineLabel(dataItemContext,
        CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT);
    // cumulative.age.maxtreatmentlevel.outpatient
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeAgeMaxTreatmentlevelOutpatientLabel,
        false)) {
      List<Integer> cumulativeMaxtreatmentlevelOutpatientAgeList =
          new CumulativeMaxTreatmentLevelAge().createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcuDiseasePositiveOverall, OUTPATIENT);
      currentDataList.add(
          new DiseaseDataItem(cumulativeAgeMaxTreatmentlevelOutpatientLabel, ITEMTYPE_LIST,
              cumulativeMaxtreatmentlevelOutpatientAgeList));
    }
    String cumulativeAgeMaxTreatmentlevelNormalWardLabel = determineLabel(dataItemContext,
        CUMULATE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
    // cumulative.age.maxtreatmentlevel.normal_ward
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeAgeMaxTreatmentlevelNormalWardLabel,
        false)) {
      List<Integer> cumulativeMaxtreatmentlevelNormalWardAgeList =
          new CumulativeMaxTreatmentLevelAge().createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcuDiseasePositiveOverall, NORMAL_WARD);
      currentDataList.add(
          new DiseaseDataItem(cumulativeAgeMaxTreatmentlevelNormalWardLabel, ITEMTYPE_LIST,
              cumulativeMaxtreatmentlevelNormalWardAgeList));
    }
    // cumulative.age.maxtreatmentlevel.icu
    String cumulativeAgeMaxTreatmentlevelIcuLabel = determineLabel(dataItemContext,
        CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeAgeMaxTreatmentlevelIcuLabel, false)) {
      List<Integer> cumulativeMaxtreatmentlevelIcuAgeList =
          new CumulativeMaxTreatmentLevelAge().createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcuDiseasePositiveOverall, ICU);
      currentDataList.add(new DiseaseDataItem(cumulativeAgeMaxTreatmentlevelIcuLabel, ITEMTYPE_LIST,
          cumulativeMaxtreatmentlevelIcuAgeList));
    }
    String cumulativeAgeMaxTreatmentlevelVentLabel = determineLabel(dataItemContext,
        CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION);
    // cumulative.age.maxtreatmentlevel.icu_with_ventilation
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeAgeMaxTreatmentlevelVentLabel, false)) {
      List<Integer> cumulativeMaxtreatmentlevelIcuVentAgeList =
          new CumulativeMaxTreatmentLevelAge().createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcuDiseasePositiveOverall, ICU_VENTILATION);
      currentDataList.add(
          new DiseaseDataItem(cumulativeAgeMaxTreatmentlevelVentLabel, ITEMTYPE_LIST,
              cumulativeMaxtreatmentlevelIcuVentAgeList));
    }
    // cumulative.age.maxtreatmentlevel.icu_with_ecmo
    String cumulativeAgeMaxTreatmentlevelEcmoLabel = determineLabel(dataItemContext,
        CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeAgeMaxTreatmentlevelEcmoLabel, false)) {
      List<Integer> cumulativeMaxtreatmentlevelIcuEcmoAgeList =
          new CumulativeMaxTreatmentLevelAge().createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcuDiseasePositiveOverall, ICU_ECMO);
      currentDataList.add(
          new DiseaseDataItem(cumulativeAgeMaxTreatmentlevelEcmoLabel, ITEMTYPE_LIST,
              cumulativeMaxtreatmentlevelIcuEcmoAgeList));
    }
    // cumulative zip code
    String cumulativeZipCodeLabel = determineLabel(dataItemContext,
        CUMULATIVE_ZIPCODE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeZipCodeLabel, false)) {
      currentDataList.add(new DiseaseDataItem(cumulativeZipCodeLabel, ITEMTYPE_LIST,
          new CumulativeZipCode().createZipCodeList()));
    }

    // timeline tests
    String timelineTestsLabel = determineLabel(dataItemContext, TIMELINE_TESTS);
    if (isItemNotExcluded(mapExcludeDataItems, timelineTestsLabel, false)) {
      currentDataList.add(new DiseaseDataItem(timelineTestsLabel, ITEMTYPE_LIST,
          new TimelineTests().createTimelineTestsMap(dataItemContext)));
    }

    // timeline.test.positive
    String timelineTestPositiveLabel = determineLabel(dataItemContext, TIMELINE_TEST_POSITIVE);
    if (isItemNotExcluded(mapExcludeDataItems, timelineTestPositiveLabel, false)) {
      TimestampedListPair timelineTestPositivePair =
          new TimelineTests().createTimelineTestPositiveMap(
              dataItemContext);
      currentDataList.add(
          new DiseaseDataItem(timelineTestPositiveLabel, ITEMTYPE_LIST, timelineTestPositivePair));
    }

    // timeline.maxtreatmentlevel
    String timelineMaxtreatmentlevelLabel = determineLabel(dataItemContext,
        TIMELINE_MAXTREATMENTLEVEL);
    if (isItemNotExcluded(mapExcludeDataItems, timelineMaxtreatmentlevelLabel, false)) {
      Map<TreatmentLevels, Map<Long, Set<String>>> resultMaxTreatmentTimeline;
      Map<String, List<Long>> mapResultTreatment = new LinkedHashMap<>();

      // The result contains the case numbers for debugging and the sums by date
      resultMaxTreatmentTimeline = TimelineMaxTreatmentLevel.createMaxTreatmentTimeline();
      mapResultTreatment.put(SUBITEMTYPE_DATE, getDatesOutputList());

      for (Map.Entry<TreatmentLevels, Map<Long, Set<String>>> entry :
          resultMaxTreatmentTimeline.entrySet()) {
        for (Map.Entry<Long, Set<String>> secondEntry : entry.getValue().entrySet()) {
          addValuesToTimelineMaxMap(entry.getKey().getValue(), secondEntry.getValue(),
              mapResultTreatment);
        }
      }
      currentDataList.add(
          new DiseaseDataItem(timelineMaxtreatmentlevelLabel, ITEMTYPE_LIST, mapResultTreatment));

      // timeline maxtreatmentlevel
      // case ids can be shown for plausibility checks
      if (debug) {
        Map<String, Map<Long, Set<String>>> resultMaxTreatmentCaseNrs = new LinkedHashMap<>();
        for (Map.Entry<TreatmentLevels, Map<Long, Set<String>>> entry :
            resultMaxTreatmentTimeline.entrySet()) {
          TreatmentLevels caseContext = entry.getKey();
          Map<Long, Set<String>> mapDateAndCaseNr = entry.getValue();
          resultMaxTreatmentCaseNrs.put(caseContext.getValue(), mapDateAndCaseNr);
        }
        // The date would be redundant in the debug information but is needed, since the rest
        // endpoint is not accepting lists of type item without date lists
        Map<Long, Set<String>> mapTempAndDate = new LinkedHashMap<>();
        resultMaxTreatmentCaseNrs.put(SUBITEMTYPE_DATE, mapTempAndDate);

        currentDataList.add(
            new DiseaseDataItem(addCaseNrsToLabel(timelineMaxtreatmentlevelLabel), ITEMTYPE_DEBUG,
                resultMaxTreatmentCaseNrs));
      } // if
    }

    // cumulative inpatient gender
    String cumulativeInpatientGenderLabel = determineLabel(dataItemContext,
        CUMULATIVE_INPATIENT_GENDER);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeInpatientGenderLabel, false)) {
      Map<String, Number> cumulativeInpatientGenderMap = new HashMap<>();
      // cumulativeInpatients gets initialized in the cumulative.maxtreatmentlevel method ->
      // this method could be excluded
      cumulativeInpatientGenderMap.put(MALE_SPECIFICATION.getValue(),
          getGenderCountByCaseClass(MALE, INPATIENT));
      cumulativeInpatientGenderMap.put(FEMALE_SPECIFICATION.getValue(),
          getGenderCountByCaseClass(FEMALE, INPATIENT));
      cumulativeInpatientGenderMap.put(DIVERSE_SPECIFICATION.getValue(),
          getGenderCountByCaseClass(DIVERSE, INPATIENT));

      currentDataList.add(new DiseaseDataItem(cumulativeInpatientGenderLabel, ITEMTYPE_AGGREGATED,
          cumulativeInpatientGenderMap));
    }

    // cumulative outpatient gender
    String cumulativeOutpatientGenderLabel = determineLabel(dataItemContext,
        CUMULATIVE_OUTPATIENT_GENDER);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeOutpatientGenderLabel, false)) {
      Map<String, Number> cumulativeOutpatientGender = new HashMap<>();
      cumulativeOutpatientGender.put(MALE_SPECIFICATION.getValue(),
          getGenderCountByCaseClass(MALE, OUTPATIENT));
      cumulativeOutpatientGender.put(FEMALE_SPECIFICATION.getValue(),
          getGenderCountByCaseClass(FEMALE, OUTPATIENT));
      cumulativeOutpatientGender.put(DIVERSE_SPECIFICATION.getValue(),
          getGenderCountByCaseClass(DIVERSE, OUTPATIENT));

      currentDataList.add(new DiseaseDataItem(cumulativeOutpatientGenderLabel, ITEMTYPE_AGGREGATED,
          cumulativeOutpatientGender));
    }

    String cumulativeInpatientAge = determineLabel(dataItemContext, CUMULATIVE_INPATIENT_AGE);
    // cumulative inpatient age
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeInpatientAge, false)) {
      currentDataList.add(new DiseaseDataItem(cumulativeInpatientAge, ITEMTYPE_LIST,
          CumulativeAge.getAgeDistributionsByCaseClass(INPATIENT).toArray()));
    }

    // cumulative outpatient age
    String cumulativeOutpatientAgeLabel = determineLabel(dataItemContext,
        CUMULATIVE_OUTPATIENT_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeOutpatientAgeLabel, false)) {
      currentDataList.add(new DiseaseDataItem(cumulativeOutpatientAgeLabel, ITEMTYPE_LIST,
          CumulativeAge.getAgeDistributionsByCaseClass(OUTPATIENT).toArray()));
    }

    // timeline deaths
    String timelineDeathsLabel = determineLabel(dataItemContext, TIMELINE_DEATHS);
    if (isItemNotExcluded(mapExcludeDataItems, timelineDeathsLabel, false)) {
      currentDataList.add(new DiseaseDataItem(timelineDeathsLabel, ITEMTYPE_LIST,
          new TimelineDeath().createTimelineDeathMap()));
    }

    // Bonn cross-table calculation; currently just needed in the covid-19-context
    if (dataItemContext == DataItemContext.COVID) {
      if (isItemNotExcluded(mapExcludeDataItems, CURRENT_TREATMENTLEVEL_CROSSTAB, true)) {
        Map<String, List> aggData = new HashMap<>();
        aggData.put("columnname",
            Arrays.asList(CITY_BONN.getValue(), ICU.getValue(), ICU_VENTILATION.getValue(),
                ICU_ECMO.getValue()));
        aggData.put("state",
            Arrays.asList("0000", "0100", "0110", "0111", "1000", "1100", "1110", "1111"));
        aggData.put("value", new ArrayList<>());
        Map<TreatmentLevels, List<UkbEncounter>> crosstabMaxtreatmentlevels =
            new LinkedHashMap<>();
        crosstabMaxtreatmentlevels.put(INPATIENT, currentStandardWardEncounters);
        crosstabMaxtreatmentlevels.put(ICU, currentIcuEncounters);
        List<UkbEncounter> ventEncounters = new ArrayList<>(currentVentEncounters);
        crosstabMaxtreatmentlevels.put(ICU_VENTILATION, ventEncounters);
        List<UkbEncounter> ecmoEncounters = new ArrayList<>(currentEcmoEncounters);
        crosstabMaxtreatmentlevels.put(ICU_ECMO, ecmoEncounters);

        Set<UkbEncounter> allCurrentEncounters = new HashSet<>();

        allCurrentEncounters.addAll(currentStandardWardEncounters);
        allCurrentEncounters.addAll(currentIcuEncounters);
        allCurrentEncounters.addAll(ventEncounters);
        allCurrentEncounters.addAll(ecmoEncounters);

        Set<String> encounterPids =
            allCurrentEncounters.stream().map(UkbEncounter::getPatientId)
                .collect(Collectors.toSet());

        List<UkbPatient> currentPatients = patients.stream()
            .filter(patient -> encounterPids.contains(patient.getId()))
            .collect(Collectors.toList());
        List<String[]> ukbCrossTabList =
            generateCrosstabList(crosstabMaxtreatmentlevels,
                currentPatients);
        if (debug) {
          aggData.put("casenrs", ukbCrossTabList);
        }

        List<String> values = new ArrayList<>();
        for (String[] nr : ukbCrossTabList) {
          values.add(String.valueOf(nr.length));
        }
        aggData.put("value", values);
        currentDataList.add(
            new DiseaseDataItem(determineLabel(dataItemContext, CURRENT_TREATMENTLEVEL_CROSSTAB),
                ITEMTYPE_AGGREGATED, aggData));
      }
    }

    // list with all lengths of icu stays (in h)
    // cumulative.lengthofstay.icu
    String cumulativeLengthOfStayIcuLabel = determineLabel(dataItemContext,
        CUMULATIVE_LENGTHOFSTAY_ICU);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeLengthOfStayIcuLabel, false)) {
      Map<String, Map<Long, Set<String>>> mapIcuLengthList = createIcuLengthOfStayList(
          icuSupplyContactEncounters);
      createCumulativeLengthOfStayIcuData(cumulativeLengthOfStayIcuLabel,
          mapIcuDiseasePositiveOverall,
          debug, null, currentDataList, mapIcuLengthList);

      // list with all lengths of icu stays (in h)
      String cumulativeLengthOfStayIcuAliveLabel = determineLabel(dataItemContext,
          CUMULATIVE_LENGTHOFSTAY_ICU_ALIVE);
      if (isItemNotExcluded(mapExcludeDataItems, cumulativeLengthOfStayIcuAliveLabel, false)) {
        createCumulativeLengthOfStayIcuData(cumulativeLengthOfStayIcuAliveLabel,
            mapIcuDiseasePositiveOverall,
            debug, ALIVE, currentDataList, mapIcuLengthList);
      }

      // list with all lengths of icu stays (in h)
      String cumulativeLengthOfStayIcuDeadLabel = determineLabel(dataItemContext,
          CUMULATIVE_LENGTHOFSTAY_ICU_DEAD);
      if (isItemNotExcluded(mapExcludeDataItems, cumulativeLengthOfStayIcuDeadLabel, false)) {
        createCumulativeLengthOfStayIcuData(cumulativeLengthOfStayIcuDeadLabel,
            mapIcuDiseasePositiveOverall,
            debug, DEAD, currentDataList, mapIcuLengthList);
      }
    }

    // cumulative length of stays
    String cumulativeLengthOfStayHospitalLabel = determineLabel(dataItemContext,
        CUMULATIVE_LENGTHOFSTAY_HOSPITAL);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeLengthOfStayHospitalLabel, false)) {
      Map<String, Map<Long, Set<String>>> mapDays = createMapDaysHospitalList();
      createCumulativeLengthOfStayHospitalData(cumulativeLengthOfStayHospitalLabel,
          mapDays, debug, null, currentDataList);
      // cumulative length of stays ALIVE
      String cumulativeLengthOfStayHospitalAliveLabel = determineLabel(dataItemContext,
          CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE);
      if (isItemNotExcluded(mapExcludeDataItems, cumulativeLengthOfStayHospitalAliveLabel, false)) {
        createCumulativeLengthOfStayHospitalData(cumulativeLengthOfStayHospitalAliveLabel,
            mapDays, debug, ALIVE, currentDataList);
      }
      // cumulative.lengthofstay.dead
      String cumulativeLengthOfStayHospitalDeadLabel = determineLabel(dataItemContext,
          CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD);
      if (isItemNotExcluded(mapExcludeDataItems, cumulativeLengthOfStayHospitalDeadLabel, false)) {
        createCumulativeLengthOfStayHospitalData(cumulativeLengthOfStayHospitalDeadLabel,
            mapDays, debug, DEAD, currentDataList);
      }
    }

    // Handling of items that restricted to covid-19 context
    if (dataItemContext == DataItemContext.COVID) {
      // cumulative varianttestresults
      if (isItemNotExcluded(mapExcludeDataItems, CUMULATIVE_VARIANTTESTRESULTS, false)) {
        Map<String, Integer> resultMap =
            new CumulativeVariantTestResults().createVariantTestResultMap(variantSettings,
                inputCodeSettings);
        currentDataList.add(
            new DiseaseDataItem(CUMULATIVE_VARIANTTESTRESULTS, ITEMTYPE_AGGREGATED, resultMap));
      }

      // timeline.varianttestresults
      if (isItemNotExcluded(mapExcludeDataItems, TIMELINE_VARIANTTESTRESULTS, false)) {
        currentDataList.add(new DiseaseDataItem(TIMELINE_VARIANTTESTRESULTS, ITEMTYPE_LIST,
            new TimelineVariantTestResults().createTimelineVariantsTests(variantSettings,
                inputCodeSettings)));
      }
    } // if
    return currentDataList;
  }

  private Map<String, Boolean> getAllDataItemsThatNeedSupplyContacts() {
    Map<String, Boolean> output = new HashMap<>();
    output.put(CURRENT_TREATMENTLEVEL, true);
    output.put(CURRENT_MAXTREATMENTLEVEL, true);
    output.put(TIMELINE_MAXTREATMENTLEVEL, true);
    return output;
  }

  private static boolean isItemNotExcluded(Map<String, Boolean> mapExcludeDataItems,
      String currentMaxtreatmentlevelLabel, boolean defaultValue) {
    return !mapExcludeDataItems.getOrDefault(currentMaxtreatmentlevelLabel, defaultValue);
  }

  private String addCaseNrsToLabel(String dataItem) {
    return dataItem + "." + CASENRS;
  }

  private void reportMissingFields(List<UkbEncounter> encounters) {
    // Encounter.period.start is mandatory and critical for many data items
    List<String> casesWithoutPeriodStart =
        encounters.parallelStream().filter(x -> !x.isPeriodStartExistent())
            .map(UkbEncounter::getCaseId).toList();
    if (!casesWithoutPeriodStart.isEmpty()) {
      log.debug(
          "Warning: " + casesWithoutPeriodStart.size()
              + " Encounters without period/period.start element have been detected [for example "
              + "case with id: "
              + casesWithoutPeriodStart.get(
              0) + "]");
    }
  }

  private void reportAttributeArtifacts(List<UkbEncounter> encounters) {
    // Pre-stationary and post-stationary encounter must have class = AMB
    List<String> preOrPostEncounterNotOutpatient =
        encounters.parallelStream().filter(UkbEncounter::isCaseClassInpatient)
            .filter(x -> x.isCaseTypePreStationary() || x.isCaseTypePostStationary())
            .map(UkbEncounter::getId).toList();
    if (!preOrPostEncounterNotOutpatient.isEmpty()) {
      log.debug(
          "Warning: " + preOrPostEncounterNotOutpatient.size()
              + " Encounters found where pre-/poststationary encounter don't have class 'AMB' "
              + "[for example Encounter/"
              + preOrPostEncounterNotOutpatient.get(
              0) + "]");
    }
  }

  private void addValuesToTimelineMaxMap(String item, Set<String> value,
      Map<String, List<Long>> mapResultTreatment) {
    List<Long> tempList = new ArrayList<>();
    tempList.add((long) value.size());

    // this is for any other DataItem
    if (mapResultTreatment.containsKey(item)) {
      mapResultTreatment.get(item).addAll(tempList);
    } else {
      mapResultTreatment.put(item, tempList);
    }
  }

  /**
   * Checking if there are duplicate pids in different treatment levels removes them if they are in
   * the wrong list
   *
   * @param cumulativeOutpatientEncounter   List that contains all ambulant cases needed for the
   *                                        cumulative logic
   * @param cumulativeStandardWardEncounter List that contains all stationary cases needed for the
   *                                        cumulative logic
   * @param cumulativeIcuEncounter          List that contains all ICU cases needed for the
   *                                        cumulative logic
   * @param cumulativeIcuVentEncounter      List that contains all Ventilation cases needed for the
   *                                        cumulative logic
   * @param cumulativeIcuEcmoEncounter      List that contains all ECMO cases needed for the
   *                                        cumulative logic
   */
  private void removeDuplicatePids(List<UkbEncounter> cumulativeOutpatientEncounter,
      List<UkbEncounter> cumulativeStandardWardEncounter,
      List<UkbEncounter> cumulativeIcuEncounter,
      List<UkbEncounter> cumulativeIcuVentEncounter,
      List<UkbEncounter> cumulativeIcuEcmoEncounter) {
    Set<String> pidsOutpatient = createPidList(cumulativeOutpatientEncounter);
    Set<String> pidsStandardWard = createPidList(cumulativeStandardWardEncounter);
    Set<String> pidsIcu = createPidList(cumulativeIcuEncounter);
    Set<String> pidsVent = createPidList(cumulativeIcuVentEncounter);
    Set<String> pidsEcmo = createPidList(cumulativeIcuEcmoEncounter);

    pidsOutpatient.removeAll(pidsStandardWard);
    pidsOutpatient.removeAll(pidsIcu);
    pidsOutpatient.removeAll(pidsVent);
    pidsOutpatient.removeAll(pidsEcmo);

    cumulativeOutpatientEncounter.removeIf(e -> !pidsOutpatient.contains(e.getPatientId()));

    pidsStandardWard.removeAll(pidsIcu);
    pidsStandardWard.removeAll(pidsVent);
    pidsStandardWard.removeAll(pidsEcmo);

    cumulativeStandardWardEncounter.removeIf(
        e -> !pidsStandardWard.contains(e.getPatientId()));

    pidsIcu.removeAll(pidsVent);
    pidsIcu.removeAll(pidsEcmo);

    cumulativeIcuEncounter.removeIf(e -> !pidsIcu.contains(e.getPatientId()));

    pidsVent.removeAll(pidsEcmo);

    cumulativeIcuVentEncounter.removeIf(e -> !pidsVent.contains(e.getPatientId()));
  }

  private Set<String> createPidList(List<UkbEncounter> listCumulativeEncounter) {
    return listCumulativeEncounter.parallelStream().map(UkbEncounter::getPatientId)
        .collect(Collectors.toSet());
  }


  private void setMapCurrentTreatmentlevelCaseIds(
      Map<String, List<String>> mapCurrentTreatmentlevelCaseNrs) {
    this.mapCurrentTreatmentlevelCaseNrs = mapCurrentTreatmentlevelCaseNrs;
  }

  public Map<String, List<String>> getMapCurrentTreatmentlevelCasenrs() {
    return mapCurrentTreatmentlevelCaseNrs;
  }

  /**
   * Some data items generate the same output, but are based on different data contexts. Different
   * prefixes are set depending on this context.
   */
  public static String determineLabel(DataItemContext context, String defaultLabel) {
    return switch (context) {
      case COVID -> defaultLabel;
      case INFLUENZA -> INFLUENZA_PREFIX + defaultLabel;
      default -> defaultLabel;
    };
  }

  /**
   * Creates a map that associates each treatment level with a list of case numbers. This map is
   * useful for debugging purposes and internal reports at the UKB.
   *
   * @param standardWardEncounters List of all normal inpatient {@link UkbEncounter} resources that
   *                               include active disease-positive cases.
   * @param icuEncounters          List of all inpatient {@link UkbEncounter} resources that include
   *                               active disease-positive cases and are in an ICU ward.
   * @param ventEncounters         List of all {@link UkbEncounter} resources that include active
   *                               disease-positive cases with an active ventilation period (and no
   *                               active ECMO).
   * @param ecmoEncounters         List of all {@link UkbEncounter} resources that include active
   *                               disease-positive cases with an active ECMO period.
   * @return Map that connects treatment levels with a list of case numbers.
   */
  private Map<String, List<String>> createMapCurrentTreatmentlevelCaseIds(
      List<UkbEncounter> standardWardEncounters, List<UkbEncounter> icuEncounters,
      List<UkbEncounter> ventEncounters, List<UkbEncounter> ecmoEncounters) {

    // Lists to store case numbers for different treatment levels
    List<String> stationaryCaseNrs = new ArrayList<>();
    List<String> icuCaseNrs = new ArrayList<>();
    List<String> ventCaseNrs = new ArrayList<>();
    List<String> ecmoCaseNrs = new ArrayList<>();

    // Result map to connect treatment levels with case numbers
    Map<String, List<String>> resultCurrentTreatmentCaseNrs = new LinkedHashMap<>();

    // Creating a list with the subset for all positive stationary (needed in age calculations)
    standardWardEncounters.forEach(encounter -> {
      if (encounter.isCaseClassInpatient() && encounter.hasExtension(POSITIVE_RESULT.getValue())) {
        stationaryCaseNrs.add(encounter.getId());
      }
    });

    // Adding case numbers to the respective lists for each treatment level
    icuEncounters.forEach(encounter -> icuCaseNrs.add(encounter.getId()));
    ventEncounters.forEach(encounter -> ventCaseNrs.add(encounter.getId()));
    ecmoEncounters.forEach(encounter -> ecmoCaseNrs.add(encounter.getId()));

    // Putting lists into the result map with treatment levels as keys
    resultCurrentTreatmentCaseNrs.put(NORMAL_WARD.getValue(), stationaryCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU.getValue(), icuCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), ventCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU_ECMO.getValue(), ecmoCaseNrs);

    return resultCurrentTreatmentCaseNrs;
  }

  // Method to create cumulative length of stay hospital data for different vital statuses
  private void createCumulativeLengthOfStayHospitalData(String label,
      Map<String, Map<Long, Set<String>>> mapDays,
      boolean debug, VitalStatus vitalStatus,
      List<DiseaseDataItem> currentDataList) {
    // Create mapDays based on vital status
    Map<String, Map<Long, Set<String>>> mapDaysFiltered = createLengthOfStayHospitalByVitalstatus(
        mapDays, vitalStatus);

    // Initialize lists to maintain the association between hospital days and case IDs
    List<Map.Entry<Long, Set<String>>> hospitalEntries = new ArrayList<>();

    // Populate hospitalEntries with days and case IDs
    mapDaysFiltered.forEach((patientId, daysMap) ->
        daysMap.forEach((days, caseIds) ->
            hospitalEntries.add(new AbstractMap.SimpleEntry<>(days, caseIds))));

    // Sort hospital entries based on hospital days
    hospitalEntries.sort(Comparator.comparingLong(Map.Entry::getKey));

    // Extract hospital days and case IDs maintaining the association
    List<Long> hospitalDays = hospitalEntries.stream()
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    List<String> caseIdsHospital = hospitalEntries.stream()
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toList());

    // Add data to currentDataList
    currentDataList.add(new DiseaseDataItem(label, ITEMTYPE_LIST, hospitalDays));
    if (debug) {
      currentDataList.add(
          new DiseaseDataItem(addCaseNrsToLabel(label), ITEMTYPE_DEBUG, caseIdsHospital));
    }
  }


  // Method to create cumulative length of stay ICU data for different vital statuses
  private void createCumulativeLengthOfStayIcuData(String label,
      Map<TreatmentLevels, List<UkbEncounter>> mapIcuDiseasePositiveOverall,
      boolean debug, VitalStatus vitalStatus,
      List<DiseaseDataItem> currentDataList,
      Map<String, Map<Long, Set<String>>> mapIcuLengthList) {
    // Create mapDays based on vital status; take all if it's not set
    Map<String, Map<Long, Set<String>>> mapDaysFiltered = mapIcuLengthList;
    if (vitalStatus != null) {
      mapDaysFiltered = createIcuLengthListByVitalstatus(
          vitalStatus, mapIcuLengthList, mapIcuDiseasePositiveOverall);
    }
    // Initialize lists to maintain the association between ICU hours and case IDs
    List<Map.Entry<Long, Set<String>>> icuEntries = new ArrayList<>();

    // Populate icuEntries with hours and case IDs
    mapDaysFiltered.forEach((patientId, hoursMap) ->
        hoursMap.forEach((hours, caseIds) ->
            icuEntries.add(new AbstractMap.SimpleEntry<>(hours, caseIds))));

    // Sort ICU entries based on ICU hours
    icuEntries.sort(Comparator.comparingLong(Map.Entry::getKey));

    // Extract ICU hours and case IDs maintaining the association
    List<Long> listHours = icuEntries.stream()
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    List<String> caseIds = new ArrayList<>();
    // Ensure case IDs are ordered according to listHours
    icuEntries.forEach(entry -> caseIds.addAll(entry.getValue()));

    // Add data to currentDataList
    currentDataList.add(new DiseaseDataItem(label, ITEMTYPE_LIST, listHours));
    if (debug) {
      currentDataList.add(new DiseaseDataItem(addCaseNrsToLabel(label), ITEMTYPE_DEBUG, caseIds));
    }
  }
}
