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
package de.ukbonn.mwtek.dashboardlogic.logic.current.age;

import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Patient;

/**
 * This class is used for generating the data item {@link CoronaDataItem
 * current.maxtreatmentlevel.age.* with subitems like *.normal_ward and *.age.icu}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */

public record CurrentMaxTreatmentLevelAge(List<UkbPatient> listPatients,
                                          List<UkbEncounter> listCurrentMaxEncounter) {

  public List<Long> createCurrentMaxAgeMap(
      HashMap<String, List<UkbEncounter>> mapPositiveEncounterByClass) {
    List<Long> resultList = new ArrayList<>();
    Map<String, UkbEncounter> currentMaxPidAdmissionMap = new HashMap<>();

    List<String> currentMaxEncounterPidList =
        listCurrentMaxEncounter.stream().map(UkbEncounter::getPatientId).toList();

    for (Map.Entry<String, List<UkbEncounter>> entry : mapPositiveEncounterByClass.entrySet()) {
      for (UkbEncounter encounter : entry.getValue()) {
        if (currentMaxEncounterPidList.contains(encounter.getPatientId())) {
          CoronaResultFunctionality.assignFirstAdmissionDateToPid(encounter,
              currentMaxPidAdmissionMap);
        }
      }
    }
    calculateAndCheckAgeGroup(currentMaxPidAdmissionMap, resultList, listPatients);
    Collections.sort(resultList);
    return resultList;
  }

  private static void calculateAndCheckAgeGroup(Map<String, UkbEncounter> pidAdmissionMap,
      List<Long> resultList, List<UkbPatient> listPatients) {
    // Filter the patients with no birthday (usually these resources are non-valid anyway)
    List<UkbPatient> patientsWithBirthday =
        listPatients.stream().filter(Patient::hasBirthDate).toList();

    for (Map.Entry<String, UkbEncounter> entry : pidAdmissionMap.entrySet()) {
      Date birthdayPatient = patientsWithBirthday.stream()
          .filter(patient -> entry.getValue().getPatientId().equals(patient.getId()))
          .map(UkbPatient::getBirthDate).findFirst().orElse(null);

      if (entry.getValue().isPeriodStartExistent() && birthdayPatient != null) {
        int age = CoronaResultFunctionality.calculateAge(birthdayPatient,
            entry.getValue().getPeriod().getStart());
        long cohortAge = CoronaResultFunctionality.checkAgeGroup(age);
        resultList.add(cohortAge);
      }
    }
  }
}
