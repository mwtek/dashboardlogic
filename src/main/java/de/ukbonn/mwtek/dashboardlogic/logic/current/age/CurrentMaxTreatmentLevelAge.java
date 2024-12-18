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
package de.ukbonn.mwtek.dashboardlogic.logic.current.age;

import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used for generating the data item {@link DiseaseDataItem
 * current.maxtreatmentlevel.age.* with subitems like *.normal_ward and *.age.icu}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
public record CurrentMaxTreatmentLevelAge(
    List<UkbPatient> listPatients, List<UkbEncounter> listCurrentMaxEncounter) {

  /**
   * Creates a list of ages for patients with encounters in the current maximum encounter list.
   *
   * @param mapPositiveEncounterByClass Map that assigns a list of encounters to a class.
   * @return List of ages for patients with encounters in the current maximum encounter list.
   */
  public List<Long> createCurrentMaxAgeMap(
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass) {
    // Result list to store ages
    List<Long> resultList = new ArrayList<>();

    // Map to store the first admission date for each patient in the current maximum encounter list
    Map<String, UkbEncounter> currentMaxPidAdmissionMap = new HashMap<>();

    // Convert the list of current maximum encounter PIDs to a set for efficient containment checks
    Set<String> currentMaxEncounterPidSet =
        listCurrentMaxEncounter.stream()
            .map(UkbEncounter::getPatientId)
            .collect(Collectors.toSet());

    // Iterate over encounters in the positive encounter map and assign first admission dates
    mapPositiveEncounterByClass.values().stream()
        .flatMap(List::stream)
        .filter(encounter -> currentMaxEncounterPidSet.contains(encounter.getPatientId()))
        .forEach(
            encounter ->
                CoronaResultFunctionality.assignFirstAdmissionDateToPid(
                    encounter, currentMaxPidAdmissionMap));

    // Calculate and check the age group for patients in the current maximum encounter list
    calculateAndCheckAgeGroup(currentMaxPidAdmissionMap, resultList, listPatients);

    // Sort the result list in ascending order
    resultList.sort(Comparator.naturalOrder());

    // Return the list of ages
    return resultList;
  }

  /**
   * Calculates and checks the age group for patients based on their first admission date.
   *
   * @param pidAdmissionMap Map of patient IDs to their first admission encounters.
   * @param resultList List to store the calculated ages.
   * @param listPatients List of patients for additional information.
   */
  private static void calculateAndCheckAgeGroup(
      Map<String, UkbEncounter> pidAdmissionMap,
      List<Long> resultList,
      List<UkbPatient> listPatients) {
    // Filter the patients with a valid birthday
    List<UkbPatient> validPatientsWithBirthday =
        listPatients.stream().filter(UkbPatient::hasBirthDate).toList();

    pidAdmissionMap.forEach(
        (pid, encounter) -> {
          Date birthdayPatient = findBirthdayForPatient(pid, validPatientsWithBirthday);
          if (encounter.isPeriodStartExistent() && birthdayPatient != null) {
            // Calculate the age based on the birthday and the encounter's start period
            int age =
                CoronaResultFunctionality.calculateAge(
                    birthdayPatient, encounter.getPeriod().getStart());
            long cohortAge = CoronaResultFunctionality.checkAgeGroup(age);
            resultList.add(cohortAge);
          }
        });
  }

  /**
   * Finds the birthday for a given patient ID in the list of valid patients with a birthday.
   *
   * @param patientId The ID of the patient.
   * @param validPatientsWithBirthday List of valid patients with a birthday.
   * @return The birthday of the patient or {@code null} if not found.
   */
  private static Date findBirthdayForPatient(
      String patientId, List<UkbPatient> validPatientsWithBirthday) {
    return validPatientsWithBirthday.stream()
        .filter(patient -> patientId.equals(patient.getId()))
        .map(UkbPatient::getBirthDate)
        .findFirst()
        .orElseGet(() -> null);
  }
}
