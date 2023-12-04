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
public record CoronaTreatmentLevelExport(List<String> listNormalWard, List<String> listIcu,
                                         List<String> listIcuVent, List<String> listEcmo) {

  private static final String deliminator = ";";

  public String toCsv() {
    StringBuilder sb = new StringBuilder();
    sb.append("Normalstation;ICU;ICU_mit_Beatmung;ICU_mit_ECMO\n");

    int maxSize = Math.max(listNormalWard.size(), listIcu.size());
    for (int i = 0; i < maxSize; i++) {
      if (listNormalWard.size() > i && listNormalWard.get(i) != null) {
        sb.append(listNormalWard.get(i).replace("-000000-EK", "")).append(deliminator);
      } else {
        sb.append(deliminator);
      }
      if (listIcu.size() > i && listIcu.get(i) != null) {
        sb.append(listIcu.get(i).replace("-000000-EK", "")).append(deliminator);
      } else {
        sb.append(deliminator);
      }
      if (listIcuVent.size() > i && listIcuVent.get(i) != null) {
        sb.append(listIcuVent.get(i).replace("-000000-EK", "")).append(deliminator);
      } else {
        sb.append(deliminator);
      }
      if (listEcmo.size() > i && listEcmo.get(i) != null) {
        sb.append(listEcmo.get(i).replace("-000000-EK", ""));
      } else {
        sb.append(deliminator);
      }

      sb.append("\n");
    }

    return sb.toString();
  }
}

