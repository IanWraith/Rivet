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

import javax.swing.JOptionPane;

public class CircularDataBuffer {
	
	private final int MAXCIRC=1024*10;
	private int circBufferCounter=0;
	private int[] circDataBuffer=new int[MAXCIRC];
	
	// Add data to the incoming data circular buffer
	public void addToCircBuffer (int i)	{
		try	{
			circDataBuffer[circBufferCounter]=i;
			circBufferCounter++;
			if (circBufferCounter==MAXCIRC)	circBufferCounter=0;
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in addToCircBuffer() "+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	// Return a user defined section of the circular buffer
	public int[] extractData (int start,int length)	{
		try	{
			int count=0,a=circBufferCounter+start;
			int outData[]=new int[length];
			if (a>=MAXCIRC) a=a-MAXCIRC;
			for (count=0;count<length;count++)	{
				outData[count]=circDataBuffer[a];
				a++;
				if (a==MAXCIRC) a=0;
			}
			return outData;
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in extractData() "+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	// Return a user defined section of the circular buffer
	public double[] extractDataDouble (int start,int length)	{
		try	{
			int count=0,a=circBufferCounter+start;
			double outData[]=new double[length];
			if (a>=MAXCIRC) a=a-MAXCIRC;
			for (count=0;count<length;count++)	{
				outData[count]=circDataBuffer[a];
				a++;
				if (a==MAXCIRC) a=0;
			}
			return outData;
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in extractDataDouble() "+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	// Return the maximum size of this circular buffer
	public int retMax()	{
		return MAXCIRC;
	}
		
	// Return the current value of the buffer counter
	public int getBufferCounter()	{
		return circBufferCounter;
	}
	
	// Allow the value of the buffer counter to be set
	public void setBufferCounter(int val)	{
		circBufferCounter=val;
	}
	
	// Return the array number with the highest value
	public int returnHighestBin ()	{
		try	{
			int a,highBin=-1;
			int highVal=Integer.MIN_VALUE;
			for (a=0;a<circBufferCounter;a++)	{
				if (circDataBuffer[a]>highVal)	{
					highVal=circDataBuffer[a];
					highBin=a;
				}
			}
			return highBin;
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in returnHighestBin() "+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return 0;
		}
	}
	
	// Return the array number with the lowest value
	public int returnLowestBin ()	{
		try	{
			int a,lowBin=-1;
			int lowVal=Integer.MAX_VALUE;
			for (a=0;a<circBufferCounter;a++)	{
				if (circDataBuffer[a]<lowVal)	{
					lowVal=circDataBuffer[a];
					lowBin=a;
				}
			}
			return lowBin;
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in returnLowestBin() "+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return 0;
		}
	}
	
	// Directly access the data buffer
	public int directAccess (int i)	{
		return this.circDataBuffer[i];
	}
	
	// TODO add a method that takes an average from a section of the circular buffer
	
}
