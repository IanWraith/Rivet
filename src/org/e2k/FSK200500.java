package org.e2k;

import javax.swing.JOptionPane;

public class FSK200500 extends FSK {
	
	private int baudRate=200;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int characterCount=0;
	private int centre=0;
	private long syncFoundPoint;
	
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
			if (waveData.getSampleRate()>11025.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nFSK200/500 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			lineBuffer.delete(0,lineBuffer.length());
			theApp.setStatusLabel("Sync Hunt");
			return null;
		}
		
		// Hunt for the sync sequence
		if (state==1)	{
			if (sampleCount>0) outLines[0]=syncSequenceHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				state=2;
				energyBuffer.setBufferCounter(0);
				syncFoundPoint=sampleCount;
				theApp.setStatusLabel("Acquiring Symbol Timing");
			}
		}
		
		// Set the symbol timing
		if (state==2)	{
			do64FFT(circBuf,waveData,0);
			energyBuffer.addToCircBuffer(getHighSpectrum());
			sampleCount++;
			symbolCounter++;
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*1)) return null;
			long perfectPoint=energyBuffer.returnHighestBin()+syncFoundPoint+(int)samplesPerSymbol;
			// Calculate what the value of the symbol counter should be
			symbolCounter=(int)samplesPerSymbol-(perfectPoint-sampleCount);
			state=3;
		}
		
		// Decode traffic
		if (state==3)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;		
				boolean bit=getSymbolBit(circBuf,waveData,0);
				if (bit==true)	lineBuffer.append("1");
				else lineBuffer.append("0");
				if (characterCount==60)	{
					outLines[0]=lineBuffer.toString();
					lineBuffer.delete(0,lineBuffer.length());
					characterCount=0;
				}
				else characterCount++;
				
			}
		}
		sampleCount++;
		symbolCounter++;
		return outLines;				
	}
	
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
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
		// Calculate the centre frequency
		centre=(freq1+freq2)/2;
		String line=theApp.getTimeStamp()+" FSK200/500 Sync Sequence Found";
		return line;
	}
	
	private int fsk200500Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doFSK200500_8000FFT(circBuf,waveData,pos);
			return freq;
		}
		// 11.025 KHz sampling
		else if (waveData.getSampleRate()==11025.0)	{
			int freq=doFSK200500_11025FFT(circBuf,waveData,pos);
			return freq;
		}
		return -1;
	}
	
	// Given a frequency decide the bit value
	private boolean freqDecision (int freq)	{
		if (theApp.isInvertSignal()==false)	{
			if (freq>centre) return true;
			else return false;
		}
		else	{
			if (freq>centre) return false;
			else return true;
			}
		}
	
	// Return the bit value for a certain symbol
	private boolean getSymbolBit (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int f=fsk200500Freq(circBuf,waveData,start);
		boolean bit=freqDecision(f);
		return bit;
		}

}
