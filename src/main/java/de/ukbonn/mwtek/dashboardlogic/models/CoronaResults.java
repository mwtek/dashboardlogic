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

package de.ukbonn.mwtek.dashboardlogic.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CoronaCumulativeLogic;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CoronaCurrentLogic;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.CoronaTimelineLogic;
import de.ukbonn.mwtek.dashboardlogic.tools.ListNumberPair;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
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
public class CoronaResults
{

  // initialize FHIR lists
  List<UkbCondition> listConditions = new ArrayList<>();
  List<UkbObservation> listObservations = new ArrayList<>();
  List<UkbPatient> listPatients = new ArrayList<>();
  List<UkbEncounter> listEncounters = new ArrayList<>();
  List<UkbProcedure> listIcuProcedures = new ArrayList<>();
  List<UkbLocation> listLocations = new ArrayList<>();

  // map with all inpatient c19 cases, which is needed for internal reports in the UKB
  HashMap<String, List<String>> mapCurrentTreatmentlevelCaseNrs = new HashMap<>();

  /**
   * Initialization of the CoronaResults object with the required FhirRessource lists.
   * 
   * @param listConditions List of FHIR condition resources for U07.1 and U07.2 diagnoses
   * @param listObservations List of FHIR Observation Resources based on the Corona Lab Codes
   * @param listPatients List of FHIR patient resources based on the Condition and Observation
   *          inputs
   * @param listEncounters List of FHIR encounter resources based on the Condition and Observation
   *          inputs
   * @param listIcuProcedures List of FHIR ICU procedures resources based on the Patient/Encounter
   *          inputs
   * @param listLocations List of all FHIR location resources based on the encounter inputs
   */
  public CoronaResults(List<UkbCondition> listConditions, List<UkbObservation> listObservations,
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
   *          "current.treatmentlevel")
   * @param debug Flag to provide debug information (e.g. casenrs) in the output
   * @return List with all the {@link CoronaDataItem data items} that are defined in the corona
   *         dashboard json specification
   */
  public ArrayList<CoronaDataItem> getDataItems(Map<String, Boolean> mapExcludeDataItems,
      Boolean debug) {
    ArrayList<CoronaDataItem> currentDataList = new ArrayList<CoronaDataItem>();
    if (mapExcludeDataItems == null)
      mapExcludeDataItems = new HashMap<String, Boolean>();

    if (debug == null)
      debug = false;

    // If there are resources with unfilled mandatory attributes, report them immediately (may give
    // partially reduced result sets)
    reportMissingFields(listEncounters);

    HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass =
        CoronaResultFunctionality.createEncounterMap(listEncounters);

    Map<String, List<UkbEncounter>> mapIcu =
        CoronaResultFunctionality.createIcuMap(listEncounters, listLocations, listIcuProcedures);
    /* used for current Logic */
    Map<String, List<UkbEncounter>> mapCurrentIcu = CoronaResultFunctionality
        .createCurrentIcuMap(listEncounters, listLocations, listIcuProcedures);

    // Partial lists of current cases broken down by case status
    List<UkbEncounter> listCurrentStandardWardEncounter = new ArrayList<>();
    List<UkbEncounter> listCurrentIcuEncounter = new ArrayList<>();
    List<UkbEncounter> listCurrentVentEncounter = new ArrayList<>();
    List<UkbEncounter> listCurrentEcmoEncounter = new ArrayList<>();

    // Lists of current MaxTreatments
    List<UkbEncounter> listCurrentMaxStationary = new ArrayList<>();

    List<UkbEncounter> listCurrentMaxIcu = new ArrayList<>();

    List<UkbEncounter> listCurrentMaxIcuVent = new ArrayList<>();

    List<UkbEncounter> listCurrentMaxIcuEcmo = new ArrayList<>();
    List<Long> currentMaxStationaryAgeList = new ArrayList<>();
    List<Long> currentMaxIcuAgeList = new ArrayList<>();
    List<Long> currentMaxIcuVentAgeList = new ArrayList<>();
    List<Long> currentMaxIcuEcmoAgeList = new ArrayList<>();
    HashMap<String, List<UkbEncounter>> mapCurrentEncounter = new LinkedHashMap<>();
    // Inpatients = STATIONARY + ICU + ICU_VENT + ECMO
    // needed later for the splitting cumulative.gender into
    // cumulative.inpatient/outpatient.gender

    // same for cumulative results
    List<UkbEncounter> listCumulativeAmbulantEncounter = CoronaCumulativeLogic.getCumulativeByClass(
        listEncounters, mapIcu, CoronaFixedValues.AMBULANT_ITEM, mapPositiveEncounterByClass);

    List<UkbEncounter> listCumulativeStandardWardEncounter =
        CoronaCumulativeLogic.getCumulativeByClass(listEncounters, mapIcu,
            CoronaFixedValues.STATIONARY_ITEM, mapPositiveEncounterByClass);

    // initialize ICU data
    List<UkbEncounter> listCumulativeIcuEncounter = CoronaCumulativeLogic
        .getCumulativeByIcuLevel(listEncounters, mapIcu, CoronaFixedValues.ICU);

    // initialize ICU_VENT data
    List<UkbEncounter> listCumulativeIcuVentEncounter = CoronaCumulativeLogic
        .getCumulativeByIcuLevel(listEncounters, mapIcu, CoronaFixedValues.ICU_VENTILATION);

    // initialize ECMO data
    List<UkbEncounter> listCumulativeIcuEcmoEncounter = CoronaCumulativeLogic
        .getCumulativeByIcuLevel(listEncounters, mapIcu, CoronaFixedValues.ICU_ECMO);

    removeDuplicatePids(listCumulativeAmbulantEncounter, listCumulativeStandardWardEncounter,
        listCumulativeIcuEncounter, listCumulativeIcuVentEncounter, listCumulativeIcuEcmoEncounter);

    // Inpatients = STATIONARY + ICU + ICU_VENT + ECMO
    // needed later for the splitting cumulative.gender into
    // cumulative.inpatient/outpatient.gender
    List<UkbEncounter> listCumulativeInpatients = new ArrayList<>();
    listCumulativeInpatients.addAll(listCumulativeStandardWardEncounter);
    listCumulativeInpatients.addAll(listCumulativeIcuEncounter);
    listCumulativeInpatients.addAll(listCumulativeIcuVentEncounter);
    listCumulativeInpatients.addAll(listCumulativeIcuEcmoEncounter);

    // list with all positive encounters that are currently in the hospital
    List<UkbEncounter> listCurrentEncounter = new ArrayList<>();

    CoronaDataItem cd;
    // current treatmentlevel
    if (!mapExcludeDataItems.containsKey("current.treatmentlevel")) {
      cd = new CoronaDataItem();

      cd.setItemname("current.treatmentlevel");
      cd.setItemtype("aggregated");

      HashMap<String, Number> listCurrent = new LinkedHashMap<String, Number>();

      listCurrentStandardWardEncounter = CoronaCurrentLogic
          .getCurrentStandardWardEncounter(listEncounters, mapCurrentIcu, listIcuProcedures);
      listCurrentIcuEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
          listEncounters, listIcuProcedures, CoronaFixedValues.ICU);
      listCurrentVentEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
          listEncounters, listIcuProcedures, CoronaFixedValues.ICU_VENTILATION);
      listCurrentEcmoEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
          listEncounters, listIcuProcedures, CoronaFixedValues.ICU_ECMO);

      // add the results to the total list
      listCurrentEncounter.addAll(listCurrentStandardWardEncounter);
      listCurrentEncounter.addAll(listCurrentIcuEncounter);
      listCurrentEncounter.addAll(listCurrentVentEncounter);
      listCurrentEncounter.addAll(listCurrentEcmoEncounter);

      mapCurrentEncounter.put(CoronaFixedValues.NORMALSTATION.getValue(),
          listCurrentStandardWardEncounter);
      mapCurrentEncounter.put(CoronaFixedValues.ICU.getValue(), listCurrentIcuEncounter);
      mapCurrentEncounter.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
          listCurrentVentEncounter);
      mapCurrentEncounter.put(CoronaFixedValues.ICU_ECMO.getValue(), listCurrentEcmoEncounter);

      listCurrent.put(TreatmentLevel.NORMALSTATION.getText(),
          listCurrentStandardWardEncounter.size());
      listCurrent.put(TreatmentLevel.ICU.getText(), listCurrentIcuEncounter.size());
      listCurrent.put(TreatmentLevel.ICUWITHVENTILATION.getText(), listCurrentVentEncounter.size());
      listCurrent.put(TreatmentLevel.ICUWITHECMO.getText(), listCurrentEcmoEncounter.size());

      cd.setData(listCurrent);
      currentDataList.add(cd);

      // storing the casenrs of the current treatmentlevel
      HashMap<String, List<String>> mapCurrentTreatmentlevelCaseNrs =
          createMapCurrentTreatmentlevelCasenrs(listCurrentStandardWardEncounter,
              listCurrentIcuEncounter, listCurrentVentEncounter, listCurrentEcmoEncounter);
      this.setMapCurrentTreatmentlevelCasenrs(mapCurrentTreatmentlevelCaseNrs);

      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname("current.treatmentlevel.casenrs");
        cd.setItemtype("debug");

        cd.setData(mapCurrentTreatmentlevelCaseNrs);
        currentDataList.add(cd);

      }
    }

    // current.treatmentlevel.newadmission.direct
    if (!mapExcludeDataItems.containsKey("current.treatmentlevel.newadmission.direct")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.treatmentlevel.newadmission.direct");
      cd.setItemtype("aggregated");

      if (mapExcludeDataItems.containsKey("current.treatmentlevel")) {
        listCurrentStandardWardEncounter = CoronaCurrentLogic
            .getCurrentStandardWardEncounter(listEncounters, mapCurrentIcu, listIcuProcedures);
        listCurrentIcuEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU);
        listCurrentVentEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU_VENTILATION);
        listCurrentEcmoEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU_ECMO);

        mapCurrentEncounter.put(CoronaFixedValues.NORMALSTATION.getValue(),
            listCurrentStandardWardEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU.getValue(), listCurrentIcuEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
            listCurrentVentEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU_ECMO.getValue(), listCurrentEcmoEncounter);
      }
      Map<String, Long> mapCurrentAdmissionDirect =
          CoronaCurrentLogic.createAdmissionMap(mapCurrentEncounter, mapIcu, "A");
      cd.setData(mapCurrentAdmissionDirect);
      currentDataList.add(cd);
    }

    // current treatmentlevel newadmission transfer
    if (!mapExcludeDataItems.containsKey("current.treatmentlevel.newadmission.transfer")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.treatmentlevel.newadmission.transfer");
      cd.setItemtype("aggregated");

      if (mapExcludeDataItems.containsKey("current.treatmentlevel")) {
        listCurrentStandardWardEncounter = CoronaCurrentLogic
            .getCurrentStandardWardEncounter(listEncounters, mapCurrentIcu, listIcuProcedures);
        listCurrentIcuEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU);
        listCurrentVentEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU_VENTILATION);
        listCurrentEcmoEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU_ECMO);

        mapCurrentEncounter.put(CoronaFixedValues.NORMALSTATION.getValue(),
            listCurrentStandardWardEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU.getValue(), listCurrentIcuEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
            listCurrentVentEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU_ECMO.getValue(), listCurrentEcmoEncounter);
      }

      Map<String, Long> mapCurrentAdmissionTransfer =
          CoronaCurrentLogic.createAdmissionMap(mapCurrentEncounter, mapIcu, "V");
      cd.setData(mapCurrentAdmissionTransfer);
      currentDataList.add(cd);
    }

    // current treatmentlevel newadmission complement
    if (!mapExcludeDataItems.containsKey("current.treatmentlevel.newadmission.complement")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.treatmentlevel.newadmission.complement");
      cd.setItemtype("aggregated");

      if (mapExcludeDataItems.containsKey("current.treatmentlevel")) {
        listCurrentStandardWardEncounter = CoronaCurrentLogic
            .getCurrentStandardWardEncounter(listEncounters, mapCurrentIcu, listIcuProcedures);
        listCurrentIcuEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU);
        listCurrentVentEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU_VENTILATION);
        listCurrentEcmoEncounter = CoronaCurrentLogic.getCurrentEncounterByIcuLevel(mapCurrentIcu,
            listEncounters, listIcuProcedures, CoronaFixedValues.ICU_ECMO);

        mapCurrentEncounter.put(CoronaFixedValues.NORMALSTATION.getValue(),
            listCurrentStandardWardEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU.getValue(), listCurrentIcuEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
            listCurrentVentEncounter);
        mapCurrentEncounter.put(CoronaFixedValues.ICU_ECMO.getValue(), listCurrentEcmoEncounter);
      }

      Map<String, Long> mapCurrentAdmissionComplement =
          CoronaCurrentLogic.createAdmissionComplementMap(mapCurrentEncounter, mapIcu);
      cd.setData(mapCurrentAdmissionComplement);
      currentDataList.add(cd);
    }
    // current maxtreatmentlevel
    if (!mapExcludeDataItems.containsKey("current.maxtreatmentlevel")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.maxtreatmentlevel");
      cd.setItemtype("aggregated");
      Map<String, Number> mapCurrentMax = new LinkedHashMap<String, Number>();

      listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
          CoronaFixedValues.STATIONARY_ITEM, listEncounters);

      listCurrentMaxIcu = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
          CoronaFixedValues.ICU, listEncounters);

      listCurrentMaxIcuVent = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
          CoronaFixedValues.ICU_VENTILATION, listEncounters);

      listCurrentMaxIcuEcmo = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
          CoronaFixedValues.ICU_ECMO, listEncounters);

      mapCurrentMax.put(TreatmentLevel.NORMALSTATION.getText(), listCurrentMaxStationary.size());
      mapCurrentMax.put(TreatmentLevel.ICU.getText(), listCurrentMaxIcu.size());
      mapCurrentMax.put(TreatmentLevel.ICUWITHVENTILATION.getText(), listCurrentMaxIcuVent.size());
      mapCurrentMax.put(TreatmentLevel.ICUWITHECMO.getText(), listCurrentMaxIcuEcmo.size());
      cd.setData(mapCurrentMax);
      currentDataList.add(cd);

      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname("current.maxtreatmentlevel.casenrs");
        cd.setItemtype("debug");
        List<String> listStationaryCaseNrs = new ArrayList<String>();
        List<String> listIcuCaseNrs = new ArrayList<String>();
        List<String> listVentCaseNrs = new ArrayList<String>();
        List<String> listEcmoCaseNrs = new ArrayList<String>();
        HashMap<String, List<String>> resultCurrentTreatmentCaseNrs =
            new LinkedHashMap<String, List<String>>();

        listCurrentMaxStationary.forEach(encounter -> listStationaryCaseNrs.add(encounter.getId()));
        listCurrentMaxIcu.forEach(encounter -> listIcuCaseNrs.add(encounter.getId()));
        listCurrentMaxIcuVent.forEach(encounter -> listVentCaseNrs.add(encounter.getId()));
        listCurrentMaxIcuEcmo.forEach(encounter -> listEcmoCaseNrs.add(encounter.getId()));

        resultCurrentTreatmentCaseNrs.put(TreatmentLevel.NORMALSTATION.getText(),
            listStationaryCaseNrs);
        resultCurrentTreatmentCaseNrs.put(TreatmentLevel.ICU.getText(), listIcuCaseNrs);
        resultCurrentTreatmentCaseNrs.put(TreatmentLevel.ICUWITHVENTILATION.getText(),
            listVentCaseNrs);
        resultCurrentTreatmentCaseNrs.put(TreatmentLevel.ICUWITHECMO.getText(), listEcmoCaseNrs);

        cd.setData(resultCurrentTreatmentCaseNrs);
        currentDataList.add(cd);
      }
    }

    // current.maxtreatment.age.normalstation
    if (!mapExcludeDataItems.containsKey("current.maxtreatment.age.normalstation")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.maxtreatment.age.normalstation");
      cd.setItemtype("list");

      if (mapExcludeDataItems.containsKey("current.maxtreatmentlevel")) {
        listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
            CoronaFixedValues.STATIONARY_ITEM, listEncounters);
      }
      currentMaxStationaryAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
          mapPositiveEncounterByClass, listCurrentMaxStationary);

      cd.setData(currentMaxStationaryAgeList);
      currentDataList.add(cd);
    }
    // current.maxtreatment.age.icu
    if (!mapExcludeDataItems.containsKey("current.maxtreatment.age.icu")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.maxtreatment.age.icu");
      cd.setItemtype("list");

      if (mapExcludeDataItems.containsKey("current.maxtreatmentlevel")) {
        listCurrentMaxIcu = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
            CoronaFixedValues.ICU, listEncounters);
      }
      currentMaxIcuAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
          mapPositiveEncounterByClass, listCurrentMaxIcu);

      cd.setData(currentMaxIcuAgeList);
      currentDataList.add(cd);
    }
    // current.maxtreatment.age.icu_mit_beatmung
    if (!mapExcludeDataItems.containsKey("current.maxtreatment.age.icu_mit_beatmung")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.maxtreatment.age.icu_mit_beatmung");
      cd.setItemtype("list");

      if (mapExcludeDataItems.containsKey("current.maxtreatmentlevel")) {
        listCurrentMaxIcuVent = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
            CoronaFixedValues.ICU_VENTILATION, listEncounters);
      }
      currentMaxIcuVentAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
          mapPositiveEncounterByClass, listCurrentMaxIcuVent);

      cd.setData(currentMaxIcuVentAgeList);
      currentDataList.add(cd);
    }
    // current.maxtreatment.age.icu_mit_ecmo
    if (!mapExcludeDataItems.containsKey("current.maxtreatment.age.icu_mit_ecmo")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.maxtreatment.age.icu_mit_ecmo");
      cd.setItemtype("list");

      if (mapExcludeDataItems.containsKey("current.maxtreatmentlevel")) {
        listCurrentMaxIcuEcmo = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
            CoronaFixedValues.ICU_ECMO, listEncounters);
      }
      currentMaxIcuEcmoAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
          mapPositiveEncounterByClass, listCurrentMaxIcuEcmo);

      cd.setData(currentMaxIcuEcmoAgeList);
      currentDataList.add(cd);
    }

    if (!mapExcludeDataItems.containsKey("current.maxtreatment.age.inpatient")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.maxtreatment.age.inpatient");
      cd.setItemtype("list");
      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.normalstation")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.STATIONARY_ITEM, listEncounters);
        }
        currentMaxStationaryAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.normalstation")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.STATIONARY_ITEM, listEncounters);
        }
        currentMaxStationaryAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.icu")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxIcu = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.ICU, listEncounters);
        }
        currentMaxIcuAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.icu_mit_beatmung")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.ICU_VENTILATION, listEncounters);
        }
        currentMaxIcuVentAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.icu_mit_ecmo")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.ICU_ECMO, listEncounters);
        }
        currentMaxIcuEcmoAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      List<Long> currentInpatientAgeList =
          CoronaCurrentLogic.createCurrentMaxTotalAgeMap(currentMaxStationaryAgeList,
              currentMaxIcuAgeList, currentMaxIcuVentAgeList, currentMaxIcuEcmoAgeList);

      cd.setData(currentInpatientAgeList);
      currentDataList.add(cd);
    }

    if (!mapExcludeDataItems.containsKey("current.maxtreatment.age.total_icu")) {
      cd = new CoronaDataItem();
      cd.setItemname("current.maxtreatment.age.total_icu");
      cd.setItemtype("list");

      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.icu")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxIcu = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.ICU, listEncounters);
        }
        currentMaxIcuAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.icu_mit_beatmung")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.ICU_VENTILATION, listEncounters);
        }
        currentMaxIcuVentAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      if (mapExcludeDataItems.containsKey("current.maxtreatment.age.icu_mit_ecmo")) {
        if (mapExcludeDataItems.containsKey("current.maxtreatment")) {
          listCurrentMaxStationary = CoronaCurrentLogic.getNumberOfCurrentMaxTreatmentLevel(mapIcu,
              CoronaFixedValues.ICU_ECMO, listEncounters);
        }
        currentMaxIcuEcmoAgeList = CoronaCurrentLogic.createCurrentMaxAgeMap(listPatients,
            mapPositiveEncounterByClass, listCurrentMaxStationary);
      }

      List<Long> currentTotalIcuAgeList =
          CoronaCurrentLogic.createCurrentMaxTotalAgeMap(currentMaxStationaryAgeList,
              currentMaxIcuAgeList, currentMaxIcuVentAgeList, currentMaxIcuEcmoAgeList);

      cd.setData(currentTotalIcuAgeList);
      currentDataList.add(cd);
    }
    // cumulative results
    if (!mapExcludeDataItems.containsKey("cumulative.results")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.results");
      cd.setItemtype("aggregated");

      Set<UkbObservation> setPositiveObservations = CoronaResultFunctionality
          .getObservationsByResult(listObservations, CoronaFixedValues.POSITIVE);
      Set<UkbObservation> setNegativeObservations = CoronaResultFunctionality
          .getObservationsByResult(listObservations, CoronaFixedValues.NEGATIVE);
      Set<UkbObservation> setBorderlineLabCaseNrs = CoronaResultFunctionality
          .getObservationsByResult(listObservations, CoronaFixedValues.BORDERLINE);
      Map<String, Number> cumulativeResultMap = new LinkedHashMap<String, Number>();

      cumulativeResultMap.put(CoronaFixedValues.POSITIVE.getValue(), setPositiveObservations.size());
      cumulativeResultMap.put(CoronaFixedValues.BORDERLINE.getValue(), setBorderlineLabCaseNrs.size());
      cumulativeResultMap.put(CoronaFixedValues.NEGATIVE.getValue(), setNegativeObservations.size());

      cd.setData(cumulativeResultMap);
      currentDataList.add(cd);

      // cumulative gender
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.gender");
      cd.setItemtype("aggregated");

      Map<String, Number> cumulativeGenderMap = new LinkedHashMap<String, Number>();

      cumulativeGenderMap.put(CoronaFixedValues.MALE_SPECIFICATION.getValue(), CoronaCumulativeLogic
          .getGenderCount(listEncounters, listPatients, CoronaFixedValues.MALE.getValue()));
      cumulativeGenderMap.put(CoronaFixedValues.FEMALE_SPECIFICATION.getValue(), CoronaCumulativeLogic
          .getGenderCount(listEncounters, listPatients, CoronaFixedValues.FEMALE.getValue()));
      cumulativeGenderMap.put(CoronaFixedValues.DIVERSE_SPECIFICATION.getValue(), CoronaCumulativeLogic
          .getGenderCount(listEncounters, listPatients, CoronaFixedValues.DIVERSE.getValue()));

      cd.setData(cumulativeGenderMap);
      currentDataList.add(cd);
    }

    // cumulative age
    if (!mapExcludeDataItems.containsKey("cumulative.age")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.age");
      cd.setItemtype("list");

      cd.setData(CoronaCumulativeLogic
          .getAgeDistributionsByCaseClass(CoronaFixedValues.ALL.getValue(), listEncounters,
              listPatients)
          .toArray());
      currentDataList.add(cd);

      // cumulative age alive
      if (!mapExcludeDataItems.containsKey("cumulative.age.alive")) {
        cd = new CoronaDataItem();
        cd.setItemname("cumulative.age.alive");
        cd.setItemtype("list");

        cd.setData(CoronaCumulativeLogic
            .getAgeCountByVitalStatus(VitalStatus.ALIVE, listEncounters, listPatients,
                mapPositiveEncounterByClass)
            .toArray());
        currentDataList.add(cd);
      }

      // cumulative age dead
      if (!mapExcludeDataItems.containsKey("cumulative.age.dead")) {
        cd = new CoronaDataItem();
        cd.setItemname("cumulative.age.dead");
        cd.setItemtype("list");

        cd.setData(CoronaCumulativeLogic
            .getAgeCountByVitalStatus(VitalStatus.DEAD, listEncounters, listPatients,
                mapPositiveEncounterByClass)
            .toArray());
        currentDataList.add(cd);
      }
    }

    // cumulative maxtreatmentlevel
    if (!mapExcludeDataItems.containsKey("cumulative.maxtreatmentlevel")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.maxtreatmentlevel");
      cd.setItemtype("aggregated");
      LinkedHashMap<String, Number> mapCumulativeMaxtTreatment =
          new LinkedHashMap<String, Number>();

      mapCumulativeMaxtTreatment.put(CoronaFixedValues.AMBULANT_ITEM.getValue(),
          listCumulativeAmbulantEncounter.size());
      mapCumulativeMaxtTreatment.put(CoronaFixedValues.NORMALSTATION.getValue(),
          listCumulativeStandardWardEncounter.size());
      mapCumulativeMaxtTreatment.put(CoronaFixedValues.ICU.getValue(),
          listCumulativeIcuEncounter.size());
      mapCumulativeMaxtTreatment.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
          listCumulativeIcuVentEncounter.size());
      mapCumulativeMaxtTreatment.put(CoronaFixedValues.ICU_ECMO.getValue(),
          listCumulativeIcuEcmoEncounter.size());

      cd.setData(mapCumulativeMaxtTreatment);
      currentDataList.add(cd);

      // add casenrs
      if (debug) {
        cd = new CoronaDataItem();
        cd.setItemname("cumulative.maxtreatmentlevel.casenrs");
        cd.setItemtype("debug");
        HashMap<String, Map<String, List<String>>> resultMaxTreatmentCaseNrs =
            new LinkedHashMap<String, Map<String, List<String>>>();

        resultMaxTreatmentCaseNrs.put(CoronaFixedValues.AMBULANT_ITEM.getValue(),
            new HashMap<String, List<String>>());
        resultMaxTreatmentCaseNrs.put(CoronaFixedValues.NORMALSTATION.getValue(),
            new HashMap<String, List<String>>());
        resultMaxTreatmentCaseNrs.put(CoronaFixedValues.ICU.getValue(),
            new HashMap<String, List<String>>());
        resultMaxTreatmentCaseNrs.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
            new HashMap<String, List<String>>());
        resultMaxTreatmentCaseNrs.put(CoronaFixedValues.ICU_ECMO.getValue(),
            new HashMap<String, List<String>>());

        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeAmbulantEncounter,
            CoronaFixedValues.AMBULANT_ITEM.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeStandardWardEncounter,
            CoronaFixedValues.NORMALSTATION.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeIcuEncounter,
            CoronaFixedValues.ICU.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeIcuVentEncounter,
            CoronaFixedValues.ICU_VENTILATION.getValue(), resultMaxTreatmentCaseNrs);
        CoronaResultFunctionality.createCumulativeMaxDebug(listCumulativeIcuEcmoEncounter,
            CoronaFixedValues.ICU_ECMO.getValue(), resultMaxTreatmentCaseNrs);

        cd.setData(resultMaxTreatmentCaseNrs);
        currentDataList.add(cd);
      }
    }
    // cumulative.maxtreatment.age.normalstation
    if (!mapExcludeDataItems.containsKey("cumulative.maxtreatment.age.normalstation")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.maxtreatment.age.normalstation");
      cd.setItemtype("list");

      List<Long> cumulativeMaxtreatmentStationaryAgeList =
          CoronaCumulativeLogic.createMaxtreatmentAgeMap(listPatients, mapPositiveEncounterByClass,
              mapIcu, CoronaFixedValues.NORMALSTATION.getValue());

      cd.setData(cumulativeMaxtreatmentStationaryAgeList);
      currentDataList.add(cd);
    }
    // cumulative.maxtreatment.age.icu
    if (!mapExcludeDataItems.containsKey("cumulative.maxtreatment.age.icu")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.maxtreatment.age.icu");
      cd.setItemtype("list");

      List<Long> cumulativeMaxtreatmentIcuAgeList = CoronaCumulativeLogic.createMaxtreatmentAgeMap(
          listPatients, mapPositiveEncounterByClass, mapIcu, CoronaFixedValues.ICU.getValue());

      cd.setData(cumulativeMaxtreatmentIcuAgeList);
      currentDataList.add(cd);
    }
    // cumulative.maxtreatment.age.icu_mit_beatmung
    if (!mapExcludeDataItems.containsKey("cumulative.maxtreatment.age.icu_mit_beatmung")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.maxtreatment.age.icu_mit_beatmung");
      cd.setItemtype("list");

      List<Long> cumulativeMaxtreatmentIcuVentAgeList =
          CoronaCumulativeLogic.createMaxtreatmentAgeMap(listPatients, mapPositiveEncounterByClass,
              mapIcu, CoronaFixedValues.ICU_VENTILATION.getValue());

      cd.setData(cumulativeMaxtreatmentIcuVentAgeList);
      currentDataList.add(cd);
    }
    // cumulative.maxtreatment.age.icu_mit_ecmo
    if (!mapExcludeDataItems.containsKey("cumulative.maxtreatment.age.icu_mit_ecmo")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.maxtreatment.age.icu_mit_ecmo");
      cd.setItemtype("list");

      List<Long> cumulativeMaxtreatmentIcuEcmoAgeList =
          CoronaCumulativeLogic.createMaxtreatmentAgeMap(listPatients, mapPositiveEncounterByClass,
              mapIcu, CoronaFixedValues.ICU_ECMO.getValue());

      cd.setData(cumulativeMaxtreatmentIcuEcmoAgeList);
      currentDataList.add(cd);
    }
    // cumulative plz
    if (!mapExcludeDataItems.containsKey("cumulative.zipcode")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.zipcode");
      cd.setItemtype("list");

      List<String> listZipCodes =
          CoronaCumulativeLogic.createZipCodeList(listEncounters, listPatients);

      cd.setData(listZipCodes);
      currentDataList.add(cd);
    }

    // timeline tests
    if (!mapExcludeDataItems.containsKey("timeline.tests")) {
      cd = new CoronaDataItem();
      cd.setItemname("timeline.tests");
      cd.setItemtype("list");
      ListNumberPair testsMap = CoronaTimelineLogic.createTimelineTestsMap(listObservations);

      cd.setData(testsMap);
      currentDataList.add(cd);
    }

    // timeline tests positive
    if (!mapExcludeDataItems.containsKey("timeline.test.positive")) {
      cd = new CoronaDataItem();
      cd.setItemname("timeline.test.positive");
      cd.setItemtype("list");

      ListNumberPair testPositivePair = CoronaTimelineLogic.createTimelineTestPositiveMap(listObservations);


      cd.setData(testPositivePair);
      currentDataList.add(cd);
    }

    // timeline maxtreatmentlevel
    if (!mapExcludeDataItems.containsKey("timeline.maxtreatmentlevel")) {
      cd = new CoronaDataItem();
      cd.setItemname("timeline.maxtreatmentlevel");
      cd.setItemtype("list");
      Map<String, Map<Long, Set<Long>>> resultMaxTreatmentTimeline = new LinkedHashMap<>();
      Map<String, List<Long>> mapResultTreatment = new LinkedHashMap<>();
      List<Long> listDate = new ArrayList<>();

      resultMaxTreatmentTimeline = CoronaTimelineLogic.createMaxTreatmentTimeline(listEncounters,
          listLocations, listIcuProcedures);

      listDate.addAll(resultMaxTreatmentTimeline.get("date")
          .keySet());
      mapResultTreatment.put("date", listDate);
      for (Map.Entry<String, Map<Long, Set<Long>>> entry : resultMaxTreatmentTimeline.entrySet()) {
        for (Map.Entry<Long, Set<Long>> secondEntry : entry.getValue()
            .entrySet()) {
          if (entry.getKey()
              .equals(CoronaFixedValues.AMBULANT_ITEM.getValue())) {
            addValuesToTimelineMaxMap(CoronaFixedValues.AMBULANT_ITEM.getValue(),
                secondEntry.getValue(), mapResultTreatment);
          } else if (entry.getKey()
              .equals(CoronaFixedValues.NORMALSTATION.getValue())) {
            addValuesToTimelineMaxMap(CoronaFixedValues.NORMALSTATION.getValue(),
                secondEntry.getValue(), mapResultTreatment);
          } else if (entry.getKey()
              .equals(CoronaFixedValues.ICU.getValue())) {
            addValuesToTimelineMaxMap(CoronaFixedValues.ICU.getValue(), secondEntry.getValue(),
                mapResultTreatment);
          } else if (entry.getKey()
              .equals(CoronaFixedValues.ICU_VENTILATION.getValue())) {
            addValuesToTimelineMaxMap(CoronaFixedValues.ICU_VENTILATION.getValue(),
                secondEntry.getValue(), mapResultTreatment);
          } else if (entry.getKey()
              .equals(CoronaFixedValues.ICU_ECMO.getValue())) {
            addValuesToTimelineMaxMap(CoronaFixedValues.ICU_ECMO.getValue(), secondEntry.getValue(),
                mapResultTreatment);
          } else if (entry.getKey()
              .equals(CoronaFixedValues.INPATIENT.getValue())) {
            addValuesToTimelineMaxMap(CoronaFixedValues.INPATIENT.getValue(),
                secondEntry.getValue(), mapResultTreatment);
          }
        }
      }


      cd.setData(mapResultTreatment);
      currentDataList.add(cd);

      // show casenrs for mtl plausibility checks
      // timeline maxtreatmentlevel
      if (debug == true) {
        cd = new CoronaDataItem();
        cd.setItemname("timeline.maxtreatmentlevel.casenrs");
        cd.setItemtype("debug");
        Map<String, Map<Long, Set<Long>>> resultMaxTreatmentCaseNrs =
            new LinkedHashMap<String, Map<Long, Set<Long>>>();

        Set<Long> setDate = new LinkedHashSet<>();
        setDate.addAll(listDate);

        for (Map.Entry<String, Map<Long, Set<Long>>> entry : resultMaxTreatmentTimeline
            .entrySet()) {
          String mapNameAndValues = entry.getKey();
          Map<Long, Set<Long>> mapDateAndCaseNr = entry.getValue();
          if (mapNameAndValues.equals(CoronaFixedValues.AMBULANT_ITEM.getValue())) {
            resultMaxTreatmentCaseNrs.put(TreatmentLevel.AMBULANT.getText(), mapDateAndCaseNr);
          }

          else if (mapNameAndValues.equals(CoronaFixedValues.NORMALSTATION.getValue())) {
            resultMaxTreatmentCaseNrs.put(TreatmentLevel.NORMALSTATION.getText(), mapDateAndCaseNr);
          }

          else if (mapNameAndValues.equals(CoronaFixedValues.ICU.getValue())) {
            resultMaxTreatmentCaseNrs.put(TreatmentLevel.ICU.getText(), mapDateAndCaseNr);
          }

          else if (mapNameAndValues.equals(CoronaFixedValues.ICU_VENTILATION.getValue())) {
            resultMaxTreatmentCaseNrs.put(TreatmentLevel.ICUWITHVENTILATION.getText(),
                mapDateAndCaseNr);
          }

          else if (mapNameAndValues.equals(CoronaFixedValues.ICU_ECMO.getValue())) {
            resultMaxTreatmentCaseNrs.put(TreatmentLevel.ICUWITHECMO.getText(), mapDateAndCaseNr);
          }
        }
        Map<Long, Set<Long>> mapTempAndDate = new LinkedHashMap<>();
        mapTempAndDate.put(0l, setDate);
        resultMaxTreatmentCaseNrs.put("date", mapTempAndDate);

        cd.setData(resultMaxTreatmentCaseNrs);
        currentDataList.add(cd);
      }
    }

    // cumulative inpatient gender
    if (!mapExcludeDataItems.containsKey("cumulative.inpatient.gender")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.inpatient.gender");
      cd.setItemtype("aggregated");

      HashMap<String, Number>cumulativeInpatientGenderMap = new HashMap<String, Number>();

      // listCumulativeInpatients gets initialized in the cumulative.maxtreatmentlevel method ->
      // this method could be excluded

      cumulativeInpatientGenderMap.put(CoronaFixedValues.MALE_SPECIFICATION.getValue(),
          CoronaCumulativeLogic.getGenderCountByCaseClass(listPatients, listCumulativeInpatients,
              CoronaFixedValues.MALE.getValue(), CoronaFixedValues.CASECLASS_INPATIENT.getValue()));
      cumulativeInpatientGenderMap.put(CoronaFixedValues.FEMALE_SPECIFICATION.getValue(),
          CoronaCumulativeLogic.getGenderCountByCaseClass(listPatients, listCumulativeInpatients,
              CoronaFixedValues.FEMALE.getValue(),
              CoronaFixedValues.CASECLASS_INPATIENT.getValue()));
      cumulativeInpatientGenderMap.put(CoronaFixedValues.DIVERSE_SPECIFICATION.getValue(),
          CoronaCumulativeLogic.getGenderCountByCaseClass(listPatients, listCumulativeInpatients,
              CoronaFixedValues.DIVERSE.getValue(),
              CoronaFixedValues.CASECLASS_INPATIENT.getValue()));

      cd.setData(cumulativeInpatientGenderMap);
      currentDataList.add(cd);
    }

    // cumulative outpatient gender
    if (!mapExcludeDataItems.containsKey("cumulative.outpatient.gender")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.outpatient.gender");
      cd.setItemtype("aggregated");

      HashMap<String, Number> cumulativeOutpatientGender = new HashMap<String, Number>();

      cumulativeOutpatientGender.put(CoronaFixedValues.MALE_SPECIFICATION.getValue(),
          CoronaCumulativeLogic.getGenderCountByCaseClass(listPatients,
              listCumulativeAmbulantEncounter, CoronaFixedValues.MALE.getValue(),
              CoronaFixedValues.CASECLASS_OUTPATIENT.getValue()));
      cumulativeOutpatientGender.put(CoronaFixedValues.FEMALE_SPECIFICATION.getValue(),
          CoronaCumulativeLogic.getGenderCountByCaseClass(listPatients,
              listCumulativeAmbulantEncounter, CoronaFixedValues.FEMALE.getValue(),
              CoronaFixedValues.CASECLASS_OUTPATIENT.getValue()));
      cumulativeOutpatientGender.put(CoronaFixedValues.DIVERSE_SPECIFICATION.getValue(),
          CoronaCumulativeLogic.getGenderCountByCaseClass(listPatients,
              listCumulativeAmbulantEncounter, CoronaFixedValues.DIVERSE.getValue(),
              CoronaFixedValues.CASECLASS_OUTPATIENT.getValue()));

      cd.setData(cumulativeOutpatientGender);
      currentDataList.add(cd);
    }

    // cumulative inpatient age
    if (!mapExcludeDataItems.containsKey("cumulative.inpatient.age")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.inpatient.age");
      cd.setItemtype("list");


      cd.setData(CoronaCumulativeLogic
          .getAgeDistributionsByCaseClass(CoronaFixedValues.INPATIENT.getValue(), listEncounters,
              listPatients)
          .toArray());
      currentDataList.add(cd);
    }

    // cumulative outpatient age
    if (!mapExcludeDataItems.containsKey("cumulative.outpatient.age")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.outpatient.age");
      cd.setItemtype("list");

      cd.setData(CoronaCumulativeLogic
          .getAgeDistributionsByCaseClass(CoronaFixedValues.OUTPATIENT.getValue(), listEncounters,
              listPatients)
          .toArray());
      currentDataList.add(cd);
    }

    // timeline deaths
    if (!mapExcludeDataItems.containsKey("timeline.deaths")) {
      cd = new CoronaDataItem();
      cd.setItemname("timeline.deaths");
      cd.setItemtype("list");

      ListNumberPair tlDeathPairList = CoronaTimelineLogic.createTimeLineDeathMap(listEncounters);

      cd.setData(tlDeathPairList);

      currentDataList.add(cd);
    }

    // Bonn cross tabulation
    if (!mapExcludeDataItems.containsKey("ukb.crosstab")) {
      cd = new CoronaDataItem();
      cd.setItemname("ukb.crosstab");
      cd.setItemtype("aggregated");

      Map<String, List> aggData = new HashMap<String, List>();
      aggData.put("columnname", Arrays.asList("Bonn", "ICU", "Beatmet", "Ecmo"));
      aggData.put("state",
          Arrays.asList("0000", "0100", "0110", "0111", "1000", "1100", "1110", "1111"));

      aggData.put("value", new ArrayList<>());
      LinkedHashMap<CoronaFixedValues, List<UkbEncounter>> listCrosstabMaxTreatments =
          new LinkedHashMap<>();
      listCrosstabMaxTreatments.put(CoronaFixedValues.STATIONARY_ITEM,
          listCurrentStandardWardEncounter);
      listCrosstabMaxTreatments.put(CoronaFixedValues.ICU, listCurrentIcuEncounter);

      List<UkbEncounter> listVentEncounter = new ArrayList<>();
      listVentEncounter.addAll(listCurrentVentEncounter);
      listCrosstabMaxTreatments.put(CoronaFixedValues.ICU_VENTILATION, listVentEncounter);

      List<UkbEncounter> listEcmoEncounter = new ArrayList<>();
      listEcmoEncounter.addAll(listCurrentEcmoEncounter);
      listCrosstabMaxTreatments.put(CoronaFixedValues.ICU_ECMO, listEcmoEncounter);

      Set<String> setEncounterPatientIds = new HashSet<>();
      Set<UkbEncounter> setAllCurrentEncounter = new HashSet<>();

      setAllCurrentEncounter.addAll(listCurrentStandardWardEncounter);
      setAllCurrentEncounter.addAll(listCurrentIcuEncounter);
      setAllCurrentEncounter.addAll(listVentEncounter);
      setAllCurrentEncounter.addAll(listEcmoEncounter);

      setEncounterPatientIds.addAll(setAllCurrentEncounter.stream()
          .map(e -> e.getPatientId())
          .collect(Collectors.toList()));

      List<UkbPatient> listCurrentPatients = listPatients.stream()
          .filter(patient -> setEncounterPatientIds.contains(patient.getId()))
          .collect(Collectors.toList());
      List<String[]> ukbCrossTabList = CoronaResultFunctionality
          .generateCrosstabList(listCrosstabMaxTreatments, listCurrentPatients);
      if (debug)
        aggData.put("casenrs", ukbCrossTabList);

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
    if (!mapExcludeDataItems.containsKey("cumulative.lengthofstay.icu")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.lengthofstay.icu");
      cd.setItemtype("list");

      Map<String, Map<Long, Set<String>>> mapIcuLengthList =
          CoronaCumulativeLogic.createIcuLengthOfStayList(listEncounters, listLocations, mapIcu);
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
        cd.setItemname("cumulative.lengthofstay.icu.casenrs");
        cd.setItemtype("debug");

        cd.setData(listCaseId);
        currentDataList.add(cd);
      }

      // list with all lengths of icu stays (in h)
      if (!mapExcludeDataItems.containsKey("cumulative.lengthofstay.icu.alive")) {
        cd = new CoronaDataItem();
        cd.setItemname("cumulative.lengthofstay.icu.alive");
        cd.setItemtype("list");

        Map<String, Map<Long, Set<String>>> mapLengthIcuAlive = CoronaCumulativeLogic
            .createIcuLengthListByVitalstatus(VitalStatus.ALIVE, mapIcuLengthList, mapIcu);
        List<Long> listAliveHours = new ArrayList<>();
        List<String> listAliveCaseId = new ArrayList<>();

        for (Map.Entry<String, Map<Long, Set<String>>> entry : mapLengthIcuAlive.entrySet()) {
          Map<Long, Set<String>> value = entry.getValue();
          for (Map.Entry<Long, Set<String>> secondEntry : value.entrySet()) {
            listAliveHours.add(secondEntry.getKey());
            listAliveCaseId.addAll(secondEntry.getValue());
          }
        }

        Collections.sort(listAliveHours);
        cd.setData(listAliveHours);
        currentDataList.add(cd);
        if (debug) {
          cd = new CoronaDataItem();
          cd.setItemname("cumulative.lengthofstay.icu.alive.casenrs");
          cd.setItemtype("debug");
          cd.setData(listAliveCaseId);
          currentDataList.add(cd);
        }
      }


      // list with all lengths of icu stays (in h)
      if (!mapExcludeDataItems.containsKey("cumulative.lengthofstay.icu.dead")) {
        cd = new CoronaDataItem();
        cd.setItemname("cumulative.lengthofstay.icu.dead");
        cd.setItemtype("list");

        HashMap<String, Map<Long, Set<String>>> mapLengthOfStayDead = CoronaCumulativeLogic
            .createIcuLengthListByVitalstatus(VitalStatus.DEAD, mapIcuLengthList, mapIcu);

        List<Long> listDeadHours = new ArrayList<>();
        List<String> listDeadCaseId = new ArrayList<>();

        for (Map.Entry<String, Map<Long, Set<String>>> entry : mapLengthOfStayDead.entrySet()) {
          Map<Long, Set<String>> value = entry.getValue();
          for (Map.Entry<Long, Set<String>> secondEntry : value.entrySet()) {
            listDeadHours.add(secondEntry.getKey());
            listDeadCaseId.addAll(secondEntry.getValue());
          }
        }

        Collections.sort(listDeadHours);
        cd.setData(listDeadHours);
        currentDataList.add(cd);
        if (debug) {
          cd = new CoronaDataItem();
          cd.setItemname("cumulative.lengthofstay.icu.dead.casenrs");
          cd.setItemtype("debug");
          cd.setData(listDeadCaseId);
          currentDataList.add(cd);
        }
      }
    }

    // cumulative length of stays
    if (!mapExcludeDataItems.containsKey("cumulative.lengthofstay.hospital")) {
      cd = new CoronaDataItem();
      cd.setItemname("cumulative.lengthofstay.hospital");
      cd.setItemtype("list");
      HashMap<String, Map<Long, List<String>>> mapDays =
          CoronaCumulativeLogic.createMapDaysHospitalList(listEncounters);

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
        cd.setItemname("cumulative.lengthofstay.hospital.casenrs");
        cd.setItemtype("debug");
        cd.setData(listHospitalCaseId);
        currentDataList.add(cd);
      }

      // cumulative length of stays ALIVE
      if (!mapExcludeDataItems.containsKey("cumulative.lengthofstay.hospital.alive")) {
        cd = new CoronaDataItem();
        cd.setItemname("cumulative.lengthofstay.hospital.alive");
        cd.setItemtype("list");

        HashMap<String, Map<Long, List<String>>> mapDaysAlive =
            CoronaCumulativeLogic.createLengthOfStayHospitalByVitalstatus(listEncounters, mapDays,
                CoronaFixedValues.ALIVE.getValue());
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
          cd.setItemname("cumulative.lengthofstay.hospital.alive.casenrs");
          cd.setItemtype("debug");
          cd.setData(listHospitalAliveCaseId);
          currentDataList.add(cd);
        }
      }

      // cumulative length of stays ALIVE
      if (!mapExcludeDataItems.containsKey("cumulative.lengthofstay.hospital.dead")) {
        cd = new CoronaDataItem();
        cd.setItemname("cumulative.lengthofstay.hospital.dead");
        cd.setItemtype("list");

        HashMap<String, Map<Long, List<String>>> mapDaysDead =
            CoronaCumulativeLogic.createLengthOfStayHospitalByVitalstatus(listEncounters, mapDays,
                CoronaFixedValues.DEAD.getValue());
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
          cd.setItemname("cumulative.lengthofstay.hospital.dead.casenrs");
          cd.setItemtype("debug");

          cd.setData(listHospitalDeadCaseId);
          currentDataList.add(cd);
        }
      }
    }
    return currentDataList;
  }

  private void reportMissingFields(List<UkbEncounter> listEncounters) {
    // Encounter.period.start is mandatory and critical for many data items
    List<String> casesWithoutPeriodStart = listEncounters.parallelStream()
        .filter(x -> !x.isPeriodStartExistent())
        .map(UkbEncounter::getCaseId)
        .collect(Collectors.toList());
    if (casesWithoutPeriodStart.size() > 0)
      log.debug("Warning: " + casesWithoutPeriodStart.size()
          + " Encounters without period/period.start element have been detected [for example case with id: "
          + casesWithoutPeriodStart.get(0) + "]");
  }

  private void addValuesToTimelineMaxMap(String item, Set<Long> value,
    Map<String, List<Long>> mapResultTreatment) {
    List<Long> tempList = new ArrayList<>();
    tempList.add(new Long(value.size()));

    // this is for any other DataItem

    if (mapResultTreatment.containsKey(item)) {
      mapResultTreatment.get(item)
          .addAll(tempList);
    } else {
      mapResultTreatment.put(item, tempList);
    }
  }

  /**
   * checks if there are duplicate pids in different treatmentlevels removes them if they are in the
   * wrong list
   * 
   * @param listCumulativeAmbulantEncounter containing all ambulant cases needed for the cumulative
   *          logic
   * @param listCumulativeStandardWardEncounter containing all stationary cases needed for the
   *          cumulative logic
   * @param listCumulativeIcuEncounter containing all ICU cases needed for the cumulative logic
   * @param listCumulativeIcuVentEncounter containing all Ventilation cases needed for the
   *          cumulative logic
   * @param listCumulativeIcuEcmoEncounter containing all ECMO cases needed for the cumulative logic
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

    listCumulativeStandardWardEncounter
        .removeIf(e -> !listStandardWardPids.contains(e.getPatientId()));

    listIcuPids.removeAll(listVentPids);
    listIcuPids.removeAll(listEcmoPids);

    listCumulativeIcuEncounter.removeIf(e -> !listIcuPids.contains(e.getPatientId()));

    listVentPids.removeAll(listEcmoPids);

    listCumulativeIcuVentEncounter.removeIf(e -> !listVentPids.contains(e.getPatientId()));
  }


  private Set<String> createPidList(List<UkbEncounter> listCumulativeEncounter) {
    Set<String> resultList = new HashSet<>();
    resultList = listCumulativeEncounter.parallelStream()
        .map(UkbEncounter::getPatientId)
        .collect(Collectors.toSet());
    return resultList.stream()
        .distinct()
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
   *          resources that include active C19 cases.
   * @param listCurrentIcuEncounter List of all inpatient {@link UkbEncounter} resources that
   *          include active C19 cases and who are on an icu ward.
   * @param listCurrentVentEncounter List of all {@link UkbEncounter} resources that include active
   *          C19 cases that have an active ventilation period (and no active ecmo)
   * @param listCurrentEcmoEncounter List of all {@link UkbEncounter} resources that include active
   *          C19 cases that have an active ecmo period
   * @return map that connects treatmentlevel with a list of casenrs
   */
  private HashMap<String, List<String>> createMapCurrentTreatmentlevelCasenrs(
      List<UkbEncounter> listCurrentStandardWardEncounter,
      List<UkbEncounter> listCurrentIcuEncounter, List<UkbEncounter> listCurrentVentEncounter,
      List<UkbEncounter> listCurrentEcmoEncounter) {
    List<String> listStationaryCaseNrs = new ArrayList<String>();
    List<String> listIcuCaseNrs = new ArrayList<String>();
    List<String> listVentCaseNrs = new ArrayList<String>();
    List<String> listEcmoCaseNrs = new ArrayList<String>();
    HashMap<String, List<String>> resultCurrentTreatmentCaseNrs =
        new LinkedHashMap<String, List<String>>();

    // creating a list with the subset for all positive stationary (needed in age calculations)
    listCurrentStandardWardEncounter.forEach(encounter -> {
      if (CoronaResultFunctionality.isCaseClassInpatient(encounter)
          && encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())) {
        listStationaryCaseNrs.add(encounter.getId());
      }
    });
    listCurrentIcuEncounter.forEach(encounter -> listIcuCaseNrs.add(encounter.getId()));
    listCurrentVentEncounter.forEach(encounter -> listVentCaseNrs.add(encounter.getId()));
    listCurrentEcmoEncounter.forEach(encounter -> listEcmoCaseNrs.add(encounter.getId()));

    resultCurrentTreatmentCaseNrs.put(TreatmentLevel.NORMALSTATION.getText(),
        listStationaryCaseNrs);
    resultCurrentTreatmentCaseNrs.put(TreatmentLevel.ICU.getText(), listIcuCaseNrs);
    resultCurrentTreatmentCaseNrs.put(TreatmentLevel.ICUWITHVENTILATION.getText(), listVentCaseNrs);
    resultCurrentTreatmentCaseNrs.put(TreatmentLevel.ICUWITHECMO.getText(), listEcmoCaseNrs);

    return resultCurrentTreatmentCaseNrs;
  }
}
