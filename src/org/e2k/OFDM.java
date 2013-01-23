package org.e2k;

import java.util.ArrayList;
import java.util.List;

public class OFDM extends FFT {
		
	// Does a RDFT_FFT_SIZE point FFT then returns the full spectrum
	// If type is true return the processed spectrum and if now the full data array
	public double[] doRDFTFFTSpectrum (CircularDataBuffer circBuf,WaveData waveData,int start,boolean type,int ss,boolean window)	{
		// Get the data from the circular buffer
		double datao[]=circBuf.extractDataDouble(start,ss);
		double datar[]=new double[RDFT_FFT_SIZE];
		int a;
	    for (a=0;a<datar.length;a++)	{
	    	if (ss==65)	{
	    		if ((a>=167)&&(a<232)) datar[a]=datao[a-167];
	    		else datar[a]=0.0;
	    	}
	    	else if (ss==650)	{
	    		if ((a>=75)&&(a<725)) datar[a]=datao[a-75];
	    		else datar[a]=0.0;
	    	}
	    	else if (ss==RDFT_FFT_SIZE)	datar[a]=datao[a];
	    	if (window==true) datar[a]=windowHamming(datar[a],a,datar.length);
	    }
		RDFTfft.realForward(datar);
		if (type==true) return getSpectrum(datar);
		else return datar;
		}
	
	// Find the peaks in the spectrum data and return them as CarrierInfo objects
	public List<CarrierInfo> findOFDMCarriers (double spectrum[],double sampleRate,int binCount)	{
		List<CarrierInfo> cList=new ArrayList<CarrierInfo>();
		int a;
		double dPoint=-1.0;
		for (a=0;a<(spectrum.length-1);a++)	{
			if (spectrum[a]>dPoint) dPoint=spectrum[a];
		}
		dPoint=dPoint*0.2;
		for (a=1;a<(spectrum.length-1);a++)	{
			// Check the current spectrum value is higher than the last one and the next one
			// if it is then this is a peak so classify this as a carrier
			if ((spectrum[a]>spectrum[a-1])&&(spectrum[a]>spectrum[a+1])&&(spectrum[a]>dPoint))	{
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
	
	// Return the average spacing between the carriers
	public double averageCarrierSpacing (List<CarrierInfo> carrierList)	{
		int a;
		double average=0.0;
		for (a=1;a<carrierList.size();a++)	{
			average=average+(carrierList.get(a).getFrequencyHZ()-carrierList.get(a-1).getFrequencyHZ());
		}
		average=average/carrierList.size();
		return average;
	}
	
	
}
