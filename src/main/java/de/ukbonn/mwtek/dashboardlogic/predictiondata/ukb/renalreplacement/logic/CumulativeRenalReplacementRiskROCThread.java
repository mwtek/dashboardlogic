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

import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskConstants.OUTLIER_BOTTOM;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskConstants.OUTLIER_TOP;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.BODY_WEIGHT;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.CREATININE;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.EPISODES;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.LACTATE;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.START_REPLACEMENT;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.UREA;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.URINE_OUTPUT;

import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.misc.ValueOperations;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.ROCItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CumulativeRenalReplacementRiskROCThread implements Runnable {

  private final Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter;
  private final List<CoreBaseDataItem> encounters;
  private final Long responseStartingTimestamp;
  private final BlockingQueue<List<ROCItem>> rocItemsBlockingQueue;

  public CumulativeRenalReplacementRiskROCThread(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter,
      List<CoreBaseDataItem> encounters,
      Long responseStartingTimestamp,
      BlockingQueue<List<ROCItem>> rocItemsBlockingQueue) {
    this.mapModelParameter = mapModelParameter;
    this.encounters = encounters;
    this.responseStartingTimestamp = responseStartingTimestamp;
    this.rocItemsBlockingQueue = rocItemsBlockingQueue;
  }

  public void run() {
    long periodTo;
    long periodFrom;
    Double bodyWeight;

    List<ROCItem> rocItems = new ArrayList<>();

    Map<String, List<CoreBaseDataItem>> ureaByCaseId =
        mapModelParameter.get(UREA).stream()
            .collect(Collectors.groupingBy(CoreBaseDataItem::hisCaseId));

    Map<String, List<CoreBaseDataItem>> lactatesByCaseId =
        mapModelParameter.get(LACTATE).stream()
            .collect(Collectors.groupingBy(CoreBaseDataItem::hisCaseId));

    Map<String, List<CoreBaseDataItem>> urineOutputsByCaseId =
        mapModelParameter.get(URINE_OUTPUT).stream()
            .collect(Collectors.groupingBy(CoreBaseDataItem::hisCaseId));

    Map<String, List<CoreBaseDataItem>> renalReplacementsByCaseId =
        mapModelParameter.get(START_REPLACEMENT).stream()
            .collect(Collectors.groupingBy(CoreBaseDataItem::hisCaseId));

    Map<String, List<CoreBaseDataItem>> bodyWeightByCaseId =
        mapModelParameter.get(BODY_WEIGHT).stream()
            .collect(Collectors.groupingBy(CoreBaseDataItem::hisCaseId));

    Map<String, List<CoreBaseDataItem>> creatineByCaseIds =
        mapModelParameter.get(CREATININE).stream()
            .collect(Collectors.groupingBy(CoreBaseDataItem::hisCaseId));

    for (CoreBaseDataItem encounter : this.encounters) {
      String caseId = encounter.hisCaseId();
      log.trace("START: calculation for caseId: {}", caseId);

      List<CoreBaseDataItem> currentCvvhItems =
          renalReplacementsByCaseId.getOrDefault(caseId, null);
      List<CoreBaseDataItem> currentUrineOutputItems =
          urineOutputsByCaseId.getOrDefault(caseId, null);
      List<CoreBaseDataItem> currentLactateItems = lactatesByCaseId.getOrDefault(caseId, null);
      List<CoreBaseDataItem> currentUreaItems = ureaByCaseId.getOrDefault(caseId, null);
      List<CoreBaseDataItem> currentBodyWeightItems = bodyWeightByCaseId.getOrDefault(caseId, null);
      List<CoreBaseDataItem> currentCreatineItems = creatineByCaseIds.getOrDefault(caseId, null);

      // Getting the first ever recorded Creatinine for this case. Dependent on case. Not on
      // episodes.
      Double firstCreatinine = ValueOperations.getFirstValueInPeriod(currentCreatineItems);
      // If null value is found then risk can't be calculated. So skipping further steps for this
      // case.
      if (firstCreatinine == null) {
        log.trace("Missing Creatinine for caseId: {}", caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      }

      // If no bodyWeight is found then risk can't be calculated. So skipping further steps for this
      // case.
      if (currentBodyWeightItems == null || currentBodyWeightItems.isEmpty()) {
        log.trace("Missing Body weight for caseId: {}", caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      } else {
        bodyWeight = currentBodyWeightItems.get(0).value();
      }

      if (currentCvvhItems != null && !currentCvvhItems.isEmpty()) {
        // For renal replacement risk, we are going from 72h earlier than first CVVH till case start
        // date
        // Getting first CVVH
        var firstCVVH = Collections.min(currentCvvhItems);

        // endTimestamp is 72h earlier than the dateFrom of first CVVH
        long endTimestamp =
            DateTools.dateToUnixTime(firstCVVH.dateFrom()) - (3 * ValueOperations.dayInSeconds);
        // startTimestamp is the dateFrom of encounter
        Long startTimestamp = DateTools.dateToUnixTime(encounter.dateFrom());
        // If encounter start date is older than the timestamp from where we start generating data
        // then update startTimestamp to that timestamp to save calculation time
        if (startTimestamp < this.responseStartingTimestamp) {
          startTimestamp = this.responseStartingTimestamp;
        }

        // Going backward from endTimestamp till startTimestamp
        periodTo = endTimestamp;
        periodFrom = endTimestamp - ValueOperations.dayInSeconds;

        boolean isFirstROCItem = true;
        while (periodTo > startTimestamp) {
          // Calculating renal replacement risk
          Double currentCreatinine =
              ValueOperations.getClosestValueToMid(currentCreatineItems, periodFrom, periodTo);
          Double currentUrea =
              ValueOperations.getClosestValueToMid(currentUreaItems, periodFrom, periodTo);
          Double currentLactate =
              ValueOperations.getLatestValueInPeriod(currentLactateItems, periodFrom, periodTo);
          Double meanUrineOutput =
              ValueOperations.getMeanUrineValueInPeriod(
                  currentUrineOutputItems, bodyWeight, periodFrom, periodTo);
          Double risk =
              ValueOperations.getDiscriminantValue(
                  currentCreatinine, firstCreatinine, currentUrea, currentLactate, meanUrineOutput);

          // If the risk is not null then add it as a ROC item
          if (risk != null) {
            if (Double.isInfinite(risk)) {
              log.warn(
                  "Risk is {} for {}",
                  "infinite",
                  createLogMessage(
                      caseId,
                      risk,
                      periodFrom,
                      periodTo,
                      currentCreatinine,
                      firstCreatinine,
                      currentUrea,
                      currentLactate,
                      meanUrineOutput));
            } else if (risk < OUTLIER_BOTTOM || risk > OUTLIER_TOP) {
              log.warn(
                  "Outlier found for caseId {} for {}",
                  caseId,
                  createLogMessage(
                      caseId,
                      risk,
                      periodFrom,
                      periodTo,
                      currentCreatinine,
                      firstCreatinine,
                      currentUrea,
                      currentLactate,
                      meanUrineOutput));
            }
            log.trace(
                createLogMessage(
                    caseId,
                    risk,
                    periodFrom,
                    periodTo,
                    currentCreatinine,
                    firstCreatinine,
                    currentUrea,
                    currentLactate,
                    meanUrineOutput));

            ROCItem rocItem;
            if (isFirstROCItem) {
              rocItem = new ROCItem(risk, 1);
              isFirstROCItem = false;
            } else {
              rocItem = new ROCItem(risk, 0);
            }
            rocItems.add(rocItem);
          } else {
            log.trace(
                "Risk is null for caseId "
                    + caseId
                    + ". periodFrom = "
                    + periodFrom
                    + ", periodTo = "
                    + periodTo);
          }

          // Going to previous 24 hours period
          periodTo = periodFrom - 1;
          periodFrom = periodTo - ValueOperations.dayInSeconds;
        }
      }
      // If there is no CVHH for a case then look for admission date
      else {
        // Getting Episodes
        List<CoreBaseDataItem> episodes =
            this.mapModelParameter.get(EPISODES).stream()
                .filter(o -> o.hisCaseId().equals(caseId))
                .toList();
        for (CoreBaseDataItem episode : episodes) {
          // Getting episode start and end timestamps
          Long admissionTimestamp = DateTools.dateToUnixTime(episode.dateFrom());
          Long releaseTimestamp = DateTools.dateToUnixTime(episode.dateTo());

          // For ROC curves we are considering released cases only. If encounter.dateTo()!=null but
          // episode.dateTo()==null that means still open episode for a released case.
          if (releaseTimestamp == null) {
            log.debug("releaseTimestamp is null for episode {} in caseId={}", episode, caseId);
            continue;
          }

          // If admissionTimestamp is older than the timestamp from where we start generating data
          // then update releaseTimestamp to that timestamp to save calculation time
          if (admissionTimestamp < this.responseStartingTimestamp) {
            admissionTimestamp = this.responseStartingTimestamp;
          }

          // Going froward from admissionTimestamp till releaseTimestamp
          periodFrom = admissionTimestamp;
          periodTo = periodFrom + ValueOperations.dayInSeconds;
          while (periodTo < releaseTimestamp) {
            // Calculating renal replacement risk
            Double currentCreatinine =
                ValueOperations.getClosestValueToMid(currentCreatineItems, periodFrom, periodTo);
            Double currentUrea =
                ValueOperations.getClosestValueToMid(currentUreaItems, periodFrom, periodTo);
            Double currentLactate =
                ValueOperations.getLatestValueInPeriod(currentLactateItems, periodFrom, periodTo);
            Double meanUrineOutput =
                ValueOperations.getMeanUrineValueInPeriod(
                    currentUrineOutputItems, bodyWeight, periodFrom, periodTo);
            Double risk =
                ValueOperations.getDiscriminantValue(
                    currentCreatinine,
                    firstCreatinine,
                    currentUrea,
                    currentLactate,
                    meanUrineOutput);

            // If the risk is not null then add it to the list in timestampToRenalReplacementRiskMap
            // based on maxTimestamp as key
            if (risk != null) {
              if (Double.isInfinite(risk)) {
                log.warn(
                    "Risk is infinite for {}",
                    createLogMessage(
                        caseId,
                        risk,
                        periodFrom,
                        periodTo,
                        currentCreatinine,
                        firstCreatinine,
                        currentUrea,
                        currentLactate,
                        meanUrineOutput));
              } else if (risk < OUTLIER_BOTTOM || risk > OUTLIER_TOP) {
                log.warn(
                    "Outlier found for caseId {} for {}",
                    caseId,
                    createLogMessage(
                        caseId,
                        risk,
                        periodFrom,
                        periodTo,
                        currentCreatinine,
                        firstCreatinine,
                        currentUrea,
                        currentLactate,
                        meanUrineOutput));
              }
              log.trace(
                  "Risk = {} for {}",
                  risk,
                  createLogMessage(
                      caseId,
                      risk,
                      periodFrom,
                      periodTo,
                      currentCreatinine,
                      firstCreatinine,
                      currentUrea,
                      currentLactate,
                      meanUrineOutput));

              ROCItem rocItem = new ROCItem(risk, 0);
              rocItems.add(rocItem);

            } else {
              log.trace(
                  "Risk is null for caseId "
                      + caseId
                      + ". periodFrom = "
                      + periodFrom
                      + ", periodTo = "
                      + periodTo);
            }

            // Going to next 24 hours period
            periodFrom = periodTo + 1;
            periodTo = periodFrom + ValueOperations.dayInSeconds;
          }
        }
      }
      log.trace("END: calculation for caseId: " + caseId);
    }

    try {
      // Providing data back to main thread
      this.rocItemsBlockingQueue.put(rocItems);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static String createLogMessage(
      String caseId,
      double risk,
      double periodFrom,
      double periodTo,
      double currentCreatinine,
      double firstCreatinine,
      double currentUrea,
      double currentLactate,
      double meanUrineOutput) {
    return "caseId "
        + caseId
        + ". Risk="
        + risk
        + ", periodFrom = "
        + periodFrom
        + ", periodTo = "
        + periodTo
        + ", currentCreatinine="
        + currentCreatinine
        + ", firstCreatinine="
        + firstCreatinine
        + ", currentUrea="
        + currentUrea
        + ", currentLactate="
        + currentLactate
        + ", meanUrineOutput="
        + meanUrineOutput;
  }
}
