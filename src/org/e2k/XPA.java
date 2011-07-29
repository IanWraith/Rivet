package org.e2k;

public class XPA extends MFSK {
	
	private int baudRate;
	private int state=0;
	private String outLine;
	private boolean haveOutput=false;
	private int correctionValue=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	
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
		int currentFreq=doDCT(circBuf,waveData,0,(int)samplesPerSymbol,samplesPerSymbol);
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
