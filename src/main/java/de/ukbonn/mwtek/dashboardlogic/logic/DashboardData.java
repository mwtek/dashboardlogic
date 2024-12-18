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

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.ALL_DISORDERS;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.ALL_ICD_CODES;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.MEAN_LENGTH_OF_STAY;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getObservationsByContext;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.getVariantObservationsByContext;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class DashboardData {

  @Getter private List<UkbEncounter> encounters;
  @Getter private List<UkbPatient> patients;
  @Getter private List<UkbObservation> observations;
  @Getter private List<UkbProcedure> icuProcedures;
  @Getter private List<UkbCondition> conditions;
  @Getter private List<UkbObservation> variantObservations;
  @Getter private InputCodeSettings inputCodeSettings;
  @Getter private QualitativeLabCodesSettings qualitativeLabCodesSettings;
  @Getter private List<UkbLocation> locations;
  @Getter private List<UkbEncounter> supplyContactEncounters;
  @Getter private List<UkbEncounter> facilityContactEncounters;

  private static Map<String, Integer> encounterAgeMap = null;
  private static boolean encounterAgeMapInitialized = false; // Flag to check if map is initialized
  private static AtomicReference<Map<String, Integer>> encounterAgeMapRef =
      new AtomicReference<>(null);

  public static final List<String> LENGTH_OF_STAY_STACKS = Arrays.asList(MEAN_LENGTH_OF_STAY);
  public static final List<String> DISORDERS_CHARTS = List.of(ALL_DISORDERS);
  public static final List<String> ALL_ICD_CODES_CHARTS = List.of(ALL_ICD_CODES);

  /** Initialization of often used data collections. */
  public DashboardData initializeData(
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      List<UkbEncounter> encounters,
      List<UkbPatient> patients,
      List<UkbObservation> observations,
      List<UkbCondition> conditions,
      List<UkbLocation> locations,
      List<UkbProcedure> icuProcedures,
      DataItemContext dataItemContext) {
    this.inputCodeSettings = inputCodeSettings;
    this.qualitativeLabCodesSettings = qualitativeLabCodesSettings;
    this.encounters = encounters;
    this.patients = patients;
    this.icuProcedures = icuProcedures;
    this.conditions = conditions;
    if (observations != null) {
      this.observations =
          new ArrayList<>(
              getObservationsByContext(observations, getInputCodeSettings(), dataItemContext));
      Set<UkbObservation> variantObservationsByContext =
          getVariantObservationsByContext(observations, getInputCodeSettings(), dataItemContext);
      if (variantObservationsByContext != null) {
        this.variantObservations = new ArrayList<>(variantObservationsByContext);
      }
    }
    this.locations = locations;
    if (encounters != null) {
      this.facilityContactEncounters =
          encounters.parallelStream().filter(UkbEncounter::isFacilityContact).toList();
      this.supplyContactEncounters =
          encounters.parallelStream().filter(UkbEncounter::isSupplyContact).toList();
    }
    return this;
  }
}
