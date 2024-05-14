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

package de.ukbonn.mwtek.dashboardlogic.logic.cumulative;

import static de.ukbonn.mwtek.dashboardlogic.examples.EncounterExampleData.ENCOUNTER_ID_INPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.examples.EncounterExampleData.ENCOUNTER_ID_MISSING_IDENTIFIER;
import static de.ukbonn.mwtek.dashboardlogic.examples.EncounterExampleData.getExampleList;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.examples.InputCodeSettingsExampleData;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

public class CumulativeZipCodeTests {

  @Test
  @DisplayName("Ensuring that the cumulative.zipcode data item is able to handle empty fields in "
      + "the patient resource even with missing fields.")
  void testCumulativeResults() {
    // Initialization of the input list
    List<UkbPatient> patients = new ArrayList<>();
    List<Identifier> identifiers = new ArrayList<>();
    List<HumanName> humanNames = new ArrayList<>();
    List<Address> addresses = new ArrayList<>();

    // Add an observation without a value, since this field is not mandatory
    identifiers.add(
        new Identifier().setValue(ENCOUNTER_ID_INPATIENT).setUse(IdentifierUse.OFFICIAL));
    humanNames.add(new HumanName().setFamily("Testpatient"));
    addresses.add(new Address().setPostalCode("12345").setCountry("DE"));
    UkbPatient patientWithMissingCountryCode = new UkbPatient(identifiers, humanNames,
        AdministrativeGender.MALE, addresses);
    patientWithMissingCountryCode.setId(ENCOUNTER_ID_INPATIENT);

    patients.add(patientWithMissingCountryCode);

    List<Identifier> identifiersPatientTwo = new ArrayList<>();
    List<HumanName> humanNamesPatientTwo = new ArrayList<>();
    List<Address> addressesPatientTwo = new ArrayList<>();

    // Creation of a patient who has no zip code.
    identifiersPatientTwo.add(
        new Identifier().setValue(ENCOUNTER_ID_MISSING_IDENTIFIER).setUse(IdentifierUse.OFFICIAL));
    humanNamesPatientTwo.add(new HumanName().setFamily("Testpatient"));
    addressesPatientTwo.add(new Address().setCountry("DE"));
    UkbPatient patientWithMissingZipCode = new UkbPatient(identifiersPatientTwo,
        humanNamesPatientTwo,
        AdministrativeGender.MALE, addressesPatientTwo);
    patientWithMissingZipCode.setId(ENCOUNTER_ID_MISSING_IDENTIFIER);

    patients.add(patientWithMissingZipCode);

    DashboardDataItemLogics.initializeData(InputCodeSettingsExampleData.getExampleData(),
        getExampleList(), patients, null, null, null, DataItemContext.COVID);

    List<String> result = new CumulativeZipCode().createZipCodeList();
    // Even the ones with missing value should return a "null" string.
    Assert.isTrue(result.size() == patients.size(), "");
  }
}
