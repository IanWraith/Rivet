package org.e2k;

public class XPA2 extends MFSK {
	
	private double baudRate=7.5;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	private int symbolCounter=0;
	private String previousCharacter;
	private int groupCount=0;
	private StringBuffer lineBuffer=new StringBuffer();
		
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
		
		
		// Look for a sync low (998 Hz) followed by a sync high (1041 Hz) 
		if (state==2)	{
			final int SYNCLOW=998;
			final int SYNCHIGH=1041;
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
			pos=(int)samplesPerSymbol;
			int sfft3=doMidFFT (circBuf,waveData,pos);
			if (toneTest(sfft3,SYNCHIGH,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			pos=pos+(int)samplesPerSymbol-MID_FFT_SIZE;
			int sfft4=doMidFFT (circBuf,waveData,pos);
			if (toneTest(sfft4,SYNCHIGH,ERRORALLOWANCE)==false)	{
				sampleCount++;
				symbolCounter++;
				return null;
			}
			state=3;
			symbolCounter=0;
			theApp.setStatusLabel("Sync Achieved");
			outLines[0]=theApp.getTimeStamp()+" Sync Achieved at position "+Long.toString(sampleCount);	
		}
		
		
		// Get valid data
		if (state==3)	{
			// Only do this at the start of each symbol
			if (symbolCounter==(int)samplesPerSymbol)	{
				symbolCounter=0;				
				int freq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
				
				//String st=Integer.toString(freq)+","+Long.toString(sampleCount);
				//theApp.debugDump(st);
				
				//outLines=displayMessage(freq);
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
			waveData.midCorrectionFactor=midFreq-980;
			int longFreq=doFFT(circBuf,waveData,0,LONG_FFT_SIZE);
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

}
