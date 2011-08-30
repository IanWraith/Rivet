package org.e2k;

import javax.swing.JOptionPane;

public class CROWD36 extends MFSK {
	
	private int baudRate=40;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private String previousCharacter;
	private int groupCount=0;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private long syncFoundPoint;
	
	public CROWD36 (Rivet tapp,int baud)	{
		baudRate=baud;
		theApp=tapp;
	}
	
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
	
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
				
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.sampleRate>11025)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCROWD36 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.channels!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.sampleRate);
			state=1;
			sampleCount=0;
			symbolCounter=0;
			waveData.Clear();
			previousCharacter=null;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			//theApp.setStatusLabel("Start Tone Hunt");
			return null;
		}
	
		return null;
	}

}
