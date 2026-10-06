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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.SNID;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_DEBUG;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_PARAMETERS_SET;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_PARAMETERS_TIMELINE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_RECRUITMENT_DIZ;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_RECRUITMENT_DIZ;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.ParametersSetItem;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.ProviderConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.DataBuilder;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiQuestionnaireResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the json specification with
 * SNID context are generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class SnidDataItemGenerator extends DataItemGenerator {

  List<MiiConsent> consents;
  List<MiiQuestionnaireResponse> questionnaireResponses;

  /** Initialization of the Acribis related fhir resource lists. */
  public SnidDataItemGenerator(
      List<MiiConsent> consents,
      List<MiiCondition> ukbConditions,
      List<MiiPatient> miiPatients,
      List<MiiEncounter> miiEncounters,
      List<MiiProcedure> ukbProcedures,
      List<MiiQuestionnaireResponse> questionnaireResponses) {
    super(ukbConditions, null, miiPatients, miiEncounters, ukbProcedures, null);
    this.consents = consents;
    this.questionnaireResponses = questionnaireResponses;
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
      GlobalConfiguration globalConfiguration,
      ProviderConfiguration providerConfiguration) {
    List<DiseaseDataItem> currentDataList = new ArrayList<>();
    if (mapExcludeDataItems == null) {
      mapExcludeDataItems = new HashMap<>();
    }
    boolean outputDebugItems = globalConfiguration.getOutputDebugItems();

    // The valid cases in our use case must have agreed to specific fields
    var snidConsents = consents.stream().filter(MiiConsent::isSnidConsentAllowed).toList();

    DiseaseDataItem cd;
    // snid.current.recruitment.diz
    String currentRecruitmentLabel = determineLabel(SNID, CURRENT_RECRUITMENT_DIZ);
    if (isItemNotExcluded(mapExcludeDataItems, currentRecruitmentLabel, false)) {
      ParametersSetItem<Integer> item =
          new DataBuilder()
              .providerConfiguration(providerConfiguration)
              .consents(snidConsents)
              .buildCurrentRecruitmentDiz();
      currentDataList.add(
          new DiseaseDataItem(currentRecruitmentLabel, ITEMTYPE_PARAMETERS_SET, item));
      if (outputDebugItems) {
        currentDataList.add(
            new DiseaseDataItem(
                addDebugLabel(currentRecruitmentLabel), ITEMTYPE_DEBUG, item.getDebugData()));
      }
    }
    // snid.timeline.recruitment.diz
    String timelineRecruitmentLabel = determineLabel(SNID, TIMELINE_RECRUITMENT_DIZ);
    if (isItemNotExcluded(mapExcludeDataItems, timelineRecruitmentLabel, false)) {
      currentDataList.add(
          new DiseaseDataItem(
              timelineRecruitmentLabel,
              ITEMTYPE_PARAMETERS_TIMELINE,
              new DataBuilder()
                  .consents(consents)
                  .providerConfiguration(providerConfiguration)
                  .buildTimelineRecruitmentDizMap()));
    }
    return currentDataList;
  }
}
