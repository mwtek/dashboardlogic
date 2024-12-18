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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.getPositiveCurrentlyOnIcuWardEncounters;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.SNOMED;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCoding;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;

/**
 * This class is used for generating the data item {@link DiseaseDataItem current.treatmentlevel}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CurrentTreatmentLevel extends DashboardDataItemLogic {

  /**
   * This method is used to calculate the current treatment level for the icu encounters, used in
   * the data item 'current.treatmentlevel'.
   *
   * @param mapCurrentIcu A list of all current inpatient c19-positive cases separated by treatment
   *     level.
   * @param icuTreatmentLevel The icu treatment level for which the encounter is to be retrieved
   *     (e.g. {@link TreatmentLevels#ICU}).
   * @return Returns a list of ongoing icu encounter.
   */
  public static List<UkbEncounter> getCurrentEncounterByIcuLevel(
      Map<TreatmentLevels, List<UkbEncounter>> mapCurrentIcu,
      TreatmentLevels icuTreatmentLevel,
      List<UkbEncounter> icuSupplyContactEncounters,
      List<UkbEncounter> facilityEncounters,
      List<UkbProcedure> icuProcedures,
      List<UkbLocation> locations,
      InputCodeSettings inputCodeSettings) {

    List<UkbEncounter> currentIcuEncounters = mapCurrentIcu.get(ICU);
    List<UkbEncounter> currentVentEncounters = mapCurrentIcu.get(ICU_VENTILATION);
    List<UkbEncounter> currentEcmoEncounters = mapCurrentIcu.get(ICU_ECMO);

    // Figuring out the current status of all treatment levels (an encounter can appear in all of
    // these at the same time!)
    // Figure out if there is an ACTIVE ICU ward admission ongoing
    List<UkbEncounter> positiveCurrentlyOnIcuWardSupplyContacts =
        getPositiveCurrentlyOnIcuWardEncounters(icuSupplyContactEncounters, locations);
    List<String> positiveCurrentlyOnIcuWardFacilityContactIds =
        positiveCurrentlyOnIcuWardSupplyContacts.stream()
            .map(UkbEncounter::getFacilityContactId)
            .toList();
    // Find the corresponding facility encounter resource to keep the output
    List<UkbEncounter> currentPositiveIcuFacilityContactEncounter =
        currentIcuEncounters.stream()
            .filter(x -> positiveCurrentlyOnIcuWardFacilityContactIds.contains(x.getId()))
            .toList();

    List<UkbProcedure> currentActiveIcuVentProcedures =
        icuProcedures.stream()
            .filter(
                x ->
                    x.isCodeExistingInValueSet(
                        inputCodeSettings.getProcedureVentilationCodes(), SNOMED, false))
            .filter(UkbProcedure::isInProgress)
            .toList();
    Set<String> currentActiveVentFacilityContactIds =
        currentActiveIcuVentProcedures.stream()
            .map(UkbProcedure::getCaseId)
            .collect(Collectors.toSet());
    List<UkbEncounter> currentEncountersWithActiveVent =
        currentVentEncounters.stream()
            .filter(x -> currentActiveVentFacilityContactIds.contains(x.getId()))
            .toList();

    List<UkbProcedure> currentActiveIcuEcmoProcedures =
        icuProcedures.stream()
            .filter(
                x ->
                    x.isCodeExistingInValueSet(
                        inputCodeSettings.getProcedureEcmoCodes(), SNOMED, false))
            .filter(UkbProcedure::isInProgress)
            .toList();
    Set<String> currentActiveEcmoFacilityContactIds =
        currentActiveIcuEcmoProcedures.stream()
            .map(UkbProcedure::getCaseId)
            .collect(Collectors.toSet());
    List<UkbEncounter> currentEncountersWithActiveEcmo =
        currentEcmoEncounters.stream()
            .filter(x -> currentActiveEcmoFacilityContactIds.contains(x.getId()))
            .toList();

    // Assigning regarding the icu level hierarchy
    switch (icuTreatmentLevel) {
      case NORMAL_WARD -> {
        // The inpatient filter filters pre-stationary cases
        // If it's not on one of the upper level it must be a normal ward.
        return facilityEncounters.parallelStream()
            .filter(EncounterFilter::isDiseasePositive)
            .filter(UkbEncounter::isCaseClassInpatient)
            .filter(UkbEncounter::isActive)
            .filter(x -> !positiveCurrentlyOnIcuWardFacilityContactIds.contains(x.getId()))
            .filter(x -> !currentActiveVentFacilityContactIds.contains(x.getId()))
            .filter(x -> !currentActiveEcmoFacilityContactIds.contains(x.getId()))
            .toList();
      }
      case ICU -> {
        // Just treat it as ICU if there is no active VENT or ECMO
        return currentPositiveIcuFacilityContactEncounter.stream()
            .filter(x -> !currentActiveVentFacilityContactIds.contains(x.getId()))
            .filter(x -> !currentActiveEcmoFacilityContactIds.contains(x.getId()))
            .toList();
      }
      case ICU_VENTILATION -> {
        // Just treat it as VENT if there is no active ECMO
        return currentEncountersWithActiveVent.stream()
            .filter(x -> !currentActiveEcmoFacilityContactIds.contains(x.getId()))
            .toList();
      }
      case ICU_ECMO -> {
        return currentEncountersWithActiveEcmo;
      }
      default -> {
        log.error(
            "Invalid treatment level ({}) used in getCurrentEncounterByIcuLevel.",
            icuTreatmentLevel);
        return new ArrayList<>();
      }
    }
  }

  /**
   * Filters encounters from a given level that are not present in provided higher level encounter
   * lists.
   *
   * @param encounters The set of encounters to filter.
   * @param higherLevel1 The list of encounters from the first higher treatment level.
   * @param higherLevel2 The list of encounters from the second higher treatment level.
   * @return A list of encounters that are not present in the higher level encounter lists.
   */
  private List<UkbEncounter> filterEncountersNotInHigherLevels(
      Set<UkbEncounter> encounters,
      List<UkbEncounter> higherLevel1,
      List<UkbEncounter> higherLevel2) {
    return encounters.stream()
        .filter(
            encounter ->
                !higherLevel1.contains(encounter)
                    && (higherLevel2.isEmpty() || !higherLevel2.contains(encounter)))
        .collect(Collectors.toList());
  }

  /**
   * Filters encounters from a given level that are actively receiving a specific procedure based on
   * provided codes.
   *
   * @param encounters The set of encounters to filter.
   * @param procedureCodes A list of procedure codes to identify active procedures.
   * @return A list of encounters with active procedures (based on codes) that are not present in
   *     the higher level list.
   */
  private Set<UkbEncounter> filterActiveEncountersWithProcedure(
      Set<UkbEncounter> encounters, List<UkbProcedure> icuProcedures, List<String> procedureCodes) {
    return encounters.stream()
        .filter(encounter -> hasActiveIcuProcedure(icuProcedures, encounter, procedureCodes))
        .collect(Collectors.toSet());
  }

  /**
   * Simple check whether the icu procedure is ongoing or already finished
   *
   * @param listIcu The procedures which contain information on whether they are ventilation or
   *     ecmo.
   * @param encounter The Encounter to be inspected.
   * @param procedureCodes The procedure code(s) that is/are going to be checked.
   * @return true or false whether the ventilation or ecmo procedure is still ongoing or not.
   */
  private static boolean hasActiveIcuProcedure(
      List<UkbProcedure> listIcu, UkbEncounter encounter, List<String> procedureCodes) {
    List<UkbProcedure> proceduresCurrentEncounter =
        listIcu.stream()
            .filter(x -> x.getCaseId().equals(encounter.getId()))
            .filter(x -> x.hasStatus() && x.getStatus().equals(ProcedureStatus.INPROGRESS))
            .filter(x -> x.hasCode() && x.getCode().hasCoding())
            .filter(x -> procedureCodes.contains(getCodeOfFirstCoding(x.getCode().getCoding())))
            .toList();
    return !proceduresCurrentEncounter.isEmpty();
  }

  /**
   * Creates a map that associates each treatment level with a list of case numbers. This map is
   * useful for debugging purposes and internal reports at the UKB.
   *
   * @param standardWardEncounters List of all normal inpatient {@link UkbEncounter} resources that
   *     include active disease-positive cases.
   * @param icuEncounters List of all inpatient {@link UkbEncounter} resources that include active
   *     disease-positive cases and are in an ICU ward.
   * @param ventEncounters List of all {@link UkbEncounter} resources that include active
   *     disease-positive cases with an active ventilation period (and no active ECMO).
   * @param ecmoEncounters List of all {@link UkbEncounter} resources that include active
   *     disease-positive cases with an active ECMO period.
   * @return Map that connects treatment levels with a list of case numbers.
   */
  public static Map<String, List<String>> createMapCurrentTreatmentlevelCaseIds(
      List<UkbEncounter> standardWardEncounters,
      List<UkbEncounter> icuEncounters,
      List<UkbEncounter> ventEncounters,
      List<UkbEncounter> ecmoEncounters) {

    // Lists to store case numbers for different treatment levels
    List<String> stationaryCaseNrs = new ArrayList<>();
    List<String> icuCaseNrs = new ArrayList<>();
    List<String> ventCaseNrs = new ArrayList<>();
    List<String> ecmoCaseNrs = new ArrayList<>();

    // Result map to connect treatment levels with case numbers
    Map<String, List<String>> resultCurrentTreatmentCaseNrs = new LinkedHashMap<>();

    // Creating a list with the subset for all positive stationary (needed in age calculations)
    standardWardEncounters.forEach(
        encounter -> {
          if (encounter.isCaseClassInpatient()
              && encounter.hasExtension(POSITIVE_RESULT.getValue())) {
            stationaryCaseNrs.add(encounter.getId());
          }
        });

    // Adding case numbers to the respective lists for each treatment level
    icuEncounters.forEach(encounter -> icuCaseNrs.add(encounter.getId()));
    ventEncounters.forEach(encounter -> ventCaseNrs.add(encounter.getId()));
    ecmoEncounters.forEach(encounter -> ecmoCaseNrs.add(encounter.getId()));

    // Putting lists into the result map with treatment levels as keys
    resultCurrentTreatmentCaseNrs.put(NORMAL_WARD.getValue(), stationaryCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU.getValue(), icuCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU_VENTILATION.getValue(), ventCaseNrs);
    resultCurrentTreatmentCaseNrs.put(ICU_ECMO.getValue(), ecmoCaseNrs);

    return resultCurrentTreatmentCaseNrs;
  }
}
