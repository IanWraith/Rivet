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

import javax.swing.JOptionPane;

public class XPA extends MFSK {
	
	private int baudRate=10;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private String previousCharacter;
	private int groupCount=0;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private long syncFoundPoint;
	private int longCorrectionFactor;
	private int shortCorrectionFactor;
	
	public XPA (Rivet tapp,int baud)	{
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
			if (waveData.getSampleRate()>11025)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nXPA recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			state=1;
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			longCorrectionFactor=0;
			shortCorrectionFactor=0;
			previousCharacter=null;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			theApp.setStatusLabel("Start Tone Hunt");
			return null;
		}
		// Hunting for a start tone
		if (state==1)	{
			if (sampleCount>=0) outLines[0]=startToneHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				// Have start tone
				state=2;
				theApp.setStatusLabel("Sync Hunt");
				return outLines;
			}
		}
		// Look for a sync high (1120 Hz) 
		if (state==2)	{
			final int ERRORALLOWANCE=40;
			// First do a short FFT to check for the sync high tone
			int sfft1=do128FFT (circBuf,waveData,0);
			sfft1=sfft1+shortCorrectionFactor;
			if (toneTest(sfft1,1120,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			// If that passes to a proper long FFT to ensure the tone is really there
			int lfft=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
			lfft=lfft+longCorrectionFactor;
			if (toneTest(lfft,1120,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			// Now set the symbol timing
			state=3;
			// Remember this value as it is the start of the energy values
			syncFoundPoint=sampleCount;
			theApp.setStatusLabel("Sync Found");
			outLines[0]=theApp.getTimeStamp()+" High sync tone found";
			return outLines;
		}
		
		// Set the symbol timing
		if (state==3)	{
			do8FFT (circBuf,waveData,0);
			energyBuffer.addToCircBuffer((int)getTotalEnergy());
			sampleCount++;
			symbolCounter++;
			// Gather 3 symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*3)) return null;
			// Now find the highest energy value
			long perfectPoint=energyBuffer.returnHighestBin()+syncFoundPoint;
			// Calculate what the value of the symbol counter should be
			symbolCounter=symbolCounter-perfectPoint;
			state=4;
			theApp.setStatusLabel("Symbol Timing Achieved");
			outLines[0]=theApp.getTimeStamp()+" Symbol timing found";
			return outLines;
		}
		
		// Get valid data
		if (state==4)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=(long)samplesPerSymbol)	{
				symbolCounter=0;				
				int freq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
				freq=freq+longCorrectionFactor;
				outLines=displayMessage(freq,waveData.isFromFile());
			}
		}
		
		sampleCount++;
		symbolCounter++;
		return outLines;
	}
	
	// Hunt for an XPA start tone
	private String startToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		String line;
		final int HighTONE=1280;
		final int LowTONE=520;
		final int toneDIFFERENCE=HighTONE-LowTONE;
		final int ErrorALLOWANCE=40;
		// Look for a low start tone followed by a high start tone
	    int tone1=do1024FFT(circBuf,waveData,0);
	    int tone2=do1024FFT(circBuf,waveData,(int)samplesPerSymbol*1);
	    // Check tone1 is the same as tone2
	    if (tone1!=tone2) return null;
	    int tone3=do1024FFT(circBuf,waveData,(int)samplesPerSymbol*2);
	    // Check the first tone is lower than the second tone
	    if (tone1>tone3) return null;
	    int tone4=do1024FFT(circBuf,waveData,(int)samplesPerSymbol*3);
		// Check tone1 and 2 are the same and that tones 3 and 4 are the same also
	    if ((tone1!=tone2)||(tone3!=tone4)) return null;
	    // Check tones2 and 3 aren't the same
	    if (tone2==tone3) return null;
	    // Find the frequency difference between the tones
	    int difference=tone3-tone1;
	    // Check the difference is correct
	    if ((difference<(toneDIFFERENCE-ErrorALLOWANCE)||(difference>(toneDIFFERENCE+ErrorALLOWANCE)))) return null;
	    // Tones found
	    // Calculate the long error correction factor
	    longCorrectionFactor=LowTONE-tone1;
	    // Calculate the short error correction factor
	    int stone=do128FFT(circBuf,waveData,0);
	    shortCorrectionFactor=LowTONE-stone;
	    // Tell the user
	    line=theApp.getTimeStamp()+" XPA Start Tones Found (correcting by "+Integer.toString(longCorrectionFactor)+" Hz)";
	    return line;
	}
	
	// Return a String for a tone
	private String getChar (int tone,String prevChar)	{
	    final int errorAllowance=20;
	    if ((tone>(520-errorAllowance))&&(tone<(520+errorAllowance))) return ("Start Low");
	    else if ((tone>=(600-errorAllowance))&&(tone<(600+errorAllowance))) return ("Sync Low");
	    else if ((tone>=(680-errorAllowance))&&(tone<(680+errorAllowance))) return (" ");
	    else if ((tone>=(720-errorAllowance))&&(tone<(720+errorAllowance))) return ("End Tone");
	    else if ((tone>=(760-errorAllowance))&&(tone<(760+errorAllowance))) return ("0");
	    else if ((tone>=(800-errorAllowance))&&(tone<(800+errorAllowance))) return ("1");
	    else if ((tone>=(840-errorAllowance))&&(tone<(840+errorAllowance))) return ("2");
	    else if ((tone>=(880-errorAllowance))&&(tone<(880+errorAllowance))) return ("3");
	    else if ((tone>=(920-errorAllowance))&&(tone<(920+errorAllowance))) return ("4");
	    else if ((tone>=(960-errorAllowance))&&(tone<(960+errorAllowance))) return ("5");
	    else if ((tone>=(1000-errorAllowance))&&(tone<(1000+errorAllowance))) return ("6");
	    else if ((tone>=(1040-errorAllowance))&&(tone<(1040+errorAllowance))) return ("7");
	    else if ((tone>=(1080-errorAllowance))&&(tone<(1080+errorAllowance))) return ("8");
	    else if ((tone>=(1120-errorAllowance))&&(tone<(1120+errorAllowance)))	{
	      if (prevChar=="Sync Low") return ("Sync High");
	      else return ("9");
	    }
	    else if ((tone>=(1160-errorAllowance))&&(tone<(1160+errorAllowance))) return ("Message Start");
	    else if ((tone>=(1200-errorAllowance))&&(tone<(1200+errorAllowance))) return ("R");
	    else if ((tone>=(1280-errorAllowance))&&(tone<(1280+errorAllowance))) return ("Start High");
	    else return ("UNID");
	  }
	
	private String[] displayMessage (int freq,boolean isFile)	{
		String tChar=getChar(freq,previousCharacter);
		String outLines[]=new String[2];
		int tlength=0,llength=0;
		// If we get two End Tones in a row then stop decoding
		if ((tChar=="R")&&(previousCharacter=="End Tone")) {
			outLines[0]=theApp.getTimeStamp()+" XPA Decode Complete";
			lineBuffer.delete(0,lineBuffer.length());
			// If this is a file don't keep trying to decode
			// Also stop reading from the file
			if (isFile==true) state=5;
			else state=0;
			return outLines;
		}
		if (tChar=="R") tChar=previousCharacter;
		
		if ((tChar=="Message Start")&&(previousCharacter=="Message Start"))	{
			previousCharacter=tChar;
			return null;
		}
		
		if ((tChar==" ")&&(previousCharacter==" "))	{
			previousCharacter=tChar;
			return null;
		}
		
		// Don't add a space at the start of a line
		if ((tChar==" ")&&(lineBuffer.length()==0))	{
			previousCharacter=tChar;
			return null;
		}
		
		if ((tChar!="Sync High")&&(tChar!="Sync Low")&&(tChar!="Start High")&&(tChar!="Start Low"))	{
			tlength=tChar.length();
			lineBuffer.append(tChar);
			llength=lineBuffer.length();
		}
	
		previousCharacter=tChar;
			
		// Write to a new line after a Message Start
		if (tChar=="Message Start")	{
			groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
			outLines[1]="Message Start";
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
			}
		// Write to a new line after an End Tone
		if (tChar=="End Tone")	{
        	groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
			}
		// Hunt for 666662266262
		final String blockSync="6666622662626";
        if (lineBuffer.indexOf(blockSync)!=-1)	{
        	groupCount=0;
        	tlength=blockSync.length();
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
			if (outLines[0].length()<1) outLines[0]="Block Sync";
			else outLines[1]="Block Sync";
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
        	}
        // Hunt for 4444444444
        final String sbreak="4444444444";
        if (lineBuffer.indexOf(sbreak)!=-1)	{
        	groupCount=0;
        	tlength=sbreak.length();
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
			if (outLines[0].length()<1) outLines[0]=sbreak;
			else outLines[1]=sbreak;
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
        	}
        
        // Hunt for UNID
        if (lineBuffer.indexOf("UNID")!=-1)	{
        	groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
			outLines[1]="UNID "+freq+" Hz";
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
        	}
        
        // Count the group spaces
        if (tChar==" ") groupCount++;
        // After 15 group spaces add a line break
        if (groupCount==15)	{
        	groupCount=0;
        	outLines[0]=lineBuffer.toString();
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
        	}
		return null;
	}
	

}
