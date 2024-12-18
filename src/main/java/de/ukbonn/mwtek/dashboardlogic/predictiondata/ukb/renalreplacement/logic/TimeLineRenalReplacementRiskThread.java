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
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.misc.ValueOperations.getMaxValueLowerThan;

import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.misc.ValueOperations;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeLineRenalReplacementRiskThread implements Runnable {

  private final Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter;
  private final List<CoreBaseDataItem> encounters;
  private final List<Long> timestamps;
  private final Long currentTimestamp;
  private final Long responseStartingTimestamp;
  private final BlockingQueue<Map<Long, Integer>>
      timestampToRenalReplacementTherapyMapBlockingQueue;
  private final BlockingQueue<Map<Long, List<Double>>>
      timestampToRenalReplacementRiskMapBlockingQueue;
  private final BlockingQueue<Map<Long, Set<String>>> timestampToCaseIdForRiskMapBlockingQueue;
  private final BlockingQueue<Map<Long, Set<String>>> timestampToCaseIdForTherapyMapBlockingQueue;

  public TimeLineRenalReplacementRiskThread(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter,
      List<CoreBaseDataItem> encounters,
      List<Long> timestamps,
      Long currentTimestamp,
      Long responseStartingTimestamp,
      BlockingQueue<Map<Long, Integer>> timestampToRenalReplacementTherapyMapBlockingQueue,
      BlockingQueue<Map<Long, List<Double>>> timestampToRenalReplacementRiskMapBlockingQueue,
      BlockingQueue<Map<Long, Set<String>>> timestampToCaseIdForRiskMapBlockingQueue,
      BlockingQueue<Map<Long, Set<String>>> timestampToCaseIdForTherapyMapBlockingQueue) {
    this.mapModelParameter = mapModelParameter;
    this.encounters = encounters;
    this.timestamps = timestamps;
    this.currentTimestamp = currentTimestamp;
    this.responseStartingTimestamp = responseStartingTimestamp;
    this.timestampToRenalReplacementRiskMapBlockingQueue =
        timestampToRenalReplacementRiskMapBlockingQueue;
    this.timestampToRenalReplacementTherapyMapBlockingQueue =
        timestampToRenalReplacementTherapyMapBlockingQueue;
    this.timestampToCaseIdForRiskMapBlockingQueue = timestampToCaseIdForRiskMapBlockingQueue;
    this.timestampToCaseIdForTherapyMapBlockingQueue = timestampToCaseIdForTherapyMapBlockingQueue;
  }

  public void run() {
    long periodTo;
    long periodFrom;
    Double bodyWeight;

    // Debug purpose
    Map<Long, Set<String>> timestampToCaseIdForRiskMap = new HashMap<>();
    Map<Long, Set<String>> timestampToCaseIdForTherapyMap = new HashMap<>();

    Map<Long, List<Double>> timestampToRenalReplacementRiskMap = new HashMap<>();
    Map<Long, Integer> timestampToRenalReplacementTherapyMap = new HashMap<>();
    // Initializing the renalReplacementTherapy counter to 0 and renalReplacementRisk list to an
    // empty list for each timestamp
    for (Long timestamp : this.timestamps) {
      timestampToRenalReplacementTherapyMap.put(timestamp, 0);
      timestampToRenalReplacementRiskMap.put(timestamp, new ArrayList<>());
      // Debug purpose
      timestampToCaseIdForRiskMap.put(timestamp, new HashSet<>());
      timestampToCaseIdForTherapyMap.put(timestamp, new HashSet<>());
    }

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
        log.trace("Missing Body weight for caseId: " + caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      } else {
        bodyWeight = currentBodyWeightItems.get(0).value();
      }

      if (currentCvvhItems != null && !currentCvvhItems.isEmpty()) {
        // For renal replacement risk, we are going from first CVVH till case start date
        // Getting first CVVH
        var firstCVVH = Collections.min(currentCvvhItems);

        // endTimestamp is the dateFrom of first CVVH
        Long endTimestamp = DateTools.dateToUnixTime(firstCVVH.dateFrom());
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

        while (periodTo > startTimestamp) {
          // Getting the maximum timestamp that is lower than the periodTo
          Long maxTimestamp = getMaxValueLowerThan(this.timestamps, periodTo);

          if (maxTimestamp != null) {
            if (maxTimestamp >= startTimestamp && maxTimestamp <= endTimestamp) {
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

              // If the risk is not null then add it to the list in
              // timestampToRenalReplacementRiskMap based on maxTimestamp as key
              if (risk != null) {
                if (Double.isInfinite(risk)) {
                  log.error(
                      logRiskData(
                          "Risk is infinite",
                          periodTo,
                          periodFrom,
                          caseId,
                          firstCreatinine,
                          currentCreatinine,
                          currentUrea,
                          currentLactate,
                          meanUrineOutput,
                          risk));
                } else if (risk < OUTLIER_BOTTOM || risk > OUTLIER_TOP) {
                  log.error(
                      logRiskData(
                          "Outlier found",
                          periodTo,
                          periodFrom,
                          caseId,
                          firstCreatinine,
                          currentCreatinine,
                          currentUrea,
                          currentLactate,
                          meanUrineOutput,
                          risk));
                }
                List<Double> existingList = timestampToRenalReplacementRiskMap.get(maxTimestamp);
                // Debug purpose
                Set<String> existingCaseIdSet = timestampToCaseIdForRiskMap.get(maxTimestamp);
                if (existingList != null && existingCaseIdSet != null) {
                  if (existingCaseIdSet.contains(caseId)) {
                    log.trace(
                        "CaseId "
                            + caseId
                            + " has already been used for Risk calculation against timestamp "
                            + maxTimestamp);
                  } else {
                    log.trace(
                        logRiskData(
                            "Risk=",
                            periodTo,
                            periodFrom,
                            caseId,
                            firstCreatinine,
                            currentCreatinine,
                            currentUrea,
                            currentLactate,
                            meanUrineOutput,
                            risk));
                    existingList.add(risk);
                    timestampToRenalReplacementRiskMap.put(maxTimestamp, existingList);

                    // Debug purpose
                    existingCaseIdSet.add(caseId);
                    timestampToCaseIdForRiskMap.put(maxTimestamp, existingCaseIdSet);
                  }
                } else {
                  log.trace(logNoTimestampFound(periodTo, periodFrom, caseId));
                }
              } else {
                log.trace(
                    "Risk is null for caseId "
                        + caseId
                        + ". periodFrom = "
                        + periodFrom
                        + ", periodTo = "
                        + periodTo);
              }
            }
          } else {
            log.trace(logNoTimestampFound(periodTo, periodFrom, caseId));
          }

          // Going to previous 24 hours period
          periodTo = periodFrom - 1;
          periodFrom = periodTo - ValueOperations.dayInSeconds;
        }

        // For each CVVH renalReplacementTherapy is counted within that CVVH's interval
        for (CoreBaseDataItem cvvhItem : currentCvvhItems) {
          // Getting CVVH start and end timestamps
          Long cvvhItemEndTimestamp = DateTools.dateToUnixTime(cvvhItem.dateTo());
          Long cvvhItemStartTimestamp = DateTools.dateToUnixTime(cvvhItem.dateFrom());

          // If currentCvvhItems start date is older than the timestamp from where we start
          // generating data then update cvvhItemStartTimestamp to that timestamp to save
          // calculation time
          if (cvvhItemStartTimestamp < this.responseStartingTimestamp) {
            cvvhItemStartTimestamp = this.responseStartingTimestamp;
          }
          // Making cvvhItemStartTimestamp as 12:00am of the day it is starting. As 10.10 23:55 -
          // 11.10 00:05 should be counted twice. One for 10.10 and another 11.10 .
          else {
            cvvhItemStartTimestamp = getMaxValueLowerThan(this.timestamps, cvvhItemStartTimestamp);
          }

          // If cvvhItemEndTimestamp is null, it means currentCvvhItems is still in process. Taking
          // current timestamp in that case
          if (cvvhItemEndTimestamp == null) {
            cvvhItemEndTimestamp = this.currentTimestamp;
          }

          // Going backward from endTimestamp till startTimestamp
          periodTo = cvvhItemEndTimestamp;
          periodFrom = cvvhItemEndTimestamp - ValueOperations.dayInSeconds;

          while (periodTo > cvvhItemStartTimestamp) {
            // Getting the maximum timestamp that is lower than the periodTo
            Long maxTimestamp = getMaxValueLowerThan(this.timestamps, periodTo);

            if (maxTimestamp != null) {
              if (maxTimestamp >= cvvhItemStartTimestamp && maxTimestamp <= cvvhItemEndTimestamp) {
                // Debug purpose
                Set<String> existingCaseIdSet = timestampToCaseIdForTherapyMap.get(maxTimestamp);
                if (existingCaseIdSet != null) {
                  if (existingCaseIdSet.contains(caseId)) {
                    log.trace(
                        "CaseId "
                            + caseId
                            + " has already been counted once for therapy against timestamp "
                            + maxTimestamp);
                  } else {
                    // Increasing the renalReplacementTherapy counter for the timestamp
                    timestampToRenalReplacementTherapyMap.compute(
                        maxTimestamp, (k, v) -> Objects.requireNonNullElse(v, 0) + 1);

                    existingCaseIdSet.add(caseId);
                    timestampToCaseIdForTherapyMap.put(maxTimestamp, existingCaseIdSet);
                  }
                } else {
                  log.trace(logNoTimeStampFound(periodTo, periodFrom, caseId));
                }
              }
            } else {
              log.trace(logNoTimeStampFound(periodTo, periodFrom, caseId));
            }

            // Going to previous 24 hours period
            periodTo = periodFrom - 1;
            periodFrom = periodTo - ValueOperations.dayInSeconds;
          }
        }
      }
      // If there is no CVVH for a case then look for admission date
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

          // If releaseTimestamp is null, it means currently in ICU. Taking current timestamp in
          // that case
          if (releaseTimestamp == null) {
            releaseTimestamp = this.currentTimestamp;
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
            // Getting the maximum timestamp that is lower than the periodFrom
            Long maxTimestamp = getMaxValueLowerThan(this.timestamps, periodTo);

            if (maxTimestamp != null) {
              if (maxTimestamp >= admissionTimestamp && maxTimestamp <= releaseTimestamp) {
                // Calculating renal replacement risk
                Double currentCreatinine =
                    ValueOperations.getClosestValueToMid(
                        currentCreatineItems, periodFrom, periodTo);
                Double currentUrea =
                    ValueOperations.getClosestValueToMid(currentUreaItems, periodFrom, periodTo);
                Double currentLactate =
                    ValueOperations.getLatestValueInPeriod(
                        currentLactateItems, periodFrom, periodTo);
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
                // If the risk is not null then add it to the list in
                // timestampToRenalReplacementRiskMap based on maxTimestamp as key
                if (risk != null) {
                  if (Double.isInfinite(risk)) {
                    log.error(
                        logRiskData(
                            "Risk is infinite ",
                            periodTo,
                            periodFrom,
                            caseId,
                            firstCreatinine,
                            currentCreatinine,
                            currentUrea,
                            currentLactate,
                            meanUrineOutput,
                            risk));
                  } else if (risk < OUTLIER_BOTTOM || risk > OUTLIER_TOP) {
                    log.error(
                        logRiskData(
                            "Outlier found",
                            periodTo,
                            periodFrom,
                            caseId,
                            firstCreatinine,
                            currentCreatinine,
                            currentUrea,
                            currentLactate,
                            meanUrineOutput,
                            risk));
                  }
                  List<Double> existingList = timestampToRenalReplacementRiskMap.get(maxTimestamp);
                  // Debug purpose
                  Set<String> existingCaseIdSet = timestampToCaseIdForRiskMap.get(maxTimestamp);
                  if (existingList != null && existingCaseIdSet != null) {
                    if (existingCaseIdSet.contains(caseId)) {
                      log.trace(
                          "CaseId "
                              + caseId
                              + " has already been used for Risk calculation against timestamp "
                              + maxTimestamp);
                    } else {
                      log.trace(
                          "Risk = "
                              + risk
                              + " for caseId = "
                              + caseId
                              + ". periodFrom = "
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
                              + meanUrineOutput);
                      existingList.add(risk);
                      timestampToRenalReplacementRiskMap.put(maxTimestamp, existingList);

                      // Debug pupose
                      existingCaseIdSet.add(caseId);
                      timestampToCaseIdForRiskMap.put(maxTimestamp, existingCaseIdSet);
                    }
                  } else {
                    log.trace(logNoTimestampFound(periodTo, periodFrom, caseId));
                  }
                } else {
                  log.trace(
                      "Risk is null for caseId "
                          + caseId
                          + ". periodFrom = "
                          + periodFrom
                          + ", periodTo = "
                          + periodTo);
                }
              }
            } else {
              log.trace(logNoTimestampFound(periodTo, periodFrom, caseId));
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
      this.timestampToRenalReplacementRiskMapBlockingQueue.put(timestampToRenalReplacementRiskMap);
      this.timestampToRenalReplacementTherapyMapBlockingQueue.put(
          timestampToRenalReplacementTherapyMap);
      // Debug purpose
      this.timestampToCaseIdForRiskMapBlockingQueue.put(timestampToCaseIdForRiskMap);
      this.timestampToCaseIdForTherapyMapBlockingQueue.put(timestampToCaseIdForTherapyMap);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static String logNoTimeStampFound(long periodTo, long periodFrom, String caseId) {
    return "No timestamp found to link the therapy counter for caseId "
        + caseId
        + ". periodFrom = "
        + periodFrom
        + ", periodTo = "
        + periodTo;
  }

  private static String logNoTimestampFound(long periodTo, long periodFrom, String caseId) {
    return "No timestamp found to link the risk list for caseId "
        + caseId
        + ". periodFrom = "
        + periodFrom
        + ", periodTo = "
        + periodTo;
  }

  protected static String logRiskData(
      String context,
      long periodTo,
      long periodFrom,
      String caseId,
      Double firstCreatinine,
      Double currentCreatinine,
      Double currentUrea,
      Double currentLactate,
      Double meanUrineOutput,
      Double risk) {
    return context
        + " for caseId="
        + caseId
        + ". Risk="
        + risk
        + ", periodFrom= "
        + periodFrom
        + ", periodTo= "
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
