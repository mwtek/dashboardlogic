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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.BORDERLINE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.NEGATIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.TWELVE_DAYS_LOGIC;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;

public class FlaggingExtension {

  /**
   * Creation of an {@linkplain Extension} which is used as a flag for SARS-CoV-2/influenza positive
   * cases
   */
  public static final Extension POSITIVE_EXTENSION = new Extension();

  static {
    POSITIVE_EXTENSION.setUrl(POSITIVE_RESULT.getValue());
    POSITIVE_EXTENSION.setValue(new BooleanType(true));
  }

  public static final Extension NEGATIVE_EXTENSION = new Extension();

  static {
    NEGATIVE_EXTENSION.setUrl(NEGATIVE_RESULT.getValue());
    NEGATIVE_EXTENSION.setValue(new BooleanType(true));
  }

  public static final Extension BORDERLINE_EXTENSION = new Extension();

  static {
    BORDERLINE_EXTENSION.setUrl(BORDERLINE_RESULT.getValue());
    BORDERLINE_EXTENSION.setValue(new BooleanType(true));
  }

  public static final Extension TWELVE_DAYS_EXTENSION = new Extension();

  static {
    TWELVE_DAYS_EXTENSION.setUrl(TWELVE_DAYS_LOGIC.getValue());
    TWELVE_DAYS_EXTENSION.setValue(new BooleanType(true));
  }
}
