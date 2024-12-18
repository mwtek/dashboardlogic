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

import static de.ukbonn.mwtek.dashboardlogic.logic.cumulative.lengthofstay.CumulativeLengthOfStayIcu.createIcuLengthOfStayList;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.examples.EncounterExampleData;
import de.ukbonn.mwtek.dashboardlogic.examples.InputCodeSettingsExampleData;
import de.ukbonn.mwtek.dashboardlogic.examples.LocationExampleData;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardData;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CumulativeLengthOfStayIcuTests {

  @Test
  @DisplayName(
      "Ensuring that the cumulative.lengthofstay.icu data item is able to handle empty "
          + "fields in the location resource.")
  void testCreateListWithoutLocations() {
    DashboardData dbData =
        new DashboardData()
            .initializeData(
                InputCodeSettingsExampleData.getExampleData(),
                null,
                EncounterExampleData.getExampleList(),
                null,
                null,
                null,
                null,
                null,
                DataItemContext.COVID);
    Map<String, Map<Long, Set<String>>> mapIcuLengthList =
        createIcuLengthOfStayList(dbData.getSupplyContactEncounters(), dbData.getLocations());
  }

  @Test
  @DisplayName(
      "Ensuring that the cumulative.lengthofstay.icu data item is able to handle even "
          + "non-valid-locations.")
  void testCreateListWithLocations() {
    DashboardData dbData =
        new DashboardData()
            .initializeData(
                InputCodeSettingsExampleData.getExampleData(),
                null,
                EncounterExampleData.getExampleList(),
                null,
                null,
                null,
                LocationExampleData.getExampleList(),
                null,
                DataItemContext.COVID);
    Map<String, Map<Long, Set<String>>> mapIcuLengthList =
        createIcuLengthOfStayList(dbData.getSupplyContactEncounters(), dbData.getLocations());
  }
}
