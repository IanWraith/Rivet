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
	private int carrierBinNos[][][]=new int[8][3][2];
	private double totalCarriersEnergy;
	private double energyBuffer[]=new double[65];
	private int energyBufferCounter=0;
	
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
			    	
			    	//////////////////////////////////////////////////////////////////////////////////////////////
			    	StringBuilder sba=new StringBuilder();
		    		sba.append("Tones difs (");
		    		int aa;
		    		for (aa=1;aa<clist.size();aa++)	{
		    			
		    			double dif=clist.get(aa).getFrequencyHZ()-clist.get(aa-1).getFrequencyHZ();
		    			
		    			if (aa>1) sba.append(",");
		    			sba.append(Double.toString(dif)+" Hz");
		    		}
		    		sba.append(") at "+Long.toString(sampleCount));
		    		
		    		theApp.writeLine(sba.toString(),Color.BLACK,theApp.boldFont );	
		    		//////////////////////////////////////////////////////////////////////////////////////////////
			    	
			    	// Check the carrier spacing is correct
			    	if (carrierSpacingCheck(clist,23)==true)	{
			    		int leadInToneBins[]=new int[8];
			    		// Display this carrier info
			    		StringBuilder sb=new StringBuilder();
			    		sb.append(theApp.getTimeStamp()+" RDFT lead in tones found in FFT bins (");
			    		int a;
			    		for (a=0;a<clist.size();a++)	{
			    			if (a>0) sb.append(",");
			    			sb.append(Integer.toString(clist.get(a).getBinFFT()));
			    			
			    			sb.append(" "+clist.get(a).getFrequencyHZ()+" Hz");
			    			
			    			// Also store the 8 lead in tones bins
			    			leadInToneBins[a]=clist.get(a).getBinFFT();
			    		}
			    		sb.append(")");
			    		theApp.writeLine(sb.toString(),Color.BLACK,theApp.boldFont );	
			    		// Populate the carrier bins
			    		// A 104 point FFT gives us a 76.9 Hz resolution so 3 bins per carrier
			    		for (a=0;a<8;a++)	{
			    			// Run through each bin
			    			// -1
			    			carrierBinNos[a][0][0]=returnRealBin(leadInToneBins[a]-1);
			    			carrierBinNos[a][0][1]=returnImagBin(leadInToneBins[a]-1);
			    			// Centre 0
			    			carrierBinNos[a][1][0]=returnRealBin(leadInToneBins[a]);
			    			carrierBinNos[a][1][1]=returnImagBin(leadInToneBins[a]);
			    			// +1
			    			carrierBinNos[a][2][0]=returnRealBin(leadInToneBins[a]+1);
			    			carrierBinNos[a][2][1]=returnImagBin(leadInToneBins[a]+1);
			    		}
			    		// All done detecting
			    		setState(2);
			    		
			    		symbolCounter=40;
			    		
			    	}
			    }
			}
		}
		else if (state==2)	{
			sampleCount++;
			
			
			// TODO: Work out how to do symbol timing with 9-PSK
			
			symbolCounter++;
			//if (symbolCounter<=samplesPerSymbol) return;
			//symbolCounter=0;
			
			// Get the complex spectrum
			double ri[]=doRDFTFFTSpectrum(circBuf,waveData,0,false);
			// Extract each carrier symbol as a complex number
			List<Complex> symbolComplex=extractCarrierSymbols(ri);
			
			
			
			//energyBuffer[energyBufferCounter]=totalCarriersEnergy;
			//energyBufferCounter++;
			
			
			StringBuilder sb=new StringBuilder();
			sb.append(Long.toString(sampleCount));
			
			int a;
			for (a=0;a<symbolComplex.size();a++)	{
				sb.append(","+Double.toString(symbolComplex.get(a).getReal())+","+Double.toString(symbolComplex.get(a).getImag()));
			}
			
			//sb.append(","+Double.toString(totalCarriersEnergy));
			
			//int s;
			//for (s=0;s<symbolComplex.size();s++)	{
				//sb.append(","+Double.toString(symbolComplex.get(s).getReal())+","+Double.toString(symbolComplex.get(s).getImag()));
			//}
			//theApp.debugDump(sb.toString());
			
			
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
			for (b=0;b<3;b++)	{
				int rBin=carrierBinNos[carrierNo][b][0];
				int iBin=carrierBinNos[carrierNo][b][1];
				Complex tbin=new Complex(fdata[rBin],fdata[iBin]);
				total=total.add(tbin);
				totalCarriersEnergy=totalCarriersEnergy+tbin.getMagnitude();
			}
			// Add this to the list
			complexList.add(total);
		}
		return complexList;
	}
	

	
}
