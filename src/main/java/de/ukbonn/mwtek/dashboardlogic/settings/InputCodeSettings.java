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

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Configuration class to dynamically respond to emerging covid variants.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InputCodeSettings {

  // Initialization with default values

  @Getter
  private List<String> observationPcrLoincCodes = ImmutableList.of("94306-8", "96763-8",
      "94640-0");

  @Getter
  private List<String> observationVariantLoincCodes = ImmutableList.of("96741-4", "96895-8");


  @Getter
  private List<String> conditionIcdCodes = ImmutableList.of("U07.1", "U07.2");


  @Getter
  private List<String> procedureVentilationCodes = ImmutableList.of("40617009",
      "57485005");

  @Getter
  private List<String> procedureEcmoCodes = ImmutableList.of("182744004");

  public InputCodeSettings(List<String> observationPcrLoincCodes,
      List<String> observationVariantLoincCodes,
      List<String> conditionIcdCodes,
      List<String> procedureVentilationCodes, List<String> procedureEcmoCodes) {
    this.observationPcrLoincCodes = observationPcrLoincCodes;
    this.observationVariantLoincCodes = observationVariantLoincCodes;
    this.conditionIcdCodes = conditionIcdCodes;
    this.procedureVentilationCodes = procedureVentilationCodes;
    this.procedureEcmoCodes = procedureEcmoCodes;
  }
}
