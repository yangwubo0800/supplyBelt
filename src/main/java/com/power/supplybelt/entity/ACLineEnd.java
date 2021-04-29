package com.power.supplybelt.entity;

public class ACLineEnd {
    private String id;
    private String name;
    private String bayId;
    private String bvId;
    private String aclnsegId;
    private String nd;
    private String stId;


    public String getStId() {
        return stId;
    }

    public void setStId(String stId) {
        this.stId = stId;
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

    public String getAclnsegId() {
        return aclnsegId;
    }

    public void setAclnsegId(String aclnsegId) {
        this.aclnsegId = aclnsegId;
    }

    public String getNd() {
        return nd;
    }

    public void setNd(String nd) {
        this.nd = nd;
    }
}
