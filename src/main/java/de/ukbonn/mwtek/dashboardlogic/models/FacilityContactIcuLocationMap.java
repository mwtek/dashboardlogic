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

package de.ukbonn.mwtek.dashboardlogic.models;

import de.ukbonn.mwtek.dashboardlogic.tools.LocationFilter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;

public class FacilityContactIcuLocationMap {

  private final Map<String, List<EncounterLocationComponent>> map;

  /**
   * Index structure mapping a facility contact (encounter) ID to all of its ICU {@link
   * EncounterLocationComponent}s.
   */
  public FacilityContactIcuLocationMap(
      List<MiiEncounter> encounters, List<MiiLocation> allLocations) {
    Collection<String> icuLocationIds = LocationFilter.getIcuLocationIds(allLocations);
    this.map =
        encounters.stream()
            .filter(enc -> enc.isIcuCase(icuLocationIds, false))
            .collect(
                Collectors.toMap(
                    MiiEncounter::getFacilityContactId,
                    enc -> enc.getIcuLocationComponents(icuLocationIds, false),
                    (list1, list2) -> {
                      List<MiiEncounter.EncounterLocationComponent> merged = new ArrayList<>(list1);
                      merged.addAll(list2);
                      return merged;
                    }));
  }

  /**
   * Returns ICU location components for a given facility contact ID.
   *
   * @param facilityContactId the encounter/facility contact identifier
   * @return list of ICU {@link EncounterLocationComponent}s, or an empty list if none found
   */
  public List<MiiEncounter.EncounterLocationComponent> get(String facilityContactId) {
    return map.getOrDefault(facilityContactId, Collections.emptyList());
  }

  /**
   * Checks whether the index contains an entry for the given facility contact ID.
   *
   * @param facilityContactId the encounter/facility contact identifier
   * @return {@code true} if present; {@code false} otherwise
   */
  public boolean contains(String facilityContactId) {
    return map.containsKey(facilityContactId);
  }

  /**
   * Returns the set of all facility contact IDs in the index.
   *
   * @return key set view of the index
   */
  public Set<String> keySet() {
    return map.keySet();
  }

  /**
   * Exposes the internal map as an unmodifiable view.
   *
   * @return unmodifiable map of facilityContactId -> ICU components
   */
  public Map<String, List<MiiEncounter.EncounterLocationComponent>> asMap() {
    return Collections.unmodifiableMap(map);
  }
}
