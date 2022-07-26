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
 * Java representation of the <a href="https://simplifier.net/medizininformatikinitiative-modullabor/valuesetqualitativelaborergebnisse">
 * MII value set for qualitative lab results</a>.
 */
public enum QualitativeLabResultCodes {

  /**
   * 10828004	Positive (qualifier value)
   */
  POSITIVE_CODE("10828004"),
  /**
   * 280416009	Indeterminate result (qualifier value)
   */
  INDETERMINATE_RESULT_CODE("280416009"),
  /**
   * 260385009	Negative (qualifier value)
   */
  NEGATIVE_CODE("260385009"),
  /**
   * 260415000	Not detected (qualifier value)
   */
  NOT_DETECTED_CODE("260415000"),
  /**
   * 419984006	Inconclusive (qualifier value)
   */
  INCONCLUSIVE_CODE("419984006"),
  /**
   * 260373001	Detected (qualifier value)
   */
  DETECTED_CODE("260373001"),
  /**
   * 52101004	Present (qualifier value)
   */
  PRESENT_CODE("52101004"),
  /**
   * 410594000	Definitely NOT present (qualifier value)
   */
  DEFINITELY_NOT_PRESENT_CODE("410594000");

  final String snomedCtCode;

  QualitativeLabResultCodes(String snomedCtCode) {
    this.snomedCtCode = snomedCtCode;
  }

  public String getValue() {
    return snomedCtCode;
  }

  /**
   * Group with all codes that declare a laboratory finding as "positive".
   */
  public static List<String> getPositiveCodes() {
    return Arrays.asList(POSITIVE_CODE.getValue(), DETECTED_CODE.getValue(),
        PRESENT_CODE.getValue());
  }

  /**
   * Group with all codes that declare a laboratory finding as "negative".
   */
  public static List<String> getNegativeCodes() {
    return Arrays.asList(NEGATIVE_CODE.getValue(), NOT_DETECTED_CODE.getValue(),
        DEFINITELY_NOT_PRESENT_CODE.getValue());
  }

  /**
   * Group with all codes that declare a laboratory finding as "borderline/indeterminate".
   */
  public static List<String> getBorderlineCodes() {
    return Arrays.asList(INDETERMINATE_RESULT_CODE.getValue(), INCONCLUSIVE_CODE.getValue());
  }
}
