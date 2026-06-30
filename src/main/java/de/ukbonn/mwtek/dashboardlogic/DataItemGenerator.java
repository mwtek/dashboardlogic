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
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.INFLUENZA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_AGGREGATED;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.SUBITEMTYPE_DATE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.BCT_PREFIX;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_UNDIFF;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD;
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
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_UNDIFF;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL_CROSSTAB;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.INFLUENZA_PREFIX;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX_ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX_KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX_PED;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX_PED_COV;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX_PED_INFL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX_PED_PERTUSSIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.KIDS_RADAR_PREFIX_PED_RSV;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.SNID_PREFIX;
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
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_UNDIFF;
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
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.isLocationReferenceExisting;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGender.translateGenderSpecIntoEnum;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayHospital.createLengthOfStayHospitalByVitalstatus;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayHospital.createMapDaysHospitalList;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayIcu.createIcuLengthListByVitalstatus;
import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayIcu.createIcuLengthOfStayList;
import static de.ukbonn.mwtek.dashboardlogic.tools.ProcedureFilter.filterProceduresByIcuWardCheck;

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Covid;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Influenza;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardData;
import de.ukbonn.mwtek.dashboardlogic.logic.DiseaseDetectionManagement;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age.CumulativeAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.results.CumulativeVariantTestResults;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineVariantTestResults;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityEncounterToIcuSupplyContactsMap;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration.CheckInProgressPeriodStart;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.DataBuilder;
import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;

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
  List<MiiCondition> conditions;
  List<MiiObservation> observations;
  List<MiiPatient> patients;
  List<MiiEncounter> encounters;
  List<MiiEncounter> facilityContactEncountersInpatient;
  List<MiiProcedure> procedures;
  List<MiiLocation> locations;

  List<MiiEncounter> cumulativeIcuEncounters;
  List<MiiEncounter> cumulativeIcuVentEncounters;
  List<MiiEncounter> cumulativeIcuEcmoEncounters;
  List<MiiEncounter> cumulativeIcuUndiffEncounters;

  // map with all inpatient-disease-positive cases, which is needed for internal reports in the UKB
  Map<String, List<String>> mapCurrentTreatmentlevelCaseNrs = new HashMap<>();

  /**
   * Initialization of the CoronaResults object with the required FhirRessource lists.
   *
   * @param conditions List of FHIR condition resources for U07.1 and U07.2 diagnoses
   * @param observations List of FHIR Observation Resources based on the Corona Lab Codes
   * @param patients List of FHIR patient resources based on the Condition and Observation inputs
   * @param encounters List of FHIR encounter resources based on the Condition and Observation
   *     inputs
   * @param procedures List of FHIR ICU procedures resources based on the Patient/Encounter inputs
   * @param locations List of all FHIR location resources based on the encounter inputs
   */
  public DataItemGenerator(
      List<MiiCondition> conditions,
      List<MiiObservation> observations,
      List<MiiPatient> patients,
      List<MiiEncounter> encounters,
      List<MiiProcedure> procedures,
      List<MiiLocation> locations) {
    super();
    this.conditions = conditions;
    this.observations = observations;
    this.patients = patients;
    this.encounters = encounters;
    this.procedures = procedures;
    this.locations = locations;
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems Map with data items to be excluded from the output (e.g.
   *     "current.treatmentlevel").
   * @param variantSettings {@link VariantSettings Configuration} of extended covid-19 variants for
   *     the definition of not yet known/captured variants.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *     codes or procedure codes.
   * @param globalConfiguration An instance of the global configuration settings.
   * @return List with all the {@link DiseaseDataItem data items} that are defined in the corona
   *     dashboard json specification
   */
  @SuppressWarnings("unused")
  public List<DiseaseDataItem> getDataItems(
      Map<String, Boolean> mapExcludeDataItems,
      VariantSettings variantSettings,
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      DataItemContext dataItemContext,
      GlobalConfiguration globalConfiguration) {
    List<DiseaseDataItem> currentDataList = new ArrayList<>();
    if (mapExcludeDataItems == null) {
      mapExcludeDataItems = new HashMap<>();
    }
    boolean useIcuUndiff = globalConfiguration.getUseIcuUndifferentiated();
    boolean debug = globalConfiguration.getDebug();

    // Save the in the YAML configured data items exclude map
    Map<String, Boolean> effExcludeDataItems = new HashMap<>(mapExcludeDataItems);

    // If there are resources with unfilled mandatory attributes, report them immediately (may give
    // partially reduced result sets)
    reportMissingFields(encounters);

    filterResourcesByDate(encounters, conditions, observations, dataItemContext);

    // Filters out encounters with invalid status, unless explicitly allowed via configuration.
    // (should be done in the data-retrieval step before anyway but this step here will do the same
    // for unit tests)
    encounters = filterEncounterByStatus(encounters, globalConfiguration);

    // Updating the encounter status to FINISHED if IN-PROGRESS and period.end is set
    updateInProgressEncounterStatusByDemand(globalConfiguration);

    // Group the encounter resources by facility type
    facilityContactEncountersInpatient =
        encounters.parallelStream().filter(MiiEncounter::isFacilityContact).toList();

    // Logging of unexpected attribute assignments within the resources.
    reportAttributeArtifacts(facilityContactEncountersInpatient);

    // To obtain the case transfer history of an encounter, we need to check the supply level
    List<MiiEncounter> supplyContactEncounters =
        encounters.parallelStream()
            .filter(MiiEncounter::isSupplyContact)
            .filter(MiiEncounter::isCaseClassInpatientOrShortStay)
            .toList();

    // Department contacts are just needed if we need to determine encounter hierarchy via .partOf
    List<MiiEncounter> departmentContactEncounters =
        encounters.parallelStream()
            .filter(MiiEncounter::isDepartmentContact)
            .filter(MiiEncounter::isCaseClassInpatientOrShortStay)
            .toList();

    // If no Encounter of type 'Versorgungsstellenkontakt' could be found, many data items cannot
    // be meaningfully filled and these are excluded from the export.
    // Same logic if there is no location data found. If Encounter.service provider is used instead,
    // there should be at least one dummy icu location.
    boolean supplyContactsFound = isSupplyContactFound(supplyContactEncounters);
    boolean locationsFound = locations != null && !locations.isEmpty();
    if (supplyContactsFound && locationsFound) {
      Map<String, String> supplyContactIdFacilityContactId =
          generateSupplyContactToFacilityContactMap(
              supplyContactEncounters,
              departmentContactEncounters,
              facilityContactEncountersInpatient,
              globalConfiguration.getUsePartOfInsteadOfIdentifier());
    } else {
      log.warn(
          "No encounter with level 'Versorgungsstellenkontakt' and/or location resources were"
              + " found. All data items that require a 'maxtreatmentlevel' separation are"
              + " filtered.");
      effExcludeDataItems.putAll(getAllDataItemsThatNeedSupplyContacts(dataItemContext));
    }

    // Marking encounter resources as positive via setting of extensions
    DiseaseDetectionManagement.flagEncounters(
        encounters,
        conditions,
        observations,
        inputCodeSettings,
        qualitativeLabCodesSettings,
        dataItemContext);

    // the icu information is part of the supply contact
    List<MiiEncounter> icuSupplyContactEncounters =
        supplyContactEncounters.stream()
            .filter(x -> x.isIcuCase(LocationFilter.getIcuLocationIds(locations), false))
            .toList();

    if (globalConfiguration.getCheckProceduresIcuStays())
      procedures = filterProceduresByIcuWardCheck(procedures, icuSupplyContactEncounters);

    Map<TreatmentLevels, List<MiiEncounter>> mapPositiveEncounterByClass =
        createEncounterMapByClass(facilityContactEncountersInpatient);

    // List of stationary Cases
    List<MiiEncounter> inpatientEncounters =
        encounters.parallelStream().filter(MiiEncounter::isCaseClassInpatientOrShortStay).toList();

    FacilityEncounterToIcuSupplyContactsMap facilityEncounterIdToIcuSupplyContactsMap =
        assignSupplyEncountersToFacilityEncounter(icuSupplyContactEncounters, inpatientEncounters);

    Map<TreatmentLevels, List<MiiEncounter>> mapIcuDiseasePositiveOverall =
        createIcuMap(
            encounters,
            icuSupplyContactEncounters,
            locations,
            procedures,
            inputCodeSettings,
            useIcuUndiff);

    /* used for current logic */
    Map<TreatmentLevels, List<MiiEncounter>> mapCurrentIcuDiseasePositive =
        createCurrentIcuMap(mapIcuDiseasePositiveOverall, useIcuUndiff);

    // Initialize an instance with the clinical data sets.
    DashboardData dbData =
        new DashboardData()
            .initializeData(
                inputCodeSettings,
                qualitativeLabCodesSettings,
                encounters,
                patients,
                observations,
                conditions,
                locations,
                procedures,
                dataItemContext);

    // Partial lists of current cases broken down by case status
    List<MiiEncounter> currentStandardWardEncounters = new ArrayList<>();
    List<MiiEncounter> currentIcuEncounters = new ArrayList<>();
    List<MiiEncounter> currentVentEncounters = new ArrayList<>();
    List<MiiEncounter> currentEcmoEncounters = new ArrayList<>();
    List<MiiEncounter> currentIcuUndiffEncounters = new ArrayList<>();

    // Lists of current maxtreatmentlevels
    List<MiiEncounter> currentMaxStationary = new ArrayList<>();
    List<MiiEncounter> currentMaxIcu = new ArrayList<>();
    List<MiiEncounter> currentMaxIcuVent = new ArrayList<>();
    List<MiiEncounter> currentMaxIcuEcmo = new ArrayList<>();
    List<MiiEncounter> currentMaxIcuUndiff = new ArrayList<>();

    // Lists of current maxtreatmentlevels with ages
    List<Long> currentMaxStationaryAgeList;
    List<Long> currentMaxIcuAgeList;
    List<Long> currentMaxIcuVentAgeList;
    List<Long> currentMaxIcuEcmoAgeList;
    List<Long> currentMaxIcuUndiffAgeList;

    // Inpatients = NORMAL_WARD + ICU + ICU_VENT + ECMO
    // needed later for the splitting cumulative.gender into
    // cumulative.inpatient/outpatient.gender

    // same for cumulative results
    List<MiiEncounter> cumulativeOutpatientEncounters =
        new DataBuilder()
            .treatmentLevel(OUTPATIENT)
            .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
            .icuDiseaseMap(mapCurrentIcuDiseasePositive)
            .buildCumulativeByClass();

    List<MiiEncounter> cumulativeStandardWardEncounters =
        new DataBuilder()
            .treatmentLevel(INPATIENT)
            .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
            .icuDiseaseMap(mapCurrentIcuDiseasePositive)
            .buildCumulativeByClass();
    // initialize ICU data
    if (useIcuUndiff) {
      // initialize ICU_UNDIFF data
      cumulativeIcuUndiffEncounters =
          new DataBuilder()
              .treatmentLevel(ICU_UNDIFF)
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .buildCumulativeByIcuLevel();
      removeDuplicatePids(
          cumulativeOutpatientEncounters,
          cumulativeStandardWardEncounters,
          cumulativeIcuUndiffEncounters);
    } else {

      cumulativeIcuEncounters =
          new DataBuilder()
              .treatmentLevel(ICU)
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .buildCumulativeByIcuLevel();
      // initialize ICU_VENT data
      cumulativeIcuVentEncounters =
          new DataBuilder()
              .treatmentLevel(ICU_VENTILATION)
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .buildCumulativeByIcuLevel();
      // initialize ECMO data
      cumulativeIcuEcmoEncounters =
          new DataBuilder()
              .treatmentLevel(ICU_ECMO)
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .buildCumulativeByIcuLevel();
      removeDuplicatePids(
          cumulativeOutpatientEncounters,
          cumulativeStandardWardEncounters,
          cumulativeIcuEncounters,
          cumulativeIcuVentEncounters,
          cumulativeIcuEcmoEncounters);
    }
    DiseaseDataItem cd;
    // current treatmentlevel
    String currentTreatmentlevelItemName = determineLabel(dataItemContext, CURRENT_TREATMENTLEVEL);
    if (isItemNotExcluded(effExcludeDataItems, currentTreatmentlevelItemName, false)) {
      Map<String, Number> mapCurrent = new LinkedHashMap<>();
      currentStandardWardEncounters =
          new DataBuilder()
              .mapCurrentIcuPositive(mapCurrentIcuDiseasePositive)
              .treatmentLevel(NORMAL_WARD)
              .icuSupplyContactEncounters(icuSupplyContactEncounters)
              .dbData(dbData)
              .buildCurrentEncounterByIcuLevel();
      mapCurrent.put(NORMAL_WARD.getValue(), currentStandardWardEncounters.size());

      if (!useIcuUndiff) {
        currentIcuEncounters =
            new DataBuilder()
                .mapCurrentIcuPositive(mapCurrentIcuDiseasePositive)
                .treatmentLevel(ICU)
                .icuSupplyContactEncounters(icuSupplyContactEncounters)
                .dbData(dbData)
                .buildCurrentEncounterByIcuLevel();
        currentVentEncounters =
            new DataBuilder()
                .mapCurrentIcuPositive(mapCurrentIcuDiseasePositive)
                .treatmentLevel(ICU_VENTILATION)
                .icuSupplyContactEncounters(icuSupplyContactEncounters)
                .dbData(dbData)
                .buildCurrentEncounterByIcuLevel();
        currentEcmoEncounters =
            new DataBuilder()
                .mapCurrentIcuPositive(mapCurrentIcuDiseasePositive)
                .treatmentLevel(ICU_ECMO)
                .icuSupplyContactEncounters(icuSupplyContactEncounters)
                .dbData(dbData)
                .buildCurrentEncounterByIcuLevel();
        mapCurrent.put(ICU.getValue(), currentIcuEncounters.size());
        mapCurrent.put(ICU_VENTILATION.getValue(), currentVentEncounters.size());
        mapCurrent.put(ICU_ECMO.getValue(), currentEcmoEncounters.size());
      } else {
        currentIcuUndiffEncounters =
            new DataBuilder()
                .mapCurrentIcuPositive(mapCurrentIcuDiseasePositive)
                .treatmentLevel(ICU_UNDIFF)
                .icuSupplyContactEncounters(icuSupplyContactEncounters)
                .dbData(dbData)
                .buildCurrentEncounterByIcuLevel();
        mapCurrent.put(ICU_UNDIFF.getValue(), currentIcuUndiffEncounters.size());
      }

      currentDataList.add(
          new DiseaseDataItem(currentTreatmentlevelItemName, ITEMTYPE_AGGREGATED, mapCurrent));

      // Storing the case ids of the current treatmentlevel.
      // This list will be used in coming item generations.
      Map<String, List<String>> mapCurrentTreatmentlevelCaseIds =
          new DataBuilder()
              .currentStandardWardEncounters(currentStandardWardEncounters)
              .currentIcuEncounters(currentIcuEncounters)
              .currentVentEncounters(currentVentEncounters)
              .currentEcmoEncounters(currentEcmoEncounters)
              .currentIcuUndiffEncounters(currentIcuUndiffEncounters)
              .useIcuUndiff(useIcuUndiff)
              .buildCurrentTreatmentlevelMapCaseIds();
      this.setMapCurrentTreatmentlevelCaseIds(mapCurrentTreatmentlevelCaseIds);

      if (debug) {
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(currentTreatmentlevelItemName),
                ITEMTYPE_DEBUG,
                mapCurrentTreatmentlevelCaseIds));
      }
    }

    String currentMaxtreatmentlevelLabel =
        determineLabel(dataItemContext, CURRENT_MAXTREATMENTLEVEL);
    // current maxtreatmentlevel
    if (isItemNotExcluded(effExcludeDataItems, currentMaxtreatmentlevelLabel, false)) {
      Map<String, Number> mapCurrentMax = new LinkedHashMap<>();
      currentMaxStationary =
          new DataBuilder()
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .dbData(dbData)
              .useIcuUndiff(useIcuUndiff)
              .treatmentLevel(NORMAL_WARD)
              .buildNumberOfCurrentMaxTreatmentLevel();
      mapCurrentMax.put(NORMAL_WARD.getValue(), currentMaxStationary.size());
      if (!useIcuUndiff) {
        currentMaxIcu =
            new DataBuilder()
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(false)
                .treatmentLevel(ICU)
                .buildNumberOfCurrentMaxTreatmentLevel();
        currentMaxIcuVent =
            new DataBuilder()
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(false)
                .treatmentLevel(ICU_VENTILATION)
                .buildNumberOfCurrentMaxTreatmentLevel();
        currentMaxIcuEcmo =
            new DataBuilder()
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(false)
                .treatmentLevel(ICU_ECMO)
                .buildNumberOfCurrentMaxTreatmentLevel();
        mapCurrentMax.put(ICU.getValue(), currentMaxIcu.size());
        mapCurrentMax.put(ICU_VENTILATION.getValue(), currentMaxIcuVent.size());
        mapCurrentMax.put(ICU_ECMO.getValue(), currentMaxIcuEcmo.size());
      } else {
        currentMaxIcuUndiff =
            new DataBuilder()
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(true)
                .treatmentLevel(ICU_UNDIFF)
                .buildNumberOfCurrentMaxTreatmentLevel();
        mapCurrentMax.put(ICU_UNDIFF.getValue(), currentMaxIcuUndiff.size());
      }
      currentDataList.add(
          new DiseaseDataItem(currentMaxtreatmentlevelLabel, ITEMTYPE_AGGREGATED, mapCurrentMax));

      if (debug) {
        Map<String, List<String>> resultCurrentTreatmentCaseNrs = new LinkedHashMap<>();

        List<String> normalWardEncounterIds =
            currentMaxStationary.stream().map(Encounter::getId).collect(Collectors.toList());
        resultCurrentTreatmentCaseNrs.put(NORMAL_WARD.getValue(), normalWardEncounterIds);

        if (useIcuUndiff) {
          List<String> icuUndiffEncounterIds =
              currentMaxIcuUndiff.stream().map(Encounter::getId).collect(Collectors.toList());
          resultCurrentTreatmentCaseNrs.put(ICU_UNDIFF.getValue(), icuUndiffEncounterIds);
        } else {
          resultCurrentTreatmentCaseNrs.put(
              ICU.getValue(),
              currentMaxIcu.stream().map(Encounter::getId).collect(Collectors.toList()));
          resultCurrentTreatmentCaseNrs.put(
              ICU_VENTILATION.getValue(),
              currentMaxIcuVent.stream().map(Encounter::getId).collect(Collectors.toList()));
          resultCurrentTreatmentCaseNrs.put(
              ICU_ECMO.getValue(),
              currentMaxIcuEcmo.stream().map(Encounter::getId).collect(Collectors.toList()));
        }

        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(currentMaxtreatmentlevelLabel),
                ITEMTYPE_DEBUG,
                resultCurrentTreatmentCaseNrs));
      }
    }

    String currentAgeMaxtreatmentlevelNormalWard =
        determineLabel(dataItemContext, CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
    // current.age.maxtreatmentlevel.normal_ward
    if (isItemNotExcluded(effExcludeDataItems, currentAgeMaxtreatmentlevelNormalWard, false)) {
      if (effExcludeDataItems.getOrDefault(currentMaxtreatmentlevelLabel, false)) {
        currentMaxStationary =
            new DataBuilder()
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .treatmentLevel(NORMAL_WARD)
                .useIcuUndiff(useIcuUndiff)
                .buildNumberOfCurrentMaxTreatmentLevel();
      }
      currentDataList.add(
          new DiseaseDataItem(
              currentAgeMaxtreatmentlevelNormalWard,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(dbData)
                  .encounterSubSet(currentMaxStationary)
                  .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
                  .buildCurrentMaxAgeMap()));
    }
    // current.age.maxtreatmentlevel.icu+
    if (!useIcuUndiff) {
      processIcuTreatmentLevel(
          dataItemContext,
          CURRENT_AGE_MAXTREATMENTLEVEL_ICU,
          ICU,
          currentDataList,
          effExcludeDataItems,
          currentMaxtreatmentlevelLabel,
          mapIcuDiseasePositiveOverall,
          mapPositiveEncounterByClass,
          dbData,
          false);
      processIcuTreatmentLevel(
          dataItemContext,
          CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION,
          ICU_VENTILATION,
          currentDataList,
          effExcludeDataItems,
          currentMaxtreatmentlevelLabel,
          mapIcuDiseasePositiveOverall,
          mapPositiveEncounterByClass,
          dbData,
          false);
      processIcuTreatmentLevel(
          dataItemContext,
          CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO,
          ICU_ECMO,
          currentDataList,
          effExcludeDataItems,
          currentMaxtreatmentlevelLabel,
          mapIcuDiseasePositiveOverall,
          mapPositiveEncounterByClass,
          dbData,
          false);
    } else {
      // ICU_UNDIFF handling
      processIcuTreatmentLevel(
          dataItemContext,
          CURRENT_AGE_MAXTREATMENTLEVEL_ICU_UNDIFF,
          ICU_UNDIFF,
          currentDataList,
          effExcludeDataItems,
          currentMaxtreatmentlevelLabel,
          mapIcuDiseasePositiveOverall,
          mapPositiveEncounterByClass,
          dbData,
          true);
    }

    // cumulative.results
    String cumulativeResultsLabel = determineLabel(dataItemContext, CUMULATIVE_RESULTS);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeResultsLabel, false)) {
      Map<String, Number> cumulativeResultMap = new LinkedHashMap<>();
      for (DashboardLogicFixedValues result : List.of(POSITIVE, BORDERLINE, NEGATIVE)) {
        Set<MiiObservation> observations =
            buildObservationsByResult(result, dataItemContext, dbData);
        cumulativeResultMap.put(result.getValue(), observations.size());
      }
      currentDataList.add(
          new DiseaseDataItem(cumulativeResultsLabel, ITEMTYPE_AGGREGATED, cumulativeResultMap));
    }

    String cumulativeGenderLabel = determineLabel(dataItemContext, CUMULATIVE_GENDER);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeGenderLabel, false)) {
      Map<String, Number> cumulativeGenderMap = new LinkedHashMap<>();
      Map<String, Set<String>> cumulativeGenderPids = debug ? new LinkedHashMap<>() : null;

      for (DashboardLogicFixedValues gender :
          List.of(MALE_SPECIFICATION, FEMALE_SPECIFICATION, DIVERSE_SPECIFICATION)) {
        // Compute the list of patient IDs ONCE per gender
        Set<String> genderPids =
            new DataBuilder()
                .dbData(dbData)
                .gender(translateGenderSpecIntoEnum(gender))
                .buildGenderList();
        // Store the count derived from the list size
        cumulativeGenderMap.put(gender.getValue(), genderPids.size());
        if (debug) {
          cumulativeGenderPids.put(gender.getValue(), genderPids);
        }
      }
      currentDataList.add(
          new DiseaseDataItem(cumulativeGenderLabel, ITEMTYPE_AGGREGATED, cumulativeGenderMap));

      if (debug) {
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(cumulativeGenderLabel), ITEMTYPE_DEBUG, cumulativeGenderPids));
      }
    }

    // cumulative.age
    String cumulativeAgeLabel = determineLabel(dataItemContext, CUMULATIVE_AGE);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeAgeLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeAgeLabel,
              ITEMTYPE_LIST,
              CumulativeAge.getAgeDistributionsByCaseClass(
                  dbData.getFacilityContactEncounters(),
                  dbData.getPatients(),
                  TreatmentLevels.ALL)));
    }

    // cumulative.maxtreatmentlevel
    String cumulativeMaxTreatmentLevelLabel =
        determineLabel(dataItemContext, CUMULATIVE_MAXTREATMENTLEVEL);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeMaxTreatmentLevelLabel, false)) {
      Map<String, Number> mapCumulativeMaxtreatmentlevel =
          getStringNumberMap(
              cumulativeOutpatientEncounters, cumulativeStandardWardEncounters, useIcuUndiff);
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeMaxTreatmentLevelLabel,
              ITEMTYPE_AGGREGATED,
              mapCumulativeMaxtreatmentlevel));

      // adding case ids on demand
      if (debug) {
        Map<String, Map<String, List<String>>> resultMaxTreatmentCaseNrs = new LinkedHashMap<>();

        resultMaxTreatmentCaseNrs.put(OUTPATIENT.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(NORMAL_WARD.getValue(), new HashMap<>());
        createCumulativeMaxDebug(
            cumulativeOutpatientEncounters, OUTPATIENT.getValue(), resultMaxTreatmentCaseNrs);
        createCumulativeMaxDebug(
            cumulativeStandardWardEncounters, NORMAL_WARD.getValue(), resultMaxTreatmentCaseNrs);

        if (!useIcuUndiff) {
          resultMaxTreatmentCaseNrs.put(ICU.getValue(), new HashMap<>());
          resultMaxTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), new HashMap<>());
          resultMaxTreatmentCaseNrs.put(ICU_ECMO.getValue(), new HashMap<>());
          createCumulativeMaxDebug(
              cumulativeIcuEncounters, ICU.getValue(), resultMaxTreatmentCaseNrs);
          createCumulativeMaxDebug(
              cumulativeIcuVentEncounters, ICU_VENTILATION.getValue(), resultMaxTreatmentCaseNrs);
          createCumulativeMaxDebug(
              cumulativeIcuEcmoEncounters, ICU_ECMO.getValue(), resultMaxTreatmentCaseNrs);
        } else {
          resultMaxTreatmentCaseNrs.put(ICU_UNDIFF.getValue(), new HashMap<>());
          createCumulativeMaxDebug(
              cumulativeIcuUndiffEncounters, ICU_UNDIFF.getValue(), resultMaxTreatmentCaseNrs);
        }

        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(cumulativeMaxTreatmentLevelLabel),
                ITEMTYPE_DEBUG,
                resultMaxTreatmentCaseNrs));
      }
    }
    String cumulativeAgeMaxTreatmentlevelOutpatientLabel =
        determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT);
    // cumulative.age.maxtreatmentlevel.outpatient
    if (isItemNotExcluded(
        effExcludeDataItems, cumulativeAgeMaxTreatmentlevelOutpatientLabel, false)) {
      List<Integer> cumulativeMaxtreatmentlevelOutpatientAgeList =
          new DataBuilder()
              .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .dbData(dbData)
              .useIcuUndiff(useIcuUndiff)
              .treatmentLevel(OUTPATIENT)
              .buildCumMaxtreatmentlevelAgeList();
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeAgeMaxTreatmentlevelOutpatientLabel,
              ITEMTYPE_LIST,
              cumulativeMaxtreatmentlevelOutpatientAgeList));
    }
    String cumulativeAgeMaxTreatmentlevelNormalWardLabel =
        determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
    // cumulative.age.maxtreatmentlevel.normal_ward
    if (isItemNotExcluded(
        effExcludeDataItems, cumulativeAgeMaxTreatmentlevelNormalWardLabel, false)) {
      List<Integer> cumulativeMaxtreatmentlevelNormalWardAgeList =
          new DataBuilder()
              .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .dbData(dbData)
              .useIcuUndiff(useIcuUndiff)
              .treatmentLevel(NORMAL_WARD)
              .buildCumMaxtreatmentlevelAgeList();
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeAgeMaxTreatmentlevelNormalWardLabel,
              ITEMTYPE_LIST,
              cumulativeMaxtreatmentlevelNormalWardAgeList));
    }
    // cumulative.age.maxtreatmentlevel.icu

    if (!useIcuUndiff) {
      String cumulativeAgeMaxTreatmentlevelIcuLabel =
          determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU);
      if (isItemNotExcluded(effExcludeDataItems, cumulativeAgeMaxTreatmentlevelIcuLabel, false)) {
        List<Integer> cumulativeMaxtreatmentlevelIcuAgeList =
            new DataBuilder()
                .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(false)
                .treatmentLevel(ICU)
                .buildCumMaxtreatmentlevelAgeList();
        currentDataList.add(
            new DiseaseDataItem(
                cumulativeAgeMaxTreatmentlevelIcuLabel,
                ITEMTYPE_LIST,
                cumulativeMaxtreatmentlevelIcuAgeList));
      }
      String cumulativeAgeMaxTreatmentlevelVentLabel =
          determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION);
      // cumulative.age.maxtreatmentlevel.icu_with_ventilation
      if (isItemNotExcluded(effExcludeDataItems, cumulativeAgeMaxTreatmentlevelVentLabel, false)) {
        List<Integer> cumulativeMaxtreatmentlevelIcuVentAgeList =
            new DataBuilder()
                .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(false)
                .treatmentLevel(ICU_VENTILATION)
                .buildCumMaxtreatmentlevelAgeList();
        currentDataList.add(
            new DiseaseDataItem(
                cumulativeAgeMaxTreatmentlevelVentLabel,
                ITEMTYPE_LIST,
                cumulativeMaxtreatmentlevelIcuVentAgeList));
      }
      // cumulative.age.maxtreatmentlevel.icu_with_ecmo
      String cumulativeAgeMaxTreatmentlevelEcmoLabel =
          determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO);
      if (isItemNotExcluded(effExcludeDataItems, cumulativeAgeMaxTreatmentlevelEcmoLabel, false)) {
        List<Integer> cumulativeMaxtreatmentlevelIcuEcmoAgeList =
            new DataBuilder()
                .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(false)
                .treatmentLevel(ICU_ECMO)
                .buildCumMaxtreatmentlevelAgeList();
        currentDataList.add(
            new DiseaseDataItem(
                cumulativeAgeMaxTreatmentlevelEcmoLabel,
                ITEMTYPE_LIST,
                cumulativeMaxtreatmentlevelIcuEcmoAgeList));
      }
    } else {
      String cumulativeAgeMaxTreatmentlevelIcuUndiffLabel =
          determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_UNDIFF);
      if (isItemNotExcluded(
          effExcludeDataItems, cumulativeAgeMaxTreatmentlevelIcuUndiffLabel, false)) {
        List<Integer> cumMtlIcuUndiffAgeList =
            new DataBuilder()
                .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
                .icuDiseaseMap(mapIcuDiseasePositiveOverall)
                .dbData(dbData)
                .useIcuUndiff(true)
                .treatmentLevel(ICU_UNDIFF)
                .buildCumMaxtreatmentlevelAgeList();
        currentDataList.add(
            new DiseaseDataItem(
                cumulativeAgeMaxTreatmentlevelIcuUndiffLabel,
                ITEMTYPE_LIST,
                cumMtlIcuUndiffAgeList));
      }
    }
    // cumulative zip code
    String cumulativeZipCodeLabel = determineLabel(dataItemContext, CUMULATIVE_ZIPCODE);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeZipCodeLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeZipCodeLabel,
              ITEMTYPE_LIST,
              new DataBuilder().dbData(dbData).buildCumulativeZipCodeList()));
    }

    // timeline tests
    String timelineTestsLabel = determineLabel(dataItemContext, TIMELINE_TESTS);
    if (isItemNotExcluded(effExcludeDataItems, timelineTestsLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              timelineTestsLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dataItemContext(dataItemContext)
                  .dbData(dbData)
                  .buildTimelineTestsMap()));
    }

    // timeline.test.positive
    String timelineTestPositiveLabel = determineLabel(dataItemContext, TIMELINE_TEST_POSITIVE);
    if (isItemNotExcluded(effExcludeDataItems, timelineTestPositiveLabel, false)) {
      TimestampedListPair timelineTestPositivePair =
          new DataBuilder()
              .dataItemContext(dataItemContext)
              .dbData(dbData)
              .buildTimelineTestsPositiveMap();
      currentDataList.add(
          new DiseaseDataItem(timelineTestPositiveLabel, ITEMTYPE_LIST, timelineTestPositivePair));
    }

    // timeline.maxtreatmentlevel
    String timelineMaxtreatmentlevelLabel =
        determineLabel(dataItemContext, TIMELINE_MAXTREATMENTLEVEL);
    if (isItemNotExcluded(effExcludeDataItems, timelineMaxtreatmentlevelLabel, false)) {
      Map<TreatmentLevels, Map<Long, Set<String>>> resultMaxTreatmentTimeline;
      Map<String, List<Long>> mapResultTreatment = new LinkedHashMap<>();

      // The result contains the case numbers for debugging and the sums by date
      resultMaxTreatmentTimeline =
          new DataBuilder()
              .dataItemContext(dataItemContext)
              .dbData(dbData)
              .useIcuUndiff(useIcuUndiff)
              .buildMaxTreatmentTimeline();
      mapResultTreatment.put(SUBITEMTYPE_DATE, getDatesOutputList(dataItemContext));

      for (Entry<TreatmentLevels, Map<Long, Set<String>>> entry :
          resultMaxTreatmentTimeline.entrySet()) {
        for (Entry<Long, Set<String>> secondEntry : entry.getValue().entrySet()) {
          addValuesToTimelineMaxMap(
              entry.getKey().getValue(), secondEntry.getValue(), mapResultTreatment);
        }
      }
      currentDataList.add(
          new DiseaseDataItem(timelineMaxtreatmentlevelLabel, ITEMTYPE_LIST, mapResultTreatment));

      // timeline maxtreatmentlevel
      // case ids can be shown for plausibility checks
      if (debug) {
        Map<String, Map<Long, Set<String>>> resultMaxTreatmentCaseNrs = new LinkedHashMap<>();
        // Iterate over the resultMaxTreatmentTimeline while preserving order
        for (Entry<TreatmentLevels, Map<Long, Set<String>>> entry :
            resultMaxTreatmentTimeline.entrySet()) {
          resultMaxTreatmentCaseNrs.put(entry.getKey().getValue(), entry.getValue());
        }
        // Add an empty date map, as required by the REST endpoint (maintaining order)
        resultMaxTreatmentCaseNrs.put(SUBITEMTYPE_DATE, new LinkedHashMap<>());
        // Create a new DiseaseDataItem and add it to the current data list
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(timelineMaxtreatmentlevelLabel),
                ITEMTYPE_DEBUG,
                resultMaxTreatmentCaseNrs));
      } // if
    }

    // cumulative inpatient gender
    String cumulativeInpatientGenderLabel =
        determineLabel(dataItemContext, CUMULATIVE_INPATIENT_GENDER);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeInpatientGenderLabel, false)) {
      Map<String, Number> cumulativeInpatientGenderMap = new HashMap<>();
      Map<String, Set<String>> cumulativeInpatientGenderPids = debug ? new HashMap<>() : null;

      for (DashboardLogicFixedValues gender :
          List.of(MALE_SPECIFICATION, FEMALE_SPECIFICATION, DIVERSE_SPECIFICATION)) {
        // Gather all the patient ids for debug item and count it for the regular item
        Set<String> genderPids =
            new DataBuilder()
                .dbData(dbData)
                .gender(translateGenderSpecIntoEnum(gender))
                .treatmentLevel(INPATIENT)
                .buildGenderPidsByCaseClass();
        cumulativeInpatientGenderMap.put(gender.getValue(), genderPids.size());

        if (debug) {
          cumulativeInpatientGenderPids.put(gender.getValue(), genderPids);
        }
      }
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeInpatientGenderLabel, ITEMTYPE_AGGREGATED, cumulativeInpatientGenderMap));
      if (debug) {
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(cumulativeInpatientGenderLabel),
                ITEMTYPE_DEBUG,
                cumulativeInpatientGenderPids));
      }
    }

    // cumulative outpatient gender
    String cumulativeOutpatientGenderLabel =
        determineLabel(dataItemContext, CUMULATIVE_OUTPATIENT_GENDER);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeOutpatientGenderLabel, false)) {
      Map<String, Number> cumulativeOutpatientGender = new HashMap<>();
      Set<String> outpatientsMale =
          new DataBuilder()
              .dbData(dbData)
              .gender(MALE)
              .treatmentLevel(OUTPATIENT)
              .buildGenderPidsByCaseClass();
      Set<String> outpatientsFemale =
          new DataBuilder()
              .dbData(dbData)
              .gender(FEMALE)
              .treatmentLevel(OUTPATIENT)
              .buildGenderPidsByCaseClass();
      Set<String> outpatientsDiverse =
          new DataBuilder()
              .dbData(dbData)
              .gender(DIVERSE)
              .treatmentLevel(OUTPATIENT)
              .buildGenderPidsByCaseClass();
      cumulativeOutpatientGender.put(MALE_SPECIFICATION.getValue(), outpatientsMale.size());
      cumulativeOutpatientGender.put(FEMALE_SPECIFICATION.getValue(), outpatientsFemale.size());
      cumulativeOutpatientGender.put(DIVERSE_SPECIFICATION.getValue(), outpatientsDiverse.size());
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeOutpatientGenderLabel, ITEMTYPE_AGGREGATED, cumulativeOutpatientGender));
      if (debug) {
        Map<String, Set<String>> outpatientDebugMap =
            Map.of(
                MALE_SPECIFICATION.getValue(), outpatientsMale,
                FEMALE_SPECIFICATION.getValue(), outpatientsFemale,
                DIVERSE_SPECIFICATION.getValue(), outpatientsDiverse);
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(cumulativeOutpatientGenderLabel),
                ITEMTYPE_DEBUG,
                outpatientDebugMap));
      }
    }

    String cumulativeInpatientAge = determineLabel(dataItemContext, CUMULATIVE_INPATIENT_AGE);
    // cumulative inpatient age
    if (isItemNotExcluded(effExcludeDataItems, cumulativeInpatientAge, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeInpatientAge,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(dbData)
                  .treatmentLevel(INPATIENT)
                  .buildAgeDistributionByCaseClass()));
    }

    // cumulative outpatient age
    String cumulativeOutpatientAgeLabel =
        determineLabel(dataItemContext, CUMULATIVE_OUTPATIENT_AGE);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeOutpatientAgeLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeOutpatientAgeLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(dbData)
                  .treatmentLevel(OUTPATIENT)
                  .buildAgeDistributionByCaseClass()));
    }

    // timeline deaths
    String timelineDeathsLabel = determineLabel(dataItemContext, TIMELINE_DEATHS);
    if (isItemNotExcluded(effExcludeDataItems, timelineDeathsLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              timelineDeathsLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(dbData)
                  .dataItemContext(dataItemContext)
                  .buildTimelineDeathMap()));
    }

    // Bonn cross-table calculation; currently just needed in the covid-19-context
    if (dataItemContext == DataItemContext.COVID) {
      if (isItemNotExcluded(effExcludeDataItems, CURRENT_TREATMENTLEVEL_CROSSTAB, true)) {
        Map<String, List> aggData = new HashMap<>();
        aggData.put(
            "columnname",
            Arrays.asList(
                CITY_BONN.getValue(),
                ICU.getValue(),
                ICU_VENTILATION.getValue(),
                ICU_ECMO.getValue()));
        aggData.put(
            "state", Arrays.asList("0000", "0100", "0110", "0111", "1000", "1100", "1110", "1111"));
        aggData.put("value", new ArrayList<>());
        Map<TreatmentLevels, List<MiiEncounter>> crosstabMaxtreatmentlevels = new LinkedHashMap<>();
        crosstabMaxtreatmentlevels.put(INPATIENT, currentStandardWardEncounters);
        crosstabMaxtreatmentlevels.put(ICU, currentIcuEncounters);
        List<MiiEncounter> ventEncounters = new ArrayList<>(currentVentEncounters);
        crosstabMaxtreatmentlevels.put(ICU_VENTILATION, ventEncounters);
        List<MiiEncounter> ecmoEncounters = new ArrayList<>(currentEcmoEncounters);
        crosstabMaxtreatmentlevels.put(ICU_ECMO, ecmoEncounters);

        Set<MiiEncounter> allCurrentEncounters = new HashSet<>();

        allCurrentEncounters.addAll(currentStandardWardEncounters);
        allCurrentEncounters.addAll(currentIcuEncounters);
        allCurrentEncounters.addAll(ventEncounters);
        allCurrentEncounters.addAll(ecmoEncounters);

        Set<String> encounterPids =
            allCurrentEncounters.stream()
                .map(MiiEncounter::getPatientId)
                .collect(Collectors.toSet());

        List<MiiPatient> currentPatients =
            patients.stream()
                .filter(patient -> encounterPids.contains(patient.getId()))
                .collect(Collectors.toList());
        List<String[]> ukbCrossTabList =
            generateCrosstabList(crosstabMaxtreatmentlevels, currentPatients);
        if (debug) {
          aggData.put("casenrs", ukbCrossTabList);
        }

        List<String> values = new ArrayList<>();
        for (String[] nr : ukbCrossTabList) {
          values.add(String.valueOf(nr.length));
        }
        aggData.put("value", values);
        currentDataList.add(
            new DiseaseDataItem(
                determineLabel(dataItemContext, CURRENT_TREATMENTLEVEL_CROSSTAB),
                ITEMTYPE_AGGREGATED,
                aggData));
      }
    }

    // list with all lengths of icu stays (in h)
    // cumulative.lengthofstay.icu
    String cumulativeLengthOfStayIcuLabel =
        determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_ICU);
    Map<String, Map<Long, Set<String>>> mapIcuLengthList =
        createIcuLengthOfStayList(icuSupplyContactEncounters, dbData.getLocations());
    if (isItemNotExcluded(effExcludeDataItems, cumulativeLengthOfStayIcuLabel, false)) {
      createCumulativeLengthOfStayIcuData(
          cumulativeLengthOfStayIcuLabel,
          mapIcuDiseasePositiveOverall,
          debug,
          null,
          currentDataList,
          mapIcuLengthList);
    }
    // list with all lengths of icu stays (in h)
    String cumulativeLengthOfStayIcuAliveLabel =
        determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_ICU_ALIVE);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeLengthOfStayIcuAliveLabel, false)) {
      createCumulativeLengthOfStayIcuData(
          cumulativeLengthOfStayIcuAliveLabel,
          mapIcuDiseasePositiveOverall,
          debug,
          ALIVE,
          currentDataList,
          mapIcuLengthList);
    }

    // list with all lengths of icu stays (in h)
    String cumulativeLengthOfStayIcuDeadLabel =
        determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_ICU_DEAD);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeLengthOfStayIcuDeadLabel, false)) {
      createCumulativeLengthOfStayIcuData(
          cumulativeLengthOfStayIcuDeadLabel,
          mapIcuDiseasePositiveOverall,
          debug,
          DEAD,
          currentDataList,
          mapIcuLengthList);
    }

    // cumulative length of stays
    String cumulativeLengthOfStayHospitalLabel =
        determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_HOSPITAL);
    Map<String, Map<Long, Set<String>>> mapDays =
        createMapDaysHospitalList(dbData.getFacilityContactEncounters());
    if (isItemNotExcluded(effExcludeDataItems, cumulativeLengthOfStayHospitalLabel, false)) {
      createCumulativeLengthOfStayHospitalData(
          dbData.getFacilityContactEncounters(),
          cumulativeLengthOfStayHospitalLabel,
          mapDays,
          debug,
          null,
          currentDataList);
    }
    // cumulative length of stays ALIVE
    String cumulativeLengthOfStayHospitalAliveLabel =
        determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeLengthOfStayHospitalAliveLabel, false)) {
      createCumulativeLengthOfStayHospitalData(
          dbData.getFacilityContactEncounters(),
          cumulativeLengthOfStayHospitalAliveLabel,
          mapDays,
          debug,
          ALIVE,
          currentDataList);
    }
    // cumulative.lengthofstay.dead
    String cumulativeLengthOfStayHospitalDeadLabel =
        determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD);
    if (isItemNotExcluded(effExcludeDataItems, cumulativeLengthOfStayHospitalDeadLabel, false)) {
      createCumulativeLengthOfStayHospitalData(
          dbData.getFacilityContactEncounters(),
          cumulativeLengthOfStayHospitalDeadLabel,
          mapDays,
          debug,
          DEAD,
          currentDataList);
    }

    // Handling of items that restricted to covid-19 context
    if (dataItemContext == DataItemContext.COVID) {
      // cumulative varianttestresults
      if (isItemNotExcluded(effExcludeDataItems, CUMULATIVE_VARIANTTESTRESULTS, false)) {
        Map<String, Integer> resultMap =
            new CumulativeVariantTestResults()
                .createVariantTestResultMap(
                    dbData.getVariantObservations(), variantSettings, inputCodeSettings);
        currentDataList.add(
            new DiseaseDataItem(CUMULATIVE_VARIANTTESTRESULTS, ITEMTYPE_AGGREGATED, resultMap));
      }

      // timeline.varianttestresults
      if (isItemNotExcluded(effExcludeDataItems, TIMELINE_VARIANTTESTRESULTS, false)) {
        currentDataList.add(
            new DiseaseDataItem(
                TIMELINE_VARIANTTESTRESULTS,
                ITEMTYPE_LIST,
                new TimelineVariantTestResults()
                    .createTimelineVariantsTests(
                        dbData.getVariantObservations(), variantSettings, inputCodeSettings)));
      }
    } // if
    return currentDataList;
  }

  private Map<String, Number> getStringNumberMap(
      List<MiiEncounter> cumulativeOutpatientEncounters,
      List<MiiEncounter> cumulativeStandardWardEncounters,
      boolean useIcuUndiff) {
    Map<String, Number> mapCumulativeMaxtreatmentlevel = new LinkedHashMap<>();
    mapCumulativeMaxtreatmentlevel.put(
        OUTPATIENT.getValue(), cumulativeOutpatientEncounters.size());
    mapCumulativeMaxtreatmentlevel.put(
        NORMAL_WARD.getValue(), cumulativeStandardWardEncounters.size());
    if (!useIcuUndiff) {
      mapCumulativeMaxtreatmentlevel.put(ICU.getValue(), cumulativeIcuEncounters.size());
      mapCumulativeMaxtreatmentlevel.put(
          ICU_VENTILATION.getValue(), cumulativeIcuVentEncounters.size());
      mapCumulativeMaxtreatmentlevel.put(ICU_ECMO.getValue(), cumulativeIcuEcmoEncounters.size());
    } else
      mapCumulativeMaxtreatmentlevel.put(
          ICU_UNDIFF.getValue(), cumulativeIcuUndiffEncounters.size());
    return mapCumulativeMaxtreatmentlevel;
  }

  private void updateInProgressEncounterStatusByDemand(GlobalConfiguration globalConfiguration) {
    updateInProgressEncounterStatusByPeriodEnd(globalConfiguration);
    updateInProgressEncounterStatusByDate(globalConfiguration);
  }

  private void updateInProgressEncounterStatusByDate(GlobalConfiguration globalConfiguration) {
    if (globalConfiguration.getCheckInProgressPeriodStart().getOlderThanXDays() != null)
      updateInProgressEncounterStatusByDate(globalConfiguration.getCheckInProgressPeriodStart());
  }

  private void updateInProgressEncounterStatusByPeriodEnd(GlobalConfiguration globalConfiguration) {
    if (globalConfiguration.getCheckInProgressPeriodEnd())
      encounters.stream()
          .filter(MiiEncounter::isActive)
          .filter(x -> x.hasPeriod() && x.getPeriod().hasEnd())
          .forEach(x -> x.setStatus(EncounterStatus.FINISHED));
  }

  /**
   * Filters the list of in-progress encounters by comparing their start dates against a configured
   * reference date and cutoff duration.
   *
   * <p>Encounters are removed if their start date is before both: - the reference (base) date - and
   * the base date minus the configured number of days.
   *
   * @param checkInProgressPeriodStart configuration object containing base date and day offset
   * @throws IllegalArgumentException if the configuration or required fields are null
   */
  private void updateInProgressEncounterStatusByDate(
      CheckInProgressPeriodStart checkInProgressPeriodStart) {
    if (checkInProgressPeriodStart == null
        || checkInProgressPeriodStart.getOlderThanXDays() == null) {
      throw new IllegalArgumentException(
          "InProgressEncounterFilterConfig and olderThanXDays must not be null");
    }

    // Use current date as fallback if baseDate is not set
    Date baseDate = checkInProgressPeriodStart.getBaseDateType();
    if (baseDate == null) {
      baseDate = DateTools.getCurrentDateTime();
    }

    LocalDate referenceDate = baseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    LocalDate cutoffDate = referenceDate.minusDays(checkInProgressPeriodStart.getOlderThanXDays());

    // Remove encounters whose start date is before both the reference date and the cutoff
    encounters =
        encounters.stream()
            .filter(
                encounter -> {
                  if (!encounter.isActive()
                      || !encounter.hasPeriod()
                      || !encounter.getPeriod().hasStart()) {
                    return true; // keep encounters with missing or inactive period
                  }

                  Date startDate = encounter.getPeriod().getStart();
                  LocalDate startLocalDate =
                      startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                  return !(startLocalDate.isBefore(referenceDate)
                      && startLocalDate.isBefore(cutoffDate));
                })
            .collect(Collectors.toList());
  }

  /**
   * Filters lists of encounters, conditions, and observations by a qualifying date depending on the
   * data item context (e.g., Influenza or COVID). Logs the count of removed resources and the IDs
   * of the first removed resource in each category.
   *
   * @param encounters List of UkbEncounter resources to filter.
   * @param conditions List of UkbCondition resources to filter.
   * @param observations List of UkbObservation resources to filter.
   * @param dataItemContext The context which determines the qualifying cutoff date.
   */
  private void filterResourcesByDate(
      List<MiiEncounter> encounters,
      List<MiiCondition> conditions,
      List<MiiObservation> observations,
      DataItemContext dataItemContext) {
    Long cutOffDateMillis =
        (dataItemContext == INFLUENZA)
            ? Influenza.QUALIFYING_DATE_MILLISECONDS
            : Covid.QUALIFYING_DATE_MILLISECONDS;

    String firstRemovedEncounterId = "";
    String firstRemovedConditionId = "";
    String firstRemovedObservationId = "";

    List<MiiEncounter> encountersFilteredByDate =
        filterEncountersAfterDate(encounters, cutOffDateMillis);
    int encounterDiff = encounters.size() - encountersFilteredByDate.size();
    if (encounterDiff > 0) {
      MiiEncounter firstRemovedEncounter =
          findFirstRemovedElement(encounters, encountersFilteredByDate);
      firstRemovedEncounterId = firstRemovedEncounter != null ? firstRemovedEncounter.getId() : "";
      encounters.clear();
      encounters.addAll(encountersFilteredByDate);
    }

    List<MiiCondition> conditionsFilteredByDate =
        filterConditionsAfterDate(conditions, cutOffDateMillis);
    int condDiff = conditions.size() - conditionsFilteredByDate.size();
    if (condDiff > 0) {
      MiiCondition firstRemovedCondition =
          findFirstRemovedElement(conditions, conditionsFilteredByDate);
      firstRemovedConditionId = firstRemovedCondition != null ? firstRemovedCondition.getId() : "";
    }

    List<MiiObservation> observationsFilteredByDate =
        filterObservationsAfterDate(observations, cutOffDateMillis);
    int obsDiff = observations.size() - observationsFilteredByDate.size();
    if (obsDiff > 0) {
      MiiObservation firstRemovedObservation =
          findFirstRemovedElement(observations, observationsFilteredByDate);
      firstRemovedObservationId =
          firstRemovedObservation != null ? firstRemovedObservation.getId() : "";
      observations.clear();
      observations.addAll(observationsFilteredByDate);
    }

    logFilteredCounts(
        encounterDiff,
        firstRemovedEncounterId,
        condDiff,
        firstRemovedConditionId,
        obsDiff,
        firstRemovedObservationId);
  }

  /**
   * Finds the first element removed during filtering by comparing the original and filtered lists.
   *
   * @param originalList The original list before filtering.
   * @param filteredList The list after filtering.
   * @param <T> The type of elements in the lists.
   * @return The first removed element, or null if no elements were removed.
   */
  private <T> T findFirstRemovedElement(List<T> originalList, List<T> filteredList) {
    for (T item : originalList) {
      if (!filteredList.contains(item)) {
        return item;
      }
    }
    return null;
  }

  /**
   * Filters encounters to include only those with a start date after the given cutoff date.
   *
   * @param encounters List of UkbEncounter resources to filter.
   * @param cutOffDateMillis The cutoff date for filtering.
   * @return A list of encounters that occur after the cutoff date.
   */
  private List<MiiEncounter> filterEncountersAfterDate(
      List<MiiEncounter> encounters, Long cutOffDateMillis) {

    return encounters.parallelStream()
        .filter(
            e ->
                e.hasPeriod()
                    && e.getPeriod().hasStart()
                    && e.getPeriod().getStart().getTime() >= cutOffDateMillis)
        .toList();
  }

  /**
   * Filters conditions to include only those recorded after the given cutoff date.
   *
   * @param conditions List of UkbCondition resources to filter.
   * @param cutOffDateMillis The cutoff date for filtering.
   * @return A list of conditions recorded after the cutoff date.
   */
  private List<MiiCondition> filterConditionsAfterDate(
      List<MiiCondition> conditions, Long cutOffDateMillis) {
    return conditions.parallelStream()
        .filter(c -> c.hasRecordedDate() && c.getRecordedDate().getTime() >= cutOffDateMillis)
        .toList();
  }

  /**
   * Filters observations to include only those with an effective date after the given cutoff date.
   *
   * @param observations List of UkbObservation resources to filter.
   * @param cutOffDateMillis The cutoff date for filtering.
   * @return A list of observations effective after the cutoff date.
   */
  private List<MiiObservation> filterObservationsAfterDate(
      List<MiiObservation> observations, Long cutOffDateMillis) {
    return observations.parallelStream()
        .filter(
            o ->
                o.hasEffective()
                    && o.getEffectiveDateTimeType().getValue().getTime() > cutOffDateMillis)
        .toList();
  }

  private void logFilteredCounts(
      int encounterDelta,
      String firstEncounterIdRemoved,
      int conditionDelta,
      String firstConditionIdRemoved,
      int observationDelta,
      String firstObservationIdRemoved) {

    if (encounterDelta > 0) {
      logDelta(FHIRAllTypes.ENCOUNTER, encounterDelta, firstEncounterIdRemoved);
    }
    if (conditionDelta > 0) {
      logDelta(FHIRAllTypes.CONDITION, conditionDelta, firstConditionIdRemoved);
    }
    if (observationDelta > 0) {
      logDelta(FHIRAllTypes.OBSERVATION, observationDelta, firstObservationIdRemoved);
    }
  }

  private static void logDelta(
      FHIRAllTypes fhirAllTypes, int observationDelta, String firstObservationIdRemoved) {
    log.info(
        "Filtered {} {}s because the date is before the kickoff date. Example: {}",
        observationDelta,
        fhirAllTypes.getDisplay(),
        firstObservationIdRemoved);
  }

  private Map<String, Boolean> getAllDataItemsThatNeedSupplyContacts(
      DataItemContext dataItemContext) {
    Map<String, Boolean> output = new HashMap<>();
    output.put(determineLabel(dataItemContext, CURRENT_TREATMENTLEVEL), true);
    output.put(determineLabel(dataItemContext, CURRENT_MAXTREATMENTLEVEL), true);
    output.put(determineLabel(dataItemContext, CUMULATIVE_MAXTREATMENTLEVEL), true);
    output.put(determineLabel(dataItemContext, TIMELINE_MAXTREATMENTLEVEL), true);
    output.put(determineLabel(dataItemContext, CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD), true);
    output.put(determineLabel(dataItemContext, CURRENT_AGE_MAXTREATMENTLEVEL_ICU), true);
    output.put(
        determineLabel(dataItemContext, CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION), true);
    output.put(determineLabel(dataItemContext, CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO), true);
    output.put(determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT), true);
    output.put(determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD), true);
    output.put(determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU), true);
    output.put(
        determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION),
        true);
    output.put(
        determineLabel(dataItemContext, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO), true);
    output.put(determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_ICU), true);
    output.put(determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_ICU_ALIVE), true);
    output.put(determineLabel(dataItemContext, CUMULATIVE_LENGTHOFSTAY_ICU_DEAD), true);
    output.put(CURRENT_TREATMENTLEVEL_CROSSTAB, true);
    return output;
  }

  protected static boolean isItemNotExcluded(
      Map<String, Boolean> mapExcludeDataItems,
      String currentMaxtreatmentlevelLabel,
      boolean defaultValue) {
    return !mapExcludeDataItems.getOrDefault(currentMaxtreatmentlevelLabel, defaultValue);
  }

  protected String addDebugLabel(String dataItem) {
    return dataItem + "." + DEBUG;
  }

  protected void reportMissingFields(List<MiiEncounter> encounters) {
    // Encounter.period.start is mandatory and critical for many data items
    List<String> casesWithoutPeriodStart =
        encounters.parallelStream()
            .filter(x -> !x.isPeriodStartExistent())
            .map(MiiEncounter::getCaseId)
            .toList();
    if (!casesWithoutPeriodStart.isEmpty()) {
      log.debug(
          "Warning: {} Encounters without period/period.start element have been detected [for "
              + "example case with id: {}]",
          casesWithoutPeriodStart.size(),
          casesWithoutPeriodStart.getFirst());
    }
    // Inform about Encounter.location entries that probably contain identifier instead of
    // references which is technically valid but doesn't allow gathering icu information.
    List<String> encounterIdsWithLocationEntryButMissingRef =
        encounters.parallelStream()
            .filter(Encounter::hasLocation)
            .filter(
                x -> x.getLocation().stream().anyMatch(loc -> !isLocationReferenceExisting(loc)))
            .map(MiiEncounter::getId)
            .toList();
    if (!encounterIdsWithLocationEntryButMissingRef.isEmpty()) {
      log.warn(
          "Warning: {} Encounters with Encounter.location attribute but missing reference found. "
              + "[for example encounter with id: {}]",
          encounterIdsWithLocationEntryButMissingRef.size(),
          encounterIdsWithLocationEntryButMissingRef.getFirst());
    }
  }

  protected void reportAttributeArtifacts(List<MiiEncounter> encounters) {
    Set<String> encounterIdsToBeFiltered = new HashSet<>();
    // Pre-stationary and post-stationary encounter must have class = AMB
    List<String> preOrPostEncounterNotOutpatient =
        encounters.parallelStream()
            .filter(MiiEncounter::isCaseClassInpatientOrShortStay)
            .filter(x -> x.isCaseTypePreStationary() || x.isCaseTypePostStationary())
            .map(MiiEncounter::getId)
            .toList();
    if (!preOrPostEncounterNotOutpatient.isEmpty()) {
      log.debug(
          "Warning: {} Encounters found where pre-/poststationary encounter don't have class "
              + "'AMB' [example: Encounter/{}]",
          preOrPostEncounterNotOutpatient.size(),
          preOrPostEncounterNotOutpatient.getFirst());
      encounterIdsToBeFiltered.addAll(preOrPostEncounterNotOutpatient);
    }
    // Filtering encounters where the discharge date lies after the admission date
    List<String> dischargeDateBeforeAdmissionDate =
        encounters.parallelStream()
            .filter(Encounter::hasPeriod)
            .filter(
                x -> {
                  boolean valid =
                      x.getPeriod().getStart() != null && x.getPeriod().getEnd() != null;
                  if (!valid) {
                    log.warn(
                        "Encounter {} has an invalid period. start={}, end={}",
                        x.getId(),
                        x.getPeriod().getStart(),
                        x.getPeriod().getEnd());
                  }
                  return valid;
                })
            .filter(x -> x.getPeriod().getEnd().before(x.getPeriod().getStart()))
            .map(MiiEncounter::getId)
            .toList();
    if (!dischargeDateBeforeAdmissionDate.isEmpty()) {
      log.debug(
          "Warning: {} Encounters found where the discharge date is before the admission date "
              + "[example: Encounter/{}]",
          dischargeDateBeforeAdmissionDate.size(),
          dischargeDateBeforeAdmissionDate.getFirst());
      encounterIdsToBeFiltered.addAll(dischargeDateBeforeAdmissionDate);
    }
    if (!encounterIdsToBeFiltered.isEmpty())
      log.warn(
          "{} facility contact encounters getting filtered because of unexpected attributes.",
          encounterIdsToBeFiltered.size());
  }

  private boolean isSupplyContactFound(List<MiiEncounter> supplyContacts) {
    boolean supplyContactsFound = !supplyContacts.isEmpty();
    if (!supplyContactsFound) {
      log.warn(
          "Warning: No supply contacts ('Versorgungsstellenkontakte') were found. All the data "
              + "items that require"
              + " the transfer history are therefore excluded from the output.");
    }
    return supplyContactsFound;
  }

  /**
   * Filters out encounters with invalid status, unless explicitly allowed via configuration.
   *
   * <p>This method should theoretically never exclude any encounters in production, since encounter
   * status validation is already enforced during data retrieval. It exists primarily as a safeguard
   * and is especially relevant for unit tests. An encounter is considered valid if:
   *
   * <ul>
   *   <li>{@code isEncounterStatusValid()} returns {@code true}, or
   *   <li>{@code getStatus() == UNKNOWN} and the global flag {@code
   *       useOutpatientEncounterWithStatusUnknown} is enabled
   * </ul>
   *
   * @param encounters the full list of encounters to validate
   * @param globalConfiguration global config containing feature flags
   * @return list of encounters considered valid for further processing
   */
  protected List<MiiEncounter> filterEncounterByStatus(
      List<MiiEncounter> encounters, GlobalConfiguration globalConfiguration) {

    // Partition the encounters into valid (true) and invalid (false) groups
    Map<Boolean, List<MiiEncounter>> partitioned =
        encounters.stream()
            .collect(
                Collectors.partitioningBy(
                    encounter ->
                        encounter.isEncounterStatusValid()
                            || (globalConfiguration.getUseOutpatientEncounterWithStatusUnknown()
                                && encounter.getStatus() == EncounterStatus.UNKNOWN)));

    List<MiiEncounter> validEncounters = partitioned.get(true);
    List<MiiEncounter> filteredOut = partitioned.get(false);

    // Defensive logging for unexpected cases — mostly for unit testing or fallback scenarios
    if (!filteredOut.isEmpty()) {
      log.debug(
          "Warning: {} encounters with invalid status were filtered out [e.g. case ID: {}]",
          filteredOut.size(),
          filteredOut.getFirst().getId());
    }
    return validEncounters;
  }

  private void addValuesToTimelineMaxMap(
      String item, Set<String> value, Map<String, List<Long>> mapResultTreatment) {
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
   * Removes duplicate patient IDs (pids) from treatment levels, keeping only valid entries in each
   * list.
   *
   * @param cumulativeOutpatientEncounter List of outpatient cases for cumulative logic
   * @param cumulativeStandardWardEncounter List of standard ward cases for cumulative logic
   * @param cumulativeIcuEncounter List of ICU cases for cumulative logic
   * @param cumulativeIcuVentEncounter List of Ventilation cases for cumulative logic
   * @param cumulativeIcuEcmoEncounter List of ECMO cases for cumulative logic
   */
  private void removeDuplicatePids(
      List<MiiEncounter> cumulativeOutpatientEncounter,
      List<MiiEncounter> cumulativeStandardWardEncounter,
      List<MiiEncounter> cumulativeIcuEncounter,
      List<MiiEncounter> cumulativeIcuVentEncounter,
      List<MiiEncounter> cumulativeIcuEcmoEncounter) {

    // Create sets of patient IDs for each encounter type
    Set<String> pidsOutpatient = createPidList(cumulativeOutpatientEncounter);
    Set<String> pidsStandardWard = createPidList(cumulativeStandardWardEncounter);
    Set<String> pidsIcu = createPidList(cumulativeIcuEncounter);
    Set<String> pidsVent = createPidList(cumulativeIcuVentEncounter);
    Set<String> pidsEcmo = createPidList(cumulativeIcuEcmoEncounter);

    // Helper method to remove duplicate patient IDs across levels
    removeDuplicates(pidsOutpatient, pidsStandardWard, pidsIcu, pidsVent, pidsEcmo);
    removeDuplicates(pidsStandardWard, pidsIcu, pidsVent, pidsEcmo);
    removeDuplicates(pidsIcu, pidsVent, pidsEcmo);
    removeDuplicates(pidsVent, pidsEcmo);

    // Filter each encounter list based on updated patient IDs
    filterEncounters(cumulativeOutpatientEncounter, pidsOutpatient);
    filterEncounters(cumulativeStandardWardEncounter, pidsStandardWard);
    filterEncounters(cumulativeIcuEncounter, pidsIcu);
    filterEncounters(cumulativeIcuVentEncounter, pidsVent);
    filterEncounters(cumulativeIcuEcmoEncounter, pidsEcmo);
  }

  private void removeDuplicatePids(
      List<MiiEncounter> cumulativeOutpatientEncounter,
      List<MiiEncounter> cumulativeStandardWardEncounter,
      List<MiiEncounter> cumulativeIcuUndiffEncounter) {

    // Create sets of patient IDs for each encounter type
    Set<String> pidsOutpatient = createPidList(cumulativeOutpatientEncounter);
    Set<String> pidsStandardWard = createPidList(cumulativeStandardWardEncounter);
    Set<String> pidsIcuUndiff = createPidList(cumulativeIcuUndiffEncounter);

    // Helper method to remove duplicate patient IDs across levels
    removeDuplicates(pidsOutpatient, pidsStandardWard, pidsIcuUndiff);
    removeDuplicates(pidsStandardWard, pidsIcuUndiff);

    // Filter each encounter list based on updated patient IDs
    filterEncounters(cumulativeOutpatientEncounter, pidsOutpatient);
    filterEncounters(cumulativeStandardWardEncounter, pidsStandardWard);
  }

  /** Removes duplicate patient IDs from a base set compared to other sets. */
  @SafeVarargs
  private void removeDuplicates(Set<String> baseSet, Set<String>... otherSets) {
    for (Set<String> otherSet : otherSets) {
      baseSet.removeAll(otherSet);
    }
  }

  /** Filters the given encounter list by retaining only entries with valid patient IDs. */
  private void filterEncounters(List<MiiEncounter> encounters, Set<String> validPids) {
    encounters.removeIf(e -> !validPids.contains(e.getPatientId()));
  }

  private Set<String> createPidList(List<MiiEncounter> listCumulativeEncounter) {
    return listCumulativeEncounter.parallelStream()
        .map(MiiEncounter::getPatientId)
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
      // Covid uses default label
      case INFLUENZA -> INFLUENZA_PREFIX + defaultLabel;
      case KIDS_RADAR -> KIDS_RADAR_PREFIX + defaultLabel;
      case KIDS_RADAR_KJP -> KIDS_RADAR_PREFIX_KJP + defaultLabel;
      case KIDS_RADAR_PED -> KIDS_RADAR_PREFIX_PED + defaultLabel;
      case KIDS_RADAR_PED_RSV -> KIDS_RADAR_PREFIX_PED_RSV + defaultLabel;
      case ACRIBIS -> KIDS_RADAR_PREFIX_ACRIBIS + defaultLabel;
      case BCT -> BCT_PREFIX + defaultLabel;
      case SNID -> SNID_PREFIX + defaultLabel;
      default -> defaultLabel;
    };
  }

  /**
   * Some data items generate the same output, but are based on different data contexts. Different
   * prefixes are set depending on this context.
   */
  public static String determineKiRaLabel(KidsRadarDataItemContext context, String defaultLabel) {
    return switch (context) {
      case KJP -> KIDS_RADAR_PREFIX_KJP + defaultLabel;
      case PED -> KIDS_RADAR_PREFIX_PED + defaultLabel;
      case PED_RSV -> KIDS_RADAR_PREFIX_PED_RSV + defaultLabel;
      case PED_COV -> KIDS_RADAR_PREFIX_PED_COV + defaultLabel;
      case PED_INFL -> KIDS_RADAR_PREFIX_PED_INFL + defaultLabel;
      case PED_PERTUSSIS -> KIDS_RADAR_PREFIX_PED_PERTUSSIS + defaultLabel;
    };
  }

  // Method to create cumulative length of stay hospital data for different vital statuses
  private void createCumulativeLengthOfStayHospitalData(
      List<MiiEncounter> facilityEncounters,
      String label,
      Map<String, Map<Long, Set<String>>> mapDays,
      boolean debug,
      VitalStatus vitalStatus,
      List<DiseaseDataItem> currentDataList) {

    // Create mapDays based on vital status
    Map<String, Map<Long, Set<String>>> mapDaysFiltered =
        createLengthOfStayHospitalByVitalstatus(facilityEncounters, mapDays, vitalStatus);

    // Sum days per patient and keep case ids for debug
    Map<String, Long> totalDaysPerPatient = new HashMap<>();
    Map<String, Set<String>> caseIdsPerPatient = new HashMap<>();
    mapDaysFiltered.forEach(
        (patientId, daysMap) -> {
          long totalDays =
              daysMap.entrySet().stream()
                  // multiplication needed because cases can have the same length of stay
                  .mapToLong(e -> e.getKey() * e.getValue().size())
                  .sum();
          Set<String> allCaseIds =
              daysMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
          totalDaysPerPatient.put(patientId, totalDays);
          caseIdsPerPatient.put(patientId, allCaseIds);
        });

    // Keep sorted output
    List<Map.Entry<String, Long>> sortedPatients =
        totalDaysPerPatient.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();

    List<Long> hospitalDays =
        sortedPatients.stream().map(Map.Entry::getValue).collect(Collectors.toList());

    List<Map<String, Set<String>>> debugPerPatient =
        sortedPatients.stream()
            .map(e -> Map.of(e.getKey(), caseIdsPerPatient.get(e.getKey())))
            .collect(Collectors.toList());

    // Add data to currentDataList
    currentDataList.add(new DiseaseDataItem(label, ITEMTYPE_LIST, hospitalDays));
    if (debug) {
      currentDataList.add(
          new DiseaseDataItem(addDebugLabel(label), ITEMTYPE_DEBUG, debugPerPatient));
    }
  }

  // Method to create cumulative length of stay ICU data for different vital statuses
  private void createCumulativeLengthOfStayIcuData(
      String label,
      Map<TreatmentLevels, List<MiiEncounter>> mapIcuDiseasePositiveOverall,
      boolean debug,
      VitalStatus vitalStatus,
      List<DiseaseDataItem> currentDataList,
      Map<String, Map<Long, Set<String>>> mapIcuLengthList) {
    // Create mapDays based on vital status; take all if it's not set
    Map<String, Map<Long, Set<String>>> mapDaysFiltered = mapIcuLengthList;
    if (vitalStatus != null) {
      mapDaysFiltered =
          createIcuLengthListByVitalstatus(
              vitalStatus, mapIcuLengthList, mapIcuDiseasePositiveOverall);
    }
    // Initialize lists to maintain the association between ICU hours and case IDs
    List<Entry<Long, Set<String>>> icuEntries = new ArrayList<>();

    // Populate icuEntries with hours and case IDs
    mapDaysFiltered.forEach(
        (_, hoursMap) ->
            hoursMap.forEach(
                (hours, caseIds) -> icuEntries.add(new SimpleEntry<>(hours, caseIds))));

    // Sort ICU entries based on ICU hours
    icuEntries.sort(Comparator.comparingLong(Entry::getKey));

    // Extract ICU hours and case IDs maintaining the association
    List<Long> listHours = icuEntries.stream().map(Entry::getKey).collect(Collectors.toList());
    List<String> caseIds = new ArrayList<>();
    // Ensure case IDs are ordered according to listHours
    icuEntries.forEach(entry -> caseIds.addAll(entry.getValue()));

    // Add data to currentDataList
    currentDataList.add(new DiseaseDataItem(label, ITEMTYPE_LIST, listHours));
    if (debug) {
      currentDataList.add(new DiseaseDataItem(addDebugLabel(label), ITEMTYPE_DEBUG, caseIds));
    }
  }

  private void processIcuTreatmentLevel(
      DataItemContext dataItemContext,
      String treatmentLevelLabel,
      TreatmentLevels treatmentLevel,
      List<DiseaseDataItem> currentDataList,
      Map<String, Boolean> mapExcludeDataItems,
      String currentMaxtreatmentlevelLabel,
      Map<TreatmentLevels, List<MiiEncounter>> mapIcuDiseasePositiveOverall,
      Map<TreatmentLevels, List<MiiEncounter>> mapPositiveEncounterByClass,
      DashboardData dbData,
      Boolean useIcuUndiff) {

    String label = determineLabel(dataItemContext, treatmentLevelLabel);

    if (isItemNotExcluded(mapExcludeDataItems, label, false)
        && isItemNotExcluded(mapExcludeDataItems, currentMaxtreatmentlevelLabel, false)) {

      var currentMax =
          new DataBuilder()
              .icuDiseaseMap(mapIcuDiseasePositiveOverall)
              .dbData(dbData)
              .treatmentLevel(treatmentLevel)
              .useIcuUndiff(useIcuUndiff)
              .buildNumberOfCurrentMaxTreatmentLevel();

      currentDataList.add(
          new DiseaseDataItem(
              label,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(dbData)
                  .encounterSubSet(currentMax)
                  .mapPositiveEncounterByClass(mapPositiveEncounterByClass)
                  .useIcuUndiff(useIcuUndiff)
                  .buildCurrentMaxAgeMap()));
    }
  }

  private Set<MiiObservation> buildObservationsByResult(
      DashboardLogicFixedValues result, DataItemContext context, DashboardData dbData) {
    return new DataBuilder()
        .labResult(result)
        .dataItemContext(context)
        .dbData(dbData)
        .buildObservationsByResult();
  }
}
