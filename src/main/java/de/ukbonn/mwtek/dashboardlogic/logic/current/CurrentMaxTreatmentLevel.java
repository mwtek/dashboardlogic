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
package de.ukbonn.mwtek.dashboardlogic.logic.current;

import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isActive;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isDiseasePositive;

import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item
 * {@link DiseaseDataItem current.maxtreatmentlevel}.
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
   * @param treatmentLevel Treatmentlevel as a separation criterion.
   * @return All current ongoing inpatient disease-positive encounters for the given
   * maxtreatmentlevel.
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getNumberOfCurrentMaxTreatmentLevel(
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu,
      TreatmentLevels treatmentLevel) {
    log.debug("started getNumberOfCurrentMaxTreatmentLevel");
    Instant startTimer = TimerTools.startTimer();

    if (facilityContactsInpatients == null) {
      facilityContactsInpatients = listEncounters.parallelStream()
          .filter(EncounterFilter::isCaseClassInpatient)
          .toList();
    }

    List<UkbEncounter> results = new ArrayList<>();
    List<UkbEncounter> icuEncounters = mapIcu.get(ICU);
    List<UkbEncounter> icuVentEncounters = mapIcu.get(ICU_VENTILATION);
    List<UkbEncounter> ecmoEncounters = mapIcu.get(ICU_ECMO);
    boolean isPositive, isActive;

    try {
      switch (treatmentLevel) {
        case INPATIENT:
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isDiseasePositive(currentEncounter);
            isActive = isActive(currentEncounter);

            if (isActive && isPositive && !icuEncounters.contains(currentEncounter)
                && !icuVentEncounters.contains(currentEncounter)
                && !ecmoEncounters.contains(currentEncounter)) {
              results.add(currentEncounter);
            }
          }
          break;

        case ICU:
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isDiseasePositive(currentEncounter);
            isActive = isActive(currentEncounter);

            if (!icuVentEncounters.contains(currentEncounter) && !ecmoEncounters.contains(
                currentEncounter)
                && icuEncounters.contains(currentEncounter) && isActive && isPositive) {
              results.add(currentEncounter);
            }
          }
          break;

        case ICU_VENTILATION:
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isDiseasePositive(currentEncounter);
            isActive = isActive(currentEncounter);

            if (isActive && isPositive && !ecmoEncounters.contains(currentEncounter)
                && icuVentEncounters.contains(currentEncounter)) {
              results.add(currentEncounter);
            }
          }
          break;

        case ICU_ECMO:
          for (UkbEncounter currentEncounter : facilityContactsInpatients) {
            isPositive = isDiseasePositive(currentEncounter);
            isActive = isActive(currentEncounter);

            if (isActive && isPositive && ecmoEncounters.contains(currentEncounter)) {
              results.add(currentEncounter);
            }
          }
          break;
      }
    } catch (Exception ex) {
      log.error("Error in the getNumberOfCurrentMaxTreatmentLevel method. ", ex);
    }

    TimerTools.stopTimerAndLog(startTimer, "finished getNumberOfCurrentMaxTreatmentLevel");
    return results;
  }
}
