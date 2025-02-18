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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay;

import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.calculateDaysInBetweenInHours;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.isLocationReferenceExisting;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;

/**
 * This class is used for generating the data items {@link DiseaseDataItem
 * cumulative.lengthofstay.icu} and the subitems.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeLengthOfStayIcu extends DashboardDataItemLogic {

  /**
   * Creates a map containing the length of stay in hours for every patient/encounter who was in
   * intensive care
   *
   * <p>Used for cumulative.lengthofstay.icu
   *
   * @param icuSupplyContactEncounters The icu information can be found on the Encounter.location of
   *     the supply contact.
   * @return A Map that links a patient id to a map that containing the length of stay in the
   *     hospital and all the case ids from which this total was calculated
   */
  public static Map<String, Map<Long, Set<String>>> createIcuLengthOfStayList(
      List<UkbEncounter> icuSupplyContactEncounters, List<UkbLocation> locations) {

    log.debug("Started createIcuLengthOfStayList");
    Instant startTimer = TimerTools.startTimer();

    // If locations or ICU encounters are missing, return an empty map
    if (locations == null || icuSupplyContactEncounters == null) {
      log.warn("Missing data: Locations or ICU encounters are null.");
      return Collections.emptyMap();
    }

    // Get all ICU location IDs
    Set<String> icuLocationIds = LocationFilter.getIcuLocationIds(locations);

    // Filter positive encounters and pre-filter ICU locations
    Map<String, List<Encounter.EncounterLocationComponent>> encounterLocationMap =
        icuSupplyContactEncounters.stream()
            .filter(EncounterFilter::isDiseasePositive)
            .collect(
                Collectors.toMap(
                    UkbEncounter::getId,
                    encounter ->
                        encounter.getLocation().stream()
                            .filter(
                                loc ->
                                    isLocationReferenceExisting(loc)
                                        && icuLocationIds.contains(
                                            extractIdFromReference(loc.getLocation())))
                            .toList()));

    Map<String, Map<Long, Set<String>>> mapResult = new HashMap<>();
    boolean anyLocationPeriodMissing = false;

    for (UkbEncounter encounter : icuSupplyContactEncounters) {
      String pid = encounter.getPatientId();
      List<Encounter.EncounterLocationComponent> icuLocations =
          encounterLocationMap.get(encounter.getId());

      if (icuLocations == null || icuLocations.isEmpty()) {
        continue; // Skip if no valid ICU locations exist for this encounter
      }

      for (Encounter.EncounterLocationComponent location : icuLocations) {
        boolean locationPeriodMissing = !location.hasPeriod();
        if (locationPeriodMissing) {
          anyLocationPeriodMissing = true;
        }

        Period period = getValidPeriod(location, encounter);
        if (period == null || !period.hasStart()) { // ✅ Added check for period.getStart()
          log.warn("No valid period start found for Encounter: {}", encounter.getId());
          continue;
        }

        LocalDateTime start =
            period.getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime end =
            period.hasEnd()
                ? period.getEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now();

        long hours = calculateDaysInBetweenInHours(start, end);

        addIcuHours(mapResult, pid, hours, encounter);
      }
    }

    if (anyLocationPeriodMissing) {
      log.info(
          "At least one encounter.location.period was null, fallback to encounter.period was used instead.");
    }

    TimerTools.stopTimerAndLog(startTimer, "Finished createIcuLengthOfStayList");
    return mapResult;
  }

  /**
   * Add a number of Icu hours to a PatientId including a check if there is already an entry for
   * this patient. If this is the case, the old value is increased by the specified number of hours.
   * [ Used by: createIcuHoursList ]
   *
   * @param mapResult A map that assigns a map with number of Icu Hours and the underlying case ids
   *     to a patient id
   * @param patientId patient resource id
   * @param hoursToAdd Number of hours of Icu stay to be assigned to the respective patient ID or to
   *     be added to the previous total.
   * @param encounter {@link UkbEncounter} that is considered
   */
  private static void addIcuHours(
      Map<String, Map<Long, Set<String>>> mapResult,
      String patientId,
      Long hoursToAdd,
      UkbEncounter encounter) {
    // Check if there are already saved hours for the patient
    mapResult.compute(
        patientId,
        (key, mapHourCase) -> {
          if (mapHourCase == null) {
            // If the patient is not present, create a new entry
            Set<String> listCaseId = new HashSet<>();
            listCaseId.add(encounter.getId());
            Map<Long, Set<String>> mapTemp = new HashMap<>();
            mapTemp.put(hoursToAdd, listCaseId);
            return mapTemp;
          } else {
            // The patient is already present, update the hours and the list of cases
            Map.Entry<Long, Set<String>> entry = mapHourCase.entrySet().iterator().next();
            Long keyNew = entry.getKey() + hoursToAdd;
            Set<String> listCases = entry.getValue();
            listCases.add(encounter.getId());
            mapHourCase.clear();
            mapHourCase.put(keyNew, listCases);
            return mapHourCase;
          }
        });
  }

  /**
   * Creates a map containing the length of stay in hours for ICU cases where the patients did not
   * decease.
   *
   * <p>This method is used by cumulative.lengthofstay.icu.alive and dead.
   *
   * @param vitalStatus Vital status of a patient (e.g. {@link VitalStatus#ALIVE})
   * @param mapIcuLengthList Map with the number of ICU stay hours per patientId
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @return A map containing filtered ICU length lists based on the given vital status.
   */
  public static Map<String, Map<Long, Set<String>>> createIcuLengthListByVitalstatus(
      VitalStatus vitalStatus,
      Map<String, Map<Long, Set<String>>> mapIcuLengthList,
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu) {

    log.debug("started createIcuLengthListByVitalstatus");
    Instant startTime = TimerTools.startTimer();

    Map<String, Map<Long, Set<String>>> mapResult = new HashMap<>();

    // Merge ICU encounters from different treatment levels into a single set
    Set<UkbEncounter> icuEncounters = new HashSet<>();
    mapIcu.values().forEach(icuEncounters::addAll);

    // Iterate through each ICU supply contact encounter
    for (UkbEncounter encounter : icuEncounters) {
      String pid = encounter.getPatientId();
      // Check if the encounter should be included based on the given vital status
      boolean shouldInclude = shouldIncludeEncounter(vitalStatus, encounter);
      if (shouldInclude && mapIcuLengthList.containsKey(pid)) {
        // Add or update the filtered ICU length list for the patient in the result map
        mapResult.computeIfAbsent(pid, k -> new HashMap<>(mapIcuLengthList.get(k)));
      }
    }
    TimerTools.stopTimerAndLog(startTime, "finished createIcuLengthListByVitalstatus");
    return mapResult;
  }

  /**
   * Determines whether an encounter should be included based on the vital status of the patient.
   *
   * @param vitalStatus The {@link VitalStatus} of the patient
   * @param encounter The encounter to be evaluated
   * @return True if the encounter should be included, otherwise false
   */
  private static boolean shouldIncludeEncounter(VitalStatus vitalStatus, UkbEncounter encounter) {
    switch (vitalStatus) {
      case DEAD -> {
        return encounter.isPatientDeceased();
      }
      case ALIVE -> {
        return !encounter.getHospitalization().hasDischargeDisposition()
            || !encounter.isPatientDeceased();
      }
    }
    return false; // Do not include encounter if vital status is null
  }
}
