/*
 *
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
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 *
 */
package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCodeableConcept;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.getCodeOfFirstCoding;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;

/**
 * Various auxiliary methods that affect the location resources.
 */
public class LocationFilter {

  /**
   * Is the given {@link UkbLocation location} of ICU type ?
   *
   * @param loc Any {@link UkbLocation location} resource
   * @return <code>True</code> if the given location describes an intensive care unit.
   */
  public static boolean isLocationIcu(UkbLocation loc) {
    if (loc.hasType()) {
      String codeLocationType = getCodeOfFirstCodeableConcept(loc.getType());
      return codeLocationType != null && codeLocationType
          .equals(CoronaFixedValues.ICU.getValue());
    } else {
      return false;
    }
  }

  /**
   * Does the passed  {@link UkbLocation location} resource describe a ward/station ?
   *
   * @param loc Any {@link UkbLocation location} resource
   * @return <code>True</code> if the given location describes a ward.
   */
  public static boolean isLocationWard(UkbLocation loc) {
    if (loc.hasPhysicalType()) {
      String codePhysicalType = getCodeOfFirstCoding(loc.getPhysicalType().getCoding());
      return codePhysicalType != null && codePhysicalType.equals(
          CoronaFixedValues.WARD.getValue());
    } else {
      return false;
    }
  }
}
