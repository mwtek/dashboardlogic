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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender;

import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;

import de.ukbonn.mwtek.dashboardlogic.enums.Gender;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items {@link DiseaseDataItem
 * cumulative.inpatient.gender and cumulative.outpatient.gender}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeGenderByClass extends CumulativeGender {

  /**
   * Generic method to process patients by gender and encounter case class.
   *
   * @param ukbEncounters List of encounters.
   * @param ukbPatients List of patients.
   * @param gender The gender to filter by.
   * @param encounterClass The case class to filter by.
   * @param resultFunction Function that processes the filtered encounters and patients.
   * @param <T> Return type.
   * @return Result of processing.
   */
  public static <T> T processGenderByCaseClass(
      List<UkbEncounter> ukbEncounters,
      List<UkbPatient> ukbPatients,
      Gender gender,
      TreatmentLevels encounterClass,
      BiFunction<List<UkbEncounter>, List<UkbPatient>, T> resultFunction) {

    log.debug(
        "Started processGenderByCaseClass for class: {} and gender: {}", encounterClass, gender);
    Instant startTimer = TimerTools.startTimer();

    List<UkbEncounter> filteredEncounterList =
        ukbEncounters.parallelStream()
            .filter(
                encounter -> {
                  if (encounterClass == OUTPATIENT) {
                    // Definition outpatient for this item: ambulant or prestationary
                    return encounter.isCaseClassOutpatient();
                  } else if (encounterClass == INPATIENT) {
                    // Definition inpatient for this item: (post-)stationary or semi-stationary
                    return isInpatientBasedOnDsd(encounter);
                  }
                  return false;
                })
            .collect(Collectors.toList());

    T result = resultFunction.apply(filteredEncounterList, ukbPatients);

    TimerTools.stopTimerAndLog(startTimer, "Finished processGenderByCaseClass");
    return result;
  }

  private static boolean isInpatientBasedOnDsd(UkbEncounter encounter) {
    return encounter.isCaseClassInpatient()
        || encounter.isCaseTypePostStationary()
        || encounter.isSemiStationary();
  }

  public static Set<String> getGenderCountByCaseClass(
      List<UkbEncounter> ukbEncounters,
      List<UkbPatient> ukbPatients,
      Gender gender,
      TreatmentLevels encounterClass) {

    return processGenderByCaseClass(
        ukbEncounters,
        ukbPatients,
        gender,
        encounterClass,
        (filteredEncounters, patients) -> getGenderCount(filteredEncounters, patients, gender));
  }

  public static Set<String> getGenderPidsByCaseClass(
      List<UkbEncounter> ukbEncounters,
      List<UkbPatient> ukbPatients,
      Gender gender,
      TreatmentLevels encounterClass) {

    return processGenderByCaseClass(
        ukbEncounters,
        ukbPatients,
        gender,
        encounterClass,
        (filteredEncounters, patients) ->
            getGenderPatientIdList(filteredEncounters, patients, gender));
  }
}
