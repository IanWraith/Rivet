package org.e2k;

import java.awt.Color;
import javax.swing.JOptionPane;

public class SITOR extends FSK {
	
	private final double BAUDRATE=100;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int characterCount=0;
	private int highBin;
	private int lowBin;
	private final int MAXCHARLENGTH=100;
	private long missingCharCounter=0;
	private double adjBuffer[]=new double[5];
	private int adjCounter=0;
	private double symbolTotal;
	private double previousSymbolTotal;
	private double oldSymbolPercentage[]=new double[4];
	private boolean inChar7[]=new boolean[7];
	private int bcount;
	
	public SITOR (Rivet tapp)	{
		theApp=tapp;
		samplesPerSymbol=samplesPerSymbol(BAUDRATE,8000);
	}
	
	// The main decode routine
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nSITOR recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			// Clear the display side of things
			characterCount=0;
			lettersMode=true;
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
					energyBuffer.setBufferCounter(0);
					missingCharCounter=0;
					bcount=0;
				}
			}
		}
				
		// Decode traffic
		if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				boolean ibit=sitorFreqHalf(circBuf,waveData,0);
				// Is the bit stream being recorded ?
				if (theApp.isBitStreamOut()==true)	{
					if (ibit==true) theApp.bitStreamWrite("1");
					else if (ibit==false) theApp.bitStreamWrite("0");
				}
				// Shuffle the old stored percentage values
				oldSymbolPercentage[3]=oldSymbolPercentage[2];
				oldSymbolPercentage[2]=oldSymbolPercentage[1];
				oldSymbolPercentage[1]=oldSymbolPercentage[0];
				// Calculate the current percentage value
				if (symbolTotal<previousSymbolTotal) oldSymbolPercentage[0]=100.0-((symbolTotal/previousSymbolTotal)*100.0);
				else oldSymbolPercentage[0]=100.0-((previousSymbolTotal/symbolTotal)*100.0);
				double av=(oldSymbolPercentage[0]+oldSymbolPercentage[1]+oldSymbolPercentage[2]+oldSymbolPercentage[3])/4;
				// If the percentage different is over 40% and more than two characters are missing then the signal has been lost
				if ((av>40.0)&&(missingCharCounter>2)) setState(1);
					
				
				// Add the bit to the character buffer
				addToCharBuffer(ibit);
				
				
				// Adjust the symbol counter
				symbolCounter=adjAdjust();
				
				// If the character count has reached MAXCHARLENGTH then display this line and write a newline to the screen
				if (characterCount>=MAXCHARLENGTH)	{
					characterCount=0;
					theApp.newLineWrite();
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return;				
	}	
	
	
	// Set the objects decode state and the status bar
	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Decoding Traffic");
	}
	
	// Look for a sequence of 4 alternating tones with a certain shift
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		final int SHIFT=170;
		// Get 4 symbols
		int freq1=sitorFreq(circBuf,waveData,0);
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=sitorFreq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
		// Check we have a high low
		if (freq2>freq1) return null;
		// Check there is around shift (+25 and -25 Hz) of difference between the tones
		difference=freq1-freq2;
		if ((difference<(SHIFT-25))||(difference>(SHIFT+25))) return null;
		int freq3=sitorFreq(circBuf,waveData,(int)samplesPerSymbol*2);
		// Don't waste time carrying on if freq1 isn't the same as freq3
		if (freq1!=freq3) return null;
		int freq4=sitorFreq(circBuf,waveData,(int)samplesPerSymbol*3);
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
		String line=theApp.getTimeStamp()+" RTTY Sync Sequence Found";
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

	// Find the frequency of a SITOR symbol
	// Currently the program only supports a sampling rate of 8000 KHz
	private int sitorFreq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doRTTY_8000FFT(circBuf,waveData,pos,(int)samplesPerSymbol,BAUDRATE);
			return freq;
		}
		return -1;
	}	
	
	// Return the symbol frequency given the bins that hold the possible tones
	private boolean sitorFreqHalf (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Run FFTs on the early and late parts of the symbol
		double early[]=do160FFTHalfSymbolBinRequest(circBuf,start,lowBin,highBin);
		start=start+((int)samplesPerSymbol/2);
		double late[]=do160FFTHalfSymbolBinRequest(circBuf,start,lowBin,highBin);
		// Store the previous symbol energy total
		previousSymbolTotal=symbolTotal;
		symbolTotal=early[0]+late[0]+early[1]+late[1];
		// Feed the early late difference into a buffer
		if ((early[0]+late[0])>(early[1]+late[1])) addToAdjBuffer(getPercentageDifference(early[0],late[0]));
		else addToAdjBuffer(getPercentageDifference(early[1],late[1]));
		// Now work out the binary state represented by this symbol
		double lowTotal=early[0]+late[0];
		double highTotal=early[1]+late[1];
		if (theApp.isInvertSignal()==false)	{
			if (lowTotal>highTotal) return true;
			else return false;
		}
		else	{
			// If inverted is set invert the bit returned
			if (lowTotal>highTotal) return false;
			else return true;
		}
	}
	
	
	// Add incoming data to the character buffer
	private void addToCharBuffer (boolean in)	{
		int a;
		for (a=1;a<inChar7.length;a++)	{
			inChar7[a-1]=inChar7[a];
		}
		inChar7[6]=in;
		// Increment the bit counter
		bcount++;
	}	
	
}
