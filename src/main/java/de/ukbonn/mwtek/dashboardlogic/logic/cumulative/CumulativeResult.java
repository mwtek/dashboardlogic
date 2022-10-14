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
package de.ukbonn.mwtek.dashboardlogic.logic.cumulative;

import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.LOINC_SYSTEM;
import static de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues.SNOMED_SYSTEM;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirCodingTools.isCodeInCodesystem;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.QualitativeLabResultCodes;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;

/**
 * This class is used for generating the data item {@link CoronaDataItem cumulative.result}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

public class CumulativeResult {

  List<UkbObservation> listObservations;

  public CumulativeResult(List<UkbObservation> listObservation) {
    this.listObservations = listObservation;
  }


  private List<UkbObservation> covidObservations;

  /**
   * Determination of the laboratory tests of all patients for whom there is an outpatient,
   * pre-hospital, posthospital, day-care or full inpatient case in connection with the test
   * depending on the laboratory result.
   *
   * @param labResult         The laboratory result to be filtered for (e.g.
   *                          {@link CoronaFixedValues#POSITIVE}).
   * @param inputCodeSettings The configuration of the parameterizable codes such as the observation
   *                          codes or procedure codes.
   * @return Get tests of all patients for whom an outpatient, pre-hospital, posthospital, partial
   * hospitalisation or full hospitalisation case related to the test exists.
   */
  public Set<UkbObservation> getObservationsByResult(CoronaFixedValues labResult,
      InputCodeSettings inputCodeSettings) {
    Set<UkbObservation> listObs = new HashSet<>();

    // Initial filtering of the needed observations
    if (covidObservations == null) {
      covidObservations = listObservations.parallelStream()
          .filter(x -> x.hasCode() && x.getCode().hasCoding() && x.hasValueCodeableConcept())
          .filter(x -> isCodeInCodesystem(x.getCode().getCoding(),
              inputCodeSettings.getObservationPcrLoincCodes(), LOINC_SYSTEM)).toList();
    }

    switch (labResult) {
      case POSITIVE -> {
        listObs = covidObservations.parallelStream().filter(
                x -> isCodeInCodesystem(((CodeableConcept) x.getValue()).getCoding(),
                    QualitativeLabResultCodes.getPositiveCodes(), SNOMED_SYSTEM))
            .collect(Collectors.toSet());
      } // case
      case BORDERLINE -> {
        listObs = covidObservations.parallelStream()
            .filter(x -> isCodeInCodesystem(((CodeableConcept) x.getValue()).getCoding(),
                QualitativeLabResultCodes.getBorderlineCodes(), SNOMED_SYSTEM))
            .collect(Collectors.toSet());
      } // case
      case NEGATIVE -> {
        listObs = covidObservations.parallelStream()
            .filter(x -> isCodeInCodesystem(((CodeableConcept) x.getValue()).getCoding(),
                QualitativeLabResultCodes.getNegativeCodes(), SNOMED_SYSTEM))
            .collect(Collectors.toSet());
      } // case
      default -> {
        // do nothing
      }
    }
    return listObs;
  }

  public Map<String, Number> createResultMap(Set<UkbObservation> positiveResult,
      Set<UkbObservation> negativeResult, Set<UkbObservation> borderLineResult) {
    Map<String, Number> resultMap = new HashMap<>();

    resultMap.put(CoronaFixedValues.POSITIVE.getValue(), positiveResult.size());
    resultMap.put(CoronaFixedValues.NEGATIVE.getValue(), negativeResult.size());
    resultMap.put(CoronaFixedValues.BORDERLINE.getValue(), borderLineResult.size());

    return resultMap;

  }
}
