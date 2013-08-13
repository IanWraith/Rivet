package org.e2k;

import java.awt.Color;

import javax.swing.JOptionPane;

public class GLN extends FSK {
	
	private int state=0;
	private double samplesPerSymbol100;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private int highTone;
	private int lowTone;
	private int highBin;
	private int lowBin;
	private double adjBuffer[]=new double[1];
	private int adjCounter=0;
	private CircularBitSet dataBitSet=new CircularBitSet();
	private int bitCount;
	private boolean sBit0;
	private boolean sBit1;
	
	public GLN (Rivet tapp)	{
		theApp=tapp;
		dataBitSet.setTotalLength(100);
	}
	
	// The main decode routine
	public boolean decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Initial startup
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nGLN FSK recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol100=samplesPerSymbol(100.0,waveData.getSampleRate());
			setState(1);
			return true;
		}
		else if (state==1)	{
			if (sampleCount>0)	{
				if (syncSequenceHunt(circBuf,waveData)==true)	{
					setState(2);
					bitCount=0;
					dataBitSet.totalClear();
					// Add the bits found in the sync process
					addBit(sBit0);
					addBit(sBit1);
				}
			}
		}
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol100)	{
				symbolCounter=0;
				// Get a bit and add it to the bit buffer
				addBit(glnFreqHalf(circBuf,waveData,0));
			}	
		}
		sampleCount++;
		symbolCounter++;
		return true;
	}		
	

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Msg Hunt");
	}
	
	// Get the frequency at a certain symbol
	private int getSymbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int fr=do100baudFFT(circBuf,waveData,start);
		return fr;
	}
	
	// The "normal" way of determining the frequency of a GLN FSK symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private boolean glnFreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		boolean out;
		int sp=(int)samplesPerSymbol100/2;
		// First half
		double early[]=do100baudFSKHalfSymbolBinRequest(circBuf,pos,lowBin,highBin);
		// Last half
		double late[]=do100baudFSKHalfSymbolBinRequest(circBuf,(pos+sp),lowBin,highBin);
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

	// Add a comparator output to a circular buffer of values
	private void addToAdjBuffer (double in)	{
		// If the percentage difference is more than 45% then we have lost the signal
		if (in>45.0)	{
			processGLNData();
			setState(1);
		}
		else	{
			adjBuffer[adjCounter]=in;
			adjCounter++;
			if (adjCounter==adjBuffer.length) adjCounter=0;
		}
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
		double r=Math.abs(av)/15;
		if (av<0) r=0-r;
		return (int)r;
	}
	
	// Hunt for a two bit alternating sequence with a 200 Hz difference
	private boolean syncSequenceHunt(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0,b0,b1;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		b0=getFreqBin();
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol100*1;
		int f1=getSymbolFreq(circBuf,waveData,pos);
		b1=getFreqBin();
		// Check this second tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		if (f0==f1) return false;
		if (f0>f1)	{
			highTone=f0;
			highBin=b0;
			lowTone=f1;
			lowBin=b1;
			// Detected sequence was 10
			sBit0=true;
			sBit1=false;
			}
		else	{
			highTone=f1;
			highBin=b1;
			lowTone=f0;
			lowBin=b0;
			// Detected sequence was 01
			sBit0=false;
			sBit1=true;
			}
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return false; 
		// The shift for GW FSK should be should be 200 Hz
		if ((highTone-lowTone)!=200) return false;
		else return true;
	}	

	// The main method to process GLN traffic
	private void processGLNData ()	{
		
		if (bitCount<90) return;
		
		// Turn the data into a string
		String sData=dataBitSet.extractSectionFromStart(0,bitCount);	
		// Display this
		String disp=theApp.getTimeStamp()+" "+sData+" ("+Integer.toString(bitCount)+")";
		theApp.writeLine(disp,Color.BLACK,theApp.boldFont);
	}
	
	// Add a bit to the bit buffer
	private void addBit (boolean b)	{
		dataBitSet.add(b);
		// Increment the bit counter
		bitCount++;	
	}
		
	
}
