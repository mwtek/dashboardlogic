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
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CumulativeRenalReplacementData;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.ROCItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CumulativeRenalReplacementRiskROC {

  public static CumulativeRenalReplacementData createData(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter)
      throws InterruptedException {
    // Dates in the response json is starting from this timestamp
    Long responseStartingTimestamp = getDatesOutputList(DataItemContext.COVID).get(0);
    // Get all the encounters
    List<CoreBaseDataItem> encounters = mapModelParameter.get(ENCOUNTER);
    /*
    Consider only the encounters that are released after responseStartingTimestamp
    */
    encounters =
        encounters.stream()
            .filter(
                o ->
                    o.dateTo() != null
                        && DateTools.dateToUnixTime(o.dateTo()) >= responseStartingTimestamp)
            .toList();
    // Getting number of cores available in the machine
    int numCores = Runtime.getRuntime().availableProcessors();

    // Concurrent variable to access data among different threads
    BlockingQueue<List<ROCItem>> rocItemsBlockingQueue = new LinkedBlockingQueue<>();

    int numberOfEncounters = encounters.size();
    int sublistSize = (int) Math.ceil((double) numberOfEncounters / numCores);

    int startIndex = 0;
    for (int i = 0; i < numCores; i++) {
      int endIndex = Math.min(startIndex + sublistSize, numberOfEncounters);
      List<CoreBaseDataItem> encounterSublist = encounters.subList(startIndex, endIndex);
      startIndex = endIndex;

      Thread thread =
          new Thread(
              new CumulativeRenalReplacementRiskROCThread(
                  mapModelParameter,
                  encounterSublist,
                  responseStartingTimestamp,
                  rocItemsBlockingQueue));
      thread.start();
    }

    List<ROCItem> rocItemsFromAllThreads = new ArrayList<>();
    // Wait for all threads to finish and collect the results
    for (int i = 0; i < numCores; i++) {
      rocItemsFromAllThreads.addAll(rocItemsBlockingQueue.take());
    }

    CumulativeRenalReplacementData cumulativeRenalReplacementData =
        new CumulativeRenalReplacementData();
    for (ROCItem rocItem : rocItemsFromAllThreads) {
      cumulativeRenalReplacementData.Renalreplacementrisk_roc.add(rocItem.getROCItem());
    }

    return cumulativeRenalReplacementData;
  }
}
