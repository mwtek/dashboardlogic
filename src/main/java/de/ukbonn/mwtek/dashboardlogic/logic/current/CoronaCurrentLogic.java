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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import lombok.extern.slf4j.Slf4j;

/**
 * All logic concerning the current logic {@link CoronaDataItem data items}
 * 
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 * 
 */
@Slf4j
public class CoronaCurrentLogic {

  public CoronaCurrentLogic() {
  }

  /**
   * Creation of a list with the current inpatient c19-positive encounters that are currently on a
   * standard ward [needed for the data item current.treatmentlevel]
   * 
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @param mapCurrentIcu A list of all current inpatient c19-positive cases separated by treatment
   *          level
   * @param listIcuProcedures The {@link UkbProcedure} resources, which include information about
   *          ECMO / artificial ventilation periods
   * @return List with ongoing inpatient encounter that are currently on a standard ward
   */
  public static List<UkbEncounter> getCurrentStandardWardEncounter(
      List<UkbEncounter> listEncounters, Map<String, List<UkbEncounter>> mapCurrentIcu,
      List<UkbProcedure> listIcuProcedures) {
    List<UkbEncounter> resultList = new ArrayList<>();
    List<UkbEncounter> listIcuEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter =
        mapCurrentIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    // just a check of the inpatient encounters is needed here.
    List<UkbEncounter> listEncounterWithoutOutpatients = listEncounters.parallelStream()
        .filter(x -> CoronaResultFunctionality.isCaseClassInpatient(x))
        .collect(Collectors.toList());

    // check inpatient
    try {
      listEncounterWithoutOutpatients.forEach(e -> {
        AtomicBoolean stationary = new AtomicBoolean(false);
        if (!e.getPeriod()
            .hasEnd() && e.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())) {
          if (!listIcuEncounter.contains(e)) {
            if (!listVentilationEncounter.contains(e) && !listEcmoEncounter.contains(e)) {
              stationary.set(true);
            } else {
              if (!hasActiveIcuProcedure(listIcuProcedures, e,
                  CoronaFixedValues.VENT_CODE.getValue())
                  && !hasActiveIcuProcedure(listIcuProcedures, e,
                      CoronaFixedValues.ECMO_CODE.getValue())) {
                stationary.set(true);
              }
            }
          }
        }
        if (stationary.get())
          resultList.add(e);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
    return resultList;
  }

  /**
   * Used to calculate the current treatmentlevel for the icu encounters, for current.treatmentlevel
   * 
   * @param mapCurrentIcu A list of all current inpatient c19-positive cases separated by treatment
   *          level
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @param listIcuProcedures The {@link UkbProcedure} resources, which include information about
   *          ECMO / artificial ventilation periods
   * @param icuTreatmentLevel The Icu treatment level for which the encounter are to be retrieved
   *          (e.g. {@link CoronaFixedValues#ICU})
   * @return Returns a list of ongoing ICU Encounter
   */
  @SuppressWarnings("incomplete-switch")
  public static List<UkbEncounter> getCurrentEncounterByIcuLevel(
      Map<String, List<UkbEncounter>> mapCurrentIcu, List<UkbEncounter> listEncounters,
      List<UkbProcedure> listIcuProcedures, CoronaFixedValues icuTreatmentLevel) {
    List<UkbEncounter> currentIcuList = new ArrayList<>();

    List<UkbEncounter> listICUEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter =
        mapCurrentIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapCurrentIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    boolean isPositive = false;
    boolean hasPeriodEnd = false;
    try {
      switch (icuTreatmentLevel) {
        case ICU:
          for (UkbEncounter encounter : listICUEncounter) {
            isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasPeriodEnd = encounter.getPeriod()
                .hasEnd();

            // check End, flag and appearance in higher treatmentlevel
            if (!hasPeriodEnd && isPositive) {
              if (!listVentilationEncounter.contains(encounter)
                  && !listEcmoEncounter.contains(encounter)) {
                currentIcuList.add(encounter);
              }
            }
          }
          break;
        case ICU_VENTILATION:
          for (UkbEncounter encounter : listVentilationEncounter) {
            isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasPeriodEnd = encounter.getPeriod()
                .hasEnd();

            // check End, flag, procedure status and appearance in higher treatmentlevel
            if (!hasPeriodEnd && isPositive) {
              for (UkbProcedure ventilation : listIcuProcedures) {
                if (ventilation.getCaseId()
                    .equals(encounter.getId())) {
                  if (!listEcmoEncounter.contains(encounter)) {
                    if (hasActiveIcuProcedure(listIcuProcedures, encounter,
                        CoronaFixedValues.VENT_CODE.getValue())) {
                      currentIcuList.add(encounter);
                      break;
                    }
                  } else {
                    // check if higher treatmentlevel is finished and lower is ongoing
                    if (!hasActiveIcuProcedure(listIcuProcedures, encounter,
                        CoronaFixedValues.ECMO_CODE.getValue())
                        && hasActiveIcuProcedure(listIcuProcedures, encounter,
                            CoronaFixedValues.VENT_CODE.getValue())) {
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
            hasPeriodEnd = encounter.getPeriod()
                .hasEnd();

            // check end, flag and procedure status
            if (!hasPeriodEnd && isPositive) {
              for (UkbProcedure ecmo : listIcuProcedures) {
                if (ecmo.getCaseId()
                    .equals(encounter.getId())) {
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
   * @param listIcu The Procedures which contain information on whether they are ventilation or ecmo
   * @param encounter The Encounter to be inspected
   * @param procedureCode The procedure code that is going to be checked (e.g.
   *          {@link CoronaFixedValues#ECMO_CODE})
   * @return true or false whether the Ventilation or Ecmo Procedure is still ongoing or not
   */
  private static boolean hasActiveIcuProcedure(List<UkbProcedure> listIcu, UkbEncounter encounter,
      String procedureCode) {
    boolean isActive = false;
    for (UkbProcedure icu : listIcu) {
      if (icu.getCaseId()
          .equals(encounter.getId())
          && icu.getCategory()
              .getCoding()
              .get(0)
              .getCode()
              .equals(procedureCode)) {
        if (icu.getStatus()
            .equals(ProcedureStatus.INPROGRESS)) {
          isActive = true;
        }
      }
    }
    return isActive;
  }

  /**
   * Search for the maxtreatment of the current ongoing encounter, for the current.maxtreatmentlevel
   * 
   * @param mapIcu Map that assigns a list of case numbers to an ICU treatment level class
   * @param treatmentLevel TreatmentLevel as separation criterion
   * @param listEncounters A list with {@link UkbEncounter} resources
   * @return All current ongoing inpatient c19 positive encounters for the given Maxtreatmentlevel
   */
  @SuppressWarnings("incomplete-switch")
  public static List<UkbEncounter> getNumberOfCurrentMaxTreatmentLevel(
      Map<String, List<UkbEncounter>> mapIcu, CoronaFixedValues treatmentLevel,
      List<UkbEncounter> listEncounters) {
    log.debug("started getNumberOfCurrentMaxTreatmentLevel");
    Instant startTimer = TimerTools.startTimer();

    List<UkbEncounter> listEncountersInpatients = listEncounters.parallelStream()
        .filter(x -> CoronaResultFunctionality.isCaseClassInpatient(x))
        .collect(Collectors.toList());

    List<UkbEncounter> listResult = new ArrayList<>();
    List<UkbEncounter> listIcuEncounter = mapIcu.get(CoronaFixedValues.ICU.getValue());
    List<UkbEncounter> listVentilationEncounter =
        mapIcu.get(CoronaFixedValues.ICU_VENTILATION.getValue());
    List<UkbEncounter> listEcmoEncounter = mapIcu.get(CoronaFixedValues.ICU_ECMO.getValue());

    // Used to check if Positive flag is set and if Period has no end
    boolean isPositive = false;
    boolean hasEnd = false;

    // check whether the current encounters had a higher treatmentlevel before
    try {
      switch (treatmentLevel) {
        case STATIONARY_ITEM:
          for (UkbEncounter encounter : listEncountersInpatients) {
            isPositive = encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = encounter.getPeriod()
                .hasEnd();

            // check encounter class, flagging, and appearance in icu map
            if (!hasEnd) {
              if (isPositive) {
                if (!listIcuEncounter.contains(encounter)) {
                  if (!listVentilationEncounter.contains(encounter)
                      && !listEcmoEncounter.contains(encounter)) {
                    listResult.add(encounter);
                  }
                }
              }
            }
          }
          break;
        // ICU: Same procedure as above
        case ICU:
          for (UkbEncounter currentEncounter : listEncountersInpatients) {
            isPositive =
                currentEncounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = currentEncounter.getPeriod()
                .hasEnd();

            if (!listVentilationEncounter.contains(currentEncounter)
                && !listEcmoEncounter.contains(currentEncounter)
                && listIcuEncounter.contains(currentEncounter)) {
              if (!hasEnd && isPositive)
                listResult.add(currentEncounter);
            }
          }
          break;
        // Ventilation
        case ICU_VENTILATION:
          for (UkbEncounter currentEncounter : listEncountersInpatients) {
            isPositive =
                currentEncounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = currentEncounter.getPeriod()
                .hasEnd();

            // check icu appearance, end date and flag
            if (!hasEnd && isPositive) {
              if (!listEcmoEncounter.contains(currentEncounter)
                  && listVentilationEncounter.contains(currentEncounter)) {
                listResult.add(currentEncounter);
              }
            }
          }
          break;
        // ECMO
        case ICU_ECMO:
          for (UkbEncounter currentEncounter : listEncountersInpatients) {
            isPositive =
                currentEncounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
            hasEnd = currentEncounter.getPeriod()
                .hasEnd();

            if (!hasEnd && isPositive && listEcmoEncounter.contains(currentEncounter)) {
              listResult.add(currentEncounter);
            }
          }
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    TimerTools.stopTimerAndLog(startTimer, "finished getNumberOfCurrentMaxTreatmentLevel");
    return listResult;
  }

  public static List<Long> createCurrentMaxAgeMap(List<UkbPatient> listPatients,
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass,
      List<UkbEncounter> listCurrentMaxEncounter) {
    List<Long> resultList = new ArrayList<>();
    Map<String, UkbEncounter> currentMaxPidAdmissionMap = new HashMap<>();

    List<String> currentMaxEncounterPidList = listCurrentMaxEncounter.stream()
        .map(UkbEncounter::getPatientId)
        .collect(Collectors.toList());

    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        if (currentMaxEncounterPidList.contains(encounter.getPatientId())) {
          CoronaResultFunctionality.sortingFirstAdmissionDateToPid(encounter,
              currentMaxPidAdmissionMap);
        }
      }
    }

    calculateAndCheckAgeGroup(currentMaxPidAdmissionMap, resultList, listPatients);
    Collections.sort(resultList);
    return resultList;
  }

  private static void calculateAndCheckAgeGroup(Map<String, UkbEncounter> pidAdmissionMap,
      List<Long> resultList, List<UkbPatient> listPatients) {
    for (Map.Entry<String, UkbEncounter> entry : pidAdmissionMap.entrySet()) {
      List<Date> bday = listPatients.stream()
          .filter(patient -> entry.getValue()
              .getPatientId()
              .equals(patient.getId()))
          .map(UkbPatient::getBirthDate)
          .collect(Collectors.toList());
      if (entry.getValue()
          .isPeriodStartExistent()) {
        int age = CoronaResultFunctionality.calculateAge(bday.get(0), entry.getValue()
            .getPeriod()
            .getStart());
        long cohortedAge = CoronaResultFunctionality.checkAgeGroup(age);
        resultList.add(cohortedAge);
      }
    }
  }



  public static Map<String, Long> createAdmissionMap(
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass,
      Map<String, List<UkbEncounter>> mapIcu, String treatmentLevel) {
    Map<String, List<UkbEncounter>> encounterMap = new HashMap<>();
    encounterMap.put(CoronaFixedValues.NORMALSTATION.getValue(), new ArrayList<>());
    encounterMap.put(CoronaFixedValues.ICU.getValue(), new ArrayList<>());
    encounterMap.put(CoronaFixedValues.ICU_VENTILATION.getValue(), new ArrayList<>());
    encounterMap.put(CoronaFixedValues.ICU_ECMO.getValue(), new ArrayList<>());

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfToday = now.with(LocalTime.MIN);
    LocalDateTime endOfToday = now.with(LocalTime.MAX);

    long startOfTodayUnix =
        DateTools.dateToUnixTime(Date.from(startOfToday.atZone(ZoneId.systemDefault())
            .toInstant()));
    long endOfTodayUnix =
        DateTools.dateToUnixTime(Date.from(endOfToday.atZone(ZoneId.systemDefault())
            .toInstant()));
    long startOfPrevDayUnix = startOfTodayUnix - CoronaDashboardConstants.dayInSeconds;
    long endOfPrevDayUnix = endOfTodayUnix - CoronaDashboardConstants.dayInSeconds;

    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        if (encounter.isPeriodStartExistent()) {
          long encounterStartUnix = DateTools.dateToUnixTime(encounter.getPeriod()
              .getStart());
          if (startOfPrevDayUnix <= encounterStartUnix && endOfPrevDayUnix >= encounterStartUnix) {
            if (encounter.hasHospitalization() && !encounter.getPeriod()
                .hasEnd()) {
              if (encounter.getHospitalization()
                  .hasAdmitSource()) {
                if (encounter.getHospitalization()
                    .getAdmitSource()
                    .getCoding()
                    .get(0)
                    .getCode()
                    .equals(treatmentLevel)) {

                  if (entry.getKey()
                      .equals(CoronaFixedValues.NORMALSTATION.getValue())) {
                    encounterMap.get(CoronaFixedValues.NORMALSTATION.getValue())
                        .add(encounter);
                  } else if (entry.getKey()
                      .equals(CoronaFixedValues.ICU.getValue())) {
                    encounterMap.get(CoronaFixedValues.ICU.getValue())
                        .add(encounter);
                  } else if (entry.getKey()
                      .equals(CoronaFixedValues.ICU_VENTILATION.getValue())) {
                    encounterMap.get(CoronaFixedValues.ICU_VENTILATION.getValue())
                        .add(encounter);
                  } else if (entry.getKey()
                      .equals(CoronaFixedValues.ICU_ECMO.getValue())) {
                    encounterMap.get(CoronaFixedValues.ICU_ECMO.getValue())
                        .add(encounter);
                  }
                }
              }
            }
          }
        }
      }
    }
    Map<String, Long> resultMap = new HashMap<>();
    resultMap.put(CoronaFixedValues.NORMALSTATION.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.NORMALSTATION.getValue())
            .size()));
    resultMap.put(CoronaFixedValues.ICU.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.ICU.getValue())
            .size()));
    resultMap.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.ICU_VENTILATION.getValue())
            .size()));
    resultMap.put(CoronaFixedValues.ICU_ECMO.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.ICU_ECMO.getValue())
            .size()));
    return resultMap;
  }

  public static Map<String, Long> createAdmissionComplementMap(
      HashMap<String, List<UkbEncounter>> mapCurrentEncounter,
      Map<String, List<UkbEncounter>> mapIcu) {
    Map<String, List<UkbEncounter>> encounterMap = new HashMap<>();
    encounterMap.put(CoronaFixedValues.NORMALSTATION.getValue(), new ArrayList<>());
    encounterMap.put(CoronaFixedValues.ICU.getValue(), new ArrayList<>());
    encounterMap.put(CoronaFixedValues.ICU_VENTILATION.getValue(), new ArrayList<>());
    encounterMap.put(CoronaFixedValues.ICU_ECMO.getValue(), new ArrayList<>());

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfToday = now.with(LocalTime.MIN);
    LocalDateTime endOfToday = now.with(LocalTime.MAX);

    long startOfTodayUnix =
        DateTools.dateToUnixTime(Date.from(startOfToday.atZone(ZoneId.systemDefault())
            .toInstant()));
    long endOfTodayUnix =
        DateTools.dateToUnixTime(Date.from(endOfToday.atZone(ZoneId.systemDefault())
            .toInstant()));
    long startOfPrevDayUnix = startOfTodayUnix - CoronaDashboardConstants.dayInSeconds;
    long endOfPrevDayUnix = endOfTodayUnix - CoronaDashboardConstants.dayInSeconds;

    for (Map.Entry<String, List<UkbEncounter>> entry : mapCurrentEncounter.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        if (encounter.isPeriodStartExistent()) {
          long encounterStartUnix = DateTools.dateToUnixTime(encounter.getPeriod()
              .getStart());
          boolean isBiggerThanStart = startOfPrevDayUnix <= encounterStartUnix;
          if (startOfPrevDayUnix > encounterStartUnix
              || (isBiggerThanStart && endOfPrevDayUnix < encounterStartUnix)) {
            if (!encounter.getPeriod()
                .hasEnd()) {

              if (entry.getKey()
                  .equals(CoronaFixedValues.NORMALSTATION.getValue())) {
                encounterMap.get(CoronaFixedValues.NORMALSTATION.getValue())
                    .add(encounter);
              } else if (entry.getKey()
                  .equals(CoronaFixedValues.ICU.getValue())) {
                encounterMap.get(CoronaFixedValues.ICU.getValue())
                    .add(encounter);
              } else if (entry.getKey()
                  .equals(CoronaFixedValues.ICU_VENTILATION.getValue())) {
                encounterMap.get(CoronaFixedValues.ICU_VENTILATION.getValue())
                    .add(encounter);
              } else if (entry.getKey()
                  .equals(CoronaFixedValues.ICU_ECMO.getValue())) {
                encounterMap.get(CoronaFixedValues.ICU_ECMO.getValue())
                    .add(encounter);
              }
            }
          }
        }
      }
    }
    Map<String, Long> resultMap = new HashMap<>();
    resultMap.put(CoronaFixedValues.NORMALSTATION.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.NORMALSTATION.getValue())
            .size()));
    resultMap.put(CoronaFixedValues.ICU.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.ICU.getValue())
            .size()));
    resultMap.put(CoronaFixedValues.ICU_VENTILATION.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.ICU_VENTILATION.getValue())
            .size()));
    resultMap.put(CoronaFixedValues.ICU_ECMO.getValue(),
        Long.valueOf(encounterMap.get(CoronaFixedValues.ICU_ECMO.getValue())
            .size()));
    return resultMap;
  }

  public static List<Long> createCurrentMaxTotalAgeMap(List<Long> currentMaxStationaryAgeList,
      List<Long> currentMaxIcuAgeList, List<Long> currentMaxIcuVentAgeList,
      List<Long> currentMaxIcuEcmoAgeList) {
    List<Long> resultList = new ArrayList<>();
    if (!currentMaxStationaryAgeList.isEmpty()) {
      resultList.addAll(currentMaxStationaryAgeList);
    }

    if (!currentMaxIcuAgeList.isEmpty()) {
      resultList.addAll(currentMaxIcuAgeList);
    }
    if (!currentMaxIcuVentAgeList.isEmpty()) {
      resultList.addAll(currentMaxIcuVentAgeList);
    }
    if (!currentMaxIcuEcmoAgeList.isEmpty()) {
      resultList.addAll(currentMaxIcuEcmoAgeList);
    }
    Collections.sort(resultList);
    return resultList;
  }
}