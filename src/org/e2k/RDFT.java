package org.e2k;

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
	private int carrierBinNos[][][]=new int[8][11][2];
	
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
			// Only run this check every 20 samples as this is rather maths intensive
			if (sampleCount%20==0)	{
				double spr[]=doRDFTFFTSpectrum(circBuf,waveData,0,true);
			    List<CarrierInfo> clist=findOFDMCarriers(spr,waveData.getSampleRate(),RDFT_FFT_SIZE);
			    // Look for 8 carriers
			    if (clist.size()==8)	{
			    	// Check the carrier spacing is correct
			    	if (carrierSpacingCheck(clist,220.0,60.0)==true)	{
			    		int leadInToneBins[]=new int[8];
			    		// Display this carrier info
			    		StringBuilder sb=new StringBuilder();
			    		sb.append(theApp.getTimeStamp()+" RDFT lead in tones found (");
			    		int a;
			    		for (a=0;a<clist.size();a++)	{
			    			if (a>0) sb.append(",");
			    			sb.append(Double.toString(clist.get(a).getFrequencyHZ())+" Hz");
			    			// Also store the 8 lead in tones bins
			    			leadInToneBins[a]=clist.get(a).getBinFFT();
			    		}
			    		sb.append(")");
			    		theApp.writeLine(sb.toString(),Color.BLACK,theApp.boldFont );	
			    		// Populate the carrier bins
			    		// A 400 point FFT gives us a 20 Hz resolution so 11 bins per carrier
			    		for (a=0;a<8;a++)	{
			    			// Run through each bin
			    			// -5
			    			carrierBinNos[a][0][0]=returnRealBin(leadInToneBins[a]-5);
			    			carrierBinNos[a][0][1]=returnImagBin(leadInToneBins[a]-5);
			    			// -4
			    			carrierBinNos[a][1][0]=returnRealBin(leadInToneBins[a]-4);
			    			carrierBinNos[a][1][1]=returnImagBin(leadInToneBins[a]-4);
			    			// -3
			    			carrierBinNos[a][2][0]=returnRealBin(leadInToneBins[a]-3);
			    			carrierBinNos[a][2][1]=returnImagBin(leadInToneBins[a]-3);
			    			// -2
			    			carrierBinNos[a][3][0]=returnRealBin(leadInToneBins[a]-2);
			    			carrierBinNos[a][3][1]=returnImagBin(leadInToneBins[a]-2);
			    			// -1
			    			carrierBinNos[a][4][0]=returnRealBin(leadInToneBins[a]-1);
			    			carrierBinNos[a][4][1]=returnImagBin(leadInToneBins[a]-1);
			    			// 0
			    			carrierBinNos[a][5][0]=returnRealBin(leadInToneBins[a]);
			    			carrierBinNos[a][5][1]=returnImagBin(leadInToneBins[a]);
			    			// +1
			    			carrierBinNos[a][6][0]=returnRealBin(leadInToneBins[a]+1);
			    			carrierBinNos[a][6][1]=returnImagBin(leadInToneBins[a]+1);
			    			// +2
			    			carrierBinNos[a][7][0]=returnRealBin(leadInToneBins[a]+2);
			    			carrierBinNos[a][7][1]=returnImagBin(leadInToneBins[a]+2);
			    			// +3
			    			carrierBinNos[a][8][0]=returnRealBin(leadInToneBins[a]+3);
			    			carrierBinNos[a][8][1]=returnImagBin(leadInToneBins[a]+3);
			    			// +4
			    			carrierBinNos[a][9][0]=returnRealBin(leadInToneBins[a]+4);
			    			carrierBinNos[a][9][1]=returnImagBin(leadInToneBins[a]+4);
			    			// +5
			    			carrierBinNos[a][10][0]=returnRealBin(leadInToneBins[a]+5);
			    			carrierBinNos[a][10][1]=returnImagBin(leadInToneBins[a]+5);
			    		}
			    		
			    		symbolCounter=50;
			    		
			    		// All done detecting
			    		setState(2);
			    	}
			    }
			}
		}
		else if (state==2)	{
			sampleCount++;
			
			
			// TODO: Work out how to do symbol timing with 9-PSK
			
			//symbolCounter++;
			//if (symbolCounter<=samplesPerSymbol) return;
			
			//symbolCounter=0;
			
			// Get the complex spectrum
			double ri[]=doRDFTFFTSpectrum(circBuf,waveData,0,false);
			// Extract each carrier symbol as a complex number
			List<Complex> symbolComplex=extractCarrierSymbols(ri);
			
			StringBuilder sb=new StringBuilder();
			sb.append(Long.toString(sampleCount));
			int c;
				
			for (c=0;c<8;c++)	{
				Complex carrier=symbolComplex.get(c);
				sb.append(","+Double.toString(carrier.getReal())+","+Double.toString(carrier.getImag())+","+Double.toString(carrier.returnFull())+",X");
				
				//sb.append(","+Double.toString(carrier.returnFull()));
				
			}
			theApp.debugDump(sb.toString());
			
			
			
		}
		
	}
	
	private List<Complex> extractCarrierSymbols (double fdata[])	{
		List<Complex> complexList=new ArrayList<Complex>();
		int carrierNo;
		// Run through each carrier
		for (carrierNo=0;carrierNo<8;carrierNo++)	{
			// Get the central carrier bin only
			int rBin=carrierBinNos[carrierNo][5][0];
			int iBin=carrierBinNos[carrierNo][5][1];
			Complex tbin=new Complex(fdata[rBin],fdata[iBin]);
			// Add this to the list
			complexList.add(tbin);
		}
		return complexList;
	}
	
	
	
}
