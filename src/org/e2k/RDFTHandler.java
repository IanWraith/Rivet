package org.e2k;

import javax.swing.JOptionPane;

public class RDFTHandler extends OFDM {
	
	private int state=0;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private double samplesPerSymbol;
	public StringBuilder lineBuffer=new StringBuilder();

	
	public RDFTHandler (Rivet tapp)	{
		theApp=tapp;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state=state;
	}
	
	// The main decode routine
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Initial startup
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nRDFT recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			samplesPerSymbol=samplesPerSymbol(122.5,waveData.getSampleRate());
			state=1;
			lineBuffer.delete(0,lineBuffer.length());
			theApp.setStatusLabel("Sync Hunt");
			return;
		}
		else if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return;
			
			double spr[]=doRDFTFFTAllBinsRequest(circBuf,waveData,0);
			
			StringBuilder sb=new StringBuilder();
			int a;
			for (a=0;a<spr.length;a++)	{
				sb.append(Double.toString(spr[a])+",");
			}
			theApp.debugDump(sb.toString());
			
		}
		
	}
	

}
