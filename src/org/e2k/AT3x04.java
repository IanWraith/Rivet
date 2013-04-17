package org.e2k;

// AT-3004D & AT-3014
// has 12 * 120Bd BPSK or QPSK modulated carriers
// these carriers are 200 Hz apart with a pilot tone 400 Hz higher than the last carrier

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
	private int carrierBinNos[][][]=new int[12][23][2];
	private double totalCarriersEnergy;
	
	private double pastEnergyBuffer[]=new double[3];
	private int pastEnergyBufferCounter=0;
	
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
			// Only run this check every 50 samples as this is rather maths intensive
			if (sampleCount%50==0)	{
				double spr[]=doRDFTFFTSpectrum(circBuf,waveData,0,true,650,true);
			    List<CarrierInfo> clist=findOFDMCarriers(spr,waveData.getSampleRate(),RDFT_FFT_SIZE,0.4);
			    // Look for an AT3x04 start sequence
			    if (AT3x04Check(clist)==true)	{
			    	// Display this carrier info
			    	StringBuilder sb=new StringBuilder();
			    	sb.append(theApp.getTimeStamp()+" AT3x04 tones found. Carrier 1 at "+Double.toString(clist.get(0).getFrequencyHZ())+" Hz");
			    	sb.append(" & Carrier 12 at "+Double.toString(clist.get(11).getFrequencyHZ())+" Hz");
			    	theApp.writeLine(sb.toString(),Color.BLACK,theApp.boldFont);	
			    	
			    	// Populate the carrier bins
			    	//populateCarrierTonesBins(clist.get(0).getBinFFT());
			    	
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
			//List<Complex> symbolComplex=extractCarrierSymbols(ri);
			
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
			
			//theApp.debugDump(sb.toString());
				
		}
		
	}	

	// Check we have a AT3x04 wave form here
	private boolean AT3x04Check (List<CarrierInfo> carrierList)	{
		
		// TODO : Detect a AT3x04 waveform reliably
		
		// Check there are 12 carriers
		if (carrierList.size()!=12) return false;
		int a,leadCarrierNos[]=new int[12];
		// Check the difference between the highest carrier bin and the lowest is within an allowable range
		double totalDifference=carrierList.get(11).getFrequencyHZ()-carrierList.get(0).getFrequencyHZ();
		if ((totalDifference<2150)||(totalDifference>2250)) return false;
		// Check the average spacing of the carriers is more than 190 Hz and less than 250 Hz
		double spacing=averageCarrierSpacing(carrierList);
		if ((spacing<190.0)||(spacing>250.0)) return false;
		// Calculate the central bins used by each carrier
		for (a=0;a<12;a++)	{
			leadCarrierNos[a]=carrierList.get(0).getBinFFT()+(a*23);
		}
		return true;
	}	
	

}
