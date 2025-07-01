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
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_UNDIFF;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem
 * current.maxtreatmentlevel}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CurrentMaxTreatmentLevel extends DashboardDataItemLogic {

  private List<UkbEncounter> facilityContactsInpatientPositive;

  /**
   * Searches for the maximum treatment level of the current ongoing encounter for the given
   * treatment level.
   *
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class.
   * @param facilityContacts List of facility encounters.
   * @param treatmentLevel The treatment level as a separation criterion.
   * @param useIcuUndiff Boolean flag indicating whether ICU_UNDIFFERENTIATED should be used instead
   *     of ICU, ICU_VENTILATION, and ICU_ECMO.
   * @return All current ongoing inpatient disease-positive encounters for the given treatment
   *     level.
   */
  public List<UkbEncounter> getNumberOfCurrentMaxTreatmentLevel(
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu,
      List<UkbEncounter> facilityContacts,
      TreatmentLevels treatmentLevel,
      boolean useIcuUndiff) {

    log.debug("Started getNumberOfCurrentMaxTreatmentLevel");
    Instant startTimer = TimerTools.startTimer();

    if (facilityContactsInpatientPositive == null) {
      facilityContactsInpatientPositive =
          facilityContacts.stream()
              .filter(UkbEncounter::isCaseClassInpatientOrShortStay)
              .filter(UkbEncounter::isActive)
              .filter(EncounterFilter::isDiseasePositive)
              .collect(Collectors.toList());
    }
    List<UkbEncounter> results;

    // ICU lists based on whether ICU_UNDIFFERENTIATED should be used
    List<UkbEncounter> icuUndiffEncounters =
        useIcuUndiff ? mapIcu.get(ICU_UNDIFF) : Collections.emptyList();
    List<UkbEncounter> icuEncounters = useIcuUndiff ? Collections.emptyList() : mapIcu.get(ICU);
    List<UkbEncounter> icuVentEncounters =
        useIcuUndiff ? Collections.emptyList() : mapIcu.get(ICU_VENTILATION);
    List<UkbEncounter> ecmoEncounters =
        useIcuUndiff ? Collections.emptyList() : mapIcu.get(ICU_ECMO);

    results =
        facilityContactsInpatientPositive.stream()
            .filter(
                encounter ->
                    switch (treatmentLevel) {
                      // When icu_undiff is used its only upper hierarchy level to check
                      case NORMAL_WARD ->
                          useIcuUndiff
                              ? !icuUndiffEncounters.contains(encounter)
                              : !icuEncounters.contains(encounter)
                                  && !icuVentEncounters.contains(encounter)
                                  && !ecmoEncounters.contains(encounter);
                      case ICU ->
                          icuEncounters.contains(encounter)
                              && !icuVentEncounters.contains(encounter)
                              && !ecmoEncounters.contains(encounter);
                      case ICU_VENTILATION ->
                          icuVentEncounters.contains(encounter)
                              && !ecmoEncounters.contains(encounter);
                      case ICU_ECMO -> ecmoEncounters.contains(encounter);
                      case ICU_UNDIFF -> icuUndiffEncounters.contains(encounter);
                      default ->
                          throw new IllegalStateException("Unexpected value: " + treatmentLevel);
                    })
            .collect(Collectors.toList());

    TimerTools.stopTimerAndLog(startTimer, "Finished getNumberOfCurrentMaxTreatmentLevel");
    return results;
  }
}
