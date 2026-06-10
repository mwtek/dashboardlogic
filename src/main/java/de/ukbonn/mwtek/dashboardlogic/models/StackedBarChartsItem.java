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

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ukbonn.mwtek.dashboardlogic.enums.StackedBarCharts;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StackedBarChartsItem<T> implements StackedBarCharts<T> {

  /** List of chart names. */
  private List<String> charts;

  /** List of bar names corresponding to each chart. */
  private List<List<String>> bars;

  /** List of stack names for each bar in each chart. */
  private List<List<String>> stacks;

  /** Values for each stack in each bar in each chart. */
  private List<List<List<T>>> values;

  @JsonIgnore private Map<String, List<String>> debugData;

  /** Returns the list of values associated with the given bar name (from the first chart). */
  public List<T> getValueByBarChart(String barChartName) {
    // Guard against null/empty structures
    if (bars == null || bars.isEmpty() || values == null || values.isEmpty()) {
      return null;
    }

    // Find the index of the bar in the first chart
    int barIndex = bars.getFirst().indexOf(barChartName);
    if (barIndex == -1) {
      return null;
    }

    // Return the values corresponding to the found index in the first chart
    return values.getFirst().get(barIndex);
  }

  /** Same as above but lets you specify which chart to read from. */
  public List<T> getValueByBarChart(int chartIndex, String barChartName) {
    // Guard against invalid chart index
    if (bars == null
        || values == null
        || chartIndex < 0
        || chartIndex >= bars.size()
        || chartIndex >= values.size()) {
      return null;
    }

    List<String> barsOfChart = bars.get(chartIndex);
    int barIndex = (barsOfChart != null) ? barsOfChart.indexOf(barChartName) : -1;
    if (barIndex == -1) {
      return null;
    }

    return values.get(chartIndex).get(barIndex);
  }
}
