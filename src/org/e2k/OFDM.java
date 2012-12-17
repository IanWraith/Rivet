package org.e2k;

public class OFDM extends FFT {
	
	// Does a 400 point FFT then returns all bins
	public double[] doRDFTFFTAllBinsRequest (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		// Get the data from the circular buffer
		double datar[]=circBuf.extractDataDouble(start,FFT_400_SIZE);
		fft400.realForward(datar);
		return getSpectrum(datar);
		}
	
	
}
