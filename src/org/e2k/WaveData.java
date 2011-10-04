// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

public class WaveData {
	private boolean endian=false;
	private boolean fromFile=false;
	private double sampleRate=0.0;
	private int channels=0;
	private int sampleSizeInBits=0;
	private int bytesPerFrame=0;
	
	public boolean isEndian() {
		return endian;
	}
	
	public void setEndian(boolean endian) {
		this.endian = endian;
	}
	
	public boolean isFromFile() {
		return fromFile;
	}
	
	public void setFromFile(boolean fromFile) {
		this.fromFile = fromFile;
	}
	
	public double getSampleRate() {
		return sampleRate;
	}
	
	public void setSampleRate(double sampleRate) {
		this.sampleRate = sampleRate;
	}
	
	public int getChannels() {
		return channels;
	}
	
	public void setChannels(int channels) {
		this.channels = channels;
	}
	
	public int getSampleSizeInBits() {
		return sampleSizeInBits;
	}
	
	public void setSampleSizeInBits(int sampleSizeInBits) {
		this.sampleSizeInBits = sampleSizeInBits;
	}
	
	public int getBytesPerFrame() {
		return bytesPerFrame;
	}
	
	public void setBytesPerFrame(int bytesPerFrame) {
		this.bytesPerFrame = bytesPerFrame;
	}
	
	
}
