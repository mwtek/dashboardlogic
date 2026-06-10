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
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.getRsvOnlyCoreCaseDataByGroups;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.KiraInteger;
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
public class KiraCumulativeDiagsGender extends DashboardDataItemLogic {

  final List<String> STACKS_GENDER = Arrays.asList("male", "female", "diverse");

  /**
   * Builds a gender-distribution stacked bar chart for KJP where numeric values are wrapped as
   * {@link KiraInteger}. One chart is produced with bars per diagnostic group and stacks for
   * genders (e.g., male/female/other). Unknown/missing genders are logged but excluded.
   *
   * @param coreCaseDataByGroups map of diagnostic group -> (caseId → {@code CoreCaseData})
   */
  public StackedBarChartsItem<KiraInteger> createStackBarChartsKjp(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // Build with KiraValue wrapping
    return createStackBarChartsCore(
        KidsRadarDataItemContext.KJP, coreCaseDataByGroups, KiraInteger::new);
  }

  /**
   * Builds a gender-distribution stacked bar chart for PED with plain numeric values ({@link
   * java.lang.Integer}). The input is RSV-filtered internally (group map reduced to RSV scope).
   * Unknown/missing genders are logged but excluded.
   *
   * @param coreCaseDataByGroups map of diagnostic group → (caseId → {@code CoreCaseData})
   */
  public StackedBarChartsItem<Integer> createStackBarChartsPed(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    // Filter RSV-only for PED and build with plain Integer
    Map<String, Map<String, CoreCaseData>> filtered =
        getRsvOnlyCoreCaseDataByGroups(coreCaseDataByGroups);
    return createStackBarChartsCore(
        KidsRadarDataItemContext.PED, filtered, java.lang.Integer::valueOf);
  }

  private <T> StackedBarChartsItem<T> createStackBarChartsCore(
      KidsRadarDataItemContext ctx,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      java.util.function.IntFunction<T> wrap) {

    log.debug("started createStackBarChartsCore ({})", ctx);
    Instant startTime = TimerTools.startTimer();

    StackedBarChartsItem<T> result = new StackedBarChartsItem<>();

    // Determine chart label by context
    String chartsLabel = determineKiRaChartsAllLabelByContext(ctx);

    // Labels
    result.setCharts(new ArrayList<>(List.of(chartsLabel)));
    result.setStacks(List.of(STACKS_GENDER)); // e.g., ["male", "female", "other"]
    result.setBars(List.of(createLabelList(coreCaseDataByGroups.keySet())));

    // Build values per bar (group key)
    List<List<T>> resultValues = new ArrayList<>();

    coreCaseDataByGroups.forEach(
        (groupKey, coreCaseDataItem) -> {
          // Retrieve cases by gender
          List<CoreCaseData> males =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.MALE);
          List<CoreCaseData> females =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.FEMALE);
          List<CoreCaseData> others =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.OTHER);

          // Add the gender distribution values to the result (wrapped)
          resultValues.add(
              List.of(
                  wrap.apply(males.size()), wrap.apply(females.size()), wrap.apply(others.size())));

          // Debug for unknown/missing genders (not included in chart)
          List<CoreCaseData> unknowns =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.UNKNOWN);
          List<CoreCaseData> missing =
              getCoreCaseDataByGender(coreCaseDataItem, AdministrativeGender.NULL);

          if (!unknowns.isEmpty()) {
            log.warn(
                "Skipping {} patient resources with gender 'unknown' in group {}. [Example:"
                    + " Patient/{}]",
                unknowns.size(),
                groupKey,
                unknowns.get(0).getPatientId());
          }
          if (!missing.isEmpty()) {
            log.warn(
                "Skipping {} patient resources with missing gender in group {}. [Example:"
                    + " Patient/{}]",
                missing.size(),
                groupKey,
                missing.get(0).getPatientId());
          }
        });

    // Set values as a single-chart payload
    result.setValues(List.of(resultValues));

    TimerTools.stopTimerAndLog(startTime, "finished createStackBarChartsCore (" + ctx + ")");
    return result;
  }

  private static List<CoreCaseData> getCoreCaseDataByGender(
      Map<String, CoreCaseData> coreCaseDataItem, AdministrativeGender administrativeGender) {
    return coreCaseDataItem.values().stream()
        .filter(x -> x.getPatient().getGender() == administrativeGender)
        .toList();
  }
}
