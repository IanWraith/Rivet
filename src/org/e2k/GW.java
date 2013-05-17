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
	private double adjBuffer[]=new double[1];
	private int adjCounter=0;
	private CircularBitSet dataBitSet=new CircularBitSet();
	private int characterCount=0;
	private int bitCount;
	private StringBuilder positionReport=new StringBuilder();
	private boolean receivingPositionReport=false;
	private String lastPositionFragment;
	private int positionFragmentCounter=0;
	private long fragmentStartTime=0;
	
	public GW (Rivet tapp)	{
		theApp=tapp;
		dataBitSet.setTotalLength(200);
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
					dataBitSet.totalClear();
				}
			}
		}
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol100)	{
				symbolCounter=0;
				boolean ibit=gwFreqHalf(circBuf,waveData,0);
				dataBitSet.add(ibit);
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
				// Increment the bit counter
				bitCount++;	
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
		int fr=do100baudFFT(circBuf,waveData,start);
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
			processGWData();
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
	

	// Return the GW station name
	private String stationName (int id)	{
		if (id==0x33) return "LFI, Rogaland, Norway";
		// HLF was 0x47
		else if (id==0xb8) return "HLF, Seoul, South Korea";
		else if (id==0x4e) return "VCS, Halifax, Canada";
		else if (id==0x5d) return "KEJ, Honolulu, Hawaii ";
		else if (id==0x5e) return "CPK, Santa Cruz, Bolivia";
		// A9M was 0x5f
		else if (id==0xbe) return "A9M, Hamala, Bahrain";
		else if (id==0x63) return "9HD, Malta";
		// XSV was 0xc3
		else if (id==0xf0) return "XSV, Tianjin, China";
		else if (id==0xc6) return "9MG, Georgetown, Malaysia";
		else if (id==0xc9) return "ZLA, Awanui, New Zealand";
		else if (id==0xcc) return "HEC, Bern, Switzerland";
		else if (id==0xd2) return "ZSC, Capetown, RSA";
		// KPH was 0xd7
		else if (id==0xfa) return "KPH, San Franisco, USA";
		else if (id==0xd8) return "WNU, Slidell Radio, USA";
		else if (id==0xdb) return "KHF, Agana, Guam";
		// KFS was 0xdc
		else if (id==0xce) return "KFS, Palo Alto, USA";
		// LSD836 was 0xdd
		else if (id==0xee) return "LSD836, Buenos Airos, Argentina";
		else if (id==0xde) return "SAB, Goeteborg, Sweden";
		else if (id==0xe3) return "8PO, Bridgetown, Barbados";
		else return "Unknown";
	}
	
	// The main method to process GW traffic
	private void processGWData ()	{
		// Turn the data into a string
		String sData=dataBitSet.extractSectionFromStart(0,bitCount);
		// Possible channel free marker
		if (bitCount>144)	{
			// Hunt for 0x38A3 or 0011100010100011
			int pos=sData.indexOf("0011100010100011");
			if (pos<8) return;
			pos=pos-8;
			List<Integer> frame=dataBitSet.returnIntsFromStart(pos);
			// Make sure the frame collection contains more than 18 elements
			if (frame.size()<18) return;
			// Free Channel Marker
			// frame[] 8 to 10 should be 0xf2 & frame[] 11 to 16 should all be the same
			if ((frame.get(8).equals(0xf2))&&(frame.get(9).equals(0xf2))&&(frame.get(10).equals(0xf2))&&(frame.get(11).equals(frame.get(12)))&&(frame.get(12).equals(frame.get(13)))&&(frame.get(13).equals(frame.get(14)))&&(frame.get(14).equals(frame.get(15)))&&(frame.get(15).equals(frame.get(16)))&&(frame.get(17).equals(0xff)))	{
					StringBuilder lo=new StringBuilder();
					lo.append(theApp.getTimeStamp());
					lo.append(" GW Free Channel Marker from Station Code 0x"+Integer.toHexString(frame.get(12))+" ("+stationName(frame.get(12))+") ");
					int a;
					for (a=0;a<18;a++)	{
						lo.append(" "+Integer.toHexString(frame.get(a))+" ");
					}
					if (theApp.isViewGWChannelMarkers()==true) theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
					return;
			}
		}
		else if ((bitCount>63)&&(bitCount<95))	{
			// Ensure this starts with 10 which seems to be a part of the sync sequence
			if ((sData.charAt(0)!='1')||(sData.charAt(1)!='0')) return;
			StringBuilder lo=new StringBuilder();
			lo.append(theApp.getTimeStamp()+" GW");
			int type=0,packetCounter=0,subType=0;
			// Type
			if (sData.charAt(2)=='1') type=8;
			if (sData.charAt(3)=='1') type=type+4;
			if (sData.charAt(4)=='1') type=type+2;
			if (sData.charAt(5)=='1') type++;
			// Counter
			if (sData.charAt(6)=='1') packetCounter=1;
			// Sub type
			if (sData.charAt(7)=='1') subType=64;
			if (sData.charAt(8)=='1') subType=subType+32;
			if (sData.charAt(9)=='1') subType=subType+16;
			if (sData.charAt(10)=='1') subType=subType+8;
			if (sData.charAt(11)=='1') subType=subType+4;
			if (sData.charAt(12)=='1') subType=subType+2;
			if (sData.charAt(13)=='1') subType++;
			// Setup the display
			lo.append(" Type="+Integer.toString(type)+" Count="+Integer.toString(packetCounter)+" Subtype="+Integer.toString(subType)+" (");
			// Display the header as binary
			lo.append(dataBitSet.extractSectionFromStart(0,14));
			lo.append(") ("+displayGWAsHex(0)+")");
			
			// If we have been in receiving position report for over 60 seconds it is never going to come so reset
			if (receivingPositionReport==true)	{
				long difTime=(System.currentTimeMillis()/1000)-fragmentStartTime;
				if (difTime>60)	{
					String line=theApp.getTimeStamp()+" position report timeout (fragment Count is "+Integer.toString(positionFragmentCounter)+")";
					theApp.writeLine(line,Color.RED,theApp.boldFont);
					receivingPositionReport=false;
				}
			}
			
			// Is this the start of a position report ?
			if ((type==5)&&(subType==102))	{
				// Clear the position report StringBuilder object
				positionReport.delete(0,positionReport.length());
				lastPositionFragment="";
				receivingPositionReport=true;
				positionFragmentCounter=0;
				// Record the fragment starting time
				fragmentStartTime=System.currentTimeMillis()/1000;
				//positionReport.append(theApp.getTimeStamp()+" "+displayGWAsAscii(0));
				// Display the packet details
				theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
				// Display the content 
				theApp.writeLine(displayGWAsAscii(0),Color.BLUE,theApp.boldFont);
				return;
			}
			// An ongoing position report
			else if ((type==5)&&(subType==86))	{
				// Check if this fragment of the position report is a repeat and can be ignored
				String curFrag=displayGWAsAscii(0);
				// If this starts with a "$" then clear everything
				if (curFrag.startsWith("$")==true)	{
					positionFragmentCounter=0;
					positionReport.delete(0,positionReport.length());
				}
				// Check if this is a repeat
				else if (curFrag.equals(lastPositionFragment)) return;
				// It isn't so add this to the position report StringBuilder
				positionReport.append(curFrag);
				// Store this fragment to check against the next fragment
				lastPositionFragment=curFrag;
				// Count the number of fragment sent
				positionFragmentCounter++;
				// Display the packet details
				theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
				theApp.writeLine(displayGWAsAscii(0),Color.BLUE,theApp.boldFont);
				return;
			}
			// End of a position report
			else if ((type==5)&&(subType==118))	{
				// Display the packet details
				theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
				// Display the content 
				theApp.writeLine(displayGWAsAscii(0),Color.BLUE,theApp.boldFont);
				// Have we a complete position report here ?
				createPositionReportLine();
				return;
			}
			// Type 5 Subtype 54 
			else if ((type==5)&&(subType==54))	{
				// Display the packet details
				theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
				// Display the content 
				theApp.writeLine(displayGWAsAscii(0),Color.BLUE,theApp.boldFont);
				// Have we a complete position report here ?
				createPositionReportLine();
				return;
			}
			// Type 2 Subtype 106
			else if ((type==2)&&(subType==106))	{
				// Display the packet details
				theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
				// Display the content 
				theApp.writeLine(displayGWAsAscii(0),Color.BLUE,theApp.boldFont);
				return;
			}
			// Type 2 Subtype 101
			else if ((type==2)&&(subType==101))	{
				// Display the packet details
				theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
				// Convert the payload to ints
				List<Integer> mInts=dataBitSet.returnIntsFromStart(14);
				// Display the MMSI and contents
				theApp.writeLine(getGW_MMSI(mInts),Color.BLUE,theApp.boldFont);
				return;
			}
			
			// Display everything else
			theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
			return;
		}
		// Handle very short packets
		// These packets are only 8 bits long and may be ACKs
		// Reports suggest they are 10010101 (0x95) but we need to see if this is always the case
		else if ((bitCount>7)&&(bitCount<12))	{
			StringBuilder lo=new StringBuilder();
			String shortContent=dataBitSet.extractSectionFromStart(0,6);
			if ((shortContent.equals("010101"))||(shortContent.equals("101010")))	{
				lo.append(theApp.getTimeStamp()+" GW ACK");
				theApp.writeLine(lo.toString(),Color.BLACK,theApp.boldFont);
			}
			return;
		}
	}
	
	// Display a GW packet as ASCII
	private String displayGWAsAscii (int sPos)	{
		StringBuilder lo=new StringBuilder();
		List<Integer> aInts=dataBitSet.returnIntsFromStart(sPos+14);
		int a;
		for (a=0;a<6;a++)	{
			lo.append(getGWChar(aInts.get(a)));
		}
		return lo.toString();
	}
	
	// Display a GW packet as hex
	private String displayGWAsHex (int sPos)	{
		StringBuilder lo=new StringBuilder();
		List<Integer> aInts=dataBitSet.returnIntsFromStart(sPos+14);
		int a;
		for (a=0;a<6;a++)	{
			if (a>0) lo.append(",");
			lo.append("0x"+Integer.toHexString(aInts.get(a)));
		}
		return lo.toString();
	}
	
	// Convert from a byte to the GW character
	private String getGWChar(int c)	{
		if (c==0x60) return "0";
		else if (c==0x20) return "1";
		else if (c==0x40) return "2";
		else if (c==0x00) return "3";
		else if (c==0x70) return "4";
		else if (c==0x30) return "5";
		else if (c==0x50) return "6";
		else if (c==0x10) return "7";
		else if (c==0x68) return "8";
		else if (c==0x28) return "9";
		else if ((c==0x78)||(c==0x3e)||(c==0x4e)||(c==0xf8)) return "<";
		else if ((c==0x2e)||(c==0x7c)) return ",";
		else if (c==0x5c) return ".";
		else if (c==0x74) return "$";
		else if (c==0x4c) return "*";
		else if (c==0x27) return "A";
		else if ((c==0x47)||(c==0x3f)) return "B";
		else if ((c==0x37)||(c==0x07)) return "E";
		else if (c==0x17) return "G";
		else if ((c==0x7f)||(c==0x63)) return "L";
		else if (c==0x5f) return "N";
		else if ((c==0x1f)||(c==0x43)) return "O";
		else if (c==0x13) return "W";
		else return ("[0x"+Integer.toHexString(c)+"]");
	}
	
	// Decode the 2/101 packets contents into a MMSI and see if we have any details of this ship
	private String getGW_MMSI (List<Integer> mm)	{
		UserIdentifier uid=new UserIdentifier();
		// Decode the MMSI
		String sMMSI=displayGW_MMSI(mm);
		// See if we have a match for this MMSI
		Ship ship=uid.getShipDetails(sMMSI);
		// If nothing returned just return the MMSI
		if (ship==null)	{
			String ret="MMSI : "+sMMSI;
			return ret;
		}
		else	{
			StringBuilder sb=new StringBuilder();
			sb.append("MMSI : "+sMMSI+" ("+ship.getName()+","+ship.getFlag()+")");
			return sb.toString();
		}
	}
	
	// Convert a List of Ints from a 2/101 packet into an MMSI
	public String displayGW_MMSI (List<Integer> mm)	{
		StringBuilder sb=new StringBuilder();
		int a,digitCounter=0;
		for (a=0;a<6;a++)	{
			// High nibble
			int hn=(mm.get(a)&240)>>4;
			// Low nibble
			int ln=mm.get(a)&15;
			// The following nibble
			int followingNibble;
			// Look at the next byte for this unless this is the last byte
			if (a<5) followingNibble=(mm.get(a+1)&240)>>4;
			else followingNibble=0;
			boolean alternate;
			// Low nibble
			// If the nibble following the low nibble (which is in the next byte) is 0x8 or greater
			// then we use the alternate numbering method
			if (followingNibble>=0x8) alternate=true;
			else alternate=false;
			sb.append(convertMMSI(ln,alternate));
			digitCounter++;
			// Once digit counter is 9 then we are done
			if (digitCounter==9) return sb.toString();
			// High nibble
			// If the nibble following the high nibble (which is the low nibble) is 0x8 or greater
			// then we use the alternate numbering scheme
			if (ln>=0x8) alternate=true;
			else alternate=false;
			sb.append(convertMMSI(hn,alternate));
			digitCounter++;
			// Once the digit counter is 9 then we are done
			if (digitCounter==9) return sb.toString();
		}
		return sb.toString();
	}
	
	// Convert a 4 bit nibble into a number
	// GW use this method for encoding ships MMSIs in 2/101 FSK packets
	// I really don't understand the theory behind this encoding method.
	// Big thanks to Alan W for all his help working out the encoding method used here
	private String convertMMSI (int n,boolean alternate)	{
		// 0x0 match with text and Alan W confirm
		if (n==0x0) return "3";
		// 0x1 match with text and Alan W confirm
		else if (n==0x1) return "7";
		// 0x2 match with text and Alan W confirm
		else if (n==0x2)	{
			if (alternate==false) return "1";
			else return "9";
		}
		// 0x3 match with text and Alan W confirm
		else if (n==0x3) return "5";
		// 0x4 match with text and Alan W confirm
		else if (n==0x4) return "2";
		// 0x5 match with text and Alan W confirm
		else if (n==0x5) return "6";
		// 0x6 match with text and Alan W confirm
		else if (n==0x6)	{
			if (alternate==false) return "0";
			else return "8";
		}
		// 0x7 match with text and Alan W confirm
		else if (n==0x7) return "4";
		// 0x8 Alan W confirm
		else if (n==0x8) return "3";
		else if (n==0x9) return "7";
		// 0xa limited Alan W confirmation
		else if (n==0xa)	{
			if (alternate==false) return "1";
			else return "9";
		}
		// 0xb limited Alan W confirmation
		else if (n==0xb) return "5";
		// 0xc limited Alan W confirmation
		else if (n==0xc) return "2";
		// 0xd limited Alan W confirmation
		else if (n==0xd) return "6";
		else if (n==0xe)	{
			if (alternate==false) return "0";
			else return "8";
		}
		else if (n==0xf) return "4";
		else return ("[0x"+Integer.toHexString(n)+"]");
	}
	
	// Check if we have a complete position report and if we have then display it
	private void createPositionReportLine ()	{
		if (receivingPositionReport==false) return;
		else receivingPositionReport=false;
		// Check enough position fragments have been received
		if (positionFragmentCounter<10)	{
			String line="Position fragment count is "+Integer.toString(positionFragmentCounter);
			theApp.writeLine(line,Color.RED,theApp.boldFont);
			return;
		}
		String curFrag=displayGWAsAscii(0);
		if (curFrag.equals(lastPositionFragment)) return;
		// It isn't so add this to the position report StringBuilder
		else positionReport.append(curFrag);
		// Display this position report
		theApp.writeLine(positionReport.toString(),Color.BLUE,theApp.boldFont);
	}
	
	
}
