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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_AGGREGATED;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_CHART_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_AGE_PCR;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DIAGS_OCCURRENCE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.UPPER_AGE_BORDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.PED;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.PED_COV;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.PED_INFL;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.PED_PERTUSSIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.PED_RSV;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar.QUALIFYING_DATE;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.generateSupplyContactToFacilityContactMap;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic.createCasesByDiag;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.filterEncounterByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.getInpatientFacilityEncounters;
import static de.ukbonn.mwtek.dashboardlogic.tools.PatientFilter.filterPatientByExistingEncounter;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools.filterConditionsByRecordDate;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.KiraData;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraTimelineDisorders;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityContactIcuLocationMap;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.DataBuilder;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the json specification with
 * kids radar context are generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiRaPedDataItemGenerator extends DataItemGenerator {

  /**
   * Initialization of the CoronaResults object with the required FhirRessource lists.
   *
   * @param conditions List of FHIR condition resources for U07.1 diagnoses
   * @param patients List of FHIR patient resources based on the Condition and Observation inputs
   * @param encounters List of FHIR encounter resources based on the Condition and Observation
   *     inputs
   * @param icuProcedures List of FHIR ICU procedures resources based on the Patient/Encounter
   *     inputs
   * @param locations List of all FHIR location resources based on the encounter inputs
   */
  public KiRaPedDataItemGenerator(
      List<MiiCondition> conditions,
      List<MiiObservation> observations,
      List<MiiPatient> patients,
      List<MiiEncounter> encounters,
      List<MiiProcedure> icuProcedures,
      List<MiiLocation> locations) {
    super(conditions, observations, patients, encounters, icuProcedures, locations);
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems Map with data items to be excluded from the output (e.g.
   *     "current.treatmentlevel").
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *     codes or procedure codes.
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
    List<DiseaseDataItem> resultDataItems = new ArrayList<>();
    if (mapExcludeDataItems == null) {
      mapExcludeDataItems = new HashMap<>();
    }
    boolean useIcuUndiff = globalConfiguration.getUseIcuUndifferentiated();
    boolean debug = globalConfiguration.getDebug();

    // If there are resources with unfilled mandatory attributes, report them immediately (may give
    // partially reduced result sets)
    reportMissingFields(encounters);

    // The kira ped data got a different treatment level structure but uses the same configuration
    // file as covid/influenza regarding ventilation; so some customizing is needed
    updateInputSettingsToKiraRules(inputCodeSettings);

    // Filtering outpatient and semi-stationary encounter (that could hold Encounter.class =
    // 'IMP' as well)
    facilityContactEncountersInpatient = getInpatientFacilityEncounters(encounters);

    List<MiiEncounter> departmentContacts =
        encounters.stream().filter(MiiEncounter::isDepartmentContact).toList();

    List<MiiEncounter> supplyContacts =
        encounters.stream().filter(MiiEncounter::isSupplyContact).toList();

    // Initialize supply contact -> facility contact hierarchy
    boolean supplyContactsFound = !supplyContacts.isEmpty();
    boolean locationsFound = locations != null && !locations.isEmpty();
    if (supplyContactsFound && locationsFound) {
      Map<String, String> supplyContactIdFacilityContactId =
          generateSupplyContactToFacilityContactMap(
              supplyContacts,
              departmentContacts,
              facilityContactEncountersInpatient,
              globalConfiguration.getUsePartOfInsteadOfIdentifier());
    } else {
      log.warn(
          "No encounter with level 'Versorgungsstellenkontakt' and/or location resources were"
              + " found. All data items that require a 'maxtreatmentlevel' separation are"
              + " filtered.");
    }

    FacilityContactIcuLocationMap facilityContactIcuLocationMap =
        new FacilityContactIcuLocationMap(supplyContacts, locations);

    // Logging of unexpected attribute assignments within the resources.
    reportAttributeArtifacts(facilityContactEncountersInpatient);

    // Filter conditions to the ones that lie after the starting date (2020-01)
    List<MiiCondition> conditionsFiltered =
        filterConditionsByRecordDate(conditions, QUALIFYING_DATE);
    log.info(
        "{} conditions got filtered because they're older than {} or doesn't contain any recorded"
            + " date. ",
        conditions.size() - conditionsFiltered.size(),
        QUALIFYING_DATE);

    // Filtering of all cases where the patient age is >=18 at admission date
    List<MiiEncounter> facilityContactsFilteredByAge =
        filterEncounterByAge(facilityContactEncountersInpatient, patients, UPPER_AGE_BORDER);
    log.info(
        "{} encounter got filtered because the patients were older than 17 years at admission "
            + "date. ",
        facilityContactEncountersInpatient.size() - facilityContactsFilteredByAge.size());

    // Filter the patients to the ones that have at least 1 encounter with age <19
    List<MiiPatient> patientsFiltered =
        filterPatientByExistingEncounter(patients, facilityContactsFilteredByAge);
    log.info(
        "{} patients got filtered because they were older than 17 years at any admission date ",
        patients.size() - patientsFiltered.size());

    Map<String, Map<String, CoreCaseData>> coreCaseDataByRsvDiagnosis =
        createCasesByDiag(
            conditions,
            facilityContactsFilteredByAge, // no merging for rsv patients
            facilityContactIcuLocationMap,
            patientsFiltered,
            inputCodeSettings.getKidsRadarConditionPedIcdCodes());

    Map<String, Map<String, CoreCaseData>> coreCaseDataByPed =
        createCasesByDiag(
            conditions,
            facilityContactsFilteredByAge,
            facilityContactIcuLocationMap,
            patientsFiltered,
            null);

    Set<String> facilityEncounterIdsRsv =
        coreCaseDataByRsvDiagnosis.values().stream()
            .flatMap(innerMap -> innerMap.values().stream())
            .map(CoreCaseData::getFacilityEncounterId)
            .collect(Collectors.toSet());

    Set<String> facilityEncounterIdsPed =
        coreCaseDataByPed.values().stream()
            .flatMap(innerMap -> innerMap.values().stream())
            .map(CoreCaseData::getFacilityEncounterId)
            .collect(Collectors.toSet());

    // Initialize the base data for the following data item calculations
    // Using the kjp merged data as base, but be aware that there is an individual rsv one
    KiraData kiraData = new KiraData();
    kiraData.initializeData(
        inputCodeSettings,
        qualitativeLabCodesSettings,
        facilityContactsFilteredByAge,
        patientsFiltered,
        observations,
        conditionsFiltered,
        locations,
        procedures,
        dataItemContext);
    // The rsv data items rely on less filter (and merging steps) than the items that are based
    // on disorder groups.
    kiraData.setFacilityEncountersRsv(
        facilityContactsFilteredByAge.stream()
            .filter(x -> facilityEncounterIdsRsv.contains(x.getFacilityContactId()))
            .toList());

    kiraData.setFacilityEncountersPed(
        facilityContactsFilteredByAge.stream()
            .filter(x -> facilityEncounterIdsPed.contains(x.getFacilityContactId()))
            .toList());

    DiseaseDataItem cd;
    // kira.pediatric.current.zipcode
    String pedCurrentZipcodeLabel = determineKiRaLabel(PED, CURRENT_ZIPCODE);
    if (isItemNotExcluded(mapExcludeDataItems, pedCurrentZipcodeLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              pedCurrentZipcodeLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .applyDiseasePositiveFilter(false)
                  .encounterSubSet(kiraData.getFacilityContactEncounters())
                  .buildCurrentZipCodeList()));
    }

    // kira.pediatric.current.treatmentlevel
    String pedCurrentTreatmentlevelLabel = determineKiRaLabel(PED, CURRENT_TREATMENTLEVEL);
    if (isItemNotExcluded(mapExcludeDataItems, pedCurrentTreatmentlevelLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              pedCurrentTreatmentlevelLabel,
              ITEMTYPE_AGGREGATED,
              new DataBuilder()
                  .kiraData(kiraData)
                  .kidsRadarDataItemContext(PED)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .encounterSubSet(kiraData.getFacilityContactEncounters())
                  .useIcuUndiff(useIcuUndiff)
                  .buildCurrentTreatmentlevel()
                  .getData()));
    }

    String pedTlMaxtreatmentlevelLabel = determineKiRaLabel(PED, TIMELINE_MAXTREATMENTLEVEL);
    if (isItemNotExcluded(mapExcludeDataItems, pedTlMaxtreatmentlevelLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              pedTlMaxtreatmentlevelLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .facilityContactIcuLocationMap(facilityContactIcuLocationMap)
                  .useIcuUndiff(useIcuUndiff)
                  .buildKiraPedMaxTreatmentlevelTimeline()));
    }

    // kira.ped.covid.timeline.age
    String timelineAgeCovLabel = determineKiRaLabel(PED_COV, TIMELINE_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgeCovLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgeCovLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgeCovid()));
    }

    // kira.ped.influenza.timeline.age.pcr
    String timelineAgePcrCovLabel = determineKiRaLabel(PED_COV, TIMELINE_AGE_PCR);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgePcrCovLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgePcrCovLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgePcrCovid()));
    }

    // kira.ped.influenza.timeline.age
    String timelineAgeInflLabel = determineKiRaLabel(PED_INFL, TIMELINE_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgeInflLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgeInflLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgeInfluenza()));
    }

    // kira.ped.influenza.timeline.age.pcr
    String timelineAgePcrInflLabel = determineKiRaLabel(PED_INFL, TIMELINE_AGE_PCR);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgePcrInflLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgePcrInflLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgePcrInfluenza()));
    }

    // kira.ped.pertussis.timeline.age
    String timelineAgePertLabel = determineKiRaLabel(PED_PERTUSSIS, TIMELINE_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgePertLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgePertLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgePertussis()));
    }

    // kira.ped.pertussis.timeline.age.pcr
    String timelineAgePcrPertLabel = determineKiRaLabel(PED_PERTUSSIS, TIMELINE_AGE_PCR);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgePcrPertLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgePcrPertLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgePcrPertussis()));
    }

    // kira.rsv.cumulative.diags.age
    String cumulativeRsvAgeLabel = determineKiRaLabel(PED_RSV, CUMULATIVE_DIAGS_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeRsvAgeLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              cumulativeRsvAgeLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KidsRadarDataItemContext.PED)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraCumulativeAgeDisorders()));
    }

    // kira.rsv.cumulative.diags.age
    String cumulativeRsvDiagsZipcodeLabel = determineKiRaLabel(PED_RSV, CUMULATIVE_DIAGS_ZIPCODE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeRsvDiagsZipcodeLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              cumulativeRsvDiagsZipcodeLabel,
              ITEMTYPE_CHART_LIST,
              new DataBuilder()
                  .kidsRadarDataItemContext(KidsRadarDataItemContext.PED)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .encounterSubSet(kiraData.getFacilityContactEncounters())
                  .kiraData(kiraData)
                  .buildKiraZipCodeList()));
    }

    // kira.rsv.cumulative.diags.gender
    String cumulativeRsvGender = determineKiRaLabel(PED_RSV, CUMULATIVE_DIAGS_GENDER);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeRsvGender, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              cumulativeRsvGender,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KidsRadarDataItemContext.PED)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraCumulativeDisordersGenderPed()));
    }

    String cumulativeRsvLengthOfStay = determineKiRaLabel(PED_RSV, CUMULATIVE_LENGTHOFSTAY);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeRsvLengthOfStay, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              cumulativeRsvLengthOfStay,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KidsRadarDataItemContext.PED)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraLengthOfStay()));
    }

    String tlRsvOccurrenceLabel = determineKiRaLabel(PED_RSV, TIMELINE_DIAGS_OCCURRENCE);
    if (isItemNotExcluded(mapExcludeDataItems, tlRsvOccurrenceLabel, false)) {
      KiraTimelineDisorders item = new DataBuilder().buildKiraTimelineDisorders();
      resultDataItems.add(
          new DiseaseDataItem(
              tlRsvOccurrenceLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KidsRadarDataItemContext.PED)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraRsvTimelineDiagsItem(item)));
      if (debug) {
        resultDataItems.add(
            new DiseaseDataItem(
                addDebugLabel(tlRsvOccurrenceLabel), ITEMTYPE_DEBUG, item.getDebugData()));
      }
    }

    // kira.ped.rsv.timeline.age
    String timelineAgeRsvLabel = determineKiRaLabel(PED_RSV, TIMELINE_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgeRsvLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgeRsvLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgeRsv()));
    }

    // kira.ped.rsv.timeline.age.pcr
    String timelineAgePcrRsvLabel = determineKiRaLabel(PED_RSV, TIMELINE_AGE_PCR);
    if (isItemNotExcluded(mapExcludeDataItems, timelineAgePcrRsvLabel, false)) {
      resultDataItems.add(
          new DiseaseDataItem(
              timelineAgePcrRsvLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .dbData(kiraData)
                  .coreCaseDataByGroups(coreCaseDataByPed)
                  .buildTimelineAgePcrRsv()));
    }

    return resultDataItems;
  }

  /**
   * Normalizes ventilation codes by removing entries that are classified as their own treatment
   * levels (High-Flow and CPAP).
   */
  private void updateInputSettingsToKiraRules(InputCodeSettings s) {
    if (s == null) return;

    // Make a mutable copy of ventilation codes (null-safe)
    List<String> ventilation =
        Optional.ofNullable(s.getProcedureVentilationCodes())
            .map(ArrayList::new) // mutable copy
            .orElseGet(ArrayList::new);

    // Build removal set (null-safe)
    Set<String> toRemove = new HashSet<>();
    List<String> highFlow = s.getProcedureHighFlowCodes();
    List<String> cpap = s.getProcedureCpapCodes();
    if (highFlow != null) toRemove.addAll(highFlow);
    if (cpap != null) toRemove.addAll(cpap);

    if (!toRemove.isEmpty()) {
      ventilation.removeIf(toRemove::contains);
    }

    // Persist back (ensures the field becomes mutable going forward)
    s.setProcedureVentilationCodes(ventilation);
  }
}
