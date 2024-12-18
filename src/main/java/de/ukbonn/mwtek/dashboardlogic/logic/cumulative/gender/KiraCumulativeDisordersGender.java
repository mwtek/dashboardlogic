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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.gender;

import static de.ukbonn.mwtek.dashboardlogic.logic.KiraData.createLabelList;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;

@Slf4j
public class KiraCumulativeDisordersGender extends DashboardDataItemLogic {

  final List<String> STACKS_GENDER = Arrays.asList("male", "female", "diverse");

  /**
   * Creates a stacked bar chart representation of the given core case data grouped by diagnosis
   * groups. The chart will display gender distribution (male, female, other) for each diagnosis
   * group.
   *
   * <p>Processes a map of core case data, grouped by KJP / RSV diagnosis groups. For each group,
   * the number of male, female, and other gender cases is calculated and included in the chart.
   * Additionally, cases with unknown or missing gender entries are logged as warnings.
   *
   * @param coreCaseDataByGroups a map where the key represents a diagnosis group, and the value is
   *     another map of core case data for that group.
   * @return a {@link StackedBarChartsItem} object that contains the charts, stacks, bars, and
   *     values representing the gender distribution for each KJP diagnosis group.
   */
  public StackedBarChartsItem createStackBarCharts(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    log.debug("started createStackBarCharts");
    Instant startTime = TimerTools.startTimer();

    StackedBarChartsItem result = new StackedBarChartsItem();
    String chartsLabel = determineKiRaChartsAllLabelByContext(kidsRadarDataItemContext);

    // Setting chart, stack, and bar labels
    result.setCharts(new ArrayList<>(List.of(chartsLabel)));
    result.setStacks(List.of(STACKS_GENDER));
    result.setBars(List.of(createLabelList(coreCaseDataByGroups.keySet())));

    List<List<? extends Number>> resultValues = new ArrayList<>();

    coreCaseDataByGroups.forEach(
        (kjpGroupKey, coreCaseDataItem) -> {
          // Retrieve cases by gender
          List<CoreCaseData> males =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.MALE);
          List<CoreCaseData> females =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.FEMALE);
          List<CoreCaseData> others =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.OTHER);
          // Add the gender distribution values to the result
          resultValues.add(List.of(males.size(), females.size(), others.size()));
          // Debug logging for unknown and missing gender entries
          List<CoreCaseData> unknowns =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.UNKNOWN);
          List<CoreCaseData> missingGenderEntries =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.NULL);

          if (!unknowns.isEmpty()) {
            log.warn(
                "Skipping {} patient resources with gender 'unknown' in group {}. [Example:"
                    + " Patient/{}]",
                unknowns.size(),
                kjpGroupKey, // Log the KJP group key
                unknowns.get(0).getPatientId());
          }
          if (!missingGenderEntries.isEmpty()) {
            log.warn(
                "Skipping {} patient resources with missing gender in group {}. [Example:"
                    + " Patient/{}]",
                missingGenderEntries.size(),
                kjpGroupKey, // Log the KJP group key
                missingGenderEntries.get(0).getPatientId());
          }
        });

    // Set the values for the chart
    result.setValues(List.of(resultValues));

    // Log the time taken for the method to complete
    TimerTools.stopTimerAndLog(startTime, "finished createStackBarCharts");
    return result;
  }

  private static List<CoreCaseData> getCoreCaseDataByGender(
      Map<String, CoreCaseData> coreCaseDataItem, AdministrativeGender administrativeGender) {
    return coreCaseDataItem.values().stream()
        .filter(x -> x.getPatient().getGender() == administrativeGender)
        .toList();
  }
}
