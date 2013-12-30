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

import javax.swing.JOptionPane;

public class FSK extends FFT {
	private final String BAUDOT_LETTERS[]={"N/A","E","<LF>","A"," ","S","I","U","<CR>","D","R","J","N","F","C","K","T","Z","L","W","H","Y","P","Q","O","B","G","<FIG>","M","X","V","<LET>"};
	private final String BAUDOT_NUMBERS[]={"N/A","3","<LF>","-"," ","<BELL>","8","7","<CR>","$","4","'",",","!",":","(","5","+",")","2","#","6","0","1","9","?","&","<FIG>",".","/","=","<LET>"};
	public final int ITA3VALS[]={13,37,56,100,69,21,50,112,70,74,26,42,28,19,97,82,35,11,98,49,22,76,73,25,84,81,67,88,38,14,41,44,52,104,7};
	public final String ITA3LETS[]={"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M","<cr>","<lf>","<fig>","<let>","<alpha>","<beta>","<rep>","<0x68>","<0x7>"};
	public final int CCIR476VALS[]={106,92,46,39,86,85,116,43,78,77,113,45,71,75,83,27,53,105,23,30,101,99,58,29,60,114,89,57,120,108,54,90,15};
	public final String CCIR476LETS[]={"<32>"," ","Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M","<cr>","<lf>","<fig>","<let>","<alpha>"};
	public final String CCIR476NUMS[]={"<32>"," ","1","2","3","4","5","6","7","8","9","0","-","'"," ","%","@","#","*","(",")","+","/",":","=","?",",",".","<cr>","<lf>","<fig>","<let>","<alpha>"};
	public boolean lettersMode=true;
	public double kalmanNew=0.0;
	public double kalmanOld=0.0; 
			
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
	
