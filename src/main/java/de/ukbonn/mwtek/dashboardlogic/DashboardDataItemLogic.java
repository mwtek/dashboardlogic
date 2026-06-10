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
 */ package de.ukbonn.mwtek.dashboardlogic;

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.KJP_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDate;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeCluster;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeKjpCluster;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgePedCluster;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;

public class DashboardDataItemLogic {
  public static String DATE = "date";
  public static final String ACR_COHORT_K_1 = "acr_cohort_k1";
  public static final String ACR_COHORT_K_2 = "acr_cohort_k2";
  public static final String ACR_COHORT_K_3 = "acr_cohort_k3";
  public static final String KJP_PATIENT = "kjp_patient";

  // Accepts forms like "2020-Q1", "2020-q4"
  private static final Pattern YEAR_QUARTER_PATTERN = Pattern.compile("^(\\d{4})-[Qq]([1-4])$");

  /**
   * Filters observations to those strictly after the kickoff date for the given context.
   *
   * @param dataItemContext context which defines the kickoff date
   * @param ukbObservations input observations (may be empty)
   * @return observations with an effectiveDateTime after the kickoff date
   */
  public static List<MiiObservation> getObservationsYoungerKickoffDate(
      DataItemContext dataItemContext, Collection<MiiObservation> ukbObservations) {
    return ukbObservations.parallelStream()
        .filter(Observation::hasEffectiveDateTimeType)
        .filter(x -> x.getEffectiveDateTimeType().hasValue())
        .filter(x -> x.getEffectiveDateTimeType().getValue().after(getKickOffDate(dataItemContext)))
        .toList();
  }

  /**
   * Checks whether a date falls within the specified year and month (YYYY-MM).
   *
   * @param date date to check
   * @param yearMonthString target year-month in format {@code yyyy-MM}
   * @return true if {@code date} is in that year-month
   */
  protected static boolean isDateInYearMonth(LocalDate date, String yearMonthString) {
    String[] parts = yearMonthString.split("-");
    int year = Integer.parseInt(parts[0]);
    int month = Integer.parseInt(parts[1]);
    return date.getYear() == year && date.getMonthValue() == month;
  }

  /**
   * Checks whether a date falls within the specified calendar quarter (e.g., {@code "2020-Q4"}).
   *
   * <p>Quarter mapping: Q1 → Jan–Mar, Q2 → Apr–Jun, Q3 → Jul–Sep, Q4 → Oct–Dec.
   *
   * @param date the date to check (non-null)
   * @param yearQuarterString target year-quarter in format {@code yyyy-Qn}, e.g., {@code "2020-Q4"}
   * @return {@code true} if {@code date} is inside that year/quarter, otherwise {@code false}
   * @throws IllegalArgumentException if {@code yearQuarterString} is not in {@code yyyy-Qn} format
   */
  protected static boolean isDateInQuarter(Date date, String yearQuarterString) {
    Objects.requireNonNull(date, "date must not be null");
    YearQuarter yq = parseYearQuarter(yearQuarterString);

    Calendar cal = Calendar.getInstance(); // or Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.setTime(date);
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1; // 1..12

    if (year != yq.year) return false;

    int startMonth = (yq.quarter - 1) * 3 + 1; // 1, 4, 7, 10
    int endMonth = startMonth + 2; // 3, 6, 9, 12
    return month >= startMonth && month <= endMonth;
  }

  private record YearQuarter(int year, int quarter) {}

  /**
   * Checks whether a {@link YearMonth} falls within the specified calendar quarter.
   *
   * @param ym the YearMonth to check
   * @param yearQuarterString target year-quarter in format {@code yyyy-Qn}
   * @return {@code true} if {@code ym} is inside that quarter, otherwise {@code false}
   * @throws IllegalArgumentException if {@code yearQuarterString} is not in {@code yyyy-Qn} format
   */
  protected static boolean isYearMonthInQuarter(YearMonth ym, String yearQuarterString) {
    YearQuarter yq = parseYearQuarter(yearQuarterString);
    if (ym.getYear() != yq.year) return false;

    int month = ym.getMonthValue();
    int startMonth = (yq.quarter - 1) * 3 + 1;
    int endMonth = startMonth + 2;
    return month >= startMonth && month <= endMonth;
  }

  /** Parses strings like "2020-Q4" and returns the (year, quarter). */
  private static YearQuarter parseYearQuarter(String yearQuarterString) {
    String s = yearQuarterString == null ? "" : yearQuarterString.trim();
    Matcher m = YEAR_QUARTER_PATTERN.matcher(s);
    if (!m.matches()) {
      throw new IllegalArgumentException(
          "Expected format yyyy-Qn (e.g., 2020-Q4), but got: " + yearQuarterString);
    }
    int year = Integer.parseInt(m.group(1));
    int quarter = Integer.parseInt(m.group(2));
    return new YearQuarter(year, quarter);
  }

  /**
   * Checks whether a date falls within the specified calendar year.
   *
   * @param date date to check
   * @param yearString target year in format {@code yyyy}
   * @return true if {@code date} is in that year
   */
  protected static boolean isDateInYear(Date date, String yearString) {
    Objects.requireNonNull(date, "date must not be null");
    int year = Integer.parseInt(yearString);
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    return cal.get(Calendar.YEAR) == year;
  }

