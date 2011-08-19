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
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.sampleRate);
			state=1;
			sampleCount=0;
			symbolCounter=0;
			waveData.Clear();
			previousCharacter=null;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			theApp.setStatusLabel("Start Tone Hunt");
			return null;
		}
		// Hunting for a start tone
		if (state==1)	{
			outLines[0]=startToneHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				// Have start tone
				state=2;
				theApp.setStatusLabel("Sync Hunt");
				return outLines;
			}
		}
		// Look for a sync high (1120 Hz) 
		if (state==2)	{
			final int ERRORALLOWANCE=20;
			int pos=0;
			int sfft1=doShortFFT (circBuf,waveData,pos);
			if (toneTest(sfft1,1120,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}	
			pos=(int)samplesPerSymbol-SHORT_FFT_SIZE;
			int sfft2=doShortFFT (circBuf,waveData,pos);
			if (toneTest(sfft2,1120,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			// Now set the symbol timing
			state=3;
			// Remember this value as it is the start of the energy values
			syncFoundPoint=sampleCount;
			theApp.setStatusLabel("Sync Found");
			outLines[0]=theApp.getTimeStamp()+" Sync tone found at position "+Long.toString(sampleCount);
		}
		
		// Set the symbol timing
		if (state==3)	{
			doMiniFFT (circBuf,waveData,0);
			energyBuffer.addToCircBuffer((int)getTotalEnergy());
			sampleCount++;
			symbolCounter++;
			// Gather 3 symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*3)) return null;
			// Now find the highest energy value
			long perfectPoint=energyBuffer.returnHighestBin()+syncFoundPoint;
			// Caluclate what the value of the symbol counter should be
			symbolCounter=symbolCounter-perfectPoint;
			state=4;
			theApp.setStatusLabel("Symbol Timing Achieved");
			outLines[0]=theApp.getTimeStamp()+" Symbol timing found at position "+Long.toString(perfectPoint);
			return outLines;
		}
		
		// Get valid data
		if (state==4)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=(long)samplesPerSymbol)	{
				symbolCounter=0;				
				int freq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
				outLines=displayMessage(freq);
			}
		}
		
		sampleCount++;
		symbolCounter++;
		return outLines;
	}
	
	// Hunt for an XPA start tone
	private String startToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		String line;
		int shortFreq=doShortFFT(circBuf,waveData,0);
		// Low start tone
		if (toneTest(shortFreq,520,50)==true)	{
			waveData.shortCorrectionFactor=shortFreq-520;
			int longFreq=doFFT(circBuf,waveData,0,LONG_FFT_SIZE);
			waveData.longCorrectionFactor=longFreq-520;
			line=theApp.getTimeStamp()+" XPA Low Start Tone Found ("+Integer.toString(longFreq)+" Hz)";
			return line;
		}
		// High start tone
		else if (toneTest(shortFreq,1280,50)==true)	{
			waveData.shortCorrectionFactor=shortFreq-1280;
			int longFreq=doFFT(circBuf,waveData,0,LONG_FFT_SIZE);
			waveData.longCorrectionFactor=longFreq-1280;
			line=theApp.getTimeStamp()+" XPA High Start Tone Found ("+Integer.toString(longFreq)+" Hz)";
			return line;
		}
		else return null;
	}
	
	// Return a String for a tone
	private String getChar (int tone,String prevChar)	{
	    int lw=20;
	    if ((tone>(520-lw))&&(tone<(520+lw))) return ("Start Low");
	    else if ((tone>=(600-lw))&&(tone<(600+lw))) return ("Sync Low");
	    else if ((tone>=(680-lw))&&(tone<(680+lw))) return (" ");
	    else if ((tone>=(720-lw))&&(tone<(720+lw))) return ("End Tone");
	    else if ((tone>=(760-lw))&&(tone<(760+lw))) return ("0");
	    else if ((tone>=(800-lw))&&(tone<(800+lw))) return ("1");
	    else if ((tone>=(840-lw))&&(tone<(840+lw))) return ("2");
	    else if ((tone>=(880-lw))&&(tone<(880+lw))) return ("3");
	    else if ((tone>=(920-lw))&&(tone<(920+lw))) return ("4");
	    else if ((tone>=(960-lw))&&(tone<(960+lw))) return ("5");
	    else if ((tone>=(1000-lw))&&(tone<(1000+lw))) return ("6");
	    else if ((tone>=(1040-lw))&&(tone<(1040+lw))) return ("7");
	    else if ((tone>=(1080-lw))&&(tone<(1080+lw))) return ("8");
	    else if ((tone>=(1120-lw))&&(tone<(1120+lw)))	{
	      if (prevChar=="Sync Low") return ("Sync High");
	      else return ("9");
	    }
	    else if ((tone>=(1160-lw))&&(tone<(1160+lw))) return ("Message Start");
	    else if ((tone>=(1200-lw))&&(tone<(1200+lw))) return ("R");
	    else if ((tone>=(1280-lw))&&(tone<(1280+lw))) return ("Start High");
	    else return ("UNID");
	  }
	
	private String[] displayMessage (int freq)	{
		String tChar=getChar(freq,previousCharacter);
		String outLines[]=new String[2];
		int tlength=0,llength=0;
		// If we get two End Tones in a row then stop decoding
		if ((tChar=="R")&&(previousCharacter=="End Tone")) {
			outLines[0]=theApp.getTimeStamp()+" XPA Decode Complete";
			lineBuffer.delete(0,lineBuffer.length());
			state=0;
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
		
		if ((tChar!="Sync High")&&(tChar!="Sync Low"))	{
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
			//outLines[1]="End Tone "+freq+" Hz at pos "+sampleCount;
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
			}
		// Hunt for 666662266262
        if (lineBuffer.indexOf("666662266262")!=-1)	{
        	groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
			outLines[1]="Block Sync";
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
        	}
        // Hunt for 4444444444
        if (lineBuffer.indexOf("4444444444")!=-1)	{
        	groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
			outLines[1]="4444444444";
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
