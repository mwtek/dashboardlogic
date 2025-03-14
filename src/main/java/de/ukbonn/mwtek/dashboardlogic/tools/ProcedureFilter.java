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
 */ package de.ukbonn.mwtek.dashboardlogic.tools;

import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcedureFilter {

  /**
   * Filters ICU procedures based on whether the linked case contains at least one icu stay in its
   * supply contact encounter resource.
   */
  public static List<UkbProcedure> filterProceduresByIcuWardCheck(
      List<UkbProcedure> icuProcedures, List<UkbEncounter> icuSupplyContactEncounters) {

    // Extract all facility contact IDs from ICU encounters
    Set<String> facilityContactIds =
        icuSupplyContactEncounters.parallelStream()
            .map(UkbEncounter::getFacilityContactId)
            .collect(Collectors.toSet());

    // Identify procedures that do not have a matching ICU stay
    List<String> caseIdsWithoutIcuStay =
        icuProcedures.parallelStream()
            .map(UkbProcedure::getCaseId)
            .filter(caseId -> !facilityContactIds.contains(caseId))
            .toList();

    // Log the number of filtered procedures and provide example case IDs
    if (!caseIdsWithoutIcuStay.isEmpty()) {
      log.info(
          "{} procedures filtered since no ICU stay was found on the linked encounter. Examples:"
              + " {}",
          caseIdsWithoutIcuStay.size(),
          caseIdsWithoutIcuStay.subList(0, Math.min(3, caseIdsWithoutIcuStay.size())));
    }

    // Return only procedures that have a corresponding ICU stay
    return icuProcedures.parallelStream()
        .filter(procedure -> facilityContactIds.contains(procedure.getCaseId()))
        .toList();
  }
}
