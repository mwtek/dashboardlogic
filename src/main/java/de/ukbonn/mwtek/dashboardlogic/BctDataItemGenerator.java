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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.BCT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_CONSENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_CONSENT;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.DataBuilder;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the JSON specification with
 * bct context are generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class BctDataItemGenerator extends DataItemGenerator {

  List<MiiConsent> consents;

  /** Initialization of the BCT related fhir resource lists. */
  public BctDataItemGenerator(List<MiiConsent> consents) {
    super(null, null, null, null, null, null);
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

    // The valid cases in our use case must have agreed to three specific fields
    consents = getValidBroadConsents(consents);

    DiseaseDataItem cd;
    // bct.current.consent
    String currentConsent = determineLabel(BCT, CURRENT_CONSENT);
    if (isItemNotExcluded(mapExcludeDataItems, currentConsent, false)) {
      StackedBarChartsItem<Integer> item =
          new DataBuilder().consents(consents).buildCurrentConsent();
      currentDataList.add(new DiseaseDataItem(currentConsent, ITEMTYPE_STACKED_BAR_CHARTS, item));
      if (debug) {
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(currentConsent), ITEMTYPE_DEBUG, item.getDebugData()));
      }
    }

    // bct.timeline.consent
    String timelineConsent = determineLabel(BCT, TIMELINE_CONSENT);
    if (isItemNotExcluded(mapExcludeDataItems, timelineConsent, false)) {
      Map<String, List<Long>> item = new DataBuilder().consents(consents).buildTimelineConsent();
      currentDataList.add(new DiseaseDataItem(timelineConsent, ITEMTYPE_LIST, item));
    }
    return currentDataList;
  }

  /** Keeps only the latest consent entry per patient based on permit start date. */
  private List<MiiConsent> filterConsentsToLastOnes(List<MiiConsent> consents) {

    int originalSize = consents.size();

    List<MiiConsent> filteredConsents =
        new ArrayList<>(
            consents.stream()
                // Ignore invalid entries
                .filter(consent -> consent.getPatientId() != null && consent.getDateTime() != null)
                // Group by patientId and keep the newest consent
                .collect(
                    Collectors.toMap(
                        MiiConsent::getPatientId,
                        Function.identity(),
                        (existing, replacement) ->
                            replacement.getDateTime().after(existing.getDateTime())
                                ? replacement
                                : existing))
                .values());

    int removedCount = originalSize - filteredConsents.size();

    // Example remaining consent ID after filtering
    String exampleRemainingConsentId =
        filteredConsents.stream()
            .map(MiiConsent::getIdPart)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("n/a");

    // Log removed outdated entries
    if (removedCount > 0)
      log.info(
          "Filtered {} consent entries because just the last one by patient is bct relevant."
              + " Example remaining consentId: {}",
          removedCount,
          exampleRemainingConsentId);

    return filteredConsents;
  }

  private List<MiiConsent> filterConsentByValidity(
      List<MiiConsent> ukbConsents, Predicate<MiiConsent> consentFilter) {

    return ukbConsents.parallelStream()
        // Filter revoked entries
        .filter(MiiConsent::isActive)
        .filter(MiiConsent::isPrivacyPolicyDocumentAndMiiConsentCategory)
        .filter(consentFilter)
        .toList();
  }

  private List<MiiConsent> filterConsentByValidity(List<MiiConsent> ukbConsents) {
    return ukbConsents.stream().filter(MiiConsent::isAcribisConsentAllowed).toList();
  }

  private static Set<String> extractPatientIds(List<MiiConsent> validPatDataUsage) {
    return validPatDataUsage.stream().map(MiiConsent::getPatientId).collect(Collectors.toSet());
  }

  private List<MiiConsent> getValidBroadConsents(List<MiiConsent> consents) {
    // No validation date to be checked needed
    return filterConsentByValidity(consents, consent -> consent.isMDataUsageAllowed(null));
  }
}
