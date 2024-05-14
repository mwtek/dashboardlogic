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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.BORDERLINE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.NEGATIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByContext;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByInterpretation;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByValue;

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem cumulative.result}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeResult extends DashboardDataItemLogics {

  private Set<UkbObservation> observationsByContext;

  /**
   * Determination of the laboratory tests of all patients for whom there is an outpatient,
   * pre-hospital, posthospital, day-care or full inpatient case in connection with the test
   * depending on the laboratory result.
   *
   * @param labResult The laboratory result to be filtered for (e.g.
   *                  {@link DashboardLogicFixedValues#POSITIVE}).
   * @return Get tests of all patients for whom an outpatient, pre-hospital, posthospital, partial
   * hospitalization or full hospitalization case related to the test exists.
   */
  public Set<UkbObservation> getObservationsByResult(DashboardLogicFixedValues labResult,
      DataItemContext dataItemContext) {
    Set<UkbObservation> listObs = new HashSet<>();

    // Initial filtering of the needed observations
    if (observationsByContext == null) {
      observationsByContext = getObservationsByContext(getObservations(),
          getInputCodeSettings(), dataItemContext);
    }

    // Check Observation.value if present, otherwise Observation.interpretation
    if (observationsByContext != null) {
      switch (labResult) {
        case POSITIVE -> {
          listObs = getObservationsByValue(observationsByContext, POSITIVE);
          listObs.addAll(getObservationsByInterpretation(observationsByContext, POSITIVE));
        } // case
        case BORDERLINE -> {
          listObs = getObservationsByValue(observationsByContext, BORDERLINE);
          listObs.addAll(getObservationsByInterpretation(observationsByContext, BORDERLINE));
        } // case
        case NEGATIVE -> {
          listObs = getObservationsByValue(observationsByContext, NEGATIVE);
          listObs.addAll(getObservationsByInterpretation(observationsByContext, NEGATIVE));
        } // case
        default -> {
          // do nothing
        }
      }
    } else {
      log.warn("No " + dataItemContext + " observations have been found");
    }
    return listObs;
  }

}
