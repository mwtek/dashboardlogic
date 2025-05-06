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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.SUBITEMTYPE_DATE;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.DAY_IN_SECONDS;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListTriple;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem acr.timeline.recruitment}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class TimelineRecruitment extends DashboardDataItemLogic implements TimelineFunctionalities {

  public static final String ACR_RECRUITMENT_CONSENT = "acr_recruitment_consent";
  public static final String ACR_RECRUITMENT_FOLLOWUP = "acr_recruitment_followup";

  /**
   * Creates a TimestampedListTriple that contains the number of deceased patients for each day
   * since Qualifying date
   *
   * @return ListNumberPair Containing dates and number of deceased people
   */
  public TimestampedListTriple createTimelineRecruitment(
      List<UkbConsent> consents, DataItemContext dataItemContext) {
    log.debug("started createTimelineRecruitment");
    Instant startTimer = TimerTools.startTimer();
    LinkedHashMap<Long, Long> dateRecruitmentConsentMap = new LinkedHashMap<>();
    LinkedHashMap<Long, Long> dateRecruitmentFollowUpMap = new LinkedHashMap<>();

    List<Long> recruitmentConsentList;
    List<Long> recruitmentFollowUpList;
    TimestampedListTriple resultTriple = new TimestampedListTriple();

    // Retrieval of the current date
    long currentUnixTime = DateTools.getCurrentUnixTime();
    // Retrieval of the start date
    long tempDateUnix = getKickOffDateInSeconds(dataItemContext);

    try {
      List<Long> consentPermitStartDatesUnix =
          consents.stream()
              // Grouping consents by PatientId
              .collect(Collectors.groupingBy(UkbConsent::getPatientId))
              .values()
              .stream()
              // For each patient's consent list, take the first valid consent date
              .map(
                  patientConsents ->
                      patientConsents.stream()
                          .map(UkbConsent::getAcribisPermitStartDate) // Filter valid dates
                          .findFirst() // Take only the first valid date
                          .map(DateTools::dateToUnixTime)
                          .orElse(null))
              .filter(Objects::nonNull)
              .toList();

      // Loop through each day
      while (tempDateUnix <= currentUnixTime) {
        final long dayStart = tempDateUnix;
        final long dayEnd = tempDateUnix + DAY_IN_SECONDS;
        long count =
            consentPermitStartDatesUnix.stream()
                .filter(timestamp -> timestamp >= dayStart && timestamp < dayEnd)
                .count();

        dateRecruitmentConsentMap.put(dayStart, count);
        dateRecruitmentFollowUpMap.put(dayStart, 0L); // UNSUPPORTED ATM

        tempDateUnix = dayEnd;
      }
      recruitmentConsentList = divideMapValuesToLists(dateRecruitmentConsentMap);
      recruitmentFollowUpList = divideMapValuesToLists(dateRecruitmentFollowUpMap);
      resultTriple =
          new TimestampedListTriple(
              getDatesOutputList(dataItemContext), recruitmentConsentList, recruitmentFollowUpList);
    } catch (Exception e) {
      log.debug("Error is calculating the timeline death: {}", e.getMessage());
    }
    TimerTools.stopTimerAndLog(startTimer, "finished createTimelineRecruitment");
    return resultTriple;
  }

  public Map<String, List<? extends Number>> createTimelineRecruitmentMap(
      TimestampedListTriple timestampedListTriple) {
    Map<String, List<? extends Number>> dataMap = new LinkedHashMap<>();
    dataMap.put(SUBITEMTYPE_DATE, timestampedListTriple.getDate());
    dataMap.put(ACR_RECRUITMENT_CONSENT, timestampedListTriple.getValue1());
    dataMap.put(ACR_RECRUITMENT_FOLLOWUP, timestampedListTriple.getValue2());
    return dataMap;
  }
}
