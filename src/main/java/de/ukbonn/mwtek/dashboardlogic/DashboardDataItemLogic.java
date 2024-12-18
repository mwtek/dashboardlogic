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
 */ package de.ukbonn.mwtek.dashboardlogic;

import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.KJP_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDate;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Observation;

public class DashboardDataItemLogic {
  public static List<UkbObservation> getObservationsYoungerKickoffDate(
      DataItemContext dataItemContext, Collection<UkbObservation> ukbObservations) {
    return ukbObservations.parallelStream()
        .filter(Observation::hasEffectiveDateTimeType)
        .filter(x -> x.getEffectiveDateTimeType().hasValue())
        .filter(x -> x.getEffectiveDateTimeType().getValue().after(getKickOffDate(dataItemContext)))
        .toList();
  }

  protected static boolean isDateInYearMonth(LocalDate date, String yearMonthString) {
    String[] parts = yearMonthString.split("-");
    int year = Integer.parseInt(parts[0]);
    int month = Integer.parseInt(parts[1]);
    return date.getYear() == year && date.getMonthValue() == month;
  }

  protected static boolean isDateInYear(LocalDate date, String yearString) {
    int year = Integer.parseInt(yearString);
    return date.getYear() == year;
  }

  protected static LocalDate convertToLocalDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  protected static String determineKiRaChartsAllLabelByContext(
      KidsRadarDataItemContext kidsRadarDataItemContext) {
    String chartsLabel = "";
    switch (kidsRadarDataItemContext) {
      case KJP -> chartsLabel = KJP_DIAGNOSES_ALL;
      case RSV -> chartsLabel = RSV_DIAGNOSES_ALL;
    }
    return chartsLabel;
  }
}
