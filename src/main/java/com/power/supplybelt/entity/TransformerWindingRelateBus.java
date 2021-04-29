package com.power.supplybelt.entity;

public class TransformerWindingRelateBus {

    //记录主变某侧，中低压输出侧或者高压输入侧， 与母线的关联关系
    private BusbarsectionStatus busbarsectionStatus;
    //绕阻
    private TransformerWinding transformerWinding;

    public TransformerWinding getTransformerWinding() {
        return transformerWinding;
    }

    public void setTransformerWinding(TransformerWinding transformerWinding) {
        this.transformerWinding = transformerWinding;
    }

    public BusbarsectionStatus getBusbarsectionStatus() {
        return busbarsectionStatus;
    }

    public void setBusbarsectionStatus(BusbarsectionStatus busbarsectionStatus) {
        this.busbarsectionStatus = busbarsectionStatus;
    }
}
