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
	private final String BAUDOT_LETTERS[]={"N/A","E","<LF>","A"," ","S","I","U","<CR>","D","R","J","N","F","C","K","T","Z","L","W","H","Y","P","Q","O","B","G","<FIG>","M","X","V","<LET>"};
	private final String BAUDOT_NUMBERS[]={"N/A","3","<LF>","-"," ","<BELL>","8","7","<CR>","$","4","'",",","!",":","(","5","\"",")","2","#","6","0","1","9","?","&","<FIG>",".","/","=","<LET>"};
	private boolean lettersMode=true;
	private final int MAXCHARLENGTH=80;
	
	private int bcount;
	
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
			
			baudRate=50;
			
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
				
				if (theApp.isInvertSignal()==true)	{
					if (ibit==0) ibit=1;
					else if (ibit==1) ibit=0;
					else if (ibit==2) ibit=3;
					else if (ibit==3) ibit=2;
				}
				
				// If this is a full bit add it to the character buffer
				// If it is a half bit it signals the end of a character
				if (ibit>1)	{
					
					
					if (bcount!=7) lineBuffer.append("("+Integer.toString(bcount)+") ");
					bcount=0;
					
					
					symbolCounter=(int)samplesPerSymbol/2;
					String ch=getBaudotChar();
					// LF
					if (ch.equals(BAUDOT_LETTERS[2])) characterCount=MAXCHARLENGTH;
					// CR
					else if (ch.equals(BAUDOT_LETTERS[8])) characterCount=MAXCHARLENGTH;
					else	{
						lineBuffer.append(ch);
						if (theApp.isDebug()==false) characterCount++;
						else characterCount=characterCount+3;
					}
				}
				else	{
					addToCharBuffer(ibit);	
				}
				
				if (characterCount>=MAXCHARLENGTH)	{
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
		double ff1[]=do64FFTHalfSymbolBinRequest (circBuf,pos,sp,lowBin,highBin);
		double ff2[]=do64FFTHalfSymbolBinRequest (circBuf,(pos+sp),sp,lowBin,highBin);
		
		int high1,high2;
		if (ff1[0]>ff1[1]) high1=0;
		else high1=1;
		if (ff2[0]>ff2[1]) high2=0;
		else high2=1;
		
		String line;
		line=Integer.toString(high1)+","+Integer.toString(high2)+","+Double.toString(ff1[0])+","+Double.toString(ff1[1])+","+Double.toString(ff2[0])+","+Double.toString(ff2[1]);
		theApp.debugDump(line);
		
		// Both the same
		if (high1==high2)	{
			symbolCounter=gateEarlyLateFSK200500(ff1,ff2);
			
			if (symbolCounter!=0)	{
				theApp.debugDump(Long.toString(symbolCounter));
			}
			
			if (high1==0) return 0;
			else return 1;
		}
		else	{
			if (high2>high1) return 2;
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
		
		bcount++;
	}
	
	// Returns the baudot character in the character buffer
	private String getBaudotChar()	{
		int a=0;
		if (inChar[1]==true) a=16;
		if (inChar[2]==true) a=a+8;
		if (inChar[3]==true) a=a+4;
		if (inChar[4]==true) a=a+2;
		if (inChar[5]==true) a++;
		// If in debug mode just return this number
		if (theApp.isDebug()==true)	return Integer.toString(a)+" ";
		
		theApp.debugDump(BAUDOT_LETTERS[a]);
		
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
		else if (lettersMode==true) return BAUDOT_LETTERS[a];
		else return BAUDOT_NUMBERS[a];
	}
	
}
