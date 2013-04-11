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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// FSK200/1000 Protocol Description
// 
// BLOCK 000
// ---------
// 
// 00 000 SSSSSSSS These are the sync bytes always 
// 01 008 SSSSSSSS 0x7d12b0e6
// 02 016 SSSSSSSS
// 03 024 SSSSSSSS
// 
// 04 032 CCCCCCCC The C bits make up the line number
// 05 040 CCCXXXXX X bits are unknown but are always the same depending on the line number
// 
// 06 048 XXXXXXXX
// 07 056 XXXXXXXX
// 
// 08 064 TTTTXXXX In block 0 the bits T show the number of blocks to follow
// 09 072 XXXXXXXX
// 10 080 TTTXXXXX 
// 
// 11 088 
// 12 096
// 13 104
// 14 112 
// 15 120
// 16 128 
// 17 136
// 18 144 
// 19 152
// 20 160 
// 21 168
// 22 176
// 23 184
// 24 192
// 25 200
// 26 208
// 27 216
// 28 224
// 29 232
// 30 240
// 31 248
// 32 256
// 33 264
// 34 272
// 35 280
// 
// BLOCK 001
// ---------
// 
// 00 000 SSSSSSSS These are the sync bytes always 
// 01 008 SSSSSSSS 0x7d12b0e6
// 02 016 SSSSSSSS
// 03 024 SSSSSSSS
// 04 032 CCCCCCCC The C bits make up the line number
// 05 040 CCCXXXXX X bits are unknown but are always the same depending on the line number
// 06 048 OOOOWWWW W bits have an unknown purpose
// 07 056 BBBBEEEE
// 08 064 OOOOWWWW 
// 09 072 BBBBEEEE B bits appear to be a group count ?
// 10 080 OOOOWWWW  
// 11 088 XXXXXXXX
// 12 096 OOOOWWWW O bits appear to usually be 0x1be9 = 07145 but in 0x00 specials can be 0x1bc0 = 07109 : Possible message type
// 13 104 XXXXEEEE
// 14 112 IIIIDDDD 
// 15 120 GGGGHHHH
// 16 128 IIIIDDDD The bits D are the message date
// 17 136 GGGGHHHH
// 18 144 IIIINNNN The bits N are the message number
// 19 152 GGGGHHHH
// 20 160 IIIINNNN The bits I appear to be the link identifier
// 21 168 GGGGHHHH
// 22 176 JJJJKKKK
// 23 184 LLLLMMMM
// 24 192 JJJJKKKK
// 25 200 LLLLMMMM
// 26 208 JJJJKKKK
// 27 216 LLLLMMMM
// 28 224 JJJJKKKK
// 29 232 LLLLMMMM
// 30 240 
// 31 248
// 32 256
// 33 264
// 34 272
// 35 280
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package org.e2k;

import java.awt.Color;
import javax.swing.JOptionPane;

public class FSK2001000 extends FSK {
	
