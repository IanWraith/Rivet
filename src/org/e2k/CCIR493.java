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
	private int characterCount=0;
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
	private int invertedPDXCounter;
	
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
				lineBuffer.delete(0,lineBuffer.length());
				characterCount=0;
				rx=0;
				dx=0;
				buffer10=0;
				buffer20=0;
				bitCount=0;
				invertedPDXCounter=0;
				clearMessageBuffer();
				return outLines;
			}
		}		
		// Receive and decode the message
		if (state==2)	{
			if (symbolCounter>=(long)samplesPerSymbol)	{		
				// Demodulate a single bit
				boolean bit=getSymbolFreqBin(circBuf,waveData,0);
				// If debugging display only the raw bits
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
				// If not debugging handle the traffic properly
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
		// Run FFTs on the early and late parts of the symbol
		double early[]=do160FFTHalfSymbolBinRequest(circBuf,start,lowBin,highBin);
		start=start+((int)samplesPerSymbol/2);
		double late[]=do160FFTHalfSymbolBinRequest(circBuf,start,lowBin,highBin);
		// Feed the early late difference into a buffer
		addToAdjBuffer(early[0]-late[0]);
		// Calculate the symbol timing correction
		symbolCounter=adjAdjust();
		// Now work out the binary state represented by this symbol
		double lowTotal=early[0]+late[0];
		double highTotal=early[1]+late[1];
		if (theApp.isInvertSignal()==false)	{
			if (lowTotal>highTotal) bit=true;
			else bit=false;
		}
		else	{
			// If inverted is set invert the bit returned
			if (lowTotal>highTotal) bit=false;
			else bit=true;
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
		if (Math.abs(av)<5) return 0;
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
			// Look for and count inverted PDX characters
			if (buffer10==262) invertedPDXCounter++;
			// If more than 2 inverted PDXs have been received change the invert setting
			if (invertedPDXCounter==2)	{
				invertedPDXCounter=0;
				if (theApp.isInvertSignal()==true) theApp.setInvertSignal(false);
				else theApp.setInvertSignal(true);
			}
			int c=ret10BitCode(buffer10);
			// Detect and count phasing characters
			if (c==125) dx++;
			else if ((c<=111)&&(c>=104)) rx++;
			// Is phasing complete ?
			if (((dx==2)&&(rx==1))||((dx==1)&&(rx==2)))	{
				bitCount=0;
				messageState=1;
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
				if (formatSpecifier==112) lineBuffer.append(theApp.getTimeStamp()+" CCIR493-4 Distress Alert ");
				else if (formatSpecifier==116) lineBuffer.append(theApp.getTimeStamp()+" CCIR493-4 All Stations ");
				else if (formatSpecifier==114) lineBuffer.append(theApp.getTimeStamp()+" CCIR493-4 Group Selective Call ");
				else if (formatSpecifier==120) lineBuffer.append(theApp.getTimeStamp()+" CCIR493-4 Individual Selective Call ");
				else if (formatSpecifier==102) lineBuffer.append(theApp.getTimeStamp()+" CCIR493-4 Geographic Selective Call ");
				else if (formatSpecifier==123) lineBuffer.append(theApp.getTimeStamp()+" CCIR493-4 Individual Selective Call Using Semi/Automatic Service ");
				else lineBuffer.append(theApp.getTimeStamp()+" CCIR493-4 Unknown Call ");
			}
		}
		// Load the body of the message into the messageBuffer array
		else if (messageState==2)	{
			if (bitCount%10==0)	{
				int i=bitCount/10;
				// Check if the message length is over running the messageBuffer
				// If it isn't add the error checked and decoded 10 bit character to it
				if (i>=messageBuffer.length) messageState=3;
				else messageBuffer[i-1]=ret10BitCode(buffer10);
				// Check for an ARQ ARQ end sequence
				if (i>3)	{
					if ((messageBuffer[i-1]==117)&&(messageBuffer[i-2]==117)) messageState=3;
				}
			}	
		}
		// The message has been decoded so display it
		else if (messageState==3)	{
			outLines=decodeMessageBody();
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
	
	// Hunt for the call format specifier
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
	
	// Convert the data into English
	private String[] decodeMessageBody()	{
		String ol[]=new String[3];
		ol[0]=lineBuffer.toString()+"Station "+getAStationIdentity()+" Calling "+getBStationIdentity()+getCategory();
		return ol;
	}
	
	// Gets the identity of the B station and returns it as a String
	private String getBStationIdentity()	{
		int b1,b2;
		// B1
		int b11=messageBuffer[0];
		int b12=messageBuffer[5];
		if ((b11==-1)&&(b12!=-1)) b1=b12;
		else if ((b12==-1)&&(b11!=-1)) b1=b11;
		else if ((b11==-1)&&(b12==-1)) return "ERROR";
		else if (b11==b12) b1=b11;
		else b1=b11;
		// B2
		int b21=messageBuffer[2];
		int b22=messageBuffer[7];	
		if ((b21==-1)&&(b22!=-1)) b2=b22;
		else if ((b22==-1)&&(b21!=-1)) b2=b21;
		else if ((b21==-1)&&(b22==-1)) return "ERROR";
		else if (b21==b22) b2=b21;
		else b2=b21;
		// Make up the identity B1 + B2
		String r=Integer.toString(b1)+Integer.toString(b2);
		return r;
	}
	
	// Gets the identity of the A station and returns it as a String
	private String getAStationIdentity()	{
		int a1,a2;
		// A1
		int a11=messageBuffer[6];
		int a12=messageBuffer[11];
		if ((a11==-1)&&(a12!=-1)) a1=a12;
		else if ((a12==-1)&&(a11!=-1)) a1=a11;
		else if ((a11==-1)&&(a12==-1)) return "ERROR";
		else a1=a11;
		// A2
		int a21=messageBuffer[8];
		int a22=messageBuffer[13];	
		if ((a21==-1)&&(a22!=-1)) a2=a22;
		else if ((a22==-1)&&(a21!=-1)) a2=a21;
		else if ((a21==-1)&&(a22==-1)) return "ERROR";
		else a2=a21;
		// Make up the identity A1 + A2
		String r=Integer.toString(a1)+Integer.toString(a2);
		return r;
	}
	
	// Gets the calls category and return it as a String
	private String getCategory()	{
		int c,c1,c2;
		c1=messageBuffer[4];
		c2=messageBuffer[9];
		if ((c1==-1)&&(c2!=-1)) c=c2;
		else if ((c2==-1)&&(c1!=-1)) c=c1;
		else if ((c2==-1)&&(c1==-1)) return " (ERROR)";
		else c=c1;
		// Categories
		if (c==100) return " (Routine)";
		else if (c==106) return " (Ship's Business)";
		else if (c==108) return " (Safety)";
		else if (c==110) return " (Urgency)";
		else if (c==112) return " (Distress)";
		else return "(Unknown)";
	}
	
	// Clear the messageBuffer
	private void clearMessageBuffer ()	{
		int a;
		for (a=0;a<messageBuffer.length;a++){
			messageBuffer[a]=-1;
		}
	}

}
