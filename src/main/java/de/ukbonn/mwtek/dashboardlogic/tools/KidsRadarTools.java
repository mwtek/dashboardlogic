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
package de.ukbonn.mwtek.dashboardlogic.tools;

import de.ukbonn.mwtek.dashboardlogic.models.CoreCaseData;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KidsRadarTools {

  /** The icd codes for all kids radar groups in one list. */
  public static List<String> getIcdCodesAsString(
      Map<String, List<String>> kidsRadarDiagnosisIcdCodes) {
    return kidsRadarDiagnosisIcdCodes.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  public static List<UkbPatient> removeEntriesByAge(
      Map<String, CoreCaseData> coreCaseDataByEncounterId, List<UkbPatient> patientsFiltered) {
    Set<String> validPids = new HashSet<>();
    Set<String> encounterIdsToBeRemoved = new HashSet<>();
    coreCaseDataByEncounterId.forEach(
        (encounterId, coreCaseData) -> {
          if (coreCaseData.getAgeAtAdmission() >= 18) {
            log.trace(
                "{} will get removed from further calculations. Admission date: {} Birthday: {}",
                encounterId,
                coreCaseData.getAdmissionDate(),
                coreCaseData.getPatient().getBirthDate());
            encounterIdsToBeRemoved.add(encounterId);
          } else {
            // Current encounter age is fine -> add pid to a list to filter it later to the valid
            // ones
            validPids.add(coreCaseData.getPatientId());
          }
        });
    log.info(
        "{}/{} encounters got removed ",
        encounterIdsToBeRemoved.size(),
        coreCaseDataByEncounterId.size());
    encounterIdsToBeRemoved.forEach(coreCaseDataByEncounterId::remove);
    return patientsFiltered.parallelStream().filter(x -> validPids.contains(x.getId())).toList();
  }
}
