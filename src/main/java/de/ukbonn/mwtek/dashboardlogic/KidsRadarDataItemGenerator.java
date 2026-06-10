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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;
import static de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter.getInpatientFacilityEncounters;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.GlobalConfiguration;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Class in which the individual {@link DiseaseDataItem DataItems} of the json specification with
 * kids radar context are generated.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class KidsRadarDataItemGenerator extends DataItemGenerator {

  /**
   * Initialization of the CoronaResults object with the required FhirRessource lists.
   *
   * @param conditions List of FHIR condition resources for U07.1 diagnoses
   * @param patients List of FHIR patient resources based on the Condition and Observation inputs
   * @param encounters List of FHIR encounter resources based on the Condition and Observation
   *     inputs
   * @param icuProcedures List of FHIR ICU procedures resources based on the Patient/Encounter
   *     inputs
   * @param locations List of all FHIR location resources based on the encounter inputs
   */
  public KidsRadarDataItemGenerator(
      List<MiiCondition> conditions,
      List<MiiObservation> observations,
      List<MiiPatient> patients,
      List<MiiEncounter> encounters,
      List<MiiProcedure> icuProcedures,
      List<MiiLocation> locations) {
    super(conditions, observations, patients, encounters, icuProcedures, locations);
  }

  /**
   * Creation of the JSON specification file for the Corona dashboard based on FHIR resources.
   *
   * @param mapExcludeDataItems Map with data items to be excluded from the output (e.g.
   *     "current.treatmentlevel").
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
    reportMissingFields(encounters);

    // Filtering outpatient and semi-stationary encounter (that could hold Encounter.class =
    // 'IMP' as well)
    facilityContactEncountersInpatient = getInpatientFacilityEncounters(encounters);

    // Logging of unexpected attribute assignments within the resources.
    reportAttributeArtifacts(facilityContactEncountersInpatient);

    if (dataItemContext == DataItemContext.KIDS_RADAR_PED || dataItemContext == KIDS_RADAR) {
      KiRaPedDataItemGenerator pedDataItemGenerator =
          new KiRaPedDataItemGenerator(
              conditions, observations, patients, encounters, procedures, locations);
      currentDataList.addAll(
          pedDataItemGenerator.getDataItems(
              mapExcludeDataItems,
              variantSettings,
              inputCodeSettings,
              qualitativeLabCodesSettings,
              DataItemContext.KIDS_RADAR_PED,
              globalConfiguration));
    }
    if (dataItemContext == DataItemContext.KIDS_RADAR_KJP || dataItemContext == KIDS_RADAR) {

      KiRaKjpDataItemGenerator kjpDataItemGenerator =
          new KiRaKjpDataItemGenerator(conditions, null, patients, encounters, procedures, null);
      currentDataList.addAll(
          kjpDataItemGenerator.getDataItems(
              mapExcludeDataItems,
              variantSettings,
              inputCodeSettings,
              qualitativeLabCodesSettings,
              DataItemContext.KIDS_RADAR_KJP,
              globalConfiguration));
    }
    return currentDataList;
  }
}
