package org.e2k;

import java.util.ArrayList;
import java.util.List;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FFT extends Core {
	
	private int highSpectrum;
	private double totalEnergy;
	private double highestValue;
	public final int FFT_64_SIZE=64;
	public final int FFT_80_SIZE=80;
	public final int FFT_106_SIZE=106;
	public final int FFT_160_SIZE=160;
	public final int FFT_176_SIZE=176;
	public final int RDFT_FFT_SIZE=400;
	private int freqBin;
	public DoubleFFT_1D fft64=new DoubleFFT_1D(FFT_64_SIZE);
	public DoubleFFT_1D fft80=new DoubleFFT_1D(FFT_80_SIZE);
	public DoubleFFT_1D fft106=new DoubleFFT_1D(FFT_106_SIZE);
	public DoubleFFT_1D fft160=new DoubleFFT_1D(FFT_160_SIZE);
	public DoubleFFT_1D fft176=new DoubleFFT_1D(FFT_176_SIZE);
	public DoubleFFT_1D RDFTfft=new DoubleFFT_1D(RDFT_FFT_SIZE);
	private double componentDC;
	private List<Double>spectrumVals=new ArrayList<Double>();
	private boolean spectrumRecord=false;

	
	// Combine the complex data returned by the JTransform FFT routine to provide
	// a power spectrum
	public double[] getSpectrum (double[]data)	{
			double spectrum[]=new double[data.length/2];
			double highS=0;
			if (spectrumRecord==true) spectrumVals.clear();
			// Clear the total energy sum
			totalEnergy=0.0;
			int a,count=0;
			componentDC=data[0];
			for (a=2;a<data.length;a=a+2)	{
				spectrum[count]=Math.sqrt(Math.pow(data[a],2.0)+Math.pow(data[a+1],2.0));
				if (spectrumRecord==true) spectrumVals.add(spectrum[count]);
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
			freqBin=bin-1;
			double len=x.length*2;
			double ret=((sampleFreq/len)*bin);
			return (int)ret;
		}
	
	// Find the bin containing the high value from an array of doubles
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
	
	public int getFreqBin ()	{
		return freqBin;
	}
	
	// Returns the DC component of the signal
	public double getComponentDC() {
		return componentDC;
	}
	
	// Return a spectrum array as a CSV string
	public String getSpectrumValsString()	{
		int a;
		StringBuilder sb=new StringBuilder();
		for (a=0;a<spectrumVals.size();a++)	{
			double s=spectrumVals.get(a);
			sb.append(Double.toString(s)+",");
		}
		return sb.toString();
	}
	
	
}
