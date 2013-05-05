package org.e2k;

public class Ship {
	
	private String name;
	private String mmsi;
	private int[] gwIdent=new int[6];
	
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

	public int[] getGwIdent() {
		return gwIdent;
	}

	public void setGwIdent(int[] gwIdent) {
		this.gwIdent = gwIdent;
	}
	
}
