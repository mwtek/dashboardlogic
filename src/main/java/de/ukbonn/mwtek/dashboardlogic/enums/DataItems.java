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

/**
 * Class with constants names for the different data items
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public class DataItems {

  public static final String CURRENT_TREATMENTLEVEL = "current.treatmentlevel";
  public static final String CURRENT_MAXTREATMENTLEVEL = "current.maxtreatmentlevel";
  public static final String DEBUG = "debug";
  public static final String CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD =
      "current.age.maxtreatmentlevel.normal_ward";
  public static final String CURRENT_AGE_MAXTREATMENTLEVEL_ICU =
      "current.age.maxtreatmentlevel.icu";
  public static final String CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION =
      "current.age.maxtreatmentlevel.icu_with_ventilation";
  public static final String CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO =
      "current.age.maxtreatmentlevel.icu_with_ecmo";
  public static final String CURRENT_AGE_MAXTREATMENTLEVEL_ICU_UNDIFF =
      "current.age.maxtreatmentlevel.icu_undifferentiated";
  public static final String CUMULATIVE_VARIANTTESTRESULTS = "cumulative.varianttestresults";
  public static final String CUMULATIVE_RESULTS = "cumulative.results";
  public static final String CUMULATIVE_GENDER = "cumulative.gender";
  public static final String CUMULATIVE_AGE = "cumulative.age";
  public static final String CUMULATIVE_MAXTREATMENTLEVEL = "cumulative.maxtreatmentlevel";
  public static final String CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD =
      "cumulative.age.maxtreatmentlevel.normal_ward";
  public static final String CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT =
      "cumulative.age" + ".maxtreatmentlevel.outpatient";
  public static final String CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU =
      "cumulative.age.maxtreatmentlevel.icu";
  public static final String CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_UNDIFF =
      "cumulative.age.maxtreatmentlevel.icu_undifferentiated";
  public static final String CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION =
      "cumulative.age.maxtreatmentlevel.icu_with_ventilation";
  public static final String CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO =
      "cumulative.age.maxtreatmentlevel.icu_with_ecmo";
  public static final String CUMULATIVE_ZIPCODE = "cumulative.zipcode";
  public static final String TIMELINE_TESTS = "timeline.tests";
  public static final String TIMELINE_TEST_POSITIVE = "timeline.test.positive";
  public static final String TIMELINE_MAXTREATMENTLEVEL = "timeline.maxtreatmentlevel";
  public static final String TIMELINE_VARIANTTESTRESULTS = "timeline.varianttestresults";
  public static final String TIMELINE_DEATHS = "timeline.deaths";
  public static final String CUMULATIVE_INPATIENT_GENDER = "cumulative.inpatient.gender";
  public static final String CUMULATIVE_OUTPATIENT_GENDER = "cumulative.outpatient.gender";
  public static final String CUMULATIVE_INPATIENT_AGE = "cumulative.inpatient.age";
  public static final String CUMULATIVE_OUTPATIENT_AGE = "cumulative.outpatient.age";
  public static final String CUMULATIVE_LENGTHOFSTAY_ICU = "cumulative.lengthofstay.icu";
  public static final String CUMULATIVE_LENGTHOFSTAY_ICU_ALIVE =
      "cumulative.lengthofstay.icu" + ".alive";
  public static final String CUMULATIVE_LENGTHOFSTAY_ICU_DEAD = "cumulative.lengthofstay.icu.dead";
  public static final String CUMULATIVE_LENGTHOFSTAY_HOSPITAL = "cumulative.lengthofstay.hospital";
  public static final String CUMULATIVE_LENGTHOFSTAY_HOSPITAL_ALIVE =
      "cumulative.lengthofstay.hospital.alive";
  public static final String CUMULATIVE_LENGTHOFSTAY_HOSPITAL_DEAD =
      "cumulative.lengthofstay.hospital.dead";

  // Internal data items
  public static final String CURRENT_TREATMENTLEVEL_CROSSTAB = "current.treatmentlevel.crosstab";

  // The prefix for the influenza data items
  public static final String INFLUENZA_PREFIX = "infl.";

  // The prefix for the kids radar data items
  public static final String KIDS_RADAR_PREFIX = "kira.";

  public static final String KIDS_RADAR_PREFIX_KJP = KIDS_RADAR_PREFIX + "kjp.";

  public static final String KIDS_RADAR_PREFIX_RSV = KIDS_RADAR_PREFIX + "rsv.";

  // Kids radar related items
  public static final String CUMULATIVE_DIAGS_ZIPCODE = "cumulative.diags.zipcode";
  public static final String CUMULATIVE_DIAGS_GENDER = "cumulative.diags.gender";
  public static final String CUMULATIVE_DIAGS_AGE = "cumulative.diags.age";
  public static final String CUMULATIVE_LENGTHOFSTAY = "cumulative.diags.lengthofstay";
  public static final String TIMELINE_DIAGS_OCCURRENCE = "timeline.diags.occurrence";
}
