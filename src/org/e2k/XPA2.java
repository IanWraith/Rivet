package org.e2k;

public class XPA2 extends MFSK {
	
	private double baudRate;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private long syncFoundPoint;
	private String previousCharacter;
	private int groupCount=0;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
		
	public XPA2 (Rivet tapp,double baud)	{
		baudRate=baud;
		theApp=tapp;
	}
	
	public void setBaudRate(double baudRate) {
		this.baudRate = baudRate;
	}

	public double getBaudRate() {
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
		
		
		// Look for a sync (1037 Hz)
		if (state==2)	{
			final int SYNCLOW=1037;
			final int ERRORALLOWANCE=20;
			int pos=0;
			int sfft1=doMidFFT (circBuf,waveData,pos);
			if (toneTest(sfft1,SYNCLOW,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}	
			pos=(int)samplesPerSymbol-MID_FFT_SIZE;
			int sfft2=doMidFFT (circBuf,waveData,pos);
			if (toneTest(sfft2,SYNCLOW,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			state=3;
			// Remember this value as it is the start of the energy values
			syncFoundPoint=sampleCount;
			theApp.setStatusLabel("Sync Found");
			outLines[0]=theApp.getTimeStamp()+" Sync tone found at position "+Long.toString(sampleCount);
		}
				
		// Set the symbol timing
		if (state==3)	{
			doShortFFT (circBuf,waveData,0);
			energyBuffer.addToCircBuffer((int)getTotalEnergy());
			
			String st=Double.toString(getTotalEnergy());
			theApp.debugDump(st);
			
			sampleCount++;
			symbolCounter++;
			// Gather 3 symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*2)) return null;
			
			st="Perfect point is "+energyBuffer.returnLowestBin();
			theApp.debugDump(st);
			st="Sync found at  "+Long.toString(syncFoundPoint);
			theApp.debugDump(st);
			st="Symbol Counter is  "+Long.toString(symbolCounter);
			theApp.debugDump(st);
			
			
			// Now find the lowest energy value
			long perfectPoint=energyBuffer.returnLowestBin()+syncFoundPoint;
			// Caluclate what the value of the symbol counter should be
			symbolCounter=symbolCounter-perfectPoint;
			
			st="Symbol Counter is now  "+Long.toString(symbolCounter);
			theApp.debugDump(st);
			
			state=4;
			theApp.setStatusLabel("Symbol Timing Achieved");
			outLines[0]=theApp.getTimeStamp()+" Symbol timing found at position "+Long.toString(perfectPoint);
			return outLines;
		}
		
		// Get valid data
		if (state==4)	{
			
			// TODO: Fix this code which tries to modify the symbol timing 
			doShortFFT (circBuf,waveData,0);
			energyBuffer.addToCircBuffer((int)getTotalEnergy());
			if (energyBuffer.getBufferCounter()>(int)(samplesPerSymbol*1))	{
				long perfectPoint=energyBuffer.returnLowestBin();
				long predictedPoint=symbolCounter;
				long sc=sampleCount;
				// Caluclate what the value of the symbol counter should be
				//long rsymbolCounter=sampleCount-perfectPoint;
				energyBuffer.setBufferCounter(0);
				
				//String st=Long.toString(perfectPoint)+","+Long.toString(predictedPoint)+","+Long.toString(sc);
				//theApp.debugDump(st);
			}
			//////////////////////////////////////////////////////////////////////
			
			// Only do this at the start of each symbol
			if (symbolCounter>=(int)samplesPerSymbol)	{
				symbolCounter=0;			
			
				int freq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
				
				//String st=Integer.toString(freq)+","+Long.toString(sampleCount);
				//theApp.debugDump(st);
				
				outLines=displayMessage(freq);
			}
		}
		
		sampleCount++;
		symbolCounter++;
		return outLines;
	}
	
	// Hunt for an XPA2 start tone
	private String startToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		String line;
		int midFreq=doMidFFT(circBuf,waveData,0);
		// Low start tone
		if (toneTest(midFreq,980,20)==true)	{
			// Check we have a good low start tone by doing a longer FFT
			int longFreq=doFFT(circBuf,waveData,0);
			if (toneTest(longFreq,980,20)==false) return null;
			waveData.midCorrectionFactor=midFreq-980;
			waveData.longCorrectionFactor=longFreq-980;
			line=theApp.getTimeStamp()+" XPA2 Low Start Tone Found ("+Integer.toString(longFreq)+" Hz)";
			return line;
		}
		else return null;
	}
	
	// Return a String for a tone
	private String getChar (int tone,String prevChar)	{
	    final int lw=7;
	    if ((tone>=(978-lw))&&(tone<(978+lw))) return (" ");
	    else if ((tone>=(1010-lw))&&(tone<(1010+lw))) return ("Sync Low");
	    // Allow for 1041 Hz and 1057 Hz Sync Highs
	    else if ((tone>=(1041-lw))&&(tone<(1057+lw)))	{
	    	if (prevChar=="Sync Low") return ("Sync High");
	    	else return "R";
	    }
	    else if ((tone>=(1073-lw))&&(tone<(1073+lw))) return ("0");
	    else if ((tone>=(1088-lw))&&(tone<(1088+lw))) return ("1");
	    else if ((tone>=(1104-lw))&&(tone<(1104+lw))) return ("2");
	    else if ((tone>=(1119-lw))&&(tone<(1119+lw))) return ("3");
	    else if ((tone>=(1134-lw))&&(tone<(1134+lw))) return ("4");
	    else if ((tone>=(1151-lw))&&(tone<(1151+lw))) return ("5");
	    else if ((tone>=(1167-lw))&&(tone<(1167+lw))) return ("6");
	    else if ((tone>=(1182-lw))&&(tone<(1182+lw))) return ("7");
	    else if ((tone>=(1197-lw))&&(tone<(1197+lw))) return ("8");
	    else if ((tone>=(1212-lw))&&(tone<(1212+lw))) return ("9");
	    else return ("UNID");
	  }

	private String[] displayMessage (int freq)	{
		String tChar=getChar(freq,previousCharacter);
		String outLines[]=new String[2];
		int tlength=0,llength=0;
		// If we get two End Tones in a row then stop decoding
		if ((tChar=="R")&&(previousCharacter=="End Tone")) {
			outLines[0]=theApp.getTimeStamp()+" XPA2 Decode Complete";
			lineBuffer.delete(0,lineBuffer.length());
			state=0;
			return outLines;
		}
		if (tChar=="R")	{
			if (previousCharacter==null) tChar="";
			 else tChar=previousCharacter;
		}
		
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
			outLines[1]="UNID "+freq+" Hz at "+Long.toString(sampleCount);
        	lineBuffer.delete(0,lineBuffer.length());
        	return outLines;
        	}
        
        
        if (tChar=="Sync Low")	{
			outLines[0]="Sync Low "+freq+" Hz at "+Long.toString(sampleCount);
        	return outLines;
        	}
        if (tChar=="Sync High")	{
			outLines[0]="Sync High "+freq+" Hz at "+Long.toString(sampleCount);
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
