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

package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.maxtreatmentlevel;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.TWELVE_DAYS_LOGIC;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_UNDIFF;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;

import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem
 * cumulative.maxtreatmentlevel}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeMaxTreatmentLevel extends DashboardData {

  /**
   * Returns a list containing encounter which has or had ambulant or stationary as their highest
   * treatmentlevel
   *
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @param treatmentLevel Treatmentlevel (e.g. {@link TreatmentLevels#ICU_ECMO}) as a separation
   *     criterion
   * @param mapPositiveEncounterByClass Map with all positive encounters, grouped by case class
   * @return List with all encounters that have the given MaxTreatmentLevel
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCumulativeByClass(
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu,
      TreatmentLevels treatmentLevel,
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass) {
    Set<String> ambulantPidSet = new HashSet<>();
    Set<String> normalWardPidSet = new HashSet<>();
    List<UkbEncounter> resultEncounters = new ArrayList<>();
    log.debug("started getCumulativeByClass");
    Instant startTime = TimerTools.startTimer();

    for (Map.Entry<TreatmentLevels, List<UkbEncounter>> entry :
        mapPositiveEncounterByClass.entrySet()) {
      List<UkbEncounter> value = entry.getValue();
      switch (treatmentLevel) {
        case OUTPATIENT -> // If the 12 days appeared its minimum normal ward+ treatmentlevel
            ambulantPidSet.addAll(
                value.stream()
                    .filter(
                        encounter ->
                            encounter.isCaseClassOutpatient()
                                && !encounter.hasExtension(TWELVE_DAYS_LOGIC.getValue()))
                    .map(UkbEncounter::getPatientId)
                    .collect(Collectors.toSet()));
        case INPATIENT -> // Check if the encounter is not part of any icu+ treatmentlevel
            normalWardPidSet.addAll(
                value.stream()
                    .filter(UkbEncounter::isCaseClassInpatient)
                    .filter(e -> isMaxTreatmentlevelNormalWard(mapIcu, e))
                    .map(UkbEncounter::getPatientId)
                    .collect(Collectors.toSet()));
      }
    }
    // print out only the most recent cases in case a patient has more than one case with the same
    // treatmentlevel
    if (treatmentLevel == OUTPATIENT) {
      resultEncounters =
          getYoungestCases(treatmentLevel, mapPositiveEncounterByClass, ambulantPidSet);
    } else if (treatmentLevel == INPATIENT) {
      resultEncounters =
          getYoungestCases(treatmentLevel, mapPositiveEncounterByClass, normalWardPidSet);
    }

    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByClass");
    return resultEncounters;
  }

  private static boolean isMaxTreatmentlevelNormalWard(
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu, UkbEncounter e) {
    // If ICU_UNDIFFERENTIATED is found, the other variants do not exist in the map
    if (mapIcu.containsKey(ICU_UNDIFF)) return !mapIcu.get(ICU_UNDIFF).contains(e);
    else
      return !mapIcu.get(ICU_ECMO).contains(e)
          && !mapIcu.get(ICU_VENTILATION).contains(e)
          && !mapIcu.get(ICU).contains(e);
  }

  /**
   * Finds the youngest encounters for each patient based on the encounter start time.
   *
   * @param treatmentLevel The treatment level to filter encounters by (e.g., ICU_ECMO).
   * @param mapPositiveEncounterByClass A map containing all positive encounters grouped by their
   *     class.
   * @param setInpatientPids A set containing the patient IDs of in-patient patients.
   * @return A list of encounters, where each encounter represents the youngest encounter for its
   *     respective patient.
   */
  private List<UkbEncounter> getYoungestCases(
      TreatmentLevels treatmentLevel,
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass,
      Set<String> setInpatientPids) {

    try (Stream<UkbEncounter> encounterStream =
        mapPositiveEncounterByClass.get(treatmentLevel).stream()) {
      return encounterStream
          // Filter encounters for the desired treatment level and in-patient patients with valid
          // start times
          .filter(
              encounter ->
                  setInpatientPids.contains(encounter.getPatientId())
                      && encounter.isPeriodStartExistent())
          .collect(Collectors.groupingBy(UkbEncounter::getPatientId))
          .values()
          .stream()
          // Find the encounter with the earliest start time (youngest)
          .map(
              encounterList ->
                  encounterList.stream()
                      .min(Comparator.comparing(x -> x.getPeriod().getStart()))
                      .orElseThrow())
          // Collect the youngest encounters into a list
          .collect(Collectors.toList());
    } catch (Exception ex) {
      log.error("Error determining youngest cases: ", ex);
      return Collections.emptyList();
    }
  }

  // Multiple required lists to project larger FHIR resource object lists to the needed attributes
  private Set<String> setVentilationPids;
  private Set<String> setEcmoPids;

  /**
   * Creates a list which contains encounters who have or had icu, ventilation or ecmo as highest
   * treatmentlevel. [needed in data item: cumulative.maxtreatmentlevel]
   *
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class.
   * @param treatmentLevel The treatmentlevel (e.g. {@link TreatmentLevels#ICU}) which is going to
   *     be checked as a separation criterion.
   * @return List of all encounters that have the given treatment level as maximum treatment level.
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCumulativeByIcuLevel(
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu, TreatmentLevels treatmentLevel) {

    List<UkbEncounter> resultList = new ArrayList<>();
    HashMap<String, List<UkbEncounter>> mapPidCases = new HashMap<>();
    log.debug("started getCumulativeByIcuLevel [{}]", treatmentLevel);
    Instant startTime = TimerTools.startTimer();

    // Retrieve the encounters for all treatment levels
    List<UkbEncounter> icuEncounters = mapIcu.getOrDefault(ICU, new ArrayList<>());
    List<UkbEncounter> ventilationEncounters =
        mapIcu.getOrDefault(ICU_VENTILATION, new ArrayList<>());
    List<UkbEncounter> ecmoEncounters = mapIcu.getOrDefault(ICU_ECMO, new ArrayList<>());
    List<UkbEncounter> icuUndiffEncounters = mapIcu.getOrDefault(ICU_UNDIFF, new ArrayList<>());

    // If ICU_UNDIFFERENTIATED encounters are present, process them and ignore other levels.
    if (!icuUndiffEncounters.isEmpty()) {
      processIcuUndiffEncounters(icuUndiffEncounters, mapPidCases, resultList);
      TimerTools.stopTimerAndLog(
          startTime, "finished getCumulativeByIcuLevel (ICU_UNDIFFERENTIATED)");
      return resultList;
    }

    // Flatten the patient ids by treatment level if not already initialized
    if (setVentilationPids == null || setEcmoPids == null) {
      setVentilationPids =
          ventilationEncounters.stream()
              .map(UkbEncounter::getPatientId)
              .collect(Collectors.toSet());
      setEcmoPids =
          ecmoEncounters.stream().map(UkbEncounter::getPatientId).collect(Collectors.toSet());
    }

    Set<String> setIcuOnlyPids = new HashSet<>();
    Set<UkbEncounter> setEncounterToBeRemoved = new HashSet<>();

    // Process encounters based on the treatment level
    switch (treatmentLevel) {
      case ICU:
        processIcuEncounters(
            icuEncounters,
            ventilationEncounters,
            ecmoEncounters,
            mapPidCases,
            resultList,
            setIcuOnlyPids,
            setEncounterToBeRemoved);
        break;
      case ICU_VENTILATION:
        processVentilationEncounters(
            ventilationEncounters, ecmoEncounters, mapPidCases, resultList);
        break;
      case ICU_ECMO:
        processEcmoEncounters(ecmoEncounters, mapPidCases, resultList);
        break;
    }

    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByIcuLevel");
    return resultList;
  }

  /**
   * Processes the ICU_UNDIFFERENTIATED encounters and adds the first disease-positive encounter for
   * each patient to the result list.
   *
   * @param icuUndiffEncounters List of encounters for ICU_UNDIFFERENTIATED treatment level.
   * @param mapPidCases A map to store encounters by patient ID.
   * @param resultList The list that will contain the first disease-positive encounter for each
   *     patient.
   */
  private void processIcuUndiffEncounters(
      List<UkbEncounter> icuUndiffEncounters,
      Map<String, List<UkbEncounter>> mapPidCases,
      List<UkbEncounter> resultList) {
    // Process ICU_UNDIFFERENTIATED encounters
    for (UkbEncounter undiffEncounter : icuUndiffEncounters) {
      addPidToMap(mapPidCases, undiffEncounter);
    }

    // Add first disease-positive encounter for each patient
    resultList.addAll(getPatientsFirstDiseasePositiveEncounter(mapPidCases));
  }

  /**
   * Processes the ICU encounters and handles patients that have no ventilation or ECMO. Also
   * removes encounters with higher treatment levels.
   *
   * @param icuEncounters List of ICU encounters.
   * @param ventilationEncounters List of ICU_VENTILATION encounters.
   * @param ecmoEncounters List of ICU_ECMO encounters.
   * @param mapPidCases A map to store encounters by patient ID.
   * @param resultList The list to store the result encounters.
   * @param setIcuOnlyPids Set of patient IDs for ICU-only patients.
   * @param setEncounterToBeRemoved Set of encounters to be removed from the result list.
   */
  private void processIcuEncounters(
      List<UkbEncounter> icuEncounters,
      List<UkbEncounter> ventilationEncounters,
      List<UkbEncounter> ecmoEncounters,
      Map<String, List<UkbEncounter>> mapPidCases,
      List<UkbEncounter> resultList,
      Set<String> setIcuOnlyPids,
      Set<UkbEncounter> setEncounterToBeRemoved) {
    // Process ICU encounters
    icuEncounters.stream()
        .filter(
            e ->
                !setEcmoPids.contains(e.getPatientId())
                    && !setVentilationPids.contains(e.getPatientId()))
        .forEach(
            e -> {
              addPidToMap(mapPidCases, e);
              setIcuOnlyPids.add(e.getPatientId());
            });

    // Add first disease-positive encounter for each patient
    resultList.addAll(getPatientsFirstDiseasePositiveEncounter(mapPidCases));

    // Remove encounters with higher treatment levels than ICU
    removeHigherLevelEncounters(ventilationEncounters, setIcuOnlyPids, setEncounterToBeRemoved);
    removeHigherLevelEncounters(ecmoEncounters, setIcuOnlyPids, setEncounterToBeRemoved);

    resultList.removeAll(setEncounterToBeRemoved);
  }

  /**
   * Processes the ICU_VENTILATION encounters and adds the first disease-positive encounter for each
   * patient to the result list.
   *
   * @param ventilationEncounters List of ICU_VENTILATION encounters.
   * @param ecmoEncounters List of ICU_ECMO encounters.
   * @param mapPidCases A map to store encounters by patient ID.
   * @param resultList The list to store the result encounters.
   */
  private void processVentilationEncounters(
      List<UkbEncounter> ventilationEncounters,
      List<UkbEncounter> ecmoEncounters,
      Map<String, List<UkbEncounter>> mapPidCases,
      List<UkbEncounter> resultList) {
    ventilationEncounters.stream()
        .filter(ventEncounter -> !ecmoEncounters.contains(ventEncounter))
        .forEach(encounter -> addPidToMap(mapPidCases, encounter));

    // Add first disease-positive encounter for each patient
    resultList.addAll(getPatientsFirstDiseasePositiveEncounter(mapPidCases));
  }

  /**
   * Processes the ICU_ECMO encounters and adds the first disease-positive encounter for each
   * patient to the result list.
   *
   * @param ecmoEncounters List of ICU_ECMO encounters.
   * @param mapPidCases A map to store encounters by patient ID.
   * @param resultList The list to store the result encounters.
   */
  private void processEcmoEncounters(
      List<UkbEncounter> ecmoEncounters,
      Map<String, List<UkbEncounter>> mapPidCases,
      List<UkbEncounter> resultList) {
    ecmoEncounters.forEach(encounter -> addPidToMap(mapPidCases, encounter));

    // Add first disease-positive encounter for each patient
    resultList.addAll(getPatientsFirstDiseasePositiveEncounter(mapPidCases));
  }

  /**
   * Removes encounters from the list that have higher treatment levels than the specified ICU
   * level.
   *
   * @param encounters List of encounters to process.
   * @param setIcuOnlyPids Set of patient IDs that should only be in the ICU level.
   * @param setEncounterToBeRemoved Set of encounters to be removed from the result list.
   */
  private void removeHigherLevelEncounters(
      List<UkbEncounter> encounters,
      Set<String> setIcuOnlyPids,
      Set<UkbEncounter> setEncounterToBeRemoved) {
    encounters.stream()
        .filter(encounter -> setIcuOnlyPids.contains(encounter.getPatientId()))
        .forEach(setEncounterToBeRemoved::add);
  }

  /**
   * Adds an encounter to the map of patient encounters. Uses computeIfAbsent to minimize
   * unnecessary HashMap lookups.
   *
   * @param mapPidCases A map storing encounters by patient ID.
   * @param encounter The encounter to add.
   */
  private void addPidToMap(Map<String, List<UkbEncounter>> mapPidCases, UkbEncounter encounter) {
    mapPidCases.computeIfAbsent(encounter.getPatientId(), k -> new ArrayList<>()).add(encounter);
  }

  /**
   * Identify patients' first positive corona cases without considering any subsequent
   * disease-positive cases.
   *
   * @param mapPidCase A map that assigns all associated disease-positive cases (0:n) to a pid.
   * @return List of {@link UkbEncounter} resources of first disease-positive cases of transferred
   *     patients.
   */
  private List<UkbEncounter> getPatientsFirstDiseasePositiveEncounter(
      Map<String, List<UkbEncounter>> mapPidCase) {
    List<UkbEncounter> encounters = new ArrayList<>();
    for (Map.Entry<String, List<UkbEncounter>> mapEncountersByPid : mapPidCase.entrySet()) {
      encounters.add(getFirstDiseasePositiveCase(mapEncountersByPid));
    }
    return encounters;
  }

  /**
   * Returns the case from an encounter list with the lowest admission date. ({@link
   * UkbEncounter#getPeriod()})
   *
   * @param mapEncounterByPid Map of all encounters per pid.
   * @return The oldest positive C19 case of a patient.
   */
  private UkbEncounter getFirstDiseasePositiveCase(
      Map.Entry<String, List<UkbEncounter>> mapEncounterByPid) {
    return mapEncounterByPid.getValue().stream()
        .filter(UkbEncounter::isPeriodStartExistent)
        .min(Comparator.comparing(x -> x.getPeriod().getStart()))
        .orElse(null);
  }
}
