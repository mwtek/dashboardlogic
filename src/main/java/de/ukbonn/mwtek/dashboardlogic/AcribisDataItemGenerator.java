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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_RECRUITMENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_RECRUITMENT;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.DataBuilder;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the json specification with
 * acribis context are generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class AcribisDataItemGenerator extends DataItemGenerator {

  List<UkbConsent> ukbConsents;

  /** Initialization of the Acribis related fhir resource lists. */
  public AcribisDataItemGenerator(List<UkbConsent> ukbConsents) {
    super(null, null, null, null, null, null);
    this.ukbConsents = ukbConsents;
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems Map with data items to be excluded from the output (e.g.
   *     `acr.current.recruitment`)
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *     codes or procedure codes.
   * @return List with all the {@link DiseaseDataItem data items} that are defined in the corona
   *     dashboard json specification
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
    List<UkbConsent> validAcribisConsents =
        filterConsentByValidity(ukbConsents, UkbConsent::isAcribisConsentAllowed);
    List<UkbConsent> validPatDataUsage =
        filterConsentByValidity(ukbConsents, UkbConsent::isPatDataUsageAllowed);
    List<UkbConsent> validRecontacting =
        filterConsentByValidity(ukbConsents, UkbConsent::isRecontactingAllowed);
    // Determine all consent forms that have agreed to all 3 fields.
    ukbConsents =
        filterAcribisConsentWithValidMainConsent(
            validAcribisConsents, validPatDataUsage, validRecontacting);

    DiseaseDataItem cd;
    // acr.current.recruitment
    String currentRecruitmentLabel = determineLabel(ACRIBIS, CURRENT_RECRUITMENT);
    if (isItemNotExcluded(mapExcludeDataItems, currentRecruitmentLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              currentRecruitmentLabel,
              ITEMTYPE_STACKED_BAR_CHARTS,
              new DataBuilder().consents(ukbConsents).buildCurrentRecruitment()));
    }
    // acr.timeline.recruitment
    String timelineRecruitmentLabel = determineLabel(ACRIBIS, TIMELINE_RECRUITMENT);
    if (isItemNotExcluded(mapExcludeDataItems, timelineRecruitmentLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              timelineRecruitmentLabel,
              ITEMTYPE_LIST,
              new DataBuilder()
                  .consents(ukbConsents)
                  .dataItemContext(ACRIBIS)
                  .buildTimelineRecruitmentMap()));
    }
    return currentDataList;
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
}
