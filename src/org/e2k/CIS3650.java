package org.e2k;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class CIS3650 extends FSK {

	public final int FFT_128_SIZE=128;
	private DoubleFFT_1D fft128=new DoubleFFT_1D(FFT_128_SIZE);
	private int shift;
	private int state=0;
	private double samplesPerSymbol50;
	private double samplesPerSymbol36;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int highTone;
	private int lowTone;
	private int centre;
	
	private String line="";
	private int ccount=0;
	
	public CIS3650 (Rivet tapp)	{
		theApp=tapp;
	}
	
	
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		
		if (state==0)	{
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol36=samplesPerSymbol(36.0,waveData.getSampleRate());
			samplesPerSymbol50=samplesPerSymbol(50.0,waveData.getSampleRate());
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			state=1;
			line="";
			ccount=0;
			return null;
		}
		
		
		// Look for a 36 baud or a 50 baud alternating sequence
		if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return null;
			int pos=0;
			int f0=do128FFT(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol36*1;
			int f1=do128FFT(circBuf,waveData,pos);
			if (f0==f1) return null;
			pos=(int)samplesPerSymbol36*2;
			int f2=do128FFT(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol36*3;
			int f3=do128FFT(circBuf,waveData,pos);
			// Look for a 36 baud alternating sequence
			if ((f0==f2)&&(f1==f3)&&(f0!=f1)&&(f2!=f3))	{
				outLines[0]=theApp.getTimeStamp()+" CIS 36-50 36 baud sync sequence found";
				if (f0>f1)	{
					highTone=f0;
					lowTone=f1;
				}
				else	{
					highTone=f1;
					lowTone=f0;
				}
				centre=(highTone+lowTone)/2;
				shift=highTone-lowTone;
				// Now we need to look for the start of the 50 baud data
				state=2;
				return outLines;
			}
			// Look for an alternating 50 baud sequence
			pos=(int)samplesPerSymbol50*1;
			f1=do128FFT(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol50*2;
			f2=do128FFT(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol50*3;
			f3=do128FFT(circBuf,waveData,pos);
			if ((f0==f2)&&(f1==f3)&&(f0!=f1)&&(f2!=f3))	{
				outLines[0]=theApp.getTimeStamp()+" CIS 36-50 50 baud sync sequence found";
				if (f0>f1)	{
					highTone=f0;
					lowTone=f1;
				}
				else	{
					highTone=f1;
					lowTone=f0;
				}
				centre=(highTone+lowTone)/2;
				shift=highTone-lowTone;
				// Jump the next stage to acquire symbol timing
				state=3;
				return outLines;
			}
			
		}
		
		
		// Look for 50 baud data
		if (state==2)	{
			
			}
		
		// Acquire symbol timing
		if (state==3)	{
			
			int f=do128FFT(circBuf,waveData,0);
			energyBuffer.addToCircBuffer(getHighSpectrum());
					
			sampleCount++;
			symbolCounter++;
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol50*1)) return null;
			int pos=energyBuffer.returnHighestBin();
			// Calculate what the value of the symbol counter should be
			symbolCounter=symbolCounter-pos;
			state=4;
		}
		
		// Read in symbols
		if (state==4)	{
			
			if (symbolCounter>=(long)samplesPerSymbol50)	{
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
	
	
	private boolean freqDecision (int freq)	{
		if (freq>centre) return true;
		else return false;
	}
}
