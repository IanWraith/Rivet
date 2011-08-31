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

public class WaveData {
	public boolean endian=false;
	public boolean fromFile=false;
	public double sampleRate=0.0;
	public int channels=0;
	public int sampleSizeInBits=0;
	public int bytesPerFrame=0;
	public int shortCorrectionFactor=0;
	public int longCorrectionFactor=0;
	public int midCorrectionFactor=0;
	public int CorrectionFactor200=0;
	public int CorrectionFactor256=0;
	
	// Clear the various correction factors
	public void Clear()	{
		shortCorrectionFactor=0;
		longCorrectionFactor=0;
		midCorrectionFactor=0;
		CorrectionFactor200=0;
		CorrectionFactor256=0;
	}
	
}
