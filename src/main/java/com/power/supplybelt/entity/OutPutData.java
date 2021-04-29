package com.power.supplybelt.entity;

import java.util.List;

public class OutPutData {
    //站名
    private String stationName;
    //供带自身数据
    private List<PowerSupplyData> self;
    //供数据
    private List<PowerSupplyData> producer;
    //带数据
    private List<PowerSupplyData> consumer;
    //是否只有一条母线供数据,如果是，则供数据相同，即该站供主变只要罗列一次
    private boolean isOneBus;

    public boolean isOneBus() {
        return isOneBus;
    }

    public void setOneBus(boolean oneBus) {
        isOneBus = oneBus;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public List<PowerSupplyData> getSelf() {
        return self;
    }

    public void setSelf(List<PowerSupplyData> self) {
        this.self = self;
    }

    public List<PowerSupplyData> getProducer() {
        return producer;
    }

    public void setProducer(List<PowerSupplyData> producer) {
        this.producer = producer;
    }

    public List<PowerSupplyData> getConsumer() {
        return consumer;
    }

    public void setConsumer(List<PowerSupplyData> consumer) {
        this.consumer = consumer;
    }
}
