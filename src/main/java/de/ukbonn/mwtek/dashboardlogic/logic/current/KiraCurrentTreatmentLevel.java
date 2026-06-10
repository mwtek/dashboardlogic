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
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_UNDIFF;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.getPositiveCurrentlyOnIcuWardEncounters;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.SNOMED;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric;
import de.ukbonn.mwtek.dashboardlogic.models.AggregatedDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;

/**
 * This class is used for generating the data item {@link DiseaseDataItem current.treatmentlevel}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraCurrentTreatmentLevel extends DashboardDataItemLogic {

  /**
   * This method is used to calculate the current treatment level for the icu encounters, used in
   * the data item 'current.treatmentlevel'.
   *
   * @param mapCurrentIcu A list of all current inpatient disease-positive cases separated by
   *     treatment level.
   * @param icuTreatmentLevel The icu treatment level for which the encounter is to be retrieved
   *     (e.g. {@link TreatmentLevels#ICU}).
   * @return Returns a list of ongoing icu encounter.
   */
  public static List<MiiEncounter> getCurrentEncounterByIcuLevel(
      Map<TreatmentLevels, List<MiiEncounter>> mapCurrentIcu,
      TreatmentLevels icuTreatmentLevel,
      List<MiiEncounter> icuSupplyContactEncounters,
      List<MiiEncounter> facilityEncounters,
      List<MiiProcedure> icuProcedures,
      List<MiiLocation> locations,
      InputCodeSettings inputCodeSettings) {

    // Further processing for encounters based on ICU treatment levels
    List<MiiEncounter> positiveCurrentlyOnIcuWardSupplyContacts =
        getPositiveCurrentlyOnIcuWardEncounters(icuSupplyContactEncounters, locations);
    List<String> positiveCurrentlyOnIcuWardFacilityContactIds =
        positiveCurrentlyOnIcuWardSupplyContacts.stream()
            .map(MiiEncounter::getFacilityContactId)
            .toList();

    // Check if ICU_UNDIFFERENTIATED exists
    if (mapCurrentIcu.containsKey(ICU_UNDIFF)) {
      // If ICU_UNDIFFERENTIATED exists, only process those encounters
      switch (icuTreatmentLevel) {
        case NORMAL_WARD:
          // Process encounters for normal ward, excluding ICU, Ventilation, and ECMO encounters
          return facilityEncounters.parallelStream()
              .filter(EncounterFilter::isDiseasePositive)
              .filter(MiiEncounter::isCaseClassInpatientOrShortStay)
              .filter(MiiEncounter::isActive)
              // No higher treatmentlevel found
              .filter(x -> !positiveCurrentlyOnIcuWardFacilityContactIds.contains(x.getId()))
              .toList();
        case ICU_UNDIFF:
          return positiveCurrentlyOnIcuWardSupplyContacts;
      }
    }

    // Continue with normal processing for other ICU levels
    List<MiiEncounter> currentIcuEncounters = mapCurrentIcu.get(ICU);
    List<MiiEncounter> currentVentEncounters = mapCurrentIcu.get(ICU_VENTILATION);
    List<MiiEncounter> currentEcmoEncounters = mapCurrentIcu.get(ICU_ECMO);

    // If no ICU, ICU_VENTILATION, or ICU_ECMO encounters exist, return an empty list
    if (currentIcuEncounters == null
        || currentVentEncounters == null
        || currentEcmoEncounters == null) {
      return new ArrayList<>();
    }

    // Find the corresponding facility encounters for positive ICU ward contacts
    List<MiiEncounter> currentPositiveIcuFacilityContactEncounter =
        currentIcuEncounters.stream()
            .filter(x -> positiveCurrentlyOnIcuWardFacilityContactIds.contains(x.getId()))
            .toList();

    // Active ventilation and ECMO procedures
    List<MiiProcedure> currentActiveIcuVentProcedures =
        icuProcedures.stream()
            .filter(
                x ->
                    x.isCodeExistingInValueSet(
                        inputCodeSettings.getProcedureVentilationCodes(), SNOMED, false))
            .filter(MiiProcedure::isInProgress)
            .toList();
    Set<String> currentActiveVentFacilityContactIds =
        currentActiveIcuVentProcedures.stream()
            .map(MiiProcedure::getCaseId)
            .collect(Collectors.toSet());
    List<MiiEncounter> currentEncountersWithActiveVent =
        currentVentEncounters.stream()
            .filter(x -> currentActiveVentFacilityContactIds.contains(x.getId()))
            .toList();

    List<MiiProcedure> currentActiveIcuEcmoProcedures =
        icuProcedures.stream()
            .filter(
                x ->
                    x.isCodeExistingInValueSet(
                        inputCodeSettings.getProcedureEcmoCodes(), SNOMED, false))
            .filter(MiiProcedure::isInProgress)
            .toList();
    Set<String> currentActiveEcmoFacilityContactIds =
        currentActiveIcuEcmoProcedures.stream()
            .map(MiiProcedure::getCaseId)
            .collect(Collectors.toSet());
    List<MiiEncounter> currentEncountersWithActiveEcmo =
        currentEcmoEncounters.stream()
            .filter(x -> currentActiveEcmoFacilityContactIds.contains(x.getId()))
            .toList();

    // Handling the encounters according to ICU treatment level
    return switch (icuTreatmentLevel) {
      case NORMAL_WARD ->
          // Process encounters for normal ward, excluding ICU, Ventilation, and ECMO encounters
          facilityEncounters.parallelStream()
              .filter(EncounterFilter::isDiseasePositive)
              .filter(MiiEncounter::isCaseClassInpatientOrShortStay)
              .filter(MiiEncounter::isActive)
              .filter(x -> !positiveCurrentlyOnIcuWardFacilityContactIds.contains(x.getId()))
              .filter(x -> !currentActiveVentFacilityContactIds.contains(x.getId()))
              .filter(x -> !currentActiveEcmoFacilityContactIds.contains(x.getId()))
              .toList();
      case ICU ->
          // Process encounters for ICU level, excluding active Vent and ECMO
          currentPositiveIcuFacilityContactEncounter.stream()
              .filter(x -> !currentActiveVentFacilityContactIds.contains(x.getId()))
              .filter(x -> !currentActiveEcmoFacilityContactIds.contains(x.getId()))
              .toList();
      case ICU_VENTILATION ->
          // Process encounters for ICU Ventilation, excluding active ECMO
          currentEncountersWithActiveVent.stream()
              .filter(x -> !currentActiveEcmoFacilityContactIds.contains(x.getId()))
              .toList();
      case ICU_ECMO ->
          // Process encounters for ECMO treatment level
          currentEncountersWithActiveEcmo;
      default -> {
        log.error(
            "Invalid treatment level ({}) used in getCurrentEncounterByIcuLevel.",
            icuTreatmentLevel);
        yield new ArrayList<>();
      }
    };
  }

  /**
   * Creates a map that associates each treatment level with a list of case numbers. This map is
   * useful for debugging purposes and internal reports at the UKB.
   *
   * @param standardWardEncounters List of all normal inpatient {@link MiiEncounter} resources that
   *     include active disease-positive cases.
   * @param icuEncounters List of all inpatient {@link MiiEncounter} resources that include active
   *     disease-positive cases and are in an ICU ward.
   * @param ventEncounters List of all {@link MiiEncounter} resources that include active
   *     disease-positive cases with an active ventilation period (and no active ECMO).
   * @param ecmoEncounters List of all {@link MiiEncounter} resources that include active
   *     disease-positive cases with an active ECMO period.
   * @return Map that connects treatment levels with a list of case numbers.
   */
  public static Map<String, List<String>> createMapCurrentTreatmentlevelCaseIds(
      List<MiiEncounter> standardWardEncounters,
      List<MiiEncounter> icuEncounters,
      List<MiiEncounter> ventEncounters,
      List<MiiEncounter> ecmoEncounters,
      List<MiiEncounter> icuUndiffEncounters,
      Boolean useIcuUndiff) {

    // Lists to store case numbers for different treatment levels
    List<String> stationaryCaseNrs =
        standardWardEncounters.stream()
            .filter(
                encounter ->
                    encounter.isCaseClassInpatientOrShortStay()
                        && encounter.hasExtension(POSITIVE_RESULT.getValue()))
            .map(MiiEncounter::getId)
            .toList();

    // If icu undiff is used, return the corresponding map
    if (useIcuUndiff) {
      List<String> icuUndiffCaseNrs =
          icuUndiffEncounters.stream().map(MiiEncounter::getId).toList();
      return Map.of(
          NORMAL_WARD.getValue(), stationaryCaseNrs,
          ICU_UNDIFF.getValue(), icuUndiffCaseNrs);
    }

    List<String> icuCaseNrs = icuEncounters.stream().map(MiiEncounter::getId).toList();
    List<String> ventCaseNrs = ventEncounters.stream().map(MiiEncounter::getId).toList();
    List<String> ecmoCaseNrs = ecmoEncounters.stream().map(MiiEncounter::getId).toList();

    // Result map to connect treatment levels with case numbers
    return Map.of(
        NORMAL_WARD.getValue(), stationaryCaseNrs,
        ICU.getValue(), icuCaseNrs,
        ICU_VENTILATION.getValue(), ventCaseNrs,
        ICU_ECMO.getValue(), ecmoCaseNrs);
  }

  public AggregatedDataItem createCurrentTreatmentLevel(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      List<MiiEncounter> encounterSubSet,
      List<MiiProcedure> icuProcedures,
      InputCodeSettings inputCodeSettings,
      boolean useIcuUndiff) {

    log.debug("started createCurrentTreatmentLevel");
    Instant timerStart = TimerTools.startTimer();
    AggregatedDataItem dataItem = new AggregatedDataItem();
    dataItem.setItemtype(DataItemTypes.ITEMTYPE_AGGREGATED);

    // Map active ICU encounters to their active ICU location
    Map<String, EncounterLocationComponent> activeIcuLocationsByFacilityEncounter =
        coreCaseDataByGroups.values().stream()
            .flatMap(group -> group.values().stream())
            .filter(caseData -> caseData.getDischargeDate() == null)
            .map(
                caseData ->
                    caseData.getLocationComponentList().stream()
                        .filter(loc -> !loc.getPeriod().hasEnd())
                        .findFirst()
                        .map(loc -> Map.entry(caseData.getFacilityEncounterId(), loc)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Active encounters (e.g. from all sources)
    Set<String> activeEncounters =
        encounterSubSet.stream()
            .filter(MiiEncounter::isActive)
            .map(MiiEncounter::getFacilityContactId)
            .collect(Collectors.toSet());

    Set<String> activeIcuEncounters = activeIcuLocationsByFacilityEncounter.keySet();

    // Encounters that are active but not in ICU → considered normal ward
    Set<String> activeNormalWardEncounters =
        activeEncounters.stream()
            .filter(id -> !activeIcuEncounters.contains(id))
            .collect(Collectors.toSet());

    Map<String, Integer> results = new HashMap<>();
    results.put(Pediatric.NORMAL_WARD, activeNormalWardEncounters.size());

    // If undiff is activated, we can end the method already.
    if (useIcuUndiff) {
      // If undifferentiated view is requested, just count all active ICU encounters
      results.put(Pediatric.UNDIFFERENTIATED, activeIcuEncounters.size());
      dataItem.setData(results);
      return dataItem;
    }

    // Filter in-progress ICU procedures
    List<MiiProcedure> activeIcuProcedures =
        icuProcedures.stream().filter(MiiProcedure::isInProgress).toList();

    // Categorize procedures by type
    List<MiiProcedure> highFlow =
        filterByCodes(activeIcuProcedures, inputCodeSettings.getProcedureHighFlowCodes());
    List<MiiProcedure> cpap =
        filterByCodes(activeIcuProcedures, inputCodeSettings.getProcedureCpapCodes());
    List<MiiProcedure> ventilation =
        filterByCodes(activeIcuProcedures, inputCodeSettings.getProcedureVentilationCodes());
    List<MiiProcedure> ecmo =
        filterByCodes(activeIcuProcedures, inputCodeSettings.getProcedureEcmoCodes());

    // Extract case IDs per category
    Set<String> ecmoCases = toCaseIds(ecmo);
    Set<String> ventCases = toCaseIds(ventilation);
    Set<String> ventOnlyCases = subtract(ventCases, ecmoCases);
    Set<String> cpapOnlyCases = subtract(toCaseIds(cpap), union(ecmoCases, ventCases));
    Set<String> highFlowOnlyCases =
        subtract(toCaseIds(highFlow), union(ecmoCases, ventCases, cpapOnlyCases));

    // ICU cases not covered by any specific treatment category
    Set<String> categorizedIcuCases = union(ecmoCases, ventCases, cpapOnlyCases, highFlowOnlyCases);
    Set<String> plainIcuCases = subtract(activeIcuEncounters, categorizedIcuCases);

    // Final result map
    results.put(Pediatric.NORMAL_WARD, activeNormalWardEncounters.size());
    results.put(Pediatric.ICU, plainIcuCases.size());
    results.put(Pediatric.CPAP, cpapOnlyCases.size());
    results.put(Pediatric.HIGHFLOW, highFlowOnlyCases.size());
    results.put(Pediatric.INVASIVE_VENTILATION, ventOnlyCases.size());
    results.put(Pediatric.ECMO, ecmoCases.size());

    TimerTools.stopTimerAndLog(timerStart, "finished createTimelineTestsMap");
    dataItem.setData(results);
    return dataItem;
  }

  private List<MiiProcedure> filterByCodes(
      List<MiiProcedure> procedures, Collection<String> codeSet) {
    return procedures.stream()
        .filter(proc -> proc.isCodeExistingInValueSet(codeSet, SNOMED, true))
        .toList();
  }

  private Set<String> toCaseIds(List<MiiProcedure> procedures) {
    return procedures.stream().map(MiiProcedure::getCaseId).collect(Collectors.toSet());
  }

  @SafeVarargs
  private final <T> Set<T> union(Set<T>... sets) {
    return Arrays.stream(sets).flatMap(Set::stream).collect(Collectors.toSet());
  }

  private <T> Set<T> subtract(Set<T> base, Set<T> toRemove) {
    Set<T> result = new HashSet<>(base);
    result.removeAll(toRemove);
    return result;
  }
}
