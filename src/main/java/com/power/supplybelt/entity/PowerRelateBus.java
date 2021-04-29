package com.power.supplybelt.entity;

import java.util.List;

public class PowerRelateBus {
    //记录主变各侧对应连接的母线
    private String powerName;
    private String bvId;
    private List<String> busId;

    public String getPowerName() {
        return powerName;
    }

    public void setPowerName(String powerName) {
        this.powerName = powerName;
    }

    public String getBvId() {
        return bvId;
    }

    public void setBvId(String bvId) {
        this.bvId = bvId;
    }

    public List<String> getBusId() {
        return busId;
    }

    public void setBusId(List<String> busId) {
        this.busId = busId;
    }
}
