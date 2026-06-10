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

package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE;

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.Gender;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.StackedBarCharts;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardData;
import de.ukbonn.mwtek.dashboardlogic.logic.KiraData;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CumulativeZipCode;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age.CumulativeAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age.KiraCumulativeAgeDisorders;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGender;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGenderByClass;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.KiraCumulativeDiagsGender;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.KiraLengthOfStay;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.results.CumulativeResult;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentRecruitment;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentZipCode;
import de.ukbonn.mwtek.dashboardlogic.logic.current.KiraCurrentTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.age.CurrentMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraKjpTimelineAdmission;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraKjpTimelineAge;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraKjpTimelineIntensiveCare;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraPedTimelineAge;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraPedTimelineMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraTimelineDisorders;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineConsent;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineDeath;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineRecruitment;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineTests;
import de.ukbonn.mwtek.dashboardlogic.models.AggregatedDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.ChartListItem;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityContactIcuLocationMap;
import de.ukbonn.mwtek.dashboardlogic.models.GroupedBarChartsCalcItem;
import de.ukbonn.mwtek.dashboardlogic.models.KiraInteger;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsUniformItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiQuestionnaireResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true, chain = true)
public class DataBuilder {
  private Map<TreatmentLevels, List<MiiEncounter>> icuDiseaseMap;
  private TreatmentLevels treatmentLevel;
  private Map<TreatmentLevels, List<MiiEncounter>> mapPositiveEncounterByClass;
  private Map<TreatmentLevels, List<MiiEncounter>> mapCurrentIcuPositive;
  private List<MiiEncounter> icuSupplyContactEncounters;
  private List<MiiEncounter> encounterSubSet;
  private DashboardData dbData;
  private FacilityContactIcuLocationMap facilityContactIcuLocationMap;
  private List<MiiEncounter> currentStandardWardEncounters;
  private List<MiiEncounter> currentIcuEncounters;
  private List<MiiEncounter> currentVentEncounters;
  private List<MiiEncounter> currentEcmoEncounters;
  private List<MiiEncounter> currentIcuUndiffEncounters;

  private List<MiiConsent> consents;
  private List<MiiQuestionnaireResponse> questionnaireResponses;

  private DashboardLogicFixedValues labResult;
  private DataItemContext dataItemContext;
  private Gender gender;

  private KidsRadarDataItemContext kidsRadarDataItemContext;
  private KiraData kiraData;
  private Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups;
  private Map<String, CoreCaseData> coreCaseDataAll;

  // Needed for the zip code generation since there is different logic between covid/infl + ped
  private Boolean applyDiseasePositiveFilter = true;
  private Boolean useIcuUndiff;

  public List<MiiEncounter> buildCumulativeByClass() {
    return new CumulativeMaxTreatmentLevel()
        .getCumulativeByClass(icuDiseaseMap, treatmentLevel, mapPositiveEncounterByClass);
  }

  public List<MiiEncounter> buildCumulativeByIcuLevel() {
    return new CumulativeMaxTreatmentLevel().getCumulativeByIcuLevel(icuDiseaseMap, treatmentLevel);
  }

  public List<MiiEncounter> buildCurrentEncounterByIcuLevel() {
    return CurrentTreatmentLevel.getCurrentEncounterByIcuLevel(
        mapCurrentIcuPositive,
        treatmentLevel,
        icuSupplyContactEncounters,
        dbData.getFacilityContactEncounters(),
        dbData.getIcuProcedures(),
        dbData.getLocations(),
        dbData.getInputCodeSettings());
  }

  public Map<String, List<String>> buildCurrentTreatmentlevelMapCaseIds() {
    return CurrentTreatmentLevel.createMapCurrentTreatmentlevelCaseIds(
        currentStandardWardEncounters,
        currentIcuEncounters,
        currentVentEncounters,
        currentEcmoEncounters,
        currentIcuUndiffEncounters,
        useIcuUndiff);
  }

  public List<MiiEncounter> buildNumberOfCurrentMaxTreatmentLevel() {
    return new CurrentMaxTreatmentLevel()
        .getNumberOfCurrentMaxTreatmentLevel(
            icuDiseaseMap, dbData.getFacilityContactEncounters(), treatmentLevel, useIcuUndiff);
  }

  public Set<MiiObservation> buildObservationsByResult() {
    return new CumulativeResult()
        .getObservationsByResult(
            labResult,
            dataItemContext,
            dbData.getObservations(),
            dbData.getInputCodeSettings(),
            dbData.getQualitativeLabCodesSettings());
  }

  public Set<String> buildGenderCountList() {
    return CumulativeGender.getGenderCount(
        dbData.getFacilityContactEncounters(), dbData.getPatients(), gender);
  }

  public Set<String> buildGenderList() {
    return CumulativeGender.getGenderPatientIdList(
        dbData.getFacilityContactEncounters(), dbData.getPatients(), gender);
  }

  public Set<String> buildGenderCountByClass() {
    return CumulativeGenderByClass.getGenderCountByCaseClass(
        dbData.getFacilityContactEncounters(), dbData.getPatients(), gender, treatmentLevel);
  }

