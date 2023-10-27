/*
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */
package de.ukbonn.mwtek.dashboardlogic.logic.current;

import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isActive;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isCovidPositive;

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
 * This class is used for generating the data item
 * {@link CoronaDataItem current.maxtreatmentlevel}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CurrentMaxTreatmentLevel {

  public List<UkbEncounter> listEncounters;
  private List<UkbEncounter> facilityContactsInpatients;

  public CurrentMaxTreatmentLevel(List<UkbEncounter> listEncounter) {
    this.listEncounters = listEncounter;
  }

  /**
   * Search for the maximum treatment level of the current ongoing encounter, for the
   * current.maxtreatmentlevel
   *
   * @param mapIcu         Map that assigns a list of case numbers to an ICU treatment level class.
   * @param treatmentLevel Treatmentlevel as separation criterion.
   * @return All current ongoing inpatient c19 positive encounters for the given maxtreatmentlevel.
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getNumberOfCurrentMaxTreatmentLevel(
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel) {
    log.debug("started getNumberOfCurrentMaxTreatmentLevel");
    Instant startTimer = TimerTools.startTimer();

    // Initialize list that filters on inpatient cases
    if (facilityContactsInpatients == null) {
      facilityContactsInpatients =
          listEncounters.parallelStream().filter(CoronaResultFunctionality::isCaseClassInpatient)
              .toList();
    }

    List<UkbEncounter> listResult = new ArrayList<>();
    List<UkbEncounter> listIcuEncounter = mapIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listIcuVentEncounter =
        mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    // Used to check if case is flagged as covid positive and if the case status is "in-progress".
    boolean isPositive;
    boolean isActive;

    // Check whether the current encounters had a higher treatmentlevel before.
    try {
      switch (treatmentLevel) {
        case INPATIENT_ITEM -> {
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isCovidPositive(currentEncounter);
            isActive = isActive(currentEncounter);

            // check currentEncounter class, flagging, and appearance in icu map
            if (isActive && isPositive && !listIcuEncounter.contains(currentEncounter)) {
              if (!listIcuEncounter.contains(currentEncounter)) {
                if (!listIcuVentEncounter.contains(currentEncounter)
                    && !listEcmoEncounter.contains(
                    currentEncounter)) {
                  listResult.add(currentEncounter);
                }
              }
            }
          }
        }
        // ICU: Same procedure as above
        case ICU -> {
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isCovidPositive(currentEncounter);
            isActive = isActive(currentEncounter);

            if (!listIcuVentEncounter.contains(currentEncounter) && !listEcmoEncounter.contains(
                currentEncounter) && listIcuEncounter.contains(currentEncounter)) {
              if (isActive && isPositive) {
                listResult.add(currentEncounter);
              }
            }
          }
        }
        // Ventilation
        case ICU_VENTILATION -> {
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isCovidPositive(currentEncounter);
            isActive = isActive(currentEncounter);

            // Checking the ICU status, the status of the case and checking covid flag
            if (isActive && isPositive) {
              if (!listEcmoEncounter.contains(
                  currentEncounter) && listIcuVentEncounter.contains(currentEncounter)) {
                listResult.add(currentEncounter);
              }
            }
          }
        }
        // ECMO
        case ICU_ECMO -> {
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isCovidPositive(currentEncounter);
            isActive = isActive(currentEncounter);

            if (isActive && isPositive && listEcmoEncounter.contains(currentEncounter)) {
              listResult.add(currentEncounter);
            }
          }
        }
      }
    } catch (Exception ex) {
      log.error("Error in the getNumberOfCurrentMaxTreatmentLevel method. ", ex);
    }
    TimerTools.stopTimerAndLog(startTimer, "finished getNumberOfCurrentMaxTreatmentLevel");
    return listResult;
  }
}
