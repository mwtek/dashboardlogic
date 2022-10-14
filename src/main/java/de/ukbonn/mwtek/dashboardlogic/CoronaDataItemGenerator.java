/*
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */
package de.ukbonn.mwtek.dashboardlogic;

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.ALIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.BORDERLINE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.CASESTATUS_ALL;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.CASESTATUS_INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.CASESTATUS_OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.CITY_BONN;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.DEAD;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.DIVERSE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.FEMALE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.GENDER_DIVERSE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.GENDER_FEMALE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.GENDER_MALE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.INPATIENT_ITEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.MALE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.NEGATIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.OUTPATIENT_ITEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.POSITIVE_RESULT;
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
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_ICU_DEAD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_ICU_LIVE;
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
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DEATHS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_TESTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_TEST_POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_VARIANTTESTRESULTS;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaLogic;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CumulativeResult;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CumulativeVariantTestResults;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CumulativeZipCode;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age.CumulativeAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGender;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGenderByClass;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayHospital;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayIcu;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.age.CurrentMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineDeath;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineTests;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineVariantTestResults;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.ListNumberPair;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Class in which the individual {@link CoronaDataItem DataItems} of the Json specification are
 * generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class CoronaDataItemGenerator {

  // Initialization of the fhir resource lists
  List<UkbCondition> listConditions;
  List<UkbObservation> listObservations;
  List<UkbPatient> listPatients;
  List<UkbEncounter> listEncounters;
  List<UkbProcedure> listIcuProcedures;
  List<UkbLocation> listLocations;

  // map with all inpatient c19 cases, which is needed for internal reports in the UKB
  HashMap<String, List<String>> mapCurrentTreatmentlevelCaseNrs = new HashMap<>();

  /**
   * Initialization of the CoronaResults object with the required FhirRessource lists.
   *
   * @param listConditions    List of FHIR condition resources for U07.1 and U07.2 diagnoses
   * @param listObservations  List of FHIR Observation Resources based on the Corona Lab Codes
   * @param listPatients      List of FHIR patient resources based on the Condition and Observation
   *                          inputs
   * @param listEncounters    List of FHIR encounter resources based on the Condition and
   *                          Observation inputs
   * @param listIcuProcedures List of FHIR ICU procedures resources based on the Patient/Encounter
   *                          inputs
   * @param listLocations     List of all FHIR location resources based on the encounter inputs
   */
  public CoronaDataItemGenerator(List<UkbCondition> listConditions,
      List<UkbObservation> listObservations,
      List<UkbPatient> listPatients, List<UkbEncounter> listEncounters,
      List<UkbProcedure> listIcuProcedures, List<UkbLocation> listLocations) {
    super();
    this.listConditions = listConditions;
    this.listObservations = listObservations;
    this.listPatients = listPatients;
    this.listEncounters = listEncounters;
    this.listIcuProcedures = listIcuProcedures;
    this.listLocations = listLocations;
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems Map with data items to be excluded from the output (e.g.
   *                            "current.treatmentlevel").
   * @param debug               Flag to provide debug information (e.g. casenrs) in the output.
   * @param variantSettings     {@link VariantSettings Configuration} of extended covid variants for
   *                            the definition of not yet known/captured variants.
   * @param inputCodeSettings   The configuration of the parameterizable codes such as the
   *                            observation codes or procedure codes.
   * @return List with all the {@link CoronaDataItem data items} that are defined in the corona
   * dashboard json specification
   */
  @SuppressWarnings("unused")
  public ArrayList<CoronaDataItem> getDataItems(Map<String, Boolean> mapExcludeDataItems,
      Boolean debug, VariantSettings variantSettings, InputCodeSettings inputCodeSettings) {
    ArrayList<CoronaDataItem> currentDataList = new ArrayList<>();
    if (mapExcludeDataItems == null) {
      mapExcludeDataItems = new HashMap<>();
    }

    if (debug == null) {
      debug = false;
    }

    // If there are resources with unfilled mandatory attributes, report them immediately (may give
    // partially reduced result sets)
    reportMissingFields(listEncounters);

    // Marking encounter resources as covid positive via setting of extensions
    CoronaLogic.flagCases(listEncounters, listConditions, listObservations,
        inputCodeSettings);

    HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass =
        CoronaResultFunctionality.createEncounterMap(listEncounters);

    Map<String, List<UkbEncounter>> mapIcu =
        CoronaResultFunctionality.createIcuMap(listEncounters, listLocations,
            listIcuProcedures, inputCodeSettings);
    /* used for current logic */
    Map<String, List<UkbEncounter>> mapCurrentIcu =
        CoronaResultFunctionality.createCurrentIcuMap(listEncounters, listLocations,
            listIcuProcedures, inputCodeSettings);

    // CurrentLogic Classes
    CurrentTreatmentLevel currentTreatmentlevel =
        new CurrentTreatmentLevel(listEncounters, listIcuProcedures);
    CurrentMaxTreatmentLevel currentMaxTreatmentlevel =
        new CurrentMaxTreatmentLevel(listEncounters);

    // CumulativeLogic classes
    CumulativeAge cumulativeAge = new CumulativeAge(listEncounters, listPatients);
    CumulativeGender cumulativeGender = new CumulativeGender(listEncounters, listPatients);
    CumulativeLengthOfStayHospital cumulativeLengthOfStayHospital =
        new CumulativeLengthOfStayHospital(listEncounters);
    CumulativeLengthOfStayIcu cumulativeLengthOfStayIcu =
        new CumulativeLengthOfStayIcu(listEncounters, listLocations);
    CumulativeMaxTreatmentLevelAge cumulativeMaxtreatmentLevelAge =
        new CumulativeMaxTreatmentLevelAge(listPatients);
    CumulativeMaxTreatmentLevel cumulativeMaxtreatmentLevel =
        new CumulativeMaxTreatmentLevel(listEncounters);
    CumulativeResult cumulativeResult = new CumulativeResult(listObservations);
    CumulativeZipCode cumulativeZipCode = new CumulativeZipCode(listEncounters, listPatients);
    CumulativeVariantTestResults cumulativeVariantTestResults =
        new CumulativeVariantTestResults(listObservations);

    // Timeline Classes
    TimelineMaxTreatmentLevel timelineMaxtreatmentlevel =
        new TimelineMaxTreatmentLevel(listEncounters, listIcuProcedures, listLocations);
    TimelineTests timelineTests = new TimelineTests(listObservations);
    TimelineDeath timelineDeath = new TimelineDeath(listEncounters);

    // Partial lists of current cases broken down by case status
    List<UkbEncounter> listCurrentStandardWardEncounter = new ArrayList<>();
    List<UkbEncounter> listCurrentIcuEncounter = new ArrayList<>();
    List<UkbEncounter> listCurrentVentEncounter = new ArrayList<>();
    List<UkbEncounter> listCurrentEcmoEncounter = new ArrayList<>();

    // Lists of current maxtreatmentlevels
    List<UkbEncounter> listCurrentMaxStationary = new ArrayList<>();
    List<UkbEncounter> listCurrentMaxIcu = new ArrayList<>();
    List<UkbEncounter> listCurrentMaxIcuVent = new ArrayList<>();
    List<UkbEncounter> listCurrentMaxIcuEcmo = new ArrayList<>();

    // Lists of current maxtreatmentlevels with ages
    List<Long> currentMaxStationaryAgeList;
    List<Long> currentMaxIcuAgeList;
    List<Long> currentMaxIcuVentAgeList;
    List<Long> currentMaxIcuEcmoAgeList;

    // Inpatients = STATIONARY + ICU + ICU_VENT + ECMO
    // needed later for the splitting cumulative.gender into
    // cumulative.inpatient/outpatient.gender

    // same for cumulative results
    List<UkbEncounter> listCumulativeAmbulantEncounter =
        cumulativeMaxtreatmentLevel.getCumulativeByClass(mapIcu, OUTPATIENT_ITEM,
            mapPositiveEncounterByClass);

    List<UkbEncounter> listCumulativeStandardWardEncounter =
        cumulativeMaxtreatmentLevel.getCumulativeByClass(mapIcu, INPATIENT_ITEM,
            mapPositiveEncounterByClass);

    // initialize ICU data
    List<UkbEncounter> listCumulativeIcuEncounter =
        cumulativeMaxtreatmentLevel.getCumulativeByIcuLevel(mapIcu, ICU);

    // initialize ICU_VENT data
    List<UkbEncounter> listCumulativeIcuVentEncounter =
        cumulativeMaxtreatmentLevel.getCumulativeByIcuLevel(mapIcu, ICU_VENTILATION);

    // initialize ECMO data
    List<UkbEncounter> listCumulativeIcuEcmoEncounter =
        cumulativeMaxtreatmentLevel.getCumulativeByIcuLevel(mapIcu, ICU_ECMO);

    removeDuplicatePids(listCumulativeAmbulantEncounter, listCumulativeStandardWardEncounter,
        listCumulativeIcuEncounter, listCumulativeIcuVentEncounter,
        listCumulativeIcuEcmoEncounter);

    // Inpatients = STATIONARY + ICU + ICU_VENT + ECMO
    // needed later for the splitting cumulative.gender into
    // cumulative.inpatient/outpatient.gender
    List<UkbEncounter> listCumulativeInpatients = new ArrayList<>();
    listCumulativeInpatients.addAll(listCumulativeStandardWardEncounter);
    listCumulativeInpatients.addAll(listCumulativeIcuEncounter);
    listCumulativeInpatients.addAll(listCumulativeIcuVentEncounter);
    listCumulativeInpatients.addAll(listCumulativeIcuEcmoEncounter);

    CoronaDataItem cd;
    // current treatmentlevel
    if (!mapExcludeDataItems.getOrDefault(CURRENT_TREATMENTLEVEL, false)) {
      cd = new CoronaDataItem();

      cd.setItemname(CURRENT_TREATMENTLEVEL);
      cd.setItemtype(ITEMTYPE_AGGREGATED);

      HashMap<String, Number> listCurrent = new LinkedHashMap<>();

      listCurrentStandardWardEncounter =
          currentTreatmentlevel.getCurrentStandardWardEncounter(mapCurrentIcu, inputCodeSettings);
      listCurrentIcuEncounter =
          currentTreatmentlevel.getCurrentEncounterByIcuLevel(mapCurrentIcu, ICU,
              inputCodeSettings);
      listCurrentVentEncounter =
          currentTreatmentlevel.getCurrentEncounterByIcuLevel(mapCurrentIcu, ICU_VENTILATION,
              inputCodeSettings);
      listCurrentEcmoEncounter =
          currentTreatmentlevel.getCurrentEncounterByIcuLevel(mapCurrentIcu, ICU_ECMO,
              inputCodeSettings);

      listCurrent.put(NORMAL_WARD.getValue(), listCurrentStandardWardEncounter.size());
      listCurrent.put(ICU.getValue(), listCurrentIcuEncounter.size());
      listCurrent.put(ICU_VENTILATION.getValue(), listCurrentVentEncounter.size());
      listCurrent.put(ICU_ECMO.getValue(), listCurrentEcmoEncounter.size());

      cd.setData(listCurrent);
      currentDataList.add(cd);

      // Storing the casenrs of the current treatmentlevel. This list will be used in coming item generations.
      HashMap<String, List<String>> mapCurrentTreatmentlevelCaseNrs =
          createMapCurrentTreatmentlevelCasenrs(listCurrentStandardWardEncounter,
              listCurrentIcuEncounter, listCurrentVentEncounter, listCurrentEcmoEncounter);
      this.setMapCurrentTreatmentlevelCasenrs(mapCurrentTreatmentlevelCaseNrs);

      if (debug) {

        cd = new CoronaDataItem();
        cd.setItemname(addCaseNrsToLabel(CURRENT_TREATMENTLEVEL));
        cd.setItemtype(ITEMTYPE_DEBUG);

        cd.setData(mapCurrentTreatmentlevelCaseNrs);
        currentDataList.add(cd);

      }
    }

    // current maxtreatmentlevel
    if (!mapExcludeDataItems.getOrDefault(CURRENT_MAXTREATMENTLEVEL, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CURRENT_MAXTREATMENTLEVEL);
      cd.setItemtype(ITEMTYPE_AGGREGATED);
      Map<String, Number> mapCurrentMax = new LinkedHashMap<>();

      listCurrentMaxStationary =
          currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu, INPATIENT_ITEM);

      listCurrentMaxIcu = currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu, ICU);

      listCurrentMaxIcuVent =
          currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu, ICU_VENTILATION);

      listCurrentMaxIcuEcmo =
          currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu, ICU_ECMO);

      mapCurrentMax.put(NORMAL_WARD.getValue(), listCurrentMaxStationary.size());
      mapCurrentMax.put(ICU.getValue(), listCurrentMaxIcu.size());
      mapCurrentMax.put(ICU_VENTILATION.getValue(), listCurrentMaxIcuVent.size());
      mapCurrentMax.put(ICU_ECMO.getValue(), listCurrentMaxIcuEcmo.size());
      cd.setData(mapCurrentMax);
      currentDataList.add(cd);

      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname(addCaseNrsToLabel(CURRENT_MAXTREATMENTLEVEL));
        cd.setItemtype(ITEMTYPE_DEBUG);
        List<String> listStationaryCaseNrs = new ArrayList<>();
        List<String> listIcuCaseNrs = new ArrayList<>();
        List<String> listVentCaseNrs = new ArrayList<>();
        List<String> listEcmoCaseNrs = new ArrayList<>();
        HashMap<String, List<String>> resultCurrentTreatmentCaseNrs = new LinkedHashMap<>();

        listCurrentMaxStationary.forEach(encounter -> listStationaryCaseNrs.add(encounter.getId()));
        listCurrentMaxIcu.forEach(encounter -> listIcuCaseNrs.add(encounter.getId()));
        listCurrentMaxIcuVent.forEach(encounter -> listVentCaseNrs.add(encounter.getId()));
        listCurrentMaxIcuEcmo.forEach(encounter -> listEcmoCaseNrs.add(encounter.getId()));

        resultCurrentTreatmentCaseNrs.put(NORMAL_WARD.getValue(), listStationaryCaseNrs);
        resultCurrentTreatmentCaseNrs.put(ICU.getValue(), listIcuCaseNrs);
        resultCurrentTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), listVentCaseNrs);
        resultCurrentTreatmentCaseNrs.put(ICU_ECMO.getValue(), listEcmoCaseNrs);

        cd.setData(resultCurrentTreatmentCaseNrs);
        currentDataList.add(cd);
      }
    }

    // current.age.maxtreatmentlevel.normal_ward
    if (!mapExcludeDataItems.getOrDefault(CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
      cd.setItemtype(ITEMTYPE_LIST);

      if (mapExcludeDataItems.getOrDefault(CURRENT_MAXTREATMENTLEVEL, false)) {
        listCurrentMaxStationary =
            currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
                INPATIENT_ITEM);
      }

      CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
          new CurrentMaxTreatmentLevelAge(listPatients, listCurrentMaxStationary);

      currentMaxStationaryAgeList =
          currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass);

      cd.setData(currentMaxStationaryAgeList);
      currentDataList.add(cd);
    }
    // current.age.maxtreatmentlevel.icu
    if (!mapExcludeDataItems.getOrDefault(CURRENT_AGE_MAXTREATMENTLEVEL_ICU, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CURRENT_AGE_MAXTREATMENTLEVEL_ICU);
      cd.setItemtype(ITEMTYPE_LIST);

      if (mapExcludeDataItems.getOrDefault(CURRENT_MAXTREATMENTLEVEL, false)) {
        listCurrentMaxIcu =
            currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu, ICU);
      }

      CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
          new CurrentMaxTreatmentLevelAge(listPatients, listCurrentMaxIcu);

      currentMaxIcuAgeList =
          currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass);

      cd.setData(currentMaxIcuAgeList);
      currentDataList.add(cd);
    }
    // current.age.maxtreatmentlevel.icu_with_ventilation
    if (!mapExcludeDataItems.getOrDefault(CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION,
        false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION);
      cd.setItemtype(ITEMTYPE_LIST);

      if (mapExcludeDataItems.getOrDefault(CURRENT_MAXTREATMENTLEVEL, false)) {
        listCurrentMaxIcuVent = currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
            ICU_VENTILATION);
      }

      CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
          new CurrentMaxTreatmentLevelAge(listPatients, listCurrentMaxIcuVent);

      currentMaxIcuVentAgeList =
          currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass);

      cd.setData(currentMaxIcuVentAgeList);
      currentDataList.add(cd);
    }
    // current.age.maxtreatmentlevel.icu_with_ecmo
    if (!mapExcludeDataItems.getOrDefault(CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO);
      cd.setItemtype(ITEMTYPE_LIST);

      if (mapExcludeDataItems.getOrDefault(CURRENT_MAXTREATMENTLEVEL, false)) {
        listCurrentMaxIcuEcmo =
            currentMaxTreatmentlevel.getNumberOfCurrentMaxTreatmentLevel(mapIcu, ICU_ECMO);
      }

      CurrentMaxTreatmentLevelAge currentMaxtreatmentlevelAge =
          new CurrentMaxTreatmentLevelAge(listPatients, listCurrentMaxIcuEcmo);

      currentMaxIcuEcmoAgeList =
          currentMaxtreatmentlevelAge.createCurrentMaxAgeMap(mapPositiveEncounterByClass);

      cd.setData(currentMaxIcuEcmoAgeList);
      currentDataList.add(cd);
    }

    // cumulative.results
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_RESULTS, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_RESULTS);
      cd.setItemtype(ITEMTYPE_AGGREGATED);

      Set<UkbObservation> setPositiveObservations =
          cumulativeResult.getObservationsByResult(POSITIVE, inputCodeSettings);
      Set<UkbObservation> setNegativeObservations =
          cumulativeResult.getObservationsByResult(NEGATIVE, inputCodeSettings);
      Set<UkbObservation> setBorderlineLabCaseNrs =
          cumulativeResult.getObservationsByResult(BORDERLINE, inputCodeSettings);
      Map<String, Number> cumulativeResultMap = new LinkedHashMap<>();

      cumulativeResultMap.put(POSITIVE.getValue(), setPositiveObservations.size());
      cumulativeResultMap.put(BORDERLINE.getValue(), setBorderlineLabCaseNrs.size());
      cumulativeResultMap.put(NEGATIVE.getValue(), setNegativeObservations.size());

      cd.setData(cumulativeResultMap);
      currentDataList.add(cd);
    }

    // cumulative varianttestresults
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_VARIANTTESTRESULTS, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_VARIANTTESTRESULTS);
      cd.setItemtype(ITEMTYPE_AGGREGATED);

      Map<String, Integer> resultMap =
          cumulativeVariantTestResults.createVariantTestResultMap(variantSettings,
              inputCodeSettings);

      cd.setData(resultMap);
      currentDataList.add(cd);
    }
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_GENDER, false)) {
      // cumulative.gender
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_GENDER);
      cd.setItemtype(ITEMTYPE_AGGREGATED);

      Map<String, Number> cumulativeGenderMap = new LinkedHashMap<>();

      cumulativeGenderMap.put(MALE_SPECIFICATION.getValue(),
          cumulativeGender.getGenderCount(listEncounters, listPatients,
              GENDER_MALE.getValue()));
      cumulativeGenderMap.put(FEMALE_SPECIFICATION.getValue(),
          cumulativeGender.getGenderCount(listEncounters, listPatients,
              GENDER_FEMALE.getValue()));
      cumulativeGenderMap.put(DIVERSE_SPECIFICATION.getValue(),
          cumulativeGender.getGenderCount(listEncounters, listPatients,
              GENDER_DIVERSE.getValue()));

      cd.setData(cumulativeGenderMap);
      currentDataList.add(cd);
    }

    // cumulative.age
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_AGE, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_AGE);
      cd.setItemtype(ITEMTYPE_LIST);

      cd.setData(cumulativeAge.getAgeDistributionsByCaseClass(CASESTATUS_ALL.getValue()).toArray());
      currentDataList.add(cd);
    }

    // cumulative.maxtreatmentlevel
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_MAXTREATMENTLEVEL, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_MAXTREATMENTLEVEL);
      cd.setItemtype(ITEMTYPE_AGGREGATED);
      LinkedHashMap<String, Number> mapCumulativeMaxtreatmentlevel = new LinkedHashMap<>();

      mapCumulativeMaxtreatmentlevel.put(OUTPATIENT_ITEM.getValue(),
          listCumulativeAmbulantEncounter.size());
      mapCumulativeMaxtreatmentlevel.put(NORMAL_WARD.getValue(),
          listCumulativeStandardWardEncounter.size());
      mapCumulativeMaxtreatmentlevel.put(ICU.getValue(), listCumulativeIcuEncounter.size());
      mapCumulativeMaxtreatmentlevel.put(ICU_VENTILATION.getValue(),
          listCumulativeIcuVentEncounter.size());
      mapCumulativeMaxtreatmentlevel.put(ICU_ECMO.getValue(),
          listCumulativeIcuEcmoEncounter.size());

      cd.setData(mapCumulativeMaxtreatmentlevel);
      currentDataList.add(cd);

      // add casenrs
      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname(addCaseNrsToLabel(CUMULATIVE_MAXTREATMENTLEVEL));
        cd.setItemtype(ITEMTYPE_DEBUG);
        HashMap<String, Map<String, List<String>>> resultMaxTreatmentCaseNrs =
            new LinkedHashMap<>();

        resultMaxTreatmentCaseNrs.put(OUTPATIENT_ITEM.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(NORMAL_WARD.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(ICU.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), new HashMap<>());
        resultMaxTreatmentCaseNrs.put(ICU_ECMO.getValue(), new HashMap<>());

        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeAmbulantEncounter,
            OUTPATIENT_ITEM.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeStandardWardEncounter,
            NORMAL_WARD.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeIcuEncounter,
            ICU.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeIcuVentEncounter,
            ICU_VENTILATION.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeIcuEcmoEncounter,
            ICU_ECMO.getValue(), resultMaxTreatmentCaseNrs);

        cd.setData(resultMaxTreatmentCaseNrs);
        currentDataList.add(cd);
      }
    }
    // cumulative.age.maxtreatmentlevel.outpatient
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT);
      cd.setItemtype(ITEMTYPE_LIST);

      List<Integer> cumulativeMaxtreatmentlevelOutpatientAgeList =
          cumulativeMaxtreatmentLevelAge.createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcu, OUTPATIENT_ITEM);

      cd.setData(cumulativeMaxtreatmentlevelOutpatientAgeList);
      currentDataList.add(cd);
    }
    // cumulative.age.maxtreatmentlevel.normal_ward
    if (!mapExcludeDataItems.getOrDefault(CUMULATE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
      cd.setItemtype(ITEMTYPE_LIST);

      List<Integer> cumulativeMaxtreatmentlevelStationaryAgeList =
          cumulativeMaxtreatmentLevelAge.createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcu, NORMAL_WARD);

      cd.setData(cumulativeMaxtreatmentlevelStationaryAgeList);
      currentDataList.add(cd);
    }
    // cumulative.age.maxtreatmentlevel.icu
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU);
      cd.setItemtype(ITEMTYPE_LIST);

      List<Integer> cumulativeMaxtreatmentlevelIcuAgeList =
          cumulativeMaxtreatmentLevelAge.createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcu, ICU);

      cd.setData(cumulativeMaxtreatmentlevelIcuAgeList);
      currentDataList.add(cd);
    }
    // cumulative.age.maxtreatmentlevel.icu_with_ventilation
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION,
        false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION);
      cd.setItemtype(ITEMTYPE_LIST);

      List<Integer> cumulativeMaxtreatmentlevelIcuVentAgeList =
          cumulativeMaxtreatmentLevelAge.createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcu, ICU_VENTILATION);

      cd.setData(cumulativeMaxtreatmentlevelIcuVentAgeList);
      currentDataList.add(cd);
    }
    // cumulative.age.maxtreatmentlevel.icu_with_ecmo
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO);
      cd.setItemtype(ITEMTYPE_LIST);

      List<Integer> cumulativeMaxtreatmentlevelIcuEcmoAgeList =
          cumulativeMaxtreatmentLevelAge.createMaxTreatmentLevelAgeMap(
              mapPositiveEncounterByClass, mapIcu, ICU_ECMO);

      cd.setData(cumulativeMaxtreatmentlevelIcuEcmoAgeList);
      currentDataList.add(cd);
    }
    // cumulative zip code
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_ZIPCODE, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_ZIPCODE);
      cd.setItemtype(ITEMTYPE_LIST);

      List<String> listZipCodes = cumulativeZipCode.createZipCodeList();

      cd.setData(listZipCodes);
      currentDataList.add(cd);
    }

    // timeline tests
    if (!mapExcludeDataItems.getOrDefault(TIMELINE_TESTS, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(TIMELINE_TESTS);
      cd.setItemtype(ITEMTYPE_LIST);
      ListNumberPair testsMap = timelineTests.createTimelineTestsMap();

      cd.setData(testsMap);
      currentDataList.add(cd);
    }

    // timeline.test.positive
    if (!mapExcludeDataItems.getOrDefault(TIMELINE_TEST_POSITIVE, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(TIMELINE_TEST_POSITIVE);
      cd.setItemtype(ITEMTYPE_LIST);

      ListNumberPair testPositivePair = timelineTests.createTimelineTestPositiveMap(
          inputCodeSettings);

      cd.setData(testPositivePair);
      currentDataList.add(cd);
    }

    // timeline.varianttestresults
    if (!mapExcludeDataItems.getOrDefault(TIMELINE_VARIANTTESTRESULTS, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(TIMELINE_VARIANTTESTRESULTS);
      cd.setItemtype(ITEMTYPE_LIST);

      TimelineVariantTestResults variantTestResult =
          new TimelineVariantTestResults(listObservations);

      cd.setData(variantTestResult.createTimelineVariantsTests(variantSettings, inputCodeSettings));
      currentDataList.add(cd);
    }
    // timeline.maxtreatmentlevel
    if (!mapExcludeDataItems.getOrDefault(TIMELINE_MAXTREATMENTLEVEL, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(TIMELINE_MAXTREATMENTLEVEL);
      cd.setItemtype(ITEMTYPE_LIST);
      Map<String, Map<Long, Set<String>>> resultMaxTreatmentTimeline;
      Map<String, List<Long>> mapResultTreatment = new LinkedHashMap<>();

      // The result contain the case numbers for debugging and the sums by date
      resultMaxTreatmentTimeline = timelineMaxtreatmentlevel.createMaxTreatmentTimeline(
          inputCodeSettings);

      mapResultTreatment.put(SUBITEMTYPE_DATE, getDatesOutputList());

      for (Map.Entry<String, Map<Long, Set<String>>> entry : resultMaxTreatmentTimeline.entrySet()) {
        for (Map.Entry<Long, Set<String>> secondEntry : entry.getValue().entrySet()) {
          if (entry.getKey().equals(OUTPATIENT_ITEM.getValue())) {
            addValuesToTimelineMaxMap(OUTPATIENT_ITEM.getValue(), secondEntry.getValue(),
                mapResultTreatment);
          } else if (entry.getKey().equals(NORMAL_WARD.getValue())) {
            addValuesToTimelineMaxMap(NORMAL_WARD.getValue(), secondEntry.getValue(),
                mapResultTreatment);
          } else if (entry.getKey().equals(ICU.getValue())) {
            addValuesToTimelineMaxMap(ICU.getValue(), secondEntry.getValue(), mapResultTreatment);
          } else if (entry.getKey().equals(ICU_VENTILATION.getValue())) {
            addValuesToTimelineMaxMap(ICU_VENTILATION.getValue(), secondEntry.getValue(),
                mapResultTreatment);
          } else if (entry.getKey().equals(ICU_ECMO.getValue())) {
            addValuesToTimelineMaxMap(ICU_ECMO.getValue(), secondEntry.getValue(),
                mapResultTreatment);
          } else if (entry.getKey().equals(CASESTATUS_INPATIENT.getValue())) {
            addValuesToTimelineMaxMap(CASESTATUS_INPATIENT.getValue(), secondEntry.getValue(),
                mapResultTreatment);
          }
        }
      }

      cd.setData(mapResultTreatment);
      currentDataList.add(cd);

      // show casenrs for mtl plausibility checks
      // timeline maxtreatmentlevel
      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname(addCaseNrsToLabel(TIMELINE_MAXTREATMENTLEVEL));
        cd.setItemtype(ITEMTYPE_DEBUG);
        Map<String, Map<Long, Set<String>>> resultMaxTreatmentCaseNrs = new LinkedHashMap<>();

        for (Map.Entry<String, Map<Long, Set<String>>> entry : resultMaxTreatmentTimeline.entrySet()) {
          String mapNameAndValues = entry.getKey();
          Map<Long, Set<String>> mapDateAndCaseNr = entry.getValue();
          if (mapNameAndValues.equals(OUTPATIENT_ITEM.getValue())) {
            resultMaxTreatmentCaseNrs.put(OUTPATIENT_ITEM.getValue(), mapDateAndCaseNr);
          } else if (mapNameAndValues.equals(NORMAL_WARD.getValue())) {
            resultMaxTreatmentCaseNrs.put(NORMAL_WARD.getValue(), mapDateAndCaseNr);
          } else if (mapNameAndValues.equals(ICU.getValue())) {
            resultMaxTreatmentCaseNrs.put(ICU.getValue(), mapDateAndCaseNr);
          } else if (mapNameAndValues.equals(ICU_VENTILATION.getValue())) {
            resultMaxTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), mapDateAndCaseNr);
          } else if (mapNameAndValues.equals(ICU_ECMO.getValue())) {
            resultMaxTreatmentCaseNrs.put(ICU_ECMO.getValue(), mapDateAndCaseNr);
          }
        }
        // The date would be redundant in the debug information but is needed, since the rest endpoint is not accepting lists of type item without date lists
        Map<Long, Set<String>> mapTempAndDate = new LinkedHashMap<>();
        resultMaxTreatmentCaseNrs.put(SUBITEMTYPE_DATE, mapTempAndDate);

        cd.setData(resultMaxTreatmentCaseNrs);
        currentDataList.add(cd);
      }
    }

    // cumulative inpatient gender
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_INPATIENT_GENDER, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_INPATIENT_GENDER);
      cd.setItemtype(ITEMTYPE_AGGREGATED);

      HashMap<String, Number> cumulativeInpatientGenderMap = new HashMap<>();

      // listCumulativeInpatients gets initialized in the cumulative.maxtreatmentlevel method ->
      // this method could be excluded

      CumulativeGenderByClass cumulativeInPatientGender =
          new CumulativeGenderByClass(listCumulativeInpatients, listPatients);
      cumulativeInpatientGenderMap.put(MALE_SPECIFICATION.getValue(),
          cumulativeInPatientGender.getGenderCountByCaseClass(GENDER_MALE.getValue(),
              INPATIENT_ITEM.getValue()));
      cumulativeInpatientGenderMap.put(FEMALE_SPECIFICATION.getValue(),
          cumulativeInPatientGender.getGenderCountByCaseClass(GENDER_FEMALE.getValue(),
              INPATIENT_ITEM.getValue()));
      cumulativeInpatientGenderMap.put(DIVERSE_SPECIFICATION.getValue(),
          cumulativeInPatientGender.getGenderCountByCaseClass(GENDER_DIVERSE.getValue(),
              INPATIENT_ITEM.getValue()));

      cd.setData(cumulativeInpatientGenderMap);
      currentDataList.add(cd);
    }

    // cumulative outpatient gender
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_OUTPATIENT_GENDER, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_OUTPATIENT_GENDER);
      cd.setItemtype(ITEMTYPE_AGGREGATED);

      HashMap<String, Number> cumulativeOutpatientGender = new HashMap<>();
      CumulativeGenderByClass cumulativeOutPatientGender =
          new CumulativeGenderByClass(listCumulativeAmbulantEncounter, listPatients);

      cumulativeOutpatientGender.put(MALE_SPECIFICATION.getValue(),
          cumulativeOutPatientGender.getGenderCountByCaseClass(GENDER_MALE.getValue(),
              OUTPATIENT_ITEM.getValue()));
      cumulativeOutpatientGender.put(FEMALE_SPECIFICATION.getValue(),
          cumulativeOutPatientGender.getGenderCountByCaseClass(GENDER_FEMALE.getValue(),
              OUTPATIENT_ITEM.getValue()));
      cumulativeOutpatientGender.put(DIVERSE_SPECIFICATION.getValue(),
          cumulativeOutPatientGender.getGenderCountByCaseClass(GENDER_DIVERSE.getValue(),
              OUTPATIENT_ITEM.getValue()));

      cd.setData(cumulativeOutpatientGender);
      currentDataList.add(cd);
    }

    // cumulative inpatient age
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_INPATIENT_AGE, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_INPATIENT_AGE);
      cd.setItemtype(ITEMTYPE_LIST);

      cd.setData(cumulativeAge.getAgeDistributionsByCaseClass(CASESTATUS_INPATIENT.getValue())
          .toArray());
      currentDataList.add(cd);
    }

    // cumulative outpatient age
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_OUTPATIENT_AGE, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_OUTPATIENT_AGE);
      cd.setItemtype(ITEMTYPE_LIST);

      cd.setData(cumulativeAge.getAgeDistributionsByCaseClass(CASESTATUS_OUTPATIENT.getValue())
          .toArray());
      currentDataList.add(cd);
    }

    // timeline deaths
    if (!mapExcludeDataItems.getOrDefault(TIMELINE_DEATHS, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(TIMELINE_DEATHS);
      cd.setItemtype(ITEMTYPE_LIST);

      ListNumberPair tlDeathPairList = timelineDeath.createTimelineDeathMap();

      cd.setData(tlDeathPairList);

      currentDataList.add(cd);
    }

    // Bonn cross tabulation
    if (!mapExcludeDataItems.getOrDefault(CURRENT_TREATMENTLEVEL_CROSSTAB, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CURRENT_TREATMENTLEVEL_CROSSTAB);
      cd.setItemtype(ITEMTYPE_AGGREGATED);

      Map<String, List> aggData = new HashMap<>();
      aggData.put("columnname",
          Arrays.asList(CITY_BONN.getValue(), ICU.getValue(), ICU_VENTILATION.getValue(),
              ICU_ECMO.getValue()));
      aggData.put("state",
          Arrays.asList("0000", "0100", "0110", "0111", "1000", "1100", "1110", "1111"));

      aggData.put("value", new ArrayList<>());
      LinkedHashMap<CoronaFixedValues, List<UkbEncounter>> listCrosstabmaxtreatmentlevels =
          new LinkedHashMap<>();
      listCrosstabmaxtreatmentlevels.put(INPATIENT_ITEM, listCurrentStandardWardEncounter);
      listCrosstabmaxtreatmentlevels.put(ICU, listCurrentIcuEncounter);

      List<UkbEncounter> listVentEncounter = new ArrayList<>(listCurrentVentEncounter);
      listCrosstabmaxtreatmentlevels.put(ICU_VENTILATION, listVentEncounter);

      List<UkbEncounter> listEcmoEncounter = new ArrayList<>(listCurrentEcmoEncounter);
      listCrosstabmaxtreatmentlevels.put(ICU_ECMO, listEcmoEncounter);

      Set<UkbEncounter> setAllCurrentEncounter = new HashSet<>();

      setAllCurrentEncounter.addAll(listCurrentStandardWardEncounter);
      setAllCurrentEncounter.addAll(listCurrentIcuEncounter);
      setAllCurrentEncounter.addAll(listVentEncounter);
      setAllCurrentEncounter.addAll(listEcmoEncounter);

      Set<String> setEncounterPatientIds =
          setAllCurrentEncounter.stream().map(UkbEncounter::getPatientId)
              .collect(Collectors.toSet());

      List<UkbPatient> listCurrentPatients = listPatients.stream()
          .filter(patient -> setEncounterPatientIds.contains(patient.getId()))
          .collect(Collectors.toList());
      List<String[]> ukbCrossTabList =
          CoronaResultFunctionality.generateCrosstabList(listCrosstabmaxtreatmentlevels,
              listCurrentPatients);
      if (debug) {
        aggData.put("casenrs", ukbCrossTabList);
      }

      List<String> values = new ArrayList<>();
      for (String[] nr : ukbCrossTabList) {
        values.add(String.valueOf(nr.length));
      }
      aggData.put("value", values);

      cd.setData(aggData);

      currentDataList.add(cd);
    }

    // list with all lengths of icu stays (in h)
    // cumulative.lengthofstay.icu
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_LENGTHOFSTAY_ICU, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_LENGTHOFSTAY_ICU);
      cd.setItemtype(ITEMTYPE_LIST);

      Map<String, Map<Long, Set<String>>> mapIcuLengthList =
          cumulativeLengthOfStayIcu.createIcuLengthOfStayList(mapIcu);
      List<Long> listHours = new ArrayList<>();
      List<String> listCaseId = new ArrayList<>();
      for (Map.Entry<String, Map<Long, Set<String>>> entry : mapIcuLengthList.entrySet()) {
        Map<Long, Set<String>> value = entry.getValue();
        for (Map.Entry<Long, Set<String>> secondEntry : value.entrySet()) {
          listHours.add(secondEntry.getKey());
          listCaseId.addAll(secondEntry.getValue());
        }
      }
      Collections.sort(listHours);
      cd.setData(listHours);
      currentDataList.add(cd);
      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname(addCaseNrsToLabel(CUMULATIVE_LENGTHOFSTAY_ICU));
        cd.setItemtype(ITEMTYPE_DEBUG);

        cd.setData(listCaseId);
        currentDataList.add(cd);
      }

      // list with all lengths of icu stays (in h)
      if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_LENGTHOFSTAY_ICU_LIVE, false)) {
        cd = new CoronaDataItem();
        cd.setItemname(CUMULATIVE_LENGTHOFSTAY_ICU_LIVE);
        cd.setItemtype(ITEMTYPE_LIST);
        Map<String, Map<Long, Set<String>>> mapLengthIcuAlive =
            cumulativeLengthOfStayIcu.createIcuLengthListByVitalstatus(VitalStatus.ALIVE,
                mapIcuLengthList, mapIcu);
        List<Long> listAliveHours = new ArrayList<>();
        List<String> listAliveCaseId = new ArrayList<>();

        for (Map.Entry<String, Map<Long, Set<String>>> entry : mapLengthIcuAlive.entrySet()) {

          if (entry.getValue() != null) {
            for (Map.Entry<Long, Set<String>> secondEntry : entry.getValue().entrySet()) {
              listAliveHours.add(secondEntry.getKey());
              listAliveCaseId.addAll(secondEntry.getValue());
            }
          } else {
            log.warn(
                "Could not find any icu location information of patient " + entry.getKey()
                    + " though he got a ventilation procedure.");
          }
        }

        Collections.sort(listAliveHours);
        cd.setData(listAliveHours);
        currentDataList.add(cd);
        if (debug) {
          cd = new CoronaDataItem();
          cd.setItemname(addCaseNrsToLabel(CUMULATIVE_LENGTHOFSTAY_ICU_LIVE));
          cd.setItemtype(ITEMTYPE_DEBUG);
          cd.setData(listAliveCaseId);
          currentDataList.add(cd);
        }
      }

      // list with all lengths of icu stays (in h)
      if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_LENGTHOFSTAY_ICU_DEAD, false)) {
        cd = new CoronaDataItem();
        cd.setItemname(CUMULATIVE_LENGTHOFSTAY_ICU_DEAD);
        cd.setItemtype(ITEMTYPE_LIST);

        HashMap<String, Map<Long, Set<String>>> mapLengthOfStayDead =
            cumulativeLengthOfStayIcu.createIcuLengthListByVitalstatus(VitalStatus.DEAD,
                mapIcuLengthList, mapIcu);

        List<Long> listDeadHours = new ArrayList<>();
        List<String> listDeadCaseId = new ArrayList<>();

        for (Map.Entry<String, Map<Long, Set<String>>> entry : mapLengthOfStayDead.entrySet()) {
          Map<Long, Set<String>> value = entry.getValue();
          if (value != null) {
            for (Map.Entry<Long, Set<String>> secondEntry : value.entrySet()) {
              listDeadHours.add(secondEntry.getKey());
              listDeadCaseId.addAll(secondEntry.getValue());
            }
          } else {
            log.debug("Unable to find an entry.getValue for entry.getKey: " + entry.getKey());
          }
        }

        Collections.sort(listDeadHours);
        cd.setData(listDeadHours);
        currentDataList.add(cd);
        if (debug) {
          cd = new CoronaDataItem();
          cd.setItemname(addCaseNrsToLabel(CUMULATIVE_LENGTHOFSTAY_ICU_DEAD));
          cd.setItemtype(ITEMTYPE_DEBUG);
          cd.setData(listDeadCaseId);
          currentDataList.add(cd);
        }
      }
    }

    // cumulative length of stays
    if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_LENGTHOFSTAY_HOSPITAL, false)) {
      cd = new CoronaDataItem();
      cd.setItemname(CUMULATIVE_LENGTHOFSTAY_HOSPITAL);
      cd.setItemtype(ITEMTYPE_LIST);
      HashMap<String, Map<Long, List<String>>> mapDays =
          cumulativeLengthOfStayHospital.createMapDaysHospitalList();

      List<Long> listHospitalDays = new ArrayList<>();
      List<String> listHospitalCaseId = new ArrayList<>();

      for (Map.Entry<String, Map<Long, List<String>>> entry : mapDays.entrySet()) {
        Map<Long, List<String>> value = entry.getValue();
        for (Map.Entry<Long, List<String>> secondEntry : value.entrySet()) {
          listHospitalDays.add(secondEntry.getKey());
          listHospitalCaseId.addAll(secondEntry.getValue());
        }
      }

      listHospitalDays.sort(null);

      cd.setData(listHospitalDays);
      currentDataList.add(cd);
      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname(addCaseNrsToLabel(CUMULATIVE_LENGTHOFSTAY_HOSPITAL));
        cd.setItemtype(ITEMTYPE_DEBUG);
        cd.setData(listHospitalCaseId);
        currentDataList.add(cd);
      }

      // cumulative length of stays ALIVE
      if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE, false)) {
        cd = new CoronaDataItem();
        cd.setItemname(CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE);
        cd.setItemtype(ITEMTYPE_LIST);

        HashMap<String, Map<Long, List<String>>> mapDaysAlive =
            cumulativeLengthOfStayHospital.createLengthOfStayHospitalByVitalstatus(
                listEncounters, mapDays, ALIVE.getValue());
        List<Long> listHospitalAliveDays = new ArrayList<>();
        List<String> listHospitalAliveCaseId = new ArrayList<>();

        for (Map.Entry<String, Map<Long, List<String>>> entry : mapDaysAlive.entrySet()) {
          Map<Long, List<String>> value = entry.getValue();
          for (Map.Entry<Long, List<String>> secondEntry : value.entrySet()) {
            listHospitalAliveDays.add(secondEntry.getKey());
            listHospitalAliveCaseId.addAll(secondEntry.getValue());
          }
        }
        Collections.sort(listHospitalAliveDays);
        cd.setData(listHospitalAliveDays);

        currentDataList.add(cd);
        if (debug) {
          cd = new CoronaDataItem();
          cd.setItemname(addCaseNrsToLabel(CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE));
          cd.setItemtype(ITEMTYPE_DEBUG);
          cd.setData(listHospitalAliveCaseId);
          currentDataList.add(cd);
        }
      }

      // cumulative length of stays ALIVE
      if (!mapExcludeDataItems.getOrDefault(CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD, false)) {
        cd = new CoronaDataItem();
        cd.setItemname(CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD);
        cd.setItemtype(ITEMTYPE_LIST);

        HashMap<String, Map<Long, List<String>>> mapDaysDead =
            cumulativeLengthOfStayHospital.createLengthOfStayHospitalByVitalstatus(
                listEncounters, mapDays, DEAD.getValue());
        List<Long> listHospitalDeadDays = new ArrayList<>();
        List<String> listHospitalDeadCaseId = new ArrayList<>();

        for (Map.Entry<String, Map<Long, List<String>>> entry : mapDaysDead.entrySet()) {
          Map<Long, List<String>> value = entry.getValue();
          for (Map.Entry<Long, List<String>> secondEntry : value.entrySet()) {
            listHospitalDeadDays.add(secondEntry.getKey());
            listHospitalDeadCaseId.addAll(secondEntry.getValue());
          }
        }

        Collections.sort(listHospitalDeadDays);
        cd.setData(listHospitalDeadDays);
        currentDataList.add(cd);
        if (debug) {
          cd = new CoronaDataItem();
          cd.setItemname(addCaseNrsToLabel(CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD));
          cd.setItemtype(ITEMTYPE_DEBUG);

          cd.setData(listHospitalDeadCaseId);
          currentDataList.add(cd);
        }
      }
    }
    return currentDataList;
  }

  private String addCaseNrsToLabel(String dataItem) {
    return dataItem + "." + CASENRS;
  }

  private void reportMissingFields(List<UkbEncounter> listEncounters) {
    // Encounter.period.start is mandatory and critical for many data items
    List<String> casesWithoutPeriodStart =
        listEncounters.parallelStream().filter(x -> !x.isPeriodStartExistent())
            .map(UkbEncounter::getCaseId).toList();
    if (casesWithoutPeriodStart.size() > 0) {
      log.debug(
          "Warning: " + casesWithoutPeriodStart.size()
              + " Encounters without period/period.start element have been detected [for example case with id: "
              + casesWithoutPeriodStart.get(
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
   * @param listCumulativeAmbulantEncounter     List that contains all ambulant cases needed for the
   *                                            cumulative logic
   * @param listCumulativeStandardWardEncounter List that contains all stationary cases needed for
   *                                            the cumulative logic
   * @param listCumulativeIcuEncounter          List that contains all ICU cases needed for the
   *                                            cumulative logic
   * @param listCumulativeIcuVentEncounter      List that contains all Ventilation cases needed for
   *                                            the cumulative logic
   * @param listCumulativeIcuEcmoEncounter      List that contains all ECMO cases needed for the
   *                                            cumulative logic
   */
  private void removeDuplicatePids(List<UkbEncounter> listCumulativeAmbulantEncounter,
      List<UkbEncounter> listCumulativeStandardWardEncounter,
      List<UkbEncounter> listCumulativeIcuEncounter,
      List<UkbEncounter> listCumulativeIcuVentEncounter,
      List<UkbEncounter> listCumulativeIcuEcmoEncounter) {
    Set<String> listAmbuPids = createPidList(listCumulativeAmbulantEncounter);
    Set<String> listStandardWardPids = createPidList(listCumulativeStandardWardEncounter);
    Set<String> listIcuPids = createPidList(listCumulativeIcuEncounter);
    Set<String> listVentPids = createPidList(listCumulativeIcuVentEncounter);
    Set<String> listEcmoPids = createPidList(listCumulativeIcuEcmoEncounter);

    listAmbuPids.removeAll(listStandardWardPids);
    listAmbuPids.removeAll(listIcuPids);
    listAmbuPids.removeAll(listVentPids);
    listAmbuPids.removeAll(listEcmoPids);

    listCumulativeAmbulantEncounter.removeIf(e -> !listAmbuPids.contains(e.getPatientId()));

    listStandardWardPids.removeAll(listIcuPids);
    listStandardWardPids.removeAll(listVentPids);
    listStandardWardPids.removeAll(listEcmoPids);

    listCumulativeStandardWardEncounter.removeIf(
        e -> !listStandardWardPids.contains(e.getPatientId()));

    listIcuPids.removeAll(listVentPids);
    listIcuPids.removeAll(listEcmoPids);

    listCumulativeIcuEncounter.removeIf(e -> !listIcuPids.contains(e.getPatientId()));

    listVentPids.removeAll(listEcmoPids);

    listCumulativeIcuVentEncounter.removeIf(e -> !listVentPids.contains(e.getPatientId()));
  }


  private Set<String> createPidList(List<UkbEncounter> listCumulativeEncounter) {
    return listCumulativeEncounter.parallelStream().map(UkbEncounter::getPatientId)
        .collect(Collectors.toSet());
  }


  private void setMapCurrentTreatmentlevelCasenrs(
      HashMap<String, List<String>> mapCurrentTreatmentlevelCaseNrs) {
    this.mapCurrentTreatmentlevelCaseNrs = mapCurrentTreatmentlevelCaseNrs;
  }

  public HashMap<String, List<String>> getMapCurrentTreatmentlevelCasenrs() {
    return mapCurrentTreatmentlevelCaseNrs;
  }

  /**
   * Creation of a map that extends the list of current.treatmentlevel with the case numbers.
   * (Necessary for debugging purposes, but also for internal reports at the UKB).
   *
   * @param listCurrentStandardWardEncounter List of all normal inpatient {@link UkbEncounter}
   *                                         resources that include active C19 cases.
   * @param listCurrentIcuEncounter          List of all inpatient {@link UkbEncounter} resources
   *                                         that include active C19 cases and who are on an icu
   *                                         ward.
   * @param listCurrentVentEncounter         List of all {@link UkbEncounter} resources that include
   *                                         active C19 cases that have an active ventilation period
   *                                         (and no active ecmo)
   * @param listCurrentEcmoEncounter         List of all {@link UkbEncounter} resources that include
   *                                         active C19 cases that have an active ecmo period
   * @return map that connects treatmentlevel with a list of casenrs
   */
  private HashMap<String, List<String>> createMapCurrentTreatmentlevelCasenrs(
      List<UkbEncounter> listCurrentStandardWardEncounter,
      List<UkbEncounter> listCurrentIcuEncounter, List<UkbEncounter> listCurrentVentEncounter,
      List<UkbEncounter> listCurrentEcmoEncounter) {
    List<String> listStationaryCaseNrs = new ArrayList<>();
    List<String> listIcuCaseNrs = new ArrayList<>();
    List<String> listVentCaseNrs = new ArrayList<>();
    List<String> listEcmoCaseNrs = new ArrayList<>();
    HashMap<String, List<String>> resultCurrentTreatmentCaseNrs = new LinkedHashMap<>();

    // creating a list with the subset for all positive stationary (needed in age calculations)
    listCurrentStandardWardEncounter.forEach(encounter -> {
      if (CoronaResultFunctionality.isCaseClassInpatient(encounter) && encounter.hasExtension(
          POSITIVE_RESULT.getValue())) {
        listStationaryCaseNrs.add(encounter.getId());
      }
    });
    listCurrentIcuEncounter.forEach(encounter -> listIcuCaseNrs.add(encounter.getId()));
    listCurrentVentEncounter.forEach(encounter -> listVentCaseNrs.add(encounter.getId()));
    listCurrentEcmoEncounter.forEach(encounter -> listEcmoCaseNrs.add(encounter.getId()));

    resultCurrentTreatmentCaseNrs.put(NORMAL_WARD.getValue(), listStationaryCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU.getValue(), listIcuCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), listVentCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU_ECMO.getValue(), listEcmoCaseNrs);

    return resultCurrentTreatmentCaseNrs;
  }
}
