package com.power.supplybelt.entity;

import java.util.Objects;

public class PowerSupplyData {
    //电站名字
    private String stationName;
    //主变编号
    private String powerNumber;
    //电压等级
    private String voltage;
    //容量
    private String capacity;
    //负载
    private String load;
    //变压器ID
    private String trId;
    
    //增加遥测表字段
	private String historyTableName;
	private String historyColumnName;
	
	//增加遥测值查询
	private String historyValue;
	
	
	

    public String getHistoryValue() {
		return historyValue;
	}

	public void setHistoryValue(String historyValue) {
		this.historyValue = historyValue;
	}

	public String getHistoryTableName() {
		return historyTableName;
	}

	public void setHistoryTableName(String historyTableName) {
		this.historyTableName = historyTableName;
	}

	public String getHistoryColumnName() {
		return historyColumnName;
	}

	public void setHistoryColumnName(String historyColumnName) {
		this.historyColumnName = historyColumnName;
	}

	public String getTrId() {
        return trId;
    }

    public void setTrId(String trId) {
        this.trId = trId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getPowerNumber() {
        return powerNumber;
    }

    public void setPowerNumber(String powerNumber) {
        this.powerNumber = powerNumber;
    }

    public String getVoltage() {
        return voltage;
    }

    public void setVoltage(String voltage) {
        this.voltage = voltage;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getLoad() {
        return load;
    }

    public void setLoad(String load) {
        this.load = load;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((capacity == null) ? 0 : capacity.hashCode());
		result = prime * result + ((historyColumnName == null) ? 0 : historyColumnName.hashCode());
		result = prime * result + ((historyTableName == null) ? 0 : historyTableName.hashCode());
		result = prime * result + ((historyValue == null) ? 0 : historyValue.hashCode());
		result = prime * result + ((load == null) ? 0 : load.hashCode());
		result = prime * result + ((powerNumber == null) ? 0 : powerNumber.hashCode());
		result = prime * result + ((stationName == null) ? 0 : stationName.hashCode());
		result = prime * result + ((trId == null) ? 0 : trId.hashCode());
		result = prime * result + ((voltage == null) ? 0 : voltage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PowerSupplyData other = (PowerSupplyData) obj;
		if (capacity == null) {
			if (other.capacity != null)
				return false;
		} else if (!capacity.equals(other.capacity))
			return false;
		if (historyColumnName == null) {
			if (other.historyColumnName != null)
				return false;
		} else if (!historyColumnName.equals(other.historyColumnName))
			return false;
		if (historyTableName == null) {
			if (other.historyTableName != null)
				return false;
		} else if (!historyTableName.equals(other.historyTableName))
			return false;
		if (historyValue == null) {
			if (other.historyValue != null)
				return false;
		} else if (!historyValue.equals(other.historyValue))
			return false;
		if (load == null) {
			if (other.load != null)
				return false;
		} else if (!load.equals(other.load))
			return false;
		if (powerNumber == null) {
			if (other.powerNumber != null)
				return false;
		} else if (!powerNumber.equals(other.powerNumber))
			return false;
		if (stationName == null) {
			if (other.stationName != null)
				return false;
		} else if (!stationName.equals(other.stationName))
			return false;
		if (trId == null) {
			if (other.trId != null)
				return false;
		} else if (!trId.equals(other.trId))
			return false;
		if (voltage == null) {
			if (other.voltage != null)
				return false;
		} else if (!voltage.equals(other.voltage))
			return false;
		return true;
	}



 
}
