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
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_UNDIFF;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.isLocationReferenceExisting;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCoding;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
      List<MiiEncounter> facilityEncounters,
      List<MiiEncounter> supplyContactEncounters,
      List<MiiProcedure> icuProcedures,
      List<MiiLocation> locations,
      InputCodeSettings inputCodeSettings,
      boolean useIcuUndiff) {
    log.debug("started createMaxTreatmentTimeline");
    Instant startTimer = TimerTools.startTimer();

    // Map containing Lists with number of cases, sorted by their treatmentlevel, and a set of
    // caseIds for each day
    Map<TreatmentLevels, Map<Long, Set<String>>> resultWithCaseNrsMap = new LinkedHashMap<>();
    // Map containing a set of case Ids for each day
    Map<Long, Set<String>> mapAmbulantCaseNr = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapNormalWardCaseNrs =
        Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuCaseNrs = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuVentCaseNrs = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuEcmoCaseNrs = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuUndiffCaseNrs = Collections.synchronizedMap(new LinkedHashMap<>());
    // used to save the max treatment level per case up to the checked date
    Map<TreatmentLevels, Set<String>> prevMaxTreatmentlevelByCaseId = new ConcurrentHashMap<>();

    prevMaxTreatmentlevelByCaseId.put(OUTPATIENT, ConcurrentHashMap.newKeySet());
    prevMaxTreatmentlevelByCaseId.put(NORMAL_WARD, ConcurrentHashMap.newKeySet());
    if (useIcuUndiff) {
      prevMaxTreatmentlevelByCaseId.put(ICU_UNDIFF, ConcurrentHashMap.newKeySet());
    } else {
      prevMaxTreatmentlevelByCaseId.put(ICU, ConcurrentHashMap.newKeySet());
      prevMaxTreatmentlevelByCaseId.put(ICU_VENTILATION, ConcurrentHashMap.newKeySet());
      prevMaxTreatmentlevelByCaseId.put(ICU_ECMO, ConcurrentHashMap.newKeySet());
    }
    // Determination of the location IDs of all intensive care units. Only the wards are
    // considered, since in the location components within an Encounter resource, at best
    // ward/room and bed are listed with identical time periods, and the stay should only be
    // evaluated once. The highest of these hierarchy levels should be sufficient.
    Set<String> icuLocationIds = LocationFilter.getIcuLocationIds(locations);

    // Since kds case module profile 2024 the pre-stationary and post-stationary will now be
    // handled as outpatient cases with Encounter.class = AMB
    Set<MiiEncounter> positiveSupplyContactEncounters =
        supplyContactEncounters.parallelStream()
            .filter(EncounterFilter::isDiseasePositive)
            .filter(MiiEncounter::isCaseClassInpatientOrShortStay)
            .collect(Collectors.toSet());

    List<MiiEncounter> positiveOutpatientEncounters =
        facilityEncounters.parallelStream()
            .filter(MiiEncounter::isCaseClassOutpatient)
            .filter(EncounterFilter::isDiseasePositive)
            .toList();

    positiveSupplyContactEncounters.addAll(positiveOutpatientEncounters);

    long currentDate = getKickOffDateInSeconds(dataItemContext);
    long currentDayUnix = DateTools.getCurrentUnixTime();
    // Storing the highest treatmentlevel undiff since its simpler to handle
    Map<String, TreatmentLevels> highestTreatmentLevelUndiff = new HashMap<>();

    Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    while (currentDate <= currentDayUnix) {
      long checkDate = currentDate;
      mapAmbulantCaseNr.put(checkDate, ConcurrentHashMap.newKeySet());
      mapNormalWardCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
      if (useIcuUndiff) {
        mapIcuUndiffCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
      } else {
        mapIcuCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
        mapIcuVentCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
        mapIcuEcmoCaseNrs.put(checkDate, ConcurrentHashMap.newKeySet());
      }
      // Pre-filtering the encounters to ones that can have intersection with the date that is
      // currently checked
      try {
        positiveSupplyContactEncounters.parallelStream()
            .filter(
                x -> {
                  long start = DateTools.dateToUnixTime(x.getPeriod().getStart()) - DAY_IN_SECONDS;
                  long end =
                      DateTools.dateToUnixTime(
                              x.getPeriod().getEnd() != null
                                  ? x.getPeriod().getEnd()
                                  : DateTools.getCurrentDateTime())
                          + DAY_IN_SECONDS;
                  return start < checkDate && end >= checkDate;
                })
            .forEach(
                supplyContactEncounter -> {
                  // Since a case should just count once a day and the procedure usually references
                  // the facility contact, we need to look via top level resource
                  String facilityContactId = supplyContactEncounter.getFacilityContactId();
                  long caseStartUnix =
                      DateTools.dateToUnixTime(supplyContactEncounter.getPeriod().getStart());

                  // Prevent multiple cases of a patient from being processed at the same time
                  ReentrantLock lock =
                      locks.computeIfAbsent(facilityContactId, _ -> new ReentrantLock());
                  lock.lock();
                  boolean isNormalWard;
                  boolean isIcuUndiff = false;
                  boolean isIcu = false;
                  boolean isVent = false;
                  boolean isEcmo = false;
                  try {
                    isNormalWard =
                        prevMaxTreatmentlevelByCaseId.get(NORMAL_WARD).contains(facilityContactId);
                    if (useIcuUndiff) {
                      isIcuUndiff =
                          prevMaxTreatmentlevelByCaseId.get(ICU_UNDIFF).contains(facilityContactId);
                    } else {
                      isIcu = prevMaxTreatmentlevelByCaseId.get(ICU).contains(facilityContactId);
                      isVent =
                          prevMaxTreatmentlevelByCaseId
                              .get(ICU_VENTILATION)
                              .contains(facilityContactId);
                      isEcmo =
                          prevMaxTreatmentlevelByCaseId.get(ICU_ECMO).contains(facilityContactId);
                    }
                    // if the case is ambulant, check the pids of the previous maxtreatmentlevels
                    // and if none are similar with the current case than note the case as ambulant
                    if (supplyContactEncounter.isCaseClassOutpatient()) {
                      Map<TreatmentLevels, List<String>> mapMaxCaseCheck = new HashMap<>();
                      mapMaxCaseCheck.put(NORMAL_WARD, new ArrayList<>());
                      mapMaxCaseCheck.put(ICU, new ArrayList<>());
                      mapMaxCaseCheck.put(ICU_VENTILATION, new ArrayList<>());
                      mapMaxCaseCheck.put(ICU_ECMO, new ArrayList<>());
                      mapMaxCaseCheck.put(ICU_UNDIFF, new ArrayList<>());
                      // Normal ward
                      if (isNormalWard) mapMaxCaseCheck.get(NORMAL_WARD).add(facilityContactId);
                      if (isIcu) mapMaxCaseCheck.get(ICU).add(facilityContactId);
                      if (isVent) mapMaxCaseCheck.get(ICU_VENTILATION).add(facilityContactId);
                      if (isEcmo) mapMaxCaseCheck.get(ICU_ECMO).add(facilityContactId);
                      if (isIcuUndiff) mapMaxCaseCheck.get(ICU_UNDIFF).add(facilityContactId);

                      // list containing every supplyContactEncounter with a currently higher
                      // treatmentlevel than 'outpatient'.
                      Set<String> listHigherTreatment =
                          mapMaxCaseCheck.values().stream()
                              .flatMap(List::stream)
                              .collect(Collectors.toCollection(LinkedHashSet::new));
                      // Exit early if the case has a higher treatment level than outpatient
                      if (!listHigherTreatment.isEmpty()) {
                        return;
                      }
                      // Check if the case falls within the same day as checkDate
                      if (caseStartUnix >= checkDate
                          && caseStartUnix < checkDate + DAY_IN_SECONDS) {
                        addCaseToTimeline(mapAmbulantCaseNr, checkDate, facilityContactId);
                        markPreviousMaxLevel(
                            prevMaxTreatmentlevelByCaseId, OUTPATIENT, facilityContactId);
                      }
                    }
                    // At this stage it is clear that it is a stationary supplyContactEncounter
                    else if (supplyContactEncounter.isCaseClassInpatientOrShortStay()) {
                      handleInpatientEncounter(
                          icuProcedures,
                          inputCodeSettings,
                          useIcuUndiff,
                          supplyContactEncounter,
                          caseStartUnix,
                          checkDate,
                          icuLocationIds,
                          prevMaxTreatmentlevelByCaseId,
                          mapNormalWardCaseNrs,
                          mapIcuUndiffCaseNrs,
                          highestTreatmentLevelUndiff,
                          facilityContactId,
                          isIcu,
                          isVent,
                          isEcmo,
                          mapIcuCaseNrs,
                          mapIcuVentCaseNrs,
                          mapIcuEcmoCaseNrs);
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
    if (useIcuUndiff) resultWithCaseNrsMap.put(ICU_UNDIFF, mapIcuUndiffCaseNrs);
    else {
      resultWithCaseNrsMap.put(ICU, mapIcuCaseNrs);
      resultWithCaseNrsMap.put(ICU_VENTILATION, mapIcuVentCaseNrs);
      resultWithCaseNrsMap.put(ICU_ECMO, mapIcuEcmoCaseNrs);

      logVentEcmoNotOnIcu(
          supplyContactEncounters, icuLocationIds, mapIcuVentCaseNrs, mapIcuEcmoCaseNrs);
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createMaxTreatmentTimeline");
    return resultWithCaseNrsMap;
  }

  private static void handleInpatientEncounter(
      List<MiiProcedure> icuProcedures,
      InputCodeSettings inputCodeSettings,
      Boolean useIcuUndiff,
      MiiEncounter supplyContactEncounter,
      long caseStartUnix,
      long checkDate,
      Set<String> icuLocationIds,
      Map<TreatmentLevels, Set<String>> prevMaxTreatmentlevelByCaseId,
      Map<Long, Set<String>> mapNormalWardCaseNrs,
      Map<Long, Set<String>> mapIcuUndiffCaseNrs,
      Map<String, TreatmentLevels> highestTreatmentLevelUndiff,
      String facilityContactId,
      boolean isIcu,
      boolean isVent,
      boolean isEcmo,
      Map<Long, Set<String>> mapIcuCaseNrs,
      Map<Long, Set<String>> mapIcuVentCaseNrs,
      Map<Long, Set<String>> mapIcuEcmoCaseNrs) {
    // Active encounter -> use current datetime
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
      List<EncounterLocationComponent> encounterIcuLocations =
          getListIcuEncounterLocationComponents(supplyContactEncounter, icuLocationIds);

      if (useIcuUndiff) {
        handleIcuUndiff(
            supplyContactEncounter,
            encounterIcuLocations,
            checkDate,
            prevMaxTreatmentlevelByCaseId,
            mapNormalWardCaseNrs,
            mapIcuUndiffCaseNrs,
            highestTreatmentLevelUndiff,
            facilityContactId);
      } else {
        // No icu related treatmentlevel for this case -> normal ward
        if (!isIcu && !isVent && !isEcmo) {
          // if the case contains no icu locations
          if (encounterIcuLocations.isEmpty()) {
            addCaseToTimeline(mapNormalWardCaseNrs, checkDate, facilityContactId);
            markPreviousMaxLevel(prevMaxTreatmentlevelByCaseId, NORMAL_WARD, facilityContactId);
          } else {
            // check if there are any icu procedures currently going on
            List<MiiProcedure> listEncounterIcuProcedure =
                icuProcedures.stream()
                    .filter(icu -> facilityContactId.equals(icu.getCaseId()))
                    .toList();
            // if there is no ICU Procedure, check if it is currently considered
            // as ICU or stationary
            if (listEncounterIcuProcedure.isEmpty()) {
              encounterStationTypeCheckProcess(
                  encounterIcuLocations,
                  supplyContactEncounter,
                  checkDate,
                  prevMaxTreatmentlevelByCaseId,
                  mapNormalWardCaseNrs,
                  mapIcuCaseNrs);
            } else {
              // check and sort if it is a ventilation or ecmo case
              sortToVentOrEcmoTimeline(
                  supplyContactEncounter,
                  listEncounterIcuProcedure,
                  encounterIcuLocations,
                  false,
                  false,
                  checkDate,
                  mapNormalWardCaseNrs,
                  mapIcuCaseNrs,
                  mapIcuVentCaseNrs,
                  mapIcuEcmoCaseNrs,
                  prevMaxTreatmentlevelByCaseId,
                  inputCodeSettings);
            }
          }
        }
        // if an ICU case was found
        else if (isIcu && !isVent && !isEcmo) {
          List<MiiProcedure> listEncounterIcuProcedure =
              icuProcedures.stream()
                  .filter(icu -> facilityContactId.equals(icu.getCaseId()))
                  .collect(Collectors.toList());

          if (listEncounterIcuProcedure.isEmpty()) {
            addCaseToTimeline(mapIcuCaseNrs, checkDate, facilityContactId);
            markPreviousMaxLevel(prevMaxTreatmentlevelByCaseId, ICU, facilityContactId);
          } else {
            sortToVentOrEcmoTimeline(
                supplyContactEncounter,
                listEncounterIcuProcedure,
                encounterIcuLocations,
                false,
                false,
                checkDate,
                mapNormalWardCaseNrs,
                mapIcuCaseNrs,
                mapIcuVentCaseNrs,
                mapIcuEcmoCaseNrs,
                prevMaxTreatmentlevelByCaseId,
                inputCodeSettings);
          }
        }
        // if a ventilation case was found
        else if (isVent && !isEcmo) {
          List<MiiProcedure> icuProceduresEncounter =
              icuProcedures.stream()
                  .filter(icu -> facilityContactId.equals(icu.getCaseId()))
                  .toList();
          sortToVentOrEcmoTimeline(
              supplyContactEncounter,
              icuProceduresEncounter,
              encounterIcuLocations,
              true,
              false,
              checkDate,
              mapNormalWardCaseNrs,
              mapIcuCaseNrs,
              mapIcuVentCaseNrs,
              mapIcuEcmoCaseNrs,
              prevMaxTreatmentlevelByCaseId,
              inputCodeSettings);
        }
        // if an ecmo case was found
        else {
          addCaseToTimeline(mapIcuEcmoCaseNrs, checkDate, facilityContactId);
          markPreviousMaxLevel(prevMaxTreatmentlevelByCaseId, ICU_ECMO, facilityContactId);
        }
      }
    } // if date check
  }

  private static boolean isEncounterOnIcuAtDate(
      List<Encounter.EncounterLocationComponent> encountersWithIcuLocation,
      MiiEncounter supplyContactEncounter,
      Long checkedDate) {

    for (Encounter.EncounterLocationComponent location : encountersWithIcuLocation) {
      Period period = getValidPeriod(location, supplyContactEncounter);

      if (period == null || !period.hasStart()) {
        logNoValidPeriodFound(supplyContactEncounter);
        continue;
      }
      if (!period.hasEnd()) {
        period.setEnd(DateTools.getCurrentDateTime());
      }
      long locationDateStartUnix = DateTools.dateToUnixTime(period.getStart());
      long locationDateEndUnix = DateTools.dateToUnixTime(period.getEnd());
      // check overlap with day
      if (locationDateStartUnix < checkedDate && locationDateEndUnix >= checkedDate) {
        return true;
      }
    }
    return false;
  }

  private static List<EncounterLocationComponent> getListIcuEncounterLocationComponents(
      MiiEncounter supplyContactEncounter, Set<String> icuLocationIds) {
    return supplyContactEncounter.getLocation().stream()
        .filter(DiseaseResultFunctionality::isLocationReferenceExisting)
        .filter(location -> icuLocationIds.contains(extractIdFromReference(location.getLocation())))
        .toList();
  }

  private static void handleIcuUndiff(
      MiiEncounter supplyContactEncounter,
      List<EncounterLocationComponent> listEncounterHasIcuLocation,
      long checkDate,
      Map<TreatmentLevels, Set<String>> prevMaxTreatmentLevelsByCase,
      Map<Long, Set<String>> mapNormalWardCaseNrs,
      Map<Long, Set<String>> mapIcuUndiffCaseNrs,
      Map<String, TreatmentLevels> highestTreatmentLevelUndiff,
      String facilityContactId) {

    boolean encounterOnIcu =
        isEncounterOnIcuAtDate(listEncounterHasIcuLocation, supplyContactEncounter, checkDate);
    // ensure exclusive assignment
    mapNormalWardCaseNrs
        .computeIfAbsent(checkDate, _ -> ConcurrentHashMap.newKeySet())
        .remove(facilityContactId);
    mapIcuUndiffCaseNrs
        .computeIfAbsent(checkDate, _ -> ConcurrentHashMap.newKeySet())
        .remove(facilityContactId);
    // keep the highest level once reached
    if (highestTreatmentLevelUndiff.getOrDefault(facilityContactId, NORMAL_WARD).equals(ICU_UNDIFF)
        || encounterOnIcu) {
      addCaseToTimeline(mapIcuUndiffCaseNrs, checkDate, facilityContactId);
      markPreviousMaxLevel(prevMaxTreatmentLevelsByCase, ICU_UNDIFF, facilityContactId);
      highestTreatmentLevelUndiff.put(facilityContactId, ICU_UNDIFF);
    } else {
      addCaseToTimeline(mapNormalWardCaseNrs, checkDate, facilityContactId);
      markPreviousMaxLevel(prevMaxTreatmentLevelsByCase, NORMAL_WARD, facilityContactId);
      highestTreatmentLevelUndiff.putIfAbsent(facilityContactId, NORMAL_WARD);
    }
  }

  private static void logVentEcmoNotOnIcu(
      List<MiiEncounter> supplyContactEncounters,
      Set<String> icuLocationIds,
      Map<Long, Set<String>> mapIcuVentCaseNrs,
      Map<Long, Set<String>> mapIcuEcmoCaseNrs) {
    Set<String> encountersWithIcu =
        supplyContactEncounters.parallelStream()
            .filter(Encounter::hasLocation)
            .filter(x -> isLocationReferenceExisting(x.getLocationFirstRep()))
            .filter(
                location ->
                    icuLocationIds.contains(
                        extractIdFromReference(location.getLocationFirstRep().getLocation())))
            .map(MiiEncounter::getFacilityContactId)
            .collect(Collectors.toSet());

    // Precompute ICU case sets for fast lookup
    Set<String> ventCaseSet =
        mapIcuVentCaseNrs.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

    Set<String> ecmoCaseSet =
        mapIcuEcmoCaseNrs.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

    // Filter encounters that are not in ICU location but exist in one of the case sets
    List<String> results =
        supplyContactEncounters.parallelStream()
            .map(MiiEncounter::getFacilityContactId)
            .filter(id -> !encountersWithIcu.contains(id))
            .filter(id -> ventCaseSet.contains(id) || ecmoCaseSet.contains(id))
            .toList();
    if (!results.isEmpty())
      log.debug(
          "{} encounter with vent/ecmo but no icu linkage. Example: {}",
          results.size(),
          results.getFirst());
  }

  /**
   * Adds the current {@link MiiEncounter} to a list if he has had artificial respiration or ECMO
   *
   * @param listEncounterIcuProcedures The {@link MiiProcedure} resources, which include information
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
   * @param prevMaxTreatmentLevelsByCaseId A map with the previous maxtreatmentlevel of an
   *     encounter.
   */
  private static void sortToVentOrEcmoTimeline(
      MiiEncounter supplyContactEncounter,
      List<MiiProcedure> listEncounterIcuProcedures,
      List<EncounterLocationComponent> listEncounterHasIcuLocation,
      boolean isVent,
      boolean isEcmo,
      Long checkedDate,
      Map<Long, Set<String>> mapNormalWardCaseNrs,
      Map<Long, Set<String>> mapIcuCaseNrs,
      Map<Long, Set<String>> mapIcuVentCaseNrs,
      Map<Long, Set<String>> mapIcuEcmoCaseNrs,
      Map<TreatmentLevels, Set<String>> prevMaxTreatmentLevelsByCaseId,
      InputCodeSettings inputCodeSettings) {
    String caseId = supplyContactEncounter.getFacilityContactId();

    for (MiiProcedure procedure : listEncounterIcuProcedures) {
      try {
        Period procedurePeriod = procedure.getPerformedPeriod();
        if (!procedurePeriod.hasEnd()) {
          procedurePeriod.setEnd(DateTools.getCurrentDateTime());
        }

        long procedureStartUnix = DateTools.dateToUnixTime(procedurePeriod.getStart());
        long procedureEndUnix = DateTools.dateToUnixTime(procedurePeriod.getEnd());

        // check if the procedure fits into the checked time span
        if (procedure.hasCode() && procedure.getCode().hasCoding()) {
          String procedureCode = getCodeOfFirstCoding(procedure.getCode().getCoding());

          boolean procedureMatchesCheckedDate =
              procedureStartUnix < checkedDate && procedureEndUnix >= checkedDate;

          boolean procedureEndedWithinPreviousDay =
              procedureEndUnix < checkedDate
                  && procedureEndUnix >= (checkedDate - DAY_IN_SECONDS)
                  && checkedDate - procedureEndUnix < DAY_IN_SECONDS;

          // check if the end date of the procedure fits into the checked time span
          if (procedureMatchesCheckedDate || procedureEndedWithinPreviousDay) {
            if (inputCodeSettings.getProcedureVentilationCodes().contains(procedureCode)) {
              isVent = true;
            }

            if (inputCodeSettings.getProcedureEcmoCodes().contains(procedureCode)) {
              isEcmo = true;
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

    // Assign only the highest treatment level for this case and day.
    if (isEcmo) {
      removeCaseFromTimeline(mapNormalWardCaseNrs, checkedDate, caseId);
      removeCaseFromTimeline(mapIcuCaseNrs, checkedDate, caseId);
      removeCaseFromTimeline(mapIcuVentCaseNrs, checkedDate, caseId);

      addCaseToTimeline(mapIcuEcmoCaseNrs, checkedDate, caseId);
      markPreviousMaxLevel(prevMaxTreatmentLevelsByCaseId, ICU_ECMO, caseId);

    } else if (isVent) {
      removeCaseFromTimeline(mapNormalWardCaseNrs, checkedDate, caseId);
      removeCaseFromTimeline(mapIcuCaseNrs, checkedDate, caseId);

      addCaseToTimeline(mapIcuVentCaseNrs, checkedDate, caseId);
      markPreviousMaxLevel(prevMaxTreatmentLevelsByCaseId, ICU_VENTILATION, caseId);

    } else {
      encounterStationTypeCheckProcess(
          listEncounterHasIcuLocation,
          supplyContactEncounter,
          checkedDate,
          prevMaxTreatmentLevelsByCaseId,
          mapNormalWardCaseNrs,
          mapIcuCaseNrs);
    }
  }

  private static void removeCaseFromTimeline(
      Map<Long, Set<String>> timeline, long checkDate, String caseId) {
    timeline.computeIfAbsent(checkDate, _ -> ConcurrentHashMap.newKeySet()).remove(caseId);
  }

  /**
   * This process is used to determine, whether an supplyContactEncounter, would be seen as an ICU
   * or as a stationary supplyContactEncounter
   *
   * @param encountersWithIcuLocation Icu locations of an supplyContactEncounter
   * @param checkedDate The current date that is being checked
   * @param prevMaxTreatmentLevelByCaseId Map containing all previous maxtreatmentlevels up to the
   *     checked date
   * @param mapStationaryCaseNrs Map containing all case ids of stationary cases, sorted by the date
   * @param mapIcuCaseNrs Map containing all case ids of ICU cases, sorted by the date
   */
  private static void encounterStationTypeCheckProcess(
      List<Encounter.EncounterLocationComponent> encountersWithIcuLocation,
      MiiEncounter supplyContactEncounter,
      Long checkedDate,
      Map<TreatmentLevels, Set<String>> prevMaxTreatmentLevelByCaseId,
      Map<Long, Set<String>> mapStationaryCaseNrs,
      Map<Long, Set<String>> mapIcuCaseNrs) {

    boolean isInIcu = false;
    String facilityContactId = supplyContactEncounter.getFacilityContactId();

    for (Encounter.EncounterLocationComponent location : encountersWithIcuLocation) {
      Period period = getValidPeriod(location, supplyContactEncounter);

      if (period == null || !period.hasStart()) {
        logNoValidPeriodFound(supplyContactEncounter);
        continue;
      }

      if (!period.hasEnd()) {
        period.setEnd(DateTools.getCurrentDateTime());
      }

      long locationDateStartUnix = DateTools.dateToUnixTime(period.getStart());
      long locationDateEndUnix = DateTools.dateToUnixTime(period.getEnd());

      if (locationDateStartUnix < checkedDate && locationDateEndUnix >= checkedDate) {
        isInIcu = true;
        break;
      }
    }

    if (isInIcu) {
      addCaseToTimeline(mapIcuCaseNrs, checkedDate, facilityContactId);
      markPreviousMaxLevel(prevMaxTreatmentLevelByCaseId, ICU, facilityContactId);
    } else {
      addCaseToTimeline(mapStationaryCaseNrs, checkedDate, facilityContactId);
      markPreviousMaxLevel(prevMaxTreatmentLevelByCaseId, NORMAL_WARD, facilityContactId);
    }
  }

  private static void logNoValidPeriodFound(MiiEncounter supplyContactEncounter) {
    log.warn("No valid period start found for Encounter: {}", supplyContactEncounter.getId());
  }

  /** Stores or updates the previously reached maximum treatment level for a specific case. */
  private static void markPreviousMaxLevel(
      Map<TreatmentLevels, Set<String>> previousMaxLevelsByCase,
      TreatmentLevels level,
      String caseId) {
    previousMaxLevelsByCase.computeIfAbsent(level, _ -> ConcurrentHashMap.newKeySet()).add(caseId);
  }

  /**
   * Adds a case to the timeline for a specific day.
   *
   * <p>This ensures that the given case is counted for the provided date within the corresponding
   * treatment timeline.
   */
  private static void addCaseToTimeline(
      Map<Long, Set<String>> timeline, long checkDate, String caseId) {
    timeline.computeIfAbsent(checkDate, _ -> ConcurrentHashMap.newKeySet()).add(caseId);
  }
}
