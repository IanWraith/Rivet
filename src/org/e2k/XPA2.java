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
	private int correctionFactor;
	private final int PIVOT=5000;
		
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
			if (waveData.getSampleRate()>11025.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nXPA2 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
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
			state=1;
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
		// Look for a sync (1037 Hz)
		if (state==2)	{
			final int SYNCLOW=1037;
			final int ERRORALLOWANCE=30;
			int freq=xpa2Freq(circBuf,waveData,0);
			if (toneTest(freq,SYNCLOW,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			
			int a;
			int data[]=circBuf.extractData(0,(int)samplesPerSymbol);
			for (a=0;a<data.length;a++)	{
				String line=Integer.toString(data[a]);
				theApp.debugDump(line);
			}
			
			state=3;
			// Remember this value as it is the start of the energy values
			syncFoundPoint=symbolCounter;
			theApp.setStatusLabel("Sync Found");
			outLines[0]=theApp.getTimeStamp()+" Sync tone found";
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
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*lookAHEAD)) return null;
			
			//int a;
			//for (a=0;a<(int)(samplesPerSymbol*lookAHEAD);a++)	{
				//String line=Integer.toString(energyBuffer.directAccess(a));
				//theApp.debugDump(line);
			//}
			
			// Now find the lowest energy value
			long perfectPoint=energyBuffer.returnLowestBin()+syncFoundPoint+(int)samplesPerSymbol;
			// Calculate what the value of the symbol counter should be
			symbolCounter=(int)samplesPerSymbol-(perfectPoint-sampleCount);
			state=4;
			theApp.setStatusLabel("Symbol Timing Achieved");
			outLines[0]=theApp.getTimeStamp()+" Symbol timing found"; 
			return outLines;
		}
		// Get valid data
		if (state==4)	{			
			// Only do this at the start of each symbol
			if (symbolCounter>=(int)samplesPerSymbol)	{
				symbolCounter=0;					
				int freq=xpa2Freq(circBuf,waveData,0);
				outLines=displayMessage(freq);
			}
		}
		
		sampleCount++;
		symbolCounter++;
		return outLines;
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
				int freq=do1024FFT(circBuf,waveData,pos);
				if (theApp.isInvertSignal()==false) freq=freq+correctionFactor;
				else freq=PIVOT-freq+correctionFactor;
				return freq;
			}
			else if (waveData.getSampleRate()==11025.0)	{
				int freq=do1024FFT(circBuf,waveData,pos);
				freq=freq+correctionFactor;
				return freq;
			}
			return -1;
		}
	
	
}
