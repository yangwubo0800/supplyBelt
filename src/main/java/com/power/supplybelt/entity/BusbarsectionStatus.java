package com.power.supplybelt.entity;

import java.util.List;

public class BusbarsectionStatus {
    //记录母线连接状态，查询完断路器之后，知道是否多条为一条
    private boolean isEqualOneBus;
    //与主变相连的母线， 或者与进出线相连的
    private List<Busbarsection> busbarsections;
    // TODO: 增加原始查询出的母线集合信息，方便后面给进出线关联使用
    private List<Busbarsection> orignalBuses;

    public List<Busbarsection> getOrignalBuses() {
        return orignalBuses;
    }

    public void setOrignalBuses(List<Busbarsection> orignalBuses) {
        this.orignalBuses = orignalBuses;
    }

    public boolean isEqualOneBus() {
        return isEqualOneBus;
    }

    public void setEqualOneBus(boolean equalOneBus) {
        isEqualOneBus = equalOneBus;
    }

    public List<Busbarsection> getBusbarsections() {
        return busbarsections;
    }

    public void setBusbarsections(List<Busbarsection> busbarsections) {
        this.busbarsections = busbarsections;
    }
}
