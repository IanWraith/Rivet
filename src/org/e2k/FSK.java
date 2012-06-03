package org.e2k;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FSK {
	
	private final String BAUDOT_LETTERS[]={"N/A","E","<LF>","A"," ","S","I","U","<CR>","D","R","J","N","F","C","K","T","Z","L","W","H","Y","P","Q","O","B","G","<FIG>","M","X","V","<LET>"};
	private final String BAUDOT_NUMBERS[]={"N/A","3","<LF>","-"," ","<BELL>","8","7","<CR>","$","4","'",",","!",":","(","5","+",")","2","#","6","0","1","9","?","&","<FIG>",".","/","=","<LET>"};
	public boolean lettersMode=true;
	private int highSpectrum;
	private double totalEnergy;
	private double highestValue;
	public final int FFT_64_SIZE=64;
	public final int FFT_80_SIZE=80;
	public final int FFT_160_SIZE=160;
	private int freqBin;
	private DoubleFFT_1D fft64=new DoubleFFT_1D(FFT_64_SIZE);
	private DoubleFFT_1D fft80=new DoubleFFT_1D(FFT_80_SIZE);
	private DoubleFFT_1D fft160=new DoubleFFT_1D(FFT_160_SIZE);
	private double componentDC;

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
			componentDC=data[0];
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
			freqBin=bin-1;
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
	
	// Runs a 64 point FFT on a FSK200/500 sample recorded at 8 KHz 
	public int doFSK200500_8000FFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		// Get the data from the circular buffer
	    double datao[]=circBuf.extractDataDouble(start,ss);
	    double datar[]=new double[FFT_64_SIZE];
	    int a;
	    for (a=0;a<datar.length;a++)	{
	    	if ((a>=12)&&(a<52)) datar[a]=datao[a-12];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
	    }
	    fft64.realForward(datar);
	    double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	// 64 point FFT 
	public int do64FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_64_SIZE);
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}
	
	// Does a 64 point FFT then returns the values of two specific bins
	public double[] do64FFTBinRequest (CircularDataBuffer circBuf,WaveData waveData,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_64_SIZE);
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
		}
	
	// Does a 80 point FFT then returns the values of two specific bins
	public double[] do80FFTBinRequest (CircularDataBuffer circBuf,WaveData waveData,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_80_SIZE);
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			datar[a]=windowBlackman(datar[a],a,datar.length);
			}
		fft80.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
		}
	
	public int do80FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_80_SIZE);
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			datar[a]=windowBlackman(datar[a],a,datar.length);
			}		
		fft80.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}
	
	
	public int doCCIR493_160FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,80);
		int a;
		double datar[]=new double[FFT_160_SIZE];
		for (a=0;a<datar.length;a++)	{
			if ((a>=60)&&(a<120)) datar[a]=samData[a-60];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
		}
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}	
	
	
	// Returns two bins from a 160 bin FFT covering half a symbol
	public double[] do160FFTHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		int a;
		double datar[]=new double[FFT_160_SIZE];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,40);
		for (a=0;a<datar.length;a++)	{
			if ((a>=60)&&(a<100)) datar[a]=samData[a-60];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
		}
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
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
	
	public int getFreqBin ()	{
		return freqBin;
	}
	
	
	// Returns two bins from a 64 bin FFT covering half a symbol
	public double[] do64FFTHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int samples,int bin0,int bin1)	{
		double vals[]=new double[2];
		int a;
		double datar[]=new double[FFT_64_SIZE];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,samples);
		for (a=0;a<datar.length;a++)	{
			if ((a>=22)&&(a<42)) datar[a]=samData[a-22];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
		}
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
		}

	// Returns the DC component of the signal
	public double getComponentDC() {
		return componentDC;
	}
	
	// Get a Baudot letter
	public String getBAUDOT_LETTERS(int i) {
		return BAUDOT_LETTERS[i];
	}

	// Get a Baudot number
	public String getBAUDOT_NUMBERS(int i) {
		return BAUDOT_NUMBERS[i];
	}

	
	
}
