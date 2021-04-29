package com.power.supplybelt.entity;
//记录父子连接线段，用来判断进出线
public class ParentSonSegment {

    private String parentStId;
    private String sonStId;
    private String aclnSegId;
    private String aclineId;

    public String getParentStId() {
        return parentStId;
    }

    public void setParentStId(String parentStId) {
        this.parentStId = parentStId;
    }

    public String getSonStId() {
        return sonStId;
    }

    public void setSonStId(String sonStId) {
        this.sonStId = sonStId;
    }

    public String getAclnSegId() {
        return aclnSegId;
    }

    public void setAclnSegId(String aclnSegId) {
        this.aclnSegId = aclnSegId;
    }

    public String getAclineId() {
        return aclineId;
    }

    public void setAclineId(String aclineId) {
        this.aclineId = aclineId;
    }
}
