package org.e2k;

public class NCO {
	
	private int n;
	private double sampleRate;
	private double frequency;
	private double amplitude;
	private double prevSample=1;
	private double NinetyDegOut=0;
	private int peak=0;
	
	public NCO (double freq,double amp,double sr)	{
		n=0;
		sampleRate=sr;
		frequency=freq;
		amplitude=amp;
	}
	
	public void setFrequency (double freq)	{
		frequency=freq;
		peak=0;
	}
	
	// Get the next sample of the waveform
	public double getSample()	{
		// Calculate this sample
		double out=(amplitude*Math.sin((2*Math.PI*n*frequency)/(double)sampleRate));
		// & one 90 degrees out of phase
		NinetyDegOut=(amplitude*Math.cos((2*Math.PI*n*frequency)/(double)sampleRate));
		// then the one after
		double nout=(amplitude*Math.sin((2*Math.PI*(n+1)*frequency)/(double)sampleRate));
		// Is this the peak
		if ((out>prevSample)&&(out>nout))	{
			// Record the first peak and if this is a peak set the counter n to the value of the first peak detected
			// this stops the counter n from getting to large and rolling over
			if (peak==0) peak=n;
			else n=peak;
		}
		n++;
		prevSample=out;
		return out;
	}
	
	public double getNinetyDegOut()	{
		return NinetyDegOut;
	}
	
	
	
}
