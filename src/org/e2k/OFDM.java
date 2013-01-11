package org.e2k;

import java.util.ArrayList;
import java.util.List;

public class OFDM extends FFT {
		
	// Does a RDFT_FFT_SIZE point FFT then returns the full spectrum
	// If type is true return the processed spectrum and if now the full data array
	public double[] doRDFTFFTSpectrum (CircularDataBuffer circBuf,WaveData waveData,int start,boolean type)	{
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,RDFT_FFT_SIZE);
		RDFTfft.realForward(datar);
		if (type==true) return getSpectrum(datar);
		else return datar;
		}
	
	// Find the peaks in the spectrum data and return them as CarrierInfo objects
	public List<CarrierInfo> findOFDMCarriers (double spectrum[],double sampleRate,int binCount)	{
		List<CarrierInfo> cList=new ArrayList<CarrierInfo>();
		int a,highBin=0;
		boolean inPeak=false;
		double lastVal=0.0,midValue=-1.0,highVal=-1.0;
		// Find the highest value
		for (a=0;a<spectrum.length;a++)	{
			if (spectrum[a]>midValue) midValue=spectrum[a];
		}
		// Set the peak detect mid value to 25% of the maximum value found
		midValue=midValue*0.25;
		// Run though again looking for the highs
		for (a=0;a<spectrum.length;a++)	{
			// Are moving into a peak ?
			if ((lastVal<midValue)&&(spectrum[a]>midValue)&&(inPeak==false))	{
				highBin=a;
				highVal=spectrum[a];
				inPeak=true;
			}
			// Are we leaving a peak ?
			// If so take the highest value as being the peak
			if ((lastVal>midValue)&&(midValue>spectrum[a])&&(inPeak==true))	{
				inPeak=false;
				CarrierInfo cInfo=new CarrierInfo();
				cInfo.setBinFFT(highBin);
				cInfo.setEnergy(highVal);
				// Calculate the actual frequency of the carrier
				double freq=(double)highBin*(sampleRate/(double)binCount);
				cInfo.setFrequencyHZ(freq);
				// Add this carrier object to the list
				cList.add(cInfo);
			}
			// If we are in a peak record the highest bin as we go along
			if (inPeak==true)	{
				if (spectrum[a]>highVal)	{
					highVal=spectrum[a];
					highBin=a;
				}
			}
			lastVal=spectrum[a];
		}
		return cList;
	}
		
	// Check an OFDM signal described as a list of CarrierInfo objects has the correct spacing
	public boolean carrierSpacingCheck (List<CarrierInfo> carrierList,int correctBinSpacing)	{
		int a,lastBin;
		for (a=1;a<carrierList.size();a++)	{
			lastBin=carrierList.get(a).getBinFFT()-carrierList.get(a-1).getBinFFT();
			if (lastBin!=correctBinSpacing) return false;
		}
		return true;
	}

	// Given a spectral bin number return the real bin number
	public int returnRealBin (int binno)	{
		int rb=(binno+1)*2;
		return rb;
	}
	
	// Given a spectral bin number return the imaginary bin number
	public int returnImagBin (int binno)	{
		int rb=(binno+1)*2;
		return (rb+1);
	}	
	
}
