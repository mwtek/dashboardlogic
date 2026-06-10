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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_GROUPED_BAR_CHARTS_CALC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS_UNIFORM;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_PSYCH_INTENSIVE_CARE_3_MONTHS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_ADMISSION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DIAGS_ADMISSION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DIAGS_OCCURRENCE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_PSYCH_INTENSIVE_CARE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_PSYCH_INTENSIVE_CARE_CHANGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_PSYCH_INTENSIVE_CARE_RATIO;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.UPPER_AGE_BORDER_PREFILTER;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar.QUALIFYING_DATE;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic.createCasesByDiag;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic.mergeEncounterAndFilterByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.filterEncounterByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.getInpatientFacilityEncounters;
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.getKidsRadarIcdCodesKjp;
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.removeEntriesByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.PatientFilter.filterPatientByExistingEncounter;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools.filterConditionsByRecordDate;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools.getConditionsByIcdCodes;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirProcedureTools.filterProceduresByRecordDate;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirProcedureTools.getProceduresByAnyOpsCodePrefix;
import static org.hl7.fhir.r4.model.ResourceType.Condition;
import static org.hl7.fhir.r4.model.ResourceType.Procedure;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.KiraData;
import de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic;
import de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic.MergeResult;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraTimelineDisorders;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the json specification with
 * kids radar context are generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiRaKjpDataItemGenerator extends DataItemGenerator {

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
  public KiRaKjpDataItemGenerator(
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
    List<DiseaseDataItem> currentDataList = new ArrayList<>();
    if (mapExcludeDataItems == null) {
      mapExcludeDataItems = new HashMap<>();
    }
    boolean debug = globalConfiguration.getDebug();

    // If there are resources with unfilled mandatory attributes, report them immediately (may give
    // partially reduced result sets)
    reportMissingFields(encounters);

    // Filtering outpatient and semi-stationary encounter (that could hold Encounter.class =
    // 'IMP' as well)
    facilityContactEncountersInpatient = getInpatientFacilityEncounters(encounters);

    // Logging of unexpected attribute assignments within the resources.
    reportAttributeArtifacts(facilityContactEncountersInpatient);

    // Filter conditions to the ones that lie after the starting date (2020-01)
    List<MiiCondition> conditionsFiltered =
        filterConditionsByRecordDate(conditions, QUALIFYING_DATE);
    // Filter conditions that are not kjp related
    conditionsFiltered =
        new ArrayList<>(
            getConditionsByIcdCodes(
                conditionsFiltered,
                getKidsRadarIcdCodesKjp(inputCodeSettings.getKidsRadarConditionKjpIcdCodes())));
    logDateFiltering(Condition, conditions.size(), conditionsFiltered.size());

    // Filter conditions that are not kjp related
    List<MiiProcedure> proceduresFilteredCode =
        new ArrayList<>(
            getProceduresByAnyOpsCodePrefix(
                procedures, List.of(inputCodeSettings.getKidsRadarKjpOpsCodePrefix())));
    // Filter procedures to the ones that lie after the starting date (2020-01)
    List<MiiProcedure> proceduresFiltered =
        filterProceduresByRecordDate(proceduresFilteredCode, QUALIFYING_DATE);
    logDateFiltering(Procedure, proceduresFilteredCode.size(), proceduresFiltered.size());

    // Patient IDs of patients with a valid condition
    Set<String> conditionPids =
        conditionsFiltered.stream().map(MiiCondition::getPatientId).collect(Collectors.toSet());
    // Filter all patients that don't have a kjp condition
    List<MiiPatient> patientsFilteredByCond =
        patients.stream().filter(x -> conditionPids.contains(x.getId())).toList();
    List<MiiEncounter> facilityContactEncountersInpatientWithCond =
        facilityContactEncountersInpatient.stream()
            .filter(x -> conditionPids.contains(x.getPatientId()))
            .toList();

    // Pre-filtering of all cases where the patient age is >20 at admission date
    // We look just at patients <18 at admission but need the 18+-year-old-encounter as well since
    // its possible that cases get merged
    List<MiiEncounter> facilityContactsFilteredByAge =
        filterEncounterByAge(
            facilityContactEncountersInpatientWithCond,
            patientsFilteredByCond,
            UPPER_AGE_BORDER_PREFILTER);
    logFilteredAgeAtAdmission(
        ResourceType.Encounter,
        facilityContactEncountersInpatientWithCond.size(),
        facilityContactsFilteredByAge.size());

    // Filter the patients to the ones that have at least 1 encounter with age <19
    List<MiiPatient> patientsFiltered =
        filterPatientByExistingEncounter(patientsFilteredByCond, facilityContactsFilteredByAge);
    logFilteredAgeAtAdmission(
        ResourceType.Patient, patientsFilteredByCond.size(), patientsFiltered.size());

    // Merge cases
    int before = facilityContactsFilteredByAge.size();
    MergeResult merge =
        mergeEncounterAndFilterByAge(
            facilityContactsFilteredByAge,
            conditionsFiltered,
            proceduresFiltered,
            patientsFiltered,
            inputCodeSettings);
    List<MiiEncounter> facilityContactEncountersKjpMerged = merge.mergedEncounters();
    // Merge these lists for overall patient filtering
    Map<String, CoreCaseData> coreCaseDataAll = merge.registryByEncounterId();
    log.debug(
        "{} Facility contacts got filtered after merging and age condition check. ",
        before - facilityContactEncountersKjpMerged.size());

    Map<String, Map<String, CoreCaseData>> coreCaseDataByKjpDiagnosis =
        createCasesByDiag(
            conditions,
            facilityContactEncountersKjpMerged,
            null,
            patientsFiltered,
            inputCodeSettings.getKidsRadarConditionKjpIcdCodes(),
            coreCaseDataAll);

    // Update or initialize parameters that are needed in certain data items
    KiraHandlingLogic.updateAdmissionStatuses(coreCaseDataAll);
    KiraHandlingLogic.updateIntensiveCareDays(coreCaseDataAll, proceduresFiltered);

    // Now the 18+ admissions aren't necessary anymore, so we filter them
    int patientSizeBefore = patientsFiltered.size();
    patientsFiltered = removeEntriesByAge(coreCaseDataAll, patientsFiltered);
    logFilteredOldPatients(patientSizeBefore, patientsFiltered.size());

    Set<String> facilityEncounterIdsKjp =
        coreCaseDataByKjpDiagnosis.values().stream()
            .flatMap(innerMap -> innerMap.values().stream())
            .map(CoreCaseData::getFacilityEncounterId)
            .collect(Collectors.toSet());

    // Initialize the base data for the following data item calculations
    // Using the kjp merged data as base, but be aware that there is an individual rsv one
    KiraData kiraData = new KiraData();
    kiraData.initializeData(
        inputCodeSettings,
        null,
        facilityContactEncountersKjpMerged,
        patientsFiltered,
        observations,
        conditionsFiltered,
        locations,
        procedures,
        dataItemContext);

    DiseaseDataItem cd;

    // cumulative.diags.age
    String cumulativeKjpAgeLabel = determineKiRaLabel(KJP, CUMULATIVE_DIAGS_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeKjpAgeLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeKjpAgeLabel,
              ITEMTYPE_STACKED_BAR_CHARTS_UNIFORM,
              new DataBuilder()
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildKiraCumulativeAgeDisorders()));
    }

    // cumulative.diags.gender
    String cumulativeKjpDiagsGender = determineKiRaLabel(KJP, CUMULATIVE_DIAGS_GENDER);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeKjpDiagsGender, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeKjpDiagsGender,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildKiraCumulativeDisordersGenderKjp()));
    }

    // cumulative.diags.lengthofstay
    String cumulativeKjpLengthOfStay = determineKiRaLabel(KJP, CUMULATIVE_LENGTHOFSTAY);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeKjpLengthOfStay, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeKjpLengthOfStay,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildKiraLengthOfStay()));
    }

    // timeline.diags.occurrence
    String tlKjpOccurrenceLabel = determineKiRaLabel(KJP, TIMELINE_DIAGS_OCCURRENCE);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpOccurrenceLabel, false)) {
      KiraTimelineDisorders item = new DataBuilder().buildKiraTimelineDisorders();
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpOccurrenceLabel,
              ITEMTYPE_STACKED_BAR_CHARTS_UNIFORM,
              new DataBuilder()
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildKiraTimelineDisordersItem(item)));
      if (debug) {
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(tlKjpOccurrenceLabel), ITEMTYPE_DEBUG, item.getDebugData()));
      }
    }

    // timeline.age
    String tlKjpAgeLabel = determineKiRaLabel(KJP, TIMELINE_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpAgeLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpAgeLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .dbData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildTimelineAgeKjp()));
    }

    // kira.kjp.timeline.intensivecare
    String tlKjpTimelineIntensiveCare = determineKiRaLabel(KJP, TIMELINE_PSYCH_INTENSIVE_CARE);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpTimelineIntensiveCare, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpTimelineIntensiveCare,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .dbData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataAll(coreCaseDataAll)
                  .buildKiraTimelineIntensiveCare()));
    }

    // kira.kjp.timeline.intensivecare_ratio
    String tlKjpTimelineIntensiveCareRatio =
        determineKiRaLabel(KJP, TIMELINE_PSYCH_INTENSIVE_CARE_RATIO);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpTimelineIntensiveCareRatio, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpTimelineIntensiveCareRatio,
              ITEMTYPE_GROUPED_BAR_CHARTS_CALC,
              new DataBuilder()
                  .dbData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataAll(coreCaseDataAll)
                  .buildKiraTimelineIntensiveCareRatio()));
    }

    // kira.kjp.timeline.intensivecare_change
    String tlKjpTimelineIntensiveCareChange =
        determineKiRaLabel(KJP, TIMELINE_PSYCH_INTENSIVE_CARE_CHANGE);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpTimelineIntensiveCareChange, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpTimelineIntensiveCareChange,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .dbData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataAll(coreCaseDataAll)
                  .buildKiraTimelineIntensiveCareChange()));
    }

    // kira.kjp.timeline.intensivecare_3months
    String tlKjpTimelineIntensiveCare3Months =
        determineKiRaLabel(KJP, CUMULATIVE_PSYCH_INTENSIVE_CARE_3_MONTHS);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpTimelineIntensiveCare3Months, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpTimelineIntensiveCare3Months,
              ITEMTYPE_GROUPED_BAR_CHARTS_CALC,
              new DataBuilder()
                  .dbData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataAll(coreCaseDataAll)
                  .buildKiraTimelineIntensiveCare3Months()));
    }

    // timeline.admission
    String tlKjpTimelineAdmission = determineKiRaLabel(KJP, TIMELINE_ADMISSION);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpTimelineAdmission, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpTimelineAdmission,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .dbData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildTimelineAdmission()));
    }

    // timeline.diags.admission
    String tlKjpTimelineDiagsAdmission = determineKiRaLabel(KJP, TIMELINE_DIAGS_ADMISSION);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpTimelineDiagsAdmission, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpTimelineDiagsAdmission,
              ITEMTYPE_STACKED_BAR_CHARTS_UNIFORM,
              new DataBuilder()
                  .dbData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildTimelineDiagsAdmission()));
    }

    return currentDataList;
  }

  private static void logFilteredOldPatients(int patientSizeBefore, int patientSizeFiltered) {
    if (patientSizeFiltered > 0)
      log.debug(
          "{} patient resources filtered because no encounter was found where they were younger"
              + " than 18 years.",
          patientSizeBefore - patientSizeFiltered);
  }

  private static void logFilteredAgeAtAdmission(
      ResourceType resourceType, int sumBefore, int sumFiltered) {
    if (sumFiltered > 0)
      log.info(
          "{} {} resource(s) were filtered because they were older than 19 years at the time of"
              + " admission.",
          sumBefore - sumFiltered,
          resourceType.name());
  }

  private void logDateFiltering(ResourceType resourceType, int sumBefore, int sumFiltered) {
    if (sumFiltered > 0)
      log.info(
          "{} {} resource(s) were filtered because they are older than {} or do not contain any"
              + " recorded date. date. ",
          sumBefore - sumFiltered,
          resourceType.name(),
          QUALIFYING_DATE);
  }
}