  public Set<String> buildGenderPidsByCaseClass() {
    return CumulativeGenderByClass.getGenderPidsByCaseClass(
        dbData.getFacilityContactEncounters(), dbData.getPatients(), gender, treatmentLevel);
  }

  public List<Integer> buildCumMaxtreatmentlevelAgeList() {
    return new CumulativeMaxTreatmentLevelAge()
        .createMaxTreatmentLevelAgeMap(
            mapPositiveEncounterByClass,
            icuDiseaseMap,
            dbData.getPatients(),
            treatmentLevel,
            useIcuUndiff);
  }

  public TimestampedListPair buildTimelineTestsMap() {
    return new TimelineTests()
        .createTimelineTestsMap(
            dataItemContext, dbData.getObservations(), dbData.getInputCodeSettings());
  }

  public TimestampedListPair buildTimelineTestsPositiveMap() {
    return new TimelineTests()
        .createTimelineTestPositiveMap(
            dataItemContext,
            dbData.getObservations(),
            dbData.getInputCodeSettings(),
            dbData.getQualitativeLabCodesSettings());
  }

  public Map<TreatmentLevels, Map<Long, Set<String>>> buildMaxTreatmentTimeline() {
    return TimelineMaxTreatmentLevel.createMaxTreatmentTimeline(
        dataItemContext,
        dbData.getFacilityContactEncounters(),
        dbData.getSupplyContactEncounters(),
        dbData.getIcuProcedures(),
        dbData.getLocations(),
        dbData.getInputCodeSettings(),
        useIcuUndiff);
  }

  public List<Integer> buildAgeDistributionByCaseClass() {
    return CumulativeAge.getAgeDistributionsByCaseClass(
        dbData.getFacilityContactEncounters(), dbData.getPatients(), treatmentLevel);
  }

  public TimestampedListPair buildTimelineDeathMap() {
    return new TimelineDeath()
        .createTimelineDeathMap(dbData.getFacilityContactEncounters(), dataItemContext);
  }

  public List<Long> buildCurrentMaxAgeMap() {
    return new CurrentMaxTreatmentLevelAge(dbData.getPatients(), encounterSubSet)
        .createCurrentMaxAgeMap(mapPositiveEncounterByClass);
  }

  public List<String> buildCumulativeZipCodeList() {
    return CumulativeZipCode.createZipCodeList(
        dbData.getFacilityContactEncounters(),
        encounterSubSet,
        dbData.getPatients(),
        null,
        applyDiseasePositiveFilter);
  }

  public List<String> buildCurrentZipCodeList() {
    return new CurrentZipCode()
        .createZipCodeList(
            dbData.getFacilityContactEncounters(),
            encounterSubSet,
            dbData.getPatients(),
            null,
            applyDiseasePositiveFilter);
  }

  public ChartListItem buildKiraZipCodeList() {
    return new CumulativeZipCode()
        .createKiRaZipCodeList(
            kidsRadarDataItemContext,
            coreCaseDataByGroups,
            encounterSubSet,
            kiraData.getPatients());
  }

  public AggregatedDataItem buildCurrentTreatmentlevel() {
    return new KiraCurrentTreatmentLevel()
        .createCurrentTreatmentLevel(
            kidsRadarDataItemContext,
            coreCaseDataByGroups,
            encounterSubSet,
            kiraData.getIcuProcedures(),
            kiraData.getInputCodeSettings(),
            useIcuUndiff);
  }

  public StackedBarChartsItem<KiraInteger> buildKiraCumulativeDisordersGenderKjp() {
    return new KiraCumulativeDiagsGender().createStackBarChartsKjp(coreCaseDataByGroups);
  }

  public StackedBarChartsItem<Integer> buildKiraCumulativeDisordersGenderPed() {
    return new KiraCumulativeDiagsGender().createStackBarChartsPed(coreCaseDataByGroups);
  }

