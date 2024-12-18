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

import java.util.List;

/**
 * Export model for file generation that reports case/encounter ids of active cases separated by
 * treatment level.
 */
public record CoronaTreatmentLevelExport(
    List<String> casesNormalWard,
    List<String> casesIcu,
    List<String> casesIcuVent,
    List<String> casesEcmo) {

  private static final String deliminator = ";";
  public static final String SUFFIX_FACILITY_CONTACT = "-000000-EK";

  public String toCsv() {
    StringBuilder sb = new StringBuilder();
    sb.append("Normalstation;ICU;ICU_mit_Beatmung;ICU_mit_ECMO\n");
    if (casesNormalWard != null && casesIcu != null && casesIcuVent != null && casesEcmo != null) {
      int maxSize = Math.max(casesNormalWard.size(), casesIcu.size());
      for (int i = 0; i < maxSize; i++) {
        if (casesNormalWard.size() > i && casesNormalWard.get(i) != null) {
          sb.append(casesNormalWard.get(i).replace(SUFFIX_FACILITY_CONTACT, ""))
              .append(deliminator);
        } else {
          sb.append(deliminator);
        }
        if (casesIcu.size() > i && casesIcu.get(i) != null) {
          sb.append(casesIcu.get(i).replace(SUFFIX_FACILITY_CONTACT, "")).append(deliminator);
        } else {
          sb.append(deliminator);
        }
        if (casesIcuVent.size() > i && casesIcuVent.get(i) != null) {
          sb.append(casesIcuVent.get(i).replace(SUFFIX_FACILITY_CONTACT, "")).append(deliminator);
        } else {
          sb.append(deliminator);
        }
        if (casesEcmo.size() > i && casesEcmo.get(i) != null) {
          sb.append(casesEcmo.get(i).replace(SUFFIX_FACILITY_CONTACT, ""));
        } else {
          sb.append(deliminator);
        }
        sb.append("\n");
      }
    }
    return sb.toString();
  }
}
