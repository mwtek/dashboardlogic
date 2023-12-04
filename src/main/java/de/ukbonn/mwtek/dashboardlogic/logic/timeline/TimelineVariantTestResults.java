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

import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.getDatesOutputList;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.isCodingValid;
import static de.ukbonn.mwtek.dashboardlogic.tools.StringHelper.isAnyMatchSetWithString;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaDashboardConstants;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
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
 * This class is used for generating the data item
 * {@link CoronaDataItem timeline.varianttestresults}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class TimelineVariantTestResults {

  List<UkbObservation> listObservation;

  public TimelineVariantTestResults(List<UkbObservation> listObservation) {
    this.listObservation = listObservation;
  }

  public Map<String, List<Long>> createTimelineVariantsTests(VariantSettings variantSettings,
      InputCodeSettings inputCodeSettings) {
    Map<String, List<Long>> variantMap = new LinkedHashMap<>();
    List<String> observationVariantLoincCodes = inputCodeSettings.getObservationVariantLoincCodes();
    // Initialization of a map with counts for each variant for each 24-h-period
    variantMap.put(CoronaFixedValues.DATE.getValue(), new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_ALPHA, new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_BETA, new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_GAMMA, new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_DELTA, new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_OMICRON, new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_OTHER_VOC, new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_NON_VOC, new ArrayList<>());
    variantMap.put(CoronaFixedValues.VARIANT_UNKNOWN, new ArrayList<>());

    List<UkbObservation> listVariantObservation = listObservation.stream()
        .filter(x -> isCodingValid(x.getCode(), CoronaFixedValues.LOINC_SYSTEM,
            observationVariantLoincCodes))
        .filter(Observation::hasValueCodeableConcept).toList();

    long currentUnixTime = DateTools.getCurrentUnixTime();

    // initialization of the map with the date entries to keep the order ascending
    long startDate = CoronaDashboardConstants.qualifyingDate;
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
      final long nextDateUnix = startDate + CoronaDashboardConstants.dayInSeconds;

      // Retrieve all observations for the checked date
      List<UkbObservation> listFittingVariantObservation = listVariantObservation.stream()
          .filter(x -> checkDateUnix <= DateTools.dateToUnixTime(
              x.getEffectiveDateTimeType().getValue()))
          .filter(x -> nextDateUnix > DateTools.dateToUnixTime(
              x.getEffectiveDateTimeType().getValue())).toList();

      for (UkbObservation variantObservation : listFittingVariantObservation) {
//        boolean observationContainsLoinc =
//            variantObservation.getValueCodeableConcept().getCoding().stream()
//                .filter(x -> x.hasSystem()).anyMatch(
//                    x -> x.getSystem().equals(CoronaFixedValues.LOINC_SYSTEM.getValue()));

        // If a LOINC notation is found, only this is read. Otherwise, an attempt is made to determine the variant information via the free text.
        for (Coding variantCoding : variantObservation.getValueCodeableConcept().getCoding()) {
          // For now the display values are checked since its more flexible if new variants appear or to generalize non-voc variants
          if (variantCoding.hasSystem() && variantCoding.getSystem()
              .equals(CoronaFixedValues.LOINC_SYSTEM)) {
            switch (variantCoding.getCode()) {
              case CoronaFixedValues.VARIANT_ALPHA_LOINC -> alphaCount++;
              case CoronaFixedValues.VARIANT_BETA_LOINC -> betaCount++;
              case CoronaFixedValues.VARIANT_DELTA_LOINC -> deltaCount++;
              case CoronaFixedValues.VARIANT_GAMMA_LOINC -> gammaCount++;
              case CoronaFixedValues.VARIANT_OMICRON_LOINC -> omicronCount++;

              // If value is unhandled or not part of the loinc system -> count it as unknown
              default -> {
                // Optional handling of Covid variants that have not yet been assigned a LOINC code -> Check display text for variants
                String codingDisplay = variantCoding.getDisplay();
                if (isAnyMatchSetWithString(variantSettings.getOtherVoc(), codingDisplay)) {
                  otherVocCount++;
                } else if (isAnyMatchSetWithString(variantSettings.getNonVoc(), codingDisplay)) {
                  nonVocCount++;
                } else {
                  unknownCount++;
//                  log.debug(
//                      "No support for covid variant with loinc code: " + variantCoding.getCode()
//                          + " and display: " + variantCoding.getDisplay());
                }
              }
            }
          }
        }
      }
      variantMap.get(CoronaFixedValues.VARIANT_ALPHA).add(alphaCount);
      variantMap.get(CoronaFixedValues.VARIANT_BETA).add(betaCount);
      variantMap.get(CoronaFixedValues.VARIANT_GAMMA).add(gammaCount);
      variantMap.get(CoronaFixedValues.VARIANT_DELTA).add(deltaCount);
      variantMap.get(CoronaFixedValues.VARIANT_OMICRON).add(omicronCount);
      variantMap.get(CoronaFixedValues.VARIANT_OTHER_VOC).add(otherVocCount);
      variantMap.get(CoronaFixedValues.VARIANT_NON_VOC).add(nonVocCount);
      variantMap.get(CoronaFixedValues.VARIANT_UNKNOWN).add(unknownCount);

      startDate += CoronaDashboardConstants.dayInSeconds;
    }
    variantMap
        .put(CoronaFixedValues.DATE.getValue(), getDatesOutputList());

    return variantMap;
  }
}
