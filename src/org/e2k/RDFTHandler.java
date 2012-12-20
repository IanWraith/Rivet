package org.e2k;

import java.awt.Color;
import java.util.List;

import javax.swing.JOptionPane;

public class RDFTHandler extends OFDM {
	
	private int state=0;
	private Rivet theApp;
	private long sampleCount=0;
	private long symbolCounter=0;
	private double samplesPerSymbol;
	private int leadInToneBins[]=new int[8];
	
	public RDFTHandler (Rivet tapp)	{
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
			// Only run this check every 10 samples as this is rather maths intensive
			if (sampleCount%10==0)	{
				double spr[]=doRDFTFFTAllBinsRequest(circBuf,waveData,0);
			    List<CarrierInfo> clist=findOFDMCarriers(spr,waveData.getSampleRate(),FFT_400_SIZE);
			    // Look for 8 carriers
			    if (clist.size()==8)	{
			    	// Check the carrier spacing is correct
			    	if (carrierSpacingCheck(clist,220.0,80.0)==true)	{
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
						setState(2);
			    	}
			    }
			}
		}
		else if (state==2)	{
			
			
			
			
		}
		
	}
	
	
}
