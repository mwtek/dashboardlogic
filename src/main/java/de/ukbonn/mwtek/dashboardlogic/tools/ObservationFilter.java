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

package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.OBSERVATION_INTERPRETATION_SYSTEM;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.LOINC;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.SNOMED;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInAnyCoding;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodeableConcepts;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodesystem;

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.ObservationInterpretationDetectionCodes;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;

/** Various auxiliary methods that affect the {@link UkbObservation} resources. */
@Slf4j
public class ObservationFilter {

  /**
   * Filter a list of {@link UkbObservation} resources to those describing a disease-positive PCR
   * finding.
   *
   * @param labObservations A list with {@link UkbObservation observation resources}.
   * @param inputCodeSettings An {@link InputCodeSettings} instance that contains the valid
   *     disease-context-related pcr codes.
   * @return A filtered collection containing only observations with disease-context-related pcr
   *     findings. If the input list is <code>null</code> it will return an empty set.
   */
  public static Set<UkbObservation> getObservationsByContext(
      Collection<UkbObservation> labObservations,
      InputCodeSettings inputCodeSettings,
      DataItemContext dataItemContext) {
    switch (dataItemContext) {
      case COVID -> {
        return getObservationsByCode(
            labObservations, inputCodeSettings.getCovidObservationPcrLoincCodes());
      }
      case INFLUENZA -> {
        return getObservationsByCode(
            labObservations, inputCodeSettings.getInfluenzaObservationPcrLoincCodes());
      }
      default -> {
        log.error(
            "Method getObservationsByContext is invoked with an unknown context [{}].",
            dataItemContext);
        return null;
      }
    }
  }

  /**
   * Filter a list of {@link UkbObservation} resources to those describing a disease-positive PCR
   * finding.
   *
   * @param labObservations A list with {@link UkbObservation observation resources}.
   * @param inputCodeSettings An {@link InputCodeSettings} instance that contains the valid
   *     disease-context-related pcr loinc codes.
   * @return A filtered collection containing only observations with disease-context-related pcr
   *     findings. If the input list is <code>null</code> it will return an empty set.
   */
  public static Set<UkbObservation> getVariantObservationsByContext(
      Collection<UkbObservation> labObservations,
      InputCodeSettings inputCodeSettings,
      DataItemContext dataItemContext) {
    switch (dataItemContext) {
      case COVID -> {
        return getObservationsByCode(
            labObservations, inputCodeSettings.getCovidObservationVariantLoincCodes());
      }
      case INFLUENZA -> {
        // Not implemented yet
        return null;
      }
      default -> {
        return null;
      }
    }
  }

  public static Set<UkbObservation> getObservationsByLoincCode(
      Collection<UkbObservation> labObservations, Collection<String> loincCodes) {
    if (labObservations != null) {
      return labObservations.parallelStream()
          .filter(x -> x.hasCode() && x.getCode().hasCoding())
          .filter(x -> isCodeInCodesystem(x.getCode().getCoding(), loincCodes, LOINC))
          .collect(Collectors.toSet());
    } else {
      return new HashSet<>();
    }
  }

  public static Set<UkbObservation> getObservationsByCode(
      Collection<UkbObservation> labObservations, Collection<String> loincCodes) {
    if (labObservations != null) {
      return labObservations.parallelStream()
          .filter(x -> x.hasCode() && x.getCode().hasCoding())
          .filter(x -> isCodeInAnyCoding(x.getCode().getCoding(), loincCodes))
          .collect(Collectors.toSet());
    } else {
      return new HashSet<>();
    }
  }

  public static boolean hasObservationLoincCode(
      Observation labObservation, Collection<String> loincCodes) {
    if (labObservation != null) {
      return labObservation.hasCode()
          && labObservation.getCode().hasCoding()
          && isCodeInCodesystem(labObservation.getCode().getCoding(), loincCodes, LOINC);
    } else {
      return false;
    }
  }

  /**
   * Filtering a list of covid {@link UkbObservation} resources on a result in the {@link
   * UkbObservation#getValue()} field.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *     that describe disease-positive findings. Creatable via {@link
   *     ObservationFilter#getObservationsByContext }.
   * @param obsResultType The value to filter on (e.g. {@link DashboardLogicFixedValues#POSITIVE}).
   * @return A filtered collection containing the resources that have the specified result type in
   *     the {@link UkbObservation#getValue()} attribute.
   */
  public static Set<UkbObservation> getObservationsByValue(
      Collection<UkbObservation> covidObservations,
      DashboardLogicFixedValues obsResultType,
      QualitativeLabCodesSettings qualitativeLabCodesSettings) {

    return covidObservations.parallelStream()
        .filter(Observation::hasValueCodeableConcept)
        .filter(
            x ->
                isCodeInCodesystem(
                    ((CodeableConcept) x.getValue()).getCoding(),
                    getObsValueCodeSystem(obsResultType, qualitativeLabCodesSettings),
                    SNOMED))
        .collect(Collectors.toSet());
  }

  /**
   * Filtering a list of covid {@link UkbObservation} resources on a result in the {@link
   * UkbObservation#getInterpretation()} field.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *     that describe disease-positive findings. Creatable via {@link
   *     ObservationFilter#getObservationsByContext}.
   * @param obsResultType The value to filter on (e.g. {@link DashboardLogicFixedValues#POSITIVE}).
   * @return A filtered collection containing the resources that have the specified result type in
   *     the {@link UkbObservation#getInterpretation()} attribute.
   */
  public static Set<UkbObservation> getObservationsByInterpretation(
      Collection<UkbObservation> covidObservations, DashboardLogicFixedValues obsResultType) {

    return covidObservations.parallelStream()
        .filter(x -> !x.hasValue())
        .filter(
            x ->
                isCodeInCodeableConcepts(
                    x.getInterpretation(),
                    OBSERVATION_INTERPRETATION_SYSTEM,
                    getObsInterpretationCodeSystem(obsResultType)))
        .collect(Collectors.toSet());
  }

