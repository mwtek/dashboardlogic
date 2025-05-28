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
 */ package de.ukbonn.mwtek.dashboardlogic.logic;

import static de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortIcdCodes.EXCLUSION_CRITERIA_COHORT_1;
import static de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortIcdCodes.EXCLUSION_CRITERIA_COHORT_2;
import static de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortIcdCodes.EXCLUSION_CRITERIA_COHORT_3;
import static de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortIcdCodes.INCLUSION_CRITERIA_COHORT_1;
import static de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortIcdCodes.INCLUSION_CRITERIA_COHORT_2;

import de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortIcdCodes;
import de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortOpsCodes;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CohortLogic {

  public static PidTimestampCohortMap getCohort1(
      Map<String, Set<String>> pidDiagnoses, List<UkbConsent> consents) {

    return buildCohort(
        pidDiagnoses,
        null,
        consents,
        INCLUSION_CRITERIA_COHORT_1,
        null,
        EXCLUSION_CRITERIA_COHORT_1);
  }

  public static PidTimestampCohortMap getCohort2(
      Map<String, Set<String>> pidDiagnoses, List<UkbConsent> consents) {

    return buildCohort(
        pidDiagnoses,
        null,
        consents,
        INCLUSION_CRITERIA_COHORT_2,
        null,
        EXCLUSION_CRITERIA_COHORT_2);
  }

  public static PidTimestampCohortMap getCohort3(
      Map<String, Set<String>> pidDiagnoses,
      Map<String, Set<String>> pidProcedures,
      List<UkbConsent> consents) {

    return buildCohort(
        pidDiagnoses,
        pidProcedures,
        consents,
        AcribisCohortIcdCodes.INCLUSION_CRITERIA_COHORT_3,
        AcribisCohortOpsCodes.INCLUSION_CRITERIA_COHORT_3,
        EXCLUSION_CRITERIA_COHORT_3);
  }

  public static PidTimestampCohortMap buildCohort(
      Map<String, Set<String>> pidDiagnoses,
      Map<String, Set<String>> pidProcedures,
      List<UkbConsent> consents,
      List<String> inclusionDiagnoses,
      List<String> inclusionProcedures,
      List<String> exclusionDiagnoses) {

    PidTimestampCohortMap pidTimestampCohortMap = new PidTimestampCohortMap();

    Map<String, UkbConsent> firstConsentByPid =
        consents.stream()
            .filter(c -> c.getPatientId() != null && c.getAcribisPermitStartDate() != null)
            .collect(
                Collectors.toMap(
                    UkbConsent::getPatientId,
                    c -> c,
                    (existing, replacement) -> existing)); // Keep first

    for (String pid : pidDiagnoses.keySet()) {
      Set<String> diagnoses = pidDiagnoses.getOrDefault(pid, new HashSet<>());
      Set<String> procedures =
          pidProcedures != null
              ? pidProcedures.getOrDefault(pid, new HashSet<>())
              : new HashSet<>();

      boolean hasInclusion =
          matchesInclusionCriteria(diagnoses, inclusionDiagnoses)
              || matchesInclusionCriteria(procedures, inclusionProcedures);

      if (hasInclusion && firstConsentByPid.containsKey(pid)) {
        boolean hasExclusion = matchesInclusionCriteria(diagnoses, exclusionDiagnoses);
        if (!hasExclusion) {
          pidTimestampCohortMap.put(pid, firstConsentByPid.get(pid).getAcribisPermitStartDate());
        }
      }
    }

    return pidTimestampCohortMap;
  }

  private static boolean matchesInclusionCriteria(
      Collection<String> codes, Collection<String> criteriaPrefixes) {
    return codes != null
        && criteriaPrefixes != null
        && codes.stream().anyMatch(code -> criteriaPrefixes.stream().anyMatch(code::startsWith));
  }
}
