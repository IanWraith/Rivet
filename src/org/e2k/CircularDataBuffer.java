package org.e2k;

public class CircularDataBuffer {
	
	private final int MAXCIRC=1024*10;
	private int circBufferCounter=0;
	private int[] circDataBuffer=new int[MAXCIRC];
	private boolean filled=false;
	
	// Add data to the incoming data circular buffer
	public void addToCircBuffer (int i)	{
		circDataBuffer[circBufferCounter]=i;
		circBufferCounter++;
		if (circBufferCounter==MAXCIRC)	{
			circBufferCounter=0;
			filled=true;
		}
	}
	
	// Return a user defined section of the circular buffer
	public int[] extractData (int start,int length)	{
		int count=0,a=circBufferCounter+start;
		int outData[]=new int[length];
		if (a>MAXCIRC) a=a-MAXCIRC;
		for (count=0;count<length;count++)	{
			outData[count]=circDataBuffer[a];
			a++;
			if (a==MAXCIRC) a=0;
		}
		return outData;
	}
	
	// Return the maximum size of this circular buffer
	public int retMax()	{
		return MAXCIRC;
	}
	
	// Clear the filled flag
	public void clearFilled()	{
		filled=false;
	}
	
	// Return the filled flag to the user
	public boolean getFilled()	{
		return filled;
	}

}
