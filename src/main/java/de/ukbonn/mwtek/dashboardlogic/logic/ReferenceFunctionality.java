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
package de.ukbonn.mwtek.dashboardlogic.logic;

import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues.IDENTIFIER_TYPE_VISIT_NUMBER_CC;

import de.ukbonn.mwtek.dashboardlogic.enums.EncounterReferenceResourceType;
import de.ukbonn.mwtek.dashboardlogic.enums.EncounterReferenceTargetType;
import de.ukbonn.mwtek.dashboardlogic.models.FacilityEncounterToIcuSupplyContactsMap;
import de.ukbonn.mwtek.utilities.fhir.interfaces.CaseIdentifierValueProvider;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Identifier;

/**
 * This class contains all the functions that are used in different disease context.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class ReferenceFunctionality {

  /**
   * Assigns ICU supply contact encounters to their corresponding facility encounters based on the
   * visit number identifier.
   *
   * @param icuSupplyContactEncounters ICU supply contact encounters to be assigned
   * @param inpatientEncounters inpatient encounters containing the facility encounters
   * @return mapping of facility encounter IDs to their associated ICU supply contact encounters
   */
  public static FacilityEncounterToIcuSupplyContactsMap assignSupplyEncountersToFacilityEncounter(
      List<MiiEncounter> icuSupplyContactEncounters, List<MiiEncounter> inpatientEncounters) {

    // Initialize output map
    FacilityEncounterToIcuSupplyContactsMap output = new FacilityEncounterToIcuSupplyContactsMap();

    // Group ICU supply contacts by official identifier value
    Map<String, List<MiiEncounter>> icuSupplyContactsMap =
        icuSupplyContactEncounters.stream()
            .filter(
                enc -> {
                  boolean hasValue = enc.hasVisitNumberIdentifierValue();
                  if (!hasValue) {
                    log.warn("Encounter {} has no valid visit number identifier", enc.getId());
                  }
                  return hasValue;
                })
            .collect(Collectors.groupingBy(MiiEncounter::getVisitNumberIdentifierValue));

    List<MiiEncounter> facilityContacts =
        inpatientEncounters.parallelStream().filter(MiiEncounter::isFacilityContact).toList();

    // Iterate over facility contacts and map supply contacts
    for (MiiEncounter facilityContact : facilityContacts) {
      try {
        String visitNumber = facilityContact.getVisitNumberIdentifierValue();
        // Skipping element if visit number is empty
        if (visitNumber == null) {
          log.warn("Facility contact {} has no visit number identifier", facilityContact.getId());
          continue;
        }
        // Get supply contacts for the facility contact
        List<MiiEncounter> supplyContacts =
            icuSupplyContactsMap.getOrDefault(visitNumber, Collections.emptyList());
        // Add the mapping to the output, using an empty list if no supply contacts are present
        output
            .computeIfAbsent(facilityContact.getId(), _ -> new ArrayList<>())
            .addAll(supplyContacts);
      } catch (Exception ex) {
        log.error(
            "Unable to determine the supply contacts for facility contact {}",
            facilityContact.getId());
      }
    }
    return output;
  }

  /**
   * Generates a mapping from supply contacts to facility contacts.
   *
   * @param supplyContactEncounters The list of supply contacts.
   * @param departmentEncounters The list of department contacts.
   * @param facilityEncounters The list of facility contacts.
   * @param usePartOf Use {@link MiiEncounter#getPartOf()} attribute instead of the visit number in
   *     {@link MiiEncounter#getIdentifier()}
   * @return A mapping from supply contacts to facility contacts.
   * @throws RuntimeException if no encounter with an identifier.slice 'Aufnahmenummer' was found.
   */
  public static Map<String, String> generateSupplyContactToFacilityContactMap(
      List<MiiEncounter> supplyContactEncounters,
      List<MiiEncounter> departmentEncounters,
      List<MiiEncounter> facilityEncounters,
      boolean usePartOf) {

    // Initialize output map
    Map<String, String> output =
        mapContactsToFacilityContacts(supplyContactEncounters, facilityEncounters);
    applyFacilityContactIds(supplyContactEncounters, output);

    // Additionally resolve the encounter hierarchy via Encounter.partOf if configured
    if (usePartOf) {
      log.info("Encounter hierarchy resolution via .partOf was activated in the config.");
      supplyContactEncounters.forEach(
          supplyContact -> {
            if (supplyContact.hasPartOf()) {
              if (supplyContact.getPartOf().hasReference()) {
                String supplyContactPartOf = extractIdFromReference(supplyContact.getPartOf());

                if (supplyContactPartOf != null) {
                  MiiEncounter departmentContact =
                      departmentEncounters.stream()
                          .filter(x -> x.getId().equals(supplyContactPartOf))
                          .findFirst()
                          .orElse(null);

                  if (departmentContact != null) {
                    String departmentContactPartOf =
                        extractIdFromReference(departmentContact.getPartOf());

                    if (departmentContactPartOf != null) {
                      MiiEncounter facilityContact =
                          facilityEncounters.stream()
                              .filter(x -> x.getId().equals(departmentContactPartOf))
                              .findFirst()
                              .orElse(null);

                      if (facilityContact != null) {
                        supplyContact.setFacilityContactId(facilityContact.getId());
                        output.put(supplyContact.getId(), facilityContact.getId());

                        // Use the facility contact ID as visit number for downstream processing
                        supplyContact.addIdentifier(createIdentifierVisitNumber(facilityContact));
                        facilityContact.addIdentifier(createIdentifierVisitNumber(facilityContact));
                      }
                    } else {
                      log.warn(
                          "No partOf identifier found for 'Encounter.abteilungskontakt' with id {}",
                          departmentContact.getId());
                    }
                  } else {
                    log.warn(
                        "No department reference found for 'Encounter.versorgungsstellenkontakt'"
                            + " with id {}",
                        supplyContact.getId());
                  }
                } else {
                  log.warn(
                      "No partOf identifier found for 'Encounter.versorgungsstellenkontakt' with id"
                          + " {}",
                      supplyContact.getId());
                }
              } else {
                log.warn(
                    "Encounter with id {} holds a .partOf element but no literal reference",
                    supplyContact.getId());
              }
            }
          });
    }
    if (output.isEmpty()) {
      throw new RuntimeException(
          "No encounter with an identifier.slice 'Aufnahmenummer' was found.");
    }
    return output;
  }

  /**
   * Applies resolved facility contact IDs to the given contacts.
   *
   * @param contacts the contacts to update
   * @param contactToFacilityContactMap mapping from contact ID to facility contact ID
   */
  private static void applyFacilityContactIds(
      List<MiiEncounter> contacts, Map<String, String> contactToFacilityContactMap) {

    contacts.forEach(
        contact -> {
          String facilityContactId = contactToFacilityContactMap.get(contact.getId());

          if (facilityContactId != null) {
            contact.setFacilityContactId(facilityContactId);
          }
        });
  }

  private static Identifier createIdentifierVisitNumber(MiiEncounter facilityContact) {
    return new Identifier()
        .setType(IDENTIFIER_TYPE_VISIT_NUMBER_CC)
        .setValue(facilityContact.getId());
  }

  /**
   * Creates an index of facility encounters by visit number.
   *
   * @param facilityEncounters the facility encounters to index
   * @return a map from visit number to facility encounter
   */
  private static Map<String, MiiEncounter> createFacilityContactByVisitNumberMap(
      List<MiiEncounter> facilityEncounters) {

    Map<String, MiiEncounter> facilityContactByVisitNumber = new ConcurrentHashMap<>();

    facilityEncounters.stream()
        .filter(MiiEncounter::isFacilityContact)
        .forEach(
            facilityContact -> {
              String visitNumber = facilityContact.getVisitNumberIdentifierValue();

              if (visitNumber != null) {
                facilityContactByVisitNumber.putIfAbsent(visitNumber, facilityContact);
              } else {
                log.warn(
                    "No identifier with slice 'Aufnahmenummer' was found for encounter with id {}",
                    facilityContact.getId());
              }
            });

    return facilityContactByVisitNumber;
  }

  public static Map<String, String> createContactToFacilityContactMap(
      List<MiiEncounter> encounters) {

    List<MiiEncounter> contacts =
        encounters.stream()
            .filter(encounter -> encounter.isDepartmentContact() || encounter.isSupplyContact())
            .toList();

    return mapContactsToFacilityContacts(contacts, encounters);
  }

  /**
   * Resolves contacts to facility contacts using their visit number.
   *
   * @param contacts the contacts to resolve
   * @param facilityEncounters the available facility encounters
   * @return a map from contact ID to facility contact ID
   */
  private static Map<String, String> mapContactsToFacilityContacts(
      List<MiiEncounter> contacts, List<MiiEncounter> facilityEncounters) {

    Map<String, MiiEncounter> facilityContactByVisitNumber =
        createFacilityContactByVisitNumberMap(facilityEncounters);

    Map<String, String> output = new ConcurrentHashMap<>();

    // Resolve contacts to their facility contacts
    contacts.parallelStream()
        .forEach(
            contact -> {
              try {
                String visitNumber = contact.getVisitNumberIdentifierValue();
                if (visitNumber == null) {
                  log.warn(
                      "No visit number identifier found for contact encounter with id {}",
                      contact.getId());
                  return;
                }
                MiiEncounter facilityContact = facilityContactByVisitNumber.get(visitNumber);
                if (facilityContact != null) {
                  output.put(contact.getId(), facilityContact.getId());
                }
              } catch (Exception ex) {
                log.error(
                    "Error processing contact encounter {}: {}", contact.getId(), ex.getMessage());
              }
            });
    return output;
  }

  /**
   * Normalizes encounter references to facility contacts if required.
   *
   * @param resources the resources to update
   * @param encounters the available encounters
   * @param configuredTargetType the configured encounter reference target type
   * @param resourceType the resource type used for logging
   * @param <T> the encounter-referencing resource type
   * @return the updated resources
   */
  public static <T extends CaseIdentifierValueProvider> List<T> updateEncounterReferences(
      List<T> resources,
      List<MiiEncounter> encounters,
      EncounterReferenceTargetType configuredTargetType,
      EncounterReferenceResourceType resourceType) {

    if (resources.isEmpty() || configuredTargetType == EncounterReferenceTargetType.EK) {
      return resources;
    }
    String firstEncounterReference = resources.getFirst().getCaseId();

    // Skip normalization if the actual reference does not match the configured type
    if (!validateConfiguredEncounterReferenceType(
        firstEncounterReference, encounters, configuredTargetType, resourceType)) {
      return resources;
    }
    // Build the mapping only when normalization to facility contacts is required
    Map<String, String> contactToFacilityContactId = createContactToFacilityContactMap(encounters);
    resources.forEach(
        resource -> {
          String facilityContactId = contactToFacilityContactId.get(resource.getCaseId());
          if (facilityContactId != null) {
            resource.setCaseId(facilityContactId);
          }
        });

    return resources;
  }

  /**
   * Validates whether an encounter reference matches the configured retrieval type.
   *
   * @param encounterReference the encounter reference to validate
   * @param encounters the available encounters
   * @param configuredTargetType the configured encounter target type
   * @param resourceType the resource type used for logging
   */
  private static boolean validateConfiguredEncounterReferenceType(
      String encounterReference,
      List<MiiEncounter> encounters,
      EncounterReferenceTargetType configuredTargetType,
      EncounterReferenceResourceType resourceType) {

    MiiEncounter referencedEncounter =
        encounters.stream()
            .filter(encounter -> encounter.getId().equals(encounterReference))
            .findFirst()
            .orElse(null);

    if (referencedEncounter == null) {
      log.warn(
          "Unable to resolve encounter reference {} of the first {}",
          encounterReference,
          resourceType);
      return false;
    } else if (!configuredTargetType.matches(referencedEncounter)) {
      log.warn(
          "First {} references encounter {} which does not match configured target type {}",
          resourceType,
          encounterReference,
          configuredTargetType);
      return false;
    }
    return true;
  }

  /**
   * Updates the encounter references for conditions of the cases that get merged.
   *
   * @param oldEncounterId the old encounter ID to be replaced, must not be null
   * @param newEncounterId the new encounter ID to replace with, must not be null
   * @throws IllegalArgumentException if either oldEncounterId or newEncounterId is null
   */
  public static void updateConditionEncounterReferencesOnMerge(
      List<MiiCondition> conditions, String oldEncounterId, String newEncounterId) {
    if (oldEncounterId == null || newEncounterId == null) {
      throw new IllegalArgumentException("Encounter IDs must not be null");
    }
    conditions.stream()
        .filter(condition -> condition.getCaseId().equals(oldEncounterId))
        .forEach(
            condition -> {
              condition.setCaseId(newEncounterId);
              log.info(
                  "Updated condition {} encounter id from {} to {}",
                  condition.getId(),
                  oldEncounterId,
                  newEncounterId);
            });
  }

  /**
   * Updates the encounter references for procedures of the cases that get merged.
   *
   * @param procedures the list of procedures to update; may be empty but not null-safe if method is
   *     called with null
   * @param oldEncounterId the old encounter ID to be replaced, must not be null
   * @param newEncounterId the new encounter ID to replace with, must not be null
   */
  public static void updateProcedureEncounterReferences(
      List<MiiProcedure> procedures, String oldEncounterId, String newEncounterId) {

    if (oldEncounterId == null || newEncounterId == null) {
      throw new IllegalArgumentException("Encounter IDs must not be null");
    }
    if (procedures == null || procedures.isEmpty()) return;

    procedures.stream()
        .filter(Objects::nonNull)
        .filter(p -> oldEncounterId.equals(p.getCaseId()))
        .forEach(
            p -> {
              p.setCaseId(newEncounterId);
              log.info(
                  "Updated procedure {} encounter id from {} to {}",
                  p.getId(),
                  oldEncounterId,
                  newEncounterId);
            });
  }
}
