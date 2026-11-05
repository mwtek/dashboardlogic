package de.ukbonn.mwtek.dashboardlogic.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReferenceFunctionalityTests {

  private static MiiEncounter encounter(String id) {
    MiiEncounter encounter = new MiiEncounter();
    encounter.setId(id);
    return encounter;
  }

  private static MiiEncounter supplyContact(String id, String partOfId) {
    MiiEncounter encounter = encounter(id);
    encounter.setPartOf(new Reference("Encounter/" + partOfId));
    return encounter;
  }

  private static MiiCondition condition(String id, String caseId) {
    MiiCondition condition = new MiiCondition();
    condition.setId(id);
    condition.setCaseId(caseId);
    return condition;
  }

  private static MiiProcedure procedure(String id, String caseId) {
    MiiProcedure procedure = new MiiProcedure();
    procedure.setId(id);
    procedure.setCaseId(caseId);
    return procedure;
  }

  @Test
  @DisplayName(
      "Resolving the encounter hierarchy via .partOf maps the supply contact to its facility"
          + " contact and leaves contacts without a department reference unmapped.")
  void testPartOfHierarchyResolution() {
    MiiEncounter facilityContact = encounter("EK-1");
    MiiEncounter departmentContact = supplyContact("AB-1", "EK-1");
    MiiEncounter resolvedSupplyContact = supplyContact("SV-1", "AB-1");
    // Department reference points to an encounter that is not part of the extract
    MiiEncounter unresolvedSupplyContact = supplyContact("SV-2", "AB-UNKNOWN");

    Map<String, String> result =
        ReferenceFunctionality.generateSupplyContactToFacilityContactMap(
            List.of(resolvedSupplyContact, unresolvedSupplyContact),
            List.of(departmentContact),
            List.of(facilityContact),
            true);

    assertEquals("EK-1", result.get("SV-1"));
    assertFalse(result.containsKey("SV-2"));
    assertEquals("EK-1", resolvedSupplyContact.getFacilityContactId());
  }

  @Test
  @DisplayName(
      "When an encounter ID occurs more than once, the first occurrence wins - just like the"
          + " linear stream lookup this index replaced.")
  void testIndexKeepsFirstOccurrenceOfDuplicateIds() {
    MiiEncounter first = encounter("AB-1");
    MiiEncounter second = encounter("AB-1");

    Map<String, MiiEncounter> index = ReferenceFunctionality.indexEncountersById(List.of(first, second));

    assertEquals(1, index.size());
    assertEquals(first, index.get("AB-1"));
  }

  @Test
  @DisplayName(
      "Merging a chain of cases moves every condition to the canonical case, including the"
          + " conditions of an already merged case.")
  void testConditionReferencesFollowAChainOfMerges() {
    MiiCondition conditionOfB = condition("COND-B", "B");
    MiiCondition conditionOfC = condition("COND-C", "C");
    MiiCondition conditionOfA = condition("COND-A", "A");
    Map<String, List<MiiCondition>> index =
        ReferenceFunctionality.indexConditionsByCaseId(
            new ArrayList<>(List.of(conditionOfA, conditionOfB, conditionOfC)));

    // Merge B into A, then the already grown case A is merged into by C
    assertEquals(1, ReferenceFunctionality.updateConditionEncounterReferencesOnMerge(index, "B", "A"));
    assertEquals(1, ReferenceFunctionality.updateConditionEncounterReferencesOnMerge(index, "C", "A"));

    assertEquals("A", conditionOfB.getCaseId());
    assertEquals("A", conditionOfC.getCaseId());
    assertEquals("A", conditionOfA.getCaseId());
    assertFalse(index.containsKey("B"));
    assertFalse(index.containsKey("C"));
    assertEquals(3, index.get("A").size());
  }

  @Test
  @DisplayName("Merging moves the procedures of the merged case and reports how many were updated.")
  void testProcedureReferencesFollowMerge() {
    MiiProcedure procedureOfB = procedure("PROC-B", "B");
    MiiProcedure procedureOfOther = procedure("PROC-X", "X");
    Map<String, List<MiiProcedure>> index =
        ReferenceFunctionality.indexProceduresByCaseId(List.of(procedureOfB, procedureOfOther));

    assertEquals(1, ReferenceFunctionality.updateProcedureEncounterReferences(index, "B", "A"));
    assertEquals("A", procedureOfB.getCaseId());
    assertEquals("X", procedureOfOther.getCaseId());
    // A second merge for the same case has nothing left to move
    assertEquals(0, ReferenceFunctionality.updateProcedureEncounterReferences(index, "B", "A"));
  }

  @Test
  @DisplayName(
      "The list-based merge variants update the same resources as the index-based ones, so callers"
          + " that were not migrated keep working.")
  void testListBasedMergeVariantsStillWork() {
    MiiCondition conditionOfB = condition("COND-B", "B");
    List<MiiCondition> conditions = new ArrayList<>(List.of(conditionOfB));

    ReferenceFunctionality.updateConditionEncounterReferencesOnMerge(conditions, "B", "A");
    assertEquals("A", conditionOfB.getCaseId());

    MiiProcedure procedureOfB = procedure("PROC-B", "B");
    ReferenceFunctionality.updateProcedureEncounterReferences(List.of(procedureOfB), "B", "A");
    assertEquals("A", procedureOfB.getCaseId());
  }

  @Test
  @DisplayName("Conditions without a case reference are skipped by the index instead of failing.")
  void testConditionsWithoutCaseIdAreSkipped() {
    MiiCondition withoutCaseId = new MiiCondition();
    withoutCaseId.setId("COND-NO-CASE");

    Map<String, List<MiiCondition>> index =
        ReferenceFunctionality.indexConditionsByCaseId(List.of(withoutCaseId));

    assertTrue(index.isEmpty());
    assertEquals(0, ReferenceFunctionality.updateConditionEncounterReferencesOnMerge(index, "B", "A"));
  }

  @Test
  @DisplayName(
      "Mapping encounters to patients finds each patient once and keeps encounters without a"
          + " matching patient.")
  void testMapEncountersToPatients() {
    MiiPatient patientOne = new MiiPatient();
    patientOne.setId("PID-1");
    MiiPatient patientTwo = new MiiPatient();
    patientTwo.setId("PID-2");

    MiiEncounter encounterOne = encounter("EK-1");
    encounterOne.setPatientId("PID-1");
    MiiEncounter encounterTwo = encounter("EK-2");
    encounterTwo.setPatientId("PID-2");
    MiiEncounter encounterWithoutPatient = encounter("EK-3");
    encounterWithoutPatient.setPatientId("PID-UNKNOWN");

    Set<MiiEncounter> encounters = new HashSet<>(List.of(encounterOne, encounterTwo, encounterWithoutPatient));
    Map<MiiEncounter, MiiPatient> result =
        KiraHandlingLogic.mapEncountersToPatients(encounters, List.of(patientOne, patientTwo));

    Map<MiiEncounter, MiiPatient> expected = new HashMap<>();
    expected.put(encounterOne, patientOne);
    expected.put(encounterTwo, patientTwo);
    expected.put(encounterWithoutPatient, null);
    assertEquals(expected, result);
  }
}
