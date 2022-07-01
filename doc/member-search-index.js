memberSearchIndex = [{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"addExtensionToEncounterIfSetContainsCaseId(UkbEncounter, Set<UkbEncounter>, Set<String>, Extension)","u":"addExtensionToEncounterIfSetContainsCaseId(de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter,java.util.Set,java.util.Set,org.hl7.fhir.r4.model.Extension)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ALIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"VitalStatus","l":"ALIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"VitalStatus","l":"ALL"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"ambulantStationaryLogic(HashMap<String, Set<UkbEncounter>>, List<UkbEncounter>)","u":"ambulantStationaryLogic(java.util.HashMap,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"BORDERLINE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"BORDERLINE_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"BORDERLINE_RESULT"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"calculateAge(Date, Date)","u":"calculateAge(java.util.Date,java.util.Date)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"calculateDaysInBetweenInHours(LocalDateTime, LocalDateTime)","u":"calculateDaysInBetweenInHours(java.time.LocalDateTime,java.time.LocalDateTime)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASECLASS_INPATIENT"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASECLASS_OUTPATIENT"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CASENRS"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASESTATUS_ALL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASESTATUS_INPATIENT"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASESTATUS_OUTPATIENT"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASETYPE_INTENSIVESTATIONARY"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASETYPE_KONTAKTART_SYSTEM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASETYPE_NORMALSTATIONARY"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASETYPE_PARTSTATIONARY"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASETYPE_POSTSTATIONARY"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CASETYPE_PRESTATIONARY"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"checkAgeGroup(int)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"CITY_BONN"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaDashboardConstants","l":"CoronaDashboardConstants()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.models","c":"CoronaDataItem","l":"CoronaDataItem()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"CoronaLogic()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"CoronaResultFunctionality()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.models","c":"CoronaResults","l":"CoronaResults(List<UkbCondition>, List<UkbObservation>, List<UkbPatient>, List<UkbEncounter>, List<UkbProcedure>, List<UkbLocation>)","u":"%3Cinit%3E(java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.models","c":"CoronaTreatmentLevelExport","l":"CoronaTreatmentLevelExport(List<String>, List<String>, List<String>, List<String>)","u":"%3Cinit%3E(java.util.List,java.util.List,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"COUNTRY_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"COVID_LOINC_CODES"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"COVID_VARIANT_CODES"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"createCumulativeMaxDebug(List<UkbEncounter>, String, HashMap<String, Map<String, List<String>>>)","u":"createCumulativeMaxDebug(java.util.List,java.lang.String,java.util.HashMap)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"createCurrentIcuMap(List<UkbEncounter>, List<UkbLocation>, List<UkbProcedure>)","u":"createCurrentIcuMap(java.util.List,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"createCurrentMaxAgeMap(HashMap<String, List<UkbEncounter>>)","u":"createCurrentMaxAgeMap(java.util.HashMap)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"createCurrentMaxTotalAgeMap(List<Long>, List<Long>, List<Long>, List<Long>)","u":"createCurrentMaxTotalAgeMap(java.util.List,java.util.List,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"createEncounterMap(List<UkbEncounter>)","u":"createEncounterMap(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayIcu","l":"createIcuLengthListByVitalstatus(VitalStatus, Map<String, Map<Long, Set<String>>>, Map<String, List<UkbEncounter>>)","u":"createIcuLengthListByVitalstatus(de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus,java.util.Map,java.util.Map)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayIcu","l":"createIcuLengthOfStayList(Map<String, List<UkbEncounter>>)","u":"createIcuLengthOfStayList(java.util.Map)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"createIcuMap(List<UkbEncounter>, List<UkbLocation>, List<UkbProcedure>)","u":"createIcuMap(java.util.List,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayHospital","l":"createLengthOfStayHospitalByVitalstatus(List<UkbEncounter>, HashMap<String, Map<Long, List<String>>>, String)","u":"createLengthOfStayHospitalByVitalstatus(java.util.List,java.util.HashMap,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayHospital","l":"createMapDaysHospitalList()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel","c":"CumulativeMaxTreatmentLevelAge","l":"createMaxTreatmentLevelAgeMap(Map<String, List<UkbEncounter>>, Map<String, List<UkbEncounter>>, String)","u":"createMaxTreatmentLevelAgeMap(java.util.Map,java.util.Map,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineMaxTreatmentLevel","l":"createMaxTreatmentTimeline()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative","c":"CumulativeResult","l":"createResultMap(Set<UkbObservation>, Set<UkbObservation>, Set<UkbObservation>)","u":"createResultMap(java.util.Set,java.util.Set,java.util.Set)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender","c":"CumulativeGenderByClass","l":"createResultMap(String)","u":"createResultMap(java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineDeath","l":"createTimeLineDeathMap()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineTests","l":"createTimelineTestPositiveMap()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineTests","l":"createTimelineTestsMap()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineVariantTestResults","l":"createTimeLineVariantsTests(VariantSettings)","u":"createTimeLineVariantsTests(de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative","c":"CumulativeVariantTestResults","l":"createVariantTestResultMap(VariantSettings)","u":"createVariantTestResultMap(de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative","c":"CumulativeZipCode","l":"createZipCodeList()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_AGE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_AGE_ALIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_AGE_DEAD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_UNDIFFERENTIATED"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_GENDER"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_INPATIENT_AGE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_INPATIENT_GENDER"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_LENGTHOFSTAY_HOSPITAL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_LENGTHOFSTAY_ICU"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_LENGTHOFSTAY_ICU_DEAD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_LENGTHOFSTAY_ICU_LIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_MAXTREATMENTLEVEL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_OUTPATIENT_AGE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_OUTPATIENT_GENDER"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_RESULTS"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_VARIANTTESTRESULTS"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CUMULATIVE_ZIPCODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age","c":"CumulativeAge","l":"CumulativeAge(List<UkbEncounter>, List<UkbPatient>)","u":"%3Cinit%3E(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender","c":"CumulativeGender","l":"CumulativeGender(List<UkbEncounter>, List<UkbPatient>)","u":"%3Cinit%3E(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender","c":"CumulativeGenderByClass","l":"CumulativeGenderByClass(List<UkbEncounter>, List<UkbPatient>)","u":"%3Cinit%3E(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayHospital","l":"CumulativeLengthOfStayHospital(List<UkbEncounter>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayIcu","l":"CumulativeLengthOfStayIcu(List<UkbEncounter>, List<UkbLocation>)","u":"%3Cinit%3E(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel","c":"CumulativeMaxTreatmentLevel","l":"CumulativeMaxTreatmentLevel(List<UkbEncounter>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel","c":"CumulativeMaxTreatmentLevelAge","l":"CumulativeMaxTreatmentLevelAge(List<UkbPatient>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative","c":"CumulativeResult","l":"CumulativeResult(List<UkbObservation>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative","c":"CumulativeVariantTestResults","l":"CumulativeVariantTestResults(List<UkbObservation>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative","c":"CumulativeZipCode","l":"CumulativeZipCode(List<UkbEncounter>, List<UkbPatient>)","u":"%3Cinit%3E(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_AGE_MAXTREATMENTLEVEL_ICU"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_AGE_MAXTREATMENTLEVEL_ICU_UNDIFFERENTIATED"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_MAXTREATMENTLEVEL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_TREATMENTLEVEL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"CURRENT_TREATMENTLEVEL_CROSSTAB"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current","c":"CurrentMaxTreatmentLevel","l":"CurrentMaxTreatmentLevel(List<UkbEncounter>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"CurrentMaxTreatmentLevelAge(List<UkbPatient>, List<UkbEncounter>)","u":"%3Cinit%3E(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current","c":"CurrentTreatmentLevel","l":"CurrentTreatmentLevel(List<UkbEncounter>, List<UkbProcedure>)","u":"%3Cinit%3E(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"DataItems()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItemTypes","l":"DataItemTypes()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DATE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaDashboardConstants","l":"dayInSeconds"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaDashboardConstants","l":"daysAfterOutpatientStay"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DEAD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"VitalStatus","l":"DEAD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DEATH_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAG_RELIABILITY_A"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAG_RELIABILITY_G"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAG_RELIABILITY_MISSING"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAG_RELIABILITY_V"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAG_RELIABILITY_Z"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAGNOSIS_SECURITY_BORDERLINE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAGNOSIS_SECURITY_BORDERLINE_ENUM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAGNOSIS_SECURITY_NEGATIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIAGNOSIS_SECURITY_NEGATIVE_ENUM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DISCHARGE_DISPOSITION_EXT_URL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_EXT_URL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_SYSTEM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"DIVERSE_SPECIFICATION"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineFunctionalities","l":"divideMapValuesToLists(Map<Long, Long>, List<Long>, List<Long>)","u":"divideMapValuesToLists(java.util.Map,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ECMO_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"equals(Object)","u":"equals(java.lang.Object)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"extractIdFromReference(Reference)","u":"extractIdFromReference(org.hl7.fhir.r4.model.Reference)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"FEMALE_SPECIFICATION"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"flagCases(List<UkbEncounter>, List<UkbCondition>, List<UkbObservation>)","u":"flagCases(java.util.List,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"flagEncounter(List<UkbEncounter>, Set<String>, HashMap<String, Set<String>>, HashMap<String, Set<String>>)","u":"flagEncounter(java.util.List,java.util.Set,java.util.HashMap,java.util.HashMap)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"flagEncounterByPositiveLabResult(List<UkbEncounter>, Set<String>)","u":"flagEncounterByPositiveLabResult(java.util.List,java.util.Set)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"GENDER_DIVERSE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"GENDER_FEMALE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"GENDER_MALE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"GENDER_UNKNOWN"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"generateCrosstabList(LinkedHashMap<CoronaFixedValues, List<UkbEncounter>>, List<UkbPatient>)","u":"generateCrosstabList(java.util.LinkedHashMap,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"generateCurrentTreatmentLevelList(HashMap<String, List<String>>, String, String)","u":"generateCurrentTreatmentLevelList(java.util.HashMap,java.lang.String,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age","c":"CumulativeAge","l":"getAgeCountByVitalStatus(VitalStatus, HashMap<String, List<UkbEncounter>>)","u":"getAgeCountByVitalStatus(de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus,java.util.HashMap)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age","c":"CumulativeAge","l":"getAgeDistributionsByCaseClass(String)","u":"getAgeDistributionsByCaseClass(java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"getCaseIdsByDiagReliability(HashMap<String, Set<String>>, String)","u":"getCaseIdsByDiagReliability(java.util.HashMap,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"getCaseIdsByDiagReliability(List<UkbCondition>, String)","u":"getCaseIdsByDiagReliability(java.util.List,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"getCaseIdsWithPositiveLabObs(List<UkbObservation>)","u":"getCaseIdsWithPositiveLabObs(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel","c":"CumulativeMaxTreatmentLevel","l":"getCumulativeByClass(Map<String, List<UkbEncounter>>, CoronaFixedValues, HashMap<String, List<UkbEncounter>>)","u":"getCumulativeByClass(java.util.Map,de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues,java.util.HashMap)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel","c":"CumulativeMaxTreatmentLevel","l":"getCumulativeByIcuLevel(Map<String, List<UkbEncounter>>, CoronaFixedValues)","u":"getCumulativeByIcuLevel(java.util.Map,de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current","c":"CurrentTreatmentLevel","l":"getCurrentEncounterByIcuLevel(Map<String, List<UkbEncounter>>, CoronaFixedValues)","u":"getCurrentEncounterByIcuLevel(java.util.Map,de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current","c":"CurrentTreatmentLevel","l":"getCurrentStandardWardEncounter(Map<String, List<UkbEncounter>>)","u":"getCurrentStandardWardEncounter(java.util.Map)"},{"p":"de.ukbonn.mwtek.dashboardlogic.models","c":"CoronaResults","l":"getDataItems(Map<String, Boolean>, Boolean, VariantSettings)","u":"getDataItems(java.util.Map,java.lang.Boolean,de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender","c":"CumulativeGender","l":"getGenderCount(List<UkbEncounter>, List<UkbPatient>, String)","u":"getGenderCount(java.util.List,java.util.List,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender","c":"CumulativeGenderByClass","l":"getGenderCountByCaseClass(String, String)","u":"getGenderCountByCaseClass(java.lang.String,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.models","c":"CoronaResults","l":"getMapCurrentTreatmentlevelCasenrs()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current","c":"CurrentMaxTreatmentLevel","l":"getNumberOfCurrentMaxTreatmentLevel(Map<String, List<UkbEncounter>>, CoronaFixedValues)","u":"getNumberOfCurrentMaxTreatmentLevel(java.util.Map,de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative","c":"CumulativeResult","l":"getObservationsByResult(CoronaFixedValues)","u":"getObservationsByResult(de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"getObservationsByResult(List<UkbObservation>, CoronaFixedValues)","u":"getObservationsByResult(java.util.List,de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"getPidsPosFinding(List<UkbObservation>, List<UkbCondition>)","u":"getPidsPosFinding(java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"getPidsWithCovidDiagnosis(List<UkbCondition>)","u":"getPidsWithCovidDiagnosis(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"getPidsWithPosCovidLabResult(List<UkbObservation>)","u":"getPidsWithPosCovidLabResult(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"getValue()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"hashCode()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ICD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ICD_DIAG_RELIABILITY_CODING_SYSTEM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ICD_DIAG_RELIABILITY_EXT_URL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ICD_SYSTEM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ICU"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ICU_ECMO"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"ICU_VENTILATION"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"incrementVariantCount(Map<String, Integer>, String)","u":"incrementVariantCount(java.util.Map,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.tools","c":"StringHelper","l":"isAnyMatchSetWithString(Set<String>, String)","u":"isAnyMatchSetWithString(java.util.Set,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"isCaseClassInpatient(UkbEncounter)","u":"isCaseClassInpatient(de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"isCaseClassOutpatient(UkbEncounter)","u":"isCaseClassOutpatient(de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"isCaseTypePrestationary(List<CodeableConcept>)","u":"isCaseTypePrestationary(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"isCodingValid(CodeableConcept, String, List<String>)","u":"isCodingValid(org.hl7.fhir.r4.model.CodeableConcept,java.lang.String,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.tools","c":"LocationFilter","l":"isLocationIcu(UkbLocation)","u":"isLocationIcu(de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation)"},{"p":"de.ukbonn.mwtek.dashboardlogic.tools","c":"LocationFilter","l":"isLocationWard(UkbLocation)","u":"isLocationWard(de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"isPatientDeceased(UkbEncounter)","u":"isPatientDeceased(de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItemTypes","l":"ITEMTYPE_AGGREGATED"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItemTypes","l":"ITEMTYPE_DEBUG"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItemTypes","l":"ITEMTYPE_LIST"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"listCurrentMaxEncounter()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender","c":"CumulativeGenderByClass","l":"listEncounters"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayIcu","l":"listEncounters"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel","c":"CumulativeMaxTreatmentLevel","l":"listEncounters"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current","c":"CurrentMaxTreatmentLevel","l":"listEncounters"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineDeath","l":"listEncounters"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineMaxTreatmentLevel","l":"listEncounters"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineMaxTreatmentLevel","l":"listIcuProcedures"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineTests","l":"listLabObservations"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay","c":"CumulativeLengthOfStayIcu","l":"listLocation"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineMaxTreatmentLevel","l":"listLocations"},{"p":"de.ukbonn.mwtek.dashboardlogic.tools","c":"ListNumberPair","l":"ListNumberPair()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender","c":"CumulativeGenderByClass","l":"listPatients"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel","c":"CumulativeMaxTreatmentLevelAge","l":"listPatients"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"listPatients()"},{"p":"de.ukbonn.mwtek.dashboardlogic.tools","c":"LocationFilter","l":"LocationFilter()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"LOINC_SYSTEM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"MALE_SPECIFICATION"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"markBorderline()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"markNegative()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaLogic","l":"markPositive()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"NEGATIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"NEGATIVE_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"NEGATIVE_RESULT"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"NORMAL_WARD"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"OUTPATIENT_ITEM"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"POSITIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"POSITIVE_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"POSITIVE_RESULT"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaDashboardConstants","l":"qualifyingDate"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic","c":"CoronaResultFunctionality","l":"sortingFirstAdmissionDateToPid(UkbEncounter, Map<String, UkbEncounter>)","u":"sortingFirstAdmissionDateToPid(de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter,java.util.Map)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineFunctionalities","l":"splitReference(String, String)","u":"splitReference(java.lang.String,java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"STATIONARY_ITEM"},{"p":"de.ukbonn.mwtek.dashboardlogic.tools","c":"StringHelper","l":"StringHelper()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItemTypes","l":"SUBITEMTYPE_DATE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"TIMELINE_DEATHS"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"TIMELINE_MAXTREATMENTLEVEL"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"TIMELINE_TESTS"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"TIMELINE_TESTS_POSITIVE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"DataItems","l":"TIMELINE_VARIANTTESTRESULTS"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineDeath","l":"TimelineDeath(List<UkbEncounter>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineFunctionalities","l":"TimelineFunctionalities()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineMaxTreatmentLevel","l":"TimelineMaxTreatmentLevel(List<UkbEncounter>, List<UkbProcedure>, List<UkbLocation>)","u":"%3Cinit%3E(java.util.List,java.util.List,java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineTests","l":"TimelineTests(List<UkbObservation>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.timeline","c":"TimelineVariantTestResults","l":"TimelineVariantTestResults(List<UkbObservation>)","u":"%3Cinit%3E(java.util.List)"},{"p":"de.ukbonn.mwtek.dashboardlogic.models","c":"CoronaTreatmentLevelExport","l":"toCsv()"},{"p":"de.ukbonn.mwtek.dashboardlogic.logic.current.age","c":"CurrentMaxTreatmentLevelAge","l":"toString()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"TWELVE_DAYS_LOGIC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"U071"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"U072"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"valueOf(String)","u":"valueOf(java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"VitalStatus","l":"valueOf(String)","u":"valueOf(java.lang.String)"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"values()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"VitalStatus","l":"values()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_ALPHA"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_ALPHA_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_ALPHA_LOINC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_BETA"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_BETA_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_BETA_LOINC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_DELTA"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_DELTA_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_DELTA_LOINC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_GAMMA"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_GAMMA_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_GAMMA_LOINC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_NON_VOC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_OMICRON"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_OMICRON_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_OMICRON_LOINC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_OTHER_VOC"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VARIANT_UNKNOWN"},{"p":"de.ukbonn.mwtek.dashboardlogic.settings","c":"VariantSettings","l":"VariantSettings()","u":"%3Cinit%3E()"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VENT_CODE"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"VENT_CODE2"},{"p":"de.ukbonn.mwtek.dashboardlogic.enums","c":"CoronaFixedValues","l":"WARD"}];updateSearchResults();