	private int baudRate=200;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
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
	private int messageTotalBlockCount=0;
	
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
		
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nFSK200/1000 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			return;
		}
		
		// Hunt for the sync sequence
		else if (state==1)	{
			String dout;
			if (sampleCount>0) dout=syncSequenceHunt(circBuf,waveData);
			else dout=null;
			if (dout!=null)	{
				theApp.writeLine(dout,Color.BLACK,theApp.boldFont);
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
				// If there are no or just 1 differences this is a valid block so process it
				if (difSync<2) processBlock();
				// If there have been more than 2880 bits with a header (i.e 10 blocks) we have a serious problem
				if (bitsSinceLastBlockHeader>2880) setState(1);
			}		
		}	
		// Debug only
		else if (state==3)	{
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;
				boolean ibit=fsk2001000FreqHalf(circBuf,waveData,0);
				if (ibit==true) theApp.writeChar("1",Color.BLACK,theApp.boldFont);
				else theApp.writeChar("0",Color.BLACK,theApp.boldFont);
				characterCount++;
				// Display MAXCHARLENGTH characters on a line
				if (characterCount==MAXCHARLENGTH)	{
					theApp.newLineWrite();
					characterCount=0;
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return;			
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
			if (lowTotal>highTotal) out=false;
			else out=true;
		}
		else	{
			// If inverted is set invert the bit returned
			if (lowTotal>highTotal) out=true;
			else out=false;
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
		// Inverse sync 0x82ED4F19
		final String INVSYNC="10000010111011010100111100011001";
		// Sync 0x7D12B0E6
		final String SYNC="01111101000100101011000011100110";
		// If the input String isn't the same length as the SYNC String then we have a serious problem !
		if (comp.length()!=SYNC.length()) return 32;
		int a,dif=0,invdif=0;
		for (a=0;a<comp.length();a++)	{
			if (comp.charAt(a)!=SYNC.charAt(a)) dif++;
			if (comp.charAt(a)!=INVSYNC.charAt(a)) invdif++;
		}
		// If the inverted difference is less than 2 the user must have things the wrong way around
		if (invdif<2)	{
			if (theApp.isInvertSignal()==true) theApp.setInvertSignal(false);
			else theApp.setInvertSignal(true);
			return invdif;
		}
		return dif;
	}
	
	// Process a FSK200/1000 block
	private void processBlock()	{
		String linesOut[]=new String[2];
		// Convert the block to an array of ints
		int data[]=circularBitSet.returnInts();
		// Data[4] and [5] contain the line number (bits 32 to 41)
		int lineNos=(data[4]<<3)+((data[5]&224)>>5);
		// Count the number of missing blocks
		if (bitCount>288) missingBlockCount=missingBlockCount+(bitCount/288);
		// Check if this is a divider block
		if (checkDividerBlock(data)==true)	{
			// Divide the messages with this //
			linesOut[0]="----------------------------------------------------------";
		}
		else	{
			// Display the block
			linesOut[0]="Block No "+Integer.toString(lineNos);
			// If block 0 display the special information
			if (lineNos==0)	{
				// Display the total number of blocks which is encoded into block 0 bits 64,65,66,67,80,81,82
				messageTotalBlockCount=((data[8]&240)>>1)+((data[10]&224)>>5)+1;
				linesOut[0]=linesOut[0]+" : Total Message Size "+Integer.toString(messageTotalBlockCount)+" blocks";
			}
			// If block 1 display the block 1 special information
			else if (lineNos==1)	{
				
				linesOut[0]=linesOut[0]+" "+extractAddressee(data)+" "+extractDate(data)+" "+extractMsgNumber(data)+extractMsgType(data)+extractGroupCount(data)+extractBlock1Mys(data);
			}
			
			linesOut[1]=circularBitSet.extractBitSetasHex();
		}
		bitCount=0;
		bitsSinceLastBlockHeader=0;
		blockCount++;
		// Display the decoded info
		theApp.writeLine(linesOut[0],Color.BLUE,theApp.boldFont);
		theApp.writeLine(linesOut[1],Color.BLACK,theApp.boldFont);
		return;
	}
	
	// Check if this is a divider block
	private boolean checkDividerBlock(int da[])	{
		int a,zeroCount=0;
		for (a=5;a<da.length;a++)	{
			if (da[a]==0) zeroCount++;
		}
		if (zeroCount>=30) return true;
		else return false;
	}
	
	// Extract addressee
	private String extractAddressee (int da[])	{
		int a1=(da[14]&240)<<8;
		int a2=(da[16]&240)<<4;
		int a3=da[18]&240;
		int a4=(da[20]&240)>>4;
		int addr=a1+a2+a3+a4;
		String r=" : Link ID "+String.format("%05d",addr);
		return r;
	}
	
	// Message Type
	private String extractMsgType (int da[])	{
		int a1=(da[6]&240)<<8;
		int a2=(da[8]&240)<<4;
		int a3=da[10]&240;
		int a4=(da[12]&240)>>4;
		int type=a1+a2+a3+a4;
		String r=" : Msg Type "+String.format("%05d",type);
		return r;
	}
	
	// Extract message number
	private String extractMsgNumber (int da[])	{
		int num=((da[18]&15)<<4)+(da[20]&15);
		String r=" : Msg Number "+String.format("%03d",num);
		return r;
	}
	
	// Extract the group count
	private String extractGroupCount (int da[])	{
		int gc=(da[7]&240)+((da[9]&240)>>4);
		String r=" : Group Count (?) "+Integer.toString(gc);
		return r;
	}
	
	// Extract the mystery bits
	private String extractBlock1Mys (int da[])	{
		int a1,a2,a3,a4;
		// W bits
		// Low nibbles from bytes 6,8,10,12
		a1=(da[6]&15)<<12;
		a2=(da[8]&15)<<8;
		a3=(da[10]&15)<<4;
		a4=da[12]&15;
		int w=a1+a2+a3+a4;
		// E bits
		// Low nibbles from bytes 7,9,11,13
		a1=(da[7]&15)<<12;
		a2=(da[9]&15)<<8;
		a3=(da[11]&15)<<4;
		a4=da[13]&15;
		int e=a1+a2+a3+a4;
		// G bits
		// High nibbles from bytes 15,17,19,21
		a1=(da[15]&240)<<8;
		a2=(da[17]&240)<<4;
		a3=da[19]&240;
		a4=(da[21]&240)>>4;
		int g=a1+a2+a3+a4;
		// H bits
		// Low nibbles from bytes 15,17,19,21
		a1=(da[15]&15)<<12;
		a2=(da[17]&15)<<8;
		a3=(da[19]&15)<<4;
		a4=da[21]&15;
		int h=a1+a2+a3+a4;
		// Display these
		String r=" : Unknown "+String.format("%05d",w)+" "+String.format("%05d",e)+" "+String.format("%05d",g)+" "+String.format("%05d",h);
		return r;
	}
	
	// Extract and display the date as a string
	private String extractDate (int d[])	{
		int dval=extractDateNum(d);
		if (dval==1) return " : 1st of month";
		else if (dval==2) return " : 2nd of month";
		else if (dval==3) return " : 3rd of month";
		else if (dval==4) return " : 4th of month";
		else if (dval==5) return " : 5th of month";
		else if (dval==6) return " : 6th of month";
		else if (dval==7) return " : 7th of month";
		else if (dval==8) return " : 8th of month";
		else if (dval==9) return " : 9th of month";
		else if (dval==10) return " : 10th of month";
		else if (dval==11) return " : 11th of month";
		else if (dval==12) return " : 12th of month";
		else if (dval==13) return " : 13th of month";
		else if (dval==14) return " : 14th of month";
		else if (dval==15) return " : 15th of month";
		else if (dval==16) return " : 16th of month";
		else if (dval==17) return " : 17th of month";
		else if (dval==18) return " : 18th of month";
		else if (dval==19) return " : 19th of month";
		else if (dval==20) return " : 20th of month";
		else if (dval==21) return " : 21st of month";
		else if (dval==22) return " : 22nd of month";
		else if (dval==23) return " : 23rd of month";
		else if (dval==24) return " : 24th of month";
		else if (dval==25) return " : 25th of month";
		else if (dval==26) return " : 26th of month";
		else if (dval==27) return " : 27th of month";
		else if (dval==28) return " : 28th of month";
		else if (dval==29) return " : 29th of month";
		else if (dval==30) return " : 30th of month";
		else if (dval==31) return " : 31st of month";
		else return " : Corrupt Date";
		
	}
	
	// Extract the date as an int
	private int extractDateNum (int da[])	{
		int rval=((da[14]&15)<<4)+(da[16]&15);
		if (rval==10) return 1;
		else if (rval==20) return 2;
		else if (rval==30) return 3;
		else if (rval==40) return 4;
		else if (rval==50) return 5;
		else if (rval==60) return 6;
		else if (rval==70) return 7;
		else if (rval==80) return 8;
		else if (rval==90) return 9;
		else if (rval==100) return 10;
		else if (rval==110) return 11;
		else if (rval==120) return 12;
		else if (rval==130) return 13;
		else if (rval==140) return 14;
		else if (rval==150) return 15;
		else if (rval==160) return 16;
		else if (rval==170) return 17;
		else if (rval==180) return 18;
		else if (rval==190) return 19;
		else if (rval==200) return 20;
		else if (rval==210) return 21;
		else if (rval==220) return 22;
		else if (rval==230) return 23;
		else if (rval==240) return 24;
		else if (rval==250) return 25;
		else if (rval==4) return 26;
		else if (rval==14) return 27;
		else if (rval==24) return 28;
		else if (rval==34) return 29;
		else if (rval==44) return 30;
		else if (rval==54) return 31;
		else return 0;
	}
	
	
}
