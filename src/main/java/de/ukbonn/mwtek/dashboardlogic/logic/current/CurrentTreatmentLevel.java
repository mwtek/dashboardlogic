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

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hl7.fhir.r4.model.Procedure;

/**
 * This class is used for generating the data item {@link CoronaDataItem current.treatmentlevel}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

public class CurrentTreatmentLevel {

  private final List<UkbEncounter> listEncounters;
  private final List<UkbProcedure> listIcuProcedures;

  public CurrentTreatmentLevel(List<UkbEncounter> listEncounter,
      List<UkbProcedure> listIcuProcedure) {
    this.listEncounters = listEncounter;
    this.listIcuProcedures = listIcuProcedure;
  }

  /**
   * Creation of a list with the current inpatient c19-positive encounters that are currently on a
   * standard ward [needed for the data item current.treatmentlevel]
   *
   * @param mapCurrentIcu A list of all current inpatient c19-positive cases separated by treatment
   *                      level
   * @return List with ongoing inpatient encounter that are currently on a standard ward
   */
  public List<UkbEncounter> getCurrentStandardWardEncounter(
      Map<String, List<UkbEncounter>> mapCurrentIcu) {
    List<UkbEncounter> resultList = new ArrayList<>();
    List<UkbEncounter> listIcuEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter =
        mapCurrentIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    // just a check of the inpatient encounters is needed here.
    List<UkbEncounter> listEncounterWithoutOutpatients =
        listEncounters.parallelStream().filter(CoronaResultFunctionality::isCaseClassInpatient)
            .toList();

    // check inpatient
    try {
      listEncounterWithoutOutpatients.forEach(e -> {

        AtomicBoolean stationary = new AtomicBoolean(false);
        // Check if encounter has no end date and has positive flagging
        if (!e.getPeriod().hasEnd() && e.hasExtension(
            CoronaFixedValues.POSITIVE_RESULT.getValue())) {
          // Checking if encounter has a higher treatmentlevel
          if (!listIcuEncounter.contains(e)) {
            if (!listVentilationEncounter.contains(e) && !listEcmoEncounter.contains(e)) {
              stationary.set(true);
            } else {
              if (!hasActiveIcuProcedure(listIcuProcedures, e,
                  CoronaFixedValues.VENT_CODE.getValue()) && !hasActiveIcuProcedure(
                  listIcuProcedures, e, CoronaFixedValues.ECMO_CODE.getValue())) {
                stationary.set(true);
              }
            }
          }
        }
        if (stationary.get()) {
          resultList.add(e);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
    return resultList;
  }

  /**
   * Used to calculate the current treatmentlevel for the icu encounters, for
   * current.treatmentlevel
   *
   * @param mapCurrentIcu     A list of all current inpatient c19-positive cases separated by
   *                          treatment level
   * @param icuTreatmentLevel The Icu treatment level for which the encounter are to be retrieved
   *                          (e.g. {@link CoronaFixedValues#ICU})
   * @return Returns a list of ongoing ICU Encounter
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCurrentEncounterByIcuLevel(
      Map<String, List<UkbEncounter>> mapCurrentIcu, CoronaFixedValues icuTreatmentLevel) {
    List<UkbEncounter> currentIcuList = new ArrayList<>();

    List<UkbEncounter> listICUEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter =
        mapCurrentIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    boolean isPositive;
    boolean hasPeriodEnd;
    try {
      switch (icuTreatmentLevel) {
        case ICU:
          for (UkbEncounter encounter : listICUEncounter) {
            isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasPeriodEnd = encounter.getPeriod().hasEnd();

            // check End, flag and appearance in higher treatmentlevel
            if (!hasPeriodEnd && isPositive) {
              if (!listVentilationEncounter.contains(encounter) && !listEcmoEncounter.contains(
                  encounter)) {
                currentIcuList.add(encounter);
              }
            }
          }
          break;
        case ICU_VENTILATION:
          for (UkbEncounter encounter : listVentilationEncounter) {
            isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasPeriodEnd = encounter.getPeriod().hasEnd();

            // check End, flag, procedure status and appearance in higher treatmentlevel
            if (!hasPeriodEnd && isPositive) {
              for (UkbProcedure ventilation : listIcuProcedures) {
                if (ventilation.getCaseId().equals(encounter.getId())) {
                  if (!listEcmoEncounter.contains(encounter)) {
                    if (hasActiveIcuProcedure(listIcuProcedures, encounter,
                        CoronaFixedValues.VENT_CODE.getValue())) {
                      currentIcuList.add(encounter);
                      break;
                    }
                  } else {
                    // check if higher treatmentlevel is finished and lower is ongoing
                    if (!hasActiveIcuProcedure(listIcuProcedures, encounter,
                        CoronaFixedValues.ECMO_CODE.getValue()) && hasActiveIcuProcedure(
                        listIcuProcedures, encounter, CoronaFixedValues.VENT_CODE.getValue())) {
                      currentIcuList.add(encounter);
                      break;
                    }
                  }
                }
              }
            }
          }
          break;
        case ICU_ECMO:
          for (UkbEncounter encounter : listEcmoEncounter) {
            isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasPeriodEnd = encounter.getPeriod().hasEnd();

            // check end, flag and procedure status
            if (!hasPeriodEnd && isPositive) {
              for (UkbProcedure ecmo : listIcuProcedures) {
                if (ecmo.getCaseId().equals(encounter.getId())) {
                  if (hasActiveIcuProcedure(listIcuProcedures, encounter,
                      CoronaFixedValues.ECMO_CODE.getValue())) {
                    currentIcuList.add(encounter);
                    break;
                  }
                }
              }
            }
          }
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return currentIcuList;
  }

  /**
   * Simple check whether the icu procedure is ongoing or already finished
   *
   * @param listIcu       The Procedures which contain information on whether they are ventilation
   *                      or ecmo
   * @param encounter     The Encounter to be inspected
   * @param procedureCode The procedure code that is going to be checked (e.g. {@link
   *                      CoronaFixedValues#ECMO_CODE})
   * @return true or false whether the Ventilation or Ecmo Procedure is still ongoing or not
   */
  private static boolean hasActiveIcuProcedure(List<UkbProcedure> listIcu, UkbEncounter encounter,
      String procedureCode) {
    boolean isActive = false;
    for (UkbProcedure icu : listIcu) {
      if (icu.getCaseId().equals(encounter.getId()) && icu.getCategory().getCoding().get(0)
          .getCode().equals(procedureCode)) {
        if (icu.getStatus().equals(Procedure.ProcedureStatus.INPROGRESS)) {
          isActive = true;
        }
      }
    }
    return isActive;
  }
}
