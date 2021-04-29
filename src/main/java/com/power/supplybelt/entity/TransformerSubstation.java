package com.power.supplybelt.entity;

import java.util.ArrayList;
import java.util.List;

public class TransformerSubstation {
    private String name;
    private String stId;
    private String bvId;
    //线段集合
    private List<ACLineEndWithSegment> endWithSegments;
    //子节点
    private List<TransformerSubstation> children;
    //运行主变集合
    private List<PowerTransformerWithWinding> runningTransformers;
    // TODO: 暂时先不增加，需要的话也应该为集合
//    //放置在子节点中的线段记录父子通过哪根线段相连, 当一个子节点被多个父供时，就会存在多条
//    private List<ACLineSegment> acLineSegmentsInChild;

    //进线集合，带有与母线关系
    private List<InOutLines> inLines;
    //出线集合，带有与母线关系
    private List<InOutLines> outLines;
    //主变各侧与母线关系集合, 中低压输出，高压输入
    private List<TransformerWindingRelateBus> transformerWindingRelateBuses;
    //标记改站是否已经找过主变和母线，因为电压等级相等的会先找
    private boolean hasFoundPowerToBus;
    
    

    public boolean isHasFoundPowerToBus() {
		return hasFoundPowerToBus;
	}

	public void setHasFoundPowerToBus(boolean hasFoundPowerToBus) {
		this.hasFoundPowerToBus = hasFoundPowerToBus;
	}

	public List<InOutLines> getInLines() {
        return inLines;
    }
    
    //排除自己构建的虚拟线
    public List<InOutLines> getInLinesWitoutVirtual() {
    	List<InOutLines> temp = new ArrayList<InOutLines>();
    	if (null != inLines && inLines.size()>0) {
			for(InOutLines inline: inLines) {
				boolean isVirtualLine = inline.isVirtualLine();
				if (isVirtualLine) {
					System.out.println("getInLinesWitoutVirtual 发现虚拟进线name="+name);
					continue;
				}				
				temp.add(inline);
			}
		}
    	
        return temp;
    }
    
    //排除自己构建的虚拟线
    public List<InOutLines> getOutLinesWitoutVirtual() {
    	List<InOutLines> temp = new ArrayList<InOutLines>();
    	if (null != outLines && outLines.size()>0) {
			for(InOutLines outline: outLines) {
				boolean isVirtualLine = outline.isVirtualLine();
				if (isVirtualLine) {
					System.out.println("getOutLinesWitoutVirtual 发现虚拟出线name="+name);
					continue;
				}				
				temp.add(outline);
			}
		}
    	
        return temp;
    }

    public void setInLines(List<InOutLines> inLines) {
        this.inLines = inLines;
    }

    public List<InOutLines> getOutLines() {
        return outLines;
    }

    public void setOutLines(List<InOutLines> outLines) {
        this.outLines = outLines;
    }

    public List<TransformerWindingRelateBus> getTransformerWindingRelateBuses() {
        return transformerWindingRelateBuses;
    }

    public void setTransformerWindingRelateBuses(List<TransformerWindingRelateBus> transformerWindingRelateBuses) {
        this.transformerWindingRelateBuses = transformerWindingRelateBuses;
    }

    public List<PowerTransformerWithWinding> getRunningTransformers() {
        return runningTransformers;
    }

    public void setRunningTransformers(List<PowerTransformerWithWinding> runningTransformers) {
        this.runningTransformers = runningTransformers;
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

    public List<ACLineEndWithSegment> getEndWithSegments() {
        return endWithSegments;
    }

    public void setEndWithSegments(List<ACLineEndWithSegment> endWithSegments) {
        this.endWithSegments = endWithSegments;
    }

    public List<TransformerSubstation> getChildren() {
        return children;
    }

    public void setChildren(List<TransformerSubstation> children) {
        this.children = children;
    }
}
