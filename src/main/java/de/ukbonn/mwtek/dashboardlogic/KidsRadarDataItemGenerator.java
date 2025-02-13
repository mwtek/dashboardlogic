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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_CHART_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DIAGS_OCCURRENCE;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.CASE_MERGED;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.UPPER_AGE_BORDER_PREFILTER;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.RSV;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar.QUALIFYING_DATE;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic.createCasesByDiag;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic.mergeCoreCaseDataLists;
import static de.ukbonn.mwtek.dashboardlogic.logic.KiraHandlingLogic.mergeEncounterAndFilterByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.filterEncounterByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.getInpatientFacilityEncounters;
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.removeEntriesByAge;
import static de.ukbonn.mwtek.dashboardlogic.tools.PatientFilter.filterPatientByExistingEncounter;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools.filterConditionsByRecordDate;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.KiraData;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraTimelineDisorders;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.DataBuilder;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class KidsRadarDataItemGenerator extends DataItemGenerator {

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
  public KidsRadarDataItemGenerator(
      List<UkbCondition> conditions,
      List<UkbObservation> observations,
      List<UkbPatient> patients,
      List<UkbEncounter> encounters,
      List<UkbProcedure> icuProcedures,
      List<UkbLocation> locations) {
    super(conditions, null, patients, encounters, null, null);
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems Map with data items to be excluded from the output (e.g.
   *     "current.treatmentlevel").
   * @param debug Flag to provide debug information (e.g. casenrs) in the output.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *     codes or procedure codes.
   * @param usePartOfInsteadOfIdentifier Should the Encounter.partOf value be used additionally of
   *     the visit-number in {@link UkbEncounter#getIdentifier()} to assign
   *     'Versorgungsstellenkontakt' to 'Einrichtungskontakt'?
   * @return List with all the {@link DiseaseDataItem data items} that are defined in the corona
   *     dashboard json specification
   */
  @SuppressWarnings("unused")
  public List<DiseaseDataItem> getDataItems(
      Map<String, Boolean> mapExcludeDataItems,
      Boolean debug,
      VariantSettings variantSettings,
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      DataItemContext dataItemContext,
      Boolean usePartOfInsteadOfIdentifier) {
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

    // Filtering outpatient and semi-stationary encounter (that could hold Encounter.class =
    // 'IMP' as well)
    facilityContactEncounters = getInpatientFacilityEncounters(encounters);

    // Logging of unexpected attribute assignments within the resources.
    reportAttributeArtifacts(facilityContactEncounters);

    // Filter conditions to the ones that lie after the starting date (2020-01)
    List<UkbCondition> conditionsFiltered =
        filterConditionsByRecordDate(conditions, QUALIFYING_DATE);
    log.info(
        "{} conditions got filtered because they're older than {} or doesn't contain any recorded"
            + " date. ",
        conditions.size() - conditionsFiltered.size(),
        QUALIFYING_DATE);

    // Pre-filtering of all cases where the patient age is >20 at admission date
    // We look just at patients <18 at admission but need the 18+-year-old-encounter as well since
    // its possible that cases get merged
    List<UkbEncounter> facilityContactsFilteredByAge =
        filterEncounterByAge(facilityContactEncounters, patients, UPPER_AGE_BORDER_PREFILTER);
    log.info(
        "{} encounter got filtered because the patients were older than 19 years at admission "
            + "date. ",
        facilityContactEncounters.size() - facilityContactsFilteredByAge.size());

    // Filter the patients to the ones that have at least 1 encounter with age <19
    List<UkbPatient> patientsFiltered =
        filterPatientByExistingEncounter(patients, facilityContactsFilteredByAge);
    log.info(
        "{} patients got filtered because they were older than 19 years at any admission date ",
        patients.size() - patientsFiltered.size());

    // Merge cases
    int facilityContactsBeforeAgeFiltering = facilityContactsFilteredByAge.size();
    List<UkbEncounter> facilityContactEncountersKjpMerged =
        mergeEncounterAndFilterByAge(
            facilityContactsFilteredByAge, conditionsFiltered, patientsFiltered, inputCodeSettings);
    List<UkbEncounter> debugMergedCases =
        facilityContactEncountersKjpMerged.stream()
            .filter(x -> x.hasExtension(CASE_MERGED.getUrl()))
            .toList();
    log.debug(
        "{} Facility contacts got filtered after merging and age condition check. ",
        facilityContactsBeforeAgeFiltering - facilityContactEncountersKjpMerged.size());

    Map<String, Map<String, CoreCaseData>> coreCaseDataByKjpDiagnosis =
        createCasesByDiag(
            conditions,
            facilityContactEncountersKjpMerged,
            patientsFiltered,
            inputCodeSettings.getKidsRadarConditionKjpIcdCodes());

    Map<String, Map<String, CoreCaseData>> coreCaseDataByRsvDiagnosis =
        createCasesByDiag(
            conditions,
            facilityContactsFilteredByAge, // no merging for rsv patients
            patientsFiltered,
            inputCodeSettings.getKidsRadarConditionRsvIcdCodes());
    // Merge these lists for overall patient filtering
    Map<String, CoreCaseData> coreCaseDataAll =
        mergeCoreCaseDataLists(coreCaseDataByKjpDiagnosis, coreCaseDataByRsvDiagnosis);

    // Now the 18+ admissions aren't necessary anymore, so we filter them
    int patientSizeBefore = patientsFiltered.size();
    patientsFiltered = removeEntriesByAge(coreCaseDataAll, patientsFiltered);
    log.debug(
        "{} patient resources filtered because no encounter was found where they were younger than"
            + " 18 years.",
        patientSizeBefore - patientsFiltered.size());

    Set<String> facilityEncounterIdsKjp =
        coreCaseDataByKjpDiagnosis.values().stream()
            .flatMap(innerMap -> innerMap.values().stream())
            .map(CoreCaseData::getFacilityEncounterId)
            .collect(Collectors.toSet());

    Set<String> facilityEncounterIdsRsv =
        coreCaseDataByRsvDiagnosis.values().stream()
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
        icuProcedures,
        dataItemContext);
    // The rsv data items rely on less filter (and merging steps) than the items that are based
    // on disorder groups.
    kiraData.setFacilityEncountersRsv(
        facilityContactsFilteredByAge.stream()
            .filter(x -> facilityEncounterIdsRsv.contains(x.getFacilityContactId()))
            .toList());

    DiseaseDataItem cd;
    // cumulative diags zipcode
    String cumulativeZipCodeKjp = determineKiRaLabel(KJP, CUMULATIVE_DIAGS_ZIPCODE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeZipCodeKjp, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeZipCodeKjp,
              ITEMTYPE_CHART_LIST,
              new DataBuilder()
                  .kiraData(kiraData)
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .encounterSubSet(kiraData.getFacilityContactEncounters())
                  .buildKiraZipCodeList()));
    }

    String cumulativeKjpDiagsGender = determineKiRaLabel(KJP, CUMULATIVE_DIAGS_GENDER);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeKjpDiagsGender, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeKjpDiagsGender,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildKiraCumulativeDisordersGender()));
    }

    String cumulativeKjpAgeLabel = determineKiRaLabel(KJP, CUMULATIVE_DIAGS_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeKjpAgeLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeKjpAgeLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(KJP)
                  .coreCaseDataByGroups(coreCaseDataByKjpDiagnosis)
                  .buildKiraCumulativeAgeDisorders()));
    }

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

    String tlKjpOccurrenceLabel = determineKiRaLabel(KJP, TIMELINE_DIAGS_OCCURRENCE);
    if (isItemNotExcluded(mapExcludeDataItems, tlKjpOccurrenceLabel, false)) {
      KiraTimelineDisorders item = new DataBuilder().buildKiraTimelineDisorders();
      currentDataList.add(
          new DiseaseDataItem(
              tlKjpOccurrenceLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
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

    // kira.rsv.cumulative.diags.zipcode
    String cumulativeZipCodeRsv = determineKiRaLabel(RSV, CUMULATIVE_DIAGS_ZIPCODE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeZipCodeRsv, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeZipCodeRsv,
              ITEMTYPE_CHART_LIST,
              new DataBuilder()
                  .kiraData(kiraData)
                  .kidsRadarDataItemContext(RSV)
                  .encounterSubSet(kiraData.getFacilityEncountersRsv())
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraZipCodeList()));
    }

    // kira.rsv.cumulative.diags.age
    String cumulativeRsvAgeLabel = determineKiRaLabel(RSV, CUMULATIVE_DIAGS_AGE);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeRsvAgeLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeRsvAgeLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(RSV)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraCumulativeAgeDisorders()));
    }

    // kira.rsv.cumulative.diags.gender
    String cumulativeRsvGender = determineKiRaLabel(RSV, CUMULATIVE_DIAGS_GENDER);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeRsvGender, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeRsvGender,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(RSV)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraCumulativeDisordersGender()));
    }

    String cumulativeRsvLengthOfStay = determineKiRaLabel(RSV, CUMULATIVE_LENGTHOFSTAY);
    if (isItemNotExcluded(mapExcludeDataItems, cumulativeRsvLengthOfStay, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              cumulativeRsvLengthOfStay,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(RSV)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraLengthOfStay()));
    }

    String tlRsvOccurrenceLabel = determineKiRaLabel(RSV, TIMELINE_DIAGS_OCCURRENCE);
    if (isItemNotExcluded(mapExcludeDataItems, tlRsvOccurrenceLabel, false)) {
      KiraTimelineDisorders item = new DataBuilder().buildKiraTimelineDisorders();
      currentDataList.add(
          new DiseaseDataItem(
              tlRsvOccurrenceLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder()
                  .kidsRadarDataItemContext(RSV)
                  .coreCaseDataByGroups(coreCaseDataByRsvDiagnosis)
                  .buildKiraTimelineDisordersItem(item)));
      if (debug) {
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(tlRsvOccurrenceLabel), ITEMTYPE_DEBUG, item.getDebugData()));
      }
    }

    return currentDataList;
  }
}
