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
package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.YEAR_FORMAT;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.YEAR_MONTH_FORMAT;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TimelineTools {

  public static final String YEAR_FORMAT = "yyyy";
  public static final String YEAR_MONTH_FORMAT = "yyyy-MM";
  public static final String YEAR_MONTH_DAY_FORMAT = "yyyy-MM-dd";
  public static final String YEAR_QUARTER_FORMAT = "yyyy-Qn";

  /**
   * Retrieves the index corresponding to the given timestamp.
   *
   * @param timestampSec The timestamp to search for (epoch seconds).
   * @param dates The list with all the midnight timestamps used in the timeline (may be Integer or
   *     Long; values in epoch seconds).
   * @return The index corresponding to the given timestamp, or -1 if not found.
   */
  public static int getIndexByTimestamp(long timestampSec, List<? extends Number> dates) {
    if (dates == null || dates.isEmpty()) return -1;

    // Compare by value to handle Integer vs. Long
    for (int i = 0; i < dates.size(); i++) {
      Number n = dates.get(i);
      if (n != null && n.longValue() == timestampSec) {
        return i;
      }
    }
    return -1;
  }

  /** Core helper to build a sequence from start to end (inclusive) stepping by the given unit. */
  private static List<String> generateSequence(
      LocalDate start, LocalDate endInclusive, ChronoUnit step, DateTimeFormatter formatter) {
    List<String> out = new ArrayList<>();
    for (LocalDate d = start; !d.isAfter(endInclusive); d = d.plus(1, step)) {
      out.add(d.format(formatter));
    }
    return out;
  }

  /**
   * Generates a list of formatted periods from start date up to: - YEAR_FORMAT: current year
   * (inclusive) - YEAR_MONTH_FORMAT: current month (inclusive or exclusive depending on flag) -
   * YEAR_QUARTER_FORMAT: the last fully completed calendar quarter before "now"
   */
  public static List<String> generateDateList(Date startDate, String pattern) {
    // keep old signature as a convenience overload
    return generateDateList(startDate, pattern, false);
  }

  /**
   * Generates a list of formatted periods from start date up to: - YEAR_FORMAT: current year
   * (inclusive) - YEAR_MONTH_FORMAT: current month (inclusive or, if excludeCurrentMonth is true,
   * previous month) - YEAR_QUARTER_FORMAT: the last fully completed calendar quarter before "now"
   *
   * @param startDate start date (must not be null)
   * @param pattern format pattern (must not be null)
   * @param excludeCurrentMonth if true and pattern == YEAR_MONTH_FORMAT, the current calendar month
   *     is excluded and the list ends at the last fully completed month
   */
  public static List<String> generateDateList(
      Date startDate, String pattern, boolean excludeCurrentMonth) {

    Objects.requireNonNull(startDate, "startDate must not be null");
    Objects.requireNonNull(pattern, "pattern must not be null");

    // Convert start to first day of its month
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startDate);
    LocalDate start =
        LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, 1);

    if (YEAR_QUARTER_FORMAT.equals(pattern)) {
      return generateQuarterListToLastFullQuarter(start);
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    LocalDate end = LocalDate.now().withDayOfMonth(1); // first day of current month

    switch (pattern) {
      case YEAR_FORMAT -> {
        // YEAR_FORMAT is not affected by excludeCurrentMonth
        start = start.withMonth(1).withDayOfMonth(1);
        end = end.withMonth(1).withDayOfMonth(1); // current year start
        return generateSequence(start, end, ChronoUnit.YEARS, formatter);
      }
      case YEAR_MONTH_FORMAT -> {
        if (excludeCurrentMonth) {
          // end at the last fully completed month
          end = end.minusMonths(1);
        }
        return generateSequence(start, end, ChronoUnit.MONTHS, formatter);
      }
      default -> throw new IllegalArgumentException("Unsupported pattern: " + pattern);
    }
  }

  /**
   * Returns the last N fully completed months formatted with the given pattern. Example (now =
   * 2025-11-12, months = 12): 2024-10 ... 2025-09
   */
  public static List<String> generateLastFullMonths(String pattern, int months) {
    Objects.requireNonNull(pattern, "pattern must not be null");
    if (months <= 0) throw new IllegalArgumentException("months must be > 0");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    LocalDate end = LocalDate.now().withDayOfMonth(1).minusMonths(1);
    LocalDate start = end.minusMonths(months - 1);

    return generateSequence(start, end, ChronoUnit.MONTHS, formatter);
  }

  /**
   * Returns years from the calendar year AFTER {@code startDate} up to the last fully completed
   * year before today. Delegates to {@link #generateDateList(Date, String)}. Used for the
   * `kira.kjp.timeline.diags.admission` item for example.
   */
  public static List<String> generateYearListToLastFullYear(Date startDate) {
    Objects.requireNonNull(startDate, "startDate must not be null");

    // Compute bounds
    Calendar cal = Calendar.getInstance();
    cal.setTime(startDate);
    int startYearExclusive = cal.get(Calendar.YEAR); // exclude this year
    int lastFullYear = java.time.Year.now().getValue() - 1;

    if (startYearExclusive + 1 > lastFullYear) return List.of();

    // Reuse the existing generator, then filter in place
    return generateDateList(startDate, YEAR_FORMAT).stream()
        .map(Integer::parseInt)
        .filter(y -> y >= startYearExclusive + 1 && y <= lastFullYear)
        .map(String::valueOf)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Generates quarter labels "yyyy-Qn" from the quarter containing {@code start} up to the last
   * fully completed quarter before now.
   */
  private static List<String> generateQuarterListToLastFullQuarter(LocalDate start) {
    // Align start to first month of its quarter
    YearMonth startYm = YearMonth.of(start.getYear(), start.getMonth());
    YearMonth startQYm = firstMonthOfQuarter(startYm);

    // Determine the last full quarter (i.e., previous quarter relative to now)
    YearMonth nowYm = YearMonth.now();
    YearMonth currentQuarterYm = firstMonthOfQuarter(nowYm);
    YearMonth lastFullQuarterYm = currentQuarterYm.minusMonths(3);

    // If start is after end, return empty list
    if (startQYm.isAfter(lastFullQuarterYm)) return List.of();

    List<String> out = new ArrayList<>();
    for (YearMonth ym = startQYm; !ym.isAfter(lastFullQuarterYm); ym = ym.plusMonths(3)) {
      int q = quarterOf(ym);
      out.add(ym.getYear() + "-Q" + q);
    }
    return out;
  }

  private static YearMonth firstMonthOfQuarter(YearMonth ym) {
    int qStartMonth = ((ym.getMonthValue() - 1) / 3) * 3 + 1; // 1,4,7,10
    return YearMonth.of(ym.getYear(), qStartMonth);
  }

  private static int quarterOf(YearMonth ym) {
    return ((ym.getMonthValue() - 1) / 3) + 1; // 1..4
  }
}