  /**
   * Converts a {@link Date} to a {@link LocalDate} using the system default timezone.
   *
   * @param date date to convert (non-null)
   * @return corresponding {@link LocalDate}
   */
  protected static LocalDate convertToLocalDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  /**
   * Resolves the aggregated chart label for KidsRadar depending on the sub-context.
   *
   * @param kidsRadarDataItemContext KJP or PED
   * @return label constant to use in charts
   */
  protected static String determineKiRaChartsAllLabelByContext(
      KidsRadarDataItemContext kidsRadarDataItemContext) {
    String chartsLabel = "";
    switch (kidsRadarDataItemContext) {
      case KJP -> chartsLabel = KJP_DIAGNOSES_ALL;
      case PED -> chartsLabel = RSV_DIAGNOSES_ALL;
    }
    return chartsLabel;
  }

  /**
   * Retrieves the valid period for an ICU stay. Prefers {@link MiiEncounter#getLocation()
   * Encounter.location.period} , but falls back to {@link MiiEncounter#getPeriod()} if {@link
   * MiiEncounter#getLocation() * Encounter.location.period} is missing.
   */
  protected static Period getValidPeriod(
      EncounterLocationComponent location, MiiEncounter encounter) {
    return location.hasPeriod() ? location.getPeriod() : encounter.getPeriod();
  }

  /**
   * Checks whether an admission date matches the requested period granularity for the given
   * KidsRadar item. Currently matches by month for both KJP and PED.
   *
   * @param kidsRadarDataItemContext KJP or PED
   * @param admissionDate date to check
   * @param period string period (e.g. {@code yyyy-MM})
   * @return true if the date matches the period per the current rules
   */
  protected boolean isDateInPeriod(
      KidsRadarDataItemContext kidsRadarDataItemContext, Date admissionDate, String period) {
    // Currently both items check the date by month; another option was/could be 'isDateInYear'
    switch (kidsRadarDataItemContext) {
      case KJP, PED -> {
        return isDateInYearMonth(convertToLocalDate(admissionDate), period);
      }
    }
    return false;
  }

  /**
   * Checks whether a given integer {@code value} lies within a half-open/closed range whose lower
   * and upper bounds may be {@code null}.
   *
   * <pre>{@code
   * inRange(5, 3, 7)    // true  (3 <= 5 <= 7)
   * inRange(3, 3, 7)    // true  (inclusive lower)
   * inRange(7, 3, 7)    // true  (inclusive upper)
   * inRange(2, 3, 7)    // false (below lower)
   * inRange(9, 3, 7)    // false (above upper)
   * inRange(5, null, 7) // true  (no lower bound)
   * inRange(5, 3, null) // true  (no upper bound)
   * }</pre>
   *
   * @param value the value to test
   * @param lower the inclusive lower bound; {@code null} means unbounded below
   * @param upper the inclusive upper bound; {@code null} means unbounded above
   */
  protected boolean inRange(int value, Integer lower, Integer upper) {
    if (lower != null && value < lower) return false;
    return upper == null || value <= upper;
  }

  protected String resolveAgeLabel(CoreCaseData caseData, KidsRadarDataItemContext context) {
    if (caseData == null || context == null) return null;

    // Select cluster set based on context
    KiraAgeCluster[] clusters =
        switch (context) {
          case KJP -> KiraAgeKjpCluster.values();
          case PED -> KiraAgePedCluster.values();
          default -> throw new IllegalStateException("Unexpected value: " + context);
        };

    int months = caseData.getAgeAtAdmissionInMonths();
    int years = caseData.getAgeAtAdmission();

    // Prefer month-based clusters when months are available
    if (months >= 0) {
      for (KiraAgeCluster c : clusters) {
        if (c.getPeriod() == KiraAgeCluster.Period.MONTHS
            && inRange(months, c.getLowerBorder(), c.getUpperBorder())) {
          return c.getLabel();
        }
      }
      // Fallback: if only months are known and > 12, derive years
      if (months > 12 && years < 0) {
        years = months / 12;
      }
    }

    // Try year-based clusters
    if (years >= 0) {
      for (KiraAgeCluster c : clusters) {
        if (c.getPeriod() == KiraAgeCluster.Period.YEARS
            && inRange(years, c.getLowerBorder(), c.getUpperBorder())) {
          return c.getLabel();
        }
      }
    }
    // No cluster matched
    return null;
  }

  // Flattens Map<group, Map<id, CoreCaseData>> into Map<id, CoreCaseData>
  // Duplicate ids across groups are ignored after the first occurrence since they contain the same
  // information anyway.
  protected static Map<String, CoreCaseData> flattenKjpCoreCases(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // Use LinkedHashMap to keep insertion order (optional)
    Map<String, CoreCaseData> result = new LinkedHashMap<>();

    if (coreCaseDataByGroups == null) {
      return result;
    }

    // Walk through all group maps and put entries if absent
    coreCaseDataByGroups
        .values()
        .forEach(
            inner -> {
              if (inner != null) {
                // Only add if id not seen yet
                inner.forEach(result::putIfAbsent);
              }
            });

    return result;
  }
}