  public StackedBarCharts buildKiraCumulativeAgeDisorders() {
    return KiraCumulativeAgeDisorders.createStackedBarCharts(
        kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  public StackedBarChartsItem buildKiraLengthOfStay() {
    return KiraLengthOfStay.createStackedBarCharts(kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  // Keep a single instance to benefit from the simple lazy cache
  private final KiraKjpTimelineIntensiveCare kjp = new KiraKjpTimelineIntensiveCare();

  public StackedBarChartsItem<KiraInteger> buildKiraTimelineIntensiveCare() {
    return kjp.createKjpIntensiveCareTimeline(coreCaseDataAll);
  }

  public GroupedBarChartsCalcItem<KiraInteger> buildKiraTimelineIntensiveCareRatio() {
    return kjp.createKjpIntensiveCareRatio(coreCaseDataAll);
  }

  public StackedBarChartsItem<Integer> buildKiraTimelineIntensiveCareChange() {
    return kjp.createKjpIntensiveCareTimelineCoreChange(coreCaseDataAll);
  }

  public GroupedBarChartsCalcItem<KiraInteger> buildKiraTimelineIntensiveCare3Months() {
    return kjp.createKjpIntensiveCare3Months(coreCaseDataAll);
  }

  public StackedBarChartsUniformItem<KiraInteger> buildKiraTimelineDisordersItem(
      KiraTimelineDisorders kiraTimelineDisorders) {
    return kiraTimelineDisorders.createStackedBarChartsUniform(
        kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  public StackedBarChartsItem<KiraInteger> buildTimelineAgeKjp() {
    return new KiraKjpTimelineAge().createKjpAgeTimeline(coreCaseDataByGroups);
  }

  public StackedBarChartsItem<KiraInteger> buildTimelineAdmission() {
    return new KiraKjpTimelineAdmission().createKjpTimelineAdmission(coreCaseDataByGroups);
  }

  public StackedBarChartsUniformItem<KiraInteger> buildTimelineDiagsAdmission() {
    return new KiraKjpTimelineAdmission().createKjpTimelineDiagsAdmission(coreCaseDataByGroups);
  }

  public StackedBarChartsItem buildKiraRsvTimelineDiagsItem(
      KiraTimelineDisorders kiraTimelineDisorders) {
    return kiraTimelineDisorders.createStackBarCharts(
        kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  public KiraTimelineDisorders buildKiraTimelineDisorders() {
    return new KiraTimelineDisorders();
  }

  public Map<String, List<Integer>> buildKiraPedMaxTreatmentlevelTimeline() {
    return new KiraPedTimelineMaxTreatmentLevel()
        .createPediatricTreatmentLevelTimeline(
            dbData.getFacilityContactEncounters(),
            dbData.getIcuProcedures(),
            facilityContactIcuLocationMap,
            dbData.getInputCodeSettings(),
            useIcuUndiff);
  }

  public StackedBarChartsItem<Integer> buildCurrentRecruitment() {
    return new CurrentRecruitment().createStackedBarCharts(consents, questionnaireResponses);
  }

  public Map<String, List<? extends Number>> buildTimelineRecruitmentMap() {
    return new TimelineRecruitment()
        .createTimelineRecruitmentMap(
            new TimelineRecruitment()
                .createTimelineRecruitment(consents, questionnaireResponses, dataItemContext));
  }

  public Map<String, List<Integer>> buildTimelineAgeByIcd(Collection<String> icdCodes) {
    return new KiraPedTimelineAge()
        .createPediatricAgeTimeline(
            dbData.getFacilityContactEncounters(),
            dbData.getConditions(),
            icdCodes,
            coreCaseDataByGroups);
  }

  public Map<String, List<Integer>> buildTimelineAgeCovid() {
    return buildTimelineAgeByIcd(dbData.getInputCodeSettings().getCovidConditionIcdCodes());
  }

  public Map<String, List<Integer>> buildTimelineAgeInfluenza() {
    return buildTimelineAgeByIcd(dbData.getInputCodeSettings().getInfluenzaConditionIcdCodes());
  }

  public Map<String, List<Integer>> buildTimelineAgePertussis() {
    List<String> pertussisIcd =
        dbData.getInputCodeSettings().getKidsRadarConditionPedIcdCodes().get("pertussis");
    return buildTimelineAgeByIcd(pertussisIcd);
  }

  public Map<String, List<Integer>> buildTimelineAgeRsv() {
    Map<String, List<String>> pedIcdCodes =
        dbData.getInputCodeSettings().getKidsRadarConditionPedIcdCodes();

    // Collect all ICD codes from keys that start with "rsv"
    List<String> rsvIcd =
        pedIcdCodes.entrySet().stream()
            .filter(e -> e.getKey() != null && e.getKey().startsWith("rsv"))
            .flatMap(e -> e.getValue().stream())
            .toList();

    return buildTimelineAgeByIcd(rsvIcd);
  }

  public Map<String, List<Integer>> buildTimelineAgePcr(Collection<String> loincCodes) {
    return new KiraPedTimelineAge()
        .createPediatricAgeTimelineByLoinc(
            dbData.getFacilityContactEncounters(),
            dbData.getObservations(),
            loincCodes,
            POSITIVE,
            dbData.getQualitativeLabCodesSettings(),
            coreCaseDataByGroups);
  }

  public Map<String, List<Integer>> buildTimelineAgePcrCovid() {
    return buildTimelineAgePcr(dbData.getInputCodeSettings().getCovidObservationPcrLoincCodes());
  }

  public Map<String, List<Integer>> buildTimelineAgePcrInfluenza() {
    return buildTimelineAgePcr(
        dbData.getInputCodeSettings().getInfluenzaObservationPcrLoincCodes());
  }

  public Map<String, List<Integer>> buildTimelineAgePcrPertussis() {
    return buildTimelineAgePcr(dbData.getInputCodeSettings().getKidsRadarPedPertussisLoincCodes());
  }

  public Map<String, List<Integer>> buildTimelineAgePcrRsv() {
    return buildTimelineAgePcr(dbData.getInputCodeSettings().getKidsRadarPedRsvLoincCodes());
  }

  public StackedBarChartsItem<Integer> buildCurrentConsent() {
    return new CurrentConsent().createStackedBarCharts(consents);
  }

  public Map<String, List<Long>> buildTimelineConsent() {
    return new TimelineConsent().generateTimelineConsent(consents);
  }
}
