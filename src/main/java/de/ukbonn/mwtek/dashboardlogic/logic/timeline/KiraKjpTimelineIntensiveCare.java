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

import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.YEAR_MONTH_FORMAT;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.generateDateList;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.generateLastFullMonths;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.GroupedBarChartsCalcItem;
import de.ukbonn.mwtek.dashboardlogic.models.KiraInteger;
import de.ukbonn.mwtek.dashboardlogic.models.KiraMonthAgg;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.ToIntFunction;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Period;

/**
 * This class is used for generating the data items containing kiradar kjp timelines.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraKjpTimelineIntensiveCare extends DashboardDataItemLogic
    implements TimelineFunctionalities {

  public static final String PSYCH_INTENSIVE_CARE_DAYS_BY_MONTH =
      "psychintensive_care_days_by_month";
  public static final String NORMAL_CARE_DAYS_BY_MONTH = "normal_care_days_by_month";
  public static final String TOTAL_CARE_DAYS_BY_MONTH = "total_care_days_by_month";
  public static final String INTENSIVE_TOTAL_RATIO_GROUP =
      "$ratio_psychintensive_total_care_days_by_month";
  public static final String INTENSIVE_TOTAL_RATIO_CALC =
      INTENSIVE_TOTAL_RATIO_GROUP
          + "=100*($psychintensive_care_days_by_month/($psychintensive_care_days_by_month+$normal_care_days_by_month))";
  public static final String DIFF_INTENSIVE_MEAN =
      "diff_intensive_meanpsychintensive_care_days_by_month";
  public static final String PSYCH_INTENSIVE_3_MONTHS_GROUP =
      "ratio_psychintensive_total_care_days_3m";
  public static final String PSYCH_INTENSIVE_3_MONTHS_GROUP_PY =
      "ratio_psychintensive_total_care_days_3m_py";
  public static final String PSYCH_INTENSIVE_3_MONTHS_CALC =
      "$ratio_psychintensive_total_care_days_3m=100*($psychintensive_care_days_3m/$total_care_days_3m)";
  public static final String PSYCH_INTENSIVE_3_MONTHS_CALC_PY =
      "$ratio_psychintensive_total_care_days_3m_py=100*($psychintensive_care_days_3m_py/$total_care_days_3m_py)";
  public static final List<String> PSYCH_INTENSIVE_3_MONTHS_PARAM =
      List.of("psychintensive_care_days_3m", "total_care_days_3m");
  public static final List<String> PSYCH_INTENSIVE_3_MONTHS_PARAM_PY =
      List.of("psychintensive_care_days_3m_py", "total_care_days_3m_py");

  private static final TimeZone TZ_BERLIN = TimeZone.getTimeZone(DateTools.timeZoneEuropeBerlin);

  /** Simple lazy cache: computed once, reused until explicitly invalidated. */
  private Map<String, KiraMonthAgg> monthAggCache = null;

  /** Creates the kjp age timeline that separates age cluster by month. */
  public StackedBarChartsItem<KiraInteger> createKjpIntensiveCareTimeline(
      Map<String, CoreCaseData> coreCaseDataAll) {
    return createKjpIntensiveCareTimelineCore(coreCaseDataAll);
  }

  /**
   * Builds the intensive care timeline for a set of encounters and case IDs.
   *
   * @param coreCaseDataAll case data grouped by keys, must include "ALL" for full mapping
   */
  private StackedBarChartsItem<KiraInteger> createKjpIntensiveCareTimelineCore(
      Map<String, CoreCaseData> coreCaseDataAll) {

    // Ordered list of "yyyy-MM" buckets
    List<String> validPeriods =
        generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_MONTH_FORMAT, true);

    // Reuse or compute new
    Map<String, KiraMonthAgg> monthAgg = monthAgg(coreCaseDataAll, validPeriods);

    StackedBarChartsItem<KiraInteger> result = new StackedBarChartsItem<>();
    result.setCharts(List.of(KJP_PATIENT));
    result.setBars(List.of(validPeriods));
    result.setStacks(
        List.of(List.of(PSYCH_INTENSIVE_CARE_DAYS_BY_MONTH, NORMAL_CARE_DAYS_BY_MONTH)));

    List<List<KiraInteger>> valuesForChart = new ArrayList<>(validPeriods.size());
    for (String ym : validPeriods) {
      KiraMonthAgg agg = monthAgg.get(ym);
      int intensive = (agg == null) ? 0 : agg.intensiveCareDays();
      int normal = (agg == null) ? 0 : agg.normalCareDays();
      valuesForChart.add(List.of(new KiraInteger(intensive), new KiraInteger(normal)));
    }

    result.setValues(List.of(valuesForChart));
    return result;
  }

  public GroupedBarChartsCalcItem<KiraInteger> createKjpIntensiveCareRatio(
      Map<String, CoreCaseData> coreCaseDataAll) {

    List<String> validPeriods =
        generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_MONTH_FORMAT, true);

    Map<String, KiraMonthAgg> monthAgg = monthAgg(coreCaseDataAll, validPeriods);

    GroupedBarChartsCalcItem<KiraInteger> result = new GroupedBarChartsCalcItem<>();
    result.setCharts(List.of(KJP_PATIENT));
    result.setBars(validPeriods);
    result.setGroups(List.of(INTENSIVE_TOTAL_RATIO_GROUP));
    result.setCalculations(List.of(INTENSIVE_TOTAL_RATIO_CALC));
    result.setParams(
        List.of(List.of(PSYCH_INTENSIVE_CARE_DAYS_BY_MONTH, NORMAL_CARE_DAYS_BY_MONTH)));

    // wanted: values = [ [ [[intensive,total]], [[...]], ... ] ]
    List<List<List<List<KiraInteger>>>> values = new ArrayList<>(1);
    List<List<List<KiraInteger>>> chartValues = new ArrayList<>(validPeriods.size());

    for (String ym : validPeriods) {
      KiraMonthAgg agg = monthAgg.get(ym);
      int intensive = (agg == null) ? 0 : agg.intensiveCareDays();
      int normal = (agg == null) ? 0 : agg.normalCareDays();

      // [intensive, total]
      List<KiraInteger> pair = List.of(new KiraInteger(intensive), new KiraInteger(normal));
      // [[intensive, total]]
      List<List<KiraInteger>> bar = List.of(pair);
      chartValues.add(bar);
    }
    // outermost level
    values.add(chartValues);
    result.setValues(values);

    return result;
  }

  /**
   * Builds the intensive care timeline for a set of encounters and case IDs.
   *
   * @param coreCaseDataAll case data grouped by keys, must include "ALL" for full mapping
   */
  public StackedBarChartsItem<Integer> createKjpIntensiveCareTimelineCoreChange(
      Map<String, CoreCaseData> coreCaseDataAll) {

    // Ordered list of "yyyy-MM" buckets
    // We need 13 months to compare the first valid period with the one before, but in the output we
    // will just have 12 months
    List<String> periods = generateLastFullMonths(YEAR_MONTH_FORMAT, 13);

    // Reuse or compute new
    Map<String, KiraMonthAgg> monthAgg = monthAgg(coreCaseDataAll, periods);

    StackedBarChartsItem<Integer> result = new StackedBarChartsItem<>();
    result.setCharts(List.of(KJP_PATIENT));
    // Bars should only contain the 12 "real" months (skip the first dummy month)
    List<String> bars = periods.subList(1, periods.size());
    result.setBars(List.of(bars));
    result.setStacks(List.of(List.of(DIFF_INTENSIVE_MEAN)));

    // values = [ [ [-1], [5], [2], ... ] ]
    List<List<Integer>> chartValues = new ArrayList<>(bars.size());

    // iterate from index 1 onward, so we always have a "previous" month
    for (int i = 1; i < periods.size(); i++) {
      String prevYm = periods.get(i - 1);
      String currentYm = periods.get(i);

      KiraMonthAgg aggPrevMonth = monthAgg.get(prevYm);
      KiraMonthAgg aggCurrentMonth = monthAgg.get(currentYm);

      int sumIntensiveCarePrevMonth = (aggPrevMonth == null) ? 0 : aggPrevMonth.intensiveCareDays();
      int sumIntensiveCareCurrentMonth =
          (aggCurrentMonth == null) ? 0 : aggCurrentMonth.intensiveCareDays();

      int diff = sumIntensiveCareCurrentMonth - sumIntensiveCarePrevMonth;
      // one bar = [diff]
      chartValues.add(List.of(diff));
    }

    List<List<List<Integer>>> values = List.of(chartValues);
    result.setValues(values);

    return result;
  }

  public GroupedBarChartsCalcItem<KiraInteger> createKjpIntensiveCare3Months(
      Map<String, CoreCaseData> coreCaseDataAll) {

    List<String> validPeriods = generateDateList(KidsRadar.QUALIFYING_DATE, YEAR_MONTH_FORMAT);

    // Remember the current month label before removing it
    String currentMonth = null;
    if (!validPeriods.isEmpty()) {
      currentMonth = validPeriods.getLast();
      // Ignore the current month for calculations
      validPeriods.removeLast();
    }

    List<String> last3Months =
        validPeriods.subList(Math.max(0, validPeriods.size() - 3), validPeriods.size());
    // The same 3 months but in the previous year
    List<String> last3MonthsPY = shiftMonthsByYears(last3Months, -1);

    Map<String, KiraMonthAgg> monthAgg = monthAgg(coreCaseDataAll, validPeriods);

    int currIntensive = sum(monthAgg, last3Months, KiraMonthAgg::intensiveCareDays);
    int currTotal = sum(monthAgg, last3Months, KiraMonthAgg::totalCareDays);
    int pyIntensive = sum(monthAgg, last3MonthsPY, KiraMonthAgg::intensiveCareDays);
    int pyTotal = sum(monthAgg, last3MonthsPY, KiraMonthAgg::totalCareDays);

    GroupedBarChartsCalcItem<KiraInteger> result = new GroupedBarChartsCalcItem<>();
    result.setCharts(List.of(KJP_PATIENT));

    // Use the current month as bar label; fall back to the last of last3Months if needed
    result.setBars(List.of(generateBars(currentMonth)));

    result.setGroups(List.of(PSYCH_INTENSIVE_3_MONTHS_GROUP_PY, PSYCH_INTENSIVE_3_MONTHS_GROUP));
    result.setCalculations(
        List.of(PSYCH_INTENSIVE_3_MONTHS_CALC_PY, PSYCH_INTENSIVE_3_MONTHS_CALC));

    List<List<String>> params = new ArrayList<>();
    params.add(PSYCH_INTENSIVE_3_MONTHS_PARAM_PY);
    params.add(PSYCH_INTENSIVE_3_MONTHS_PARAM);
    result.setParams(params);

    // values = [ [ [[intensive,total]], [[...]], ... ] ]
    List<List<List<List<KiraInteger>>>> values =
        List.of(
            List.of(
                List.of(
                    List.of(new KiraInteger(pyIntensive), new KiraInteger(pyTotal)),
                    List.of(new KiraInteger(currIntensive), new KiraInteger(currTotal)))));
    result.setValues(values);
    return result;
  }

  private String generateBars(String barLabel) {
    if (barLabel == null || barLabel.isBlank()) {
      return "";
    }

    YearMonth current = YearMonth.parse(barLabel);

    // Calculate the end of period one month before current
    YearMonth end = current.minusMonths(1);

    // The Start is two months before the end
    YearMonth start = end.minusMonths(2);

    // Previous year's equivalent range
    YearMonth prevStart = start.minusYears(1);
    YearMonth prevEnd = end.minusYears(1);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YEAR_MONTH_FORMAT);

    return String.format(
        "%s to %s / %s to %s",
        prevStart.format(formatter),
        prevEnd.format(formatter),
        start.format(formatter),
        end.format(formatter));
  }

  // "yyyy-MM"
  private static String formatYearMonth(Date d) {
    Calendar c = Calendar.getInstance(TimeZone.getTimeZone(DateTools.timeZoneEuropeBerlin));
    c.setTime(d);
    int y = c.get(Calendar.YEAR);
    int m = c.get(Calendar.MONTH) + 1;
    return y + "-" + (m < 10 ? "0" + m : String.valueOf(m));
  }

  /** Shift list of "yyyy-MM" by N years (negative = go back). */
  private static List<String> shiftMonthsByYears(List<String> months, int years) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern(YEAR_MONTH_FORMAT);
    List<String> out = new ArrayList<>(months.size());
    for (String m : months) {
      out.add(YearMonth.parse(m, fmt).plusYears(years).format(fmt));
    }
    return out;
  }

  private static int sum(
      Map<String, KiraMonthAgg> aggByMonth,
      List<String> months,
      ToIntFunction<KiraMonthAgg> extractor) {
    int sum = 0;
    for (String m : months) {
      KiraMonthAgg a = aggByMonth.get(m);
      sum += (a == null) ? 0 : extractor.applyAsInt(a);
    }
    return sum;
  }

  /**
   * Returns true if the WHOLE calendar day 'day' (00:00..23:59:59.999 in Europe/Berlin) is fully
   * covered by ANY gap period. Partial overlap does NOT count as fully covered.
   */
  private static boolean isFullyCoveredByAnyGap(Date day, List<Period> gaps) {
    if (gaps == null || gaps.isEmpty()) return false;
    Date start = dayStart(day);
    Date end = dayEnd(day);
    for (Period p : gaps) {
      if (p == null || (!p.hasStart() && !p.hasEnd())) continue;
      Date gs = p.hasStart() ? p.getStart() : new Date(Long.MIN_VALUE);
      Date ge = p.hasEnd() ? p.getEnd() : new Date(Long.MAX_VALUE);
      // full-day coverage?
      if (!gs.after(start) && !ge.before(end)) return true;
    }
    return false;
  }

  /** Start of the local calendar day (00:00:00.000 Europe/Berlin). */
  private static Date dayStart(Date d) {
    if (d == null) return null;
    long secs = DateTools.dateToUnixTime(d);
    return DateTools.unixTimeSecondsToDateExtinguishedTime(secs, TZ_BERLIN);
  }

  /** End of the local calendar day (23:59:59.999 Europe/Berlin). */
  private static Date dayEnd(Date d) {
    Date start = dayStart(d);
    Calendar c = Calendar.getInstance(TZ_BERLIN);
    c.setTime(start);
    c.add(Calendar.DATE, 1);
    // next day 00:00 - 1 ms
    return new Date(c.getTimeInMillis() - 1);
  }

  /** ICU list -> Set<Date> normalized to dayStart (fast contains). */
  private static Set<Date> toDayStartSet(List<Date> dates) {
    if (dates == null || dates.isEmpty()) return Collections.emptySet();
    Set<Date> out = new java.util.HashSet<>();
    for (Date d : dates) if (d != null) out.add(dayStart(d));
    return out;
  }

  /** Returns cached monthAgg or computes it once. */
  private Map<String, KiraMonthAgg> monthAgg(
      Map<String, CoreCaseData> coreCaseDataAll, List<String> validPeriods) {
    if (monthAggCache != null) return monthAggCache;

    // Compute once
    Map<String, KiraMonthAgg> aggMap = new LinkedHashMap<>();
    for (String ym : validPeriods) aggMap.put(ym, new KiraMonthAgg());

    Date today = dayStart(DateTools.getCurrentDateTime());

    for (CoreCaseData ccd : coreCaseDataAll.values()) {
      if (ccd == null || ccd.getAdmissionDate() == null) continue;

      Date start = dayStart(ccd.getAdmissionDate());
      Date end = dayStart(ccd.getDischargeDate() != null ? ccd.getDischargeDate() : today);
      if (end.before(start)) continue;

      Set<Date> icuDays = toDayStartSet(ccd.getIntensiveCareDays());
      List<Period> gaps = ccd.getGapPeriods();

      Calendar cal = Calendar.getInstance(TZ_BERLIN);
      cal.setTime(start);
      while (!cal.getTime().after(end)) {
        Date day = cal.getTime();
        if (!isFullyCoveredByAnyGap(day, gaps)) {
          String ym = formatYearMonth(day);
          KiraMonthAgg agg = aggMap.get(ym);
          if (agg != null) {
            agg = agg.incTotal();
            if (icuDays.contains(day)) agg = agg.incIntensive();
            aggMap.put(ym, agg);
          }
        }
        cal.add(Calendar.DATE, 1);
      }
    }
    monthAggCache = aggMap;
    return monthAggCache;
  }

  public Map<String, List<String>> getDebugData() {
    Map<String, List<String>> output = new LinkedHashMap<>();
    return output;
  }
}
