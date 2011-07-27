package org.e2k;

public class CircularDataBuffer {
	
	public final int MAXCIRC=1024*10;
	public int circBufferCounter=0;
	public int[] circDataBuffer=new int[MAXCIRC];
	
	// Add data to the incoming data circular buffer
	public void addToCircBuffer (int i)	{
		circDataBuffer[circBufferCounter]=i;
		circBufferCounter++;
		if (circBufferCounter==MAXCIRC) circBufferCounter=0;
	}

}
