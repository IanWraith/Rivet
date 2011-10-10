package org.e2k;

import javax.swing.JOptionPane;

public class FSK200500 extends MFSK {
	
	private int baudRate=200;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private long energyStartPoint;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private boolean figureShift=false; 
	private int lineCount=0;
	private int correctionValue=0;
	
	public FSK200500 (Rivet tapp,int baud)	{
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
				JOptionPane.showMessageDialog(null,"WAV files containing\nFSK200/500 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			lineCount=0;
			lineBuffer.delete(0,lineBuffer.length());
			theApp.setStatusLabel("Known Tone Hunt");
			return null;
		}
		
		// Hunting for known tones
		if (state==1)	{
			if (sampleCount>0) outLines[0]=syncToneHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				state=2;
				energyStartPoint=sampleCount;
				energyBuffer.setBufferCounter(0);
				theApp.setStatusLabel("Hunting for a Sync Tone");
			}
		}
		
		if (state==2)	{
			int freq=fsk200500Freq(circBuf,waveData,0);
			// Check this first tone isn't just noise
			if (getPercentageOfTotal()<5.0) return null;
			if (toneTest(freq,1700,20)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			theApp.setStatusLabel("Acquiring Symbol Timing");
			state=3;
			
		}
		
		// Set the symbol timing
		if (state==3)	{
			final int lookAHEAD=5;
			
			// TODO: Fix the FSK200/500 symbol timing code
			
			do64FFT (circBuf,waveData,0);
			energyBuffer.addToCircBuffer((int)getTotalEnergy());
			
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()>(int)(samplesPerSymbol*lookAHEAD))	{
				
				int a;
				for (a=0;a<40*lookAHEAD;a++)	{
					String l=Integer.toString(energyBuffer.directAccess(a));
					theApp.debugDump(l);
				}
				
				
				// Now find the lowest energy value
				long perfectPoint=energyBuffer.returnLowestBin()+energyStartPoint+(int)samplesPerSymbol;
				// Calculate what the value of the symbol counter should be
				symbolCounter=(int)samplesPerSymbol-(perfectPoint-sampleCount);
				state=4;
				theApp.setStatusLabel("Symbol Timing Achieved");
				if (theApp.isSoundCardInput()==true) outLines[0]=theApp.getTimeStamp()+" Symbol timing found";
				else outLines[0]=theApp.getTimeStamp()+" Symbol timing found at position "+Long.toString(perfectPoint);
				sampleCount++;
				symbolCounter++;
				return outLines;
			}
		}
		
		// Decode traffic
		if (state==4)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;				
				
			}
		}
		sampleCount++;
		symbolCounter++;
		return outLines;				
	}
	
	private String syncToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		// Get 4 symbols
		int freq1=fsk200500Freq(circBuf,waveData,0);
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=fsk200500Freq(circBuf,waveData,(int)samplesPerSymbol*1);
		// Check we have a high low
		if (freq2>freq1) return null;
		// Check there is more than 450 Hz of difference
		difference=freq1-freq2;
		if (difference<450) return null;
		int freq3=fsk200500Freq(circBuf,waveData,(int)samplesPerSymbol*2);
		// Don't waste time carrying on if freq1 isn't the same as freq3
		if (freq1!=freq3) return null;
		int freq4=fsk200500Freq(circBuf,waveData,(int)samplesPerSymbol*3);
		// Check 2 of the symbol frequencies are different
		if ((freq1!=freq3)||(freq2!=freq4)) return null;
		// Check that 2 of the symbol frequencies are the same
		if ((freq1==freq2)||(freq3==freq4)) return null;
		// Calculate the difference between the sync tones
		difference=freq1-freq2;
		// was 1700
		correctionValue=1700-freq1;
		String line=theApp.getTimeStamp()+" FSK200/500 Sync Tones Found (Correcting by "+Integer.toString(correctionValue)+" Hz) sync tone difference "+Integer.toString(difference)+" Hz";
		return line;
	}
	
private int fsk200500Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doFSK200500_8000FFT(circBuf,waveData,pos);
			freq=freq+correctionValue;
			return freq;
		}
		
		return -1;
	}

}
