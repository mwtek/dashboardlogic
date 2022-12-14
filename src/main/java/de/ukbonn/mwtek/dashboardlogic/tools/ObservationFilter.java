/*
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.LOINC_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.OBSERVATION_INTERPRETATION_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.SNOMED_SYSTEM;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodeableConcepts;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodesystem;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.ObservationInterpretationDetectionCodes;
import de.ukbonn.mwtek.dashboardlogic.enums.QualitativeLabResultCodes;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.jetbrains.annotations.NotNull;

/**
 * Various auxiliary methods that affect the {@link UkbObservation} resources.
 */
public class ObservationFilter {

  /**
   * Filter a list of {@link UkbObservation} resources to those describing a covid PCR finding.
   *
   * @param labObservations   A list with {@link UkbObservation observation resources}.
   * @param inputCodeSettings An {@link InputCodeSettings} instance that contains the valid covid
   *                          pcr loinc codes.
   * @return A filtered collection containing only observations with covid pcr findings. If the
   * input list is <code>null</code> it will return an empty set.
   */
  @NotNull
  public static Set<UkbObservation> getCovidObservations(
      Collection<UkbObservation> labObservations,
      InputCodeSettings inputCodeSettings) {
    if (labObservations != null) {
      return labObservations.parallelStream()
          .filter(x -> x.hasCode() && x.getCode().hasCoding())
          .filter(x -> isCodeInCodesystem(x.getCode().getCoding(),
              inputCodeSettings.getObservationPcrLoincCodes(), LOINC_SYSTEM))
          .collect(Collectors.toSet());
    } else {
      return new HashSet<>();
    }
  }

  /**
   * Filtering a list of covid {@link UkbObservation} resources on a result in the
   * {@link UkbObservation#getValue()} field.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *                          that describe covid findings. Creatable via
   *                          {@link ObservationFilter#getCovidObservations(Collection,
   *                          InputCodeSettings)}.
   * @param obsResultType     The value to filter on (e.g. {@link CoronaFixedValues#POSITIVE}).
   * @return A filtered collection containing the resources that have the specified result type in
   * the {@link UkbObservation#getValue()} attribute.
   */
  @NotNull
  public static Set<UkbObservation> getObservationsByValue(
      Collection<UkbObservation> covidObservations, CoronaFixedValues obsResultType) {

    return covidObservations.parallelStream().filter(Observation::hasValueCodeableConcept).filter(
            x -> isCodeInCodesystem(((CodeableConcept) x.getValue()).getCoding(),
                getObsValueCodeSystem(obsResultType),
                SNOMED_SYSTEM))
        .collect(Collectors.toSet());
  }

  /**
   * Filtering a list of covid {@link UkbObservation} resources on a result in the
   * {@link UkbObservation#getInterpretation()} field.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *                          that describe covid findings. Creatable via
   *                          {@link ObservationFilter#getCovidObservations(Collection,
   *                          InputCodeSettings)}.
   * @param obsResultType     The value to filter on (e.g. {@link CoronaFixedValues#POSITIVE}).
   * @return A filtered collection containing the resources that have the specified result type in
   * the {@link UkbObservation#getInterpretation()} attribute.
   */
  @NotNull
  public static Set<UkbObservation> getObservationsByInterpretation(
      Collection<UkbObservation> covidObservations, CoronaFixedValues obsResultType) {

    return covidObservations.parallelStream().filter(x -> !x.hasValue())
        .filter(x -> isCodeInCodeableConcepts(x.getInterpretation(),
            OBSERVATION_INTERPRETATION_SYSTEM,
            getObsInterpretationCodeSystem(obsResultType)))
        .collect(Collectors.toSet());
  }

  /**
   * Auxiliary function that calls {@link #getObservationsByValue(Collection, CoronaFixedValues)}
   * and returns only the case numbers of the retrieved observations.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *                          that describe covid findings. Creatable via
   *                          {@link ObservationFilter#getCovidObservations(Collection,
   *                          InputCodeSettings)}.
   * @param obsResultType     The value to filter on (e.g. {@link CoronaFixedValues#POSITIVE}).
   */
  @NotNull
  public static Set<String> getCaseIdsByObsValue(
      Collection<UkbObservation> covidObservations, CoronaFixedValues obsResultType) {
    return getObservationsByValue(covidObservations, obsResultType).stream()
        .map(UkbObservation::getCaseId).collect(
            Collectors.toSet());
  }

  /**
   * Auxiliary function that calls
   * {@link #getObservationsByInterpretation(Collection, CoronaFixedValues)} and returns only the
   * case numbers of the retrieved observations.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *                          that describe covid findings. Creatable via
   *                          {@link ObservationFilter#getCovidObservations(Collection,
   *                          InputCodeSettings)}.
   * @param obsResultType     The value to filter on (e.g. {@link CoronaFixedValues#POSITIVE}).
   */
  @NotNull
  public static Set<String> getCaseIdsByObsInterpretation(
      Collection<UkbObservation> covidObservations, CoronaFixedValues obsResultType) {
    return getObservationsByInterpretation(covidObservations, obsResultType).stream()
        .map(UkbObservation::getCaseId).collect(
            Collectors.toSet());
  }

  /**
   * Auxiliary function that calls {@link #getObservationsByValue(Collection, CoronaFixedValues)}
   * and returns only the patient ids of the retrieved observations.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *                          that describe covid findings. Creatable via
   *                          {@link ObservationFilter#getCovidObservations(Collection,
   *                          InputCodeSettings)}.
   * @param obsResultType     The value to filter on (e.g. {@link CoronaFixedValues#POSITIVE}).
   */
  @NotNull
  public static Set<String> getPatientIdsByObsValue(
      Collection<UkbObservation> covidObservations, CoronaFixedValues obsResultType) {
    return getObservationsByValue(covidObservations, obsResultType).stream()
        .map(UkbObservation::getPatientId).collect(
            Collectors.toSet());
  }

  /**
   * Auxiliary function that calls
   * {@link #getObservationsByInterpretation(Collection, CoronaFixedValues)} and returns only the
   * patient ids of the retrieved observations.
   *
   * @param covidObservations A filtered list with {@link UkbObservation observation resources},
   *                          that describe covid findings. Creatable via
   *                          {@link ObservationFilter#getCovidObservations(Collection,
   *                          InputCodeSettings)}.
   * @param obsResultType     The value to filter on (e.g. {@link CoronaFixedValues#POSITIVE}).
   */
  @NotNull
  public static Set<String> getPatientIdsByObsInterpretation(
      Collection<UkbObservation> covidObservations, CoronaFixedValues obsResultType) {
    return getObservationsByInterpretation(covidObservations, obsResultType).stream()
        .map(UkbObservation::getPatientId).collect(
            Collectors.toSet());
  }

  /**
   * Retrieve the value sets for the passed result type for attribute
   * {@link UkbObservation#getValue()}.
   */
  private static List<String> getObsValueCodeSystem(CoronaFixedValues obsResult) {
    switch (obsResult) {
      case POSITIVE -> {
        return QualitativeLabResultCodes.getPositiveCodes();
      }
      case BORDERLINE -> {
        return QualitativeLabResultCodes.getBorderlineCodes();
      }
      case NEGATIVE -> {
        return QualitativeLabResultCodes.getNegativeCodes();
      }
    }
    // We never should end here.
    return new ArrayList<>();
  }

  /**
   * Retrieve the value sets for the passed result type for attribute
   * {@link UkbObservation#getInterpretation()}.
   */
  private static List<String> getObsInterpretationCodeSystem(CoronaFixedValues obsResult) {
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
