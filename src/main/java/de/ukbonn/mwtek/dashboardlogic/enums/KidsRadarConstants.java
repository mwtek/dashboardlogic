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

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;

public class KidsRadarConstants {

  public static final String ALL_DISORDERS = "all_disorders";
  public static final String ALL_ICD_CODES = "all_icd_codes";

  public static final String RSV_ACUTE_BRONCHIOLITIS = "rsv_acute_bronchiolitis";
  public static final String RSV_ACUTE_BRONCHITIS = "rsv_acute_bronchitis";
  public static final String RSV_PNEUMONIA = "rsv_pneumonia";
  public static final String RSV_CAUSED_DISEASE = "rsv_caused_disease";

  public static final String MEAN_LENGTH_OF_STAY = "mean_length_of_stay";

  public static final String KJP_DIAGNOSES_ALL = "kjp_diagnoses_all";
  public static final String RSV_DIAGNOSES_ALL = "rsv_diagnoses_all";

  public static final String IN_GROUP = "in_group";
  public static final String OUT_GROUP = "out_group";

  public static final Integer KIRA_NUMBER_OF_CHARS_ZIP_CODES = 3;
  public static final int UPPER_AGE_BORDER = 18;
  public static final int UPPER_AGE_BORDER_PREFILTER = 20;
  public static final int THRESHOLD_DAYS_CASE_MERGE = 21;
  public static final Extension CASE_MERGED =
      new Extension("http://case.merged", new BooleanType(true));
}
