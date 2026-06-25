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

import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.SNOMED;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric;
import de.ukbonn.mwtek.dashboardlogic.models.AggregatedDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
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
