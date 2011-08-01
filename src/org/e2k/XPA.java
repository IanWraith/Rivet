package org.e2k;

public class XPA extends MFSK {
	
	private int baudRate;
	private int state=0;
	private String outLine;
	private boolean haveOutput=false;
	private int correctionValue=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	
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
		// Look for a sync low (600 Hz) followed by a sync high (1120 Hz)
		if (state==2)	{
			int hfreq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
			if (toneTest (hfreq,600,5)==true)	{
				int lfreq=symbolFreq(circBuf,waveData,(int)samplesPerSymbol,samplesPerSymbol);
				if (toneTest (lfreq,1120,5)==true)	{
					state=3;
					// Reset the sample counter
					sampleCount=0;
					
					// Debug code to check we are at the start of a symbol
					int a;
					double datar[]=circBuf.extractDataDouble(0,(int)samplesPerSymbol);
					for (a=0;a<datar.length;a++)	{
						String str=Double.toString(datar[a]);
						theApp.debugDump(str);
					}
					//////////////////////////////////////////////////////
					
				}
			}	
		}
		// Get valid data
		if (state==3)	{
			// Only do this at the start of each symbol
			if (sampleCount==(int)samplesPerSymbol)	{
				sampleCount=0;
				int freq=symbolFreq(circBuf,waveData,0,samplesPerSymbol);
				
			}
		}
		
		sampleCount++;
		
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
		int currentFreq=doFFT(circBuf,waveData,0,1024);
		// Low start tone
		if (toneTest(currentFreq,520,25)==true)	{
			correctionValue=currentFreq-520;
			line=theApp.getTimeStamp()+" XPA Low Start Tone Found : Correcting by "+Integer.toString(correctionValue)+" Hz";
			return line;
		}
		// High start tone
		else if (toneTest(currentFreq,1280,25)==true)	{
			correctionValue=currentFreq-1280;
			line=theApp.getTimeStamp()+" XPA High Start Tone Found : Correcting by "+Integer.toString(correctionValue)+" Hz";
			return line;
		}
		else return null;
	}
	

}
