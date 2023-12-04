/*
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement;

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST_NESTED_ARRAYS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST_TUPEL;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskDataItems.CUMULATIVE_RENAL_REPLACEMENT_RISK_ROC;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskDataItems.CURRENT_RENAL_REPLACEMENT_RISK;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskDataItems.TIMELINE_RENAL_REPLACEMENT_RISK;

import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.logic.CumulativeRenalReplacementRiskROC;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.logic.CurrentRenalReplacementRisk;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.logic.TimelineRenalReplacementRisk;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RenalReplacementRiskDataGenerator {

  public static List<CoronaDataItem> generateDataItems(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter)
      throws InterruptedException {

    ArrayList<CoronaDataItem> coronaDataItems = new ArrayList<>();

    Instant timerCurrent = TimerTools.startTimer();
    CoronaDataItem currentRenalReplacement = new CoronaDataItem();
    currentRenalReplacement.setItemname(CURRENT_RENAL_REPLACEMENT_RISK);
    currentRenalReplacement.setItemtype(ITEMTYPE_LIST);
    currentRenalReplacement.setData(CurrentRenalReplacementRisk.createData(mapModelParameter));
    coronaDataItems.add(currentRenalReplacement);
    TimerTools.stopTimerAndLog(timerCurrent, "Calculation of current renal replacement risk");

    Instant timerTimeline = TimerTools.startTimer();
    CoronaDataItem timelineRenalReplacement = new CoronaDataItem();
    timelineRenalReplacement.setItemname(TIMELINE_RENAL_REPLACEMENT_RISK);
    timelineRenalReplacement.setItemtype(ITEMTYPE_LIST_NESTED_ARRAYS);
    timelineRenalReplacement.setData(TimelineRenalReplacementRisk.createData(mapModelParameter));
    coronaDataItems.add(timelineRenalReplacement);
    TimerTools.stopTimerAndLog(timerTimeline, "Calculation of timeline renal replacement risk");

    Instant timerRoc = TimerTools.startTimer();
    CoronaDataItem cumulativeRenalReplacement = new CoronaDataItem();
    cumulativeRenalReplacement.setItemname(CUMULATIVE_RENAL_REPLACEMENT_RISK_ROC);
    cumulativeRenalReplacement.setItemtype(ITEMTYPE_LIST_TUPEL);
    cumulativeRenalReplacement.setData(
        CumulativeRenalReplacementRiskROC.createData(mapModelParameter));
    coronaDataItems.add(cumulativeRenalReplacement);
    TimerTools.stopTimerAndLog(timerRoc,
        "Calculation of cumulative renal replacement risk ROC curve ");

    return coronaDataItems;
  }

}
