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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DATE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_ALPHA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_ALPHA_LOINC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_BETA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_BETA_LOINC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_DELTA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_DELTA_LOINC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_GAMMA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_GAMMA_LOINC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_NON_VOC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_OMICRON;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_OMICRON_LOINC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_OTHER_VOC;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.VARIANT_UNKNOWN;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.isCodingValid;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateInSeconds;
import static de.ukbonn.mwtek.dashboardlogic.tools.StringHelper.isAnyMatchSetWithString;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.LOINC;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.VariantSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

/**
 * This class is used for generating the data item {@link DiseaseDataItem
 * timeline.varianttestresults}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class TimelineVariantTestResults extends DashboardDataItemLogic {

  public Map<String, List<Long>> createTimelineVariantsTests(
      List<UkbObservation> observations,
      VariantSettings variantSettings,
      InputCodeSettings inputCodeSettings) {
    Map<String, List<Long>> variantMap = new LinkedHashMap<>();
    List<String> observationVariantLoincCodes =
        inputCodeSettings.getCovidObservationVariantLoincCodes();
    // Initialization of a map with counts for each variant for each 24-h-period
    variantMap.put(DATE, new ArrayList<>());
    variantMap.put(VARIANT_ALPHA, new ArrayList<>());
    variantMap.put(VARIANT_BETA, new ArrayList<>());
    variantMap.put(VARIANT_GAMMA, new ArrayList<>());
    variantMap.put(VARIANT_DELTA, new ArrayList<>());
    variantMap.put(VARIANT_OMICRON, new ArrayList<>());
    variantMap.put(VARIANT_OTHER_VOC, new ArrayList<>());
    variantMap.put(VARIANT_NON_VOC, new ArrayList<>());
    variantMap.put(VARIANT_UNKNOWN, new ArrayList<>());

    List<UkbObservation> variantObservations =
        observations.stream()
            .filter(x -> isCodingValid(x.getCode(), LOINC, observationVariantLoincCodes))
            .filter(Observation::hasValueCodeableConcept)
            .toList();

    long currentUnixTime = DateTools.getCurrentUnixTime();

    // initialization of the map with the date entries to keep the order ascending
    long startDate = getKickOffDateInSeconds(DataItemContext.COVID);
    while (startDate <= currentUnixTime) {
      long alphaCount = 0;
      long betaCount = 0;
      long gammaCount = 0;
      long deltaCount = 0;
      long omicronCount = 0;
      long otherVocCount = 0;
      long nonVocCount = 0;
      long unknownCount = 0;

      final long checkDateUnix = startDate;
      final long nextDateUnix = startDate + NumDashboardConstants.DAY_IN_SECONDS;

      // Retrieve all observations for the checked date
      List<UkbObservation> validVariantObservations =
          variantObservations.stream()
              .filter(
                  x ->
                      checkDateUnix
                          <= DateTools.dateToUnixTime(x.getEffectiveDateTimeType().getValue()))
              .filter(
                  x ->
                      nextDateUnix
                          > DateTools.dateToUnixTime(x.getEffectiveDateTimeType().getValue()))
              .toList();

      for (UkbObservation variantObservation : validVariantObservations) {
        //        boolean observationContainsLoinc =
        //            variantObservation.getValueCodeableConcept().getCoding().stream()
        //                .filter(x -> x.hasSystem()).anyMatch(
        //                    x -> x.getSystem().equals(CoronaFixedValues.LOINC_SYSTEM.getValue()));

        // If a LOINC notation is found, only this is read. Otherwise, an attempt is made to
        // determine the variant information via the free text.
        for (Coding variantCoding : variantObservation.getValueCodeableConcept().getCoding()) {
          // For now the display values are checked since its more flexible if new variants
          // appear or to generalize non-voc variants
          if (variantCoding.hasSystem() && variantCoding.getSystem().equals(LOINC)) {
            switch (variantCoding.getCode()) {
              case VARIANT_ALPHA_LOINC -> alphaCount++;
              case VARIANT_BETA_LOINC -> betaCount++;
              case VARIANT_DELTA_LOINC -> deltaCount++;
              case VARIANT_GAMMA_LOINC -> gammaCount++;
              case VARIANT_OMICRON_LOINC -> omicronCount++;

              // If value is unhandled or not part of the loinc system -> count it as unknown
              default -> {
                // Optional handling of Covid variants that have not yet been assigned a LOINC
                // code -> Check display text for variants
                String codingDisplay = variantCoding.getDisplay();
                if (isAnyMatchSetWithString(variantSettings.getOtherVoc(), codingDisplay)) {
                  otherVocCount++;
                } else if (isAnyMatchSetWithString(variantSettings.getNonVoc(), codingDisplay)) {
                  nonVocCount++;
                } else {
                  unknownCount++;
                  //                  log.debug(
                  //                      "No support for covid variant with loinc code: " +
                  // variantCoding.getCode()
                  //                          + " and display: " + variantCoding.getDisplay());
                }
              }
            }
          }
        }
      }
      variantMap.get(VARIANT_ALPHA).add(alphaCount);
      variantMap.get(VARIANT_BETA).add(betaCount);
      variantMap.get(VARIANT_GAMMA).add(gammaCount);
      variantMap.get(VARIANT_DELTA).add(deltaCount);
      variantMap.get(VARIANT_OMICRON).add(omicronCount);
      variantMap.get(VARIANT_OTHER_VOC).add(otherVocCount);
      variantMap.get(VARIANT_NON_VOC).add(nonVocCount);
      variantMap.get(VARIANT_UNKNOWN).add(unknownCount);

      startDate += NumDashboardConstants.DAY_IN_SECONDS;
    }
    variantMap.put(DATE, getDatesOutputList(DataItemContext.COVID));

    return variantMap;
  }
}
