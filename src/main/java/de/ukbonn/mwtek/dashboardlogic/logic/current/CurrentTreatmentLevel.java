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

import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isActive;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.isCovidPositive;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCoding;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hl7.fhir.r4.model.Procedure;

/**
 * This class is used for generating the data item {@link CoronaDataItem current.treatmentlevel}.
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
      Map<String, List<UkbEncounter>> mapCurrentIcu, InputCodeSettings inputCodeSettings) {
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
      listEncounterWithoutOutpatients.forEach(enc -> {
        AtomicBoolean stationary = new AtomicBoolean(false);
        // Check if encounter is active and has positive covid flagging
        if (isActive(enc) && isCovidPositive(enc)) {
          // Checking if encounter has a higher treatmentlevel
          if (!listIcuEncounter.contains(enc)) {
            if (!listVentilationEncounter.contains(enc) && !listEcmoEncounter.contains(enc)) {
              stationary.set(true);
            } else {
              if (!hasActiveIcuProcedure(listIcuProcedures, enc,
                  inputCodeSettings.getProcedureVentilationCodes()) && !hasActiveIcuProcedure(
                  listIcuProcedures, enc, inputCodeSettings.getProcedureEcmoCodes())) {
                stationary.set(true);
              }
            }
          }
        }
        if (stationary.get()) {
          resultList.add(enc);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
    return resultList;
  }

  /**
   * This method is used to calculate the current treatment level for the icu encounters, used in
   * the data item 'current.treatmentlevel'.
   *
   * @param mapCurrentIcu     A list of all current inpatient c19-positive cases separated by
   *                          treatment level.
   * @param icuTreatmentLevel The icu treatment level for which the encounter are to be retrieved
   *                          (e.g. {@link CoronaFixedValues#ICU}).
   * @return Returns a list of ongoing icu encounter.
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCurrentEncounterByIcuLevel(
      Map<String, List<UkbEncounter>> mapCurrentIcu, CoronaFixedValues icuTreatmentLevel,
      InputCodeSettings inputCodeSettings) {
    List<UkbEncounter> currentIcuList = new ArrayList<>();

    List<UkbEncounter> listIcuEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter =
        mapCurrentIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    boolean isPositive;
    boolean isActive;
    try {
      switch (icuTreatmentLevel) {
        case ICU:
          for (UkbEncounter encounter : listIcuEncounter) {
            isPositive = isCovidPositive(encounter);
            isActive = isActive(encounter);

            // check End, flag and appearance in higher treatmentlevel
            if (isActive && isPositive) {
              if (!listVentilationEncounter.contains(encounter) && !listEcmoEncounter.contains(
                  encounter)) {
                currentIcuList.add(encounter);
              }
            }
          }
          break;
        case ICU_VENTILATION:
          for (UkbEncounter encounter : listVentilationEncounter) {
            isPositive = isCovidPositive(encounter);
            isActive = isActive(encounter);

            // check End, flag, procedure status and appearance in higher treatmentlevel
            if (isActive && isPositive) {
              for (UkbProcedure ventilation : listIcuProcedures) {
                if (ventilation.getCaseId().equals(encounter.getId())) {
                  if (!listEcmoEncounter.contains(encounter)) {
                    if (hasActiveIcuProcedure(listIcuProcedures, encounter,
                        inputCodeSettings.getProcedureVentilationCodes())) {
                      currentIcuList.add(encounter);
                      break;
                    }
                  } else {
                    // check if higher treatmentlevel is finished and lower is ongoing
                    if (!hasActiveIcuProcedure(listIcuProcedures, encounter,
                        inputCodeSettings.getProcedureEcmoCodes()) && hasActiveIcuProcedure(
                        listIcuProcedures, encounter,
                        inputCodeSettings.getProcedureVentilationCodes())) {
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
            isPositive = isCovidPositive(encounter);
            isActive = isActive(encounter);

            // check end, flag and procedure status
            if (isActive && isPositive) {
              for (UkbProcedure ecmo : listIcuProcedures) {
                if (ecmo.getCaseId().equals(encounter.getId())) {
                  if (hasActiveIcuProcedure(listIcuProcedures, encounter,
                      inputCodeSettings.getProcedureEcmoCodes())) {
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
   * @param listIcu        The procedures which contain information on whether they are ventilation
   *                       or ecmo.
   * @param encounter      The Encounter to be inspected.
   * @param procedureCodes The procedure code(s) that is/are going to be checked.
   * @return true or false whether the ventilation or ecmo procedure is still ongoing or not.
   */
  private static boolean hasActiveIcuProcedure(List<UkbProcedure> listIcu, UkbEncounter encounter,
      List<String> procedureCodes) {
    List<UkbProcedure> proceduresCurrentEncounter = listIcu.stream()
        .filter(x -> x.getCaseId().equals(encounter.getId()))
        .filter(x -> x.hasCode() && x.getCode().hasCoding())
        .filter(x -> procedureCodes.contains(getCodeOfFirstCoding(x.getCode().getCoding())))
        .filter(x -> x.getStatus().equals(Procedure.ProcedureStatus.INPROGRESS)).toList();
    return proceduresCurrentEncounter.size() > 0;
  }
}
