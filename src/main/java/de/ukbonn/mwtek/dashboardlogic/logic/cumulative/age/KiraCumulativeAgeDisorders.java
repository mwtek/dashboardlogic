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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.age;

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.IN_GROUP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.OUT_GROUP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeCluster;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeKjpCluster;
import de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeRsvCluster;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Methods for the generation of the "kira.*.cumulative.diags.age" items for rsv and kjp.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KiraCumulativeAgeDisorders extends DashboardDataItemLogic {

  static final List<String> stacksKjp = Arrays.asList(IN_GROUP, OUT_GROUP);

  /**
   * Creates a stacked bar chart item based on the provided case data grouped by diseases.
   *
   * <p>The output is completely different from the similar sounding RSV age data item.
   *
   * @param coreCaseDataByGroups A map where the key is the disease group name, and the value is
   *     another map of individual case data entries.
   * @return A populated {@link StackedBarChartsItem} with chart names, bar data, and stack data.
   */
  public static StackedBarChartsItem createStackedBarCharts(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {

    switch (kidsRadarDataItemContext) {
      case KJP -> {
        return createKjpData(coreCaseDataByGroups);
      }
      case RSV -> {
        return createRsvData(coreCaseDataByGroups);
      }
    }
    // Return the final result containing all the bar chart data
    return null;
  }

  private static StackedBarChartsItem createKjpData(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {
    log.debug("Started KiraCumulativeAgeDisorders.createKjpData");
    Instant startTimer = TimerTools.startTimer(); // Start the execution timer

    StackedBarChartsItem result = new StackedBarChartsItem();

    // Set chart names from the disease group keys
    result.setCharts(new ArrayList<>(coreCaseDataByGroups.keySet()));

    // Initialize lists to store the bars and stacks
    List<List<String>> barsOutput = new ArrayList<>();
    List<List<String>> stacksOutput = new ArrayList<>();
    coreCaseDataByGroups.forEach(
        (group, values) -> {
          barsOutput.add(KiraAgeKjpCluster.BARS);
          stacksOutput.add(stacksKjp);
        });
    result.setBars(barsOutput);
    result.setStacks(stacksOutput);

    // Initialize the dataList that will store chart values
    List<List<List<? extends Number>>> dataList = new ArrayList<>();

    // Map to hold total facility encounters per age group (used for stacks)
    Map<KiraAgeKjpCluster, Set<String>> facilityEncountersByAgeGroup =
        getCasesWithAgeCluster(coreCaseDataByGroups.values(), KiraAgeKjpCluster.values());

    // Process each disease group and calculate data per age group
    coreCaseDataByGroups.forEach(
        (group, cases) -> {
          // A list to hold results for each disease group by age
          List<List<? extends Number>> resultByDisease =
              facilityEncountersByAgeGroup.entrySet().stream()
                  .map(
                      entry -> {
                        KiraAgeKjpCluster ageGroup = entry.getKey();
                        Set<String> allCasesInAgeGroup = entry.getValue();

                        // Filter cases in this disease group that belong to the current age group
                        Set<String> casesInAgeGroup =
                            allCasesInAgeGroup.stream()
                                .filter(cases::containsKey)
                                .collect(Collectors.toSet());

                        // Log the number of matching cases for debugging
                        log.trace(
                            "{} cases in age cluster {} : {}/{}",
                            group,
                            ageGroup,
                            casesInAgeGroup.size(),
                            allCasesInAgeGroup.size());

                        // Create a list with:
                        // 1. The count of cases in the current disease group and age group
                        // 2. The remaining cases in the age group not belonging to this disease
                        // group
                        return Arrays.asList(
                            casesInAgeGroup.size(),
                            (Number) (allCasesInAgeGroup.size() - casesInAgeGroup.size()));
                      })
                  .collect(Collectors.toList());

          // Add the collected data for this disease group to the main dataList
          dataList.add(resultByDisease);
        });
    // Set the populated data list into the result object
    result.setValues(dataList);

    // Stop the timer and log the execution time
    TimerTools.stopTimerAndLog(startTimer, "Finished KiraCumulativeAgeDisorders.createKjpData");
    return result;
  }

  private static StackedBarChartsItem createRsvData(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups) {
    log.debug("Started KiraCumulativeAgeDisorders.createRsvData");
    Instant startTimer = TimerTools.startTimer(); // Start the execution timer

    StackedBarChartsItem result = new StackedBarChartsItem();

    // Set chart names from the disease group keys
    result.setCharts(List.of(RSV_DIAGNOSES_ALL));

    // Initialize lists to store the bars and stacks
    List<List<String>> barsOutput = List.of(KiraAgeRsvCluster.BARS);
    result.setBars(barsOutput);
    result.setStacks(List.of(new ArrayList<>(coreCaseDataByGroups.keySet())));

    // Initialize the dataList that will store chart values
    List<List<List<? extends Number>>> dataList = new ArrayList<>();

    // Map to hold total facility encounters per age group (used for stacks)
    Map<KiraAgeRsvCluster, Set<String>> facilityEncountersByAgeGroup =
        getCasesWithAgeCluster(coreCaseDataByGroups.values(), KiraAgeRsvCluster.values());

    // A list to hold results for each disease group by age
    List<List<? extends Number>> resultByDisease =
        facilityEncountersByAgeGroup.entrySet().stream()
            .map(
                entry -> {
                  KiraAgeRsvCluster ageGroup = entry.getKey();
                  Set<String> allCasesInAgeGroup = entry.getValue();
                  List<Integer> resultSumByAgeAndDisease = new ArrayList<>();

                  coreCaseDataByGroups
                      .keySet()
                      .forEach(
                          group -> {
                            Map<String, CoreCaseData> casesWithCurrentDisease =
                                coreCaseDataByGroups.get(group);
                            // Figure out how many of the disease group that we iterate
                            // through are also in the age group
                            int sum =
                                casesWithCurrentDisease.keySet().stream()
                                    .filter(allCasesInAgeGroup::contains)
                                    .collect(Collectors.toSet())
                                    .size();
                            // Log the number of matching cases for debugging
                            log.trace("{} encounters in age cluster {} : {}", group, ageGroup, sum);
                            resultSumByAgeAndDisease.add(sum);
                          });

                  return resultSumByAgeAndDisease;
                })
            .collect(Collectors.toList());

    // Add the collected data for this disease group to the main dataList
    dataList.add(resultByDisease);

    // Add values to result object
    result.setValues(dataList);

    // Stop the timer and log the execution time
    TimerTools.stopTimerAndLog(startTimer, "Finished KiraCumulativeAgeDisorders.createRsvData");
    return result;
  }

  /**
   * Sorts cases into age clusters based on the provided age groupings and admission age. This
   * method works for different cluster types (e.g., KiraAgeKjpCluster, KiraAgeRsvCluster) by
   * determining whether the cluster is based on years or months.
   *
   * @param <T> The type of age cluster, which must implement the {@link KiraAgeCluster} interface.
   * @param caseDataByGroup A collection of maps where each map contains case data with a facility
   *     encounter ID as the key.
   * @param ageClusters An array of age clusters (e.g., {@link KiraAgeKjpCluster} or {@link
   *     KiraAgeRsvCluster}) used to group the cases.
   * @return A map where the key is an age cluster, and the value is a set of facility encounter IDs
   *     that fall within the cluster's age range.
   */
  private static <T extends KiraAgeCluster> Map<T, Set<String>> getCasesWithAgeCluster(
      Collection<Map<String, CoreCaseData>> caseDataByGroup, T[] ageClusters) {

    Map<T, Set<String>> result = new LinkedHashMap<>();

    // Initialize result set
    for (T ageCluster : ageClusters) {
      result.put(ageCluster, new HashSet<>());
    }

    // Iterate through each case group and sort cases into the appropriate age cluster
    for (Map<String, CoreCaseData> caseData : caseDataByGroup) {
      caseData.forEach(
          (entry, coreCaseData) -> {
            // Check each age cluster and match cases based on their period (years or months)
            for (T ageCluster : ageClusters) {
              if (ageCluster.getPeriod() == KiraAgeCluster.Period.YEARS) {
                int age = coreCaseData.getAgeAtAdmission();
                if (age >= ageCluster.getLowerBorder() && age <= ageCluster.getUpperBorder()) {
                  result.get(ageCluster).add(coreCaseData.getFacilityEncounterId());
                }
              } else if (ageCluster.getPeriod() == KiraAgeCluster.Period.MONTHS) {
                int age = coreCaseData.getAgeAtAdmissionInMonths();
                if (age >= ageCluster.getLowerBorder() && age <= ageCluster.getUpperBorder()) {
                  result.get(ageCluster).add(coreCaseData.getFacilityEncounterId());
                }
              }
            }
          });
    }
    return result;
  }
}
