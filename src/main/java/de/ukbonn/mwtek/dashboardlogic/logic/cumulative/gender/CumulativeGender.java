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

package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link CoronaDataItem cumulative.gender}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeGender {

  List<UkbEncounter> listEncounters;
  List<UkbPatient> listPatients;

  public CumulativeGender(List<UkbEncounter> listEncounters, List<UkbPatient> listPatients) {
    this.listEncounters = listEncounters;
    this.listPatients = listPatients;
  }

  /**
   * Count number of gender of the covid patients
   * <p>
   * called by "cumulative.gender"
   *
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @param listPatients   A list with {@link UkbPatient} resources
   * @param gender         The gender type (e.g. male) to be counted
   * @return Frequency of gender searched across all patients who are covid positive.
   */
  public Number getGenderCount(List<UkbEncounter> listEncounters, List<UkbPatient> listPatients,
      String gender) {
    log.debug("started genderCounting for gender: " + gender);
    Instant startTimer = TimerTools.startTimer();

    // get all the pids from the positive marked encounters
    Set<String> positivePids = listEncounters.parallelStream()
        .filter(x -> x.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue()))
        .map(UkbEncounter::getPatientId).collect(Collectors.toSet());

    // collect all the positive patient ids
    Set<String> resultSet = listPatients.parallelStream()
        .filter(x -> x.hasGender() && x.getGender().toCode().equalsIgnoreCase(gender))
        .map(UkbPatient::getId).filter(positivePids::contains).collect(Collectors.toSet());

    TimerTools.stopTimerAndLog(startTimer, "finished genderCounting");
    return resultSet.size();
  }

}
