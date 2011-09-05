package org.e2k;

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
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
		
	public XPA2 (Rivet tapp)	{
		theApp=tapp;
	}
	
	public void setState(int state) {
		this.state=state;
	}

	public int getState() {
		return state;
	}
	
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
				
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.sampleRate>11025)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nXPA2 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.channels!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			samplesPerSymbol=samplesPerSymbol(BAUDRATE,waveData.sampleRate);
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			setHighestFrequencyUsed(1300);
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
			sampleCount++;
			symbolCounter++;
			// Gather 3 symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*3)) return null;
			// Now find the lowest energy value
			long perfectPoint=energyBuffer.returnLowestBin()+syncFoundPoint;
			// Calculate what the value of the symbol counter should be
			symbolCounter=symbolCounter-perfectPoint;
			state=4;
			theApp.setStatusLabel("Symbol Timing Achieved");
			outLines[0]=theApp.getTimeStamp()+" Symbol timing found at position "+Long.toString(perfectPoint);
			return outLines;
		}
		// Get valid data
		if (state==4)	{			
			// Only do this at the start of each symbol
			if (symbolCounter>=(int)samplesPerSymbol)	{
				symbolCounter=0;					
				int freq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
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
		final int ErrorALLOWANCE=20;
		final int LowTONE=980;
		int midFreq=doMidFFT(circBuf,waveData,0);
		// Low start tone
		if (toneTest(midFreq,LowTONE,ErrorALLOWANCE)==true)	{
			// Check we have a good low start tone by doing a longer FFT
			int longFreq=doFFT(circBuf,waveData,0);
			if (toneTest(longFreq,LowTONE,ErrorALLOWANCE)==false) return null;
			// and check again a symbol later to prevent false positives
			longFreq=doFFT(circBuf,waveData,(int)samplesPerSymbol);
			if (toneTest(longFreq,LowTONE,ErrorALLOWANCE)==false) return null;
			waveData.midCorrectionFactor=midFreq-LowTONE;
			waveData.longCorrectionFactor=longFreq-LowTONE;
			line=theApp.getTimeStamp()+" XPA2 Low Start Tone Found ("+Integer.toString(longFreq)+" Hz)";
			return line;
		}
		else return null;
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
			
		// Write to a new line after an End Tone
		if (tChar=="End Tone")	{
        	groupCount=0;
			lineBuffer.delete((llength-tlength),llength);
			outLines[0]=lineBuffer.toString();
			//outLines[1]="End Tone "+freq+" Hz at pos "+sampleCount;
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
