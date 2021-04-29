package com.power.supplybelt.entity;

public class Substation {
    private String id;
    private String name;
    private String bvId;
    //变电站类型，判断是否为用户变, 8 为用户变
    private String stType;
    
    

    public String getStType() {
		return stType;
	}

	public void setStType(String stType) {
		this.stType = stType;
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

    public String getBvId() {
        return bvId;
    }

    public void setBvId(String bvId) {
        this.bvId = bvId;
    }
}
