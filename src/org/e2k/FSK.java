package org.e2k;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FSK {
	
	private int highSpectrum;
	private double totalEnergy;
	private double highestValue;
	public final int FFT_64_SIZE=64;
	private DoubleFFT_1D fft64=new DoubleFFT_1D(FFT_64_SIZE);
	
	// Return the number of samples per baud
	public double samplesPerSymbol (double dbaud,double sampleFreq)	{
			return (sampleFreq/dbaud);
		}
	
	// Combine the complex data returned by the JTransform FFT routine to provide
	// a power spectrum
	public double[] getSpectrum (double[]data)	{
			double spectrum[]=new double[data.length/2];
			double highS=0;
			// Clear the total energy sum
			totalEnergy=0.0;
			int a,count=0;
			for (a=2;a<data.length;a=a+2)	{
				spectrum[count]=Math.sqrt(Math.pow(data[a],2.0)+Math.pow(data[a+1],2.0));
				if (spectrum[count]>highS) highS=spectrum[count];
				// Add this to the total energy sum
				totalEnergy=totalEnergy+spectrum[count];
				count++;
			}
			setHighSpectrum((int)highS);
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
	public int findHighBin(double[]x)	{
			int a,highBin=-1;
			highestValue=-1;
			for (a=0;a<x.length;a++)	{
				if (x[a]>highestValue)	{
					highestValue=x[a];
					highBin=a;
				}
			}
			// Return the highest bin position
			return highBin+1;
		}

	public int getHighSpectrum() {
		return highSpectrum;
	}

	public void setHighSpectrum(int highSpectrum) {
		this.highSpectrum = highSpectrum;
	}
	
	// Return the total energy sum
	public double getTotalEnergy ()	{
			return this.totalEnergy;
		}
	
	// A Hamming window
	public double windowHamming (double in,int i,int m)	{
			double r=0.54-0.46*Math.cos(2*Math.PI*i/m);
			return (in*r);
		}
		
	// A Blackman window
	public double windowBlackman (double in,int i,int m)	{
			double r=0.42-0.5*Math.cos(2*Math.PI*i/m)+0.08*Math.cos(4*Math.PI*i/m);
			return (in*r);
		}
	
	// Show what percentage of the total the highest spectral value is
	public double getPercentageOfTotal()	{
			double p=(highestValue/totalEnergy)*100.0;
			return p;
		}
	
	public int doFSK200500_8000FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datao[]=circBuf.extractDataDouble(start,40);
	    double datar[]=new double[64];
	    int a,c=0;
	    for (a=0;a<64;a++)	{
	    	if (c<40) datar[a]=datao[c];
	    	else datar[a]=0.0;
	    	c++;
	    }
	    fft64.realForward(datar);
	    double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	public int doFSK200500_11025FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datao[]=circBuf.extractDataDouble(start,55);
	    double datar[]=new double[64];
	    int a,c=0;
	    for (a=0;a<64;a++)	{
	    	if (c<55) datar[a]=datao[c];
	    	else datar[a]=0.0;
	    	c++;
	    }
	    fft64.realForward(datar);
	    double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	// A 64 point FFT is fine for both 8000 KHz and 11025 KHz 
	public int do64FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_64_SIZE);
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}

	// Test for a specific tone
	public boolean toneTest (int freq,int tone,int errorAllow)	{
		if ((freq>(tone-errorAllow))&&(freq<(tone+errorAllow))) return true;
		else return false;
		}
	
	// Given a frequency decide the bit value
	public boolean freqDecision (int freq,int centreFreq,boolean inv)	{
		if (inv==false)	{
			if (freq>centreFreq) return true;
			else return false;
			}
		else	{
			if (freq>centreFreq) return false;
			else return true;
			}
		}
	
}
