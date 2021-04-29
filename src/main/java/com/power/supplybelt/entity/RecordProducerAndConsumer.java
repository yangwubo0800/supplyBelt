package com.power.supplybelt.entity;

import java.util.List;

public class RecordProducerAndConsumer {

	private List<PowerSupplyData> producerData;
	private List<PowerSupplyData> consumerData;
	public List<PowerSupplyData> getProducerData() {
		return producerData;
	}
	public void setProducerData(List<PowerSupplyData> producerData) {
		this.producerData = producerData;
	}
	public List<PowerSupplyData> getConsumerData() {
		return consumerData;
	}
	public void setConsumerData(List<PowerSupplyData> consumerData) {
		this.consumerData = consumerData;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consumerData == null) ? 0 : consumerData.hashCode());
		result = prime * result + ((producerData == null) ? 0 : producerData.hashCode());
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
		RecordProducerAndConsumer other = (RecordProducerAndConsumer) obj;
		if (consumerData == null) {
			if (other.consumerData != null)
				return false;
		} else if (!consumerData.equals(other.consumerData))
			return false;
		if (producerData == null) {
			if (other.producerData != null)
				return false;
		} else if (!producerData.equals(other.producerData))
			return false;
		return true;
	}
	
	
	
	
	
}
