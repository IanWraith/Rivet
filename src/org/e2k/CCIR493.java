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
	private int highBin;
	private int lowBin;
	private double adjBuffer[]=new double[15];
	private int adjCounter=0;
	private int buffer10=0;
	private int buffer20=0;
	private int dx;
	private int rx;
	private int formatSpecifier;
	private int bitCount=0;
	private int messageBuffer[]=new int[20];
	private int invertedPDXCounter;
	private int unCorrectedInput;
	private final int VALIDWORDS[]={7,518,262,773,134,645,389,900,70,581,325,836,197,708,452,963,38,549,293,804,
			165,676,420,931,101,612,356,867,228,739,483,994,22,533,277,788,149,660,404,
			915,85,596,340,851,212,723,467,978,53,564,308,819,180,691,435,946,116,627,
			371,882,243,754,498,1009,14,525,269,780,141,652,396,907,77,588,332,843,204,
			715,459,970,45,556,300,811,172,683,427,938,108,619,363,874,235,746,490,1001,
			29,540,284,795,156,667,411,922,92,603,347,858,219,730,474,985,60,571,315,826,
			187,698,442,953,123,634,378,889,250,761,505,1016};
	private final int BITVALUES[]={1,2,4,8,16,32,64,128,256,512};
	
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
				rx=0;
				dx=0;
				buffer10=0;
				buffer20=0;
				bitCount=0;
				invertedPDXCounter=0;
				if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 Sync Found";
				clearMessageBuffer();
				return outLines;
			}
		}		
		// Receive and decode the message
		if (state==2)	{
			if (symbolCounter>=(long)samplesPerSymbol)	{		
				// Demodulate a single bit
				boolean bit=getSymbolFreqBin(circBuf,waveData,0);
				// Decode this
				outLines=handleTraffic(bit);
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
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return false; 
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
		// Early/Late gate code
		if (lowTotal>highTotal) addToAdjBuffer(early[1]-late[1]);
		else addToAdjBuffer(early[0]-late[0]);
		symbolCounter=adjAdjust();
		// All done return the bit value
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
			if (buffer10==262)	{
				if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 Inverted PDX";
				invertedPDXCounter++;
			}
			// If more than 2 inverted PDXs have been received change the invert setting
			if (invertedPDXCounter==2)	{
				invertedPDXCounter=0;
				dx++;
				if (theApp.isInvertSignal()==true) theApp.setInvertSignal(false);
				else theApp.setInvertSignal(true);
				// Invert the contents of buffer10
				invertBuffer10();
			}
			int c=ret10BitCode(buffer10,false);
			// Detect and count phasing characters
			if (c==125)	{
				if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 PDX";
				dx++;
			}
			else if ((c<=111)&&(c>=104))	{
				if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 RX ("+Integer.toString(c)+")";
				rx++;
			}
			// Is phasing complete ?
			if (((dx==2)&&(rx==1))||((dx==1)&&(rx==2)))	{
				bitCount=0;
				messageState=1;
				if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 Phasing Detect : PDX="+Integer.toString(dx)+" RX="+Integer.toString(rx);
			}
			if (bitCount>1800)	{
				if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 Phasing Timeout";
				state=1;
			}
		}
		// Phasing complete now look for the format specifier
		else if (messageState==1)	{
			// We now have sync so only check for the format specifier every 10 bits
			if (bitCount%10==0)	{
				int c1=ret10BitCode((buffer20&0xFFC00)>>10,true);
				int c2=ret10BitCode(buffer20&1023,true);
				formatSpecifier=formatSpecifierHunt(c1,c2);
				if (theApp.isDebug()==true)	{
					if (c2!=-1) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 Character "+Integer.toString(c2);
					else outLines[0]=theApp.getTimeStamp()+" CCIR493-4 Character Error was "+Integer.toString(unCorrectedInput);
				}
				// If we haven't received a format specifier after 300 bits then something has gone wrong
				if (bitCount>300)	{
					if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CCIR493-4 Format Specifier Timeout";
					state=1;
				}
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
		}
		// Load the body of the message into the messageBuffer array
		else if (messageState==2)	{
			if (bitCount%10==0)	{
				int i=bitCount/10;
				// Check if the message length is over running the messageBuffer
				// If it isn't add the error checked and decoded 10 bit character to it
				if (i>=messageBuffer.length) messageState=3;
				else messageBuffer[i-1]=ret10BitCode(buffer10,true);
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
	
	// Returns a 7 bit value from a 10 bit block the last 3 bits give the number of B (0) bits in the first 7 bits
	// if there is an error then -1 is returned the routine will fix words with up to errorMax bits incorrect
	// if errorBitsAllowed is false then no errors will be fixed
	private int ret10BitCode (int in,boolean errorBitsAllowed)	{
		int a,b,dif,errorMax;
		// Make copy of what is going into the error corrector
		unCorrectedInput=in;
		if (errorBitsAllowed==true) errorMax=1;
		else errorMax=0;
		for (a=0;a<VALIDWORDS.length;a++){
			dif=0;
			for (b=0;b<BITVALUES.length;b++)	{
				if ((in&BITVALUES[b])!=(VALIDWORDS[a]&BITVALUES[b])) dif++;
			}
			if (dif<=errorMax) return a;
		}
		return -1;
	}
	
	// Hunt for the call format specifier
	private int formatSpecifierHunt (int c1,int c2)	{
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
		// Make up the identity B1 + B2 (adding a leading zero to both parts if needed)
		String r=String.format("%02d",b1)+String.format("%02d",b2);
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
		// Make up the identity A1 + A2 (adding a leading zero to both parts if needed)
		String r=String.format("%02d",a1)+String.format("%02d",a2);
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
	
	// Invert the contents of the buffer10 variable
	private void invertBuffer10()	{
		int a,c=0;
		for (a=0;a<BITVALUES.length;a++)	{
			if ((buffer10&BITVALUES[a])==0) c=c+BITVALUES[a];
		}
		buffer10=c;
	}


}
