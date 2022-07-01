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
package de.ukbonn.mwtek.dashboardlogic.logic.current;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link CoronaDataItem current.maxtreatmentlevel}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CurrentMaxTreatmentLevel {

  public List<UkbEncounter> listEncounters = new ArrayList<>();

  public CurrentMaxTreatmentLevel(List<UkbEncounter> listEncounter) {
    this.listEncounters = listEncounter;
  }

  /**
   * Search for the maximum treatment level of the current ongoing encounter, for the
   * current.maxtreatmentlevel
   *
   * @param mapIcu         Map that assigns a list of case numbers to an ICU treatment level class
   * @param treatmentLevel TreatmentLevel as separation criterion
   * @return All current ongoing inpatient c19 positive encounters for the given Maxtreatmentlevel
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getNumberOfCurrentMaxTreatmentLevel(
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel) {
    log.debug("started getNumberOfCurrentMaxTreatmentLevel");
    Instant startTimer = TimerTools.startTimer();

    List<UkbEncounter> listEncountersInpatients =
        listEncounters.parallelStream().filter(CoronaResultFunctionality::isCaseClassInpatient)
            .toList();

    List<UkbEncounter> listResult = new ArrayList<>();
    List<UkbEncounter> listIcuEncounter = mapIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter =
        mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    // Used to check if Positive flag is set and if Period has no end
    boolean isPositive;
    boolean hasEnd;

    // check whether the current encounters had a higher treatmentlevel before
    try {
      switch (treatmentLevel) {
        case STATIONARY_ITEM:
          for (UkbEncounter encounter : listEncountersInpatients) {
            isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = encounter.getPeriod().hasEnd();

            // check encounter class, flagging, and appearance in icu map
            if (!hasEnd) {
              if (isPositive) {
                if (!listIcuEncounter.contains(encounter)) {
                  if (!listVentilationEncounter.contains(encounter) && !listEcmoEncounter.contains(
                      encounter)) {
                    listResult.add(encounter);
                  }
                }
              }
            }
          }
          break;
        // ICU: Same procedure as above
        case ICU:
          for (UkbEncounter currentEncounter : listEncountersInpatients) {
            isPositive =
                currentEncounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = currentEncounter.getPeriod().hasEnd();

            if (!listVentilationEncounter.contains(currentEncounter) && !listEcmoEncounter.contains(
                currentEncounter) && listIcuEncounter.contains(currentEncounter)) {
              if (!hasEnd && isPositive) {
                listResult.add(currentEncounter);
              }
            }
          }
          break;
        // Ventilation
        case ICU_VENTILATION:
          for (UkbEncounter currentEncounter : listEncountersInpatients) {
            isPositive =
                currentEncounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = currentEncounter.getPeriod().hasEnd();

            // check icu appearance, end date and flag
            if (!hasEnd && isPositive) {
              if (!listEcmoEncounter.contains(
                  currentEncounter) && listVentilationEncounter.contains(currentEncounter)) {
                listResult.add(currentEncounter);
              }
            }
          }
          break;
        // ECMO
        case ICU_ECMO:
          for (UkbEncounter currentEncounter : listEncountersInpatients) {
            isPositive =
                currentEncounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = currentEncounter.getPeriod().hasEnd();

            if (!hasEnd && isPositive && listEcmoEncounter.contains(currentEncounter)) {
              listResult.add(currentEncounter);
            }
          }
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    TimerTools.stopTimerAndLog(startTimer, "finished getNumberOfCurrentMaxTreatmentLevel");
    return listResult;
  }
}
