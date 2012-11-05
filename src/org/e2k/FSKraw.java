package org.e2k;

import java.awt.Color;
import javax.swing.JOptionPane;

//KG-84 Sync string 1111101111001110101100001011100011011010010001001100101010000001

public class FSKraw extends FSK {
	
	private double baudRate=50;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int highBin;
	private int lowBin;
	private final int MAXCHARLENGTH=100;
	private int characterCounter=0; 
	private double adjBuffer[]=new double[2];
	private int adjCounter=0;
	private int shift=450;
	
	public FSKraw (Rivet tapp)	{
		theApp=tapp;
		samplesPerSymbol=samplesPerSymbol(baudRate,8000);
	}
	
	public void setBaudRate(double br) {
		if (br!=this.baudRate) setState(0);
		this.baudRate=br;
		samplesPerSymbol=samplesPerSymbol(baudRate,8000);
	}

	public double getBaudRate() {
		return baudRate;
	}

	// Set the objects decode state and the status bar
	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Decoding Traffic");
	}

	public int getState() {
		return state;
	}
	
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{

		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nFSK recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			setState(1);
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			return;
		}
		
		// Hunt for the sync sequence
		if (state==1)	{
			String sRet;
			if (sampleCount>0)	{
				sRet=syncSequenceHunt(circBuf,waveData);
				if (sRet!=null)	{
					theApp.writeLine(sRet,Color.BLACK,theApp.italicFont);
					setState(2);
					characterCounter=0;
					energyBuffer.setBufferCounter(0);
				}
			}
		}
				
		// Decode traffic
		if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				boolean ibit=fskFreqHalf(circBuf,waveData,0);
				// Display this
				if (ibit==true) theApp.writeChar("1",Color.BLACK,theApp.boldFont);
				else theApp.writeChar("0",Color.BLACK,theApp.boldFont);
				characterCounter++;
				// Have we reached the end of a line
				if (characterCounter==MAXCHARLENGTH)	{
					characterCounter=0;
					theApp.newLineWrite();
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return;				
	}

	public int getShift() {
		return shift;
	}

	public void setShift(int shift) {
		this.shift = shift;
	}	
	
	
	// Find the frequency of a RTTY symbol
	// Currently the program only supports a sampling rate of 8000 KHz
	private int rttyFreq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doRTTY_8000FFT(circBuf,waveData,pos,(int)samplesPerSymbol,baudRate);
			return freq;
		}
		return -1;
	}
	
	// Look for a sequence of 4 alternating tones with a certain shift
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		// Get 4 symbols
		int freq1=rttyFreq(circBuf,waveData,0);
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=rttyFreq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
		// Check we have a high low
		if (freq2>freq1) return null;
		// Check there is around shift (+25 and -25 Hz) of difference between the tones
		difference=freq1-freq2;
		if ((difference<(shift-25))||(difference>(shift+25))) return null;
		int freq3=rttyFreq(circBuf,waveData,(int)samplesPerSymbol*2);
		// Don't waste time carrying on if freq1 isn't the same as freq3
		if (freq1!=freq3) return null;
		int freq4=rttyFreq(circBuf,waveData,(int)samplesPerSymbol*3);
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
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return null;
		String line=theApp.getTimeStamp()+" FSK Sync Sequence Found";
		return line;
	}
	
	
	// Add a comparator output to a circular buffer of values
	private void addToAdjBuffer (double in)	{
		adjBuffer[adjCounter]=in;
		adjCounter++;
		if (adjCounter==adjBuffer.length) adjCounter=0;
	}
	
	// Return the average of the circular buffer
	private double adjAverage()	{
		int a;
		double total=0.0;
		for (a=0;a<adjBuffer.length;a++)	{
			total=total+adjBuffer[a];
		}
		return (total/adjBuffer.length);
	}
	
	// Get the average value and return an adjustment value
	private int adjAdjust()	{
		double av=adjAverage();
		double r=Math.abs(av)/5;
		if (av<0) r=0-r;
		//theApp.debugDump(Double.toString(av)+","+Double.toString(r));
		//r=0;
		return (int)r;
	}	

	
	// The "normal" way of determining the frequency of a RTTY symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate 
	private boolean fskFreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		boolean out;
		int sp=(int)samplesPerSymbol/2;
		// First half
		double early[]=doRTTYHalfSymbolBinRequest(baudRate,circBuf,pos,lowBin,highBin);
		// Last half
		double late[]=doRTTYHalfSymbolBinRequest(baudRate,circBuf,(pos+sp),lowBin,highBin);
		// Feed the early late difference into a buffer
		if ((early[0]+late[0])>(early[1]+late[1])) addToAdjBuffer(getPercentageDifference(early[0],late[0]));
		else addToAdjBuffer(getPercentageDifference(early[1],late[1]));
		// Calculate the symbol timing correction
		symbolCounter=adjAdjust();
		// Now work out the binary state represented by this symbol
		double lowTotal=early[0]+late[0];
		double highTotal=early[1]+late[1];
		// Calculate the bit value
		if (theApp.isInvertSignal()==false)	{
			if (lowTotal>highTotal) out=true;
			else out=false;
			}
		else	{
			// If inverted is set invert the bit returned
			if (lowTotal>highTotal) out=false;
			else out=true;
			}
		// Is the bit stream being recorded ?
		if (theApp.isBitStreamOut()==true)	{
			if (out==true) theApp.bitStreamWrite("1");
			else theApp.bitStreamWrite("0");
			}		
		return out;		
	}


}
