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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class TimelineTools {

  /**
   * Retrieves the index corresponding to the given timestamp.
   *
   * @param timestamp The timestamp to search for
   * @param dates The list with all the midnight timestamps used in the timeline.
   * @return The index corresponding to the given timestamp, or -1 if not found
   */
  public static int getIndexByTimestamp(long timestamp, List<? extends Number> dates) {
    OptionalInt indexOptional =
        IntStream.range(0, dates.size()).filter(i -> dates.get(i).equals(timestamp)).findFirst();
    return indexOptional.orElse(-1);
  }

  public static List<String> generateDateList(Date startDate, String pattern) {
    List<String> dateList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

    // Convert Date to LocalDate
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startDate);
    LocalDate start =
        LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, 1);

    // Current date, setting to the first day of the current month
    LocalDate end = LocalDate.now().withDayOfMonth(1);

    while (!start.isAfter(end)) {
      dateList.add(start.format(formatter));
      if (pattern.equals(YEAR_FORMAT)) {
        start = start.plusYears(1);
      } else if (pattern.equals(YEAR_MONTH_FORMAT)) {
        start = start.plusMonths(1);
      } else {
        throw new IllegalArgumentException("Unsupported pattern: " + pattern);
      }
    }

    return dateList;
  }
}
