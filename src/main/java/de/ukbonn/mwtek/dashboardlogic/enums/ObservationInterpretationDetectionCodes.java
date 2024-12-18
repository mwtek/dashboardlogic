/*
 *
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 *
 */
package de.ukbonn.mwtek.dashboardlogic.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Java representation of the <a
 * href="https://simplifier.net/packages/hl7.fhir.r4.core/4.0.1/files/78998">MII value set for
 * qualitative lab results</a>.
 */
public enum ObservationInterpretationDetectionCodes {

  /**
   * A presence finding of the specified component / analyte, organism or clinical sign based on the
   * established threshold of the performed test or procedure.
   */
  POSITIVE_CODE("POS"),
  /**
   * The specified component / analyte, organism or clinical sign could neither be declared positive
   * / negative nor detected / not detected by the performed test or procedure. Usage Note: For
   * example, if the specimen was degraded, poorly processed, or was missing the required anatomic
   * structures, then "indeterminate" (i.e. "cannot be determined") is the appropriate response, not
   * "equivocal".
   */
  INDETERMINATE_RESULT_CODE("IND"),
  /**
   * An absence finding of the specified component / analyte, organism or clinical sign based on the
   * established threshold of the performed test or procedure. [Note: Negative does not necessarily
   * imply the complete absence of the specified item.]
   */
  NEGATIVE_CODE("NEG"),
  /**
   * The presence of the specified component / analyte, organism or clinical sign could not be
   * determined within the limit of detection of the performed test or procedure.
   */
  NOT_DETECTED_CODE("ND"),
  /**
   * The test or procedure was successfully performed, but the results are borderline and can
   * neither be declared positive / negative nor detected / not detected according to the current
   * established criteria.
   */
  EQUIVOCAL_CODE("E"),
  /**
   * The measurement of the specified component / analyte, organism or clinical sign above the limit
   * of detection of the performed test or procedure.
   */
  DETECTED_CODE("DET");

  final String obsInterpretationCode;

  ObservationInterpretationDetectionCodes(String obsInterpretationCode) {
    this.obsInterpretationCode = obsInterpretationCode;
  }

  public String getValue() {
    return obsInterpretationCode;
  }

  /** Group with all codes that declare a laboratory finding as "positive". */
  public static List<String> getPositiveCodes() {
    return Arrays.asList(POSITIVE_CODE.getValue(), DETECTED_CODE.getValue());
  }

  /** Group with all codes that declare a laboratory finding as "negative". */
  public static List<String> getNegativeCodes() {
    return Arrays.asList(NEGATIVE_CODE.getValue(), NOT_DETECTED_CODE.getValue());
  }

  /** Group with all codes that declare a laboratory finding as "borderline/indeterminate". */
  public static List<String> getBorderlineCodes() {
    return Arrays.asList(INDETERMINATE_RESULT_CODE.getValue(), EQUIVOCAL_CODE.getValue());
  }
}
