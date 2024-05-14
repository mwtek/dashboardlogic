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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.COUNTRY_CODE;

import de.ukbonn.mwtek.dashboardlogic.logic.DashboardDataItemLogics;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
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
public class CumulativeZipCode extends DashboardDataItemLogics {

  /**
   * Method to create a list containing the zip code of each patient from Germany who has tested
   * positive for the given disease.
   */
  public List<String> createZipCodeList() {
    // Log the start of the method
    log.debug("started createZipCodeList");
    // Start a timer to measure the method's execution time
    Instant startTime = TimerTools.startTimer();

    // Initialize the list to store zip codes
    List<String> results = new ArrayList<>();

    // Collect patient IDs of disease-positive cases
    Set<String> tempPidSet = getFacilityContactEncounters()
        .parallelStream()
        .filter(EncounterFilter::isDiseasePositive)
        .map(UkbEncounter::getPatientId)
        .collect(Collectors.toSet());

    // Iterate over patients to retrieve their addresses
    getPatients().forEach(patient -> {
      // Check if the patient has tested positive for the given disease
      if (tempPidSet.contains(patient.getId())) {
        // Check if the patient has an address
        if (patient.hasAddress()) {
          // Retrieve the first address of the patient
          Address firstAddress = patient.getAddressFirstRep();
          // Check if the address is from Germany and has a postal code
          if (firstAddress != null && firstAddress.hasPostalCode() && firstAddress.hasCountry()
              && firstAddress.getCountry().equals(COUNTRY_CODE.getValue())) {
            // Add the postal code to the result list
            results.add(firstAddress.getPostalCode());
          } else {
            // Add "null" if the address is missing or not from Germany
            results.add("null");
          }
        } else {
          // Log a warning if the patient's address is missing
          log.warn("Patient resource with id " + patient.getId()
              + " has no address, but it's a mandatory field!");
          // Add "null" to the result list
          results.add("null");
        }
      }
    });

    // Sort the list of zip codes
    Collections.sort(results);
    // Stop the timer and log the end of the method
    TimerTools.stopTimerAndLog(startTime, "finished createZipCodeList");
    // Return the list of zip codes
    return results;
  }
}
