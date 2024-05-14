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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative.results;

import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.incrementVariantCount;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.isCodingValid;
import static de.ukbonn.mwtek.dashboardlogic.tools.StringHelper.isAnyMatchSetWithString;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.LOINC;

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

/**
 * This class is used for generating the data item
 * {@link DiseaseDataItem cumulative.varianttestresults}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeVariantTestResults extends DashboardDataItemLogics {

  List<Coding> variantCodings;
  VariantSettings variantSettings;

  /**
   * Creation of a map that assigns the supported Covid-19 variants the frequency of their
   * occurrence at the site. Currently, only LOINC encodings are supported.
   *
   * @param variantSettings   The local configuration to extend the query logic with additional
   *                          covid-19 variants that are not yet known at the time of release.
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return Map with frequencies per covid variant.
   */
  public Map<String, Integer> createVariantTestResultMap(VariantSettings variantSettings,
      InputCodeSettings inputCodeSettings) {
    this.variantSettings = variantSettings;
    Map<String, Integer> variantMap = new LinkedHashMap<>();
    initializeVariantMap(variantMap);
    List<String> observationVariantLoincCodes =
        inputCodeSettings.getCovidObservationVariantLoincCodes();

    // Get all the coding-entries that contain loinc information
    try {
      variantCodings = getVariantObservations().parallelStream()
          .filter(x -> isCodingValid(x.getCode(), LOINC,
              observationVariantLoincCodes))
          .map(Observation::getValueCodeableConcept).filter(CodeableConcept::hasCoding).flatMap(
              x -> x.getCoding().stream().filter(Coding::hasSystem)
                  .filter(y -> y.getSystem().equals(LOINC))).toList();

      for (Coding variantCoding : variantCodings) {
        readCodingAndIncrementVariantMap(variantCoding, variantMap);
      }
    } catch (Exception ex) {
      log.error("Error in the covid variant retrieval ", ex);
    }
    return variantMap;
  }

  private void initializeVariantMap(Map<String, Integer> variantMap) {
    // Initialization the map with the value set from the data set description 
    if (variantMap.isEmpty()) {
      variantMap.put(DashboardLogicFixedValues.VARIANT_ALPHA, 0);
      variantMap.put(DashboardLogicFixedValues.VARIANT_BETA, 0);
      variantMap.put(DashboardLogicFixedValues.VARIANT_GAMMA, 0);
      variantMap.put(DashboardLogicFixedValues.VARIANT_DELTA, 0);
      variantMap.put(DashboardLogicFixedValues.VARIANT_OMICRON, 0);
      variantMap.put(DashboardLogicFixedValues.VARIANT_OTHER_VOC, 0);
      variantMap.put(DashboardLogicFixedValues.VARIANT_NON_VOC, 0);
      variantMap.put(DashboardLogicFixedValues.VARIANT_UNKNOWN, 0);
    }
  }

  /**
   * Reads the given LOINC variant code and increments the entry in the overall map.
   *
   * @param variantCoding {@link CodeableConcept#getCoding() CodeableConcept.coding} that contains
   *                      the LOINC code for covid variants from
   *                      {@link Observation#getValueCodeableConcept()}.
   * @param variantMap    Map that counts the frequencies per covid variant.
   */
  private void readCodingAndIncrementVariantMap(Coding variantCoding,
      Map<String, Integer> variantMap) {

    if (variantMap.isEmpty()) {
      initializeVariantMap(variantMap);
    }

    // Counting the frequency of LOINC variant codes and then trying to get more information from
    // the display name about variants that cannot be coded via LOINC.
    switch (variantCoding.getCode()) {
      case DashboardLogicFixedValues.VARIANT_ALPHA_LOINC -> incrementVariantCount(variantMap,
          DashboardLogicFixedValues.VARIANT_ALPHA);
      case DashboardLogicFixedValues.VARIANT_BETA_LOINC -> incrementVariantCount(variantMap,
          DashboardLogicFixedValues.VARIANT_BETA);
      case DashboardLogicFixedValues.VARIANT_DELTA_LOINC -> incrementVariantCount(variantMap,
          DashboardLogicFixedValues.VARIANT_DELTA);
      case DashboardLogicFixedValues.VARIANT_GAMMA_LOINC -> incrementVariantCount(variantMap,
          DashboardLogicFixedValues.VARIANT_GAMMA);
      case DashboardLogicFixedValues.VARIANT_OMICRON_LOINC -> incrementVariantCount(variantMap,
          DashboardLogicFixedValues.VARIANT_OMICRON);

      // If the value is not handled or is not part of the Loinc system, count it as "Unknown"
      // unless other variants can be determined via the display name.
      default -> {
        // Optional handling of Covid variants that have not yet been assigned a LOINC code ->
        // Check display text for variants
        String codingDisplay = variantCoding.getDisplay();
        if (isAnyMatchSetWithString(variantSettings.getOtherVoc(), codingDisplay)) {
          incrementVariantCount(variantMap, DashboardLogicFixedValues.VARIANT_OTHER_VOC);
        } else if (isAnyMatchSetWithString(variantSettings.getNonVoc(), codingDisplay)) {
          incrementVariantCount(variantMap, DashboardLogicFixedValues.VARIANT_NON_VOC);
        } else {
          incrementVariantCount(variantMap, DashboardLogicFixedValues.VARIANT_UNKNOWN);
          // The log message got commented out since it could contain patient data and fills the
          // logs heavily. If there is an urgent need comment it in again.
//          log.debug(
//              "No support for covid-19 variant with loinc code: " + variantCoding.getCode()
//                  + " and display: " + variantCoding.getDisplay());
        }
      }
    }
  }

}

