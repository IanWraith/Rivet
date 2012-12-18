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
	public List<CarrierInfo> findOFDMCarriers (double spectrum[])	{
		List<CarrierInfo> cList=new ArrayList<CarrierInfo>();
		int a;
		// Find the highest value
		double highVal=-1.0;
		for (a=0;a<spectrum.length;a++)	{
			if (spectrum[a]>highVal) highVal=spectrum[a];
		}
		// Run though again looking for the highs
		for (a=0;a<spectrum.length;a++)	{
			double per=spectrum[a]/highVal;
			if (per>0.3)	{
				CarrierInfo cInfo=new CarrierInfo();
				cInfo.setBinFFT(a);
				cInfo.setEnergy(spectrum[a]);
				cList.add(cInfo);
				
				// TODO : We need to calculate the frequency of this carrier
				
			}
		}
		return cList;
	}
	
	
}
