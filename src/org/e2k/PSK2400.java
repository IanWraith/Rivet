package org.e2k;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class PSK2400 extends PSK {
	
	private int state=0;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	
	private final int NZEROS_LOW=10;
	private final double GAIN_LOW=4.194017117e+05;
	private double xvLow[]=new double[NZEROS_LOW+1];
	private double yvLow[]=new double[NZEROS_LOW+1];
	
	private boolean recoverCarrier=false;
	
	
	
	public PSK2400 (Rivet tapp)	{
		theApp=tapp;
	}
	
	public int getState() {
		return state;
	}
	
	public void setState(int state) {
		this.state=state;
		if (state==0) theApp.setStatusLabel("Setup");
		else if (state==1) theApp.setStatusLabel("Signal Hunt");
		else if (state==2) theApp.setStatusLabel("Msg Hunt");
	}
	
	// The main decode routine
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Initial startup
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nPSK2400 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			// Add a user warning that this mode is experimental doesn't yet decode
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			theApp.writeLine("Please note that this mode is experimental and doesn't work yet !",Color.RED,theApp.italicFont);
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			setState(1);
			return;
		}
		else {
			sampleCount++;
			if (sampleCount<0) return;
			// For every sample that arrives pad it with two zeros to raise the effective sample rate to 24000 Hz
			processSample(lowPassFilter((int)circBuf.getLast()));
			processSample(lowPassFilter(0.0));
			processSample(lowPassFilter(0.0));
		}
		
	}	
	
	// A 2500 Hz low pass filter
	private double lowPassFilter (double in)	{
		int a;
		for (a=0;a<NZEROS_LOW;a++)	{
			xvLow[a]=xvLow[a+1];
			yvLow[a]=yvLow[a+1];
		}
		xvLow[NZEROS_LOW]=in/GAIN_LOW;
		yvLow[10] =   (xvLow[0] + xvLow[10]) + 10 * (xvLow[1] + xvLow[9]) + 45 * (xvLow[2] + xvLow[8])
                + 120 * (xvLow[3] + xvLow[7]) + 210 * (xvLow[4] + xvLow[6]) + 252 * xvLow[5]
                + ( -0.0137757219 * yvLow[0]) + (  0.1953296201 * yvLow[1])
                + ( -1.2624940545 * yvLow[2]) + (  4.9039991908 * yvLow[3])
                + (-12.6953486280 * yvLow[4]) + ( 22.9248466450 * yvLow[5])
                + (-29.3030999830 * yvLow[6]) + ( 26.2473202970 * yvLow[7])
                + (-15.8199532880 * yvLow[8]) + (  5.8207343496 * yvLow[9]);	
		return yvLow[NZEROS_LOW];
	}
	
	// Process a 24000 Hz sample
	private void processSample (double in)	{
		
	 NCO nco=new NCO(1800.0,0.25,24000);
	
	 int a;
	 for (a=0;a<60000;a++)	{
		 double v=nco.getSample();
		 theApp.debugDump(Double.toString(v));
	 }
	 
	 a++;
	 
	}
	
	
	
	
}
