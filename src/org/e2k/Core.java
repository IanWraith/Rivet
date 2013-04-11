// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

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
