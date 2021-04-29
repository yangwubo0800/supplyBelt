package com.power.supplybelt.entity;

public class ACLineEndWithSegment extends ACLineEnd{
    private String istId;
    private String jstId;
    private String ind;
    private String jnd;
    private String aclineId;
    private String segName;
    
    

    public String getSegName() {
		return segName;
	}

	public void setSegName(String segName) {
		this.segName = segName;
	}

	public String getAclineId() {
        return aclineId;
    }

    public void setAclineId(String aclineId) {
        this.aclineId = aclineId;
    }

    public String getIstId() {
        return istId;
    }

    public void setIstId(String istId) {
        this.istId = istId;
    }

    public String getJstId() {
        return jstId;
    }

    public void setJstId(String jstId) {
        this.jstId = jstId;
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
