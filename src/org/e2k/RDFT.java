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
			// Only run this check every 5 samples as this is rather maths intensive
			if (sampleCount%5==0)	{
				double spr[]=doRDFTFFTSpectrum(circBuf,waveData,0,true,(int)samplesPerSymbol);
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
			
			//symbolCounter++;
			//if (symbolCounter<=samplesPerSymbol) return;
			//symbolCounter=0;
			
			// Get the complex spectrum
			double ri[]=doRDFTFFTSpectrum(circBuf,waveData,0,false,(int)samplesPerSymbol);
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
			
			sb.append(","+Double.toString(totalCarriersEnergy));
			
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
			for (b=0;b<23;b++)	{
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
	

	// Check we have a RDFT wave form here
	private boolean RDFTCheck (List<CarrierInfo> carrierList)	{
		// Check there are 8 or more carriers
		if (carrierList.size()<8) return false;
		int a,leadCarrierNos[]=new int[8];
		for (a=0;a<(carrierList.size()-1);a++)	{
			leadCarrierNos[0]=a;
			// First peak 
			leadCarrierNos[1]=carrierHunt(carrierList,230.0,leadCarrierNos[0]);
			if (leadCarrierNos[1]>0)	{
				// 2nd peak
				leadCarrierNos[2]=carrierHunt(carrierList,230.0,leadCarrierNos[1]);
				if (leadCarrierNos[2]>0)	{
					// 3rd peak
					leadCarrierNos[3]=carrierHunt(carrierList,230.0,leadCarrierNos[2]);
					if (leadCarrierNos[3]>0)	{
						// 4th peak
						leadCarrierNos[4]=carrierHunt(carrierList,230.0,leadCarrierNos[3]);
						if (leadCarrierNos[4]>0)	{
							// 5th peak
							leadCarrierNos[5]=carrierHunt(carrierList,230.0,leadCarrierNos[4]);
							if (leadCarrierNos[5]>0)	{
								// 6th peak
								leadCarrierNos[6]=carrierHunt(carrierList,230.0,leadCarrierNos[5]);
								if (leadCarrierNos[6]>0)	{
									// 7th and final peak
									leadCarrierNos[7]=carrierHunt(carrierList,230.0,leadCarrierNos[6]);
									if (leadCarrierNos[7]>0)	{
										
										return true;
										
									}
								}
							}
						}
					}
					
				}
			}
		}
		
		
		return false;
	}
	
	// Return the next carrier that has a frequency stepFreqSize greater than currentCarrierNos
	private int carrierHunt (List<CarrierInfo> carrierList,double stepFreqSize,int currentCarrierNos)	{
		int a;
		double baseFreq=carrierList.get(currentCarrierNos).getFrequencyHZ();
		for (a=(currentCarrierNos+1);a<carrierList.size();a++)	{
			double tFreq=carrierList.get(a).getFrequencyHZ()-baseFreq;
			if (tFreq==230.0) return a;
		}
		// Return -1 to show nothing found
		return -1;
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

	
	
}
