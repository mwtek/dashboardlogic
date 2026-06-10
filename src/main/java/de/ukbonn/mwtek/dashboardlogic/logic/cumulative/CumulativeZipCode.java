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
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.KIRA_PED_NUMBER_OF_CHARS_ZIP_CODES;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.getRsvOnlyCoreCaseDataByGroups;
import static de.ukbonn.mwtek.utilities.fhir.misc.FhirPatientTools.isAddressContainingCountyCode;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.ChartListItem;
import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.tools.EncounterFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
      List<MiiEncounter> facilityContacts, List<MiiPatient> patients) {
    return createZipCodeList(facilityContacts, null, patients, null, true);
  }

  /**
   * Method to create a list containing the zip code of each patient from Germany who has tested
   * positive for the given disease.
   */
  public static List<String> createZipCodeList(
      List<MiiEncounter> facilityContacts,
      List<MiiEncounter> encounterSubSet,
      List<MiiPatient> patients,
      Integer charsToBeTrimmed,
      boolean applyDiseasePositiveFilter) {
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
    // Collect patient IDs, applying filter only if required
    Stream<MiiEncounter> encounterStream = encounterSubSet.parallelStream();
    if (applyDiseasePositiveFilter) {
      // No need to check for disease-positive in the kira project
      encounterStream = encounterStream.filter(EncounterFilter::isDiseasePositive);
    }

    // Create a set of patient IDs from the filtered encounter stream
    Set<String> tempPidSet =
        encounterStream.map(MiiEncounter::getPatientId).collect(Collectors.toSet());

    // Build a map of all patients for fast lookup by ID
    Map<String, MiiPatient> patientMap =
        patients.stream()
            .collect(Collectors.toMap(MiiPatient::getId, Function.identity(), (a, b) -> a));

    // Get only the relevant patients (i.e., those who are disease-positive)
    List<MiiPatient> relevantPatients =
        tempPidSet.stream().map(patientMap::get).filter(Objects::nonNull).toList();

    // Iterate over patients to retrieve their addresses
    relevantPatients.forEach(
        patient -> {
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
        });

    // Sort the list of zip codes
    Collections.sort(results);
    // Stop the timer and log the end of the method
    TimerTools.stopTimerAndLog(startTime, "finished createZipCodeList");
    // Return the list of zip codes
    return results;
  }

  @Deprecated(since = "V0.5.5")
  public ChartListItem createKiRaKjpZipCodeList(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      List<MiiEncounter> facilityEncounters,
      List<MiiPatient> patients) {
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
                  KIRA_NUMBER_OF_CHARS_ZIP_CODES,
                  true));
        });
    return new ChartListItem(charts, values);
  }

  public ChartListItem createKiRaRsvZipCodeList(
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      List<MiiEncounter> facilityEncounters,
      List<MiiPatient> patients) {
    Map<String, Map<String, CoreCaseData>> rsvOnly =
        getRsvOnlyCoreCaseDataByGroups(coreCaseDataByGroups);
    List<String> charts = new ArrayList<>(List.of(RSV_DIAGNOSES_ALL));
    charts.addAll(rsvOnly.keySet());
    // For RSV, all the values are aggregated in one array
    List<List<String>> values = new ArrayList<>();

    // --- 1) "ALL" aggregated over all RSV groups ---
    // Collect encounter IDs that have any RSV diagnosis
    Set<String> allRsvEncounterIds =
        rsvOnly.values().stream()
            .filter(Objects::nonNull)
            .flatMap(map -> map.values().stream())
            .map(CoreCaseData::getFacilityEncounterId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    values.add(
        createZipCodeList(
            null,
            facilityEncounters.parallelStream()
                .filter(enc -> allRsvEncounterIds.contains(enc.getFacilityContactId()))
                .toList(),
            patients,
            KIRA_PED_NUMBER_OF_CHARS_ZIP_CODES,
            true));

    // --- 2) One run per RSV key, in the same order as charts ---
    for (Map.Entry<String, Map<String, CoreCaseData>> entry : rsvOnly.entrySet()) {
      Map<String, CoreCaseData> perGroup = entry.getValue();
      Set<String> perGroupEncounterIds =
          (perGroup == null)
              ? Set.of()
              : perGroup.values().stream()
                  .map(CoreCaseData::getFacilityEncounterId)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet());

      values.add(
          createZipCodeList(
              null,
              facilityEncounters.parallelStream()
                  .filter(enc -> perGroupEncounterIds.contains(enc.getFacilityContactId()))
                  .toList(),
              patients,
              KIRA_PED_NUMBER_OF_CHARS_ZIP_CODES,
              true));
    }

    return new ChartListItem(charts, values);
  }

  public ChartListItem createKiRaZipCodeList(
      KidsRadarDataItemContext kidsRadarDataItemContext,
      Map<String, Map<String, CoreCaseData>> coreCaseDataByGroups,
      List<MiiEncounter> facilityEncounters,
      List<MiiPatient> patients) {
    switch (kidsRadarDataItemContext) {
      case KJP -> {
        // Not needed anymore since version 0.5.5
        // return createKiRaKjpZipCodeList(coreCaseDataByGroups, facilityEncounters, patients);
      }
      case PED -> {
        return createKiRaRsvZipCodeList(coreCaseDataByGroups, facilityEncounters, patients);
      }
    }
    return null;
  }
}
