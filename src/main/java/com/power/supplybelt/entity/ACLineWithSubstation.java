package com.power.supplybelt.entity;

import java.util.List;

public class ACLineWithSubstation {
    //处理T接线情况，多个站具有相同aclineId的为T接线
    String aclineId;
    //站
    List<SubstationWithSegment> substationWithSegments;


    public List<SubstationWithSegment> getSubstationWithSegments() {
        return substationWithSegments;
    }

    public void setSubstationWithSegments(List<SubstationWithSegment> substationWithSegments) {
        this.substationWithSegments = substationWithSegments;
    }

    public String getAclineId() {
        return aclineId;
    }

    public void setAclineId(String aclineId) {
        this.aclineId = aclineId;
    }


}
