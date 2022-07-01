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
package de.ukbonn.mwtek.dashboardlogic.logic.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class contains some auxiliary functions that are used in the logic of the timelines
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
public class TimelineFunctionalities {

  /**
   * Splits the resource part from the id in a fhir reference (e.g. {@literal Location/123 -> 123)}
   *
   * @param fhirResourceReference A string with a FHIR resource reference (e.g. {@literal
   *                              Location/123})
   * @return The plain id of the resource
   */
  public static String splitReference(String fhirResourceReference, String fhirId) {
    String[] parts = fhirResourceReference.split("/");
    return parts[parts.length - 1];
  }

  /**
   * Purpose of this auxiliary function is to divide the content of a map into two different lists
   *
   * @param tempMap   Map that maps a frequency value to a date (unixtime)
   * @param dateList  Output list with the date entries
   * @param valueList Output list with the values (frequencies per day)
   */
  public static void divideMapValuesToLists(Map<Long, Long> tempMap, List<Long> dateList,
      List<Long> valueList) {

    // get a list with the keys in ascending order (output requirement)
    List<Long> listKeys = new ArrayList<>(tempMap.keySet());
    Collections.sort(listKeys);

    listKeys.forEach(key -> {
      Long value = tempMap.get(key);
      dateList.add(key);
      valueList.add(value);
    });
  }
}
