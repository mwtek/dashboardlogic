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
import de.ukbonn.mwtek.dashboardlogic.models.ParametersSetItem;
import de.ukbonn.mwtek.dashboardlogic.settings.ProviderConfiguration;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
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
 * This class is used for generating the data item {@link DiseaseDataItem
 * snid.current.recruitment.diz}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class CurrentRecruitmentDiz extends DashboardDataItemLogic {

  public static final String SNID_RECRUITMENT_CONSENT_DIZ = "snd_recruitment_consent";

  public ParametersSetItem<Integer> createParametersSet(
      Collection<MiiConsent> ukbConsents, ProviderConfiguration providerConfiguration) {

    log.debug("started CurrentRecruitmentDiz.createParametersSet");
    Instant startTimer = TimerTools.startTimer();

    ParametersSetItem<Integer> result = new ParametersSetItem<>();
    result.setData(new ArrayList<>(List.of(SNID_RECRUITMENT_CONSENT_DIZ)));
    result.setName(providerConfiguration.getName());

    List<String> consentPatientIds =
        ukbConsents.stream()
            .map(MiiConsent::getPatientId)
            .filter(Objects::nonNull)
            .distinct()
            .map(String::valueOf)
            .toList();

    List<Integer> values = new ArrayList<>();
    values.add(consentPatientIds.size());
    result.setValues(values);

    // Saving ids to generate debug items
    Map<String, List<String>> debugData = new LinkedHashMap<>();
    debugData.put(SNID_RECRUITMENT_CONSENT_DIZ, consentPatientIds);
    result.setDebugData(debugData);

    TimerTools.stopTimerAndLog(startTimer, "finished CurrentRecruitmentDiz.createParametersSet");
    return result;
  }
}
