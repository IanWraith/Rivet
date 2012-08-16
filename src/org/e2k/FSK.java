package org.e2k;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FSK {
	
	private final String BAUDOT_LETTERS[]={"N/A","E","<LF>","A"," ","S","I","U","<CR>","D","R","J","N","F","C","K","T","Z","L","W","H","Y","P","Q","O","B","G","<FIG>","M","X","V","<LET>"};
	private final String BAUDOT_NUMBERS[]={"N/A","3","<LF>","-"," ","<BELL>","8","7","<CR>","$","4","'",",","!",":","(","5","+",")","2","#","6","0","1","9","?","&","<FIG>",".","/","=","<LET>"};
	public final int ITA3VALS[]={13,37,56,100,69,21,50,112,70,74,26,42,28,19,97,82,35,11,98,49,22,76,73,25,84,81,67,88,38,14,41,44,52,104,7};
	public final String ITA3LETS[]={"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M","<cr>","<lf>","<fig>","<let>","<alpha>","<beta>","<rep>","<0x68>","<0x7>"};
	public final int CCIR476VALS[]={106,92,46,39,86,85,116,43,78,77,113,45,71,75,83,27,53,105,23,30,101,99,58,29,60,114,89,57,120,108,54,90,15};
	public final String CCIR476LETS[]={"<32>"," ","Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M","<cr>","<lf>","<fig>","<let>","<alpha>"};
	public final String CCIR476NUMS[]={"<32>"," ","1","2","3","4","5","6","7","8","9","0","-","'"," ","%","@","#","*","(",")","+","/",":","=","?",",",".","<cr>","<lf>","<fig>","<let>","<alpha>"};
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
	private List<Double>spectrumVals=new ArrayList<Double>();
	private boolean spectrumRecord=false;
	public double kalmanNew=0.0;
	public double kalmanOld=0.0; 
	
	// Return the number of samples per baud
	public double samplesPerSymbol (double dbaud,double sampleFreq)	{
			return (sampleFreq/dbaud);
		}
	
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
	
	// Does a 80 point FFT on 40 samples (a half symbol) then returns the values of two specific bins
	public double[] doGWHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,40);
		double datar[]=new double[FFT_80_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=20)&&(a<60)) datar[a]=samData[a-20];
			else datar[a]=0.0;
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

	// This returns the percentage difference between x and y
	public double getPercentageDifference (double x,double y)	{
		return (((x-y)/(x+y))*100.0);
	}
	
	// Return a spectrum array as a CSV string
	public String getSpectrumValsString()	{
		int a;
		StringBuffer sb=new StringBuffer();
		for (a=0;a<spectrumVals.size();a++)	{
			double s=spectrumVals.get(a);
			sb.append(Double.toString(s)+",");
		}
		return sb.toString();
	}
	
	// A Kalman filter for use by the FSK early/late gate
	public double kalmanFilter (double in,double cof1,double cof2)	{
		double newo=(cof1*kalmanOld)+(cof2*in);
		kalmanOld=kalmanNew;
		kalmanNew=newo;
		return newo;
	}
	
	// Return a ITA-3 character
	public int retITA3Val (int c)	{
		int a;
		for (a=0;a<ITA3VALS.length;a++)	{
			if (c==ITA3VALS[a]) return a;
		}
		return 0;
	}
	
	// Check if a number if a valid ITA-3 character
	public boolean checkITA3Char (int c)	{
		int a;
		for (a=0;a<ITA3VALS.length;a++)	{
			if (c==ITA3VALS[a]) return true;
		}
		return false;
	}
	
	// Return a CCIR476 character
	public int retCCIR476Val (int c)	{
		int a;
		for (a=0;a<CCIR476VALS.length;a++)	{
			if (c==CCIR476VALS[a]) return a;
		}
		return 0;
	}
	
	// Check if a number if a valid CCIR476 character
	public boolean checkCCIR476Char (int c)	{
		int a;
		for (a=0;a<CCIR476VALS.length;a++)	{
			if (c==CCIR476VALS[a]) return true;
		}
		return false;
	}	
	
	
}
