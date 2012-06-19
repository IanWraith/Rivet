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
	private int totalCharacterCount=0;
	private int highBin;
	private int lowBin;
	private final int MAXCHARLENGTH=80;
	private double adjBuffer[]=new double[5];
	private int adjCounter=0;
	private int buffer7;
	private int buffer16;
	private int buffer28;
	private int startCount;
	
	public FSK2001000 (Rivet tapp,int baud)	{
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
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Msg Hunt");
		else if (state==3) theApp.setStatusLabel("Decoding");
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
			buffer16=0xaaaa;
			return null;
		}
		
		// Hunt for the sync sequence
		else if (state==1)	{
			if (sampleCount>0) outLines[0]=syncSequenceHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				setState(2);
				energyBuffer.setBufferCounter(0);
			}
		}
		
		// Message Hunt
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;
				boolean ibit=fsk2001000FreqHalf(circBuf,waveData,0);
				//if (ibit==true) lineBuffer.append("1");
				//else lineBuffer.append("0");
				// Check for a long run of zeros and if there is re-sync
				addToBuffer16(ibit);
				if ((buffer16&0xffff)==0)	{
					buffer16=0xaaaa;
					setState(1);
				}
				// Add this to the 28 bit buffer
				addToBuffer28(ibit);
				// Test if this contains 3 valid or inverted ITA3 characters 
				int ita=testBuffer28();
				if (ita>0)	{
					startCount=0;
					characterCount=0;
					totalCharacterCount=0;
					setState(3);
					// Non inverted
					if (ita==1)	{
						int a;
						int chars[]=extract7BitCharsFromBuffer28();
						for (a=0;a<4;a++) 	{
							int c=retITA3Val(chars[a]);
							if (a==0) outLines[0]=ITA3LETS[c];
							else outLines[0]=outLines[0]+ITA3LETS[c];
						}
						
					}
					// Inverted
					else if (ita==2)	{
						// Change the programs invert setting
						if (theApp.isInvertSignal()==false) theApp.setInvertSignal(true);
						else theApp.setInvertSignal(false);
						int a;
						int chars[]=extract7BitCharsFromInvertedBuffer28();
						for (a=0;a<4;a++) 	{
							int c=retITA3Val(chars[a]);
							if (a==0) outLines[0]=ITA3NUMS[c];
							else outLines[0]=outLines[0]+ITA3NUMS[c];
						}
					}
				}
			}
		}
		
		else if (state==3)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;
				boolean ibit=fsk2001000FreqHalf(circBuf,waveData,0);
				addToBuffer7(ibit);
				startCount++;
				

				// Every 7 bits we should have an ITA-3 character
				if (startCount%7==0)	{
					if (checkITA3Char(buffer7)==true)	{
						int c=retITA3Val(buffer7);
						lineBuffer.append(ITA3LETS[c]);
						startCount=0;
						characterCount++;
						totalCharacterCount++;
						
						// Display 50 characters on a line
						if (characterCount==50)	{
							outLines[0]=lineBuffer.toString();
							lineBuffer.delete(0,lineBuffer.length());
							characterCount=0;
						}
						
					}
					
					//startCount=0;
					//buffer7=0;
					//characterCount++;
					// Keep a count of the total number of characters in a message
					//totalCharacterCount++;
					// If a message has gone on for 5000 characters there must be a problem so force an end
					//if (totalCharacterCount>5000) syncState=4;
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
			if (lowTotal>highTotal) return true;
			else return false;
		}
		else	{
			// If inverted is set invert the bit returned
			if (lowTotal>highTotal) return false;
			else return true;
		}
		
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
	
	// Have a 16 bit buffer to detect certain sequences
	private void addToBuffer16(boolean bit)	{
		buffer16=buffer16<<1;
		buffer16=buffer16&0xffff;
		if (bit==true) buffer16++;
	}
	
	private void addToBuffer28(boolean bit)	{
		buffer28=buffer28<<1;
		buffer28=buffer28&0x7FFFFFF;
		if (bit==true) buffer28++;
	}
	
	private int[] extract7BitCharsFromBuffer28()	{
		int c;
		int out[]=new int[4];
		// 1
		c=buffer28&0xFE00000;
		out[0]=c>>21;
		// 2
		c=buffer28&0x1FC000;
		out[1]=c>>14;
		// 3
		c=buffer28&0x3F80;
		out[2]=c>>7;
		// 4
		out[3]=buffer28&0x7F;
		return out;
	}
	
	private int countOnes (int in)	{
		int a,count=0;
		final int bits[]={64,32,16,8,4,2,1};
		for (a=0;a<bits.length;a++)	{
			if ((in&bits[a])>0) count++;
		}
		return count;
	}
	
	private int invertITA3 (int in)	{
		int a,out=0;
		final int bits[]={64,32,16,8,4,2,1};
		for (a=0;a<bits.length;a++)	{
			if ((in&bits[a])==0) out=out+bits[a];
		}
		return out;	
	}
	
	private int testBuffer28()	{
		int a;
		int count[]=new int[4];
		int chars[]=extract7BitCharsFromBuffer28();
		for (a=0;a<4;a++)	{
			count[a]=countOnes(chars[a]);
		}
		// Normal
		if ((count[0]==3)&&(count[1]==3)&&(count[2]==3)&&(count[3]==3)) return 1;
		// Inverted
		else if ((count[0]==4)&&(count[1]==4)&&(count[2]==4)&&(count[3]==4)) return 2;
		// Nothing
		else return 0;
	}
	
	private int[] extract7BitCharsFromInvertedBuffer28 ()	{
		int a;
		int out[]=new int[4];
		int inv[]=extract7BitCharsFromBuffer28();
		for (a=0;a<4;a++)	{
			out[a]=invertITA3(inv[a]);
		}
		return out;
	}
	
	// Add a bit to the 7 bit buffer
	private void addToBuffer7(boolean bit)	{
		buffer7<<=1;
		buffer7=buffer7&0x7F;
		if (bit==true) buffer7++;
	}
	
	// Return a quality indicator
	public String getQuailty()	{
		String line="There were "+Integer.toString(totalCharacterCount)+" characters in this message.";
		return line;
		}
	
}
