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

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiQuestionnaireResponse;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem acr.current.recruitment}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class CurrentRecruitment extends DashboardDataItemLogic {

  public static final String ACR_RECRUITMENT_CONSENT = "acr_recruitment_consent";
  public static final String ACR_RECRUITMENT_FOLLOWUP = "acr_recruitment_followup";
  public static final String ACR_NUMBER_PATIENTS = "acr_number_patients";
  public static final String ACR_PROJECT = "acr_project";

  public StackedBarChartsItem<Integer> createStackedBarCharts(
      Collection<MiiConsent> ukbConsents,
      Collection<MiiQuestionnaireResponse> questionnaireResponses) {

    log.debug("started CurrentRecruitment.createStackedBarCharts");
    Instant startTimer = TimerTools.startTimer();

    StackedBarChartsItem<Integer> result = new StackedBarChartsItem<>();
    result.setCharts(new ArrayList<>(List.of(ACR_PROJECT)));

    // Generate valid date list using the determined format.
    result.setBars(List.of(List.of(ACR_RECRUITMENT_CONSENT, ACR_RECRUITMENT_FOLLOWUP)));
    result.setStacks(List.of(List.of(ACR_NUMBER_PATIENTS)));

    List<String> consentPatientIds =
        ukbConsents.stream()
            .map(MiiConsent::getPatientId)
            .filter(Objects::nonNull)
            .distinct()
            .map(String::valueOf)
            .toList();

    List<String> followUpPatientIds =
        questionnaireResponses.stream()
            .map(MiiQuestionnaireResponse::getPatientId)
            .filter(Objects::nonNull)
            .distinct()
            .map(String::valueOf)
            .toList();

    List<List<Integer>> resultList = new ArrayList<>();
    resultList.add(List.of(consentPatientIds.size()));
    resultList.add(List.of(followUpPatientIds.size()));
    result.setValues(List.of(resultList));

    // Saving ids to generate debug items
    Map<String, List<String>> debugData = new LinkedHashMap<>();
    debugData.put(ACR_RECRUITMENT_CONSENT, consentPatientIds);
    debugData.put(ACR_RECRUITMENT_FOLLOWUP, followUpPatientIds);
    result.setDebugData(debugData);

    TimerTools.stopTimerAndLog(startTimer, "finished CurrentRecruitment.createStackedBarCharts");
    return result;
  }
}