	// Determines a frequency for the RTTY class
	public int doRTTY_8000FFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss,double baud)	{
		// 45.45 baud
	    if (baud==45.45)	{
	    	// Get the data from the circular buffer
		    double datar[]=circBuf.extractDataDouble(start,FFT_176_SIZE);
	    	fft176.realForward(datar);
	    	double spec[]=getSpectrum(datar);
	    	int freq=getFFTFreq (spec,waveData.getSampleRate());  
	    	return freq;
	    }	
	    // 50 baud
	    else if (baud==50)	{
	    	// Get the data from the circular buffer
		    double datar[]=circBuf.extractDataDouble(start,FFT_160_SIZE);
	    	fft160.realForward(datar);
	    	double spec[]=getSpectrum(datar);
	    	int freq=getFFTFreq (spec,waveData.getSampleRate());  
	    	return freq;
	    }
	    // 75 baud
	    else if (baud==75)	{
	    	// Get the data from the circular buffer
		    double datar[]=circBuf.extractDataDouble(start,FFT_106_SIZE);
	    	fft106.realForward(datar);
	    	double spec[]=getSpectrum(datar);
	    	int freq=getFFTFreq (spec,waveData.getSampleRate());  
	    	return freq;	    	
	    }
	    // 100 baud
	    else if (baud==100)	return (do100baudFFT(circBuf,waveData,start));
	    // 145 baud
	    else if (baud==145)	return (do145baudFFT(circBuf,waveData,start));
	    // 150 baud
	    else if (baud==150)	return (do150baudFFT(circBuf,waveData,start));
	    // 200 baud
	    else if (baud==200) return (doFSK200500_8000FFT (circBuf,waveData,start,ss));
	    // 300 baud
	    else if (baud==300) return (do300baudFFT(circBuf,waveData,start));
	    // 600 baud
	    else if (baud==600) return (do600baudFFT(circBuf,waveData,start));
	    else return 0;
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
	public double[] do100baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,40);
		double datar[]=new double[FFT_160_SIZE];
		// Run the data through a Blackman window
		int a;
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
	
	
	// Does a 80 point FFT on 27 samples (a half symbol) then returns the values of two specific bins
	public double[] do145baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,27);
		double datar[]=new double[FFT_160_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=66)&&(a<93)) datar[a]=samData[a-66];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
			}
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
		}
	
	
	// Does a 80 point FFT on 26 samples (a half symbol) then returns the values of two specific bins
	public double[] do150baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,26);
		double datar[]=new double[FFT_160_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=67)&&(a<93)) datar[a]=samData[a-67];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
			}
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
		}
	
	// Does a 64 point FFT on 20 samples (a half symbol) then returns the values of two specific bins
	public double[] do200baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,20);
		double datar[]=new double[FFT_64_SIZE];
		// Run the data through a Blackman window
		int a;
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
	
	// Does a 64 point FFT on 13 samples (a half symbol) then returns the values of two specific bins
	public double[] do300baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,13);
		double datar[]=new double[FFT_64_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=25)&&(a<38)) datar[a]=samData[a-25];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
			}
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
		}		
	
	
	// Does a 64 point FFT on 6 samples (a half symbol) then returns the values of two specific bins
	public double[] do600baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		double vals[]=new double[2];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,6);
		double datar[]=new double[FFT_64_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=29)&&(a<35)) datar[a]=samData[a-29];
			else datar[a]=0.0;
			datar[a]=windowBlackman(datar[a],a,datar.length);
			}
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		vals[0]=spec[bin0];
		vals[1]=spec[bin1];
		return vals;
		}	
	
	// Calculates the half symbol bin values for the RTTY code
	public double[] doRTTYHalfSymbolBinRequest (double baud,CircularDataBuffer circBuf,int start,int bin0,int bin1)	{
		int a;
		double vals[]=new double[2];
		// 45.45 baud
		if (baud==45.45)	{
			// Get the data from the circular buffer
			double samData[]=circBuf.extractDataDouble(start,88);
			double datar[]=new double[FFT_176_SIZE];
			// Run the data through a Blackman window
			for (a=0;a<datar.length;a++)	{
				if ((a>=44)&&(a<132)) datar[a]=samData[a-44];
				else datar[a]=0.0;
				datar[a]=windowBlackman(datar[a],a,datar.length);
				}
			fft176.realForward(datar);
			double spec[]=getSpectrum(datar);
			vals[0]=spec[bin0];
			vals[1]=spec[bin1];
			return vals;
		}
		// 50 baud
		else if (baud==50)	{
			// Get the data from the circular buffer
			double samData[]=circBuf.extractDataDouble(start,80);
			double datar[]=new double[FFT_160_SIZE];
			// Run the data through a Blackman window
			for (a=0;a<datar.length;a++)	{
				if ((a>=40)&&(a<120)) datar[a]=samData[a-40];
				else datar[a]=0.0;
				datar[a]=windowBlackman(datar[a],a,datar.length);
				}
			fft160.realForward(datar);
			double spec[]=getSpectrum(datar);
			vals[0]=spec[bin0];
			vals[1]=spec[bin1];
			return vals;
		}
		// 75 baud
		else if (baud==75)	{
			// Get the data from the circular buffer
			double samData[]=circBuf.extractDataDouble(start,53);
			double datar[]=new double[FFT_106_SIZE];
			// Run the data through a Blackman window
			for (a=0;a<datar.length;a++)	{
				if ((a>=26)&&(a<79)) datar[a]=samData[a-26];
				else datar[a]=0.0;
				datar[a]=windowBlackman(datar[a],a,datar.length);
				}
			fft106.realForward(datar);
			double spec[]=getSpectrum(datar);
			vals[0]=spec[bin0];
			vals[1]=spec[bin1];
			return vals;
		}
		// 100 baud
		else if (baud==100) return (do100baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1));
		// 145 baud
		else if (baud==145) return (do145baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1));
		// 150 baud
		else if (baud==150) return (do150baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1));
		// 200 baud 
		else if (baud==200) return (do200baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1));
		// 300 baud 
		else if (baud==300) return (do300baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1));
		// 600 baud 
		else if (baud==600) return (do600baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1));
		else	{
			// We have a problem here !
			JOptionPane.showMessageDialog(null,"Unsupported Baud Rate","Rivet", JOptionPane.ERROR_MESSAGE);
			return null;
		}
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
	
	
	public int do160FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_160_SIZE);
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			datar[a]=windowBlackman(datar[a],a,datar.length);
			}		
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}

	
	// Return the frequency on a 100 baud 160 point FFT sample
	public int do100baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,80);
		double datar[]=new double[FFT_160_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=40)&&(a<120)) datar[a]=samData[a-40];
			else datar[a]=0.0;
			}		
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}	
	
	// Return the frequency on a 145 baud 160 point FFT sample
	public int do145baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,80);
		double datar[]=new double[FFT_160_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=40)&&(a<120)) datar[a]=samData[a-40];
			else datar[a]=0.0;
			}		
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}		
	
	// Return the frequency on a 150 baud 160 point FFT sample
	public int do150baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,55);
		double datar[]=new double[FFT_160_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=52)&&(a<107)) datar[a]=samData[a-52];
			else datar[a]=0.0;
			}		
		fft160.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}		
	
	// Return the frequency on a 600 baud 64 point FFT sample
	public int do600baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,13);
		double datar[]=new double[FFT_64_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=25)&&(a<38)) datar[a]=samData[a-25];
			else datar[a]=0.0;
			}		
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
		}	
	
	
	// Return the frequency on a 300 baud 64 point FFT sample
	public int do300baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,26);
		double datar[]=new double[FFT_64_SIZE];
		// Run the data through a Blackman window
		int a;
		for (a=0;a<datar.length;a++)	{
			if ((a>=19)&&(a<45)) datar[a]=samData[a-19];
			else datar[a]=0.0;
			}		
		fft64.realForward(datar);
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
