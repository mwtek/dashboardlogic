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
package de.ukbonn.mwtek.dashboardlogic.settings;

import de.ukbonn.mwtek.dashboardlogic.enums.QualitativeLabResultDefaultCodes;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Configuration class to overwrite the assignment to the cumulative.results from <a
 * href="https://simplifier.net/medizininformatikinitiative-modullabor/valuesetqualitativelaborergebnisse">...</a>
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Getter
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class QualitativeLabCodesSettings {

  // Initialization with default values
  private List<String> positiveCodes;
  private List<String> borderlineCodes;
  private List<String> negativeCodes;

  public QualitativeLabCodesSettings(
      List<String> positiveCodes, List<String> borderlineCodes, List<String> negativeCodes) {
    this.positiveCodes = positiveCodes;
    this.borderlineCodes = borderlineCodes;
    this.negativeCodes = negativeCodes;
  }

  public List<String> getPositiveCodes() {
    if (positiveCodes == null) return QualitativeLabResultDefaultCodes.getPositiveCodes();
    return positiveCodes;
  }

  public List<String> getBorderlineCodes() {
    if (borderlineCodes == null) return QualitativeLabResultDefaultCodes.getBorderlineCodes();
    return borderlineCodes;
  }

  public List<String> getNegativeCodes() {
    if (negativeCodes == null) return QualitativeLabResultDefaultCodes.getNegativeCodes();
    return negativeCodes;
  }
}
