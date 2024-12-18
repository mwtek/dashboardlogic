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

package de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.misc;

import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Class for all value determination operations necessary for the applications of the model formula.
 */
@Slf4j
public class ValueOperations {

  public static final int dayInSeconds = 24 * 60 * 60;

  public static Double getDiscriminantValue(
      Double currentCreatinine,
      Double firstCreatinine,
      Double currentUrea,
      Double currentLactate,
      Double meanUrineOutput) {
    // Calculating discriminant value
    try {
      return -4.7023
          + (1.7247 * (currentCreatinine / firstCreatinine))
          + (0.0351 * currentUrea)
          + (0.6647 * currentLactate)
          - (0.4413 * meanUrineOutput);
    } catch (Exception e) {
      log.trace(e.toString());
      return null;
    }
  }

  public static Double getLatestValueInPeriod(
      Collection<CoreBaseDataItem> coreBaseDataItemList, long periodFrom, long periodTo) {
    if (coreBaseDataItemList == null || coreBaseDataItemList.isEmpty()) {
      return null;
    }
    // Filtering the items within the given time frame
    var itemsWithinTimeFrame =
        coreBaseDataItemList.stream()
            .filter(
                o ->
                    DateTools.dateToUnixTime(o.dateFrom()) >= periodFrom
                        && DateTools.dateToUnixTime(o.dateFrom()) <= periodTo)
            .toList();
    if (itemsWithinTimeFrame.isEmpty()) {
      return null;
    }

    // Returning latest value
    var latestObject = Collections.max(itemsWithinTimeFrame);
    return latestObject.value();
  }

  public static CoreBaseDataItem findClosestObject(List<CoreBaseDataItem> list, long targetValue) {
    CoreBaseDataItem closestObject = null;
    long minDifference = Long.MAX_VALUE;

    for (CoreBaseDataItem obj : list) {
      long difference = Math.abs(DateTools.dateToUnixTime(obj.dateFrom()) - targetValue);
      if (difference < minDifference) {
        minDifference = difference;
        closestObject = obj;
      }
    }

    return closestObject;
  }

  public static Double getClosestValueToMid(
      Collection<CoreBaseDataItem> coreBaseDataItemList, long periodFrom, long periodTo) {
    if (coreBaseDataItemList == null || coreBaseDataItemList.isEmpty()) {
      return null;
    }
    // Filtering the items within the given time frame
    var itemsWithinTimeFrame =
        coreBaseDataItemList.stream()
            .filter(
                o ->
                    DateTools.dateToUnixTime(o.dateFrom()) >= periodFrom
                        && DateTools.dateToUnixTime(o.dateFrom()) <= periodTo)
            .toList();
    if (itemsWithinTimeFrame.isEmpty()) {
      return null;
    }

    // Getting the closest object to mid of the given time period
    var closestObjectToMid = findClosestObject(itemsWithinTimeFrame, ((periodTo + periodFrom) / 2));
    if (closestObjectToMid != null) {
      // If the closest object is within 3 hours of periodTo then return null
      int threeHoursInSeconds = 3 * 60 * 60;
      if ((periodTo - DateTools.dateToUnixTime(closestObjectToMid.dateFrom()))
          < threeHoursInSeconds) {
        return null;
      }
      return closestObjectToMid.value();
    } else {
      return null;
    }
  }

  public static Double getFirstValueInPeriod(Collection<CoreBaseDataItem> coreBaseDataItemList) {
    if (coreBaseDataItemList == null || coreBaseDataItemList.isEmpty()) {
      return null;
    }

    // Returning oldest value
    var oldestObject = Collections.min(coreBaseDataItemList);
    return oldestObject.value();
  }

  public static Double getMeanUrineValueInPeriod(
      Collection<CoreBaseDataItem> coreBaseDataItemList,
      Double bodyWeight,
      long periodFrom,
      long periodTo) {
    if (coreBaseDataItemList == null
        || coreBaseDataItemList.isEmpty()
        || bodyWeight == null
        || bodyWeight <= 0) {
      return null;
    }

    // Filtering the items within periodFrom and periodTo and sort it
    var itemsWithinPeriod =
        coreBaseDataItemList.stream()
            .filter(
                o ->
                    DateTools.dateToUnixTime(o.dateFrom()) >= periodFrom
                        && DateTools.dateToUnixTime(o.dateFrom()) <= periodTo)
            .sorted()
            .toList();
    if (itemsWithinPeriod.isEmpty()) {
      return null;
    }

    // At least 2 entries has to be present for calculation
    if (itemsWithinPeriod.size() < 2) {
      return null;
    }

    // Calculating mean
    double totalValue = 0;
    for (int i = 1; i < itemsWithinPeriod.size(); i++) {
      totalValue = totalValue + itemsWithinPeriod.get(i).value();
    }

    // return value per hour per kg bodyWeight
    return totalValue
        / (((DateTools.dateToUnixTime(
                        itemsWithinPeriod.get(itemsWithinPeriod.size() - 1).dateFrom())
                    - DateTools.dateToUnixTime(itemsWithinPeriod.get(0).dateFrom()))
                / 3600.0)
            * bodyWeight);
  }

  public static Long getMaxValueLowerThan(List<Long> list, Long value) {
    return list.stream().filter(n -> n < value).max(Long::compare).orElse(null);
  }
}
