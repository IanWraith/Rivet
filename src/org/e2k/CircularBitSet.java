package org.e2k;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class CircularBitSet extends BitSet {

	private static final long serialVersionUID=1L;
	private int counter=0;
	private int totalLength;
	
	// Add a bit to the BitSet circular array
	public void add (boolean bit)	{
		if (bit==true) this.set(counter);
		else this.clear(counter);
		counter++;
		if (counter==totalLength) counter=0;
	}
	
	// Clear the circular BitSet
	public void totalClear()	{
		this.clear();
		counter=0;
	}
	
	// Extract a section of the circular BitSet as binary encoded as a String
	public String extractSection (int start,int end)	{
		StringBuilder out=new StringBuilder();
		int a;
		int tc=counter+start;
		if (tc>=totalLength) tc=tc-totalLength;
		for (a=start;a<end;a++)	{
			if (this.get(tc)==true) out.append("1");
			else out.append("0");
			tc++;
			if (tc==totalLength) tc=0;
		}
		return out.toString();
	}
	
	// Extract a sequence of a certain length
	public String extractSequence (int len)	{
		StringBuilder sb=new StringBuilder();
		int a,pos=counter-len;
		if (pos<0) pos=totalLength-pos;
		for (a=0;a<len;a++)	{
			if (this.get(pos)==true) sb.append("1");
			else sb.append("0");
			pos++;
			if (pos>=totalLength) pos=0;
		}
		return sb.toString();
	}
	
	
	// Extract a section of the circular BitSet as binary encoded as a String
	public String extractSectionFromStart (int start,int end)	{
		StringBuilder out=new StringBuilder();
		int a;
		for (a=start;a<end;a++)	{
			if (this.get(a)==true) out.append("1");
			else out.append("0");
		}
		return out.toString();
	}
	
	// Returns the total length of the circular BitSet 
	public int getTotalLength() {
		return totalLength;
	}

	// Sets the total length of the circular BitSet
	public void setTotalLength(int totalLength) {
		this.totalLength = totalLength;
	}
	
	// This method outputs a string which displays the circular array as 8 bit hex numbers
	public String extractBitSetasHex()	{
		int a;
		StringBuilder out=new StringBuilder();
		int idata[]=returnInts();
		for (a=0;a<idata.length;a++)	{
			// Ensure the number consists of two characters
			if (idata[a]<16) out.append("0");
			out.append(Integer.toHexString(idata[a]));
			if (a<idata.length-1) out.append(",");
		}
		return out.toString();
	}
	
	// Returns the circular BitSet as an array of 8 bit ints
	public int[] returnInts ()	{
		int itotal=totalLength/8;
		int out[]=new int[itotal];
		BitSet bset=new BitSet();
		int a,tc=counter,bcount=0,ocount=0;
		for (a=0;a<totalLength;a++)	{
			if (this.get(tc)==true) bset.set(bcount);
			else bset.clear(bcount);
			bcount++;
			if (bcount==8)	{
				out[ocount]=binaryToInt8(bset);
				ocount++;
				bset.clear();
				bcount=0;
			}
			tc++;
			if (tc==totalLength) tc=0;
		}
		return out;
	}
	
	// Returns the circular BitSet as a list of ints
	public List<Integer> returnIntsFromStart (int start)	{
		List<Integer> listI=new ArrayList<Integer>();
		BitSet bset=new BitSet();
		int a,bcount=0;
		for (a=start;a<totalLength;a++)	{
			if (this.get(a)==true) bset.set(bcount);
			else bset.clear(bcount);
			bcount++;
			if (bcount==8)	{
				listI.add(binaryToInt8(bset));
				bset.clear();
				bcount=0;
			}
		}
		return listI;
	}
	
	// Converts an 8 bit BitSet into an int
	private int binaryToInt8 (BitSet bin)	{
		int total=0;
		if (bin.get(0)==true) total=total+128;
		if (bin.get(1)==true) total=total+64;
		if (bin.get(2)==true) total=total+32;
		if (bin.get(3)==true) total=total+16;
		if (bin.get(4)==true) total=total+8;
		if (bin.get(5)==true) total=total+4;
		if (bin.get(6)==true) total=total+2;
		if (bin.get(7)==true) total++;
		return total;
	}
	
}
