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
package de.ukbonn.mwtek.dashboardlogic.logic.current;

import static de.ukbonn.mwtek.dashboardlogic.tools.BctModuleRule.getBctModuleRules;
import static de.ukbonn.mwtek.utilities.generic.time.DateTools.getCurrentDateTime;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.tools.BctModuleRule;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem bct.current.consent}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class CurrentConsent extends DashboardDataItemLogic {

  public static final String BCT_MOD_PREFIX = "bct_mod";
  public static final String BCT_MOD_1 = BCT_MOD_PREFIX + "1";
  public static final String BCT_MOD_2 = BCT_MOD_PREFIX + "2";
  public static final String BCT_MOD_3 = BCT_MOD_PREFIX + "3";
  public static final String BCT_MOD_4 = BCT_MOD_PREFIX + "4";
  public static final String BCT_MOD_5 = BCT_MOD_PREFIX + "5";
  public static final String BCT_MOD_6 = BCT_MOD_PREFIX + "6";
  public static final String BCT_MOD_7 = BCT_MOD_PREFIX + "7";
  public static final String BCT_MOD_8 = BCT_MOD_PREFIX + "8";
  public static final String BCT_MOD_9 = BCT_MOD_PREFIX + "9";
  public static final String BCT_NUMBER_PATIENTS = "bct_number_patients";
  public static final String BCT_PROJECT = "bct_project";

  public StackedBarChartsItem<Integer> createStackedBarCharts(Collection<MiiConsent> consents) {

    log.debug("started CurrentConsent.createStackedBarCharts");
    Instant startTimer = TimerTools.startTimer();

    StackedBarChartsItem<Integer> result = new StackedBarChartsItem<>();
    result.setCharts(new ArrayList<>(List.of(BCT_PROJECT)));

    // Generate valid date list using the determined format.
    result.setBars(
        List.of(
            List.of(
                BCT_MOD_1, BCT_MOD_2, BCT_MOD_3, BCT_MOD_4, BCT_MOD_5, BCT_MOD_6, BCT_MOD_7,
                BCT_MOD_8, BCT_MOD_9)));
    result.setStacks(List.of(List.of(BCT_NUMBER_PATIENTS)));

    Date validationDate = getCurrentDateTime();

    List<BctModuleRule> moduleRules = getBctModuleRules();

    Map<String, List<String>> debugData = new LinkedHashMap<>();
    List<List<Integer>> resultList = new ArrayList<>();

    Map<String, List<String>> patientsByModule = new LinkedHashMap<>();
    // Iterate over all the module and their determination rule logic
    for (BctModuleRule rule : moduleRules) {
      List<String> patientIds =
          getPatientIdsByConsent(
              consents, consent -> matchesModuleRule(consent, rule.levelTwoKeys(), validationDate));

      patientsByModule.put(rule.levelOneKey(), patientIds);
    }

    result.setValues(List.of(resultList));
    result.setDebugData(debugData);
    List<String> bctMod1Patients = patientsByModule.getOrDefault(BCT_MOD_1, List.of());
    List<String> bctMod2Patients = patientsByModule.getOrDefault(BCT_MOD_2, List.of());
    List<String> bctMod3Patients = patientsByModule.getOrDefault(BCT_MOD_3, List.of());
    List<String> bctMod4Patients = patientsByModule.getOrDefault(BCT_MOD_4, List.of());
    List<String> bctMod5Patients = patientsByModule.getOrDefault(BCT_MOD_5, List.of());
    List<String> bctMod6Patients = patientsByModule.getOrDefault(BCT_MOD_6, List.of());
    List<String> bctMod7Patients = patientsByModule.getOrDefault(BCT_MOD_7, List.of());
    List<String> bctMod8Patients = patientsByModule.getOrDefault(BCT_MOD_8, List.of());
    List<String> bctMod9Patients = patientsByModule.getOrDefault(BCT_MOD_9, List.of());

    resultList.add(List.of(bctMod1Patients.size()));
    resultList.add(List.of(bctMod2Patients.size()));
    resultList.add(List.of(bctMod3Patients.size()));
    resultList.add(List.of(bctMod4Patients.size()));
    resultList.add(List.of(bctMod5Patients.size()));
    resultList.add(List.of(bctMod6Patients.size()));
    resultList.add(List.of(bctMod7Patients.size()));
    resultList.add(List.of(bctMod8Patients.size()));
    resultList.add(List.of(bctMod9Patients.size()));
    result.setValues(List.of(resultList));

    // Saving ids to generate debug items
    debugData.put(BCT_MOD_1, bctMod1Patients);
    debugData.put(BCT_MOD_2, bctMod2Patients);
    debugData.put(BCT_MOD_3, bctMod3Patients);
    debugData.put(BCT_MOD_4, bctMod4Patients);
    debugData.put(BCT_MOD_5, bctMod5Patients);
    debugData.put(BCT_MOD_6, bctMod6Patients);
    debugData.put(BCT_MOD_7, bctMod7Patients);
    debugData.put(BCT_MOD_8, bctMod8Patients);
    debugData.put(BCT_MOD_9, bctMod9Patients);
    result.setDebugData(debugData);

    TimerTools.stopTimerAndLog(startTimer, "finished CurrentConsent.createStackedBarCharts");
    return result;
  }

  /** Checks whether the consent matches at least one module rule combination. */
  private boolean matchesModuleRule(
      MiiConsent consent, List<List<String>> levelTwoKeys, Date validationDate) {
    // Check all alternative rule combinations
    for (List<String> requiredCodes : levelTwoKeys) {
      // All required codes must match
      boolean matches =
          requiredCodes.stream().allMatch(code -> consent.hasPermitWithCode(code, validationDate));
      // Stop on first matching rule combination
      if (matches) {
        return true;
      }
    }
    return false;
  }

  /** Extracts distinct patient IDs for consents matching the given filter. */
  private static List<String> getPatientIdsByConsent(
      Collection<MiiConsent> consents, Predicate<MiiConsent> consentFilter) {
    return consents.stream()
        .filter(consentFilter)
        .map(MiiConsent::getPatientId)
        .filter(Objects::nonNull)
        .distinct()
        .map(String::valueOf)
        .toList();
  }
}
