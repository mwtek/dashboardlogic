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
package de.ukbonn.mwtek.dashboardlogic.logic.timeline;

import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.DAY_IN_SECONDS;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric.ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric.INVASIVE_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineTests.getDateMapWithoutValues;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityContactIcuLocationMap;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Period;

/**
 * This class is used for generating the data item {@link DiseaseDataItem
 * kira.pedriatric.timeline.maxtreatmentlevel}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraPedTimelineMaxTreatmentLevel extends DashboardDataItemLogic
    implements TimelineFunctionalities {

  /**
   * Creates a timeline for the current pediatric treatment level. Each encounter is counted exactly
   * once per day under its highest treatment level.
   */
  // --- Helper record for efficient ICU interval tracking ---
  record TimeInterval(long start, long end) {}

  public Map<String, List<String>> debugData;

  public Map<String, List<Integer>> createPediatricTreatmentLevelTimeline(
      List<MiiEncounter> facilityEncounters,
      List<MiiProcedure> icuProcedures,
      FacilityContactIcuLocationMap facilityContactIcuLocationMap,
      InputCodeSettings inputCodeSettings,
      boolean useIcuUndiff) {

    log.debug("started createPediatricTreatmentLevelTimeline");

    // ICU intervals per encounter
    Map<String, List<TimeInterval>> icuIntervalMap =
        buildIcuTimeIndex(facilityContactIcuLocationMap);

    // Only needed for differentiated view
    Map<String, List<MiiProcedure>> proceduresByCase =
        useIcuUndiff
            ? Map.of()
            : icuProcedures.stream().collect(Collectors.groupingBy(MiiProcedure::getCaseId));

    // Use predefined date map
    Map<Long, Long> dateMap = getDateMapWithoutValues(DataItemContext.KIDS_RADAR);
    Map<Long, Map<String, Set<String>>> timeline = new TreeMap<>();
    for (Long day : dateMap.keySet()) {
      timeline.put(day, new HashMap<>());
    }

    long timelineStart = Collections.min(timeline.keySet());
    long timelineEnd = Collections.max(timeline.keySet());

    for (MiiEncounter encounter : facilityEncounters) {
      if (!encounter.hasPeriod() || !encounter.getPeriod().hasStart()) continue;

      String facilityId = encounter.getFacilityContactId();
      long encounterStart =
          normalizeToMidnight(DateTools.dateToUnixTime(encounter.getPeriod().getStart()));
      long encounterEnd =
          encounter.getPeriod().hasEnd()
              ? normalizeToMidnight(DateTools.dateToUnixTime(encounter.getPeriod().getEnd()))
              : normalizeToMidnight(DateTools.dateToUnixTime(DateTools.getCurrentDateTime()));

      long effectiveStart = Math.max(timelineStart, encounterStart);
      long effectiveEnd = Math.min(timelineEnd, encounterEnd);

      List<TimeInterval> icuIntervals = icuIntervalMap.getOrDefault(facilityId, List.of());
      List<MiiProcedure> procedures =
          useIcuUndiff ? List.of() : proceduresByCase.getOrDefault(facilityId, List.of());

      // For differentiated view, we keep the "max level so far" behavior on the same day.
      String previousMaxLevel = TreatmentLevels.Pediatric.NORMAL_WARD;

      for (long day = effectiveStart; day <= effectiveEnd; day += DAY_IN_SECONDS) {
        if (useIcuUndiff) {
          // Undifferentiated view: ICU presence -> UNDIFFERENTIATED, otherwise NORMAL_WARD
          String lvl =
              isInIcuAt(day, icuIntervals)
                  ? TreatmentLevels.Pediatric.UNDIFFERENTIATED
                  : TreatmentLevels.Pediatric.NORMAL_WARD;

          timeline.get(day).computeIfAbsent(lvl, k -> new HashSet<>()).add(facilityId);
        } else {
          // Differentiated view: determine fine-grained level and keep max so far
          String level =
              determinePediatricLevelAt(day, procedures, icuIntervals, inputCodeSettings);
          if (TreatmentLevels.Pediatric.ORDERED.indexOf(level)
              > TreatmentLevels.Pediatric.ORDERED.indexOf(previousMaxLevel)) {
            previousMaxLevel = level;
          }
          timeline.get(day).computeIfAbsent(previousMaxLevel, k -> new HashSet<>()).add(facilityId);
        }
      }
    }

    // Build final output
    Map<String, List<Integer>> levelCounts = new LinkedHashMap<>();
    List<Integer> dateList = new ArrayList<>();

    if (useIcuUndiff) {
      // Only NORMAL_WARD and UNDIFFERENTIATED in undiff mode
      levelCounts.put(TreatmentLevels.Pediatric.NORMAL_WARD, new ArrayList<>());
      levelCounts.put(TreatmentLevels.Pediatric.UNDIFFERENTIATED, new ArrayList<>());

      for (Map.Entry<Long, Map<String, Set<String>>> entry : timeline.entrySet()) {
        long day = entry.getKey();
        dateList.add((int) day);

        Map<String, Set<String>> map = entry.getValue();
        levelCounts
            .get(TreatmentLevels.Pediatric.NORMAL_WARD)
            .add(map.getOrDefault(TreatmentLevels.Pediatric.NORMAL_WARD, Set.of()).size());
        levelCounts
            .get(TreatmentLevels.Pediatric.UNDIFFERENTIATED)
            .add(map.getOrDefault(TreatmentLevels.Pediatric.UNDIFFERENTIATED, Set.of()).size());
      }
    } else {
      // Differentiated: include all ordered pediatric levels
      for (String level : TreatmentLevels.Pediatric.ORDERED) {
        levelCounts.put(level, new ArrayList<>());
      }
      for (Map.Entry<Long, Map<String, Set<String>>> entry : timeline.entrySet()) {
        long day = entry.getKey();
        dateList.add((int) day);

        Map<String, Set<String>> map = entry.getValue();
        for (String level : TreatmentLevels.Pediatric.ORDERED) {
          levelCounts.get(level).add(map.getOrDefault(level, Set.of()).size());
        }
      }
    }
    levelCounts.put(DATE, dateList);
    return levelCounts;
  }

  private Map<String, List<TimeInterval>> buildIcuTimeIndex(
      FacilityContactIcuLocationMap icuLocationMap) {
    Map<String, List<TimeInterval>> result = new HashMap<>();
    for (Map.Entry<String, List<EncounterLocationComponent>> entry :
        icuLocationMap.asMap().entrySet()) {
      List<TimeInterval> intervals =
          entry.getValue().stream()
              .filter(loc -> loc.hasPeriod() && loc.getPeriod().hasStart())
              .map(
                  loc -> {
                    long start = DateTools.dateToUnixTime(loc.getPeriod().getStart());
                    long end =
                        loc.getPeriod().hasEnd()
                            ? DateTools.dateToUnixTime(loc.getPeriod().getEnd())
                            : DateTools.dateToUnixTime(DateTools.getCurrentDateTime());
                    return new TimeInterval(start, end);
                  })
              .toList();
      result.put(entry.getKey(), intervals);
    }
    return result;
  }

  private String determinePediatricLevelAt(
      long dateUnix,
      List<MiiProcedure> procedures,
      List<TimeInterval> icuIntervals,
      InputCodeSettings inputCodeSettings) {

    boolean isOnIcu =
        icuIntervals.stream().anyMatch(i -> i.start() <= dateUnix && i.end() >= dateUnix);

    if (procedures.stream()
        .anyMatch(
            p ->
                isProcedureActiveOnDate(p, dateUnix)
                    && matchesCode(p, inputCodeSettings.getProcedureEcmoCodes()))) {
      return ECMO;
    }

    if (procedures.stream()
        .anyMatch(
            p ->
                isProcedureActiveOnDate(p, dateUnix)
                    && matchesCode(p, inputCodeSettings.getProcedureVentilationCodes()))) {
      return INVASIVE_VENTILATION;
    }

    if (procedures.stream()
        .anyMatch(
            p ->
                isProcedureActiveOnDate(p, dateUnix)
                    && matchesCode(p, inputCodeSettings.getProcedureCpapCodes()))) {
      return TreatmentLevels.Pediatric.CPAP;
    }

    if (procedures.stream()
        .anyMatch(
            p ->
                isProcedureActiveOnDate(p, dateUnix)
                    && matchesCode(p, inputCodeSettings.getProcedureHighFlowCodes()))) {
      return TreatmentLevels.Pediatric.HIGHFLOW;
    }

    if (isOnIcu) return ICU;

    return TreatmentLevels.Pediatric.NORMAL_WARD;
  }

  private boolean isProcedureActiveOnDate(MiiProcedure procedure, long dateUnix) {
    if (procedure == null || !procedure.hasPerformedPeriod()) return false;
    Period period = procedure.getPerformedPeriod();
    if (!period.hasStart()) return false;

    long start = DateTools.dateToUnixTime(period.getStart());
    long end =
        period.hasEnd()
            ? DateTools.dateToUnixTime(period.getEnd())
            : DateTools.dateToUnixTime(DateTools.getCurrentDateTime());

    return start <= dateUnix && end >= dateUnix;
  }

  /**
   * Returns true if the given midnight 'day' lies within any ICU interval. Adjust if your intervals
   * are treated as open/closed differently.
   */
  private boolean isInIcuAt(long dayMidnightUnixSec, List<TimeInterval> icuIntervals) {
    long dayEnd = dayMidnightUnixSec + DAY_IN_SECONDS;
    for (TimeInterval iv : icuIntervals) {
      long s = normalizeToMidnight(iv.start());
      long e = normalizeToMidnight(iv.end());
      if (s < dayEnd && dayMidnightUnixSec < e) {
        return true;
      }
    }
    return false;
  }

  public Map<String, List<String>> getDebugData() {
    Map<String, List<String>> output = new LinkedHashMap<>();
    return output;
  }
}
