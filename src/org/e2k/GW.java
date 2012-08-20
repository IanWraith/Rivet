package org.e2k;

import javax.swing.JOptionPane;

public class GW extends FSK {
	
	private int state=0;
	private int syncState=0;
	private double samplesPerSymbol100;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	public StringBuffer lineBuffer=new StringBuffer();
	private int highTone;
	private int lowTone;
	private int highBin;
	private int lowBin;
	private double adjBuffer[]=new double[5];
	private int adjCounter=0;
	private CircularBitSet syncBitSet=new CircularBitSet();
	private CircularBitSet dataBitSet=new CircularBitSet();
	private int characterCount=0;
	private int bitCount;
	
	public GW (Rivet tapp)	{
		theApp=tapp;
		syncBitSet.setTotalLength(192);
		dataBitSet.setTotalLength(150);
	}
	
	// The main decode routine
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[3];
		
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nGW FSK recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol100=samplesPerSymbol(100.0,waveData.getSampleRate());
			setState(1);
			lineBuffer.delete(0,lineBuffer.length());
			return null;
		}
		else if (state==1)	{
			if (sampleCount>0)	{
				if (syncSequenceHunt(circBuf,waveData)==true)	{
					setState(2);
					syncState=0;
					bitCount=0;
					syncBitSet.clear();
				}
			}
		}
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol100)	{
				symbolCounter=0;
				boolean ibit=gwFreqHalf(circBuf,waveData,0);
				bitCount++;
				
				if (theApp.isDebug()==true)	{
					if (ibit==true) lineBuffer.append("1");
					else lineBuffer.append("0");
					characterCount++;
					// Have we reached the end of a line
					if (characterCount==80)	{
						characterCount=0;
						outLines[0]=lineBuffer.toString();
						lineBuffer.delete(0,lineBuffer.length());
					}
				}
				
				// Hunt for a sync
				if (syncState==0)	{
					syncBitSet.add(ibit);
					
					// Note : The sync may be the 16 bit 1110100110101101
					// rather than the 10 bit 0110101101 
					if (bitCount>=syncBitSet.getTotalLength())	{
						String tSync=syncBitSet.extractSection((syncBitSet.getTotalLength()-8),syncBitSet.getTotalLength());
						
						
						
						if (tSync.equals("11111111")) {
							
							
							//outLines[0]=syncBitSet.extractSection(0,syncBitSet.getTotalLength());
							outLines[0]=theApp.getTimeStamp()+" "+syncBitSet.extractBitSetasHex();
							
							
							//syncState=1;
							bitCount=0;
						}
						else if (bitCount>320) setState(1);
					}
					
				}
				else if (syncState==1)	{
					dataBitSet.add(ibit);
					if (bitCount==150)	{
						
						outLines[0]=theApp.getTimeStamp()+" Sync 0x1ad";
						outLines[1]=dataBitSet.extractSection(0,150);
						
						syncState=0;
						bitCount=0;
					}
				}
				
				
			}
			
		}
	
		
		
		sampleCount++;
		symbolCounter++;
		return outLines;
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
		int fr=do80FFT(circBuf,waveData,start);
		return fr;
	}
	
	// The "normal" way of determining the frequency of a GW FSK symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private boolean gwFreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		boolean out;
		int sp=(int)samplesPerSymbol100/2;
		// First half
		double early[]=doGWHalfSymbolBinRequest(circBuf,pos,lowBin,highBin);
		// Last half
		double late[]=doGWHalfSymbolBinRequest(circBuf,(pos+sp),lowBin,highBin);
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
	
	// Hunt for a four bit alternating sequence with a 200 Hz difference
	private boolean syncSequenceHunt(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0,b0,b1;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		b0=getFreqBin();
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol100*1;
		int f1=getSymbolFreq(circBuf,waveData,pos);
		int f2=getSymbolFreq(circBuf,waveData,(pos+pos));
		int f3=getSymbolFreq(circBuf,waveData,(pos+pos+pos));
		b1=getFreqBin();
		if (f0==f1) return false;
		if (f1!=f3) return false;
		if (f0!=f2) return false;
		if (f0>f1)	{
			highTone=f0;
			highBin=b0;
			lowTone=f1;
			lowBin=b1;
			}
		else	{
			highTone=f1;
			highBin=b1;
			lowTone=f0;
			lowBin=b0;
			}
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return false; 
		// The shift for GW FSK should be should be 200 Hz
		int shift=highTone-lowTone;
		if ((shift>250)||(shift<150)) return false;
		else return true;
	}	
	

}
