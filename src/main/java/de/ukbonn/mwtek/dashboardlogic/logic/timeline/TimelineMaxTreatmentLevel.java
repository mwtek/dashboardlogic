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
package de.ukbonn.mwtek.dashboardlogic.logic.timeline;

import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.DAY_IN_SECONDS;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCoding;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Period;

/**
 * This class is used for generating the data item {@link DiseaseDataItem
 * timeline.maxtreatmentlevel}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class TimelineMaxTreatmentLevel extends DashboardDataItemLogic
    implements TimelineFunctionalities {

  /**
   * Creates a map containing all maximal treatment of cases for each day, since the qualifying date
   *
   * @return Map that assigns the cases per day to a treatment level and also contains a map with
   *     the case ids per date
   */
  public static Map<TreatmentLevels, Map<Long, Set<String>>> createMaxTreatmentTimeline(
      DataItemContext dataItemContext,
      List<UkbEncounter> facilityEncounters,
      List<UkbEncounter> supplyContactEncounters,
      List<UkbProcedure> icuProcedures,
      List<UkbLocation> locations,
      InputCodeSettings inputCodeSettings) {
    log.debug("started createMaxTreatmentTimeline");
    Instant startTimer = TimerTools.startTimer();

    // Map containing Lists with number of cases, sorted by their treatmentlevel, and a set of
    // caseNrs for each Day
    Map<TreatmentLevels, Map<Long, Set<String>>> resultWithCaseNrsMap = new LinkedHashMap<>();
    // Map containing a set of case Ids for each day
    Map<Long, Set<String>> mapAmbulantCaseNr = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapNormalWardCaseNrs =
        Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuCaseNrs = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuVentCaseNrs = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuEcmoCaseNrs = Collections.synchronizedMap(new LinkedHashMap<>());
    // used to save the maxtreatmentlevel up to the checked date
    Map<TreatmentLevels, Set<String>> mapPrevMaxtreatmentlevel = new ConcurrentHashMap<>();

    mapPrevMaxtreatmentlevel.put(OUTPATIENT, ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(NORMAL_WARD, ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(ICU, ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(ICU_VENTILATION, ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(ICU_ECMO, ConcurrentHashMap.newKeySet());

    // Determination of the location IDs of all intensive care units. Only the wards are
    // considered, since in the location components within an Encounter resource, at best
    // ward/room and bed are listed with identical time periods, and the stay should only be
    // evaluated once. The highest of these hierarchy levels should be sufficient.
    Set<String> icuLocationIds = LocationFilter.getIcuLocationIds(locations);

    // Since kds case module profile 2024 the pre-stationary and post-stationary will now be
    // handled as outpatient cases with Encounter.class = AMB
    Set<UkbEncounter> positiveSupplyContactEncounters =
        supplyContactEncounters.parallelStream()
            .filter(EncounterFilter::isDiseasePositive)
            .filter(UkbEncounter::isCaseClassInpatient)
            .collect(Collectors.toSet());

    List<UkbEncounter> positiveOutpatientEncounters =
        facilityEncounters.parallelStream()
            .filter(UkbEncounter::isCaseClassOutpatient)
            .filter(EncounterFilter::isDiseasePositive)
            .toList();

    positiveSupplyContactEncounters.addAll(positiveOutpatientEncounters);

    long currentDate = getKickOffDateInSeconds(dataItemContext);
    long currentDayUnix = DateTools.getCurrentUnixTime();

    while (currentDate <= currentDayUnix) {
      long checkDate = currentDate;
      mapAmbulantCaseNr.put(checkDate, ConcurrentHashMap.newKeySet());
      mapNormalWardCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
      mapIcuCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
      mapIcuVentCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
      mapIcuEcmoCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());

      Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
      // Pre-filtering the encounters to ones that can have intersection with the date that is
      // currently checked
      try {
        positiveSupplyContactEncounters.parallelStream()
            .filter(
                x ->
                    DateTools.dateToUnixTime(x.getPeriod().getStart()) - DAY_IN_SECONDS < checkDate)
            .filter(
                x ->
                    DateTools.dateToUnixTime(
                                x.getPeriod().getEnd() == null
                                    ? DateTools.getCurrentDateTime()
                                    : x.getPeriod().getEnd())
                            + DAY_IN_SECONDS
                        >= checkDate)
            .forEach(
                supplyContactEncounter -> {
                  //              String encounterId = supplyContactEncounter.getId();
                  String patientId = supplyContactEncounter.getPatientId();
                  // Since a case should just count once a day and the procedure usually references
                  // the facility contact, we need to look via top level resource
                  String facilityContactId = supplyContactEncounter.getFacilityContactId();

                  long caseStartUnix =
                      DateTools.dateToUnixTime(supplyContactEncounter.getPeriod().getStart());

                  // Prevent multiple cases of a patient from being processed at the same time
                  ReentrantLock lock = locks.computeIfAbsent(patientId, k -> new ReentrantLock());
                  lock.lock();
                  try {
                    boolean isNormalWard =
                        mapPrevMaxtreatmentlevel.get(NORMAL_WARD).contains(patientId);
                    boolean isIcu = mapPrevMaxtreatmentlevel.get(ICU).contains(patientId);
                    boolean isVent =
                        mapPrevMaxtreatmentlevel.get(ICU_VENTILATION).contains(patientId);
                    boolean isEcmo = mapPrevMaxtreatmentlevel.get(ICU_ECMO).contains(patientId);
                    // if the case is ambulant, check the pids of the previous maxtreatmentlevels
                    // and if none are similar with the current case than note the case as ambulant
                    if (supplyContactEncounter.isCaseClassOutpatient()) {

                      List<String> listMaxStatPidCheck = new ArrayList<>();
                      List<String> listMaxIcuPidCheck = new ArrayList<>();
                      List<String> listMaxIcuVentPidCheck = new ArrayList<>();
                      List<String> listMaxIcuEcmoPidCheck = new ArrayList<>();

                      // Normal ward
                      if (isNormalWard) {
                        listMaxStatPidCheck.add(patientId);
                      }
                      // ICU
                      if (isIcu) {
                        listMaxIcuPidCheck.add(patientId);
                      }
                      // Ventilation
                      if (isVent) {
                        listMaxIcuVentPidCheck.add(patientId);
                      }
                      // ECMO
                      if (isEcmo) {
                        listMaxIcuEcmoPidCheck.add(patientId);
                      }

                      // list containing every supplyContactEncounter with a currently higher
                      // treatmentlevel than 'outpatient'.
                      Set<String> listHigherTreatment = new LinkedHashSet<>();
                      listHigherTreatment.addAll(listMaxStatPidCheck);
                      listHigherTreatment.addAll(listMaxIcuPidCheck);
                      listHigherTreatment.addAll(listMaxIcuVentPidCheck);
                      listHigherTreatment.addAll(listMaxIcuEcmoPidCheck);

                      if (listHigherTreatment.isEmpty()) {
                        if (caseStartUnix >= checkDate
                            && caseStartUnix <= (checkDate + DAY_IN_SECONDS)) {
                          mapAmbulantCaseNr.get(checkDate).add(facilityContactId);
                          mapPrevMaxtreatmentlevel.get(OUTPATIENT).add(patientId);
                        }
                      }
                    }
                    // At this stage it is clear that it is a stationary supplyContactEncounter
                    else if (supplyContactEncounter.isCaseClassInpatient()) {
                      if (!supplyContactEncounter.getPeriod().hasEnd()) {
                        supplyContactEncounter.getPeriod().setEnd(DateTools.getCurrentDateTime());
                      }
                      Date endDate = supplyContactEncounter.getPeriod().getEnd();
                      if (endDate == null) {
                        throw new IllegalStateException(
                            "Encounter.period.getEnd is NULL even after setting a value! Encounter"
                                + " ID: "
                                + supplyContactEncounter.getId());
                      }
                      long caseEndUnix = DateTools.dateToUnixTime(endDate);
                      // The admission date needs to be before the start date and the discharge date
                      // needs to be after the date that is going to be checked to get the whole
                      // time span
                      if (caseStartUnix < checkDate && caseEndUnix >= checkDate) {
                        List<EncounterLocationComponent> listEncounterHasIcuLocation =
                            supplyContactEncounter.getLocation().stream()
                                .filter(DiseaseResultFunctionality::isLocationReferenceExisting)
                                .filter(
                                    location ->
                                        icuLocationIds.contains(
                                            extractIdFromReference(location.getLocation())))
                                .toList();
                        // if there was no icu related treatmentlevel for this case before
                        if (!isIcu && !isVent && !isEcmo) {
                          // if the case contains no icu locations
                          if (listEncounterHasIcuLocation.isEmpty()) {
                            mapNormalWardCaseNrs.get(checkDate).add(facilityContactId);
                            mapPrevMaxtreatmentlevel.get(NORMAL_WARD).add(patientId);
                          } else {
                            // check if there are any icu procedures currently going on
                            List<UkbProcedure> listEncounterIcuProcedure =
                                icuProcedures.stream()
                                    .filter(icu -> facilityContactId.equals(icu.getCaseId()))
                                    .toList();
                            // if there is no ICU Procedure, check if it is currently considered
                            // as ICU or stationary
                            if (listEncounterIcuProcedure.isEmpty()) {
                              encounterStationTypeCheckProcess(
                                  listEncounterHasIcuLocation,
                                  supplyContactEncounter,
                                  checkDate,
                                  mapPrevMaxtreatmentlevel,
                                  mapNormalWardCaseNrs,
                                  mapIcuCaseNrs);
                            } else {
                              // check and sort if it is a ventilation or ecmo case
                              sortToVentOrEcmoTimeline(
                                  supplyContactEncounter,
                                  listEncounterIcuProcedure,
                                  listEncounterHasIcuLocation,
                                  false,
                                  false,
                                  checkDate,
                                  mapNormalWardCaseNrs,
                                  mapIcuCaseNrs,
                                  mapIcuVentCaseNrs,
                                  mapIcuEcmoCaseNrs,
                                  mapPrevMaxtreatmentlevel,
                                  inputCodeSettings);
                            }
                          }
                        }
                        // if an ICU case was found
                        else if (isIcu && !isVent && !isEcmo) {
                          List<UkbProcedure> listEncounterIcuProcedure =
                              icuProcedures.stream()
                                  .filter(icu -> facilityContactId.equals(icu.getCaseId()))
                                  .collect(Collectors.toList());

                          if (listEncounterIcuProcedure.isEmpty()) {
                            mapIcuCaseNrs.get(checkDate).add(facilityContactId);
                            mapPrevMaxtreatmentlevel.get(ICU).add(patientId);
                          } else {
                            sortToVentOrEcmoTimeline(
                                supplyContactEncounter,
                                listEncounterIcuProcedure,
                                listEncounterHasIcuLocation,
                                false,
                                false,
                                checkDate,
                                mapNormalWardCaseNrs,
                                mapIcuCaseNrs,
                                mapIcuVentCaseNrs,
                                mapIcuEcmoCaseNrs,
                                mapPrevMaxtreatmentlevel,
                                inputCodeSettings);
                          }
                        }
                        // if a ventilation case was found
                        else if (isVent && !isEcmo) {
                          List<UkbProcedure> listEncounterIcuProcedure =
                              icuProcedures.stream()
                                  .filter(icu -> facilityContactId.equals(icu.getCaseId()))
                                  .toList();
                          sortToVentOrEcmoTimeline(
                              supplyContactEncounter,
                              listEncounterIcuProcedure,
                              listEncounterHasIcuLocation,
                              true,
                              false,
                              checkDate,
                              mapNormalWardCaseNrs,
                              mapIcuCaseNrs,
                              mapIcuVentCaseNrs,
                              mapIcuEcmoCaseNrs,
                              mapPrevMaxtreatmentlevel,
                              inputCodeSettings);
                        }
                        // if an ecmo case was found
                        else {
                          mapIcuEcmoCaseNrs.get(checkDate).add(facilityContactId);
                          mapPrevMaxtreatmentlevel.get(ICU_ECMO).add(patientId);
                        }
                      } // if date check
                    } // else if Stationary check
                  } // for loop of positive supplyContactEncounter
                  finally {
                    lock.unlock();
                  }
                });
      } catch (Exception ex) {
        log.error("Creation of the max treatmentlevel timeline failed.", ex);
      }
      currentDate += DAY_IN_SECONDS;
    } // while
    resultWithCaseNrsMap.put(OUTPATIENT, mapAmbulantCaseNr);
    resultWithCaseNrsMap.put(NORMAL_WARD, mapNormalWardCaseNrs);
    resultWithCaseNrsMap.put(ICU, mapIcuCaseNrs);
    resultWithCaseNrsMap.put(ICU_VENTILATION, mapIcuVentCaseNrs);
    resultWithCaseNrsMap.put(ICU_ECMO, mapIcuEcmoCaseNrs);

    TimerTools.stopTimerAndLog(startTimer, "finished createMaxTreatmentTimeline");
    return resultWithCaseNrsMap;
  }

  /**
   * Adds the current {@link UkbEncounter} to a list if he has had artificial respiration or ECMO
   *
   * @param listEncounterIcuProcedures The {@link UkbProcedure} resources, which include information
   *     about ECMO / artificial ventilation periods.
   * @param listEncounterHasIcuLocation List containing all icu locations of an encounter.
   * @param isVent Check if the encounter has any ventilation-related resources attached to it.
   * @param isEcmo Check if the encounter has any ecmo related resources attached to it.
   * @param checkedDate The date [unix time] that is going to be checked.
   * @param mapNormalWardCaseNrs A map that contains the case ids of every stationary case, sorted
   *     by the date.
   * @param mapIcuCaseNrs A map that contains the case ids of every icu case, sorted by the date.
   * @param mapIcuVentCaseNrs A map that contains the case ids of every ventilation case, sorted by
   *     the date.
   * @param mapIcuEcmoCaseNrs A map that contains the case ids of every ecmo case, sorted by the
   *     date.
   * @param mapPrevMaxtreatmentlevel A map with the previous maxtreatmentlevel of an encounter.
   */
  private static void sortToVentOrEcmoTimeline(
      UkbEncounter supplyContactEncounter,
      List<UkbProcedure> listEncounterIcuProcedures,
      List<EncounterLocationComponent> listEncounterHasIcuLocation,
      Boolean isVent,
      Boolean isEcmo,
      Long checkedDate,
      Map<Long, Set<String>> mapNormalWardCaseNrs,
      Map<Long, Set<String>> mapIcuCaseNrs,
      Map<Long, Set<String>> mapIcuVentCaseNrs,
      Map<Long, Set<String>> mapIcuEcmoCaseNrs,
      Map<TreatmentLevels, Set<String>> mapPrevMaxtreatmentlevel,
      InputCodeSettings inputCodeSettings) {
    for (UkbProcedure procedure : listEncounterIcuProcedures) {
      try {
        Period procedurePeriod = procedure.getPerformedPeriod();
        if (!procedurePeriod.hasEnd()) {
          procedurePeriod.setEnd(DateTools.getCurrentDateTime());
        }
        long procedureStartUnix = DateTools.dateToUnixTime(procedurePeriod.getStart());
        long procedureEndUnix = DateTools.dateToUnixTime(procedurePeriod.getEnd());
        if (procedure.hasCode() && procedure.getCode().hasCoding()) {
          String procedureCode = getCodeOfFirstCoding(procedure.getCode().getCoding());
          // check if the procedure fits into the checked time span
          if (procedureStartUnix < checkedDate && procedureEndUnix >= checkedDate) {
            isVent =
                isVent(
                    mapPrevMaxtreatmentlevel,
                    mapIcuVentCaseNrs,
                    procedureCode,
                    supplyContactEncounter.getFacilityContactId(),
                    supplyContactEncounter.getPatientId(),
                    checkedDate,
                    inputCodeSettings);
            isEcmo =
                isEcmo(
                    mapPrevMaxtreatmentlevel,
                    mapIcuEcmoCaseNrs,
                    procedureCode,
                    supplyContactEncounter.getFacilityContactId(),
                    supplyContactEncounter.getPatientId(),
                    checkedDate,
                    inputCodeSettings);
          }
          // check if the end date of the procedure fits into the checked time span
          else if (procedureEndUnix < checkedDate
              && procedureEndUnix >= (checkedDate - DAY_IN_SECONDS)) {
            long difference = checkedDate - procedureEndUnix;
            if (difference < DAY_IN_SECONDS) {
              isVent =
                  isVent(
                      mapPrevMaxtreatmentlevel,
                      mapIcuVentCaseNrs,
                      procedureCode,
                      supplyContactEncounter.getFacilityContactId(),
                      supplyContactEncounter.getPatientId(),
                      checkedDate,
                      inputCodeSettings);
              isEcmo =
                  isEcmo(
                      mapPrevMaxtreatmentlevel,
                      mapIcuEcmoCaseNrs,
                      procedureCode,
                      supplyContactEncounter.getFacilityContactId(),
                      supplyContactEncounter.getPatientId(),
                      checkedDate,
                      inputCodeSettings);
            }
          }
        }
      } catch (Exception ex) {
        log.debug(
            "Unable to retrieve the performedPeriod for Procedure: {} [{}]",
            procedure.getId(),
            ex.getMessage());
      }
    }
    // check if the case was added, into the map containing the previous maxtreatmentlevels
    if (!isVent && !isEcmo) {
      // if neither ecmo nor vent was found, then check if case is icu or stationary again
      encounterStationTypeCheckProcess(
          listEncounterHasIcuLocation,
          supplyContactEncounter,
          checkedDate,
          mapPrevMaxtreatmentlevel,
          mapNormalWardCaseNrs,
          mapIcuCaseNrs);
    } else if (!isEcmo) {
      // purpose is to make sure that the maxtreatmentlevels of the case is still marked correctly,
      // even though the time check does not apply anymore.
      mapIcuVentCaseNrs.get(checkedDate).add(supplyContactEncounter.getFacilityContactId());
      mapPrevMaxtreatmentlevel.get(ICU_VENTILATION).add(supplyContactEncounter.getPatientId());
    }
  }

  /**
   * This process is used to determine, whether an supplyContactEncounter, would be seen as an ICU
   * or as a stationary supplyContactEncounter
   *
   * @param encountersWithIcuLocation Icu locations of an supplyContactEncounter
   * @param checkedDate The current date that is being checked
   * @param mapPrevMaxtreatmentlevel Map containing all previous maxtreatmentlevels up to the
   *     checked date
   * @param mapStationaryCaseNrs Map containing all case ids of stationary cases, sorted by the date
   * @param mapIcuCaseNrs Map containing all case ids of ICU cases, sorted by the date
   */
  private static void encounterStationTypeCheckProcess(
      List<Encounter.EncounterLocationComponent> encountersWithIcuLocation,
      UkbEncounter supplyContactEncounter,
      Long checkedDate,
      Map<TreatmentLevels, Set<String>> mapPrevMaxtreatmentlevel,
      Map<Long, Set<String>> mapStationaryCaseNrs,
      Map<Long, Set<String>> mapIcuCaseNrs) {
    boolean isInIcu = false;

    // iterate through the icu locations
    for (Encounter.EncounterLocationComponent location : encountersWithIcuLocation) {
      Period period = getValidPeriod(location, supplyContactEncounter);

      // If there is no valid start date, skip this location
      if (period == null || !period.hasStart()) {
        log.warn("No valid period start found for Encounter: {}", supplyContactEncounter.getId());
        continue;
      }
      // Ensure period.end is set (fallback to current date if missing)
      if (!period.hasEnd()) {
        period.setEnd(DateTools.getCurrentDateTime());
      }

      long locationDateStartUnix = DateTools.dateToUnixTime(period.getStart());
      long locationDateEndUnix = DateTools.dateToUnixTime(period.getEnd());
      // check if checkedDate fits into the time span of the icu location
      if (locationDateStartUnix < checkedDate && locationDateEndUnix >= checkedDate) {
        isInIcu = true;
        break;
      }
    }
    // If not currently in ICU, assign to normal ward
    if (isInIcu) {
      mapIcuCaseNrs
          .computeIfAbsent(checkedDate, k -> new HashSet<>())
          .add(supplyContactEncounter.getFacilityContactId());
      mapPrevMaxtreatmentlevel
          .computeIfAbsent(TreatmentLevels.ICU, k -> new HashSet<>())
          .add(supplyContactEncounter.getPatientId());
    } else {
      mapStationaryCaseNrs
          .computeIfAbsent(checkedDate, k -> new HashSet<>())
          .add(supplyContactEncounter.getFacilityContactId());
      mapPrevMaxtreatmentlevel
          .computeIfAbsent(TreatmentLevels.NORMAL_WARD, k -> new HashSet<>())
          .add(supplyContactEncounter.getPatientId());
    }
  }

  /**
   * The Purpose of the function is to find out whether the encounter is seen as a ventilation or
   * ecmo encounter
   *
   * @param mapPrevMaxtreatmentlevel The previous maximum treatment level.
   * @param mapIcuVentilationCaseId A map containing the case id of every ventilation case sorted by
   *     the date.
   * @param procedureCode The {@link UkbProcedure procedure.code.coding.code} that defines the
   *     resource as ecmo, ventilation or other procedure.
   * @param caseId The id of the {@link UkbEncounter} that is supposed to be checked.
   * @param casePid The Patient id of the Patient Resource attached to the Encounter.
   * @param checkedDate The current date that is being checked.
   */
  private static boolean isVent(
      Map<TreatmentLevels, Set<String>> mapPrevMaxtreatmentlevel,
      Map<Long, Set<String>> mapIcuVentilationCaseId,
      String procedureCode,
      String caseId,
      String casePid,
      Long checkedDate,
      InputCodeSettings inputCodeSettings) {
    boolean isVent = false;
    // check if the case would be seen as a ventilation case
    if (inputCodeSettings.getProcedureVentilationCodes().contains(procedureCode)) {
      isVent = true;
      // check if the case was not already marked as an ecmo case
      mapIcuVentilationCaseId.computeIfPresent(
          checkedDate,
          (k, v) -> {
            v.add(caseId);
            return v;
          });
      mapPrevMaxtreatmentlevel.computeIfPresent(
          ICU_VENTILATION,
          (k, v) -> {
            v.add(casePid);
            return v;
          });
    }
    return isVent;
  }

  /**
   * Determines whether the encounter is categorized as a ventilation or ECMO encounter based on the
   * provided parameters.
   *
   * @param mapPrevMaxtreatmentlevel The map containing the previous maximum treatment level.
   * @param mapIcuEcmoCaseId A map containing the case ID of every ECMO case sorted by date.
   * @param procedureCode The code that defines the resource as ECMO, ventilation, or another
   *     procedure.
   * @param caseId The ID of the encounter to be checked.
   * @param casePid The Patient ID of the Patient Resource attached to the Encounter.
   * @param checkedDate The current date being checked.
   * @return True if the encounter is categorized as an ECMO encounter, false otherwise.
   */
  private static boolean isEcmo(
      Map<TreatmentLevels, Set<String>> mapPrevMaxtreatmentlevel,
      Map<Long, Set<String>> mapIcuEcmoCaseId,
      String procedureCode,
      String caseId,
      String casePid,
      Long checkedDate,
      InputCodeSettings inputCodeSettings) {
    boolean isEcmo = false;
    // Check if the procedure code matches ECMO codes
    if (inputCodeSettings.getProcedureEcmoCodes().contains(procedureCode)) {
      isEcmo = true;
      // Add caseId to mapIcuEcmoCaseId for the current date
      mapIcuEcmoCaseId.computeIfPresent(
          checkedDate,
          (k, v) -> {
            v.add(caseId);
            return v;
          });
      // Add casePid to mapPrevMaxtreatmentlevel under ICU_ECMO
      mapPrevMaxtreatmentlevel.computeIfPresent(
          ICU_ECMO,
          (k, v) -> {
            v.add(casePid);
            return v;
          });
    }
    return isEcmo;
  }
}
