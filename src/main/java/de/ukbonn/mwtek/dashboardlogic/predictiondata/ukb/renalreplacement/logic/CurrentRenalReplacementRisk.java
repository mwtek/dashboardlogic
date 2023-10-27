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

package de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.logic;

import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.BODY_WEIGHT;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.CREATININE;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.ENCOUNTER;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.LACTATE;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.START_REPLACEMENT;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.UREA;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.URINE_OUTPUT;

import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.misc.ValueOperations;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CurrentRenalReplacementRisk {

  public static List<Double> createData(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> mapModelParameter) {
    List<Double> resultList = new ArrayList<>();
    long periodTo = DateTools.dateToUnixTime(new Date());
    long periodFrom = periodTo - ValueOperations.dayInSeconds; //24 hours period
    Double bodyWeight;

    //Get all the encounters
    List<CoreBaseDataItem> encounters = mapModelParameter.get(ENCOUNTER);

    log.debug("Number of cases before filtering = " + encounters.size());

    /*
    Consider only the encounters that has not been ended and has started at least 24 hours earlier
    and has value 1.0 which represents currently in ICU
    */
    encounters = encounters.stream().filter(o -> o.dateTo() == null &&
        DateTools.dateToUnixTime(o.dateFrom()) <= periodFrom &&
        o.value() == 1.0).toList();

    log.debug("Number of cases after filtering = " + encounters.size());

    for (CoreBaseDataItem encounter : encounters) {
      String caseId = encounter.hisCaseId();
      log.trace("START: calculation for caseId: " + caseId);

      // Getting CVVH
      List<CoreBaseDataItem> cvvh = mapModelParameter.get(START_REPLACEMENT).stream()
          .filter(o -> o.hisCaseId().equals(caseId)).toList();
      //Doing calculation only if there is no CVVH against this caseId
      if (cvvh.size() > 0) {
        log.trace("CVVH found for caseId: " + caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      }

      // Getting CREATININE values
      List<CoreBaseDataItem> items = mapModelParameter.get(CREATININE).stream()
          .filter(o -> o.hisCaseId().equals(caseId)).toList();
      Double currentCreatinine = ValueOperations.getLatestValueInPeriod(items, periodFrom,
          periodTo);
      Double firstCreatinine = ValueOperations.getFirstValueInPeriod(items);
      //If null value is found then risk can't be calculated. So skipping further steps for this case.
      if (currentCreatinine == null || firstCreatinine == null) {
        log.trace("Missing Creatinine for caseId: " + caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      }

      // Getting UREA values
      items = mapModelParameter.get(UREA).stream().filter(o -> o.hisCaseId().equals(caseId))
          .toList();
      Double currentUrea = ValueOperations.getLatestValueInPeriod(items, periodFrom, periodTo);
      //If null value is found then risk can't be calculated. So skipping further steps for this case.
      if (currentUrea == null) {
        log.trace("Missing Urea for caseId: " + caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      }

      // Getting LACTATE values
      items = mapModelParameter.get(LACTATE).stream().filter(o -> o.hisCaseId().equals(caseId))
          .toList();
      Double currentLactate = ValueOperations.getLatestValueInPeriod(items, periodFrom, periodTo);
      //If null value is found then risk can't be calculated. So skipping further steps for this case.
      if (currentLactate == null) {
        log.trace("Missing Lactate for caseId: " + caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      }

      //Getting BODY_WEIGHT
      items = mapModelParameter.get(BODY_WEIGHT).stream().filter(o -> o.hisCaseId().equals(caseId))
          .toList();
      //If no bodyWeight is found then risk can't be calculated. So skipping further steps for this case.
      if (items == null || items.isEmpty()) {
        log.trace("Missing Body weight for caseId: " + caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      } else {
        bodyWeight = items.get(0).value();
      }

      // Getting URINE_OUTPUT values
      items = mapModelParameter.get(URINE_OUTPUT).stream().filter(o -> o.hisCaseId().equals(caseId))
          .toList();
      Double meanUrineOutput = ValueOperations.getMeanUrineValueInPeriod(items, bodyWeight,
          periodFrom, periodTo);
      //If null value is found then risk can't be calculated. So skipping further steps for this case.
      if (meanUrineOutput == null) {
        log.trace("Missing Urine for caseId: " + caseId);
        log.trace("END: calculation for caseId: " + caseId);
        continue;
      }

      //Calculating discriminant value
      Double risk = ValueOperations.getDiscriminantValue(currentCreatinine, firstCreatinine,
          currentUrea,
          currentLactate, meanUrineOutput);
      if (risk != null) {
        if (Double.isInfinite(risk)) {
          log.error("risk is infinite for caseId " + caseId);
        } else if (risk < -20.0 || risk > 50.0) {
          log.error(
              "Otlier found for caseId " + caseId
                  + ". Risk=" + risk
                  + ", periodFrom = " + periodFrom
                  + ", periodTo = " + periodTo
                  + ", currentCreatinine=" + currentCreatinine
                  + ", firstCreatinine=" + firstCreatinine
                  + ", currentUrea=" + currentUrea
                  + ", currentLactate=" + currentLactate
                  + ", meanUrineOutput=" + meanUrineOutput);
        }
        log.trace(
            "Risk = " + risk + " for caseId = " + caseId
                + ". periodFrom = " + periodFrom
                + ", periodTo = " + periodTo
                + ", currentCreatinine=" + currentCreatinine
                + ", firstCreatinine=" + firstCreatinine
                + ", currentUrea=" + currentUrea
                + ", currentLactate=" + currentLactate
                + ", meanUrineOutput=" + meanUrineOutput);
        resultList.add(risk);
      }
      log.trace("END: calculation for caseId: " + caseId);
    }

    log.debug("Number of cases that have all parameters available = " + resultList.size());

    return resultList;
  }
}
