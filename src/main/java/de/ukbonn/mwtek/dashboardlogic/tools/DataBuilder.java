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

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.Gender;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardData;
import de.ukbonn.mwtek.dashboardlogic.logic.KiraData;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CumulativeZipCode;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age.CumulativeAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age.KiraCumulativeAgeDisorders;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGender;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.CumulativeGenderByClass;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender.KiraCumulativeDisordersGender;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.KiraLengthOfStay;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel.CumulativeMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.results.CumulativeResult;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentRecruitment;
import de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.current.age.CurrentMaxTreatmentLevelAge;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.KiraTimelineDisorders;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineDeath;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineMaxTreatmentLevel;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineRecruitment;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineTests;
import de.ukbonn.mwtek.dashboardlogic.models.ChartListItem;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListTriple;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true, chain = true)
public class DataBuilder {
  private Map<TreatmentLevels, List<UkbEncounter>> icuDiseaseMap;
  private TreatmentLevels treatmentLevel;
  private Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass;
  private Map<TreatmentLevels, List<UkbEncounter>> mapCurrentIcuPositive;
  private List<UkbEncounter> icuSupplyContactEncounters;
  private List<UkbEncounter> encounterSubSet;
  private DashboardData dbData;

  private List<UkbEncounter> currentStandardWardEncounters;
  private List<UkbEncounter> currentIcuEncounters;
  private List<UkbEncounter> currentVentEncounters;
  private List<UkbEncounter> currentEcmoEncounters;
  private List<UkbEncounter> currentIcuUndiffEncounters;

  private List<UkbConsent> consents;

  private DashboardLogicFixedValues labResult;
  private DataItemContext dataItemContext;
  private Gender gender;

  private KidsRadarDataItemContext kidsRadarDataItemContext;
  private KiraData kiraData;
  private Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups;

  private Boolean useIcuUndiff;

  public List<UkbEncounter> buildCumulativeByClass() {
    return new CumulativeMaxTreatmentLevel()
        .getCumulativeByClass(icuDiseaseMap, treatmentLevel, mapPositiveEncounterByClass);
  }

  public List<UkbEncounter> buildCumulativeByIcuLevel() {
    return new CumulativeMaxTreatmentLevel().getCumulativeByIcuLevel(icuDiseaseMap, treatmentLevel);
  }

  public List<UkbEncounter> buildCurrentEncounterByIcuLevel() {
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

  public List<UkbEncounter> buildNumberOfCurrentMaxTreatmentLevel() {
    return new CurrentMaxTreatmentLevel()
        .getNumberOfCurrentMaxTreatmentLevel(
            icuDiseaseMap, dbData.getFacilityContactEncounters(), treatmentLevel, useIcuUndiff);
  }

  public Set<UkbObservation> buildObservationsByResult() {
    return new CumulativeResult()
        .getObservationsByResult(
            labResult,
            dataItemContext,
            dbData.getObservations(),
            dbData.getInputCodeSettings(),
            dbData.getQualitativeLabCodesSettings());
  }

  public Number buildGenderCount() {
    return CumulativeGender.getGenderCount(
        dbData.getFacilityContactEncounters(), dbData.getPatients(), gender);
  }

  public Set<String> buildGenderList() {
    return CumulativeGender.getGenderPatientIdList(
        dbData.getFacilityContactEncounters(), dbData.getPatients(), gender);
  }

  public Number buildGenderCountByClass() {
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

  public List<String> buildZipCodeList() {
    return new CumulativeZipCode()
        .createZipCodeList(
            dbData.getFacilityContactEncounters(), encounterSubSet, dbData.getPatients(), null);
  }

  public ChartListItem buildKiraZipCodeList() {
    return new CumulativeZipCode()
        .createKiRaZipCodeList(
            kidsRadarDataItemContext,
            coreCaseDataByGroups,
            encounterSubSet,
            kiraData.getPatients());
  }

  public StackedBarChartsItem buildKiraCumulativeDisordersGender() {
    return new KiraCumulativeDisordersGender()
        .createStackBarCharts(kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  public StackedBarChartsItem buildKiraCumulativeAgeDisorders() {
    return KiraCumulativeAgeDisorders.createStackedBarCharts(
        kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  public StackedBarChartsItem buildKiraLengthOfStay() {
    return KiraLengthOfStay.createStackedBarCharts(kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  public StackedBarChartsItem buildKiraTimelineDisordersItem(
      KiraTimelineDisorders kiraTimelineDisorders) {
    return kiraTimelineDisorders.createStackedBarCharts(
        kidsRadarDataItemContext, coreCaseDataByGroups);
  }

  public KiraTimelineDisorders buildKiraTimelineDisorders() {
    return new KiraTimelineDisorders();
  }

  public StackedBarChartsItem buildCurrentRecruitment() {
    return new CurrentRecruitment().createStackedBarCharts(consents);
  }

  public TimestampedListTriple buildTimelineRecruitment() {
    return new TimelineRecruitment().createTimelineRecruitment(consents, dataItemContext);
  }

  public Map<String, List<? extends Number>> buildTimelineRecruitmentMap() {
    return new TimelineRecruitment()
        .createTimelineRecruitmentMap(
            new TimelineRecruitment().createTimelineRecruitment(consents, dataItemContext));
  }
}
