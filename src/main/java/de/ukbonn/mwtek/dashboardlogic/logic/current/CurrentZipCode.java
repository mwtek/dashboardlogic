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
package de.ukbonn.mwtek.dashboardlogic.logic.current;

import de.ukbonn.mwtek.dashboardlogic.DashboardDataItemLogic;
import de.ukbonn.mwtek.dashboardlogic.logic.cumulative.CumulativeZipCode;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for generating the data item {@link DiseaseDataItem cumulative.zipcode}.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 * @author <a href="mailto:berke_enes.dincel@ukbonn.de">Berke Enes Dincel</a>
 */
@Slf4j
public class CurrentZipCode extends DashboardDataItemLogic {

  /**
   * Method to create a list containing the zip code of each patient from Germany who has tested
   * positive for the given disease.
   */
  public List<String> createZipCodeList(
      List<MiiEncounter> facilityContacts,
      List<MiiEncounter> encounterSubSet,
      List<MiiPatient> patients,
      Integer charsToBeTrimmed,
      boolean applyDiseasePositiveFilter) {
    // Log the start of the method
    log.debug("started CurrentZipCode.createZipCodeList");

    // Just active encounters should be concerned
    List<MiiEncounter> encounterSubSetActive =
        encounterSubSet.stream().filter(MiiEncounter::isActive).toList();
    return CumulativeZipCode.createZipCodeList(
        facilityContacts,
        encounterSubSetActive,
        patients,
        charsToBeTrimmed,
        applyDiseasePositiveFilter);
  }
}
