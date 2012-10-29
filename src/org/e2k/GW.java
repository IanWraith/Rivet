package org.e2k;

import java.awt.Color;

import javax.swing.JOptionPane;

public class GW extends FSK {
	
	private int state=0;
	private double samplesPerSymbol100;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private int highTone;
	private int lowTone;
	private int highBin;
	private int lowBin;
	private double adjBuffer[]=new double[8];
	private int adjCounter=0;
	private CircularBitSet dataBitSet=new CircularBitSet();
	private int characterCount=0;
	private int bitCount;
	
	public GW (Rivet tapp)	{
		theApp=tapp;
		dataBitSet.setTotalLength(152);
	}
	
	// The main decode routine
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Initial startup
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nGW FSK recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol100=samplesPerSymbol(100.0,waveData.getSampleRate());
			setState(1);
			return;
		}
		else if (state==1)	{
			if (sampleCount>0)	{
				if (syncSequenceHunt(circBuf,waveData)==true)	{
					setState(2);
					bitCount=0;
					dataBitSet.clear();
				}
			}
		}
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol100)	{
				symbolCounter=0;
				boolean ibit=gwFreqHalf(circBuf,waveData,0);
				dataBitSet.add(ibit);
				bitCount++;
				// Debug only
				if (theApp.isDebug()==true)	{
					if (ibit==true) theApp.writeChar("1",Color.BLACK,theApp.boldFont);
					else theApp.writeChar("0",Color.BLACK,theApp.boldFont);
					characterCount++;
					// Have we reached the end of a line
					if (characterCount==80)	{
						characterCount=0;
						theApp.newLineWrite();
					}
				}
				else	{
					// Have we enough data bits to start looking for the sync sequence
					if (bitCount>=dataBitSet.getTotalLength())	{
						int data[]=dataBitSet.returnInts();
						// Look for the sync word then handle any traffic detected
						if ((data[0]&63)==0x25) handleGWTraffic(data);									
					}
					// If we have received more than 500 bits with no valid frame we have a problem
					if (bitCount>500) setState(1);
					
				}	
			}	
		}
		sampleCount++;
		symbolCounter++;
		return;
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
		int fr=do160FFT(circBuf,waveData,start);
		return fr;
	}
	
	// The "normal" way of determining the frequency of a GW FSK symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private boolean gwFreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
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
		double r=Math.abs(av)/15;
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
		b1=getFreqBin();
		// Check this second tone isn't just noise the highest bin must make up 10% of the total
		//if (getPercentageOfTotal()<10.0) return false;
		if (f0==f1) return false;
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
		if ((highTone-lowTone)!=200) return false;
		else return true;
	}	
	
	// Check if a free channel marker frame is OK
	private void handleGWTraffic(int[] frame)	{
		// Free Channel Marker
		// frame[] 9 to 11 should be 0xf2 & frame[] 12 to 17 should all be the same
		if ((frame[9]==0xf2)&&(frame[10]==0xf2)&&(frame[11]==0xf2)&&(frame[12]==frame[13])&&(frame[13]==frame[14])&&(frame[14]==frame[15])&&(frame[15]==frame[16])&&(frame[16]==frame[17])&&(frame[17]!=0xff))	{
			StringBuilder lo=new StringBuilder();
			lo.append(theApp.getTimeStamp());
			lo.append(" GW Free Channel Marker from Station Code 0x"+Integer.toHexString(frame[12])+" ("+stationName(frame[12])+") ");
			int a;
			for (a=0;a<19;a++)	{
				lo.append(" "+Integer.toHexString(frame[a])+" ");
			}
			bitCount=0;
			if (theApp.isViewGWChannelMarkers()==true) theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
			return;
		}
		else if (frame[1]==0x33)	{
			StringBuilder lo=new StringBuilder();
			lo.append(theApp.getTimeStamp()+" GW UNID (0x33) ");
			int a;
			for (a=0;a<19;a++)	{
				lo.append(" "+Integer.toHexString(frame[a])+" ");
			}
			bitCount=48;
			theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
			return;
		}
		else if (frame[1]==0x3f)	{
			StringBuilder lo=new StringBuilder();
			lo.append(theApp.getTimeStamp()+" GW UNID (0x3f) ");
			int a;
			for (a=0;a<19;a++)	{
				lo.append(" "+Integer.toHexString(frame[a])+" ");
			}
			bitCount=48;
			theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
			return;
		}
		else if (frame[1]==0x29)	{
			StringBuilder lo=new StringBuilder();
			lo.append(theApp.getTimeStamp()+" GW UNID (0x29) ");
			int a;
			for (a=0;a<19;a++)	{
				lo.append(" "+Integer.toHexString(frame[a])+" ");
			}
			bitCount=48;
			theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
			return;
		}		
		else return;	
	}
	
	// Return the GW station name
	private String stationName (int id)	{
		if (id==0x33) return "LFI, Rogaland, Norway";
		else if (id==0x47) return "HLF, Seoul, South Korea";
		else if (id==0x4e) return "VCS, Halifax, Canada";
		else if (id==0x5d) return "KEJ, Honolulu, Hawaii ";
		else if (id==0x5e) return "CPK, Santa Cruz, Bolivia";
		else if (id==0x5f) return "A9M, Hamala, Bahrain";
		else if (id==0x63) return "9HD, Malta";
		else if (id==0xc3) return "XSV, Tianjin, China";
		else if (id==0xc9) return "ZLA, Awanui, New Zealand";
		else if (id==0xcc) return "HEC, Bern, Switzerland";
		else if (id==0xd2) return "ZSC, Capetown, RSA";
		else if (id==0xd7) return "KPH, San Franisco, USA";
		else if (id==0xd8) return "WNU, Slidell Radio, USA";
		else if (id==0xdb) return "KHF, Agana, Guam";
		else if (id==0xdc) return "KFS, Palo Alto, USA";
		else if (id==0xdd) return "LSD836, Buenos Airos, Argentinia";
		else if (id==0xde) return "SAB, Goeteborg, Sweden";
		else if (id==0xe3) return "8PO, Bridgetown, Barbados";
		else return "Unknown";
	}
	
	

}
