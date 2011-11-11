package org.e2k;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FSK {
	
	public final int FFT_128_SIZE=128;
	private DoubleFFT_1D fft128=new DoubleFFT_1D(FFT_128_SIZE);
	private int highSpectrum;
	
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
			energyBuffer.addToCircBuffer(highSpectrum);
			
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
	
	// Combine the complex data returned by the JTransform FFT routine to provide
	// a power spectrum
	public double[] getSpectrum (double[]data)	{
			double spectrum[]=new double[data.length/2];
			double highS=0;
			int a,count=0;
			for (a=2;a<data.length;a=a+2)	{
				spectrum[count]=Math.sqrt(Math.pow(data[a],2.0)+Math.pow(data[a+1],2.0));
				if (spectrum[count]>highS) highS=spectrum[count];
				count++;
			}
			highSpectrum=(int)highS;
			return spectrum;
		}
	
	// Given the real data in a double array return the largest frequency component
	public int getFFTFreq (double[]x,double sampleFreq)	{
			int bin=findHighBin(x);
			double len=x.length*2;
			double ret=((sampleFreq/len)*bin);
			return (int)ret;
		}
	
	// Find the bin containing the hight value from an array of doubles
	private int findHighBin(double[]x)	{
			int a,highBin=-1;
			double highestValue=-1;
			for (a=0;a<x.length;a++)	{
				if (x[a]>highestValue)	{
					highestValue=x[a];
					highBin=a;
				}
			}
			// Return the highest bin position
			return highBin+1;
		}
	
	private boolean freqDecision (int freq)	{
		if (freq>centre) return true;
		else return false;
	}
	
}
