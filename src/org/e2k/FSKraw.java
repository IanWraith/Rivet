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

import java.awt.Color;
import java.util.List;
import javax.swing.JOptionPane;

public class FSKraw extends FSK {
	
	private final boolean NOISY=false;
	private double baudRate=50;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private int highBin;
	private int lowBin;
	private final int MAXCHARLENGTH=100;
	private int characterCounter=0; 
	private double adjBuffer[]=new double[2];
	private int adjCounter=0;
	private int shift=450;
	private CircularBitSet circularBitSet=new CircularBitSet();
	private boolean display=false;
	private int charactersRemaining=0;
	private boolean activeTrigger;
	private long bitsReceived;
	private boolean sBit0;
	private boolean sBit1;
	
	public FSKraw (Rivet tapp)	{
		theApp=tapp;
		samplesPerSymbol=samplesPerSymbol(baudRate,8000);
		circularBitSet.setTotalLength(1024);
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
	
	public boolean decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nFSK recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			setState(1);
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			activeTrigger=false;
			// Add a newline
			theApp.newLineWrite();
			return true;
		}
		
		// Hunt for the sync sequence
		if (state==1)	{
			String sRet;
			if (sampleCount>0)	{
				sRet=syncSequenceHunt(circBuf,waveData);
				if (sRet!=null)	{
					// Change the state
					setState(2);
					characterCounter=0;
					bitsReceived=0;
					circularBitSet.clear();
					circularBitSet.add(sBit0);
					circularBitSet.add(sBit1);
					bitsReceived=2;
					// Clear the adjustment buffer as well 
					clearAdjBuffer();	
					// If displaying then show this info
					if (display==true)	{
						theApp.writeLine(sRet,Color.BLACK,theApp.italicFont);
						// Add a newline
						theApp.newLineWrite();
						// Display the bits received during sync
						if (sBit0==true) theApp.writeChar("1",Color.BLACK,theApp.boldFont);
						else theApp.writeChar("0",Color.BLACK,theApp.boldFont);
						if (sBit1==true) theApp.writeChar("1",Color.BLACK,theApp.boldFont);
						else theApp.writeChar("0",Color.BLACK,theApp.boldFont);
						characterCounter=2;
					}	
				}
			}
		}			
		// Decode traffic
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				boolean ibit=fskFreqHalf(circBuf,waveData,0);
				circularBitSet.add(ibit);
				// Check triggers , if not active then enable the display
				if (theApp.getActiveTriggerCount()>0) triggerCheck();
				else display=true;
				// Display this but only if we are still in state 2 (to prevent any duff binary appearing)
				if ((display==true)&&(state==2))	{
					if (ibit==true) theApp.writeChar("1",Color.BLACK,theApp.boldFont);
					else theApp.writeChar("0",Color.BLACK,theApp.boldFont);
					characterCounter++;
					bitsReceived++;
				}
				// Is there a grab trigger in progress
				if (charactersRemaining>0)	{
					charactersRemaining--;
					if (charactersRemaining==0)	{
						display=false;
						activeTrigger=false;
					}
				}
				// Have we reached the end of a line
				if (characterCounter==MAXCHARLENGTH)	{
					characterCounter=0;
					theApp.newLineWrite();
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return true;				
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
	
	// Look for a sequence of 2 alternating tones with a certain shift
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		double signalPercentage=10.0;
		if (NOISY) signalPercentage=6.0;
		// Get 2 symbols
		int freq1=rttyFreq(circBuf,waveData,0);
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<signalPercentage) return null;
		int freq2=rttyFreq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
		// Check this second tone isn't just noise
		if (getPercentageOfTotal()<signalPercentage) return null;
		// Calculate the difference between these tones
		if (freq2>freq1) difference=freq2-freq1;
		else difference=freq1-freq2;
		if ((difference<(shift-25))||(difference>(shift+25))) return null;
		// Store the bin numbers
		if (freq1>freq2)	{
			highBin=bin1;
			lowBin=bin2;
			// Detected sequence was 10
			if (theApp.isInvertSignal()==false)	{
				sBit0=true;
				sBit1=false;
			}
			else	{
				sBit0=false;
				sBit1=true;
			}
		}
		else	{
			highBin=bin2;
			lowBin=bin1;
			// Detected sequence was 01
			if (theApp.isInvertSignal()==true)	{
				sBit0=true;
				sBit1=false;
			}
			else	{
				sBit0=false;
				sBit1=true;
			}			
		}
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return null;
		String line=theApp.getTimeStamp()+" FSK Sync Sequence Found";
		return line;
	}
	
	
	// Add a comparator output to a circular buffer of values
	private void addToAdjBuffer (double in)	{
		double lossAverage=25.0;
		if (NOISY) lossAverage=60.0;
		// If the buffer average percentage difference is more than lossAverage then we have lost the signal
		if (absAverage()>lossAverage)	{
			if (display==true)	{
				// Tell the user how many bits were received
				String line="("+Long.toString(bitsReceived)+" bits received)";
				theApp.writeLine(line,Color.BLACK,theApp.italicFont);
				// Add a new line after this
				theApp.newLineWrite();
			}	
			// Is there a trigger in progress
			if (activeTrigger==true)	{
				charactersRemaining=0;
				display=false;
				activeTrigger=false;
			}
			// Set to state 1 to try and regain a signal
			setState(1);
		}
		else	{
			adjBuffer[adjCounter]=in;
			adjCounter++;
			if (adjCounter==adjBuffer.length) adjCounter=0;
		}
	}
	
	
	private double absAverage()	{
		double av=adjAverage();
		return Math.abs(av);
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
		// A value of 5 is good for 300 baud
		double divisor=5;
		double av=adjAverage();
		double r=Math.abs(av)/divisor;
		if (av<0) r=0-r;
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
	
	// Clear the adjustment buffer
	private void clearAdjBuffer()	{
		int a;
		for (a=0;a<adjBuffer.length;a++)	{
			adjBuffer[a]=0.0;
		}
	}
	
	// Check if there have been any trigger activations
	public void triggerCheck()	{
		boolean showTrigger=false;
		// Find the number of Triggers
		List<Trigger> tList=theApp.getListTriggers();
		// If no triggers return
		if (tList==null) return;
		int a;
		for (a=0;a<tList.size();a++)	{
			// Get each trigger in turn
			Trigger trigger=tList.get(a);
			// Do we have a match ?
			if (trigger.triggerMatch(circularBitSet)==true)	{
				// Trigger type 1 is a start logging trigger
				if (trigger.getTriggerType()==1)	{
					display=true;
					characterCounter=0;
					activeTrigger=true;
					showTrigger=true;
				}
				// Trigger type 2 is a stop logging trigger
				else if ((trigger.getTriggerType()==2)&&(activeTrigger==true))	{
					display=false;
					activeTrigger=false;
					showTrigger=true;
				}
				// Trigger type 3 is a grab trigger
				if (trigger.getTriggerType()==3)	{
					display=true;
					characterCounter=0;
					// Display the coming charactersRemaining characters in the forward grab
					charactersRemaining=trigger.getForwardGrab();
					// Display the prior characters in the backward grab
					if (trigger.getBackwardGrab()>0)	{
						theApp.writeLine(trigger.getBackwardBitsString(circularBitSet),Color.BLACK,theApp.boldFont);
					}
					activeTrigger=true;
					showTrigger=true;
				}
				// Write the trigger description to the screen/log
				if (showTrigger==true)	{
					// Write a newline first
					theApp.newLineWrite();
			        // then the trigger description
					String des=theApp.getTimeStamp()+" "+trigger.getTriggerDescription();
					theApp.writeLine(des,Color.BLUE,theApp.italicFont);
					// Write another newline
					theApp.newLineWrite();
				}
			}
		}
	}
		
	
}
