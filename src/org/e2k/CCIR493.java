package org.e2k;

import javax.swing.JOptionPane;

public class CCIR493 extends FSK {
	
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	public StringBuffer lineBuffer=new StringBuffer();
	private int highTone;
	private int lowTone;
	private int messageState;
	private int totalErrorCount=0;
	private int characterCount=0;
	private int totalCharacterCount=0;
	private final int MAXCHARLENGTH=80;
	private int highBin;
	private int lowBin;
	private double adjBuffer[]=new double[5];
	private int adjCounter=0;
	private int buffer10=0;
	private int buffer20=0;
	private int dx;
	private int rx;
	private int formatSpecifier;
	private int bitCount=0;
	private int messageBuffer[]=new int[20];
	
	public CCIR493 (Rivet tapp)	{
		theApp=tapp;
	}
	
	// The main decode routine
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[3];
			
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCCIR493-4 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol=samplesPerSymbol(100.0,waveData.getSampleRate());
			state=1;
			lineBuffer.delete(0,lineBuffer.length());
			messageState=0;
			theApp.setStatusLabel("Sync Hunt");
			return null;
		}
		
		
		// Look for a 100 baud alternating sequence
		if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return null;
			// Look for a 100 baud alternating sync sequence
			if (detectSync(circBuf,waveData)==true)	{
				state=2;
				totalErrorCount=0;
				totalCharacterCount=0;
				characterCount=0;
				rx=0;
				dx=0;
				buffer10=0;
				buffer20=0;
				bitCount=0;
				return outLines;
			}
		}		
		
		if (state==2)	{
			if (symbolCounter>=(long)samplesPerSymbol)	{		
				// Demodulate a single bit
				boolean bit=getSymbolFreqBin(circBuf,waveData,0);
				
				if (theApp.isDebug()==true)	{
					if (bit==true) lineBuffer.append("1");
					else lineBuffer.append("0");
					characterCount++;
					if (characterCount==MAXCHARLENGTH)	{
						outLines[0]=lineBuffer.toString();
						characterCount=0;
						lineBuffer.delete(0,lineBuffer.length());
					}
				}
				else outLines=handleTraffic(bit);
			}
		}
		
		
		sampleCount++;
		symbolCounter++;
		return outLines;
		}
	

	
	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
	
	// See if the buffer holds a alternating sequence
	private boolean detectSync(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0,b0,b1;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		b0=getFreqBin();
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol*1;
		int f1=getSymbolFreq(circBuf,waveData,pos);
		b1=getFreqBin();
		if (f0==f1) return false;
		if (f0>f1)	{
			highTone=f0;
			highBin=b0;
			lowTone=f1;
			lowBin=b1;
			}
			else	{
			highTone=f1;
			highBin=b1;
			lowTone=f0;
			lowBin=b0;
			}
		int shift=highTone-lowTone;
		// The shift for CCIR493-4 should be should be 170 Hz
		if ((shift>=150)&&(shift<190)) return true;
		else return false;
	}	
	
	// Get the frequency at a certain symbol
	private int getSymbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int fr=doCCIR493_160FFT(circBuf,waveData,start);
		return fr;
	}	
	
	// Return the symbol frequency given the bins that hold the possible tones
	private boolean getSymbolFreqBin (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		boolean bit;
		double early[]=do160FFTHalfSymbolBinRequest(circBuf,start,lowBin,highBin);
		start=start+((int)samplesPerSymbol/2);
		double late[]=do160FFTHalfSymbolBinRequest(circBuf,start,lowBin,highBin);
		
		addToAdjBuffer(early[0]-late[0]);
		symbolCounter=adjAdjust();

		double lowTotal=early[0]+late[0];
		double highTotal=early[1]+late[1];
		if (theApp.isInvertSignal()==false)	{
			if (lowTotal>highTotal) bit=true;
			else bit=false;
		}
		else	{
			if (lowTotal>highTotal) bit=true;
			else bit=false;
		}
		return bit;
	}
	
	// Add a comparator output to a circular buffer of values
	private void addToAdjBuffer (double in)	{
		adjBuffer[adjCounter]=in;
		adjCounter++;
		if (adjCounter==adjBuffer.length) adjCounter=0;
	}
	
	// Return the average of the circular buffer
	private double adjAverage()	{
		int a;
		double total=0.0;
		for (a=0;a<adjBuffer.length;a++)	{
			total=total+adjBuffer[a];
		}
		return (total/adjBuffer.length);
	}
	
	// Get the average value and return an adjustment value
	private int adjAdjust()	{
		double av=adjAverage();
		if (Math.abs(av)<50) return 0;
		else if (av<0.0) return 1;
		else return -1;
	}		
	
	// The main function for handling incoming bits
	private String[] handleTraffic (boolean b)	{
		String outLines[]=new String[3];
		addTo10BitBuffer(b);
		addTo20BitBuffer(b);
		bitCount++;
		// Hunt for dx and rx characters which make up the phasing sequence
		if (messageState==0)	{
			int c=ret10BitCode(buffer10);
			if (c==125) dx++;
			else if ((c<=111)&&(c>=104)) rx++;
			// Is phasing complete ?
			if (((dx==2)&&(rx==1))||((dx==1)&&(rx==2)))	{
				bitCount=0;
				
				// below was 1
				
				messageState=2;
				bitCount=0;
			}
			if (bitCount>300) state=1;
		}
		// Phasing complete now look for the format specifier
		else if (messageState==1)	{
			if (bitCount>=20) formatSpecifier=formatSpecifierHunt(buffer20);
			else if (bitCount>300) state=1;
			else return null;
			if (formatSpecifier!=-1)	{
				bitCount=0;
				messageState=2;
				if (formatSpecifier==112) outLines[0]=theApp.getTimeStamp()+" Distress Alert";
				else if (formatSpecifier==116) outLines[0]=theApp.getTimeStamp()+" All Stations";
				else if (formatSpecifier==114) outLines[0]=theApp.getTimeStamp()+" Group Selective Call";
				else if (formatSpecifier==120) outLines[0]=theApp.getTimeStamp()+" Individual Selective Call";
				else if (formatSpecifier==102) outLines[0]=theApp.getTimeStamp()+" Geographic Selective Call";
				else if (formatSpecifier==123) outLines[0]=theApp.getTimeStamp()+" Individual Selective Call Using Semi/Automatic Service";
			}
		}
		// Load the body of the message into the messageBuffer array
		else if (messageState==2)	{
			if (bitCount%10==0)	{
				int i=bitCount/10;
				if (i>messageBuffer.length) messageState=4;
				else messageBuffer[i-1]=ret10BitCode(buffer10);
			}	
			// Check for an ARQ ARQ end sequence
			if (buffer20==0xaeaba)	messageState=3;
		}
		// The message has been decoded so display it
		else if (messageState==3)	{
			outLines=decodeMessageBody();
			messageState=0;
			state=1;
			}
		// No end to the message has been found so there must be a problem here
		else if (messageState==4)	{
			outLines=decodeMessageBody();
			outLines[2]="Error : Message over run !";
			messageState=0;
			state=1;
		}
		
		return outLines;
	}
	
	// Add a bit to the 10 bit buffer
	private void addTo10BitBuffer (boolean b)	{
		buffer10=buffer10<<1;
		buffer10=buffer10&0x3FF;
		if (b==true) buffer10++;
	}
	
	// Add a bit to the 20 bit buffer
	private void addTo20BitBuffer (boolean b)	{
		buffer20=buffer20<<1;
		buffer20=buffer20&0xFFFFF;
		if (b==true) buffer20++;
		}
	
	// Returns a 7 bit value from a 10 bit block
	// the last 3 bits give the number of B (0) bits in the first 7 bits
	// if there is an error then -1 is returned
	private int ret10BitCode (int in)	{
		int o=0,b=0;
		if ((in&512)>0) o=1;
		else b++;
		if ((in&256)>0) o=o+2;
		else b++;
		if ((in&128)>0) o=o+4;
		else b++;
		if ((in&64)>0) o=o+8;
		else b++;
		if ((in&32)>0) o=o+16;
		else b++;
		if ((in&16)>0) o=o+32;
		else b++;
		if ((in&8)>0) o=o+64;
		else b++;
		// Bits 4,2 and 1 are check bits
		if (b==(in&7)) return o;
		else return -1;
	}
	
	private int formatSpecifierHunt (int in)	{
		int c1,c2;
		c2=ret10BitCode(in&1023);
		c1=ret10BitCode((in&0xFFC00)>>10);
		if (c1!=c2) return -1;
		else if (c1==112) return c1;
		else if (c1==116) return c1;
		else if (c1==114) return c1;
		else if (c1==120) return c1;
		else if (c1==102) return c1;
		else if (c1==123) return c1;
		else return -1;
	}
	
	private String[] decodeMessageBody()	{
		String ol[]=new String[3];
		ol[0]="";
		ol[1]="";
		int a;
		for (a=0;a<10;a++)	{
			ol[0]=ol[0]+Integer.toString(messageBuffer[a])+",";
		}
		for (a=10;a<messageBuffer.length;a++)	{
			ol[1]=ol[1]+Integer.toString(messageBuffer[a])+",";
		}
		return ol;
	}

}
