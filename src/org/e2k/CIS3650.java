package org.e2k;

import javax.swing.JOptionPane;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class CIS3650 extends FSK {

	public final int FFT_64_SIZE=128;
	private DoubleFFT_1D fft64=new DoubleFFT_1D(FFT_64_SIZE);
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
	private long syncFoundPoint;
	private int syncState;
	private String line="";
	private int ccount=0;
	private int syncBuffer=0;
	
	public CIS3650 (Rivet tapp)	{
		theApp=tapp;
	}
	
	// The main decode routine
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()>11025.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCIS 36-50 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
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
			syncState=0;
			syncBuffer=0;
			return null;
		}
		
		
		// Look for a 36 baud or a 50 baud alternating sequence
		if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return null;
			int pos=0;
			int f0=getSymbolFreq(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol36*1;
			int f1=getSymbolFreq(circBuf,waveData,pos);
			if (f0==f1) return null;
			pos=(int)samplesPerSymbol36*2;
			int f2=getSymbolFreq(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol36*3;
			int f3=getSymbolFreq(circBuf,waveData,pos);
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
			f1=do64FFT(circBuf,waveData,pos);
			if (f0==f1) return null;
			pos=(int)samplesPerSymbol50*2;
			f2=do64FFT(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol50*3;
			f3=do64FFT(circBuf,waveData,pos);
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
				syncState=1;
				syncFoundPoint=sampleCount;
				return outLines;
			}
			
		}
		
		
		// After a 36 baud sync sequence look for 50 baud opening 9 bits
		if (state==2)	{
			// Hunt for 011101011
			int pos=0,rx=0,a;
			final int val[]={256,128,64,32,16,8,4,2,1};
			for (a=0;a<9;a++)	{
				pos=(int)samplesPerSymbol50*a;
				if (getSymbolBit(circBuf,waveData,pos)==true) rx=rx+val[a];
				}
			// If we find 235 then all is correct and invert should stay as it is
			if (rx==235)	{
				syncFoundPoint=sampleCount;
				syncState=1;
				state=3;
				}
			// If 276 we need to change the invert flag
			else if (rx==276)	{
				boolean inv=theApp.isInvertSignal();
				if (inv==true) inv=false;
				else inv=true;
				theApp.setInvertSignal(inv);
				syncFoundPoint=sampleCount;
				syncState=1;
				state=3;
				}
			}
		
		// Acquire symbol timing
		if (state==3)	{
			do64FFT(circBuf,waveData,0);
			energyBuffer.addToCircBuffer(getHighSpectrum());
			sampleCount++;
			symbolCounter++;
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol50*1)) return null;
			long perfectPoint=energyBuffer.returnHighestBin()+syncFoundPoint+(int)samplesPerSymbol50;
			// Calculate what the value of the symbol counter should be
			symbolCounter=(int)samplesPerSymbol50-(perfectPoint-sampleCount);
			state=4;
		}
		
		// Read in symbols
		if (state==4)	{
			
			if (symbolCounter>=(long)samplesPerSymbol50)	{
				symbolCounter=0;		
				boolean bit=getSymbolBit(circBuf,waveData,0);
				if (syncState==1)	{
					addToSyncBuffer(bit);
					// Look for the CIS 36-50 sync sequence
					if (syncBuffer==0xEBEB4141)	{
						syncState=2;
						outLines[0]=theApp.getTimeStamp()+" CIS 36-50 sync words found";
					}	
				}
				// Once we have the sync sequence just show binary
				else if (syncState==2)	{
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
	
	// A 64 point FFT is fine for both 8000 KHz and 11025 KHz 
	public int do64FFT (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,FFT_64_SIZE);
		fft64.realForward(datar);
		double spec[]=getSpectrum(datar);
		int freq=getFFTFreq (spec,waveData.getSampleRate());  
		return freq;
	}
	
	// Get the frequency at a certain symbol
	private int getSymbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int fr=do64FFT(circBuf,waveData,start);
		return fr;
	}
	
	// Return the bit value for a certain symbol
	private boolean getSymbolBit (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int f=getSymbolFreq(circBuf,waveData,start);
		boolean bit=freqDecision(f);
		return bit;
	}
	
	// Given a frequency decide the bit value
	private boolean freqDecision (int freq)	{
		if (theApp.isInvertSignal()==false)	{
			if (freq>centre) return true;
			else return false;
		}
		else	{
			if (freq>centre) return false;
			else return true;
		}
	}
	
	// Add the bit to a 32 bit long sync buffer
	private void addToSyncBuffer(boolean bit)	{
		syncBuffer=syncBuffer<<1;
		syncBuffer=syncBuffer&0xFFFFFFFF;
		if (bit==true) syncBuffer++;
	}
	
}
