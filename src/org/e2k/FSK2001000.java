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
	private final int MAXCHARLENGTH=80;
	private double adjBuffer[]=new double[5];
	private int adjCounter=0;
	private CircularBitSet circularBitSet=new CircularBitSet();
	private int bitCount=0;
	private int blockCount=0;
	private int missingBlockCount=0;
	private int bitsSinceLastBlockHeader=0;
	
	public FSK2001000 (Rivet tapp,int baud)	{
		baudRate=baud;
		theApp=tapp;
		circularBitSet.setTotalLength(288);
	}
	
	public void setBaudRate(int baudRate) {
		this.baudRate=baudRate;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Msg Hunt");
		else if (state==3) theApp.setStatusLabel("Debug Only");
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
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.getSampleRate());
			setState(1);
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			// Clear the display side of things
			characterCount=0;
			lettersMode=true;
			lineBuffer.delete(0,lineBuffer.length());
			return null;
		}
		
		// Hunt for the sync sequence
		else if (state==1)	{
			if (sampleCount>0) outLines[0]=syncSequenceHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				if (theApp.isDebug()==true) setState(3);
				else setState(2);
				energyBuffer.setBufferCounter(0);
				bitsSinceLastBlockHeader=0;
			}
		}
		
		// Main message decode section
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;
				boolean ibit=fsk2001000FreqHalf(circBuf,waveData,0);
				circularBitSet.add(ibit);
				bitCount++;
				bitsSinceLastBlockHeader++;
				// Compare the first 32 bits of the circular buffer to the known FSK200/1000 header
				int difSync=compareSync(circularBitSet.extractSection(0,32));
				// If there are no or just 1 differences this is a valid block
				if (difSync<2)	{
					// Count the number of missing blocks
					if (bitCount>288) missingBlockCount=missingBlockCount+(bitCount/288);
					// Display the block
					outLines[0]="Block Start ("+Integer.toString(bitCount)+" bits since last block)";
					outLines[1]=circularBitSet.extractBitSetasHex();
					bitCount=0;
					bitsSinceLastBlockHeader=0;
					blockCount++;
				}
				// If there have been more than 2880 bits with a header (i.e 10 blocks) we have a serious problem
				if (bitsSinceLastBlockHeader>2880) setState(1);
		}
		
		
		}	
		// Debug only
		else if (state==3)	{
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;
				boolean ibit=fsk2001000FreqHalf(circBuf,waveData,0);
				if (ibit==true) lineBuffer.append("1");
				else lineBuffer.append("0");
				characterCount++;
				// Display MAXCHARLENGTH characters on a line
				if (characterCount==MAXCHARLENGTH)	{
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
	
	// Look for a sequence of 4 alternating tones with 1000 Hz difference
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		// Get 4 symbols
		int freq1=fsk2001000Freq(circBuf,waveData,0);
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=fsk2001000Freq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
		// Check we have a high low
		if (freq2>freq1) return null;
		// Check there is around 1000 Hz of difference between the tones
		difference=freq1-freq2;
		if ((difference<975)||(difference>1025) ) return null;
		int freq3=fsk2001000Freq(circBuf,waveData,(int)samplesPerSymbol*2);
		// Don't waste time carrying on if freq1 isn't the same as freq3
		if (freq1!=freq3) return null;
		int freq4=fsk2001000Freq(circBuf,waveData,(int)samplesPerSymbol*3);
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
		String line=theApp.getTimeStamp()+" FSK200/1000 Sync Sequence Found";
		if (theApp.isDebug()==true)	line=line+" (lowBin="+Integer.toString(lowBin)+" highBin="+Integer.toString(highBin)+")";
		return line;
	}
	
	// Find the frequency of a FSK200/1000 symbol
	// Currently the program only supports a sampling rate of 8000 KHz
	private int fsk2001000Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doFSK200500_8000FFT(circBuf,waveData,pos,(int)samplesPerSymbol);
			return freq;
		}
		return -1;
	}
	
	// The "normal" way of determining the frequency of a FSK200/1000 symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private boolean fsk2001000FreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		boolean out;
		int sp=(int)samplesPerSymbol/2;
		// First half
		double early[]=do64FFTHalfSymbolBinRequest (circBuf,pos,sp,lowBin,highBin);
		// Last half
		double late[]=do64FFTHalfSymbolBinRequest (circBuf,(pos+sp),sp,lowBin,highBin);
		// Feed the early late difference into a buffer
		if ((early[0]+late[0])>(early[1]+late[1])) addToAdjBuffer(getPercentageDifference(early[0],late[0]));
		else addToAdjBuffer(getPercentageDifference(early[1],late[1]));
		// Calculate the symbol timing correction
		symbolCounter=adjAdjust();
		// Now work out the binary state represented by this symbol
		double lowTotal=early[0]+late[0];
		double highTotal=early[1]+late[1];
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
		double r=Math.abs(av)/10;
		if (av<0) r=0-r;
		return (int)r;
	}
	
	// Return a quality indicator
	public String getQuailty()	{
		String line="There were "+Integer.toString(blockCount)+" blocks in this message with " +Integer.toString(missingBlockCount)+" missing.";
		return line;
		}
	
	// Compare a String with the known FSK200/1000 block header
	private int compareSync (String comp)	{
		final String SYNC="10000010111011010100111100011001";
		// If the input String isn't the same length as the SYNC String then we have a serious problem !
		if (comp.length()!=SYNC.length()) return 32;
		int a,dif=0;
		for (a=0;a<comp.length();a++)	{
			if (comp.charAt(a)!=SYNC.charAt(a)) dif++;
		}
		return dif;
	}
	
}
