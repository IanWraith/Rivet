package org.e2k;

public class FSK {
	
	private int baudRate;
	private int shift;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private long energyStartPoint;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();

	public FSK (Rivet tapp,int baud)	{
		baudRate=baud;
		theApp=tapp;
	}
	
	// Return the number of samples per baud
	public double samplesPerSymbol (double dbaud,double sampleFreq)	{
			return (sampleFreq/dbaud);
		}
	
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		
		if (state==0)	{
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.getSampleRate());
			state=1;
			return null;
		}
		
		if (state==1)	{
			
			sampleCount++;
			if (sampleCount<0) return null;
			
			int sample1[]=circBuf.extractData(0,1);
			int sample2[]=circBuf.extractData((int)samplesPerSymbol,1);
			int sample=sample1[0]*sample2[0];
			
			String l=Integer.toString(sample);
			//theApp.debugDump(l);
			
		}
		
		return outLines;
	}
	
	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
	
}
