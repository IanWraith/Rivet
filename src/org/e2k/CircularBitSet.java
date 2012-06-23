package org.e2k;

import java.util.BitSet;

public class CircularBitSet extends BitSet {

	private static final long serialVersionUID=1L;
	private int counter=0;
	private int totalLength;
	
	public void add (boolean bit)	{
		if (bit==true) this.set(counter);
		else this.clear(counter);
		counter++;
		if (counter==totalLength) counter=0;
	}
	
	public void totalClear()	{
		this.clear();
		counter=0;
	}
	
	public String extractSection (int start,int end)	{
		StringBuffer out=new StringBuffer();
		int a,tc=counter;
		for (a=start;a<end;a++)	{
			if (this.get(tc)==true) out.append("1");
			else out.append("0");
			tc++;
			if (tc==totalLength) tc=0;
		}
		return out.toString();
	}

	public int getTotalLength() {
		return totalLength;
	}

	public void setTotalLength(int totalLength) {
		this.totalLength = totalLength;
	}
	
	
}
