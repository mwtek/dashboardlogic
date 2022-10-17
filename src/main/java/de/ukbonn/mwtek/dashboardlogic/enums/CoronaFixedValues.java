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
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;

/**
 * Class with all textual fixed values necessary for the creation of the Json class and data
 * processing (e.g. DataItems designations, formatting, flags).
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
public enum CoronaFixedValues {
  // COVID-States
  POSITIVE_RESULT(StaticValueProvider.system + "/covidResultPositive"),
  NEGATIVE_RESULT(StaticValueProvider.system + "/covidResultNegative"),
  BORDERLINE_RESULT(StaticValueProvider.system + "/covidResultBorderLine"),
  TWELVE_DAYS_LOGIC("12 Days Timespan"),

  // DateFormat
  DATE("date"),

  // Entry in specific data items
  POSITIVE("Positive"),
  NEGATIVE("Negative"),
  BORDERLINE("Borderline_suspected"),
  MALE_SPECIFICATION("Male"),
  FEMALE_SPECIFICATION("Female"),
  DIVERSE_SPECIFICATION("Diverse"),

  ICU("ICU"),
  ICU_VENTILATION("ICU_with_ventilation"),
  ICU_ECMO("ICU_with_ecmo"),
  NORMAL_WARD("Normal_ward"),

  // Internal usages for list management
  INPATIENT_ITEM("Stationary"),
  OUTPATIENT_ITEM("Outpatient"),

  // Gender [FHIR Value Set]
  GENDER_MALE("male"),
  GENDER_FEMALE("female"),
  GENDER_DIVERSE("diverse"),
  GENDER_UNKNOWN("unknown"),

  // Administrative data patient
  /*
   ISO 3166 country code
   */
  COUNTRY_CODE("DE"),
  CITY_BONN("Bonn"),

  // dischargeCoding [FHIR Value Set]
  DISCHARGE_DISPOSITION_EXT_URL("http://fhir.de/StructureDefinition/Entlassungsgrund"),
  DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_EXT_URL("ErsteUndZweiteStelle"),
  DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_SYSTEM(
      "http://fhir.de/CodeSystem/dkgev/EntlassungsgrundErsteUndZweiteStelle"),
  DEATH_CODE("07"),

  // VitalStatus
  ALIVE("alive"),
  DEAD("dead"),

  // Encounter.case.type.kontaktart (new fhir profile ->
  // https://simplifier.net/packages/de.basisprofil.r4/1.0.0/files/397801)
  CASETYPE_PRESTATIONARY("vorstationaer"),
  CASETYPE_POSTSTATIONARY("nachstationaer"),
  CASETYPE_PARTSTATIONARY("teilstationaer"),
  CASETYPE_NORMALSTATIONARY("normalstationaer"),
  CASETYPE_INTENSIVESTATIONARY("intensivstationaer"),
  CASETYPE_KONTAKTART_SYSTEM("http://fhir.de/CodeSystem/kontaktart-de"),

  // CaseStatus
  CASESTATUS_ALL("all"),
  CASESTATUS_INPATIENT("inpatient"),
  CASESTATUS_OUTPATIENT("outpatient"),

  // PhysicalType(s)
  WARD("wa"),

  // DiagnosticCodes
  ICD("http://fhir.de/StructureDefinition/icd-10-gm-ausrufezeichen"),
  ICD_SYSTEM("http://fhir.de/CodeSystem/bfarm/icd-10-gm"),
  ICD_DIAG_RELIABILITY_EXT_URL("http://fhir.de/StructureDefinition/icd-10-gm-diagnosesicherheit"),
  ICD_DIAG_RELIABILITY_CODING_SYSTEM(
      "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ICD_DIAGNOSESICHERHEIT"),

  // ICD code without exclamation mark, as it is used in an "observation contains" logic to make further processing more robust.
  U071("U07.1"),
  U072("U07.2"),

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

  // Terminology systems
  public static final String LOINC_SYSTEM = "http://loinc.org";
  public static final String SNOMED_SYSTEM = "http://snomed.info/sct";


  // Display forms of the covid variants to check against
  public static final String VARIANT_ALPHA = "Alpha";
  public static final String VARIANT_BETA = "Beta";
  public static final String VARIANT_GAMMA = "Gamma";
  public static final String VARIANT_DELTA = "Delta";
  public static final String VARIANT_OMICRON = "Omicron";
  public static final String VARIANT_OTHER_VOC = "OtherVOC";
  public static final String VARIANT_NON_VOC = "NonVOC";
  public static final String VARIANT_UNKNOWN = "Unknown";

  // Codes of the covid variants to check against
  // For now the display values are checked since its more flexible if new variants appear or to generalize non-voc variants
  public static final String VARIANT_ALPHA_LOINC = "LA31569-9";
  public static final String VARIANT_BETA_LOINC = "LA31570-7";
  public static final String VARIANT_GAMMA_LOINC = "LA31621-8";
  public static final String VARIANT_DELTA_LOINC = "LA32552-4";
  public static final String VARIANT_OMICRON_LOINC = "LA33381-7";

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for borderline COVID-19 findings
   */
  public static final ImmutableList<String> DIAGNOSIS_SECURITY_BORDERLINE =
      ImmutableList.of(DIAG_RELIABILITY_V.getValue(), DIAG_RELIABILITY_G.getValue(),
          DIAG_RELIABILITY_Z.getValue());

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for borderline COVID-19 findings
   */
  public static final ImmutableList<CoronaFixedValues> DIAGNOSIS_SECURITY_BORDERLINE_ENUM =
      ImmutableList.of(DIAG_RELIABILITY_V, DIAG_RELIABILITY_G, DIAG_RELIABILITY_Z);

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for negative COVID-19 findings
   */
  public static final ImmutableList<String> DIAGNOSIS_SECURITY_NEGATIVE =
      ImmutableList.of(DIAG_RELIABILITY_A.getValue());

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for negative COVID-19 findings
   */
  public static final ImmutableList<CoronaFixedValues> DIAGNOSIS_SECURITY_NEGATIVE_ENUM =
      ImmutableList.of(DIAG_RELIABILITY_A);

  /**
   * List of valid {@link UkbEncounter#getClass_() encounter.class.code} codes for inpatient
   * encounters. Both the value set from the case module version 1.0 and 2.0 are checked. <a
   * href="https://simplifier.net/packages/de.basisprofil.r4/1.0.0/files/397957">The value set of
   * version 2.0 can be found here.</a>.
   */
  public static final ImmutableList<String> ENCOUNTER_CLASS_INPATIENT_CODES =
      ImmutableList.of("IMP", "stationaer");

  /**
   * List of valid {@link UkbEncounter#getClass_() encounter.class.code} codes for outpatient
   * encounters. Both the value set from the case module version 1.0 and 2.0 are checked. <a
   * href="https://simplifier.net/packages/de.basisprofil.r4/1.0.0/files/397957">The value set of
   * version 2.0 can be found here.</a>.
   */
  public static final ImmutableList<String> ENCOUNTER_CLASS_OUTPATIENT_CODES =
      ImmutableList.of("AMB", "ambulant");

  private final String value;

  CoronaFixedValues(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
