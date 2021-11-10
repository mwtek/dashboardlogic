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

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import com.google.common.collect.ImmutableList;
import de.ukbonn.mwtek.utilities.fhir.misc.StaticValueProvider;

/**
 * Class with all textual fixed values necessary for the creation of the Json class and data
 * processing (e.g. DataItems designations, formatting, flags).
 * 
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
public enum CoronaFixedValues {
  // COVID-States
  POSITIVE_RESULT (StaticValueProvider.system + "/covidResultPositive"),
  NEGATIVE_RESULT (StaticValueProvider.system + "/covidResultNegative"),
  BORDERLINE_RESULT (StaticValueProvider.system + "/covidResultBorderLine"),
  TWELVE_DAYS_LOGIC ("12 Days Timespan"),

  // DateFormat
  DATEFORMAT ("yyyy-MM-dd"),

  // ICU-CategoryCodes
  ECMO_CODE ("182744004"),
  VENT_CODE ("40617009"),
  VENT_CODE2 ("57485005"),

  // Positive/Negative state
  POSITIVE_CODE ("10828004"),
  NEGATIVE_CODE ("260385009"),
  BORDERLINE_CODE ("419984006"),

  // Entry in specific data items
  POSITIVE ("positiv"),
  NEGATIVE ("negativ"),
  BORDERLINE ("grenzwertig_Verdacht"),
  MALE_SPECIFICATION ("maennlich"),
  FEMALE_SPECIFICATION ("weiblich"),
  DIVERSE_SPECIFICATION ("divers"),

  ICU ("ICU"),
  ICU_VENTILATION ("ICU_mit_Beatmung"),
  ICU_ECMO ("ICU_mit_ECMO"),
  NORMALSTATION ("Normalstation"),
  STATIONARY_ITEM ("stationär"),
  AFTERSTATIONARY_ITEM ("nachstationär"),
  PARTSTATIONARY_ITEM ("teilstationär"),
  PRESTATIONARY_ITEM ("vorstationär"),
  AMBULANT_ITEM ("ambulant"),

  // Gender [FHIR Value Set]
  MALE ("male"),
  FEMALE ("female"),
  DIVERSE ("diverse"),
  UNKNOWN ("unknown"), // just for debugging issues

  // Administrative data patient
  COUNTRY_CODE ("D"),
  CITY_BONN ("Bonn"),

  // dischargeCoding [FHIR Value Set]
  DEATH_CODE ("07"),

  // VitalStatus
  ALIVE ("alive"),
  DEAD ("dead"),

  // Encounter.case.class (new fhir profile ->
  // https://simplifier.net/packages/de.basisprofil.r4/1.0.0/files/397957)
  CASECLASS_INPATIENT ("IMP"),
  CASECLASS_OUTPATIENT ("AMB"),

  // Encounter.case.type.kontaktart (new fhir profile ->
  // https://simplifier.net/packages/de.basisprofil.r4/1.0.0/files/397801)
  CASETYPE_PRESTATIONARY ("vorstationaer"),
  CASETYPE_POSTSTATIONARY ("nachstationaer"),
  CASETYPE_PARTSTATIONARY ("teilstationaer"),
  CASETYPE_NORMALSTATIONARY ("normalstationaer"),
  CASETYPE_INTENSIVESTATIONARY ("intensivstationaer"),
  CASETYPE_KONTAKTART_SYSTEM ("http://fhir.de/CodeSystem/kontaktart-de"),

  // CaseStatus
  ALL ("all"),
  INPATIENT ("inpatient"),
  OUTPATIENT ("outpatient"),

  // PhysicalType(s)
  WARD ("wa"),

  // DiagnosticCodes
  PRIMAERCODE ("http://fhir.de/StructureDefinition/icd-10-gm-primaercode"),
  ICD ("http://fhir.de/StructureDefinition/icd-10-gm-ausrufezeichen"),
  ICD_SYSTEM ("http://fhir.de/CodeSystem/dimdi/icd-10-gm"),
  ICD_SECURITY ("http://fhir.de/StructureDefinition/icd-10-gm-diagnosesicherheit"),
  ICD_SECURITY_SYSTEM ("https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ICD_DIAGNOSESICHERHEIT"),
  DIAGNOSECODE_EMPTY ("No Code"),
  U071 ("U07.1"),
  U071A ("U07.1A"),
  U071G ("U07.1G"),
  U071Z ("U07.1Z"),
  U071V ("U07.1V"),
  U072 ("U07.2"),
  U072A ("U07.2A"),
  U072G ("U07.2G"),
  U072V ("U07.2V"),
  U072Z ("U07.2Z"),;

  /**
   * ValueSet with all inpatient codes ({@link Coding#getCode() Encounter.class.code}) that fit to
   * the data specification
   */
  public static final ImmutableList<String> STATIONARY_CODES =
      ImmutableList.of("stationär", "nachstationär", "teilstationär");

  /**
   * ValueSet with all inpatient codes ({@link Encounter#getType() Encounter.type.kontaktart}) that
   * fit to the data specification
   */
  public static final ImmutableList<String> STATIONARY_CODETYPES =
      ImmutableList.of("normalstationaer", "nachstationaer", "teilstationaer");

  /**
   * ValueSet with all outpatient codes ({@link Coding#getCode() Encounter.class.code}) that fit to
   * the data specification
   */
  public static final ImmutableList<String> AMBU_CODES =
      ImmutableList.of("ambulant", "vorstationär");

  /**
   * ValueSet with all codes in ({@link Encounter#getType() Encounter.type.kontaktart}) that should
   * count as outpatient in the data specification
   */
  public static final ImmutableList<String> AMBU_ADDITIONALCODETYPES =
      ImmutableList.of("vorstationaer");

  /**
   * LOINC codes for SARS-CoV-2 (COVID-19) PCR laboratory results
   */
  public static final ImmutableList<String> COVID_LOINC_CODES =
      ImmutableList.of("94306-8", "96763-8", "94640-0");

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for borderline COVID-19 findings
   */
  public static final ImmutableList<String> DIAGNOSIS_SECURITY_BORDERLINE =
      ImmutableList.of("V", "G", "Z");

  /**
   * ValueSet of diagnostic certainties for outpatient diagnoses for negative COVID-19 findings
   */
  public static final ImmutableList<String> DIAGNOSIS_SECURITY_NEGATIVE = ImmutableList.of("A");

  private final String value;

  CoronaFixedValues(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}