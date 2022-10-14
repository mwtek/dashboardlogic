/*
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboardlogic.examples;

import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaLogic.markPositive;

import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

public class EncounterExampleData {

  public static String ENCOUNTER_ID_INPATIENT = "1234";
  public static String ENCOUNTER_ID_MISSING_ATTRIBUTES = "1235";

  public static List<UkbEncounter> getExampleList() {

    List<UkbEncounter> encounterExamples = new ArrayList<>();

    // Adding an encounter resource that is in-progress and got a valid icu transfer.
    UkbEncounter encounterInpatientInProgress = new UkbEncounter(ENCOUNTER_ID_INPATIENT,
        new Encounter.EncounterStatusEnumFactory().fromType(
            new StringType(EncounterStatus.INPROGRESS.toCode())),
        new Coding("http://fhir.de/ValueSet/EncounterClassDE", "IMP", null));
    encounterInpatientInProgress.addLocation(
        new EncounterLocationComponent(
            new Reference("Location/" + LocationExampleData.ICU_LOCATION_ID)));
    encounterInpatientInProgress.addExtension(markPositive());
    encounterExamples.add(encounterInpatientInProgress);

    // Missing code system in codings should all be handled and not throw any exception.
    UkbEncounter encounterWithMissingCodeSystems = new UkbEncounter(ENCOUNTER_ID_MISSING_ATTRIBUTES,
        new Encounter.EncounterStatusEnumFactory().fromType(
            new StringType(EncounterStatus.INPROGRESS.toCode())),
        new Coding(null, "IMP", null));
    // Usually the encounter.type.kontaktart.system = "http://fhir.de/CodeSystem/kontaktart-de"
    encounterWithMissingCodeSystems.addType(
        new CodeableConcept().addCoding(new Coding(null, "vorstationaer", "vorstationaer")));
    encounterWithMissingCodeSystems.addExtension(markPositive());
    encounterInpatientInProgress.addLocation(
        new EncounterLocationComponent(
            new Reference("Location/" + LocationExampleData.NON_VALID_LOCATION_ID)));

    encounterExamples.add(encounterWithMissingCodeSystems);

    return encounterExamples;
  }

}
