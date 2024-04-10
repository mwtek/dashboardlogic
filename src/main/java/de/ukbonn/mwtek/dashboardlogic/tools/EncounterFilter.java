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

package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.CASETYPE_CONTACT_ART_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.CASETYPE_POSTSTATIONARY;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.CASETYPE_PRESTATIONARY;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.CONTACT_LEVEL_FACILITY_CODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.CONTACT_LEVEL_SUPPLY_CODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.CONTACT_LEVEL_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DEATH_CODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DISCHARGE_DISPOSITION_EXT_URL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_EXT_URL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.ENCOUNTER_CLASS_INPATIENT_CODES;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.ENCOUNTER_CLASS_OUTPATIENT_CODES;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE_RESULT;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodesystem;

import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Extension;

/**
 * Various auxiliary methods that affect the encounter resources.
 */
@Slf4j
public class EncounterFilter {

  /**
   * Determines whether the passed encounter instance has a covid-positive flag.
   */
  public static boolean isDiseasePositive(UkbEncounter encounter) {
    return encounter.hasExtension(POSITIVE_RESULT.getValue());
  }

  /**
   * Determines whether the passed encounter instance is in-progress.
   */
  public static boolean isActive(UkbEncounter encounter) {
    return encounter.hasStatus() && encounter.getStatus() == EncounterStatus.INPROGRESS;
  }

  /**
   * Determines whether the passed encounter type is a facility contact ("Einrichtungskontakt"). To
   * make it backwards compatible, we count any missing type as facility contact aswell!
   */
  public static boolean isFacilityContact(UkbEncounter encounter) {
    // Warning: the behavior of the getType method of the HAPI library is misleading.
    // If an encounter resource has 2 encodings in a type, then it has 2 entries in the list
    // attribute "Type".
    if (encounter.hasType()) {
      List<Coding> contactLevelTypeCodings = encounter.getType().stream()
          .flatMap(x -> x.getCoding().stream())
          .filter(Coding::hasSystem).filter(x -> x.getSystem().equals(CONTACT_LEVEL_SYSTEM))
          .toList();

      // Search for the code for facility contact.
      for (Coding contactLevelType : contactLevelTypeCodings) {
        if (contactLevelType.getCode().equals(CONTACT_LEVEL_FACILITY_CODE)) {
          return true;
        }
      }
      // No support for the contact level yet -> mark the resource as a facility contact, as its
      // most likely the top level
      if (contactLevelTypeCodings.isEmpty()) {
        return true;
      }
    }

    // To make the project backwards compatible we count any missing type as facility contact
    // aswell!
    // no type in resource -> count it as facility contact
    return !encounter.hasType();
  }

  /**
   * Determines whether the passed encounter type is a facility contact
   * ("Versorgungsstellenkontakt").
   */
  public static boolean isSupplyContact(UkbEncounter encounter) {
    // Warning: the behavior of the getType method of the HAPI library is misleading.
    // If an encounter resource has 2 encodings in a type, then it has 2 entries in the list
    // attribute "Type".
    if (encounter.hasType()) {
      List<Coding> contactLevelTypeCodings = encounter.getType().stream()
          .flatMap(x -> x.getCoding().stream())
          .filter(Coding::hasSystem).filter(x -> x.getSystem().equals(CONTACT_LEVEL_SYSTEM))
          .toList();

      // Search for the code for facility contact.
      for (Coding contactLevelType : contactLevelTypeCodings) {
        if (contactLevelType.getCode().equals(CONTACT_LEVEL_SUPPLY_CODE)) {
          return true;
        }
      }
    }
    return !encounter.hasType();
  }

  /**
   * Checks whether the given encounter is currently in an ICU location or not.
   *
   * @param encounter      The encounter to check.
   * @param icuLocationIds The list of ICU location IDs to check against. If its empty it will
   *                       return {@code False}.
   * @return {@code True}  if the encounter is currently in an ICU location; otherwise,
   * {@code False}.
   */
  public static boolean isCurrentlyOnIcu(@NonNull UkbEncounter encounter,
      List<String> icuLocationIds) {

    if (icuLocationIds == null || icuLocationIds.isEmpty()) {
      return false;
    }

    // Find the active transfer and if one can be found, check if it's an icu location.
    return encounter.getLocation().stream()
        .filter(x -> x.hasPeriod() && !x.getPeriod().hasEnd())
        .anyMatch(x -> icuLocationIds.contains(x.getLocation().getIdBase()));
  }

  /**
   * A simple check if the given contact type got the value "prestationary".
   *
   * @param listType A list of {@link Encounter#getType() Encounter.types}.
   * @return <code>True</code> if the case type equals "prestationary".
   */
  public static boolean isCaseTypePreStationary(List<CodeableConcept> listType) {
    String contactType = getContactType(listType);
    return contactType != null && contactType.equals(CASETYPE_PRESTATIONARY.getValue());
  }

