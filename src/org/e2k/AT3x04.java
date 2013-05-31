package org.e2k;

// AT-3004D & AT-3014
// has 12 * 120Bd BPSK or QPSK modulated carriers *
// these carriers are 200 Hz apart with a pilot tone 400 Hz higher than the last carrier
//
// * See http://signals.radioscanner.ru/base/signal37/ for further information

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class AT3x04 extends OFDM {
	
	private int state=0;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private double samplesPerSymbol;
	private int carrierBinNos[][][]=new int[12][20][2];
	private double totalCarriersEnergy;
	private long earlySamplePoint;
	
	private double earlyPhase0;
	
	private double pastEnergyBuffer[]=new double[3];
	private int pastEnergyBufferCounter=0;
	
	List<CarrierInfo> startCarrierList1=new ArrayList<CarrierInfo>();
	List<CarrierInfo> startCarrierList2=new ArrayList<CarrierInfo>();
	List<CarrierInfo> startCarrierList3=new ArrayList<CarrierInfo>();
	private int startCarrierCounter=0;
	private int pilotToneBin=0;
	
	public AT3x04 (Rivet tapp)	{
		theApp=tapp;
	}

	public int getState() {
		return state;
	}
	
	// Set the state and change the contents of the status label
	public void setState(int state) {
		this.state=state;
		if (state==0) theApp.setStatusLabel("Setup");
		else if (state==1) theApp.setStatusLabel("Signal Hunt");
		else if (state==2) theApp.setStatusLabel("Msg Hunt");
	}
	
	
	// The main decode routine
	public void decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Initial startup
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nAT3x04 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol=samplesPerSymbol(120.0,waveData.getSampleRate());
			earlySamplePoint=(long)samplesPerSymbol/2;
			
			startCarrierCounter=0;
			// Add a user warning that AT3x04 doesn't yet decode
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			theApp.writeLine("Please note that this mode is experimental and doesn't work yet !",Color.RED,theApp.italicFont);
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			setState(1);
			return;
		}
		// Look for the 12 carriers from this mode
		else if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return;
			// Only run this check every 100 samples as this is rather maths intensive
			if (sampleCount%100==0)	{
				double spr[]=doRDFTFFTSpectrum(circBuf,waveData,0,true,800,true);
				// Collect three lots of carrier info lists searching between 2700 Hz and 3500 Hz
			    if (startCarrierCounter==0)	{
			    	startCarrierList1=findOFDMCarriersWithinRange(spr,waveData.getSampleRate(),RDFT_FFT_SIZE,0.8,270,350);
			    	startCarrierCounter++;
			    }
			    else if (startCarrierCounter==1)	{
			    	startCarrierList2=findOFDMCarriersWithinRange(spr,waveData.getSampleRate(),RDFT_FFT_SIZE,0.8,270,350);
			    	startCarrierCounter++;
			    }
			    else if (startCarrierCounter==2)	{
			    	startCarrierList3=findOFDMCarriersWithinRange(spr,waveData.getSampleRate(),RDFT_FFT_SIZE,0.8,270,350);
			    	startCarrierCounter++;
			    }    
			    else if (startCarrierCounter==3)	{
			    	// Look for the AT3x04 pilot tone
			    	if (AT3x04PilotToneHunt(spr)==true)	{
			    		// Get a full list of carriers present
			    		List<CarrierInfo> clist=findOFDMCarriers(spr,waveData.getSampleRate(),RDFT_FFT_SIZE,0.8);
			    		// Check this list of carriers looks like a AT3x04 waveform
			    		if (AT3x04CarrierConfirm(clist)==true)	{
			    			setState(2);
			    			// Calculate the carrier tone bins
			    			populateCarrierTonesBins();
			    			// Tell the user
			    			StringBuilder sb=new StringBuilder();
			    			double toneFreq=pilotToneBin*10;
					    	sb.append(theApp.getTimeStamp()+" AT3x04 Pilot Tone found at "+Double.toString(toneFreq)+" Hz");
					    	toneFreq=toneFreq-400;
					    	sb.append(" , Carrier 12 at "+Double.toString(toneFreq)+" Hz");
					    	toneFreq=toneFreq-2200;
					    	sb.append(" + Carrier 1 at "+Double.toString(toneFreq)+" Hz");
					    	theApp.writeLine(sb.toString(),Color.BLACK,theApp.boldFont);	
			    			
			    		}
			    	}
			    	else	{
			    		startCarrierCounter=0;
			    	}	
			    }
			}
		}
		else if (state==2)	{
			sampleCount++;
			symbolCounter++;
			
			if (symbolCounter==earlySamplePoint)	{
				double ri[]=doRDFTFFTSpectrum(circBuf,waveData,0,false,(int)samplesPerSymbol,false);
				List<Complex> symbolComplex=extractCarrierSymbols(ri);
				earlyPhase0=symbolComplex.get(0).getPhase();
			}
			
			
			if (symbolCounter<samplesPerSymbol) return;
			symbolCounter=0;
			
			// Get the complex spectrum
			double ri[]=doRDFTFFTSpectrum(circBuf,waveData,0,false,(int)samplesPerSymbol,false);
			// Extract each carrier symbol as a complex number
			List<Complex> symbolComplex=extractCarrierSymbols(ri);
			double latePhase0=symbolComplex.get(0).getPhase();
			
			double phaseDif=earlyPhase0-latePhase0;
			
			StringBuilder sb=new StringBuilder();
			
			sb.append(Double.toString(phaseDif)+",");
			
			sb.append(Long.toString(sampleCount));
		    
			theApp.debugDump(sb.toString());
				
		}
		
	}	

	// Check we have a AT3x04 pilot tone here
	private boolean AT3x04PilotToneHunt (double spectrum[])	{
		int a,pbin;
		// Look if a particular bin in startCarrierList1 also exists in startCarrierList2 and startCarrierList3
		for (a=startCarrierList1.size()-1;a>=0;a--)	{
			pbin=startCarrierList1.get(a).getBinFFT();
			if (checkBinExists(startCarrierList2,pbin)==true)	{
				if (checkBinExists(startCarrierList3,pbin)==true)	{
					// Store this bin and return true
					pilotToneBin=pbin;
					return true;	
				}
			}
		}
		return false;
	}	
	
	
	// A method for checking in a bin exists in a carrier info list
	private boolean checkBinExists (List<CarrierInfo> cil,int bin)	{
		int a;
		for (a=0;a<cil.size();a++)	{
			if (cil.get(a).getBinFFT()==bin) return true;
		}
		return false;
	}
	
	// Look if we have at least half of the AT3x04 carriers in their expected places 
	// from what we believe is the pilot tone bin
	private boolean AT3x04CarrierConfirm (List<CarrierInfo> clist)	{
		int expectedCarrierBins[]=new int[12];
		int a,b,p=pilotToneBin-40;
		int findCounter=0;
		for (a=11;a>=0;a--){
			expectedCarrierBins[a]=p;
			p=p-20;
		}
		// Check if there are carriers where we think there should be
		for (a=0;a<clist.size();a++)	{
			for (b=0;b<12;b++)	{
				double dif=Math.abs(clist.get(a).getBinFFT()-expectedCarrierBins[b]);
				if (dif<2) findCounter++;
			}
		}
		
		if (findCounter>=6) return true;
		else return false;
	}
	
	// Populate the carrierBinNos[][][] variable
	private void populateCarrierTonesBins ()	{
		int binNos,carrierNos,lastCarrierBin=pilotToneBin-40;
		// Run though each carrier
		for (carrierNos=11;carrierNos>=0;carrierNos--)	{
			int mod=-10;
			for (binNos=0;binNos<20;binNos++)	{
				int rb=lastCarrierBin+mod;
				carrierBinNos[carrierNos][binNos][0]=returnRealBin(rb);
				carrierBinNos[carrierNos][binNos][1]=returnImagBin(rb);
				mod++;
			}
			lastCarrierBin=lastCarrierBin-20;
		}
	}
	
	// Do an inverse FFT to recover one particular carrier
	private double[] recoverCarrier (int carrierNo,double spectrumIn[])	{
		double spectrum[]=new double[spectrumIn.length];
		int b;
		for (b=0;b<20;b++)	{
				int rBin=carrierBinNos[carrierNo][b][0];
				int iBin=carrierBinNos[carrierNo][b][1];	
				spectrum[rBin]=spectrumIn[rBin];
				spectrum[iBin]=spectrumIn[iBin];
			}
		RDFTfft.realInverse(spectrum,false);		
		return spectrum;
	}	
	
	private List<Complex> extractCarrierSymbols (double fdata[])	{
		List<Complex> complexList=new ArrayList<Complex>();
		int carrierNo;
		totalCarriersEnergy=0.0;
		// Run through each carrier
		for (carrierNo=0;carrierNo<12;carrierNo++)	{
			int b;
			Complex total=new Complex();
			for (b=0;b<20;b++)	{
				int rBin=carrierBinNos[carrierNo][b][0];
				int iBin=carrierBinNos[carrierNo][b][1];
				Complex tbin=new Complex(fdata[rBin],fdata[iBin]);
				total=total.add(tbin);
			}
			// Add this to the list
			complexList.add(total);
			// Calculate the total energy
			totalCarriersEnergy=totalCarriersEnergy+total.getMagnitude();
		}
		return complexList;
	}	

}
