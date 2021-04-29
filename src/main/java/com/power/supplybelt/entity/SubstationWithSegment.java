package com.power.supplybelt.entity;

public class SubstationWithSegment extends Substation{
    //在构建T接线的时候，将其记录与站相关的End,便于后期在父子关系中确认进出线，找bayId
    private ACLineEndWithSegment acLineEndWithSegment;

    public ACLineEndWithSegment getAcLineEndWithSegment() {
        return acLineEndWithSegment;
    }

    public void setAcLineEndWithSegment(ACLineEndWithSegment acLineEndWithSegment) {
        this.acLineEndWithSegment = acLineEndWithSegment;
    }
}
