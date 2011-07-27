package org.e2k;

public class MFSK {
	
	// Do a DCT to return the frequency (in Hz) of a section of the circular buffer
	public int doDCT (CircularDataBuffer circBuf,WaveData waveData,int start,int length,double samplesPerBaud)	{
		int bin,k,highbin=-1;
	    double highval=0.0,transformData;
	    // Get the data from the circular buffer
	    int data[]=circBuf.extractData(start,length);
	    // Do the DCT
	    for (bin=0;bin<length;bin++)	{
	      transformData=0.0;
	      for (k=0;k<length;k++)	{
	        transformData+=data[k]*Math.cos(bin*Math.PI*k/length);
	      }
	      // Check if this is the highest bin value so far
	      if (transformData>highval)	{
	          highval=transformData;
	          highbin=bin;
	        }
	    }
	    if (highbin== -1) return (-1);
	     else return (calcFreqFromBin(highbin,waveData.sampleRate,samplesPerBaud,waveData.correctionFactor));
	}
	
	// Caluclate the frequency from the bin number
	private int calcFreqFromBin (int bin,double sampleFreq,double samplesPerBaud,int correctionFactor)	{
		return (int)(((sampleFreq/samplesPerBaud)*bin)/2.0)-correctionFactor;
	}
	
	// Return the number of samples per baud
	public double samplesPerSymbol (double dbaud,double sampleFreq)	{
		return (sampleFreq/dbaud);
	}
	
	// Test for a specific tone
	public boolean toneTest (int freq,int tone,int errorAllow)	{
	    if ((freq>(tone-errorAllow))&&(freq<(tone+errorAllow))) return true;
	     else return false;
	  }

}
