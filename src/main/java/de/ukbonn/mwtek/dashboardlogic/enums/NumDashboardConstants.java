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

package de.ukbonn.mwtek.dashboardlogic.enums;

import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.Date;

/**
 * Class with all numeric, parameterizable constants that can have an influence on the dashboard
 * logic
 *
 * <p>All date and time calculations are performed in GMT (Greenwich Mean Time) to ensure
 * consistency across time zones. Using GMT avoids complications arising from Daylight Saving Time,
 * ensuring that date comparisons and data handling remain reliable and uniform, regardless of local
 * time zone variations.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public class NumDashboardConstants {

  public static final String YEAR_FORMAT = "yyyy";
  public static final String YEAR_MONTH_FORMAT = "yyyy-MM";
  public static final String YEAR_MONTH_DATE_FORMAT = "yyyy-MM-dd";

  public static class Covid {

    /**
     * Calendar year from which medical cases are subject to the corona logic workflow. Used in
     * parallelization of the data requests.
     */
    public static final int QUALIFYING_YEAR = 2020; // 27.01.

    /** Calendar month from which medical cases are subject to the corona logic workflow. */
    public static final int QUALIFYING_MONTH = 1; // January

    /** Calendar day from which medical cases are subject to the corona logic workflow. */
    public static final int QUALIFYING_DAY = 27;

    /** Effective date from which medical cases are subject to the corona logic workflow */
    public static final Date QUALIFYING_DATE =
        DateTools.toDate(QUALIFYING_YEAR, QUALIFYING_MONTH, QUALIFYING_DAY);

    /** Unix time equivalent of the effective date */
    public static final long QUALIFYING_DATE_AS_LONG = DateTools.dateToUnixTime(QUALIFYING_DATE);
  }

  public static class Influenza {

    /**
     * Calendar year from which medical cases are subject to the influenza logic workflow. Used in
     * parallelization of the data requests.
     */
    public static final int QUALIFYING_YEAR = 2022; // 01.09.2022

    /** Calendar month from which medical cases are subject to the influenza logic workflow. */
    public static final int QUALIFYING_MONTH = 9; // September

    /** Calendar day from which medical cases are subject to the influenza logic workflow. */
    public static final int QUALIFYING_DAY = 1; // First of the month

    /** Effective date from which medical cases are subject to the influenza logic workflow */
    public static final Date QUALIFYING_DATE =
        DateTools.toDate(QUALIFYING_YEAR, QUALIFYING_MONTH, QUALIFYING_DAY);

    /** Unix time equivalent of the effective date */
    public static final long QUALIFYING_DATE_AS_LONG = DateTools.dateToUnixTime(QUALIFYING_DATE);
  }

  public static class KidsRadar {

    /**
     * Calendar year from which medical cases are subject to the kids radar logic workflow. Used in
     * parallelization of the data requests.
     */
    public static final int QUALIFYING_YEAR = 2020;

    /** Calendar month from which medical cases are subject to the kids radar logic workflow. */
    public static final int QUALIFYING_MONTH = 1; // January

    /** Calendar day from which medical cases are subject to the kids radar logic workflow. */
    public static final int QUALIFYING_DAY = 1; // First of the month

    /** Effective date from which medical cases are subject to the kids radar logic workflow */
    public static final Date QUALIFYING_DATE =
        DateTools.toDate(QUALIFYING_YEAR, QUALIFYING_MONTH, QUALIFYING_DAY);

    /** Unix time equivalent of the effective date */
    public static final long QUALIFYING_DATE_AS_LONG = DateTools.dateToUnixTime(QUALIFYING_DATE);
  }

  /** The duration of a day in seconds */
  public static final long DAY_IN_SECONDS = 86400;

  /**
   * Parameters to configure how many days after an outpatient stay an inpatient stay must occur to
   * be marked as covid positive
   */
  public static final int DAYS_AFTER_OUTPATIENT_STAY = 12;
}
