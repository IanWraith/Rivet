package org.e2k;

import javax.swing.JOptionPane;

public class FSK2001000 extends FSK {
	
	private int baudRate=200;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int characterCount=0;
	private int highBin;
	private int lowBin;
	private boolean inChar[]=new boolean[7];
	private final int MAXCHARLENGTH=80;
	private int bcount;
	private int missingCharCounter=0;
	private double adjBuffer[]=new double[7];
	private int adjCounter=0;
	
	public FSK2001000 (Rivet tapp,int baud)	{
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
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nFSK200/1000 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.getSampleRate());
			state=1;
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			// Clear the display side of things
			characterCount=0;
			lettersMode=true;
			lineBuffer.delete(0,lineBuffer.length());
			theApp.setStatusLabel("Sync Hunt");
			return null;
		}
		
		// Hunt for the sync sequence
		if (state==1)	{
			//if (sampleCount>0) outLines[0]=syncSequenceHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				state=2;
				energyBuffer.setBufferCounter(0);
				bcount=0;
			}
		}
		
		
		sampleCount++;
		symbolCounter++;
		return outLines;			
	}

}
