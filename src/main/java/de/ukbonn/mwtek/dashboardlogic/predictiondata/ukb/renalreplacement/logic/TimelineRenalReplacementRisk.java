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

package de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.logic;

import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.ENCOUNTER;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.TimeLineRenalReplacementData;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimelineRenalReplacementRisk {

  public static TimeLineRenalReplacementData createData(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter)
      throws InterruptedException {
    // Getting timestamps
    List<Long> timestamps = getDatesOutputList(DataItemContext.COVID);
    // Getting current timestamp
    Long currentTimestamp = DateTools.getCurrentUnixTime();
    // Dates in the response json is starting from this timestamp
    Long responseStartingTimestamp = timestamps.get(0);
    // Get all the encounters
    List<CoreBaseDataItem> encounters = mapModelParameter.get(ENCOUNTER);
    // Getting number of cores available in the machine
    int numCores = Runtime.getRuntime().availableProcessors();

    List<Map<Long, Integer>> timestampToRenalReplacementTherapyMapList = new ArrayList<>();
    List<Map<Long, List<Double>>> timestampToRenalReplacementRiskMapList = new ArrayList<>();
    // Debug purpose
    List<Map<Long, Set<String>>> timestampToCaseIdForRiskMapSet = new ArrayList<>();
    List<Map<Long, Set<String>>> timestampToCaseIdForTherapyMapSet = new ArrayList<>();

    // Concurrent variable to access data among different threads
    BlockingQueue<Map<Long, Integer>> timestampToRenalReplacementTherapyMapBlockingQueue =
        new LinkedBlockingQueue<>();
    BlockingQueue<Map<Long, List<Double>>> timestampToRenalReplacementRiskMapBlockingQueue =
        new LinkedBlockingQueue<>();
    // Debug purpose
    BlockingQueue<Map<Long, Set<String>>> timestampToCaseIdForRiskMapBlockingQueue =
        new LinkedBlockingQueue<>();
    BlockingQueue<Map<Long, Set<String>>> timestampToCaseIdForTherapyMapBlockingQueue =
        new LinkedBlockingQueue<>();

    int numberOfEncounters = encounters.size();
    int sublistSize = (int) Math.ceil((double) numberOfEncounters / numCores);

    int startIndex = 0;
    for (int i = 0; i < numCores; i++) {
      int endIndex = Math.min(startIndex + sublistSize, numberOfEncounters);
      List<CoreBaseDataItem> encounterSublist = encounters.subList(startIndex, endIndex);
      startIndex = endIndex;

      Thread thread =
          new Thread(
              new TimeLineRenalReplacementRiskThread(
                  mapModelParameter,
                  encounterSublist,
                  timestamps,
                  currentTimestamp,
                  responseStartingTimestamp,
                  timestampToRenalReplacementTherapyMapBlockingQueue,
                  timestampToRenalReplacementRiskMapBlockingQueue,
                  timestampToCaseIdForRiskMapBlockingQueue,
                  timestampToCaseIdForTherapyMapBlockingQueue));
      thread.start();
    }

    // Wait for all threads to finish and collect the results
    for (int i = 0; i < numCores; i++) {
      timestampToRenalReplacementTherapyMapList.add(
          timestampToRenalReplacementTherapyMapBlockingQueue.take());
      timestampToRenalReplacementRiskMapList.add(
          timestampToRenalReplacementRiskMapBlockingQueue.take());
      // Debug purpose
      timestampToCaseIdForRiskMapSet.add(timestampToCaseIdForRiskMapBlockingQueue.take());
      timestampToCaseIdForTherapyMapSet.add(timestampToCaseIdForTherapyMapBlockingQueue.take());
    }

    TimeLineRenalReplacementData timeLineRenalReplacementData = new TimeLineRenalReplacementData();
    // Dropping the last timestamp from timestamp list
    timestamps.subList(timestamps.size() - 1, timestamps.size()).clear();
    for (Long timestamp : timestamps) {
      // Adding timestamp to the final data
      timeLineRenalReplacementData.date.add(timestamp);

      int renalReplacementTherapyCounter = 0;
      List<Double> renalReplacementRiskList = new ArrayList<>();
      // Debug purpose
      Set<String> caseIdForRiskSet = new HashSet<>();
      Set<String> caseIdForTherapySet = new HashSet<>();
      for (int i = 0; i < numCores; i++) {
        renalReplacementTherapyCounter =
            renalReplacementTherapyCounter
                + timestampToRenalReplacementTherapyMapList.get(i).get(timestamp);
        renalReplacementRiskList.addAll(
            timestampToRenalReplacementRiskMapList.get(i).get(timestamp));
        // Debug purpose
        caseIdForRiskSet.addAll(timestampToCaseIdForRiskMapSet.get(i).get(timestamp));
        caseIdForTherapySet.addAll(timestampToCaseIdForTherapyMapSet.get(i).get(timestamp));
      }

      // Adding therapy for this timestamp to the final data
      timeLineRenalReplacementData.RenalReplacement_therapy.add(renalReplacementTherapyCounter);

      // Adding riskList for this timestamp to the final data
      timeLineRenalReplacementData.RenalReplacement_risk.add(renalReplacementRiskList);

      // Debug purpose
      log.debug(
          "For timestamp "
              + timestamp
              + ", risk data generated by caseId: "
              + caseIdForRiskSet.toString());
      log.debug(
          "For timestamp "
              + timestamp
              + ", therapy data generated by caseId: "
              + caseIdForTherapySet.toString());
    }
    return timeLineRenalReplacementData;
  }
}
