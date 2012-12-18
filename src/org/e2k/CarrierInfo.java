package org.e2k;

public class CarrierInfo {
	
	private int binFFT=0;
	private double frequencyHZ=0.0;
	private double energy=0.0;
	
	public int getBinFFT() {
		return binFFT;
	}
	public void setBinFFT(int binFFT) {
		this.binFFT = binFFT;
	}
	
	public double getFrequencyHZ() {
		return frequencyHZ;
	}
	
	public void setFrequencyHZ(double frequencyHZ) {
		this.frequencyHZ = frequencyHZ;
	}
	
	public double getEnergy() {
		return energy;
	}
	public void setEnergy(double energy) {
		this.energy = energy;
	}

}
