package org.e2k;

// RDFT has 8 carriers spaced 230 Hz apart

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class RDFT extends OFDM {
	
	private int state=0;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private double samplesPerSymbol;
	private int carrierBinNos[][][]=new int[8][23][2];
	private double totalCarriersEnergy;
	
	private double pastEnergyBuffer[]=new double[3];
	private int pastEnergyBufferCounter=0;
	
	public RDFT (Rivet tapp)	{
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
				JOptionPane.showMessageDialog(null,"WAV files containing\nRDFT recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			samplesPerSymbol=samplesPerSymbol(122.5,waveData.getSampleRate());
			setState(1);
			return;
		}
		// Look for the constant 8 carriers that signal a RDFT start
		else if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return;
			// Only run this check every 50 samples as this is rather maths intensive
			if (sampleCount%50==0)	{
				double spr[]=doRDFTFFTSpectrum(circBuf,waveData,0,true,650,true);
			    List<CarrierInfo> clist=findOFDMCarriers(spr,waveData.getSampleRate(),RDFT_FFT_SIZE);
			    // Look for an RDFT start sequence
			    if (RDFTCheck(clist)==true)	{
			    	// Display this carrier info
			    	StringBuilder sb=new StringBuilder();
			    	sb.append(theApp.getTimeStamp()+" RDFT lead in tones found. Carrier 1 at "+Double.toString(clist.get(0).getFrequencyHZ())+" Hz");
			    	sb.append(" & Carrier 8 at "+Double.toString(clist.get(7).getFrequencyHZ())+" Hz");
			    	theApp.writeLine(sb.toString(),Color.BLACK,theApp.boldFont);		
			    	// Populate the carrier bins
			    	populateCarrierTonesBins(clist.get(0).getBinFFT());
			    	// All done detecting
			    	setState(2);
			    }
			}
		}
		else if (state==2)	{
			sampleCount++;
			
			
			// TODO: Work out how to do symbol timing with 9-PSK
			
			symbolCounter++;
			//if (symbolCounter<samplesPerSymbol) return;
			//if (symbolCounter<=RDFT_FFT_SIZE) return;
			symbolCounter=0;
			
			// Get the complex spectrum
			double ri[]=doRDFTFFTSpectrum(circBuf,waveData,0,false,(int)samplesPerSymbol,false);
			// Extract each carrier symbol as a complex number
			List<Complex> symbolComplex=extractCarrierSymbols(ri);
			
			StringBuilder sb=new StringBuilder();
			sb.append(Long.toString(sampleCount));
		    
		    pastEnergyBuffer[pastEnergyBufferCounter]=totalCarriersEnergy;
		    pastEnergyBufferCounter++;
		    if (pastEnergyBufferCounter==pastEnergyBuffer.length) pastEnergyBufferCounter=0;
			
		    int a;
		    double av=0.0;
		    for (a=0;a<pastEnergyBuffer.length;a++)	{
		    	av=av+pastEnergyBuffer[a];
		    }
		    av=av/pastEnergyBuffer.length;
		    
			sb.append(","+Double.toString(av));
			
			theApp.debugDump(sb.toString());
				
		}
		
	}
	
	private List<Complex> extractCarrierSymbols (double fdata[])	{
		List<Complex> complexList=new ArrayList<Complex>();
		int carrierNo;
		totalCarriersEnergy=0.0;
		// Run through each carrier
		for (carrierNo=0;carrierNo<8;carrierNo++)	{
			int b;
			Complex total=new Complex();
			for (b=0;b<23;b++)	{
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
	

	// Check we have a RDFT wave form here
	private boolean RDFTCheck (List<CarrierInfo> carrierList)	{
		// Check there are 8 carriers
		if (carrierList.size()!=8) return false;
		int a,leadCarrierNos[]=new int[8];
		// Check the difference between the highest carrier bin and the lowest is within an allowable range
		int totalDifference=carrierList.get(7).getBinFFT()-carrierList.get(0).getBinFFT();
		if ((totalDifference<159)||(totalDifference>162)) return false;
		// Check the average spacing of the carriers is more than 190 Hz and less than 250 Hz
		double spacing=averageCarrierSpacing(carrierList);
		if ((spacing<190.0)||(spacing>250.0)) return false;
		// Calculate the central bins used by each carrier
		for (a=0;a<8;a++)	{
			leadCarrierNos[a]=carrierList.get(0).getBinFFT()+(a*23);
		}
		return true;
	}
		
	// Populate the carrierBinNos[][][] variable
	private void populateCarrierTonesBins (int carrierCentre)	{
		int binNos,mod,carrierNos;
		// Run though each carrier
		for (carrierNos=0;carrierNos<8;carrierNos++)	{
			mod=-11;
			for (binNos=0;binNos<23;binNos++)	{
				carrierBinNos[carrierNos][binNos][0]=returnRealBin(carrierCentre+mod);
				carrierBinNos[carrierNos][binNos][1]=returnImagBin(carrierCentre+mod);
				mod++;
			}
			carrierCentre=carrierCentre+23;
		}
		
	}

	// Do an inverse FFT to recover one particular carrier
	private double[] recoverCarrier (int carrierNo,double spectrumIn[])	{
		double spectrum[]=new double[spectrumIn.length];
		int b;
		for (b=0;b<23;b++)	{
				int rBin=carrierBinNos[carrierNo][b][0];
				int iBin=carrierBinNos[carrierNo][b][1];	
				spectrum[rBin]=spectrumIn[rBin];
				spectrum[iBin]=spectrumIn[iBin];
			}
		RDFTfft.realInverse(spectrum,false);		
		return spectrum;
	}
	
}







