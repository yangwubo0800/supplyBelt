package com.power.supplybelt.entity;

import java.util.List;

public class InOutLines {
    //父站
    private String parentStId;
    //子站
    private String sonStId;
    //线段ID,在判断各个变电站之间的连接关系时，通过线段查询到两端的站，由拓扑关系可以确认供带
    //需要过滤掉end一端通，一端不通的情况
    private String aclnSegId;
    //T接线记录，因为一个T有多条线段，上述的segId不用记录。
    private String aclineId;
    //电压等级,对于某一个站的多侧输出，例如110和35都输出，需要根据bvId来确认母线的某一侧
    private String bvId;
    //记录进线或者出现于母线关系
    private BusbarsectionStatus busbarsectionStatus;
    //记录进出线最终连接的主变
    private List<TransformerWinding> transformerWindings;
    //是否为虚拟进出线, 用来判断明月，江背这种同电压等级的
    private boolean isVirtualLine;
    
    

    public boolean isVirtualLine() {
		return isVirtualLine;
	}

	public void setVirtualLine(boolean isVirtualLine) {
		this.isVirtualLine = isVirtualLine;
	}

	public String getBvId() {
        return bvId;
    }

    public void setBvId(String bvId) {
        this.bvId = bvId;
    }

    public List<TransformerWinding> getTransformerWindings() {
        return transformerWindings;
    }

    public void setTransformerWindings(List<TransformerWinding> transformerWindings) {
        this.transformerWindings = transformerWindings;
    }

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

    public BusbarsectionStatus getBusbarsectionStatus() {
        return busbarsectionStatus;
    }

    public void setBusbarsectionStatus(BusbarsectionStatus busbarsectionStatus) {
        this.busbarsectionStatus = busbarsectionStatus;
    }
}
