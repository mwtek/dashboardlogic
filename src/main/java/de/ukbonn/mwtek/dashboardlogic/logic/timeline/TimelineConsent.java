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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.BCT;
import static de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.DAY_IN_SECONDS;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;
import static de.ukbonn.mwtek.dashboardlogic.tools.BctModuleRule.MODULE_KEYS;
import static de.ukbonn.mwtek.dashboardlogic.tools.BctModuleRule.getBctModuleRules;
import static de.ukbonn.mwtek.dashboardlogic.tools.BctModuleRule.getModuleIndex;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.PROVISION_CODE_SYSTEM;
import static de.ukbonn.mwtek.utilities.generic.collections.ListTools.toList;
import static org.hl7.fhir.r4.model.Consent.ConsentProvisionType.PERMIT;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.tools.BctModuleRule;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent.ProvisionComponent;

/**
 * Creation of the "timeline.consent" items for bct.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class TimelineConsent extends DashboardDataItemLogic implements TimelineFunctionalities {

  /** Generates daily timeline counts for all BCT modules. */
  public Map<String, List<Long>> generateTimelineConsent(Collection<MiiConsent> consents) {
    log.debug("started TimelineConsent.generateTimelineConsent");
    Instant startTimer = TimerTools.startTimer();
    long currentUnixTime = DateTools.getCurrentUnixTime();
    long startDateUnix = getKickOffDateInSeconds(BCT);
    // Total number of timeline days
    int dayCount = (int) ((currentUnixTime - startDateUnix) / DAY_IN_SECONDS) + 1;

    List<BctModuleRule> moduleRules = getBctModuleRules();
    // Patient ID -> module index -> covered timeline days
    Map<String, BitSet[]> patientModuleDays = new HashMap<>();

    for (MiiConsent consent : consents) {
      if (consent.getPatientId() == null || consent.getProvision() == null) {
        continue;
      }

      String patientId = consent.getPatientId();

      // Build permit coverage once per permit code.
      Map<String, BitSet> permitDaysByCode =
          buildPermitDaysByCode(consent, startDateUnix, currentUnixTime, dayCount);
      // Get/create the module timeline BitSets for the patient that will get aggregated later
      BitSet[] modules =
          patientModuleDays.computeIfAbsent(patientId, ignored -> newModuleBitSets(dayCount));

      for (BctModuleRule rule : moduleRules) {
        int moduleIndex = getModuleIndex(rule.levelOneKey());

        for (List<String> requiredCodes : rule.levelTwoKeys()) {
          // Stores overlapping valid days for all required codes
          BitSet matchingDays = null;

          for (String code : requiredCodes) {
            // Get all valid days [the indizes] for the permit code
            BitSet codeDays = permitDaysByCode.get(code);
            // Stop if the code does not exist or has no valid days
            if (codeDays == null || codeDays.isEmpty()) {
              matchingDays = null;
              break;
            }
            if (matchingDays == null) {
              // Initialize with the first permit code coverage
              matchingDays = (BitSet) codeDays.clone();
            } else {
              // Keep only overlapping valid days
              matchingDays.and(codeDays);
            }
            // Stop directly if no overlapping days remain
            if (matchingDays.isEmpty()) {
              break;
            }
          }
          // Add all matching days to the patient module coverage
          if (matchingDays != null && !matchingDays.isEmpty()) {
            modules[moduleIndex].or(matchingDays);
          }
        }
      }
    }
    // Daily patient counts per module
    long[][] countsByModule = new long[9][dayCount];
    for (BitSet[] modules : patientModuleDays.values()) {
      for (int module = 0; module < modules.length; module++) {
        BitSet days = modules[module];
        // Iterate over all covered timeline days
        for (int day = days.nextSetBit(0); day >= 0; day = days.nextSetBit(day + 1)) {
          // Increment the patient count for the given day
          countsByModule[module][day]++;
        }
      }
    }
    Map<String, List<Long>> resultMap = new LinkedHashMap<>();
    // Iterate module by module and add the counts
    for (String moduleKey : MODULE_KEYS) {
      resultMap.put(moduleKey, toList(countsByModule[getModuleIndex(moduleKey)]));
    }
    resultMap.put(DATE, getDatesOutputList(BCT));

    TimerTools.stopTimerAndLog(startTimer, "finished TimelineConsent.generateTimelineConsent");
    return resultMap;
  }

  /**
   * Builds a day-based BitSet for every permit code contained in the consent.
   *
   * @param consent The consent resource to evaluate
   * @param startDateUnix Timeline start timestamp in seconds
   * @param currentUnixTime Current timestamp in seconds
   * @param dayCount Total number of timeline days
   * @return Map of permit code to covered timeline days
   */
  private Map<String, BitSet> buildPermitDaysByCode(
      MiiConsent consent, long startDateUnix, long currentUnixTime, int dayCount) {

    Map<String, BitSet> result = new HashMap<>();

    consent.getProvision().getProvision().stream()
        .filter(pc -> pc.hasType() && pc.getType().equals(PERMIT))
        .filter(pc -> pc.hasPeriod() && pc.getPeriod().hasStart())
        .filter(ProvisionComponent::hasCode)
        .forEach(
            pc -> {
              // Calculate the first covered timeline day index
              int firstDay =
                  toDayIndex(DateTools.dateToUnixTime(pc.getPeriod().getStart()), startDateUnix);
              // Calculate the last covered timeline day index
              int lastDay =
                  pc.getPeriod().hasEnd()
                      ? toDayIndex(DateTools.dateToUnixTime(pc.getPeriod().getEnd()), startDateUnix)
                      : dayCount - 1;

              // Adjust the range to the current time regions
              firstDay = Math.max(firstDay, 0);
              lastDay = Math.min(lastDay, dayCount - 1);

              // Skip invalid ranges
              if (firstDay > lastDay) {
                return;
              }

              for (CodeableConcept code : pc.getCode()) {
                for (Coding coding : code.getCoding()) {
                  // Ignore codings from other code systems
                  if (!PROVISION_CODE_SYSTEM.equals(coding.getSystem())) {
                    continue;
                  }
                  // Get/create the BitSet for the permit code and mark all covered timeline days
                  result
                      .computeIfAbsent(coding.getCode(), _ -> new BitSet(dayCount))
                      .set(firstDay, lastDay + 1);
                }
              }
            });

    return result;
  }

  /**
   * Converts a unix timestamp into a midnight timeline day index.
   *
   * @param timestamp Timestamp in seconds
   * @param startDateUnix Timeline start midnight timestamp in seconds
   */
  private int toDayIndex(long timestamp, long startDateUnix) {
    return (int) ((timestamp - startDateUnix) / DAY_IN_SECONDS);
  }

  /**
   * Creates one empty BitSet per module.
   *
   * @param dayCount Total number of timeline days
   */
  private BitSet[] newModuleBitSets(int dayCount) {
    BitSet[] result = new BitSet[9];
    for (int i = 0; i < result.length; i++) {
      result[i] = new BitSet(dayCount);
    }
    return result;
  }
}
