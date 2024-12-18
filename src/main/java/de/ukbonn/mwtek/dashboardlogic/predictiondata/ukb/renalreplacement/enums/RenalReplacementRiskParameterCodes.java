/*
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums;

import java.util.Arrays;
import java.util.List;

public class RenalReplacementRiskParameterCodes {

  public static final String LOINC_CODE_CREATININE_SERUM_OR_PLASMA = "2160-0";
  public static final String LOINC_CODE_LACTATE_IN_ARTERIAL_BLOOD = "2518-9";
  public static final String LOINC_CODE_LACTATE_IN_VENOUS_BLOOD = "2519-7";
  public static final String LOINC_CODE_LACTATE_IN_MIXED_VENOUS_BLOOD = "19240-1";
  public static final String LOINC_CODE_LACTATE_IN_CAPILLARY_BLOOD = "19239-3";
  public static final String LOINC_CODE_LACTATE_IN_BLOOD = "32693-4";
  public static final String LOINC_CODE_UREA_IN_SERUM_OR_PLASMA = "3091-6";
  public static final String LOINC_CODE_URINE_OUTPUT = "9187-6";
  public static final String LOINC_CODE_BODY_WEIGHT = "29463-7";

  /*
  "Continuous renal replacement therapy (procedure)"
   */
  public static final String SNOMED_CODE_CONTINUOUS_THERAPY = "714749008";

  /*
  "Intermittent hemodialysis (procedure)"
   */
  public static final String SNOMED_CODE_INTERMITTENT_HEMODIALYSIS = "233575001";

  /*
  The loinc codes that are forced in the model description for the non pdms items.
   */
  public static final List<String> VALID_LOINC_CODES_HIS =
      Arrays.asList(
          LOINC_CODE_CREATININE_SERUM_OR_PLASMA,
          LOINC_CODE_LACTATE_IN_ARTERIAL_BLOOD,
          LOINC_CODE_LACTATE_IN_VENOUS_BLOOD,
          LOINC_CODE_LACTATE_IN_MIXED_VENOUS_BLOOD,
          LOINC_CODE_LACTATE_IN_CAPILLARY_BLOOD,
          LOINC_CODE_LACTATE_IN_BLOOD,
          LOINC_CODE_UREA_IN_SERUM_OR_PLASMA);

  /*
  The snomed codes that are forced in the model description for the pdms items.
   */
  public static final List<String> VALID_LOINC_CODES_PDMS =
      Arrays.asList(LOINC_CODE_URINE_OUTPUT, LOINC_CODE_BODY_WEIGHT);

  /*
  The snomed codes that are forced in the model description for the pdms items.
   */
  public static final List<String> VALID_SNOMED_CODES_PDMS =
      Arrays.asList(SNOMED_CODE_CONTINUOUS_THERAPY, SNOMED_CODE_INTERMITTENT_HEMODIALYSIS);
}