  /**
   * Auxiliary function that calls {@link
   * #getObservationsByValue(Collection,DashboardLogicFixedValues, QualitativeLabCodesSettings)} and
   * returns only the case numbers of the retrieved observations.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *     that describe disease-positive findings. Creatable via {@link
   *     ObservationFilter#getObservationsByContext}.
   * @param obsResultType The value to filter on (e.g. {@link DashboardLogicFixedValues#POSITIVE}).
   */
  public static Set<String> getCaseIdsByObsValue(
      Collection<UkbObservation> covidObservations,
      DashboardLogicFixedValues obsResultType,
      QualitativeLabCodesSettings qualitativeLabCodesSettings) {
    return getObservationsByValue(covidObservations, obsResultType, qualitativeLabCodesSettings)
        .stream()
        .map(UkbObservation::getCaseId)
        .collect(Collectors.toSet());
  }

  /**
   * Auxiliary function that calls {@link #getObservationsByInterpretation(Collection,
   * DashboardLogicFixedValues)} and returns only the case numbers of the retrieved observations.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *     that describe disease-positive findings. Creatable via {@link
   *     ObservationFilter#getObservationsByContext }.
   * @param obsResultType The value to filter on (e.g. {@link DashboardLogicFixedValues#POSITIVE}).
   */
  public static Set<String> getCaseIdsByObsInterpretation(
      Collection<UkbObservation> covidObservations, DashboardLogicFixedValues obsResultType) {
    return getObservationsByInterpretation(covidObservations, obsResultType).stream()
        .map(UkbObservation::getCaseId)
        .collect(Collectors.toSet());
  }

  /**
   * Auxiliary function that calls {@link #getObservationsByValue(Collection,
   * DashboardLogicFixedValues, QualitativeLabCodesSettings)} and returns only the patient ids of
   * the retrieved observations.
   *
   * @param positiveObservations A filtered list with {@link UkbObservation observation resources},
   *     that describe disease-positive findings. Creatable via {@link
   *     ObservationFilter#getObservationsByContext} (Collection, InputCodeSettings)}.
   * @param obsResultType The value to filter on (e.g. {@link DashboardLogicFixedValues#POSITIVE}).
   */
  public static Set<String> getPatientIdsByObsValue(
      Collection<UkbObservation> positiveObservations,
      DashboardLogicFixedValues obsResultType,
      QualitativeLabCodesSettings qualitativeLabCodesSettings) {
    return getObservationsByValue(positiveObservations, obsResultType, qualitativeLabCodesSettings)
        .stream()
        .map(UkbObservation::getPatientId)
        .collect(Collectors.toSet());
  }

  /**
   * Auxiliary function that calls {@link #getObservationsByInterpretation(Collection,
   * DashboardLogicFixedValues)} and returns only the patient ids of the retrieved observations.
   *
   * @param positiveObservations A filtered list with {@link UkbObservation observation resources},
   *     that describe disease-positive findings. Creatable via {@link
   *     ObservationFilter#getObservationsByContext }.
   * @param obsResultType The value to filter on (e.g. {@link DashboardLogicFixedValues#POSITIVE}).
   */
  public static Set<String> getPatientIdsByObsInterpretation(
      Collection<UkbObservation> positiveObservations, DashboardLogicFixedValues obsResultType) {
    return getObservationsByInterpretation(positiveObservations, obsResultType).stream()
        .map(UkbObservation::getPatientId)
        .collect(Collectors.toSet());
  }

  /**
   * Retrieve the value sets for the passed result type for attribute {@link
   * UkbObservation#getValue()}.
   */
  private static List<String> getObsValueCodeSystem(
      DashboardLogicFixedValues obsResult,
      QualitativeLabCodesSettings qualitativeLabCodesSettings) {

    // If no individual lab codes are set, use defaults instead
    if (qualitativeLabCodesSettings == null)
      qualitativeLabCodesSettings = new QualitativeLabCodesSettings();

    switch (obsResult) {
      case POSITIVE -> {
        return qualitativeLabCodesSettings.getPositiveCodes();
      }
      case BORDERLINE -> {
        return qualitativeLabCodesSettings.getBorderlineCodes();
      }
      case NEGATIVE -> {
        return qualitativeLabCodesSettings.getNegativeCodes();
      }
    }
    // We never should end here.
    return new ArrayList<>();
  }

  /**
   * Retrieve the value sets for the passed result type for attribute {@link
   * UkbObservation#getInterpretation()}.
   */
  private static List<String> getObsInterpretationCodeSystem(DashboardLogicFixedValues obsResult) {
    switch (obsResult) {
      case POSITIVE -> {
        return ObservationInterpretationDetectionCodes.getPositiveCodes();
      }
      case BORDERLINE -> {
        return ObservationInterpretationDetectionCodes.getBorderlineCodes();
      }
      case NEGATIVE -> {
        return ObservationInterpretationDetectionCodes.getNegativeCodes();
      }
    }
    // We never should end here.
    return new ArrayList<>();
  }
}
