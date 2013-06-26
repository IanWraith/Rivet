package org.e2k;

public class PSK {
	
	public double[] sineGenerate (double frequency,int sampleRate,double amplitude,int size)	{
		double buffer[]=new double[size];
		int n;
		for (n=0;n<buffer.length;n++)	{
		    buffer[n]=(amplitude*Math.sin((2*Math.PI*n*frequency)/(double)sampleRate));
		}
		return buffer;
	}

}
