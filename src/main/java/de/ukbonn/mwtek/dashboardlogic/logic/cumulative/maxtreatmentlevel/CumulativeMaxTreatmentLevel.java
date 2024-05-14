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
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;

import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
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
 * This class is used for generating the data item
 * {@link DiseaseDataItem cumulative.maxtreatmentlevel}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeMaxTreatmentLevel extends DashboardDataItemLogics {

  /**
   * Returns a list containing encounter which has or had ambulant or stationary as their highest
   * treatmentlevel
   *
   * @param mapIcu                      Map that assigns a list of case numbers to an ICU treatment
   *                                    level class
   * @param treatmentLevel              Treatmentlevel (e.g. {@link TreatmentLevels#ICU_ECMO}) as a
   *                                    separation criterion
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
        case OUTPATIENT -> {
          // If the 12 days appeared its minimum normal ward+ treatmentlevel
          ambulantPidSet.addAll(value.stream()
              .filter(encounter -> encounter.isCaseClassOutpatient()
                  && !encounter.hasExtension(TWELVE_DAYS_LOGIC.getValue()))
              .map(UkbEncounter::getPatientId)
              .collect(Collectors.toSet()));
        }
        case INPATIENT -> {
          // Check if the encounter is not part of any icu+ treatmentlevel
          normalWardPidSet.addAll(value.stream()
              .filter(UkbEncounter::isCaseClassInpatient)
              .filter(
                  e -> !mapIcu.get(ICU_ECMO).contains(e) && !mapIcu.get(ICU_VENTILATION).contains(e)
                      && !mapIcu.get(ICU).contains(e))
              .map(UkbEncounter::getPatientId)
              .collect(Collectors.toSet()));
        }
      }
    }
    // print out only the most recent cases in case a patient has more than one case with the same
    // treatmentlevel
    if (treatmentLevel == OUTPATIENT) {
      resultEncounters = getYoungestCases(treatmentLevel, mapPositiveEncounterByClass,
          ambulantPidSet);
    } else if (treatmentLevel == INPATIENT) {
      resultEncounters = getYoungestCases(treatmentLevel, mapPositiveEncounterByClass,
          normalWardPidSet);
    }

    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByClass");
    return resultEncounters;
  }

  /**
   * Finds the youngest encounters for each patient based on the encounter start time.
   *
   * @param treatmentLevel              The treatment level to filter encounters by (e.g.,
   *                                    ICU_ECMO).
   * @param mapPositiveEncounterByClass A map containing all positive encounters grouped by their
   *                                    class.
   * @param setInpatientPids            A set containing the patient IDs of in-patient patients.
   * @return A list of encounters, where each encounter represents the youngest encounter for its
   * respective patient.
   */
  private List<UkbEncounter> getYoungestCases(TreatmentLevels treatmentLevel,
      Map<TreatmentLevels, List<UkbEncounter>> mapPositiveEncounterByClass,
      Set<String> setInpatientPids) {

    try (Stream<UkbEncounter> encounterStream = mapPositiveEncounterByClass.get(treatmentLevel)
        .stream()) {
      return encounterStream
          // Filter encounters for the desired treatment level and in-patient patients with valid
          // start times
          .filter(encounter -> setInpatientPids.contains(encounter.getPatientId())
              && encounter.isPeriodStartExistent())
          .collect(Collectors.groupingBy(UkbEncounter::getPatientId))
          .values()
          .stream()
          // Find the encounter with the earliest start time (youngest)
          .map(encounterList -> encounterList.stream()
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
   * Creates a list which contains encounter who have or had icu, ventilation or ecmo as highest
   * treatmentlevel. [needed in data item: cumulative.maxtreatmentlevel]
   *
   * @param mapIcu         Map that assigns a list of case numbers to an ICU treatment level class.
   * @param treatmentLevel The treatmentlevel (e.g. {@link TreatmentLevels#ICU}) which is going to
   *                       be checked as a separation criterion.
   * @return List of all encounters that have the given treatment level as maximum treatment level.
   */
  @SuppressWarnings("incomplete-switch")
  public List<UkbEncounter> getCumulativeByIcuLevel(
      Map<TreatmentLevels, List<UkbEncounter>> mapIcu,
      TreatmentLevels treatmentLevel) {
    List<UkbEncounter> resultList = new ArrayList<>();
    HashMap<String, List<UkbEncounter>> mapPidCases = new HashMap<>();
    log.debug("started getCumulativeByIcuLevel [" + treatmentLevel + "]");
    Instant startTime = TimerTools.startTimer();

    List<UkbEncounter> icuEncounters = mapIcu.get(ICU);
    List<UkbEncounter> ventilationEncounters = mapIcu.get(ICU_VENTILATION);
    List<UkbEncounter> ecmoEncounters = mapIcu.get(ICU_ECMO);
    // Initial flattening of the patient ids by treatment level
    if (setVentilationPids == null || setEcmoPids == null) {
      setVentilationPids = ventilationEncounters.stream()
          .map(UkbEncounter::getPatientId).collect(
              Collectors.toSet());
      setEcmoPids = ecmoEncounters.stream()
          .map(UkbEncounter::getPatientId).collect(
              Collectors.toSet());
    }
    // PIDs of the patients that had no ventilation and no ecmo.
    Set<String> setIcuOnlyPids = new HashSet<>();
    Set<UkbEncounter> setEncounterToBeRemoved = new HashSet<>();

    // Process based on the treatment level
    switch (treatmentLevel) {
      case ICU: {
        // Process ICU encounters
        for (UkbEncounter icuEncounter : icuEncounters) {
          String currentPid = icuEncounter.getPatientId();
          if (!setEcmoPids.contains(currentPid) && !setVentilationPids.contains(currentPid)) {
            addPidToMap(mapPidCases, icuEncounter);
            setIcuOnlyPids.add(icuEncounter.getPatientId());
          }
        }

        // Add first disease-positive encounter for each patient
        resultList.addAll(getPatientsFirstDiseasePositiveEncounter(mapPidCases));

        // Remove encounters with higher treatment levels than ICU
        ventilationEncounters.stream()
            .filter(ventEncounter -> setIcuOnlyPids.contains(ventEncounter.getPatientId()))
            .forEach(setEncounterToBeRemoved::add);

        ecmoEncounters.stream()
            .filter(ecmoEncounter -> setIcuOnlyPids.contains(ecmoEncounter.getPatientId()))
            .forEach(setEncounterToBeRemoved::add);

        resultList.removeAll(setEncounterToBeRemoved);
        break;
      }
      case ICU_VENTILATION: {
        // Process ventilation encounters
        ventilationEncounters.stream()
            .filter(ventEncounter -> !ecmoEncounters.contains(ventEncounter))
            .forEach(encounter -> addPidToMap(mapPidCases, encounter));

        // Add first disease-positive encounter for each patient
        resultList.addAll(getPatientsFirstDiseasePositiveEncounter(mapPidCases));
        break;
      }
      case ICU_ECMO: {
        // Process ECMO encounters
        ecmoEncounters.forEach(encounter -> addPidToMap(mapPidCases, encounter));

        // Add first disease-positive encounter for each patient
        resultList.addAll(getPatientsFirstDiseasePositiveEncounter(mapPidCases));
        break;
      }
    }

    TimerTools.stopTimerAndLog(startTime, "finished getCumulativeByIcuLevel");
    return resultList;
  }

  /**
   * Identify patients' first positive corona cases without considering any subsequent
   * disease-positive cases.
   *
   * @param mapPidCase A map that assigns all associated disease-positive cases (0:n) to a pid.
   * @return List of {@link UkbEncounter} resources of first disease-positive cases of transferred
   * patients.
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
   * Returns the case from an encounter list with the lowest admission date.
   * ({@link UkbEncounter#getPeriod()})
   *
   * @param mapEncounterByPid Map of all encounters per pid.
   * @return The oldest positive C19 case of a patient.
   */
  private UkbEncounter getFirstDiseasePositiveCase(
      Map.Entry<String, List<UkbEncounter>> mapEncounterByPid) {
    return mapEncounterByPid.getValue().stream().filter(UkbEncounter::isPeriodStartExistent)
        .min(Comparator.comparing(x -> x.getPeriod().getStart())).orElse(null);
  }

  /**
   * Add specific encounter to a map.
   *
   * @param mapPidAndCase Map consisting of encounter sorted by their pids.
   * @param encounter     The {@link UkbEncounter} that has to be added.
   */
  private void addPidToMap(Map<String, List<UkbEncounter>> mapPidAndCase,
      UkbEncounter encounter) {
    if (mapPidAndCase.containsKey(encounter.getPatientId())) {
      mapPidAndCase.get(encounter.getPatientId()).add(encounter);
    } else {
      // Initialization of the encounter list for the given patient
      mapPidAndCase.put(encounter.getPatientId(),
          new ArrayList<>(Collections.singletonList(encounter)));
    }
  }
}
