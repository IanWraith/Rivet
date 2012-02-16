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
	private int highBin;
	private int lowBin;
	private boolean inChar[]=new boolean[7];
	
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
			//baudRate=186;
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
			}
		}
				
		// Decode traffic
		if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				// Get the early/late gate difference value
				double gateDif=gateEarlyLateFSK200500(circBuf,(int)samplesPerSymbol,lowBin,highBin);					
				// Adjust the symbol counter as required to obtain symbol sync
				symbolCounter=(int)gateDif/5;
				int ibit=fsk200500FreqHalf(circBuf,waveData,0);
				// If this is a full bit add it to the character buffer
				// If it is a half bit it signals the end of a character
				if (ibit>1)	{
					symbolCounter=(int)samplesPerSymbol/2;
					String ch=getBaudotChar();
					lineBuffer.append(ch);
					
					lineBuffer.append(" ");
					
					characterCount=characterCount+7;
				}
				else	{
					addToCharBuffer(ibit);	
				}
				
				if (characterCount>60)	{
					outLines[0]=lineBuffer.toString();
					lineBuffer.delete(0,lineBuffer.length());
					characterCount=0;
				}
				
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
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=fsk200500Freq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
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
		// Store the bin numbers
		if (freq1>freq2)	{
			highBin=bin1;
			lowBin=bin2;
		}
		else	{
			highBin=bin2;
			lowBin=bin1;
		}
		// Calculate the centre frequency
		centre=(freq1+freq2)/2;
		String line=theApp.getTimeStamp()+" FSK200/500 Sync Sequence Found";
		return line;
	}
	
	private int fsk200500Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doFSK200500_8000FFT(circBuf,waveData,pos,(int)samplesPerSymbol);
			return freq;
		}
		return -1;
	}
	
	private int fsk200500FreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		int sp=(int)samplesPerSymbol/2;
		int f1=doFSK200500_8000FFT(circBuf,waveData,pos,sp);
		int f2=doFSK200500_8000FFT(circBuf,waveData,(pos+sp),sp);
		// Return 0 if full low
		// Return 1 if full high
		int dif;
		if (f1>f2) dif=f1-f2;
		else dif=f2-f1;
		if (dif<300)	{
			if (f1<centre) return 0;
			else return 1;
		}
		// Return 2 if low then high
		// Return 3 if high then low
		else	{
			if (f2>f1) return 2;
			else return 3;
		}
		
	}
	
	// Add incoming data to the character buffer
	private void addToCharBuffer (int in)	{
		int a;
		for (a=1;a<inChar.length;a++)	{
			inChar[a-1]=inChar[a];
		}
		if (in==0) inChar[6]=false;
		else inChar[6]=true;
	}
	
	// Returns the baudot character in the character buffer
	private String getBaudotChar()	{
		String out="";
		int a;
		for (a=1;a<6;a++)	{
			if (inChar[a]==true) out=out+"1";
			else out=out+"0";
		}
		return out;
	}
	
}
