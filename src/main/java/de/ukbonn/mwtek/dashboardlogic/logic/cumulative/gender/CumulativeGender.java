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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.Gender.DIVERSE;
import static de.ukbonn.mwtek.dashboardlogic.enums.Gender.FEMALE;
import static de.ukbonn.mwtek.dashboardlogic.enums.Gender.MALE;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.Gender;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem cumulative.gender}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeGender extends DashboardDataItemLogic {

  /**
   * Count number of gender of the diesease-positive patients
   *
   * <p>called by "cumulative.gender"
   *
   * @param filteredEncounters A list with {@link UkbEncounter} resources
   * @param gender The gender type (e.g. male) to be counted
   * @return Frequency of gender searched across all patients who are covid positive.
   */
  public static Set<String> getGenderCount(
      List<UkbEncounter> filteredEncounters, List<UkbPatient> patients, Gender gender) {
    return getGenderPatientIdList(filteredEncounters, patients, gender);
  }

  /**
   * Generates a list with all patient ids of the given gender of the diesease-positive patients
   *
   * <p>called by "cumulative.gender"
   *
   * @param filteredEncounters A list with {@link UkbEncounter} resources
   * @param gender The gender type (e.g. male) to be counted
   * @return Frequency of gender searched across all patients who are covid positive.
   */
  public static Set<String> getGenderPatientIdList(
      List<UkbEncounter> filteredEncounters, List<UkbPatient> patients, Gender gender) {
    log.debug("Started genderCounting for gender: " + gender);
    Instant startTimer = TimerTools.startTimer();

    // get all the pids from the positive marked encounters
    Set<String> positivePids =
        filteredEncounters.parallelStream()
            .filter(x -> x.hasExtension(POSITIVE_RESULT.getValue()))
            .map(UkbEncounter::getPatientId)
            .collect(Collectors.toSet());

    // collect all the positive patient ids
    Set<String> resultSet =
        patients.parallelStream()
            .filter(
                x -> x.hasGender() && x.getGender().toCode().equalsIgnoreCase(gender.getValue()))
            .map(UkbPatient::getId)
            .filter(positivePids::contains)
            .collect(Collectors.toSet());

    TimerTools.stopTimerAndLog(startTimer, "Finished genderCounting");
    return resultSet;
  }

  public static Gender translateGenderSpecIntoEnum(DashboardLogicFixedValues dashboardSpecGender) {
    switch (dashboardSpecGender) {
      case MALE_SPECIFICATION -> {
        return MALE;
      }
      case FEMALE_SPECIFICATION -> {
        return FEMALE;
      }
      case DIVERSE_SPECIFICATION -> {
        return DIVERSE;
      }
    }
    return null;
  }
}
