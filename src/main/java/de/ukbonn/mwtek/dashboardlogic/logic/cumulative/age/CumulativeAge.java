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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.TWELVE_DAYS_LOGIC;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.calculateAge;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items {@link DiseaseDataItem cumulative.age} including
 * the subitems (*.alive, *.dead).
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeAge extends DashboardDataItemLogic {

  /**
   * Get the ages of all disease-positive patients by a given class
   *
   * <p>Used for generating "cumulative.age"
   *
   * @param encounterClass the class of an encounter (e.g. {@link TreatmentLevels#INPATIENT})
   * @return returns list of age of all positive patients, who fulfill the caseStatus criteria
   */
  public static List<Integer> getAgeDistributionsByCaseClass(
      List<UkbEncounter> facilityEncounters,
      List<UkbPatient> patients,
      TreatmentLevels encounterClass) {
    log.debug(
        "Started getAgeDistributionsByCaseClass for encounterClass: {} with {} encounters",
        encounterClass,
        facilityEncounters.size());
    Instant startTimer = TimerTools.startTimer();
    List<Integer> resultList = new ArrayList<>();
    Set<String> pidSet = new HashSet<>();
    Set<String> stationaryPidSet = new HashSet<>();
    Set<String> ambulantPidSet = new HashSet<>();

    // get the age of each patient at the admission date from the first disease-positive case
    List<UkbEncounter> encountersPositive =
        facilityEncounters.parallelStream()
            .filter(x -> x.hasExtension(POSITIVE_RESULT.getValue()))
            .toList();

    Map<TreatmentLevels, List<UkbEncounter>> mapEncounterPos =
        Map.of(TreatmentLevels.ALL, encountersPositive);
    Map<String, Date> pidAgeMap = createPidAgeMap(mapEncounterPos);

    for (UkbEncounter encounter : encountersPositive) {
      if (encounter.isCaseClassInpatient()) {
        stationaryPidSet.add(encounter.getPatientId());
      } else if (encounter.isCaseClassOutpatient()
          && !encounter.hasExtension(TWELVE_DAYS_LOGIC.getValue())) {
        ambulantPidSet.add(encounter.getPatientId());
      }
    }

    if (encounterClass == TreatmentLevels.ALL) {
      pidSet.addAll(stationaryPidSet);
      pidSet.addAll(ambulantPidSet);
    } else {
      pidSet.addAll(
          encounterClass == TreatmentLevels.INPATIENT ? stationaryPidSet : ambulantPidSet);
    }

    // calculates age
    for (UkbPatient patient : patients) {
      if (pidSet.contains(patient.getId())) {
        Date admissionDate = pidAgeMap.get(patient.getId());
        if (patient.hasBirthDate() && admissionDate != null) {
          resultList.add(calculateAge(patient.getBirthDate(), admissionDate));
        } else {
          log.warn("Could not find a birthday in the resource of patient {}", patient.getId());
        }
      }
    }
    TimerTools.stopTimerAndLog(startTimer, "finished getAgeDistributionsByCaseClass");

    // order ascending regarding the specification
    return createCohortAgeList(resultList);
  }

  /**
   * Create a map that identifies the first admission date of a patient's first positive Covid case
   * and assigns it to the PID. This is the reference point for calculating the age.
   *
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @return Map that assigns the admission date of the patient's first c19 positive case to a pid
   */
  private static Map<String, Date> createPidAgeMap(
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass) {

    Map<String, Date> pidMap = new HashMap<>();
    for (var entry : mapPositiveEncounterByClass.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        pidMap.compute(
            encounter.getPatientId(),
            (pid, existingDate) -> getEarlierAdmissionDate(existingDate, encounter));
      }
    }
    return pidMap;
  }

  /**
   * Compares an existing admission date with a new encounter and returns the earlier one.
   *
   * @param existingDate The existing earliest admission date for the patient.
   * @param encounter The new encounter to compare.
   * @return The earlier of the two dates.
   */
  private static Date getEarlierAdmissionDate(Date existingDate, UkbEncounter encounter) {
    if (!encounter.isPeriodStartExistent()) {
      return existingDate; // Keep the existing date if no period start exists
    }
    Date newAdmissionDate = encounter.getPeriod().getStart();
    return (existingDate == null || newAdmissionDate.before(existingDate))
        ? newAdmissionDate
        : existingDate;
  }

  /**
   * Converts a list of patient ages into their corresponding age groups and returns the sorted list
   * of cohort ages.
   *
   * @param resultList a list of individual patient ages; can be empty but should not be null
   * @return a sorted list of cohort age groups; returns an empty list if input is null or empty
   */
  private static List<Integer> createCohortAgeList(List<Integer> resultList) {
    if (resultList == null || resultList.isEmpty()) {
      return Collections.emptyList();
    }
    return resultList.stream()
        .map(CoronaResultFunctionality::checkAgeGroup)
        .sorted()
        .collect(Collectors.toList());
  }
}
