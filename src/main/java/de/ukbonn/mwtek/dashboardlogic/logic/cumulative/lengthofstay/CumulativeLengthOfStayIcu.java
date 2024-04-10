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

import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.calculateDaysInBetweenInHours;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isPatientDeceased;

import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.enums.VitalStatus;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;

/**
 * This class is used for generating the data items
 * {@link DiseaseDataItem cumulative.lengthofstay.icu} and the subitems.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeLengthOfStayIcu extends DashboardDataItemLogics {

  /**
   * Creates a map containing the length of stay in hours for every patient/encounter who was in
   * intensive care
   * <p>
   * Used for cumulative.lengthofstay.icu
   *
   * @param icuSupplyContactEncounters The icu information can be found on the Encounter.location of
   *                                   the supply contact.
   * @return A Map that links a patient id to a map that containing the length of stay in the
   * hospital and all the case ids from which this total was calculated
   */
  public static Map<String, Map<Long, Set<String>>> createIcuLengthOfStayList(
      List<UkbEncounter> icuSupplyContactEncounters) {

    log.debug("started createIcuLengthOfStayList");
    Instant startTimer = TimerTools.startTimer();
    HashMap<String, Map<Long, Set<String>>> mapResult = new HashMap<>();

    // Determination of the location IDs of all intensive care units. Only the wards are considered,
    // since in the location components within an Encounter resource, at best ward/room and bed are
    // listed with identical time periods and the stay should only be evaluated once. The highest of
    // these hierarchy levels should be sufficient.
    Set<String> icuLocationIds = LocationFilter.getIcuLocationIds(getLocations());

    // Just the positive encounters need to be counted
    List<UkbEncounter> icuSupplyContactEncountersPositive =
        icuSupplyContactEncounters.parallelStream()
            .filter(EncounterFilter::isDiseasePositive).toList();

    // iterate through every encounter and calculates amount of time spent in icu
    for (UkbEncounter encounter : icuSupplyContactEncountersPositive) {
      Long hours;
      String pid = encounter.getPatientId();

      // get all the locations that are ICU Locations, by comparing the id with the icu location ids
      List<Encounter.EncounterLocationComponent> icuEncounterLocations = new ArrayList<>();
      try {
        for (Encounter.EncounterLocationComponent location : encounter.getLocation()) {
          if (location != null && location.getLocation() != null && !location.getLocation()
              .isEmpty()) {
            if (icuLocationIds.contains(extractIdFromReference(location.getLocation()))) {
              icuEncounterLocations.add(location);
            }
          }
        }
      } catch (Exception ex) {
        log.error("Error in the createIcuLengthOfStayList generation ", ex);
      }
      // go through each Location and calculate the time the spend in ICU
      for (Encounter.EncounterLocationComponent location : icuEncounterLocations) {
        if (location.hasPeriod() && location.getPeriod().hasStart()) {
          LocalDateTime start =
              location.getPeriod().getStart().toInstant().atZone(ZoneId.systemDefault())
                  .toLocalDateTime();

          if (location.getPeriod().hasStart() && location.getPeriod().hasEnd()) {
            LocalDateTime end =
                location.getPeriod().getEnd().toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            hours = calculateDaysInBetweenInHours(start, end);
            addIcuHours(mapResult, pid, hours, encounter);
          } else if (!location.getPeriod().hasEnd()) {
            // Calculate with current Date
            hours = calculateDaysInBetweenInHours(start,
                LocalDateTime.now());
            addIcuHours(mapResult, pid, hours, encounter);
          }
        }
      }
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createIcuLengthOfStayList");
    return mapResult;
  }

  /**
   * Add a number of Icu hours to a PatientId including a check if there is already an entry for
   * this patient. If this is the case, the old value is increased by the specified number of hours.
   * [ Used by: createIcuHoursList ]
   *
   * @param mapResult  A map that assigns a map with number of Icu Hours and the underlying case ids
   *                   to a patient id
   * @param patientId  patient resource id
   * @param hoursToAdd Number of hours of Icu stay to be assigned to the respective patient ID or to
   *                   be added to the previous total.
   * @param encounter  {@link UkbEncounter} that is considered
   */
  private static void addIcuHours(Map<String, Map<Long, Set<String>>> mapResult, String patientId,
      Long hoursToAdd, UkbEncounter encounter) {
    // Check if there are already saved hours for the patient
    mapResult.compute(patientId, (key, mapHourCase) -> {
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
   * <p>
   * This method is used by cumulative.lengthofstay.icu.alive and dead.
   *
   * @param vitalStatus      Vital status of a patient (e.g. {@link VitalStatus#ALIVE})
   * @param mapIcuLengthList Map with the number of ICU stay hours per patientId
   * @param mapIcu           Map that assigns a list of case numbers to an ICU treatment level
   *                         class
   * @return Map with the number of ICU stay hours of all non-deceased patients per PatientId
   */
  public static Map<String, Map<Long, Set<String>>> createIcuLengthListByVitalstatus(
      VitalStatus vitalStatus, Map<String, Map<Long, Set<String>>> mapIcuLengthList,
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu) {

    log.debug("started createIcuLengthListByVitalstatus");
    Instant startTime = TimerTools.startTimer();

    Map<String, Map<Long, Set<String>>> mapResult = new HashMap<>();

    Set<UkbEncounter> icuEncounters = new HashSet<>();
    icuEncounters.addAll(mapIcu.get(ICU));
    icuEncounters.addAll(mapIcu.get(ICU_VENTILATION));
    icuEncounters.addAll(mapIcu.get(ICU_ECMO));

    for (UkbEncounter encounter : icuEncounters) {
      String pid = encounter.getPatientId();
      boolean shouldInclude = shouldIncludeEncounter(vitalStatus, encounter);

      if (shouldInclude) {
        mapResult.put(pid, mapIcuLengthList.get(pid));
      }
    }

    TimerTools.stopTimerAndLog(startTime, "finished createIcuLengthListByVitalstatus");
    return mapResult;
  }

  /**
   * Determines whether an encounter should be included based on the vital status of the patient.
   *
   * @param vitalStatus The {@link VitalStatus} of the patient
   * @param encounter   The encounter to be evaluated
   * @return True if the encounter should be included, otherwise false
   */
  private static boolean shouldIncludeEncounter(VitalStatus vitalStatus, UkbEncounter encounter) {
    switch (vitalStatus) {
      case DEAD -> {
        return isPatientDeceased(encounter);
      }
      case ALIVE -> {
        return !encounter.getHospitalization().hasDischargeDisposition() || !isPatientDeceased(
            encounter);
      }
    }
    return false; // Do not include encounter if vital status is null
  }

}
