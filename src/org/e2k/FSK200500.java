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
	private int highBin;
	private int lowBin;
	private boolean inChar[]=new boolean[7];
	private final int MAXCHARLENGTH=80;
	private int bcount;
	private int missingCharCounter=0;
	
	private int adjBuffer[]=new int[9];
	private int adjCounter=0;
	
	private StringBuffer diagBuffer=new StringBuffer();

	// 05 - 472
	// 08 - 484
	// 09 - 346
	// 10 - 385
	// 11 - 424
	
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
			
			diagBuffer.append("ff1[0],ff1[1],ff2[0],ff2[1],earlyE,lateE,Comparator,adj,bcount,high1,high2,bit,char,missing count");
			theApp.debugDump(diagBuffer.toString());
			
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
			characterCount=0;
			lettersMode=true;
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
				bcount=0;
			}
		}
				
		// Decode traffic
		if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				int ibit=fsk200500FreqHalf(circBuf,waveData,0);
				
				diagBuffer.append(",BIT "+Integer.toString(ibit));
				
				// TODO : Get the invert feature working with FSK200/500
				if (theApp.isInvertSignal()==true)	{
					if (ibit==0) ibit=1;
					else ibit=1;
				}
				// If this is a full bit add it to the character buffer
				// If it is a half bit it signals the end of a character
				if (ibit==2)	{
					symbolCounter=((int)samplesPerSymbol/2)+adjVote();
					//String line=Integer.toString(adjVote());
					//theApp.debugDump(line);
					
					// If debugging display the character buffer in binary form + the number of bits since the last character and this baudot character
					if (theApp.isDebug()==true)	{
						lineBuffer.append(getCharBuffer()+" ("+Integer.toString(bcount)+")  "+getBaudotChar());
						characterCount=MAXCHARLENGTH;
					}
					else	{
						// Display the character in the standard way
						String ch=getBaudotChar();
						
						diagBuffer.append(","+ch);
						
						// LF
						if (ch.equals(getBAUDOT_LETTERS(2))) characterCount=MAXCHARLENGTH;
						// CR
						else if (ch.equals(getBAUDOT_LETTERS(8))) characterCount=MAXCHARLENGTH;
						else	{
							lineBuffer.append(ch);
							characterCount++;
							// Does the line buffer end with "162)" if so start a new line
							if (lineBuffer.lastIndexOf("162)")!=-1) characterCount=MAXCHARLENGTH;
						}
					}
					if (bcount!=7)	{
						missingCharCounter++;
						diagBuffer.append(",MISS "+Integer.toString(missingCharCounter));
					}
					bcount=0;
				}
				else	{
					addToCharBuffer(ibit);	
				}
				// If the character count has reached MAXCHARLENGTH then display this line
				if (characterCount>=MAXCHARLENGTH)	{
					outLines[0]=lineBuffer.toString();
					lineBuffer.delete(0,lineBuffer.length());
					characterCount=0;
				}
			//theApp.debugDump(diagBuffer.toString());
			}
		}
		sampleCount++;
		symbolCounter++;
		return outLines;				
	}
	
	// Look for a sequence of 4 alternating tones with 500 Hz difference
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
		// Check there is around 500 Hz of difference between the tones
		difference=freq1-freq2;
		if ((difference<475)||(difference>525) ) return null;
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
		String line=theApp.getTimeStamp()+" FSK200/500 Sync Sequence Found";
		return line;
	}
	
	// Find the frequency of a FSK200/500 symbol
	// Currently the program only supports a sampling rate of 8000 KHz
	private int fsk200500Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doFSK200500_8000FFT(circBuf,waveData,pos,(int)samplesPerSymbol);
			return freq;
		}
		return -1;
	}
	
	// The "normal" way of determining the frequency of a FSK200/500 symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private int fsk200500FreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		int sp=(int)samplesPerSymbol/2;
		// First half
		double ff1[]=do64FFTHalfSymbolBinRequest (circBuf,pos,sp,lowBin,highBin);
		double earlyE=getComponentDC();
		// Last half
		double ff2[]=do64FFTHalfSymbolBinRequest (circBuf,(pos+sp),sp,lowBin,highBin);
		double lateE=getComponentDC();
		// Early/Late Gate
		int ad=Comparator(earlyE,lateE,10.0);
		
		symbolCounter=0;
		addToAdjBuffer(ad);
		
		
		diagBuffer.delete(0,diagBuffer.length());
		
		double comp=Math.abs(earlyE)-Math.abs(lateE);
		
		diagBuffer.append(Double.toString(ff1[0])+","+Double.toString(ff1[1])+","+Double.toString(ff2[0])+","+Double.toString(ff2[1])+","+Double.toString(earlyE)+","+Double.toString(lateE)+","+Double.toString(comp)+","+Integer.toString(ad)+","+Integer.toString(bcount));
		
		
		// 01 - 396
		// 03 - 
		// 04 - 346
		// 05 - 
		// 09 - 348
		// 10 - 333
		// 11 - 332
		// 12 - 365
		// 15 - 371
		// 20 - 
		// 25 - 
		// 30 - 
		
		
		int high1,high2;
		if (ff1[0]>ff1[1]) high1=0;
		else high1=1;
		if (ff2[0]>ff2[1]) high2=0;
		else high2=1;
		
		diagBuffer.append(",HIGH1 "+Integer.toString(high1)+", HIGH2 "+Integer.toString(high2));
		
		// Both the same
		if (high1==high2)	{
			if (high1==0) return 1;
			else return 0;
		}
		else	{
			// Test if this really could be a half bit
			if (checkValid()==true)	{
				// Is this a stop bit
				if (high2>high1) return 2;
				// No this must be a normal full bit
				if ((ff1[0]+ff2[0])>(ff1[1]+ff2[1])) return 1;
				else return 0;
			}
			else	{
				// If there isn't a vaid baudot character in the buffer this can't be a half bit and must be a full bit
				if ((ff1[0]+ff2[0])>(ff1[1]+ff2[1])) return 1;
				else return 0;
			}
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
		// Increment the bit counter
		bcount++;
	}
	
	// Display the inChar buffer in binary when in debug mode
	private String getCharBuffer()	{
		StringBuffer lb=new StringBuffer();
		int a;
		for (a=0;a<7;a++)	{
			if (inChar[a]==true) lb.append("1");
			else lb.append("0");
			if ((a==0)||(a==5)) lb.append(" ");
		}
		return lb.toString();
	}
	
	// Returns the baudot character in the character buffer
	private String getBaudotChar()	{
		int a=0;
		if (inChar[5]==true) a=16;
		if (inChar[4]==true) a=a+8;
		if (inChar[3]==true) a=a+4;
		if (inChar[2]==true) a=a+2;
		if (inChar[1]==true) a++;
		// Look out for figures or letters shift characters
		if (a==0)	{
			return "";
		}
		else if (a==27)	{
			lettersMode=false;
			return "";
		}
		else if (a==31)	{
			lettersMode=true;
			return "";
		}
		// Only return numbers when in FSK200/500 decode mode
		//else if (lettersMode==true) return BAUDOT_LETTERS[a];
		else return getBAUDOT_NUMBERS(a);
	}
	
	// Check if this a valid Baudot character this a start and a stop
	private boolean checkValid()	{
		if ((inChar[0]==false)&&(inChar[6]==true)&&(bcount>=7)) return true;
		else return false;
	}
	
	public String getQuailty()	{
		String line;
		line="There were "+Integer.toString(missingCharCounter)+" missing characters";
		return line;
	}
	
	
	private void addToAdjBuffer (int in)	{
		adjBuffer[adjCounter]=in;
		adjCounter++;
		if (adjCounter==adjBuffer.length) adjCounter=0;
	}
	
	private int adjVote ()	{
		int a,low=0,high=0,mid=0;
			for (a=0;a<adjBuffer.length;a++)	{
			if (adjBuffer[a]==-1) low++;
			else if (adjBuffer[a]==1) high++;
			else if (adjBuffer[a]==0) mid++;
		}
		
		if ((high>low)&&(high>mid)) return 1;
		else if ((low>high)&&(low>mid)) return -1;
		else return 0;
	}
	
	
}
