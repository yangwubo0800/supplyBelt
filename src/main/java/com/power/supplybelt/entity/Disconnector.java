package com.power.supplybelt.entity;

public class Disconnector {
    private String id;
    private String name;
    private String stId;
    private String bvId;
    private String bayId;
    private String ind;
    private String jnd;

    public String getBayId() {
        return bayId;
    }

    public void setBayId(String bayId) {
        this.bayId = bayId;
    }

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

    public String getBvId() {
        return bvId;
    }

    public void setBvId(String bvId) {
        this.bvId = bvId;
    }

    public String getInd() {
        return ind;
    }

    public void setInd(String ind) {
        this.ind = ind;
    }

    public String getJnd() {
        return jnd;
    }

    public void setJnd(String jnd) {
        this.jnd = jnd;
    }
}
