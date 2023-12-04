package de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ROCItem {

  private double replacementRisk;
  private int replacementPerformed;

  public List<Object> getROCItem() {
    List<Object> rocItem = new ArrayList<>();
    rocItem.add(this.getReplacementRisk());
    rocItem.add(this.getReplacementPerformed());
    return rocItem;
  }
}
