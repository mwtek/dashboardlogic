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
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class KiraData extends DashboardData {

  public List<UkbEncounter> facilityEncountersRsv = new ArrayList<>();

  public KiraData initializeData(
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      List<UkbEncounter> facilityEncountersKjp,
      List<UkbEncounter> facilityEncountersRsv,
      List<UkbPatient> patients,
      List<UkbObservation> observations,
      List<UkbCondition> conditions,
      List<UkbLocation> locations,
      List<UkbProcedure> icuProcedures,
      DataItemContext dataItemContext) {

    // Initialization with kjp facility encounters as default
    super.initializeData(
        inputCodeSettings,
        qualitativeLabCodesSettings,
        facilityEncountersKjp,
        patients,
        observations,
        conditions,
        locations,
        icuProcedures,
        dataItemContext);
    this.facilityEncountersRsv = facilityEncountersRsv;
    return this;
  }

  /**
   * Creates the subitem 'stacks' in a data-item based on a list, that is probably given via
   * yaml-file. This is why there is a replacement of all occurrences of the hyphen character ('-')
   * with an underscore ('_') so that the config entries fit with the result json format.
   */
  public static List<List<String>> createLabelListNested(Collection<String> list) {
    return List.of(createLabelList(list));
  }

  /**
   * Creates the subitem 'stacks' in a data-item based on a list, that is probably given via
   * yaml-file. This is why there is a replacement of all occurrences of the hyphen character ('-')
   * with an underscore ('_') so that the config entries fit with the result json format.
   */
  public static List<String> createLabelList(Collection<String> list) {
    return list.stream().toList();
    //    return list.stream().map(x -> x.replaceAll("-", "_")).toList();
  }
}
