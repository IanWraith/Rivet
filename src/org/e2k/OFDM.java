package org.e2k;

import java.util.ArrayList;
import java.util.List;

public class OFDM extends FFT {
	
	// Does a 400 point FFT then returns all bins
	public double[] doRDFTFFTAllBinsRequest (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_400_SIZE);
		fft400.realForward(datar);
		return getSpectrum(datar);
		}
	
	// Find the highest values in the spectrum data and return them as CarrierInfo objects
	public List<CarrierInfo> findOFDMCarriers (double spectrum[],double sampleRate,int binCount)	{
		List<CarrierInfo> cList=new ArrayList<CarrierInfo>();
		int a;
		// Find the highest value
		double highVal=-1.0;
		for (a=0;a<spectrum.length;a++)	{
			if (spectrum[a]>highVal) highVal=spectrum[a];
		}
		// Run though again looking for the highs
		for (a=0;a<spectrum.length;a++)	{
			double per=(spectrum[a]/highVal)*100.0;
			if (per>30.0)	{
				CarrierInfo cInfo=new CarrierInfo();
				cInfo.setBinFFT(a);
				cInfo.setEnergy(spectrum[a]);
				// Calculate the actual frequency of the carrier
				double freq=(double)a*(sampleRate/(double)binCount);
				cInfo.setFrequencyHZ(freq);
				// Add this carrier object to the list
				cList.add(cInfo);
			}
		}
		return cList;
	}
	
	// Check an OFDM signal described as a list of CarrierInfo objects has the correct spacing
	public boolean carrierSpacingCheck (List<CarrierInfo> carrierList,double requiredSpacing,double errorAllowance)	{
		int a;
		// Calculate the carrier spacing
		for (a=carrierList.size()-1;a>0;a--)	{
			double actualSpacing=carrierList.get(a).getFrequencyHZ()-carrierList.get(a-1).getFrequencyHZ();
			if (Math.abs(actualSpacing-requiredSpacing)>errorAllowance) return false;
		}
		return true;
	}

	
	
}
