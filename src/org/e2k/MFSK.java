// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class MFSK {
	
	public final int MINI_FFT_SIZE=8;
	public final int SHORT_FFT_SIZE=128;
	public final int FFT_200_SIZE=200;
	public final int FFT_256_SIZE=256;
	public final int MID_FFT_SIZE=512;
	public final int LONG_FFT_SIZE=1024;
	private DoubleFFT_1D long_fft=new DoubleFFT_1D(LONG_FFT_SIZE);
	private DoubleFFT_1D fft200=new DoubleFFT_1D(FFT_200_SIZE);
	private DoubleFFT_1D fft256=new DoubleFFT_1D(FFT_256_SIZE);
	private DoubleFFT_1D mid_fft=new DoubleFFT_1D(MID_FFT_SIZE);
	private DoubleFFT_1D mini_fft=new DoubleFFT_1D(MINI_FFT_SIZE);
	private DoubleFFT_1D short_fft=new DoubleFFT_1D(SHORT_FFT_SIZE);
	private double totalEnergy;
	private int highestFrequency=-1;
	private int secondHighestBin=-1;
	private int thirdHighestBin=-1;
	
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
		for (a=0;a<x.length;a++)	{
			if (x[a]>highVal)	{
				highVal=x[a];
				// Store the second and third highest bins also
				thirdHighestBin=secondHighestBin;
				secondHighestBin=highBin+1;
				highBin=a;
			}
		}
		// Return the highest bin position
		return highBin+1;
	}
		
	// Given the real data in a double array return the largest frequency component
	private int getFFTFreq (double[]x,double sampleFreq,int correctionFactor)	{
		int bin=findHighBin(x);
		double len=x.length*2;
		double ret=((sampleFreq/len)*bin)-correctionFactor;
		// If the returned frequency is higher then the highest request frequency use the next one
		if ((secondHighestBin!=-1)&&(highestFrequency!=-1))	{
			if ((int)ret>highestFrequency) ret=((sampleFreq/len)*secondHighestBin)-correctionFactor;
			// If the second highest bin frqeuency is to high try the third highest
			if (((int)ret>highestFrequency)&&(thirdHighestBin!=-1)) ret=((sampleFreq/len)*thirdHighestBin)-correctionFactor;
		}
		return (int)ret;
	}
	
	// We have a problem since FFT sizes must be to a power of 2 but samples per symbol can be any value
	// So instead I am doing a FFT in the middle of the symbol
	public int symbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start,double samplePerSymbol)	{
		// There must be at least LONG_FFT_SIZE samples Per Symbol
		if (samplePerSymbol<LONG_FFT_SIZE) return -1;
		int fftStart=start+(((int)samplePerSymbol-LONG_FFT_SIZE)/2);
		double freq=doFFT(circBuf,waveData,fftStart);
		return (int)freq;
	}
	
	public int doFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,LONG_FFT_SIZE);
		long_fft.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.sampleRate,waveData.longCorrectionFactor);  
		return freq;
	}
	
	public int doShortFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,SHORT_FFT_SIZE);
		short_fft.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.sampleRate,waveData.shortCorrectionFactor);  
		return freq;
	}
	
	public int doMidFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,MID_FFT_SIZE);
		mid_fft.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.sampleRate,waveData.shortCorrectionFactor);  
		return freq;
	}
	
	public int do256FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_256_SIZE);
		fft256.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.sampleRate,waveData.CorrectionFactor256);  
		return freq;
	}
	
	public int do200FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_200_SIZE);
		fft200.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.sampleRate,waveData.CorrectionFactor256);  
		return freq;
	}
	
	public int doMiniFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,MINI_FFT_SIZE);
		mini_fft.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.sampleRate,waveData.shortCorrectionFactor);  
		return freq;
	}
	
	// Combine the complex data returned by the JTransform FFT routine to provide
	// a power spectrum
	private double[] getSpectrum (double[]data)	{
		double spectrum[]=new double[data.length/2];
		// Clear the total energy sum
		totalEnergy=0.0;
		int a,count=0;
		for (a=2;a<data.length;a=a+2)	{
			spectrum[count]=Math.sqrt(Math.pow(data[a],2.0)+Math.pow(data[a+1],2.0));
			// Add this to the total energy sum
			totalEnergy=totalEnergy+spectrum[count];
			count++;
		}
		return spectrum;
	}
	
	// Return the total energy sum
	public double getTotalEnergy ()	{
		return this.totalEnergy;
	}
	
	// Set the highest frequency you want the object to return
	public void setHighestFrequencyUsed (int highf)	{
		highestFrequency=highf;
	}
	

}
