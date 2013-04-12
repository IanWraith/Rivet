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
	private StringBuilder lineBuffer=new StringBuilder();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private long syncFoundPoint;
	private int correctionFactor;
	
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

	// Chage the state and update the status label
	public void setState(int state) {
		this.state=state;
		// Change the status label
		if (state==1) theApp.setStatusLabel("Start Tone Hunt");
		else if (state==2) theApp.setStatusLabel("Sync Hunt");
		else if (state==3) theApp.setStatusLabel("Sync Found");
		else if (state==4) theApp.setStatusLabel("Decoding");
		else if (state==5) theApp.setStatusLabel("Complete");
	}

	public int getState() {
		return state;
	}
	
	// The main decode routine
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()>11025)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nXPA recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			correctionFactor=0;
			previousCharacter=null;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			return;
		}
		// Hunting for a start tone
		if (state==1)	{
			String dout;
			// To speed things up only do this every 100 samples
			if ((sampleCount>=0)&&(sampleCount%100==0)) dout=startToneHunt(circBuf,waveData);
			else dout=null;
			if (dout!=null)	{
				// Have start tone
				setState(2);
				theApp.writeLine(dout,Color.BLACK,theApp.italicFont);
				return;
			}
		}
		// Look for a sync high (1120 Hz) 
		if (state==2)	{
			final int ERRORALLOWANCE=40;
			// Only do this every 100 samples to speed things up
			if (sampleCount%100>0)	{
				sampleCount++;
				symbolCounter++;
				return;
			}
			// Check for a sync high
			int lfft=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
			lfft=lfft+correctionFactor;
			if (toneTest(lfft,1120,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return;
			}
			// & double check
			int lfft2=symbolFreq(circBuf,waveData,(int)samplesPerSymbol,(samplesPerSymbol*2));
			lfft2=lfft2+correctionFactor;
			if (toneTest(lfft2,1120,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return;
			}
			// Now set the symbol timing
			setState(3);
			// Remember this value as it is the start of the energy values
			syncFoundPoint=sampleCount;
			theApp.writeLine((theApp.getTimeStamp()+" High sync tone found"),Color.BLACK,theApp.italicFont);
			return;
		}
		
		// Set the symbol timing
		if (state==3)	{
			do8FFT (circBuf,waveData,0);
			energyBuffer.addToCircBuffer((int)getTotalEnergy());
			sampleCount++;
			symbolCounter++;
			// Gather 3 symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*3)) return;
			// Now find the highest energy value
			long perfectPoint=energyBuffer.returnHighestBin()+syncFoundPoint;
			// Calculate what the value of the symbol counter should be
			symbolCounter=symbolCounter-perfectPoint;
			setState(4);
			theApp.writeLine((theApp.getTimeStamp()+" Symbol timing found"),Color.BLACK,theApp.italicFont);
			return;
		}
		
		// Get valid data
		if (state==4)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=(long)samplesPerSymbol)	{
				symbolCounter=0;				
				int freq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
				freq=freq+correctionFactor;
				displayMessage(freq,waveData.isFromFile());
			}
		}
		
		sampleCount++;
		symbolCounter++;
		return;
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
	    correctionFactor=LowTONE-tone1;
	    // Tell the user
	    line=theApp.getTimeStamp()+" XPA Start Tones Found (correcting by "+Integer.toString(correctionFactor)+" Hz)";
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
	
	private void displayMessage (int freq,boolean isFile)	{
		String tChar=getChar(freq,previousCharacter);
		int tlength=0,llength=0;
		// If we get two End Tones in a row then stop decoding
		if ((tChar=="R")&&(previousCharacter=="End Tone")) {
			theApp.writeLine((theApp.getTimeStamp()+" XPA Decode Complete"),Color.BLACK,theApp.italicFont);
			lineBuffer.delete(0,lineBuffer.length());
			// If this is a file don't keep trying to decode
			// Also stop reading from the file
			if (isFile==true) setState(5);
			else setState(0);
			return;
		}
		if (tChar=="R") tChar=previousCharacter;
		
		if ((tChar=="Message Start")&&(previousCharacter=="Message Start"))	{
			previousCharacter=tChar;
			return;
		}
		
		if ((tChar==" ")&&(previousCharacter==" "))	{
			previousCharacter=tChar;
			return;
		}
		
		// Don't add a space at the start of a line
		if ((tChar==" ")&&(lineBuffer.length()==0))	{
			previousCharacter=tChar;
			return;
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
			theApp.writeLine((lineBuffer.toString()),Color.BLACK,theApp.boldFont);
			theApp.writeLine("Message Start",Color.BLACK,theApp.boldFont);
        	lineBuffer.delete(0,lineBuffer.length());
        	return;
			}
		// Write to a new line after an End Tone
		if (tChar=="End Tone")	{
        	groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			theApp.writeLine((lineBuffer.toString()),Color.BLACK,theApp.boldFont);
        	lineBuffer.delete(0,lineBuffer.length());
        	// All done look for another message
        	setState(1);
        	return;
			}
		// Hunt for 666662266262
		final String blockSync="6666622662626";
        if (lineBuffer.indexOf(blockSync)!=-1)	{
        	groupCount=0;
        	tlength=blockSync.length();
			lineBuffer.delete((llength-tlength),llength);
			if (lineBuffer.length()>0) theApp.writeLine((lineBuffer.toString()),Color.BLACK,theApp.boldFont);
			theApp.writeLine("Block Sync",Color.BLACK,theApp.boldFont);
        	lineBuffer.delete(0,lineBuffer.length());
        	return;
        	}
        // Hunt for 4444444444
        final String sbreak="4444444444";
        if (lineBuffer.indexOf(sbreak)!=-1)	{
        	groupCount=0;
        	tlength=sbreak.length();
			lineBuffer.delete((llength-tlength),llength);
			if (lineBuffer.length()>0) theApp.writeLine((lineBuffer.toString()),Color.BLACK,theApp.boldFont);
			theApp.writeLine(sbreak,Color.BLACK,theApp.boldFont);
        	lineBuffer.delete(0,lineBuffer.length());
        	return;
        	}
        // Hunt for UNID
        if (lineBuffer.indexOf("UNID")!=-1)	{
        	groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			theApp.writeLine((lineBuffer.toString()),Color.BLACK,theApp.boldFont);
			theApp.writeLine(("UNID "+freq+" Hz"),Color.BLACK,theApp.boldFont);
        	lineBuffer.delete(0,lineBuffer.length());
        	return;
        	}
        // Count the group spaces
        if (tChar==" ") groupCount++;
        // After 15 group spaces add a line break
        if (groupCount==15)	{
        	groupCount=0;
        	theApp.writeLine((lineBuffer.toString()),Color.BLACK,theApp.boldFont);
         	lineBuffer.delete(0,lineBuffer.length());
        	return;
        	}
		return;
	}
	
	
	

}
