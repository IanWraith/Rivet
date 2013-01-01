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
	
	// Find the highest values in the spectrum data and return them as CarrierInfo objects
	public List<CarrierInfo> findOFDMCarriers (double spectrum[],double sampleRate,int binCount)	{
		List<CarrierInfo> cList=new ArrayList<CarrierInfo>();
		int a,lastBin=-1;
		// Find the highest value
		double highVal=-1.0;
		for (a=0;a<spectrum.length;a++)	{
			if (spectrum[a]>highVal) highVal=spectrum[a];
		}
		// Run though again looking for the highs
		for (a=0;a<spectrum.length;a++)	{
			double per=(spectrum[a]/highVal)*100.0;
			// Check this value is within 30% of the high bin and not next to the last high value bin
			if ((per>30.0)&&(a>lastBin+1))	{
				// Store this bin
				lastBin=a;
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
			// If the frequency difference is greater than error allowance return false
			if (Math.abs(actualSpacing-requiredSpacing)>errorAllowance) return false;
		}
		// Everything must be withing tolerance so return true
		return true;
	}

	
	
}
