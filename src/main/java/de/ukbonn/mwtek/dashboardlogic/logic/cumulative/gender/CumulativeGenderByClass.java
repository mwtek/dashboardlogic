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

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items {@link CoronaDataItem
 * cumulative.inpatient.gender and cumulative.outpatient.gender}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeGenderByClass extends CumulativeGender {

  public List<UkbEncounter> listEncounters;
  public List<UkbPatient> listPatients;

  public CumulativeGenderByClass(List<UkbEncounter> listEncounters, List<UkbPatient> listPatients) {
    super(listEncounters, listPatients);
    this.listEncounters = listEncounters;
    this.listPatients = listPatients;
  }

  /**
   * Determination of the number of patients per gender and case status (e.g. inpatient)
   * <p>
   * called by both data items
   *
   * @param gender    the gender type (e.g. male) to be counted
   * @param caseClass the case class (e.g. inpatient) to be counted
   * @return number of genders per gender and case status
   */
  public Number getGenderCountByCaseClass(String gender, String caseClass) {
    log.debug(
        "started getGenderCountByCaseClass for class: " + caseClass + " and gender: " + gender);
    Instant startTimer = TimerTools.startTimer();
    List<UkbEncounter> filteredEncounterList = new ArrayList<>();
    List<UkbEncounter> listEncounterByClass = new ArrayList<>();

    boolean isAmbulant = caseClass.equals(CoronaFixedValues.OUTPATIENT_ITEM.getValue());
    boolean isStationary = caseClass.equals(CoronaFixedValues.INPATIENT_ITEM.getValue());

    if (isAmbulant) {
      listEncounterByClass = listEncounters.parallelStream()
          .filter(CoronaResultFunctionality::isCaseClassOutpatient)
          .collect(Collectors.toList());
    } else if (isStationary) {
      listEncounterByClass = listEncounters.parallelStream()
          .filter(CoronaResultFunctionality::isCaseClassInpatient).collect(Collectors.toList());
    }

    for (UkbEncounter encounter : listEncounterByClass) {
      if (isAmbulant) {
        filteredEncounterList.add(encounter);
      } else {
        filteredEncounterList.add(encounter);
      }
    }

    TimerTools.stopTimerAndLog(startTimer, "finished getGenderCountByCaseClass");
    return getGenderCount(filteredEncounterList, listPatients, gender);
  }

}
