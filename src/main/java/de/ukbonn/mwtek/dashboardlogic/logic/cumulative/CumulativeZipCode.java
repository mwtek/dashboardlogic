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

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link CoronaDataItem cumulative.zipcode}
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

@Slf4j
public class CumulativeZipCode {

  List<UkbEncounter> listEncounters = new ArrayList<>();
  List<UkbPatient> listPatients = new ArrayList<>();

  public CumulativeZipCode(List<UkbEncounter> listEncounters, List<UkbPatient> listPatients) {
    this.listEncounters = listEncounters;
    this.listPatients = listPatients;
  }

  /**
   * Returns a list containing the postcode of each patient from Germany.
   *
   * @return List with all zipcodes of the given c19-positive patients
   */
  public List<String> createZipCodeList() {
    log.debug("started createZipCodeList");
    Instant startTime = TimerTools.startTimer();
    Set<String> tempPidSet = new HashSet<>();
    List<String> listResult = new ArrayList<>();
    // get pids from all positive cases
    listEncounters.forEach(encounter -> {
      if (encounter.hasExtension(CoronaFixedValues.POSITIVE_RESULT.getValue())) {
        tempPidSet.add(encounter.getPatientId());
      }
    });
    // save addresses from all positive cases
    for (UkbPatient patient : listPatients) {
      if (tempPidSet.contains(patient.getId())) {
        if (patient.hasAddress() && patient.getAddress().get(0).getPostalCode() != null) {
          if (patient.getAddress().get(0).getCountry()
              .equals(CoronaFixedValues.COUNTRY_CODE.getValue())) {
            listResult.add(patient.getAddress().get(0).getPostalCode());
          } else {
            listResult.add("null");
          }
        } else {
          listResult.add("null");
        }
      }
    }
    Collections.sort(listResult);
    TimerTools.stopTimerAndLog(startTime, "finished createZipCodeList");
    return listResult;
  }

}
