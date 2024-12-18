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

package de.ukbonn.mwtek.dashboardlogic.examples;

import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;

import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.codesystems.LocationPhysicalType;

public class LocationExampleData {

  public static String ICU_LOCATION_ID = "VALID-LOCATION";
  public static String NON_VALID_LOCATION_ID = "NON-VALID-LOCATION";

  public static List<UkbLocation> getExampleList() {

    List<UkbLocation> locationExamples = new ArrayList<>();

    List<Identifier> identifierIcuLocation = new ArrayList<>();
    identifierIcuLocation.add(new Identifier().setValue(ICU_LOCATION_ID));
    UkbLocation icuLocation =
        new UkbLocation(
            identifierIcuLocation,
            new CodeableConcept()
                .addCoding(
                    new Coding(
                        LocationPhysicalType.WA.getSystem(),
                        LocationPhysicalType.WA.toCode(),
                        LocationPhysicalType.WA.getDisplay())));
    icuLocation.addType(new CodeableConcept().addCoding(new Coding(null, ICU.getValue(), null)));
    icuLocation.setId(ICU_LOCATION_ID);

    // Location without .physicalType and .type
    List<Identifier> identifierNonValidLocation = new ArrayList<>();
    identifierNonValidLocation.add(new Identifier().setValue(NON_VALID_LOCATION_ID));
    UkbLocation nonValidLocation =
        new UkbLocation(
            identifierNonValidLocation,
            new CodeableConcept().addCoding(new Coding(null, null, null)));
    nonValidLocation.addType(new CodeableConcept().addCoding(new Coding(null, null, null)));
    nonValidLocation.setId(NON_VALID_LOCATION_ID);

    locationExamples.add(icuLocation);
    locationExamples.add(nonValidLocation);

    return locationExamples;
  }
}
