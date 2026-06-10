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
 */ package de.ukbonn.mwtek.dashboardlogic.enums;

import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeCluster.Period.MONTHS;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeCluster.Period.YEARS;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KiraAgePedCluster implements KiraAgeCluster {
  AGE_0_3_M(label("0-3", MONTHS), 0, 3, MONTHS),
  AGE_4_12_M(label("4-12", MONTHS), 4, 12, MONTHS),
  AGE_1_2_Y(label("1-2", YEARS), 1, 2, YEARS),
  AGE_3_5_Y(label("3-5", YEARS), 3, 5, YEARS),
  AGE_6_12_Y(label("6-12", YEARS), 6, 12, YEARS),
  AGE_13_18_Y(label("13-17", YEARS), 13, 18, YEARS);

  final String label;
  final Integer lowerBorder;
  final Integer upperBorder;
  final Period period;

  private static String label(String range, Period period) {
    return String.format("age_pediatric_%s%s", range, period == Period.YEARS ? "y" : "m");
  }

  public static final List<String> BARS =
      Arrays.stream(KiraAgePedCluster.values()).map(KiraAgePedCluster::getLabel).toList();
}
