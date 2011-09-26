package org.e2k;

import javax.swing.JOptionPane;

public class CROWD36 extends MFSK {
	
	private int baudRate=40;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private long energyStartPoint;
	private StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private boolean figureShift=false; 
	private int lineCount=0;
	private int correctionValue=0;
	
	public int toneFreq[]=new int[100];
	
	private final String C36A[]={
			"NULL",
			"Q",
			"X",
			"W",
			"V",
			"E",
			"K",
			" ",
			"B",
			"R",
			"J",
			"ctl",
			"G",
			"T",
			"F",
			"fs",
			"M",
			"Y",
			"C",
			"cr",
			"Z",
			"U",
			"L",
			"*",
			"D",
			"I",
			"H",
			"ls",
			"S",
			"O",
			"N",
			"-",
			"A",
			"P",
			"",
			""
			};
	
	public CROWD36 (Rivet tapp,int baud)	{
		baudRate=baud;
		theApp=tapp;
	}
	
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
	
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		

		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.sampleRate>11025)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCROWD36 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.channels!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.sampleRate);
			state=1;
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			waveData.Clear();
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			theApp.setStatusLabel("Known Tone Hunt");
			return null;
		}
		
		// Hunting for known tones
		if (state==1)	{
			outLines[0]=syncToneHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				state=2;
				energyStartPoint=sampleCount;
				energyBuffer.setBufferCounter(0);
				theApp.setStatusLabel("Calculating Symbol Timing");
			}
		}
		
		// Set the symbol timing
		if (state==2)	{
			final int lookAHEAD=1;
			// Obtain an average of the last few samples put through ABS
			double no=samplesPerSymbol/20.0;
			energyBuffer.addToCircBuffer(circBuf.getABSAverage(0,(int)no));
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()>(int)(samplesPerSymbol*lookAHEAD))	{
				// Now find the lowest energy value
				long perfectPoint=energyBuffer.returnLowestBin()+energyStartPoint+(int)samplesPerSymbol;
				
				//theApp.debugDump("perfectPoint,"+Long.toString(perfectPoint));
				//theApp.debugDump("sampleCount,"+Long.toString(sampleCount));
				
				// Calculate what the value of the symbol counter should be
				symbolCounter=(int)samplesPerSymbol-(perfectPoint-sampleCount);
				
				//theApp.debugDump("symbolCounter,"+Long.toString(symbolCounter));
				
				state=3;
				theApp.setStatusLabel("Symbol Timing Achieved");
				outLines[0]=theApp.getTimeStamp()+" Symbol timing found at position "+Long.toString(perfectPoint);
				sampleCount++;
				symbolCounter++;
				
				
				/////////////////////////////////////////////////////////////////
				//int a;
				//for (a=0;a<energyBuffer.getBufferCounter();a++)	{
					//int ar[]=circBuf.extractData(a,1);
					//String st=Integer.toString(energyBuffer.directAccess(a))+","+Integer.toString(ar[0]);
					//if (a==energyBuffer.returnHighestBin())	st=st+",10000";
					//else if (a==energyBuffer.returnLowestBin())	st=st+",-10000";
					//else st=st+",0";		
					//theApp.debugDump(st);
				//}
				
				/////////////////////////////////////////////////////////////////
				
				return outLines;
			}
		}
		
		// Decode traffic
		if (state==3)	{
			//theApp.debugDump(Long.toString(sampleCount)+","+Long.toString(symbolCounter)+","+Integer.toString(circBuf.getLast()));
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				
				
				//theApp.debugDump("BBB");	
				//int a;
				//int data[]=circBuf.extractData(0,(int)samplesPerSymbol);
				//for (a=0;a<data.length;a++)	{
					//String st=Integer.toString(data[a]);
					//theApp.debugDump(st);
				//}
							
				symbolCounter=0;				
				int freq=crowd36Freq(circBuf,waveData,0);
				outLines=displayMessage(freq,waveData.fromFile);
			}
			
		}
		
		sampleCount++;
		symbolCounter++;
		return outLines;				
	}
	
	private int crowd36Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		
		// 8 KHz sampling
		if (waveData.sampleRate==8000.0)	{
			int freq=doCR36_8000FFT(circBuf,waveData,pos);
			freq=freq+correctionValue;
			return freq;
		}
		
		return -1;
	}
	
	private String[] displayMessage (int freq,boolean isFile)	{
		//String tChar=getChar(freq);
		String outLines[]=new String[2];
		
		int tone=getTone(freq);
		toneFreq[tone]++;
		
		outLines[0]=lineBuffer.toString();;
		lineBuffer.delete(0,lineBuffer.length());
		lineCount=0;
		outLines[0]="UNID "+freq+" Hz at "+Long.toString(sampleCount+(int)samplesPerSymbol)+" tone "+Long.toString(tone);
			
       return outLines;
		
		
		//return null;
	}
	
	private String getChar(int tone)	{
		final int errorAllowance=15;
	    //if ((tone>(1995-errorAllowance))&&(tone<(1995+errorAllowance))) return ("R");
	    //else if ((tone>(1033-errorAllowance))&&(tone<(1033+errorAllowance))) return ("Y");
	
		return null;
	}
	
	// Convert from a frequency to a tone number
	private int getTone (int freq)	{
		int a,index=-1,lowVal=999,dif;
		final int Tones[]={300,340,380,420,460,500,54,580,620,660,700,
				740,780,820,860,900,940,980,1020,1060,1100,1140,1180,1220,1260,
				1300,1340,1380,1420,1460,1500,1540,1580,1620,1660,1700};
		for (a=0;a<Tones.length;a++)	{
			dif=Math.abs(Tones[a]-freq);
			if (dif<lowVal)	{
				lowVal=dif;
				index=a;
			}
		}
		return index;
	}
	
	
	// Hunt for known CROWD 36 tones
	private String syncToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
			String line;
			final int ErrorALLOWANCE=20;
			// Get 4 symbols
			int freq1=crowd36Freq(circBuf,waveData,0);
			// Check this first tone isn't just noise
			if (getPercentageOfTotal()<5.0) return null;
			int freq2=crowd36Freq(circBuf,waveData,(int)samplesPerSymbol*1);
			int freq3=crowd36Freq(circBuf,waveData,(int)samplesPerSymbol*2);
			int freq4=crowd36Freq(circBuf,waveData,(int)samplesPerSymbol*3);
			// Check 2 of the symbol frequencies are the same
			if ((freq1!=freq3)||(freq2!=freq4)) return null;
			// Find the difference between the frequencies
			int dif=freq1-freq2;
			if ((dif<(1040-ErrorALLOWANCE))||(dif>(1040+ErrorALLOWANCE))) return null;
			// Calculate the correction value
			correctionValue=1700-freq1;
			line=theApp.getTimeStamp()+" CROWD36 Sync Tones Found (Correcting by "+Integer.toString(correctionValue)+" Hz) at "+Long.toString(sampleCount);
			return line;
		}
	


}
