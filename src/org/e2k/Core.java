package org.e2k;

public class Core {

	// Return the number of samples per baud
	public double samplesPerSymbol (double dbaud,double sampleFreq)	{
			return (sampleFreq/dbaud);
		}
	
	
	// Return an ASCII character
	public String getAsciiChar (int c)	{
		String ch;
		if ((c>=32)&&(c<=126))	{
			char cr=(char)c;
			return Character.toString(cr);
		}
		else	{
			ch="["+Integer.toString(c)+"]";
			return ch;
		}
	}
	
	
}
