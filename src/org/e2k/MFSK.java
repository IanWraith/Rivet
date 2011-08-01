package org.e2k;

public class MFSK {
	
	private FFT fft=new FFT();
	private int fft_length=-1;
	
	// Return the number of samples per baud
	public double samplesPerSymbol (double dbaud,double sampleFreq)	{
		return (sampleFreq/dbaud);
	}
	
	// Test for a specific tone
	public boolean toneTest (int freq,int tone,int errorAllow)	{
	    if ((freq>(tone-errorAllow))&&(freq<(tone+errorAllow))) return true;
	     else return false;
	  }
	
	// Find the bin containing the hight value from an array of doubles
	private int findHighBin(double[]x)	{
		int a,highBin=-1;
		double highVal=-1;
		for (a=0;a<(x.length/2);a++)	{
			if (x[a]>highVal)	{
				highVal=x[a];
				highBin=a;
			}
		}
		return highBin;
	}
	
	// Given the real data in a double array return the largest frequency component
	private int getFFTFreq (double[]x,double sampleFreq,int correctionFactor)	{
		int bin=findHighBin(x);
		double len=x.length;
		return (int)((sampleFreq/len)*bin)-correctionFactor;
	}
	
	// Run an FFT on a part of the circular buffer and return the main frequency component
	public int doFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int length)	{
		// Check if the FFT length has changed
		// and if it has setup the FFT object again
		if (fft_length!=length)	{
			fft.Setup(length);
			fft_length=length;
		}
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,length);
	    double datai[]=new double[length];
	    fft.fft(datar,datai);
		int freq=getFFTFreq (datar,waveData.sampleRate,waveData.correctionFactor);  
		return freq;
	}

}
