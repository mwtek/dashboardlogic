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

import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;

import de.ukbonn.mwtek.dashboardlogic.DataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaTreatmentLevelExport;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;

/**
 * This class contains all the functions that could be important in several of the sub-logics
 * (current, cumulative and temporal)
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CoronaResultFunctionality extends DiseaseResultFunctionality {

  /**
   * Creation of a cross table listing the current patients at the UKB site according to current
   * level of care and place of residence in Bonn or outside Bonn.
   *
   * @param mapCumulativeMaxTreatments Map with the current inpatient c19 positive cases and their
   *     maxtreatment level
   * @param listPatient List with the patient data for the given encounter
   * @return List with the crosstab states and their values
   */
  public static List<String[]> generateCrosstabList(
      Map<TreatmentLevels, List<UkbEncounter>> mapCumulativeMaxTreatments,
      List<UkbPatient> listPatient) {
    log.debug("started generateCrosstabList");
    Instant startTimer = TimerTools.startTimer();
    List<String[]> resultList = new ArrayList<>();
    List<String> listOnlyBonn = new ArrayList<>();
    List<String> listBonnAndIcu = new ArrayList<>();
    List<String> listBonnAndVent = new ArrayList<>();
    List<String> listBonnAndEcmo = new ArrayList<>();
    List<String> listNoBonn = new ArrayList<>();
    List<String> listNoBonnAndIcu = new ArrayList<>();
    List<String> listNoBonnAndVent = new ArrayList<>();
    List<String> listNoBonnAndEcmo = new ArrayList<>();
    Map<String, Boolean> mapIsBonnPatient = new HashMap<>();
    // go through each patient check if they are living in Bonn
    for (UkbPatient patient : listPatient) {
      try {
        Address address = patient.getAddressFirstRep();
        mapIsBonnPatient.put(
            patient.getId(),
            address.getCity() != null
                && address.getCity().equals(DashboardLogicFixedValues.CITY_BONN.getValue()));
      } catch (Exception e) {
        log.warn("Patient: " + patient.getId() + " got no address/city");
      }
    }
    // iterate through each encounter, and sort them to the right list
    for (Map.Entry<TreatmentLevels, List<UkbEncounter>> entry :
        mapCumulativeMaxTreatments.entrySet()) {
      TreatmentLevels key = entry.getKey();
      for (UkbEncounter encounter : entry.getValue()) {
        if (mapIsBonnPatient.containsKey(encounter.getPatientId())) {
          boolean isBonn = mapIsBonnPatient.get(encounter.getPatientId());
          switch (key) {
            case INPATIENT -> {
              if (isBonn) {
                listOnlyBonn.add(encounter.getId());
              } else {
                listNoBonn.add(encounter.getId());
              }
            }
            case ICU -> {
              if (isBonn) {
                listBonnAndIcu.add(encounter.getId());
              } else {
                listNoBonnAndIcu.add(encounter.getId());
              }
            }
            case ICU_VENTILATION -> {
              if (isBonn) {
                listBonnAndVent.add(encounter.getId());
              } else {
                listNoBonnAndVent.add(encounter.getId());
              }
            }
            case ICU_ECMO -> {
              if (isBonn) {
                listBonnAndEcmo.add(encounter.getId());
              } else {
                listNoBonnAndEcmo.add(encounter.getId());
              }
            }
          }
        }
      }
    }
    resultList.add(listNoBonn.toArray(new String[0]));
    resultList.add(listNoBonnAndIcu.toArray(new String[0]));
    resultList.add(listNoBonnAndVent.toArray(new String[0]));
    resultList.add(listNoBonnAndEcmo.toArray(new String[0]));
    resultList.add(listOnlyBonn.toArray(new String[0]));
    resultList.add(listBonnAndIcu.toArray(new String[0]));
    resultList.add(listBonnAndVent.toArray(new String[0]));
    resultList.add(listBonnAndEcmo.toArray(new String[0]));
    TimerTools.stopTimerAndLog(startTimer, "finished generateCrosstabList");

    return resultList;
  }

  /**
   * Export of a csv file that displays a list of case/encounter numbers of active cases separated
   * by treatment level when run through.
   *
   * @param mapCurrentTreatmentlevelCasenrs {@link
   *     DataItemGenerator#getMapCurrentTreatmentlevelCasenrs() Map} with the current case/encounter
   *     ids by treatment level.
   * @param exportDirectory The directory to export to (e.g.: "C:\currentTreatmentlevelExport").
   * @param fileBaseName The base file name of the generated file (e.g.:
   *     "Caseids_inpatient_covid19_patients").
   */
  @SuppressWarnings("unused")
  public static void generateCurrentTreatmentLevelList(
      Map<String, List<String>> mapCurrentTreatmentlevelCasenrs,
      String exportDirectory,
      String fileBaseName) {

    CoronaTreatmentLevelExport treatmentLevelExport =
        new CoronaTreatmentLevelExport(
            mapCurrentTreatmentlevelCasenrs.get(NORMAL_WARD.getValue()),
            mapCurrentTreatmentlevelCasenrs.get(ICU.getValue()),
            mapCurrentTreatmentlevelCasenrs.get(ICU_VENTILATION.getValue()),
            mapCurrentTreatmentlevelCasenrs.get(ICU_ECMO.getValue()));

    String currentDate = new SimpleDateFormat("yyyy-MM-dd-HHmm").format(new Date());
    try (PrintWriter out =
        new PrintWriter(exportDirectory + "\\" + fileBaseName + "_" + currentDate + ".csv")) {
      out.println(treatmentLevelExport.toCsv());
    } catch (FileNotFoundException fnf) {
      log.error(
          "Unable to export file with the current treatment levels, probably because the target "
              + "directory cant be created: "
              + fnf.getMessage());
    }
  }

  /**
   * Increase the passed covid variant frequency by one.
   *
   * @param variantMap Map that assigns the variant names, their (current) frequency.
   * @param variantName Name of the variant (As defined in the ValueSet of the corresponding Data
   *     item).
   */
  public static void incrementVariantCount(Map<String, Integer> variantMap, String variantName) {
    // Merge has the advantage that the fields with the numbers in the map do not have to be
    // initialized and the map is called only once.
    variantMap.merge(variantName, 1, Integer::sum);
  }
}
