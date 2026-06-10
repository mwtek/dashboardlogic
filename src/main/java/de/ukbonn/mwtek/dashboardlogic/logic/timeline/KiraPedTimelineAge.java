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
import static de.ukbonn.mwtek.dashboardlogic.logic.timeline.TimelineTests.getDateMapWithoutValues;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByCode;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByValue;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgePedCluster;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data items containing age timelines.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraPedTimelineAge extends DashboardDataItemLogic implements TimelineFunctionalities {

  /**
   * Creates a pediatric COVID age timeline. Counts, per day (midnight-based), how many facility
   * encounters with a COVID condition fall into each pediatric age cluster based on CoreCaseData
   * (ageAtAdmission / ageAtAdmissionInMonths).
   */
  public Map<String, List<Integer>> createPediatricAgeTimeline(
      List<MiiEncounter> facilityContactEncounters,
      List<MiiCondition> conditions,
      Collection<String> conditionIcdCodes,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    Set<String> matchingCaseIds =
        conditions.stream()
            .filter(c -> FhirConditionTools.isIcdCodeInCondition(c, conditionIcdCodes))
            .map(MiiCondition::getCaseId)
            .collect(Collectors.toSet());
    return createPediatricAgeTimelineCore(
        facilityContactEncounters, matchingCaseIds, coreCaseDataByGroups);
  }

  /**
   * Creates a pediatric age timeline for encounters based on lab observations that match the given
   * LOINC codes and observation result type.
   *
   * <p>The method filters lab observations by LOINC code and desired result (e.g., POSITIVE),
   * determines the set of matching case IDs, and then builds a timeline of encounters distributed
   * into pediatric age buckets.
   *
   * @param facilityContactEncounters all facility contact encounters
   * @param labObservations collection of lab observations
   * @param loincCodes set of LOINC codes of interest
   * @param obsResultType type of result to filter by (e.g., POSITIVE, NEGATIVE)
   * @param qualitativeLabCodesSettings settings for qualitative lab codes interpretation
   * @param coreCaseDataByGroups pre-computed case data grouped by keys (e.g. "ALL")
   * @return a map with age bucket labels as keys and lists of daily counts as values. Includes an
   *     additional key for {@code AGE_UNKNOWN} and {@code DATE}.
   */
  public Map<String, List<Integer>> createPediatricAgeTimelineByLoinc(
      List<MiiEncounter> facilityContactEncounters,
      Collection<MiiObservation> labObservations,
      Collection<String> loincCodes,
      DashboardLogicFixedValues obsResultType,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // 1) Select observations by LOINC code
    Set<MiiObservation> byCode = getObservationsByCode(labObservations, loincCodes);

    // 2) Filter to the desired result (e.g., POSITIVE)
    Set<MiiObservation> positive =
        getObservationsByValue(byCode, obsResultType, qualitativeLabCodesSettings);

    // 3) Determine pool of matching case IDs
    Set<String> matchingCaseIds =
        positive.stream()
            .map(MiiObservation::getCaseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // 4) Shared core logic for age timeline calculation
    return createPediatricAgeTimelineCore(
        facilityContactEncounters, matchingCaseIds, coreCaseDataByGroups);
  }

  /**
   * Builds the pediatric age timeline for a set of encounters and case IDs.
   *
   * <p>The method normalizes all encounter periods to midnight-based days, assigns encounters to
   * age buckets (based on {@link CoreCaseData}), and counts the number of unique encounters per day
   * per bucket.
   *
   * @param facilityContactEncounters list of all encounters
   * @param matchingCaseIds set of case IDs that match the observation filter
   * @param coreCaseDataByGroups case data grouped by keys, must include "ALL" for full mapping
   * @return a map where:
   *     <ul>
   *       <li>Each key is an age bucket label (see {@link KiraAgePedCluster#BARS})
   *       <li>{@code AGE_UNKNOWN} is added for cases without resolvable age
   *       <li>{@code DATE} contains the list of days (as integers, UNIX timestamps at midnight)
   *     </ul>
   *     Each value is a list of counts, aligned by day index.
   */
  private Map<String, List<Integer>> createPediatricAgeTimelineCore(
      List<MiiEncounter> facilityContactEncounters,
      Set<String> matchingCaseIds,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // Pre-filter encounters by matching case IDs
    List<MiiEncounter> matchingEncounters =
        facilityContactEncounters.stream()
            .filter(e -> matchingCaseIds.contains(e.getFacilityContactId()))
            .toList();

    // CoreCaseData (created by createCasesByDiag → key "ALL")
    Map<String, CoreCaseData> allCases =
        coreCaseDataByGroups.getOrDefault("ALL", Collections.emptyMap());

    // Initialize timeline (days normalized to midnight)
    Map<Long, Long> dateMap = getDateMapWithoutValues(DataItemContext.KIDS_RADAR);
    Map<Long, Map<String, Set<String>>> timeline = new TreeMap<>();
    for (Long day : dateMap.keySet()) {
      timeline.put(day, new HashMap<>());
    }
    long timelineStart = Collections.min(timeline.keySet());
    long timelineEnd = Collections.max(timeline.keySet());

    // For each day, count each encounter once in its age bucket
    for (MiiEncounter encounter : matchingEncounters) {
      if (!encounter.hasPeriod() || !encounter.getPeriod().hasStart()) continue;

      String facilityId = encounter.getFacilityContactId();
      CoreCaseData caseData = allCases.get(facilityId);
      String ageLabel = resolveAgeLabel(caseData, KidsRadarDataItemContext.PED);

      long encStart =
          normalizeToMidnight(DateTools.dateToUnixTime(encounter.getPeriod().getStart()));
      long encEnd =
          encounter.getPeriod().hasEnd()
              ? normalizeToMidnight(DateTools.dateToUnixTime(encounter.getPeriod().getEnd()))
              : normalizeToMidnight(DateTools.dateToUnixTime(DateTools.getCurrentDateTime()));

      long effectiveStart = Math.max(timelineStart, encStart);
      long effectiveEnd = Math.min(timelineEnd, encEnd);
      if (effectiveStart > effectiveEnd) continue;

      for (long day = effectiveStart; day <= effectiveEnd; day += DAY_IN_SECONDS) {
        timeline.get(day).computeIfAbsent(ageLabel, k -> new HashSet<>()).add(facilityId);
      }
    }

    // Structure the output (all known buckets + unknown + date)
    Map<String, List<Integer>> result = new LinkedHashMap<>();
    for (String label : KiraAgePedCluster.BARS) {
      result.put(label, new ArrayList<>());
    }

    List<Integer> dateList = new ArrayList<>();
    for (Map.Entry<Long, Map<String, Set<String>>> entry : timeline.entrySet()) {
      long day = entry.getKey();
      Map<String, Set<String>> perLabel = entry.getValue();

      dateList.add((int) day);

      for (String label : KiraAgePedCluster.BARS) {
        result.get(label).add(perLabel.getOrDefault(label, Collections.emptySet()).size());
      }
    }

    result.put(DATE, dateList);
    return result;
  }

  public Map<String, List<String>> getDebugData() {
    Map<String, List<String>> output = new LinkedHashMap<>();
    return output;
  }
}
