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
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.KIRA_NUMBER_OF_CHARS_ZIP_CODES;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirPatientTools.isAddressContainingCountyCode;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.ChartListItem;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class CumulativeZipCode extends DashboardDataItemLogic {

  public static final String NULL_STRING = "null";

  /** Iterating over all the facility encounters that were initialized. */
  public List<String> createZipCodeList(
      List<UkbEncounter> facilityContacts, List<UkbPatient> patients) {
    return createZipCodeList(facilityContacts, null, patients, null);
  }

  /**
   * Method to create a list containing the zip code of each patient from Germany who has tested
   * positive for the given disease.
   */
  public List<String> createZipCodeList(
      List<UkbEncounter> facilityContacts,
      List<UkbEncounter> encounterSubSet,
      List<UkbPatient> patients,
      Integer charsToBeTrimmed) {
    // Log the start of the method
    log.debug("started createZipCodeList");
    // Start a timer to measure the method's execution time
    Instant startTime = TimerTools.startTimer();

    // Initialize the list to store zip codes
    List<String> results = new ArrayList<>();
    // Using all facility contact encounters as default
    if (encounterSubSet == null) {
      encounterSubSet = facilityContacts;
    }
    // Collect patient IDs of disease-positive cases
    Set<String> tempPidSet =
        encounterSubSet.parallelStream()
            .filter(EncounterFilter::isDiseasePositive)
            .map(UkbEncounter::getPatientId)
            .collect(Collectors.toSet());

    // Iterate over patients to retrieve their addresses
    patients.forEach(
        patient -> {
          // Check if the patient has tested positive for the given disease
          if (tempPidSet.contains(patient.getId())) {
            // Check if the patient has an address
            if (patient.hasAddress()) {
              // Retrieve the first address of the patient
              Address firstAddress = patient.getAddressFirstRep();
              // Check if the address is from Germany and has a postal code
              if (isAddressContainingCountyCode(firstAddress, COUNTRY_CODE.getValue())) {
                // Add the postal code to the result list
                String postalCode = firstAddress.getPostalCode();
                if (postalCode != null) {
                  // Trimming because of data protection to the given number of chars
                  if (charsToBeTrimmed != null && postalCode.length() > charsToBeTrimmed)
                    postalCode = postalCode.substring(0, charsToBeTrimmed);
                  results.add(postalCode);
                } else {
                  log.warn(
                      "Postal code is unexpectedly null for patient with id {}", patient.getId());
                  results.add(NULL_STRING);
                }
              } else {
                // Add "null" if the address is missing or not from Germany
                results.add(NULL_STRING);
              }
            } else {
              // Log a warning if the patient's address is missing
              log.warn(
                  "Patient resource with id {} has no address, but it's a mandatory field!",
                  patient.getId());
              // Add "null" to the result list
              results.add(NULL_STRING);
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

  public ChartListItem createKiRaKjpZipCodeList(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      List<UkbEncounter> facilityEncounters,
      List<UkbPatient> patients) {
    List<String> charts = new ArrayList<>();
    List<List<String>> values = new ArrayList<>();
    coreCaseDataByGroups.forEach(
        (context, coreCaseDataByCaseId) -> {
          charts.add(context);
          values.add(
              createZipCodeList(
                  null,
                  facilityEncounters.parallelStream()
                      .filter(x -> coreCaseDataByCaseId.containsKey(x.getFacilityContactId()))
                      .toList(),
                  patients,
                  KIRA_NUMBER_OF_CHARS_ZIP_CODES));
        });
    return new ChartListItem(charts, values);
  }

  public ChartListItem createKiRaRsvZipCodeList(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      List<UkbEncounter> facilityEncounters,
      List<UkbPatient> patients) {
    List<String> charts = List.of(RSV_DIAGNOSES_ALL);
    // For RSV all the values are aggregated in one array
    Set<String> facilityEncounterIds = new HashSet<>();
    List<List<String>> values = new ArrayList<>();

    // Fill a set with all the encounter ids that got at least one rsv diagnosis annotated
    coreCaseDataByGroups.forEach(
        (context, coreCaseDataByCaseId) ->
            facilityEncounterIds.addAll(
                coreCaseDataByCaseId.values().stream()
                    .map(CoreCaseData::getFacilityEncounterId)
                    .toList()));
    values.add(
        createZipCodeList(
            null,
            facilityEncounters.parallelStream()
                .filter(x -> facilityEncounterIds.contains(x.getFacilityContactId()))
                .toList(),
            patients,
            KIRA_NUMBER_OF_CHARS_ZIP_CODES));
    return new ChartListItem(charts, values);
  }

  public ChartListItem createKiRaZipCodeList(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      List<UkbEncounter> facilityEncounters,
      List<UkbPatient> patients) {
    switch (kidsRadarDataItemContext) {
      case KJP -> {
        return createKiRaKjpZipCodeList(coreCaseDataByGroups, facilityEncounters, patients);
      }
      case RSV -> {
        return createKiRaRsvZipCodeList(coreCaseDataByGroups, facilityEncounters, patients);
      }
    }
    return null;
  }
}