  /**
   * A simple check if the given contact type got the value "prestationary".
   *
   * @param ukbEncounter The encounter to check.
   * @return <code>True</code> if the case type equals "prestationary".
   */
  public static boolean isCaseTypePostStationary(UkbEncounter ukbEncounter) {
    String contactType = getContactType(ukbEncounter.getType());
    return contactType != null && contactType.equals(CASETYPE_POSTSTATIONARY.getValue());
  }

  /**
   * Is the case class counted as "inpatient" regarding the json data specification (without
   * pre-stationary cases)?
   *
   * @param encounter An instance of an {@link UkbEncounter} object.
   * @return <code>True</code>, if the case class of the encounter is "inpatient"
   */
  public static boolean isCaseClassInpatient(UkbEncounter encounter) {
    return encounter.hasClass_() && isCodeInCodesystem(encounter.getClass_().getCode(),
        ENCOUNTER_CLASS_INPATIENT_CODES) && !isCaseTypePreStationary(
        encounter.getType());
  }

  /**
   * is the case class counted as "outpatient" regarding the json data specification (plus
   * pre-stationary cases that are counted as "outpatient" logic-wise in the workflow aswell)
   *
   * @param encounter An instance of an {@link UkbEncounter} object.
   * @return <code>True</code>, if the case class of the encounter is "outpatient".
   */
  public static boolean isCaseClassOutpatient(UkbEncounter encounter) {
    return encounter.hasClass_() && isCodeInCodesystem(encounter.getClass_().getCode(),
        ENCOUNTER_CLASS_OUTPATIENT_CODES) || isCaseTypePreStationary(
        encounter.getType());
  }


  /**
   * Retrieval of the value that is part of a slice in {@link Encounter#getType() Encounter.type}.
   *
   * @param listType List of {@link Encounter#getType() Encounter.types}.
   * @return The contact-type of a german value set (e.g. "vorstationär").
   */
  private static String getContactType(List<CodeableConcept> listType) {

    StringBuilder contactType = new StringBuilder();
    listType.forEach(ccType -> ccType.getCoding().forEach(codingType -> {
      if (codingType.hasSystem() && codingType.getSystem()
          .equals(CASETYPE_CONTACT_ART_SYSTEM.getValue())) {
        contactType.append(codingType.getCode());
      }
    }));

    return !contactType.isEmpty() ? contactType.toString() : null;
  }

  /**
   * Determine via the discharge disposition which is part of
   * {@link UkbEncounter#getHospitalization()} whether the patient is deceased within the scope of
   * the case under review.
   *
   * @param enc An instance of an {@link UkbEncounter} object
   * @return <code>false</code>, if no valid discharge disposition can be found in the {@link
   * UkbEncounter#getHospitalization()} instance and <code>true</code> if the discharge code ("07")
   * was found
   */
  public static boolean isPatientDeceased(UkbEncounter enc) {

    Encounter.EncounterHospitalizationComponent hospComp = enc.getHospitalization();
    // check if encounter resource got a discharge disposition with a certain extension url
    if (hospComp != null && hospComp.hasDischargeDisposition() && hospComp.getDischargeDisposition()
        .hasExtension(DISCHARGE_DISPOSITION_EXT_URL.getValue())) {
      Extension extDischargeDisp = hospComp.getDischargeDisposition()
          .getExtensionByUrl(DISCHARGE_DISPOSITION_EXT_URL.getValue());
      Extension extPosFirstAndSec = extDischargeDisp.getExtensionByUrl(
          DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_EXT_URL.getValue());
      if (extPosFirstAndSec != null) {
        // the extension always contains a coding as value
        try {
          Coding coding = (Coding) extPosFirstAndSec.getValue();
          // If the system is valid, check the code right after
          if (coding.hasSystem() && coding.getSystem().equals(
              DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_SYSTEM.getValue())) {
            // the code must be "07" (Death)
            if (coding.hasCode() && coding.getCode()
                .equals(DEATH_CODE.getValue())) {
              return true;
            }
          } else {
            return false;
          }
        } catch (ClassCastException cce) {
          log.error(
              "Encounter.hospitalization.dischargeDisposition"
                  + ".EntlassungsgrundErsteUndZweiteStelle.value must be from type Coding but "
                  + "found: "
                  + extPosFirstAndSec.getValue()
                  .getClass());
        }
      }
    }

    return false;
  }

  public static List<UkbEncounter> getPositiveCurrentlyOnIcuWardEncounters(
      Collection<UkbEncounter> supplyContactEncounters, List<UkbLocation> locations) {
    return supplyContactEncounters
        .stream().filter(EncounterFilter::isDiseasePositive)
        .filter(x -> x.isCurrentlyOnIcuWard(LocationFilter.getIcuLocationIds(locations)))
        .toList();
  }
}
