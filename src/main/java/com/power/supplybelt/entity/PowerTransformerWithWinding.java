package com.power.supplybelt.entity;

import java.util.List;

public class PowerTransformerWithWinding extends PowerTransformer{
    //主变和绕阻为一对多的关系，两圈变，高低侧，三圈变，高中低侧
    private List<TransformerWinding> windings;

    public List<TransformerWinding> getWindings() {
        return windings;
    }

    public void setWindings(List<TransformerWinding> windings) {
        this.windings = windings;
    }
}
