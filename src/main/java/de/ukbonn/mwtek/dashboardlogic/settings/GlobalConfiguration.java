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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalConfiguration {

  /** Comma-separated list with input codes for FHIR Search and processing logic. */
  private Map<String, Object> inputCodes = new HashMap<>();

  /** Flag to add debug information (e.g. case ids / resource ids) to the output. */
  private Boolean debug = false;

  private Map<String, Boolean> predictionModels = new HashMap<>();

  private Boolean usePartOfInsteadOfIdentifier = false;

  /** Use Patient.deceasedDateTime to classify encounters as deceased [default = false] */
  private Boolean usePatientDeceased = false;

  /**
   * Replacing all data (sub)items that separate between icu + icu_ventilation + icu_ecmo with
   * icu_undifferentiated [default: false]. This should get activated if a site cant provide
   * ventilation and ecmo procedures.
   */
  private Boolean useIcuUndifferentiated = false;

  /**
   * Should procedures that don't have a linked encounter with at least one icu stay get filtered?
   * [default: true]
   */
  private Boolean checkProceduresIcuStays = true;

  /** Should the covid-19 data item generation take place? */
  private Boolean generateCovidData = true;

  /** Should the influenza data item generation take place? */
  private Boolean generateInfluenzaData = false;

  /** Should the kids radar data item generation take place? */
  private Boolean generateKidsRadarData = false;

  /**
   * Should the generation ukb-renal-replacement prediction model data item generation take place?
   * Currently not supported via FHIR server usage.
   */
  private Boolean generateUkbRenalReplacementModelData = false;

  /** Should the acribis data item generation take place? */
  private Boolean generateAcribisData = false;

  /**
   * Instead of `Encounter.location` references, Encounters can be marked as icu-encounter via
   * service provider IDs
   */
  private Set<String> serviceProviderIdentifierOfIcuLocations = new HashSet<>();

  /**
   * Option to overwrite the assignment from the value set <a
   * href="https://simplifier.net/medizininformatikinitiative-modullabor/valuesetqualitativelaborergebnisse">here</a>.
   */
  private Map<String, List<String>> qualitativeLabCodes = new HashMap<>();
}
