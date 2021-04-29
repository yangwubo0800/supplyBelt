package com.power.supplybelt.entity;

public class TransformerWinding {
    private String id;
    private String name;
    private String stId;
    private String bayId;
    private String bvId;
    private String nd;
    //主变id
    private String trId;
    //容量
    private String mvanom;
    //绕阻端 0 高压，1中压， 2低压
    private String windType;
    //由于有的主变有三侧，如果主变运行，其中某一侧可能没有接通，所以需要标记
    // 0 表示打开，1 表示关闭， 110KV侧的可能没有breaker,需要找隔刀
    private String yx_close;
    //间隔id,在查找运行时主变通过断路器或者隔刀的bayId设置进来进行记录，方便后续查找主变各侧对应母线时查找隔刀方便
    private String bayIdForBus;
    //标记是否线变组, 1 为是， 0 为否
    private String isLineVariantGroup;

    public String getIsLineVariantGroup() {
        return isLineVariantGroup;
    }

    public void setIsLineVariantGroup(String isLineVariantGroup) {
        this.isLineVariantGroup = isLineVariantGroup;
    }

    public String getBayIdForBus() {
        return bayIdForBus;
    }

    public void setBayIdForBus(String bayIdForBus) {
        this.bayIdForBus = bayIdForBus;
    }

    public String getWindType() {
        return windType;
    }

    public void setWindType(String windType) {
        this.windType = windType;
    }

    public String getNd() {
        return nd;
    }

    public void setNd(String nd) {
        this.nd = nd;
    }

    public String getYx_close() {
        return yx_close;
    }

    public void setYx_close(String yx_close) {
        this.yx_close = yx_close;
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

    public String getTrId() {
        return trId;
    }

    public void setTrId(String trId) {
        this.trId = trId;
    }

    public String getMvanom() {
        return mvanom;
    }

    public void setMvanom(String mvanom) {
        this.mvanom = mvanom;
    }
}
