package de.ukbonn.mwtek.dashboardlogic.logic.timeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ensures that the day assignment helpers which replaced the day-by-day scans produce exactly the
 * same day memberships as the loops they replaced.
 */
public class TimelineDayAssignmentTests {

  private static final long DAY_IN_SECONDS = 86400L;
  /** Arbitrary anchor for the first day of the timeline (epoch seconds). */
  private static final long FIRST_DAY = 1_700_000_400L;
  private static final int DAYS = 12;

  private static MiiEncounter caseWithPeriod(String id, long startUnix, Long endUnix) {
    MiiEncounter encounter = new MiiEncounter();
    encounter.setId(id);
    encounter.getPeriod().setStart(DateTools.unixTimeSecondsToDate(startUnix));
    if (endUnix != null) {
      encounter.getPeriod().setEnd(DateTools.unixTimeSecondsToDate(endUnix));
    }
    return encounter;
  }

  /** The overlap rule the day loop used before the cases were bucketed. */
  private static boolean oldRuleMatches(MiiEncounter encounter, long checkDate) {
    long start = DateTools.dateToUnixTime(encounter.getPeriod().getStart()) - DAY_IN_SECONDS;
    long end =
        DateTools.dateToUnixTime(
                encounter.getPeriod().getEnd() != null
                    ? encounter.getPeriod().getEnd()
                    : new Date())
            + DAY_IN_SECONDS;
    return start < checkDate && end >= checkDate;
  }

  @Test
  @DisplayName(
      "bucketCasesByDay assigns every case to exactly those days the old per-day filter accepted.")
  void testBucketCasesByDayMatchesOldFilter() {
    List<MiiEncounter> cases =
        List.of(
            caseWithPeriod("EK-1", FIRST_DAY - 3 * DAY_IN_SECONDS, FIRST_DAY + 2 * DAY_IN_SECONDS),
            caseWithPeriod("EK-2", FIRST_DAY + 5 * DAY_IN_SECONDS, FIRST_DAY + 5 * DAY_IN_SECONDS),
            caseWithPeriod("EK-3", FIRST_DAY + 20 * DAY_IN_SECONDS, FIRST_DAY + 25 * DAY_IN_SECONDS),
            caseWithPeriod("EK-4", FIRST_DAY - 40 * DAY_IN_SECONDS, FIRST_DAY - 30 * DAY_IN_SECONDS),
            caseWithPeriod("EK-5", FIRST_DAY + DAY_IN_SECONDS / 2, FIRST_DAY + DAY_IN_SECONDS / 2),
            // Open-ended case: the old loop substituted the current date
            caseWithPeriod("EK-6", FIRST_DAY + 9 * DAY_IN_SECONDS, null));

    Map<Long, List<MiiEncounter>> buckets =
        TimelineMaxTreatmentLevel.bucketCasesByDay(
            cases, FIRST_DAY, FIRST_DAY + (DAYS - 1) * DAY_IN_SECONDS);

    for (int day = 0; day < DAYS; day++) {
      long checkDate = FIRST_DAY + day * DAY_IN_SECONDS;
      List<MiiEncounter> expected = new ArrayList<>();
      for (MiiEncounter encounter : cases) {
        if (oldRuleMatches(encounter, checkDate)) {
          expected.add(encounter);
        }
      }
      assertEquals(expected, buckets.getOrDefault(checkDate, List.of()), "day " + day);
    }
    // No days outside the timeline are created
    long lastDay = FIRST_DAY + (DAYS - 1) * DAY_IN_SECONDS;
    assertTrue(
        buckets.keySet().stream().allMatch(day -> day >= FIRST_DAY && day <= lastDay),
        "buckets must only contain days of the timeline");
  }

  @Test
  @DisplayName("countByDay counts each timestamp on the same day the old range filter counted it on.")
  void testCountByDayMatchesOldFilter() {
    List<Long> timestamps =
        List.of(
            FIRST_DAY,
            FIRST_DAY + 60,
            FIRST_DAY + DAY_IN_SECONDS - 1,
            FIRST_DAY + DAY_IN_SECONDS,
            FIRST_DAY + 7 * DAY_IN_SECONDS + 12345,
            FIRST_DAY - 1, // before the timeline
            FIRST_DAY - 10 * DAY_IN_SECONDS);

    Map<Long, Long> counted = TimelineRecruitment.countByDay(timestamps, FIRST_DAY);

    for (int day = 0; day < DAYS; day++) {
      long dayStart = FIRST_DAY + day * DAY_IN_SECONDS;
      long dayEnd = dayStart + DAY_IN_SECONDS;
      long expected =
          timestamps.stream().filter(ts -> ts >= dayStart && ts < dayEnd).count();
      assertEquals(expected, counted.getOrDefault(dayStart, 0L), "day " + day);
    }
    // Timestamps before the timeline are not counted anywhere
    long total = counted.values().stream().mapToLong(Long::longValue).sum();
    assertEquals(timestamps.size() - 2, total);
  }

  @Test
  @DisplayName(
      "addLabTestToTimeline increments the same day the day-by-day search incremented, and ignores"
          + " days that are not part of the timeline.")
  void testAddLabTestToTimelineMatchesOldSearch() {
    Map<Long, Long> newMap = new ConcurrentHashMap<>();
    Map<Long, Long> oldMap = new LinkedHashMap<>();
    for (int day = 0; day < DAYS; day++) {
      newMap.put(FIRST_DAY + day * DAY_IN_SECONDS, 0L);
      oldMap.put(FIRST_DAY + day * DAY_IN_SECONDS, 0L);
    }

    List<Long> labDates =
        List.of(
            FIRST_DAY,
            FIRST_DAY + DAY_IN_SECONDS - 1,
            FIRST_DAY + DAY_IN_SECONDS,
            FIRST_DAY + 3 * DAY_IN_SECONDS + 999,
            FIRST_DAY - 5, // before the timeline
            FIRST_DAY + 100 * DAY_IN_SECONDS); // after the timeline

    for (Long labDate : labDates) {
      TimelineTests.addLabTestToTimeline(labDate, FIRST_DAY, newMap);
      // The old implementation: walk day by day until the date falls into [day, day + 24h)
      for (long checkDate = FIRST_DAY; checkDate <= FIRST_DAY + (DAYS - 1) * DAY_IN_SECONDS;
          checkDate += DAY_IN_SECONDS) {
        long increasedUnix = checkDate + DAY_IN_SECONDS - 1;
        if (checkDate <= labDate && increasedUnix >= labDate) {
          oldMap.compute(checkDate, (k, v) -> v != null ? v + 1 : null);
          break;
        }
      }
    }

    assertEquals(oldMap, newMap);
  }

  @Test
  @DisplayName("A timeline map without the day of a timestamp stays untouched.")
  void testTimestampOutsideTimelineIsIgnored() {
    Map<Long, Long> map = new HashMap<>();
    map.put(FIRST_DAY, 0L);

    TimelineTests.addLabTestToTimeline(FIRST_DAY - DAY_IN_SECONDS, FIRST_DAY, map);
    TimelineTests.addLabTestToTimeline(FIRST_DAY + 50 * DAY_IN_SECONDS, FIRST_DAY, map);

    assertEquals(Map.of(FIRST_DAY, 0L), map);
  }
}
