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
package de.ukbonn.mwtek.dashboardlogic;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.ICD_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_DISCHARGEDIAGS_COHORTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_RECRUITMENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DISCHARGEDIAGS_COHORTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_RECRUITMENT;
import static de.ukbonn.mwtek.dashboardlogic.logic.CohortLogic.getCohort1;
import static de.ukbonn.mwtek.dashboardlogic.logic.CohortLogic.getCohort2;
import static de.ukbonn.mwtek.dashboardlogic.logic.CohortLogic.getCohort3;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.OPS;
import static de.ukbonn.mwtek.utilities.generic.time.DateTools.calcYearsBetweenDates;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.current.AcribisCurrentDischargeDiags;
import de.ukbonn.mwtek.dashboardlogic.logic.timeline.AcribisTimelineDischargeDiags;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.DataBuilder;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Coding;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the json specification with
 * acribis context are generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class AcribisDataItemGenerator extends DataItemGenerator {

  List<UkbConsent> consents;

  /** Initialization of the Acribis related fhir resource lists. */
  public AcribisDataItemGenerator(
      List<UkbConsent> consents,
      List<UkbCondition> ukbConditions,
      List<UkbPatient> ukbPatients,
      List<UkbEncounter> ukbEncounters,
      List<UkbProcedure> ukbProcedures) {
    super(ukbConditions, null, ukbPatients, ukbEncounters, ukbProcedures, null);
    this.consents = consents;
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems Map with data items to be excluded from the output (e.g.
   *     `acr.current.recruitment`)
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *     codes or procedure codes.
   * @return List with all the {@link DiseaseDataItem data items} that are defined in the corona
   *     dashboard JSON specification
   */
  @SuppressWarnings("unused")
  public List<DiseaseDataItem> getDataItems(
      Map<String, Boolean> mapExcludeDataItems,
      VariantSettings variantSettings,
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      DataItemContext dataItemContext,
      GlobalConfiguration globalConfiguration) {
    List<DiseaseDataItem> currentDataList = new ArrayList<>();
    if (mapExcludeDataItems == null) {
      mapExcludeDataItems = new HashMap<>();
    }
    boolean debug = globalConfiguration.getDebug();

    // If there are resources with unfilled mandatory attributes, report them immediately (may give
    // partially reduced result sets)
    //    reportMissingFields(encounters);

    // The valid cases in our use case must have agreed to three specific fields
    consents = getValidConsents(consents);

    DiseaseDataItem cd;
    // acr.current.recruitment
    String currentRecruitmentLabel = determineLabel(ACRIBIS, CURRENT_RECRUITMENT);
    if (isItemNotExcluded(mapExcludeDataItems, currentRecruitmentLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              currentRecruitmentLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder().consents(consents).buildCurrentRecruitment()));
    }
    // acr.timeline.recruitment
    String timelineRecruitmentLabel = determineLabel(ACRIBIS, TIMELINE_RECRUITMENT);
    if (isItemNotExcluded(mapExcludeDataItems, timelineRecruitmentLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              timelineRecruitmentLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .consents(consents)
                  .dataItemContext(ACRIBIS)
                  .buildTimelineRecruitmentMap()));
    }

    // To determine the cohorts, all of these lists needs to have entry
    if (!procedures.isEmpty()
        && !conditions.isEmpty()
        && !encounters.isEmpty()
        && !patients.isEmpty()) {

      // Filter all patients that were younger than 18 years old when they gave consent
      List<String> pidsAdults = filterPatientsByAge(consents, patients);
      Map<String, Set<String>> pidDischargeDiagnosisList =
          getDischargeDiagnosisList(pidsAdults, conditions, encounters);
      Map<String, Set<String>> pidOpsList = getOpsList(pidsAdults, procedures);
      PidTimestampCohortMap cohort1ByTimestamp = getCohort1(pidDischargeDiagnosisList, consents);
      PidTimestampCohortMap cohort2ByTimestamp = getCohort2(pidDischargeDiagnosisList, consents);
      PidTimestampCohortMap cohort3ByTimestamp =
          getCohort3(pidDischargeDiagnosisList, pidOpsList, consents);

      logPatientsWithoutCohort(
          pidsAdults, cohort1ByTimestamp, cohort2ByTimestamp, cohort3ByTimestamp);

      // acr.current.dischargediags.kohorts
      String currentDischargeDiagsLabel = determineLabel(ACRIBIS, CURRENT_DISCHARGEDIAGS_COHORTS);
      if (isItemNotExcluded(mapExcludeDataItems, currentDischargeDiagsLabel, false)) {
        var dataItem =
            new AcribisCurrentDischargeDiags(
                cohort1ByTimestamp, cohort2ByTimestamp, cohort3ByTimestamp);
        currentDataList.add(
            new DiseaseDataItem(
                currentDischargeDiagsLabel,
                ITEMTYPE_STACKED_BAR_CHARTS,
                dataItem.createStackedBarCharts()));
        if (debug) {
          currentDataList.add(
              new DiseaseDataItem(
                  addDebugLabel(currentDischargeDiagsLabel),
                  ITEMTYPE_DEBUG,
                  dataItem.getDebugData()));
        }
      }

      String timelineDischargeDiagsLabel = determineLabel(ACRIBIS, TIMELINE_DISCHARGEDIAGS_COHORTS);
      if (isItemNotExcluded(mapExcludeDataItems, timelineDischargeDiagsLabel, false)) {
        currentDataList.add(
            new DiseaseDataItem(
                timelineDischargeDiagsLabel,
                ITEMTYPE_LIST,
                new AcribisTimelineDischargeDiags()
                    .generateDailyCohortTimeline(
                        cohort1ByTimestamp, cohort2ByTimestamp, cohort3ByTimestamp)));
      }
    } else {
      log.warn(
          "Unable to create cohort data items since either procedures/conditions/encounter/patient"
              + " resources were not found.");
    }

    return currentDataList;
  }

  private void logPatientsWithoutCohort(
      List<String> pidsAdults,
      PidTimestampCohortMap cohort1ByTimestamp,
      PidTimestampCohortMap cohort2ByTimestamp,
      PidTimestampCohortMap cohort3ByTimestamp) {

    Set<String> allCohortPids = new HashSet<>();
    allCohortPids.addAll(cohort1ByTimestamp.keySet());
    allCohortPids.addAll(cohort2ByTimestamp.keySet());
    allCohortPids.addAll(cohort3ByTimestamp.keySet());

    pidsAdults.stream()
        .filter(pid -> !allCohortPids.contains(pid))
        .forEach(pid -> log.debug("Patient without cohort: {}", pid));
  }

  /**
   * Returns a map of adult patient IDs to their associated discharge diagnosis ICD codes.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Filters encounters to only those involving adult patients.
   *   <li>Collects all referenced discharge diagnosis condition IDs.
   *   <li>Filters conditions by these IDs.
   *   <li>Extracts all ICD codes (not just the first) from matching conditions.
   *   <li>Groups them per patient ID.
   * </ol>
   *
   * @param pidsAdults list of adult patient IDs
   * @param conditions list of all condition resources
   * @param encounters list of all encounter resources
   * @return a map from patient ID to a list of all their ICD diagnosis codes
   */
  private Map<String, Set<String>> getDischargeDiagnosisList(
      List<String> pidsAdults, List<UkbCondition> conditions, List<UkbEncounter> encounters) {

    Set<String> adultPidSet = new HashSet<>(pidsAdults);

    // Step 1: Filter encounters of adult patients
    Set<String> dischargeDiagnosisIds =
        encounters.stream()
            .filter(e -> adultPidSet.contains(e.getPatientId()))
            .flatMap(e -> e.getDischargeDiagnosisReferenceIds().stream())
            .collect(Collectors.toSet());

    // Step 2: Filter conditions that match discharge diagnosis IDs
    List<UkbCondition> relevantConditions =
        conditions.stream().filter(c -> dischargeDiagnosisIds.contains(c.getId())).toList();

    // Step 3: Build patientId → list of ICD codes
    return pidsAdults.parallelStream()
        .collect(
            Collectors.toMap(
                pid -> pid,
                pid ->
                    relevantConditions.stream()
                        .filter(c -> pid.equals(c.getPatientId()))
                        .flatMap(c -> c.getCode().getCoding().stream())
                        .filter(coding -> ICD_SYSTEM.getValue().equals(coding.getSystem()))
                        .map(Coding::getCode)
                        .collect(Collectors.toSet())));
  }

  private Map<String, Set<String>> getOpsList(
      Collection<String> pidsAdults, Collection<UkbProcedure> procedures) {

    Set<String> adultPidSet = new HashSet<>(pidsAdults);

    // Nur relevante OPS-Codes der erwachsenen Patienten extrahieren
    return procedures.stream()
        .filter(p -> adultPidSet.contains(p.getPatientId()))
        .flatMap(
            p ->
                p.getCode().getCoding().stream()
                    .filter(coding -> OPS.equals(coding.getSystem()))
                    .map(coding -> Map.entry(p.getPatientId(), coding.getCode())))
        .collect(
            Collectors.groupingBy(
                Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
  }

  /**
   * Filters patients by checking whether they were under 18 years old at the time of signing any
   * associated consent. Only patients with no consents or with all consents signed at age 18 or
   * older are included.
   *
   * <p>Logs any consents where the patient was under 18 at the time of signing.
   *
   * @param consents List of consent forms
   * @param patients List of patients
   * @return List of patient IDs that meet the criteria
   */
  public List<String> filterPatientsByAge(List<UkbConsent> consents, List<UkbPatient> patients) {
    // Group consents by patient ID (only valid ones)
    Map<String, List<UkbConsent>> consentMap =
        consents.stream()
            .filter(UkbConsent::isAcribisConsentAllowed)
            .filter(c -> c.getPatientId() != null && c.getAcribisPermitStartDate() != null)
            .collect(Collectors.groupingBy(UkbConsent::getPatientId));

    return patients.parallelStream()
        .filter(
            p -> {
              String patientId = p.getId();
              Date birthDate = p.getBirthDate();

              List<UkbConsent> patientConsents = consentMap.get(patientId);
              // Exclude patients without a valid main consent
              if (patientConsents == null || patientConsents.isEmpty()) {
                return false;
              }

              boolean allConsentsValid = true;

              for (UkbConsent consent : patientConsents) {
                Date consentDate = consent.getAcribisPermitStartDate();
                if (consentDate == null) {
                  log.warn("WARN: Consent date is null for consentId: {}", consent.getId());
                  continue;
                }

                int ageAtConsent = calcYearsBetweenDates(birthDate, consentDate);
                if (ageAtConsent < 18) {
                  allConsentsValid = false;
                  log.debug(
                      "PatientId {} was under 18 ({} years) at consentId {} (Date: {})",
                      patientId,
                      ageAtConsent,
                      consent.getId(),
                      consentDate);
                }
              }
              return allConsentsValid;
            })
        .map(UkbPatient::getId)
        .collect(Collectors.toList());
  }

  private List<UkbConsent> filterConsentByValidity(
      List<UkbConsent> ukbConsents, Predicate<UkbConsent> consentFilter) {
    return ukbConsents.parallelStream()
        .filter(UkbConsent::isPrivacyPolicyDocumentAndMiiConsentCategory)
        .filter(consentFilter)
        .toList();
  }

  private List<UkbConsent> filterConsentByValidity(List<UkbConsent> ukbConsents) {
    return ukbConsents.stream().filter(UkbConsent::isAcribisConsentAllowed).toList();
  }

  /**
   * Filters the list of Acribis consents, keeping only those with a valid PatientId in both the
   * provided lists of valid PatDataUsage and valid Recontacting consents. Logs the number of
   * removed consents and provides an example of a PatientId that was removed.
   *
   * @param validAcribisConsents The list of Acribis consents to be filtered.
   * @param validPatDataUsage The list of valid consents for PatDataUsage.
   * @param validRecontacting The list of valid consents for Recontacting.
   * @return A filtered list of Acribis consents that have valid PatientIds in both PatDataUsage and
   *     Recontacting consents.
   */
  public List<UkbConsent> filterAcribisConsentWithValidMainConsent(
      List<UkbConsent> validAcribisConsents,
      List<UkbConsent> validPatDataUsage,
      List<UkbConsent> validRecontacting) {

    // Extract patient IDs from validPatDataUsage and validRecontacting
    Set<String> patDataUsageIds = extractPatientIds(validPatDataUsage);
    Set<String> recontactingIds = extractPatientIds(validRecontacting);

    // Save initial size before filtering
    int beforeFilterCount = validAcribisConsents.size();

    // Find an example PatientId that would be removed by this filter
    Optional<String> exampleRemovedPatientId =
        validAcribisConsents.stream()
            .filter(
                c ->
                    !(patDataUsageIds.contains(c.getPatientId())
                        && recontactingIds.contains(c.getPatientId())))
            .map(UkbConsent::getPatientId)
            .findFirst();

    // Filter validAcribisConsents: keep only if PatientId exists in both patDataUsageIds and
    // recontactingIds
    List<UkbConsent> filteredConsents =
        validAcribisConsents.stream()
            .filter(
                c ->
                    patDataUsageIds.contains(c.getPatientId())
                        && recontactingIds.contains(c.getPatientId()))
            .collect(Collectors.toList());

    // Log the number of removed entries and optionally an example PatientId
    int removedCount = beforeFilterCount - filteredConsents.size();
    if (removedCount > 0) {
      if (exampleRemovedPatientId.isPresent()) {
        log.info(
            "{} consents were removed because no PatDataUsage or Recontacting consent was found."
                + " Example PatientId: {}",
            removedCount,
            exampleRemovedPatientId.get());
      } else {
        log.info(
            "{} consents were removed because no PatDataUsage or Recontacting consent was found.",
            removedCount);
      }
    } else {
      log.info("No consents had to be removed.");
    }

    return filteredConsents;
  }

  private static Set<String> extractPatientIds(List<UkbConsent> validPatDataUsage) {
    return validPatDataUsage.stream().map(UkbConsent::getPatientId).collect(Collectors.toSet());
  }

  private List<UkbConsent> getValidConsents(List<UkbConsent> consents) {
    List<UkbConsent> acribis =
        filterConsentByValidity(consents, UkbConsent::isAcribisConsentAllowed);
    List<UkbConsent> patData = filterConsentByValidity(consents, UkbConsent::isPatDataUsageAllowed);
    List<UkbConsent> recontact =
        filterConsentByValidity(consents, UkbConsent::isRecontactingAllowed);
    // Determine all consent forms that have agreed to all 3 fields.
    return filterAcribisConsentWithValidMainConsent(acribis, patData, recontact);
  }
}
