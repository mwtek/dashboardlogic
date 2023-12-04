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

package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.CONTACT_LEVEL_FACILITY_CODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.CONTACT_LEVEL_SYSTEM;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import java.util.List;
import lombok.NonNull;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;

/**
 * Various auxiliary methods that affect the encounter resources.
 */
public class EncounterFilter {

  /**
   * Determines whether the passed encounter instance has a covid-positive flag.
   */
  public static boolean isCovidPositive(UkbEncounter encounter) {
    return encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue());
  }

  /**
   * Determines whether the passed encounter instance is in-progress.
   */
  public static boolean isActive(UkbEncounter encounter) {
    return encounter.hasStatus() && encounter.getStatus() == EncounterStatus.INPROGRESS;
  }

  /**
   * Determines whether the passed encounter type is a facility contact ("Einrichtungskontakt"). To
   * make it backwards compatible we count any missing type as facility contact aswell!
   */
  public static boolean isFacilityContact(UkbEncounter encounter) {
    // Warning: the behavior of the getType method of the HAPI library is misleading.
    // If an encounter resource has 2 encodings in a type, then it has 2 entries in the list attribute "Type".
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
      // No support for the contact level yet -> mark the resource as a facility contact, as its most likely the top level
      if (contactLevelTypeCodings.size() == 0) {
        return true;
      }
    }

    // To make the project backwards compatible we count any missing type as facility contact aswell!
    // no type in resource -> count it as facility contact
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

    // Find the active transfer and if one can be found check if it's an icu location.
    return encounter.getLocation().stream()
        .filter(x -> x.hasPeriod() && !x.getPeriod().hasEnd())
        .anyMatch(x -> icuLocationIds.contains(x.getLocation().getIdBase()));
  }

}
