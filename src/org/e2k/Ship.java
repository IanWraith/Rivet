package org.e2k;

public class Ship {
	
	private String name;
	private String mmsi;
	private String flag;
	private String imo;
	private String callsign;
	private int logCount=0;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getMmsi() {
		return mmsi;
	}
	
	public void setMmsi(String mmsi) {
		this.mmsi = mmsi;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public String getImo() {
		return imo;
	}

	public void setImo(String imo) {
		this.imo = imo;
	}

	public String getCallsign() {
		return callsign;
	}

	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}

	public int getLogCount() {
		return logCount;
	}

	public void incrementLogCount() {
		this.logCount++;
	}
	

}
