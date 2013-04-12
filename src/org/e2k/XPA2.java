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

public class XPA2 extends MFSK {
	
	private final double BAUDRATE=7.8;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private long syncFoundPoint;
	private String previousCharacter;
	private int groupCount=0;
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int correctionFactor;
	private final int PIVOT=5000;
	private int characterCount;	
	
	public XPA2 (Rivet tapp)	{
		theApp=tapp;
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
	
	// The main decode function
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()>11025.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nXPA2 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			samplesPerSymbol=samplesPerSymbol(BAUDRATE,waveData.getSampleRate());
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			correctionFactor=0;
			previousCharacter=null;
			theApp.setInvertSignal(false);
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			setState(1);
			characterCount=0;
			return;
		}
		// Hunting for a start tone
		if (state==1)	{
			String dout;
			// Do the tone hunt every 100 samples to speed things up
			if ((sampleCount>=0)&&(sampleCount%100==0)) dout=startToneHunt(circBuf,waveData);
			else dout=null;
			if (dout!=null)	{
				// Have start tone
				setState(2);
				theApp.writeLine(dout,Color.BLACK,theApp.italicFont);
				return;
			}
		}
		// Look for a sync (1037 Hz)
		if (state==2)	{
			final int SYNCLOW=1037;
			final int ERRORALLOWANCE=30;
			// Only do this every 100 samples so as not to slow things down
			if (sampleCount%100>0)	{
				sampleCount++;
				symbolCounter++;
				return;
			}
			// Look for two symbols worth of sync tones to prevent false starts
			int freq=xpa2Freq(circBuf,waveData,0);
			if (toneTest(freq,SYNCLOW,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return;
			}
			int freq2=xpa2Freq(circBuf,waveData,(int)samplesPerSymbol);
			if (toneTest(freq2,SYNCLOW,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return;
			}
			setState(3);
			// Remember this value as it is the start of the energy values
			syncFoundPoint=symbolCounter;
			theApp.setStatusLabel("Sync Found");
			theApp.writeLine((theApp.getTimeStamp()+" Sync tone found"),Color.BLACK,theApp.italicFont);
		}	
		// Set the symbol timing
		if (state==3)	{
			final int lookAHEAD=1;
			if (waveData.getSampleRate()==11025.0) do128FFT(circBuf,waveData,0);
			else if (waveData.getSampleRate()==8000.0) do8FFT(circBuf,waveData,0);
			energyBuffer.addToCircBuffer((int)getTotalEnergy());
			sampleCount++;
			symbolCounter++;
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*lookAHEAD)) return;
			// Now find the lowest energy value
			long perfectPoint=energyBuffer.returnLowestBin()+syncFoundPoint+(int)samplesPerSymbol;
			// Calculate what the value of the symbol counter should be
			symbolCounter=(int)samplesPerSymbol-(perfectPoint-sampleCount);
			setState(4);
			theApp.setStatusLabel("Symbol Timing Achieved");
			theApp.writeLine((theApp.getTimeStamp()+" Symbol timing found"),Color.BLACK,theApp.italicFont);
			theApp.newLineWrite();
			return;
		}
		// Get valid data
		if (state==4)	{			
			// Only do this at the start of each symbol
			if (symbolCounter>=(int)samplesPerSymbol)	{
				symbolCounter=0;					
				int freq=xpa2Freq(circBuf,waveData,0);
				displayMessage(freq);
			}
		}
		sampleCount++;
		symbolCounter++;
		return;
	}
	
	// Return a String for a tone
	private String getChar (int tone,String prevChar)	{
	    final int errorAllowance=9;
	    if ((tone>=(978-errorAllowance))&&(tone<(978+errorAllowance))) return (" ");
	    else if ((tone>=(1010-errorAllowance))&&(tone<(1010+errorAllowance))) return ("Sync Low");
	    else if ((tone>=(1026-errorAllowance))&&(tone<(1026+errorAllowance))) return ("End Tone");
	    // Allow for 1041 Hz and 1057 Hz Sync Highs
	    else if ((tone>=(1041-errorAllowance))&&(tone<(1057+errorAllowance)))	{
	    	if (prevChar=="Sync Low") return ("Sync High");
	    	else return "R";
	    }
	    else if ((tone>=(1073-errorAllowance))&&(tone<(1073+errorAllowance))) return ("0");
	    else if ((tone>=(1088-errorAllowance))&&(tone<(1088+errorAllowance))) return ("1");
	    else if ((tone>=(1104-errorAllowance))&&(tone<(1104+errorAllowance))) return ("2");
	    else if ((tone>=(1119-errorAllowance))&&(tone<(1119+errorAllowance))) return ("3");
	    else if ((tone>=(1134-errorAllowance))&&(tone<(1134+errorAllowance))) return ("4");
	    else if ((tone>=(1151-errorAllowance))&&(tone<(1151+errorAllowance))) return ("5");
	    else if ((tone>=(1167-errorAllowance))&&(tone<(1167+errorAllowance))) return ("6");
	    else if ((tone>=(1182-errorAllowance))&&(tone<(1182+errorAllowance))) return ("7");
	    else if ((tone>=(1197-errorAllowance))&&(tone<(1197+errorAllowance))) return ("8");
	    else if ((tone>=(1212-errorAllowance))&&(tone<(1212+errorAllowance))) return ("9");
	    else return ("UNID");
	  }

	private void displayMessage (int freq)	{
		String tChar=getChar(freq,previousCharacter);
		// If we get two End Tones in a row then stop decoding
		if ((tChar=="R")&&(previousCharacter=="End Tone")) {
			theApp.writeLine((theApp.getTimeStamp()+" XPA2 Decode Complete"),Color.BLACK,theApp.italicFont);
			setState(1);
			return;
		}
		// Repeat character
		if (tChar=="R")	{
			if (previousCharacter==null) tChar="";
			 else tChar=previousCharacter;
		}
		// Message start
		if ((tChar=="Message Start")&&(previousCharacter=="Message Start"))	{
			previousCharacter=tChar;
			return;
		}
		// Two spaces
		if ((tChar==" ")&&(previousCharacter==" "))	{
			previousCharacter=tChar;
			return;
		}
		// Don't add a space at the start of a line
		if ((tChar==" ")&&(characterCount==0))	{
			previousCharacter=tChar;
			return;
		}
		// Write normal characters to the screen
		if ((tChar!="Sync High")&&(tChar!="Sync Low")&&(tChar!="End Tone")&&(tChar!="UNID")&&(tChar!=""))	{
			theApp.writeChar(tChar,Color.BLACK,theApp.boldFont);
			characterCount++;
		}
		// Remember the current character
		previousCharacter=tChar;
		// Write to a new line after an End Tone
		if (tChar=="End Tone")	{
        	groupCount=0;
        	theApp.writeLine("End Tone",Color.BLACK,theApp.boldFont);
        	return;
			}
		
        // Display UNID info
        if (tChar=="UNID")	{
        	groupCount=0;
        	theApp.writeLine(("UNID "+freq+" Hz"),Color.BLACK,theApp.boldFont);
        	// Add a newline here
        	theApp.newLineWrite();
        	return;
        	}
        // Count the group spaces
        if (tChar==" ") groupCount++;
        // After 15 group spaces add a line break
        if (groupCount==15)	{
        	groupCount=0;
        	characterCount=0;
        	theApp.newLineWrite();
        	return;
        	}
		return;
	}
	
		// Hunt for an XPA2 start tone
		private String startToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
			final int HighTONE=1212;
			final int LowTONE=978;
			final int toneDIFFERENCE=HighTONE-LowTONE;
			final int ErrorALLOWANCE=40;
			// Look for a low start tone followed by a high start tone
		    int tone1=xpa2Freq(circBuf,waveData,0);
		    int tone2=xpa2Freq(circBuf,waveData,(int)samplesPerSymbol*1);
		    // Check tone1 is the same as tone2
		    if (tone1!=tone2) return null;
		    int tone3=xpa2Freq(circBuf,waveData,(int)samplesPerSymbol*2);
		    // Check the first tone is lower than the second tone
		    if (tone1>tone3) return null;
		    int tone4=xpa2Freq(circBuf,waveData,(int)samplesPerSymbol*3);
			// Check tone1 and 2 are the same and that tones 3 and 4 are the same also
		    if ((tone1!=tone2)||(tone3!=tone4)) return null;
		    // Check tones2 and 3 aren't the same
		    if (tone2==tone3) return null;
		    // Find the frequency difference between the tones
		    int difference=tone3-tone1;
		    // Check the difference is correct
		    if ((difference<(toneDIFFERENCE-ErrorALLOWANCE)||(difference>(toneDIFFERENCE+ErrorALLOWANCE)))) return null;
		    // Tones found
		    // Look 6 symbols ahead and check we have a low tone
		    int toneAhead=xpa2Freq(circBuf,waveData,(int)samplesPerSymbol*6);
		    if (toneAhead==tone3)	{
		    	// The signal is inverted
		    	theApp.setInvertSignal(true);
		    	tone3=PIVOT-tone3;
		    	correctionFactor=LowTONE-tone3;
		    }
		    else	{
		    	// Isn't inverted
		    	theApp.setInvertSignal(false);
		    	// Calculate the long error correction factor
		    	correctionFactor=LowTONE-tone1;
		    }  
		    // Tell the user
		    String line=theApp.getTimeStamp()+" XPA2 Start Tones Found (correcting by "+Integer.toString(correctionFactor)+" Hz)";
		    return line;
		}
		
		
		private int xpa2Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
			if (waveData.getSampleRate()==8000.0)	{
				int freq=doXPAFFT(circBuf,waveData,pos);
				if (theApp.isInvertSignal()==false) freq=freq+correctionFactor;
				else freq=PIVOT-freq+correctionFactor;
				return freq;
			}
			else if (waveData.getSampleRate()==11025.0)	{
				int freq=doXPAFFT(circBuf,waveData,pos);
				freq=freq+correctionFactor;
				return freq;
			}
			return -1;
		}

}
