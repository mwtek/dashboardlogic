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
package de.ukbonn.mwtek.dashboardlogic.logic;

import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByContext;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getVariantObservationsByContext;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardDataItemLogics {

  @Getter
  private static List<UkbEncounter> encounters;
  @Getter
  private static List<UkbPatient> patients;
  @Getter
  private static List<UkbObservation> observations;
  @Getter
  private static List<UkbProcedure> icuProcedures;
  @Getter
  private static List<UkbObservation> variantObservations;
  @Getter
  private static InputCodeSettings inputCodeSettings;
  @Getter
  private static List<UkbLocation> locations;
  @Getter
  private static List<UkbEncounter> supplyContactEncounters;
  @Getter
  private static List<UkbEncounter> facilityContactEncounters;

  /**
   * Initialization of often used data collections.
   */
  public static void initializeData(InputCodeSettings inputCodeSettings,
      List<UkbEncounter> encounters, List<UkbPatient> patients, List<UkbObservation> observations,
      List<UkbLocation> locations, List<UkbProcedure> icuProcedures,
      DataItemContext dataItemContext) {
    DashboardDataItemLogics.inputCodeSettings = inputCodeSettings;
    DashboardDataItemLogics.encounters = encounters;
    DashboardDataItemLogics.patients = patients;
    DashboardDataItemLogics.icuProcedures = icuProcedures;
    DashboardDataItemLogics.observations = new ArrayList<>(getObservationsByContext(observations,
        getInputCodeSettings(), dataItemContext));
    Set<UkbObservation> variantObservationsByContext = getVariantObservationsByContext(observations,
        getInputCodeSettings(), dataItemContext);
    if (variantObservationsByContext != null) {
      DashboardDataItemLogics.variantObservations = new ArrayList<>(variantObservationsByContext);
    }
    DashboardDataItemLogics.locations = locations;
    if (encounters != null) {
      DashboardDataItemLogics.facilityContactEncounters = encounters.parallelStream()
          .filter(UkbEncounter::isFacilityContact).toList();
      DashboardDataItemLogics.supplyContactEncounters = encounters.parallelStream()
          .filter(UkbEncounter::isSupplyContact).toList();
    }
  }

}
