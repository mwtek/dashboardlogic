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

import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;

/**
 * This class is used for generating the data item {@link DiseaseDataItem cumulative.zipcode}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CumulativeZipCode {

  List<UkbEncounter> listEncounters;
  List<UkbPatient> listPatients;

  public CumulativeZipCode(List<UkbEncounter> listEncounters, List<UkbPatient> listPatients) {
    this.listEncounters = listEncounters;
    this.listPatients = listPatients;
  }

  /**
   * Returns a list containing the zip code of each patient from germany.
   *
   * @return List with all zip codes of the given c19-positive patients.
   */
  public List<String> createZipCodeList() {
    log.debug("started createZipCodeList");
    Instant startTime = TimerTools.startTimer();
    Set<String> tempPidSet;
    List<String> listResult = new ArrayList<>();
    // get pids from all positive cases
    tempPidSet = listEncounters.parallelStream()
        .filter(x -> x.hasExtension(DashboardLogicFixedValues.POSITIVE_RESULT.getValue()))
        .map(UkbEncounter::getPatientId).collect(
            Collectors.toSet());

    // Store zip codes from all positive cases
    for (UkbPatient patient : listPatients) {
      if (tempPidSet.contains(patient.getId())) {
        if (patient.hasAddress()) {
          // Since 'Strassenanschrift' and 'Postfach' got the same type in the kds profile it
          // should be fine to take the first entry.
          Address firstAddress = patient.getAddressFirstRep();
          if (firstAddress.hasPostalCode() && firstAddress.hasCountry()
              && firstAddress.getCountry()
              .equals(DashboardLogicFixedValues.COUNTRY_CODE.getValue())) {
            listResult.add(firstAddress.getPostalCode());
          } else {
            listResult.add("null");
          }
        } else {
          // Usually we never should end here since it's a non-valid person resource then.
          // (address is 1..*)
          log.warn("Patient resource with id " + patient.getId()
              + " got no address, but its a mandatory field! ");
          listResult.add("null");
        }
      }
    }
    Collections.sort(listResult);
    TimerTools.stopTimerAndLog(startTime, "finished createZipCodeList");
    return listResult;
  }
}
