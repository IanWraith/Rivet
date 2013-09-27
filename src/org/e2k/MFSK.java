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

public class MFSK extends FSK {
		
	public final int FFT_8_SIZE=8;
	public final int FFT_64_SIZE=64;
	public final int FFT_128_SIZE=128;
	public final int FFT_200_SIZE=200;
	public final int FFT_256_SIZE=256;
	public final int FFT_512_SIZE=512;
	public final int FFT_1024_SIZE=1024;
	private DoubleFFT_1D fft1024=new DoubleFFT_1D(FFT_1024_SIZE);
	private DoubleFFT_1D fft200=new DoubleFFT_1D(FFT_200_SIZE);
	private DoubleFFT_1D fft256=new DoubleFFT_1D(FFT_256_SIZE);
	private DoubleFFT_1D fft512=new DoubleFFT_1D(FFT_512_SIZE);
	private DoubleFFT_1D fft8=new DoubleFFT_1D(FFT_8_SIZE);
	private DoubleFFT_1D fft128=new DoubleFFT_1D(FFT_128_SIZE);
	private DoubleFFT_1D fft64=new DoubleFFT_1D(FFT_64_SIZE);
		
	// We have a problem since FFT sizes must be to a power of 2 but samples per symbol can be any value
	// So instead I am doing a FFT in the middle of the symbol
	public int symbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start,double samplePerSymbol)	{
		double freq=-1;
		// There must be at least LONG_FFT_SIZE samples Per Symbol
		if (samplePerSymbol<FFT_1024_SIZE)	{
			// If not do an 512 point FFT
			// First check this is possible
			if (samplePerSymbol<FFT_512_SIZE) return -1;
			int fftStart=start+(((int)samplePerSymbol-FFT_512_SIZE)/2);
			freq=do512FFT(circBuf,waveData,fftStart);
		}
		else	{
			int fftStart=start+(((int)samplePerSymbol-FFT_1024_SIZE)/2);
			freq=doXPAFFT(circBuf,waveData,fftStart);
		}
		return (int)freq;
	}
	
	public int do1024FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_1024_SIZE);
		fft1024.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int do128FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_128_SIZE);
		fft128.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int do64FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_64_SIZE);
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int do512FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_512_SIZE);
		fft512.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int do256FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_256_SIZE);
		fft256.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int do200FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_200_SIZE);
		fft200.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	

	public int do8FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_8_SIZE);
		fft8.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int doCR36_8000FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datao[]=circBuf.extractDataDouble(start,FFT_200_SIZE);
	    double datar[]=new double[512];
	    int a,c=0;
	    for (a=0;a<512;a++)	{
	    	if (c<200) datar[a]=datao[c];
	    	else datar[a]=0.0;
	    	datar[a]=windowBlackman(datar[a],a,datar.length);
	    	c++;
	    }
	    fft512.realForward(datar);
	    double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int doCR36_11025FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datao[]=circBuf.extractDataDouble(start,275);
	    double datar[]=new double[512];
	    int a,c=0;
	    for (a=0;a<512;a++)	{
	    	if (c<200) datar[a]=datao[c];
	    	else datar[a]=0.0;
	    	c++;
	    }
	    fft512.realForward(datar);
	    double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	private int doXPAFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datao[]=circBuf.extractDataDouble(start,FFT_1024_SIZE);
	    double datar[]=new double[FFT_1024_SIZE];
	    // Run this through a Blackman filter
	    int a;
	    for (a=0;a<datar.length;a++)	{
			datar[a]=windowBlackman(datao[a],a,datao.length);
	    }
	    fft1024.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	
}
