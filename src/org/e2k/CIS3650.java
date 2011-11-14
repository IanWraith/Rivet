package org.e2k;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class CIS3650 extends FSK {

	public final int FFT_128_SIZE=128;
	private DoubleFFT_1D fft128=new DoubleFFT_1D(FFT_128_SIZE);
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
	private int highTone;
	private int lowTone;
	private int centre;
	
	private String line="";
	private int ccount=0;
	
	public CIS3650 (Rivet tapp,int baud)	{
		baudRate=baud;
		theApp=tapp;
	}
	
	
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		
		if (state==0)	{
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.getSampleRate());
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			state=1;
			highTone=-1;
			lowTone=9999;
			line="";
			ccount=0;
			return null;
		}
		
		if (state==1)	{
			
			sampleCount++;
			if (sampleCount<0) return null;
			
			int f=do128FFT(circBuf,waveData,0);
			energyBuffer.addToCircBuffer(getHighSpectrum());
			
			if (f>highTone) highTone=f;
			if (f<lowTone) lowTone=f;
			
			sampleCount++;
			symbolCounter++;
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol*1)) return null;
			int pos=energyBuffer.returnHighestBin();
			// Calculate what the value of the symbol counter should be
			symbolCounter=symbolCounter-pos;
			centre=(highTone+lowTone)/2;
			shift=highTone-lowTone;
			state=2;
		}
		
		if (state==2)	{
			
			if (symbolCounter>=(long)samplesPerSymbol)	{
				symbolCounter=0;		
				int freq=do128FFT(circBuf,waveData,0);
				boolean bit=freqDecision(freq);
				if (bit==true) line=line+"1";
				else line=line+"0";
				ccount++;
				
				if (ccount==60)	{
					outLines[0]=line;
					line="";
					ccount=0;
				}
				
			}
				
		}
		
		sampleCount++;
		symbolCounter++;
		return outLines;
	}
	
	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
	
	public int do128FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_128_SIZE);
		fft128.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	
	private boolean freqDecision (int freq)	{
		if (freq>centre) return true;
		else return false;
	}
}
