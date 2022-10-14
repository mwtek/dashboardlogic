/*
 *
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 *
 */
package de.ukbonn.mwtek.dashboardlogic.logic.timeline;

import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCoding;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeOfFirstCodeableConceptEquals;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeOfFirstCodingEquals;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;

/**
 * This class is used for generating the data item
 * {@link CoronaDataItem timeline.maxtreatmentlevel}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class TimelineMaxTreatmentLevel extends TimelineFunctionalities {

  public List<UkbEncounter> listEncounters;
  public List<UkbLocation> listLocations;
  public List<UkbProcedure> listIcuProcedures;

  public TimelineMaxTreatmentLevel(List<UkbEncounter> listEncounters,
      List<UkbProcedure> listIcuProcedures, List<UkbLocation> listLocations) {
    super();

    this.listEncounters = listEncounters;
    this.listLocations = listLocations;
    this.listIcuProcedures = listIcuProcedures;
  }

  /**
   * Creates a map containing all maximal treatment of cases for each day, since the qualifying
   * date
   *
   * @return Map that assigns the cases per day to a treatment level and also contains a map with
   * the case ids per date
   */
  public Map<String, Map<Long, Set<String>>> createMaxTreatmentTimeline(
      InputCodeSettings inputCodeSettings) {
    log.debug("started createMaxTreatmentTimeline");
    Instant startTimer = TimerTools.startTimer();

    // Map containing Lists with amount of cases, sorted by their treatmentlevel, and a set of
    // caseNrs for each Day
    Map<String, Map<Long, Set<String>>> resultWithCaseNrsMap = new LinkedHashMap<>();
    // Map containing a set of case Ids for each day
    Map<Long, Set<String>> mapAmbulantCaseNr = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapStationaryCaseNr = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuCaseNr = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuVentilationCaseNr =
        Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapIcuEcmoCaseNr = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<Long, Set<String>> mapInpatientCaseNr = Collections.synchronizedMap(new LinkedHashMap<>());
    // used to save the maxtreatmentlevel up to the checked date
    Map<String, Set<String>> mapPrevMaxtreatmentlevel = new ConcurrentHashMap<>();

    mapPrevMaxtreatmentlevel.put(CoronaFixedValues.OUTPATIENT_ITEM.getValue(),
        ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(CoronaFixedValues.NORMAL_WARD.getValue(),
        ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(CoronaFixedValues.ICU.getValue(), ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
        ConcurrentHashMap.newKeySet());
    mapPrevMaxtreatmentlevel.put(CoronaFixedValues.ICU_ECMO.getValue(),
        ConcurrentHashMap.newKeySet());

    // Determination of the location IDs of all intensive care units. Only the wards are
    // considered,
    // since in the location components within an Encounter resource, at best ward/room and bed
    // are
    // listed with identical time periods and the stay should only be evaluated once. The
    // highest of
    // these hierarchy levels should be sufficient.
    List<String> listIcuWardsId = new ArrayList<>();

    List<UkbEncounter> listPositiveEncounter = listEncounters.parallelStream()
        .filter(encounter -> encounter.hasExtension(
            CoronaFixedValues.POSITIVE_RESULT.getValue())).toList();
    try {
      List<String> listIcuIds = listLocations.parallelStream()
          .filter(
              x -> x.hasPhysicalType() && isCodeOfFirstCodingEquals(x.getPhysicalType().getCoding()
                  , CoronaFixedValues.WARD.getValue()))
          .filter(x -> x.hasType() && isCodeOfFirstCodeableConceptEquals(x.getType(),
              CoronaFixedValues.ICU.getValue())).map(UkbLocation::getId).toList();
      listIcuWardsId.addAll(listIcuIds);
    } catch (Exception e) {
      e.printStackTrace();
    }

    long currentDate = CoronaDashboardConstants.qualifyingDate;
    long currentDayUnix = DateTools.getCurrentUnixTime();

    while (currentDate <= currentDayUnix) {
      long checkDate = currentDate;
      mapAmbulantCaseNr.put(checkDate, new HashSet<>());
      mapStationaryCaseNr.put(checkDate, new HashSet<>());
      mapIcuCaseNr.put(checkDate, new HashSet<>());
      mapIcuVentilationCaseNr.put(checkDate, new HashSet<>());
      mapIcuEcmoCaseNr.put(checkDate, new HashSet<>());
      mapInpatientCaseNr.put(checkDate, new HashSet<>());

      Map<String, Object> locks = new ConcurrentHashMap<>();

      // filter the encounter to ones that can have intersection with the date that is current
      // checked
      // TODO verify this
      listPositiveEncounter.stream().filter(UkbEncounter::isPeriodStartExistent)
          .filter(x -> DateTools.dateToUnixTime(x.getPeriod()
              .getStart()) - CoronaDashboardConstants.dayInSeconds <= checkDate)
          .filter(x -> DateTools.dateToUnixTime(x.getPeriod().getEnd() == null ?
              DateTools.getCurrentDateTime() :
              x.getPeriod().getEnd()) + CoronaDashboardConstants.dayInSeconds >= checkDate)
          .forEach(encounter -> {
            String caseId = encounter.getId();
            String casePid = encounter.getPatientId();

            // Prevent multiple cases of a patient from being processed at the same time
            locks.putIfAbsent(casePid, new Object());
            long caseStartUnix = DateTools.dateToUnixTime(encounter.getPeriod().getStart());

            synchronized (locks.get(casePid)) {
              boolean isStationary =
                  mapPrevMaxtreatmentlevel.get(CoronaFixedValues.NORMAL_WARD.getValue())
                      .contains(casePid);
              boolean isIcu = mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU.getValue())
                  .contains(casePid);
              boolean isVent =
                  mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU_VENTILATION.getValue())
                      .contains(casePid);
              boolean isEcmo = mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU_ECMO.getValue())
                  .contains(casePid);
              // if the case is ambulant, check the pids of the previous maxtreatmentlevels
              // and if none are similar with the current case than note the case as ambulant
              if (CoronaResultFunctionality.isCaseClassOutpatient(encounter)) {

                List<String> listMaxStatPidCheck = new ArrayList<>();
                List<String> listMaxIcuPidCheck = new ArrayList<>();
                List<String> listMaxIcuVentPidCheck = new ArrayList<>();
                List<String> listMaxIcuEcmoPidCheck = new ArrayList<>();

                // Normal ward
                if (isStationary) {
                  listMaxStatPidCheck.add(casePid);
                }
                // ICU
                if (isIcu) {
                  listMaxIcuPidCheck.add(casePid);
                }
                // Ventilation
                if (isVent) {
                  listMaxIcuVentPidCheck.add(casePid);
                }
                // ECMO
                if (isEcmo) {
                  listMaxIcuEcmoPidCheck.add(casePid);
                }

                // list containing every encounter with a currently higher treatmentlevel than 'outpatient'.
                List<String> listHigherTreatment = new ArrayList<>();
                listHigherTreatment.addAll(listMaxStatPidCheck);
                listHigherTreatment.addAll(listMaxIcuPidCheck);
                listHigherTreatment.addAll(listMaxIcuVentPidCheck);
                listHigherTreatment.addAll(listMaxIcuEcmoPidCheck);

                if (listHigherTreatment.isEmpty()) {
                  if (caseStartUnix >= checkDate && caseStartUnix <= (checkDate
                      + CoronaDashboardConstants.dayInSeconds)) {

                    mapAmbulantCaseNr.get(checkDate).add(caseId);
                    mapPrevMaxtreatmentlevel.get(CoronaFixedValues.OUTPATIENT_ITEM.getValue())
                        .add(casePid);
                  }
                }
              }
              // At this stage it is clear that it is a stationary encounter
              else if (CoronaResultFunctionality.isCaseClassInpatient(encounter)) {
                if (!encounter.getPeriod().hasEnd()) {
                  encounter.getPeriod().setEnd(DateTools.getCurrentDateTime());
                }
                long caseEndUnix = DateTools.dateToUnixTime(encounter.getPeriod().getEnd());
                // The admission date needs to be before the start date and the discharge date needs to be
                // after the currentDate to get the whole time span
                if (caseStartUnix <= checkDate && caseEndUnix >= checkDate) {
                  List<Encounter.EncounterLocationComponent> listEncounterHasIcuLocation =
                      encounter.getLocation().stream()
                          .filter(location -> listIcuWardsId.contains(
                              CoronaResultFunctionality.extractIdFromReference(
                                  location.getLocation()))).toList();
                  // if there was no icu related treatmentlevel for this case before
                  if (!isIcu && !isVent && !isEcmo) {
                    // if the case contains no icu locations
                    if (listEncounterHasIcuLocation.isEmpty()) {

                      mapStationaryCaseNr.get(checkDate).add(caseId);
                      mapInpatientCaseNr.get(checkDate).add(caseId);
                      mapPrevMaxtreatmentlevel.get(CoronaFixedValues.NORMAL_WARD.getValue())
                          .add(casePid);
                    } else {
                      // check if there are any icu procedures currently going on
                      List<UkbProcedure> listEncounterIcuProcedure = listIcuProcedures.stream()
                          .filter(icu -> caseId.equals(icu.getCaseId()))
                          .toList();
                      // if there is no ICU Procedure, check if it is currently considered
                      // as ICU or stationary
                      if (listEncounterIcuProcedure.isEmpty()) {
                        encounterStationTypeCheckProcess(listEncounterHasIcuLocation, checkDate,
                            mapPrevMaxtreatmentlevel, mapStationaryCaseNr, mapIcuCaseNr,
                            mapInpatientCaseNr, caseId, casePid);
                      } else {
                        // check and sort if it is a ventilation or ecmo case
                        sortToVentOrEcmoTimeline(listEncounterIcuProcedure,
                            listEncounterHasIcuLocation, caseId, casePid, isVent, isEcmo,
                            checkDate, mapStationaryCaseNr, mapIcuCaseNr,
                            mapIcuVentilationCaseNr, mapIcuEcmoCaseNr, mapInpatientCaseNr,
                            mapPrevMaxtreatmentlevel, inputCodeSettings);
                      }
                    }
                  }
                  // if an ICU case was found
                  else if (isIcu && !isVent && !isEcmo) {
                    List<UkbProcedure> listEncounterIcuProcedure = listIcuProcedures.stream()
                        .filter(icu -> caseId.equals(icu.getCaseId()))
                        .collect(Collectors.toList());

                    if (listEncounterIcuProcedure.isEmpty()) {
                      mapIcuCaseNr.get(checkDate).add(caseId);
                      mapInpatientCaseNr.get(checkDate).add(caseId);
                      mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU.getValue()).add(casePid);
                    } else {
                      sortToVentOrEcmoTimeline(listEncounterIcuProcedure,
                          listEncounterHasIcuLocation, caseId, casePid, isVent, isEcmo,
                          checkDate, mapStationaryCaseNr, mapIcuCaseNr,
                          mapIcuVentilationCaseNr, mapInpatientCaseNr, mapIcuEcmoCaseNr,
                          mapPrevMaxtreatmentlevel, inputCodeSettings);
                    }
                  }
                  // if a ventilation case was found
                  else if (isVent && !isEcmo) {
                    List<UkbProcedure> listEncounterIcuProcedure = listIcuProcedures.stream()
                        .filter(icu -> caseId.equals(icu.getCaseId()))
                        .collect(Collectors.toList());
                    sortToVentOrEcmoTimeline(listEncounterIcuProcedure,
                        listEncounterHasIcuLocation, caseId, casePid, isVent, isEcmo,
                        checkDate, mapStationaryCaseNr, mapIcuCaseNr,
                        mapIcuVentilationCaseNr, mapIcuEcmoCaseNr, mapInpatientCaseNr,
                        mapPrevMaxtreatmentlevel, inputCodeSettings);
                  }
                  // if an ecmo case was found
                  else {
                    mapIcuEcmoCaseNr.get(checkDate).add(caseId);
                    mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU_ECMO.getValue())
                        .add(casePid);
                  }
                } // if date check
              } // else if Stationary check
            } // for loop of positive encounter
          });
      currentDate += CoronaDashboardConstants.dayInSeconds;
    } // while
    resultWithCaseNrsMap.put(CoronaFixedValues.OUTPATIENT_ITEM.getValue(), mapAmbulantCaseNr);
    resultWithCaseNrsMap.put(CoronaFixedValues.NORMAL_WARD.getValue(), mapStationaryCaseNr);
    resultWithCaseNrsMap.put(CoronaFixedValues.ICU.getValue(), mapIcuCaseNr);
    resultWithCaseNrsMap.put(CoronaFixedValues.ICU_VENTILATION.getValue(), mapIcuVentilationCaseNr);
    resultWithCaseNrsMap.put(CoronaFixedValues.ICU_ECMO.getValue(), mapIcuEcmoCaseNr);
    resultWithCaseNrsMap.put(CoronaFixedValues.CASESTATUS_INPATIENT.getValue(), mapInpatientCaseNr);

    TimerTools.stopTimerAndLog(startTimer, "finished createMaxTreatmentTimeline");
    return resultWithCaseNrsMap;
  }


  /**
   * Adds the current {@link UkbEncounter} to a list if he has had artificial respiration or ECMO
   *
   * @param listEncounterIcuProcedures  The {@link UkbProcedure} resources, which include
   *                                    information about ECMO / artificial ventilation periods
   * @param listEncounterHasIcuLocation List containing all icu locations of an encounter
   * @param checkedDate                 The date [unixtime] that is going to be checked
   * @param mapIcuVentilationCaseId     A map that contains the case ids of every ventilation case,
   *                                    sorted by the date
   * @param mapIcuEcmoCaseId            A map that contains the case ids of every ecmo case, sorted
   *                                    by the date
   * @param mapPrevMaxtreatmentlevel    A map with the previous maxtreatmentlevel of an encounter
   * @param mapStationaryCaseNr         A map that contains the case ids of every stationary case,
   *                                    sorted by the date
   * @param mapIcuCaseNr                A map that contains the case ids of every icu case, sorted
   *                                    by the date
   * @param mapInpatientCaseId          A map that contains the case id of all inpatient encounter
   * @param caseId                      id of the Encounter that is supposed to be checked
   * @param isEcmo                      check if the encounter has any ecmo related resources
   *                                    attached to it
   * @param isVent                      check if the encounter has any ventilation related resources
   *                                    attached to it
   */
  private static void sortToVentOrEcmoTimeline(List<UkbProcedure> listEncounterIcuProcedures,
      List<Encounter.EncounterLocationComponent> listEncounterHasIcuLocation, String caseId,
      String casePid, Boolean isVent, Boolean isEcmo, Long checkedDate,
      Map<Long, Set<String>> mapStationaryCaseNr, Map<Long, Set<String>> mapIcuCaseNr,
      Map<Long, Set<String>> mapIcuVentilationCaseId, Map<Long, Set<String>> mapIcuEcmoCaseId,
      Map<Long, Set<String>> mapInpatientCaseId, Map<String, Set<String>> mapPrevMaxtreatmentlevel,
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
          if (procedureStartUnix <= checkedDate && procedureEndUnix >= checkedDate) {
            checkVentOrEcmo(mapPrevMaxtreatmentlevel, mapIcuVentilationCaseId, mapIcuEcmoCaseId,
                mapInpatientCaseId, procedureCode, caseId, casePid, isEcmo, checkedDate,
                inputCodeSettings);
          }
          // check if the end date of the procedure fits into the checked time span
          else if (procedureEndUnix <= checkedDate && procedureEndUnix >= (checkedDate
              - CoronaDashboardConstants.dayInSeconds)) {
            long difference = checkedDate - procedureEndUnix;
            if (difference <= CoronaDashboardConstants.dayInSeconds) {
              checkVentOrEcmo(mapPrevMaxtreatmentlevel, mapIcuVentilationCaseId, mapIcuEcmoCaseId,
                  mapInpatientCaseId, procedureCode, caseId, casePid, isEcmo,
                  checkedDate, inputCodeSettings);
            }
          }
        }
      } catch (Exception ex) {
        log.debug(
            "Unable to retrieve the performedPeriod for Procedure: " + procedure.getId() + " ["
                + ex.getMessage() + "]");
      }
    }
    // check if the case was added, into the map containing the previous maxtreatmentlevels
    if (!isVent && !isEcmo) {
      // if neither ecmo nor encounter, then check if case is icu or stationary again
      encounterStationTypeCheckProcess(listEncounterHasIcuLocation, checkedDate,
          mapPrevMaxtreatmentlevel, mapStationaryCaseNr, mapIcuCaseNr, mapInpatientCaseId, caseId,
          casePid);
    } else {
      // purpose is to make sure that the maxtreatmentlevels of the case is still marked correctly,
      // even though the time check does not apply anymore.
      if (isVent) {
        if (!isEcmo) {
          mapIcuVentilationCaseId.get(checkedDate).add(caseId);
          mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU_VENTILATION.getValue()).add(casePid);
        } else {
          mapIcuEcmoCaseId.get(checkedDate).add(caseId);
          mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU_ECMO.getValue()).add(casePid);
        }
      }
    }
  }

  /**
   * This process is used to determine, whether an encounter, would be seen as an ICU or an
   * stationary encounter
   *
   * @param listEncounterHasIcuLocation Icu locations of an encounter
   * @param checkedDate                 The current date that is being checked
   * @param mapPrevMaxtreatmentlevel    Map containing all previous maxtreatmentlevels up to the
   *                                    checked date
   * @param mapStationaryCaseNr         Map containing all case ids of stationary cases, sorted by
   *                                    the date
   * @param mapIcuCaseNr                Map containing all case ids of ICU cases, sorted by the
   *                                    date
   * @param mapInpatientCaseNr          A map that contains the case id of all inpatient encounter
   * @param caseId                      The id of the encounter that is supposed to be checked
   * @param casePid                     Patient id of the patient resource attached to the
   *                                    encounter
   */
  private static void encounterStationTypeCheckProcess(
      List<Encounter.EncounterLocationComponent> listEncounterHasIcuLocation, Long checkedDate,
      Map<String, Set<String>> mapPrevMaxtreatmentlevel,
      Map<Long, Set<String>> mapStationaryCaseNr, Map<Long, Set<String>> mapIcuCaseNr,
      Map<Long, Set<String>> mapInpatientCaseNr, String caseId, String casePid) {
    List<Encounter.EncounterLocationComponent> listCheckDateLocation = new ArrayList<>();

    // iterate through the icu locations
    for (Encounter.EncounterLocationComponent location : listEncounterHasIcuLocation) {
      if (!location.getPeriod().hasEnd()) {
        location.getPeriod().setEnd(DateTools.getCurrentDateTime());
      }

      long locationDateStartUnix = DateTools.dateToUnixTime(location.getPeriod().getStart());
      long locationDateEndUnix = DateTools.dateToUnixTime(location.getPeriod().getEnd());
      // check if checkedDate fits into the time span of the icu location
      if (locationDateStartUnix <= checkedDate && locationDateEndUnix >= checkedDate) {
        listCheckDateLocation.add(location);
      }
    }
    // if not currently in icu, then it is still a StationWard case
    if (listCheckDateLocation.isEmpty()) {
      mapStationaryCaseNr.get(checkedDate).add(caseId);
      mapInpatientCaseNr.get(checkedDate).add(caseId);
      mapPrevMaxtreatmentlevel.get(CoronaFixedValues.NORMAL_WARD.getValue()).add(casePid);
    } else {
      mapIcuCaseNr.get(checkedDate).add(caseId);
      mapInpatientCaseNr.get(checkedDate).add(caseId);
      mapPrevMaxtreatmentlevel.get(CoronaFixedValues.ICU.getValue()).add(casePid);
    }
  }

  /**
   * Purpose of the function is to find out whether the encounter is seen as a ventilation or ecmo
   * encounter
   *
   * @param mapPrevMaxtreatmentlevel The previous maximum treatment level.
   * @param mapIcuVentilationCaseId  A map containing the case id of every ventilation case sorted
   *                                 by the date.
   * @param mapIcuEcmoCaseId         A map containing the case id of every ecmo case sorted by the
   *                                 date.
   * @param mapInpatientCaseId       A map containing the case id of every inpatient case sorted by
   *                                 the date.
   * @param procedureCode            The {@link UkbProcedure procedure.code.coding.code} that
   *                                 defines the resource as ecmo, ventilation or other procedure.
   * @param caseId                   The id of the {@link UkbEncounter} that is supposed to be
   *                                 checked.
   * @param casePid                  The Patient id of the Patient Resource attached to the
   *                                 Encounter.
   * @param isEcmo                   Check if encounter has any ecmo resources attached to it
   * @param checkedDate              The current date that is being checked.
   */
  private static void checkVentOrEcmo(Map<String, Set<String>> mapPrevMaxtreatmentlevel,
      Map<Long, Set<String>> mapIcuVentilationCaseId, Map<Long, Set<String>> mapIcuEcmoCaseId,
      Map<Long, Set<String>> mapInpatientCaseId, String procedureCode, String caseId,
      String casePid, Boolean isEcmo, Long checkedDate, InputCodeSettings inputCodeSettings) {
    // check if the case would be seen as a ventilation case
    if (inputCodeSettings.getProcedureVentilationCodes().contains(procedureCode)) {
      // check if the case was not already marked as an ecmo case
      if (!isEcmo) {
        mapInpatientCaseId.get(checkedDate).add(caseId);
        mapIcuVentilationCaseId.computeIfPresent(checkedDate, (k, v) -> {
          v.add(caseId);
          return v;
        });
        mapPrevMaxtreatmentlevel.computeIfPresent(CoronaFixedValues.ICU_VENTILATION.getValue(),
            (k, v) -> {
              v.add(casePid);
              return v;
            });
      }
    }
    // check if the case would be seen as an ecmo case
    else if (inputCodeSettings.getProcedureEcmoCodes().contains(procedureCode)) {
      mapInpatientCaseId.get(checkedDate).add(caseId);

      mapIcuEcmoCaseId.computeIfPresent(checkedDate, (k, v) -> {
        v.add(caseId);
        return v;
      });
      mapPrevMaxtreatmentlevel.computeIfPresent(CoronaFixedValues.ICU_ECMO.getValue(), (k, v) -> {
        v.add(casePid);
        return v;
      });
    }
  }
}
