package com.power.supplybelt.entity;

public class PowerTransformer {
    private String id;
    private String name;
    private String stId;
    private String bayId;
    private String bvId;
    private String windType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStId() {
        return stId;
    }

    public void setStId(String stId) {
        this.stId = stId;
    }

    public String getBayId() {
        return bayId;
    }

    public void setBayId(String bayId) {
        this.bayId = bayId;
    }

    public String getBvId() {
        return bvId;
    }

    public void setBvId(String bvId) {
        this.bvId = bvId;
    }

    public String getWindType() {
        return windType;
    }

    public void setWindType(String windType) {
        this.windType = windType;
    }
}
