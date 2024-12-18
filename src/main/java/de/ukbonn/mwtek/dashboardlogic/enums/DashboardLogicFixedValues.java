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

import com.google.common.collect.ImmutableList;
import de.ukbonn.mwtek.utilities.fhir.misc.StaticValueProvider;
import lombok.Getter;

/**
 * Class with all textual fixed values necessary for the creation of the Json class and data
 * processing (e.g. DataItems designations, formatting, flags).
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Getter
public enum DashboardLogicFixedValues {
  // COVID-States
  POSITIVE_RESULT(StaticValueProvider.SYSTEM + "/resultPositive"),
  NEGATIVE_RESULT(StaticValueProvider.SYSTEM + "/resultNegative"),
  BORDERLINE_RESULT(StaticValueProvider.SYSTEM + "/resultBorderLine"),
  TWELVE_DAYS_LOGIC(StaticValueProvider.SYSTEM + "/12DaysTimespan"),

  // DateFormat
  DATE("date"),

  // Entry in specific data items
  POSITIVE("Positive"),
  NEGATIVE("Negative"),
  BORDERLINE("Borderline_suspected"),
  MALE_SPECIFICATION("Male"),
  FEMALE_SPECIFICATION("Female"),
  DIVERSE_SPECIFICATION("Diverse"),

  // Administrative data patient
  /*
  ISO 3166 country code
  */
  COUNTRY_CODE("DE"),
  CITY_BONN("Bonn"),

  // DiagnosticCodes
  ICD("http://fhir.de/StructureDefinition/icd-10-gm-ausrufezeichen"),
  ICD_SYSTEM("http://fhir.de/CodeSystem/bfarm/icd-10-gm"),
  ICD_DIAG_RELIABILITY_EXT_URL("http://fhir.de/StructureDefinition/icd-10-gm-diagnosesicherheit"),
  ICD_DIAG_RELIABILITY_CODING_SYSTEM(
      "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ICD_DIAGNOSESICHERHEIT"),

  // ICD code without exclamation mark, as it is used in an "observation contains" logic to make
  // further processing more robust.
  U071("U07.1"),

  // Diagnosis Reliability
  DIAG_RELIABILITY_MISSING("No Code"),
  DIAG_RELIABILITY_A("A"),
  DIAG_RELIABILITY_G("G"),
  DIAG_RELIABILITY_V("V"),

  // Covid variant related codes
  VARIANT_ALPHA_CODE("LA31569-9"),
  VARIANT_BETA_CODE("LA31570-7"),
  VARIANT_DELTA_CODE("LA32552-4"),
  VARIANT_GAMMA_CODE("LA31621-8"),
  VARIANT_OMICRON_CODE("LA33381-7"),

  DIAG_RELIABILITY_Z("Z");

  public static final String OBSERVATION_INTERPRETATION_SYSTEM =
      "http://terminology.hl7" + ".org/CodeSystem/v3-ObservationInterpretation";

  // Display forms of the covid-19 variants to check against
  public static final String VARIANT_ALPHA = "Alpha";
  public static final String VARIANT_BETA = "Beta";
  public static final String VARIANT_GAMMA = "Gamma";
  public static final String VARIANT_DELTA = "Delta";
  public static final String VARIANT_OMICRON = "Omicron";
  public static final String VARIANT_OTHER_VOC = "OtherVOC";
  public static final String VARIANT_NON_VOC = "NonVOC";
  public static final String VARIANT_UNKNOWN = "Unknown";

  // Codes of the covid-19 variants to check against
  // For now the display values are checked since its more flexible if new variants appear or to
  // generalize non-voc variants
  public static final String VARIANT_ALPHA_LOINC = "LA31569-9";
  public static final String VARIANT_BETA_LOINC = "LA31570-7";
  public static final String VARIANT_GAMMA_LOINC = "LA31621-8";
  public static final String VARIANT_DELTA_LOINC = "LA32552-4";
  public static final String VARIANT_OMICRON_LOINC = "LA33381-7";

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for borderline COVID-19 findings
   */
  public static final ImmutableList<String> DIAGNOSIS_SECURITY_BORDERLINE =
      ImmutableList.of(
          DIAG_RELIABILITY_V.getValue(),
          DIAG_RELIABILITY_G.getValue(),
          DIAG_RELIABILITY_Z.getValue());

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for borderline COVID-19 findings
   */
  public static final ImmutableList<DashboardLogicFixedValues> DIAGNOSIS_SECURITY_BORDERLINE_ENUM =
      ImmutableList.of(DIAG_RELIABILITY_V, DIAG_RELIABILITY_G, DIAG_RELIABILITY_Z);

  /** ValueSet of diagnostic certainties for outpatient diagnoses for negative COVID-19 findings */
  public static final ImmutableList<String> DIAGNOSIS_SECURITY_NEGATIVE =
      ImmutableList.of(DIAG_RELIABILITY_A.getValue());

  /** ValueSet of diagnostic certainties for outpatient diagnoses for negative COVID-19 findings */
  public static final ImmutableList<DashboardLogicFixedValues> DIAGNOSIS_SECURITY_NEGATIVE_ENUM =
      ImmutableList.of(DIAG_RELIABILITY_A);

  private final String value;

  DashboardLogicFixedValues(String value) {
    this.value = value;
  }
}
