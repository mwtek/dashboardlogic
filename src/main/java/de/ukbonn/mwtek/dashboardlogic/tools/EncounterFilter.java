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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.FlaggingExtension.POSITIVE_EXTENSION;

import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/** Various auxiliary methods that affect the encounter resources. */
@Slf4j
public class EncounterFilter {

  /** Determines whether the passed encounter instance has a disease-positive flag. */
  public static boolean isDiseasePositive(UkbEncounter encounter) {
    return encounter.hasExtension(POSITIVE_RESULT.getValue());
  }

  public static List<UkbEncounter> getPositiveCurrentlyOnIcuWardEncounters(
      Collection<UkbEncounter> supplyContactEncounters, List<UkbLocation> locations) {
    return supplyContactEncounters.stream()
        .filter(EncounterFilter::isDiseasePositive)
        .filter(UkbEncounter::isActive)
        .filter(x -> x.isCurrentlyOnIcuWard(LocationFilter.getIcuLocationIds(locations)))
        .toList();
  }

  /**
   * Retrieves a set of encounter IDs that match the specified ICD codes.
   *
   * @param conditions The list of conditions.
   * @param icdCodes The list of ICD codes to match.
   * @return A set of matching encounter IDs.
   */
  public static Set<String> getEncounterIdsByIcdCodes(
      List<UkbCondition> conditions, List<String> icdCodes) {
    return FhirConditionTools.getConditionsByIcdCodes(conditions, icdCodes).stream()
        .map(UkbCondition::getCaseId)
        .collect(Collectors.toSet());
  }

  /**
   * Filters encounters based on a set of IDs.
   *
   * @param encounters The list of encounters to filter.
   * @param ids The set of encounter IDs to retain.
   * @return A set of filtered encounters.
   */
  public static Set<UkbEncounter> filterEncountersByIds(
      List<UkbEncounter> encounters, Set<String> ids) {
    return encounters.parallelStream()
        .filter(encounter -> ids.contains(encounter.getId()))
        .collect(Collectors.toSet());
  }

  public static List<UkbEncounter> filterEncounterByAge(
      List<UkbEncounter> encounters, List<UkbPatient> patients, int upperAgeBorder) {

    // Create a map for quick patient lookup, only including patients with a birthdate
    Map<String, UkbPatient> patientMap =
        patients.stream()
            .filter(UkbPatient::hasBirthDate)
            .collect(Collectors.toMap(UkbPatient::getId, patient -> patient));

    // Filter encounters based on patient age
    return encounters.parallelStream()
        .filter(
            encounter -> {
              UkbPatient patient = patientMap.get(encounter.getPatientId());
              if (patient == null) {
                log.warn("No patient found for encounter ID {}", encounter.getId());
                return false;
              }
              Date birthDate = patient.getBirthDate();
              if (birthDate == null) {
                log.warn(
                    "Patient ID {} has no birth date, skipping encounter {}",
                    patient.getId(),
                    encounter.getId());
                return false;
              }
              int age =
                  DateTools.calcYearsBetweenDates(encounter.getPeriod().getStart(), birthDate);
              log.trace(
                  "Encounter ID {}: Patient age on {} is {}",
                  encounter.getId(),
                  encounter.getPeriod().getStart(),
                  age);

              return age <= upperAgeBorder;
            })
        .collect(Collectors.toList());
  }

  /**
   * Checks if a given encounter is valid based on the patient's age at admission. An encounter is
   * considered valid if it exists in the provided case data map and the age at admission is below
   * the specified upper age limit.
   *
   * @param coreCaseDataByEncounterIdMap A map containing core case data indexed by encounter IDs.
   *     This map is used to check if the encounter is part of the dataset.
   * @param encounter The encounter to be validated against the age criteria.
   * @param upperAgeBorder The upper age limit for the encounter to be considered valid.
   * @return true if the encounter is valid (exists in the map, and the age at admission is below
   *     the upper age limit); false otherwise.
   */
  public static boolean isEncounterValidByAge(
      Map<String, CoreCaseData> coreCaseDataByEncounterIdMap,
      UkbEncounter encounter,
      int upperAgeBorder) {
    // If the current case is not part of the map, the case got already filtered by age
    if (!coreCaseDataByEncounterIdMap.containsKey(encounter.getId())) {
      return false;
    }
    return coreCaseDataByEncounterIdMap.get(encounter.getId()).getAgeAtAdmission() < upperAgeBorder;
  }

  /**
   * Filters the list of {@code UkbEncounter} objects to return only inpatient facility encounters.
   *
   * <p>An encounter is considered an inpatient facility encounter if it meets the following
   * criteria:
   *
   * <ul>
   *   <li>It is a facility contact ({@code isFacilityContact()} returns {@code true}).
   *   <li>It is not an outpatient case ({@code isCaseClassOutpatient()} returns {@code false}).
   *   <li>It is not semi-stationary ({@code isSemiStationary()} returns {@code false}).
   * </ul>
   *
   * Additionally, the method adds a positive extension to each selected encounter.
   *
   * @param ukbEncounters the list of encounters to filter
   * @return a list of inpatient facility encounters with the positive extension applied
   */
  public static List<UkbEncounter> getInpatientFacilityEncounters(
      List<UkbEncounter> ukbEncounters) {
    return ukbEncounters.parallelStream()
        .filter(UkbEncounter::isFacilityContact)
        .filter(EncounterFilter::isNotOutpatient)
        .filter(EncounterFilter::isNotSemiStationary)
        .map(EncounterFilter::addPositiveExtension)
        .collect(Collectors.toList());
  }

  /**
   * Checks whether an encounter is not classified as an outpatient case.
   *
   * @param encounter the encounter to check
   * @return {@code true} if the encounter is not an outpatient case, otherwise {@code false}
   */
  private static boolean isNotOutpatient(UkbEncounter encounter) {
    return !encounter.isCaseClassOutpatient();
  }

  /**
   * Checks whether an encounter is not semi-stationary.
   *
   * @param encounter the encounter to check
   * @return {@code true} if the encounter is not semi-stationary, otherwise {@code false}
   */
  private static boolean isNotSemiStationary(UkbEncounter encounter) {
    return !encounter.isSemiStationary();
  }

  /**
   * Adds a positive extension to the given encounter.
   *
   * @param encounter the encounter to modify
   * @return the modified encounter with the positive extension added
   */
  private static UkbEncounter addPositiveExtension(UkbEncounter encounter) {
    encounter.addExtension(POSITIVE_EXTENSION);
    return encounter;
  }
}
