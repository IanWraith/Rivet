package org.e2k;

public class XPA extends MFSK {
	
	private int baudRate;
	private int state=0;
	private String outLine;
	private boolean haveOutput=false;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	private int symbolCounter=0;
	private String previousCharacter;
	private int groupCount=0;
	private StringBuffer lineBuffer=new StringBuffer();
	
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
	
	public String DisplayOut()	{
		return outLine;
	}
	
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.sampleRate);
			state=1;
			haveOutput=true;
			outLine="Hunting for a start tone";
		}
		// Hunting for a start tone
		if (state==1)	{
			String sout=startToneHunt(circBuf,waveData);
			if (sout!=null)	{
				// Have start tone
				state=2;
				outLine=sout;
				haveOutput=true;
			}
		}
		// Look for a sync low (600 Hz) followed by a sync high (1120 Hz) then another sync low (600 Hz)
		if (state==2)	{
			int hfreq=symbolFreq(true,circBuf,waveData,0,samplesPerSymbol);
			if (toneTest (hfreq,600,25)==true)	{
				int lfreq=symbolFreq(true,circBuf,waveData,(int)samplesPerSymbol,samplesPerSymbol);
				if (toneTest (lfreq,1120,25)==true)	{
					int hfreq2=symbolFreq(true,circBuf,waveData,(int)samplesPerSymbol*2,samplesPerSymbol);
					if (toneTest (hfreq2,600,25)==true)	{	
						waveData.correctionFactor=lfreq-1120;
						state=3;
						outLine=theApp.getTimeStamp()+" Sync Found at "+Long.toString(sampleCount);
						symbolCounter=0;	
						haveOutput=true;
					}					
				}
			}	
		}
		// Get valid data
		if (state==3)	{
			// Only do this at the start of each symbol
			if (symbolCounter==(int)samplesPerSymbol)	{
				symbolCounter=0;
				
				// We are having a problem with XPA_SHORT.WAV at position 11679
				if (sampleCount==11679)	{
					int a=0;
					a++;
				}
				
				int freq=symbolFreq(false,circBuf,waveData,0,samplesPerSymbol);
				displayMessage(freq);
			}
		}
		
		///////////////////////////////////////////////////////////////////////////////
		if (sampleCount==11679)	{
			// Debug code to check we are at the start of a symbol
			int a;
			double datar[]=circBuf.extractDataDouble(0,(int)samplesPerSymbol);
			
			for (a=0;a<datar.length;a++)	{
				String str=Double.toString(datar[a]);
				//theApp.debugDump(str);
			}
			//////////////////////////////////////////////////////
		}
		
		sampleCount++;
		symbolCounter++;
		
	}
	
	public boolean anyOutput()	{
		return haveOutput;
	}
	
	public String getLine()	{
		haveOutput=false;
		return outLine;
	}
	
	// Hunt for an XPA start tone
	private String startToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		String line;
		int currentFreq=doFFT(circBuf,waveData,0,512);
		// Low start tone
		if (toneTest(currentFreq,520,25)==true)	{
			line=theApp.getTimeStamp()+" XPA Low Start Tone Found";
			return line;
		}
		// High start tone
		else if (toneTest(currentFreq,1280,25)==true)	{
			line=theApp.getTimeStamp()+" XPA High Start Tone Found";
			return line;
		}
		else return null;
	}
	
	// Return a String for a tone
	private String getChar (int tone,String prevChar)	{
	    int lw=21;
	    if ((tone>(520-lw))&&(tone<(520+lw))) return ("Start Low");
	    else if ((tone>(600-lw))&&(tone<(600+lw))) return ("Sync Low");
	    else if ((tone>(680-lw))&&(tone<(680+lw))) return (" ");
	    else if ((tone>(720-lw))&&(tone<(720+lw))) return ("\nEnd Tone");
	    else if ((tone>(760-lw))&&(tone<(760+lw))) return ("0");
	    else if ((tone>(800-lw))&&(tone<(800+lw))) return ("1");
	    else if ((tone>(840-lw))&&(tone<(840+lw))) return ("2");
	    else if ((tone>(880-lw))&&(tone<(880+lw))) return ("3");
	    else if ((tone>(920-lw))&&(tone<(920+lw))) return ("4");
	    else if ((tone>(960-lw))&&(tone<(960+lw))) return ("5");
	    else if ((tone>(1000-lw))&&(tone<(1000+lw))) return ("6");
	    else if ((tone>(1040-lw))&&(tone<(1040+lw))) return ("7");
	    else if ((tone>(1080-lw))&&(tone<(1080+lw))) return ("8");
	    else if ((tone>(1120-lw))&&(tone<(1120+lw)))	{
	      if (prevChar=="Sync Low") return ("Sync High");
	      else return ("9");
	    }
	    else if ((tone>(1160-lw))&&(tone<(1160+lw))) return ("\nMessage Start");
	    else if ((tone>(1200-lw))&&(tone<(1200+lw))) return ("R");
	    else if ((tone>(1280-lw))&&(tone<(1280+lw))) return ("Start High");
	    else return ("UNID");
	  }
	
	private void displayMessage (int freq)	{
		
		String tChar=getChar(freq,previousCharacter);
		//if (tChar=="R") tChar=previousCharacter;
		lineBuffer.append(tChar);
		lineBuffer.append(" ");

		if (groupCount<10)	{
			outLine=Integer.toString(freq)+" Hz "+tChar+" at "+Long.toString(sampleCount);
			haveOutput=true;
		}

		groupCount++;
		
		previousCharacter=tChar;
		
	}
	

}
