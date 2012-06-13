package org.e2k;

import javax.swing.JOptionPane;

// From info received (which I'm very grateful for) it appears CIS36-50 (BEE) messages have the following format
// 44 bit sync sequence made up so if these bits 21 are true and 23 false
// 70 bit session key made up of 7 bit blocks of which 3 bits are true and 4 bits false
// same 70 bit session key is then repeated
// followed by the message which is made up of encrypted ITA-3 characters (so again 3 bits true and 4 false)
// the end of the message is signalled with the binary sequence 1110111 1110111 1110111

public class CIS3650 extends FSK {

	private int state=0;
	private double samplesPerSymbol50;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	public StringBuffer lineBuffer=new StringBuffer();
	private int highTone;
	private int lowTone;
	private int syncState;
	private int buffer7=0;
	private int buffer21=0;
	private int characterCount;
	private int startCount;
	private final int ITA3VALS[]={26,25,76,28,56,19,97,82,112,35,11,98,97,84,70,74,13,100,42,69,50,73,37,22,21,49,67,88,14,38,104,7,52,41,44,81};
	private final String ITA3LETS[]={"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","<cr>","<lf>","<let>","<fig>"," ","<unperf>","<Request>","<Idle a>","<Idle b>","<0x51>"}; 
	private int totalCharacterCount=0;
	private int totalErrorCount=0;
	private int highBin;
	private int lowBin;
	private int b7Count;
	private int countSinceSync;
	private boolean startBuffer[]=new boolean[184];
	private final double KALMAN1=0.99;
	private final double KALMAN2=0.009;
	private final double EARLYLATEADJUST=1;
	
	public CIS3650 (Rivet tapp)	{
		theApp=tapp;
	}
	
	// The main decode routine
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[3];
		
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCIS 36-50 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			samplesPerSymbol50=samplesPerSymbol(50.0,waveData.getSampleRate());
			setState(1);
			lineBuffer.delete(0,lineBuffer.length());
			syncState=0;
			buffer7=0;
			buffer21=0;
			characterCount=0;
			return null;
		}
		
		
		// Look for a 36 baud or a 50 baud alternating sequence
		if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return null;
			// Look for a 50 baud alternating sync sequence
			if (detect50Sync(circBuf,waveData)==true)	{
				totalErrorCount=0;
				totalCharacterCount=0;
				syncState=1;
				setState(2);
				buffer7=0;
				b7Count=0;
				return outLines;
			}
		}
		
		if (state==2)	{
			if (symbolCounter>=(long)samplesPerSymbol50)	{		
				// Demodulate a single bit
				boolean bit=getSymbolFreqBin(circBuf,waveData,0);
				addToBuffer7(bit);
				b7Count++;
				// Get 14 bits (to allow the early late gate to settle) but only look at the last 3
				if (b7Count==14)	{
					buffer7=buffer7&0x7;
					// Look for 101 (5) or 010 (2)
					if ((buffer7==5)||(buffer7==2))	{
						setState(3);
						if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CIS 36-50 50 baud sync sequence found : lowBin="+Integer.toString(lowBin)+" highBin="+Integer.toString(highBin);
						b7Count=0;
						countSinceSync=0;
						clearStartBuffer();
					}	
					else	{
						if (theApp.isDebug()==true)  outLines[0]=theApp.getTimeStamp()+" Unable to obtain CIS 36-50 50 baud alternating sequence";
						state=1;
					}
				}
			}
		}
			
		// Read in symbols
		if (state==3)	{
			// Only demodulate a bit every samplesPerSymbol50 samples
			if (symbolCounter>=(long)samplesPerSymbol50)	{		
				// Demodulate a single bit
				boolean bit=getSymbolFreqBin(circBuf,waveData,0);
				// Look for an alternating sequence
				if (syncState==1)	{
					// Increment the count since sync
					countSinceSync++;
					addToBuffer7(bit);
					// If the 7 bit buffer contains an alternating sequence reset the countSinceSync
					if ((buffer7==85)||(buffer7==42)) countSinceSync=0;
					// If no sync work has been found in 500 bits then go back to hunting
					if (countSinceSync>=500)	{
						setState(1);
						if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CIS 36-50 50 baud sync timeout";
					}
				}
				if (theApp.isDebug()==false)	{
					if (syncState==1)	{
						addToStartBuffer(bit);
						// Check if the start buffer is valid
						if (checkStartBuffer()==true)	{
							syncState=2;
							setState(state);
							outLines[0]=theApp.getTimeStamp()+" Message Start";
							long header=extractSyncAsLong();
							outLines[1]="Sync 0x"+Long.toHexString(header);
							outLines[2]=extractSessionKey();
							buffer21=0;
							buffer7=0;
							startCount=0;			
							totalCharacterCount=0;
							totalErrorCount=0;
						}	
					}
					// Read in and display the main body of the message
					else if (syncState==2)	{
						addToBuffer7(bit);
						addToBuffer21(bit);
						startCount++;
						// Look for the end of message sequence
						if (buffer21==0x1DFBF7)	{
							outLines[0]=lineBuffer.toString();
							lineBuffer.delete(0,lineBuffer.length());
							characterCount=0;
							syncState=4;
						}
						// Every 7 bits we should have an ITA-3 character
						if (startCount==7)	{
							if (checkITA3Char(buffer7)==true)	{
								int c=retITA3Val(buffer7);
								lineBuffer.append(ITA3LETS[c]);
							}
							else	{
								// Display 0x77 characters as signalling the end of a message
								if (buffer7==0x77)	{
									lineBuffer.append("<EOM>");
								}
								else	{
									lineBuffer.append("<ERROR ");
									lineBuffer.append(Integer.toString(buffer7));
									lineBuffer.append("> ");
									totalErrorCount++;
								}
							}
							startCount=0;
							buffer7=0;
							characterCount++;
							// Keep a count of the total number of characters in a message
							totalCharacterCount++;
							// If a message has gone on for 5000 characters there must be a problem so force an end
							if (totalCharacterCount>5000) syncState=4;
						} 
						// Display 50 characters on a line
						if (characterCount==50)	{
							outLines[0]=lineBuffer.toString();
							lineBuffer.delete(0,lineBuffer.length());
							characterCount=0;
						}
					}
					// The message must have ended
					else if (syncState==4)	{
						outLines[0]="End of Message ("+Integer.toString(totalCharacterCount)+" characters in this message "+Integer.toString(totalErrorCount)+" of these contained errors)";
						countSinceSync=0;
						syncState=1;
						clearStartBuffer();
						setState(3);
					}
				}
				else	{
					// Debug mode so just display raw binary
					if (bit==true)	lineBuffer.append("1");
					else lineBuffer.append("0");
					if (characterCount==60)	{
						outLines[0]=lineBuffer.toString();
						lineBuffer.delete(0,lineBuffer.length());
						characterCount=0;
					}
					else characterCount++;
				}
			}	
		}
		sampleCount++;
		symbolCounter++;
		return outLines;
	}
	
	// Set the decoder state and update the status label
	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Validating Sync");
		else if ((state==3)&&(syncState==1)) theApp.setStatusLabel("50 Baud Sync Found");
		else if ((state==3)&&(syncState==2)) theApp.setStatusLabel("Decoding Message");
	}

	public int getState() {
		return state;
	}
	

	// Get the frequency at a certain symbol
	private int getSymbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int fr=do80FFT(circBuf,waveData,start);
		return fr;
	}
	
	// Return the symbol frequency given the bins that hold the possible tones
	private boolean getSymbolFreqBin (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		boolean bit;
		double early[]=do80FFTBinRequest(circBuf,waveData,start,lowBin,highBin);
		start=start+((int)samplesPerSymbol50/2);
		double late[]=do80FFTBinRequest(circBuf,waveData,start,lowBin,highBin);
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
		// Early/Late gate code
		if (lowTotal>highTotal) kalmanFilter(getPercentageDifference(early[0],late[0]),KALMAN1,KALMAN2);
		else kalmanFilter(getPercentageDifference(early[1],late[1]),KALMAN1,KALMAN2);
		symbolCounter=adjAdjust();
		// All done return the bit value
		return bit;
	}
	
	// Add a bit to the 7 bit buffer
	private void addToBuffer7(boolean bit)	{
		buffer7<<=1;
		buffer7=buffer7&0x7F;
		if (bit==true) buffer7++;
		}
	
	// Add a bit to the 21 bit buffer
	private void addToBuffer21(boolean bit)	{
		buffer21<<=1;
		buffer21=buffer21&0x1FFFFF;
		if (bit==true) buffer21++;
	}
	
	// See if the buffer holds a 50 baud alternating sequence
	private boolean detect50Sync(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0,b0,b1;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		b0=getFreqBin();
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol50*1;
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
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return false; 
		// The shift for CIS36-50 should be should be 200 Hz
		int shift=highTone-lowTone;
		if ((shift>210)||(shift<190)) return false;
		else return true;
	}
	
	// Check if a number if a valid ITA-3 character
	private boolean checkITA3Char (int c)	{
		int a;
		for (a=0;a<ITA3VALS.length;a++)	{
			if (c==ITA3VALS[a]) return true;
		}
		return false;
	}
	
	// Return a ITA-3 character
	private int retITA3Val (int c)	{
		int a;
		for (a=0;a<ITA3VALS.length;a++)	{
			if (c==ITA3VALS[a]) return a;
		}
		return 0;
	}
	

	// Get an adjustment value from the Kalman filter
	private int adjAdjust()	{
		double r=Math.abs(kalmanNew)/EARLYLATEADJUST;
		if (kalmanNew<0) r=0-r;
		return (int)r;
	}	
	
	// Add a bit to the start buffer
	private void addToStartBuffer (boolean in)	{
		int a;
		// Move all bits one bit to the left
		for (a=1;a<startBuffer.length;a++)	{
			startBuffer[a-1]=startBuffer[a];
			}
		startBuffer[183]=in;
	}
	
	// Check if the start buffer contains a valid 44 bit sync word and two identical 70 bit session keys
	private boolean checkStartBuffer()	{
		int a,count=0,o;
		// Check for 21 true bits in the first 44 bits
		for (a=0;a<44;a++)	{
			if (startBuffer[a]==true) count++;
		}
		if (count!=21) return false;
		count=0;
		// Check the 70 bit session keys are almost the same
		for (a=0;a<70;a++)	{
			if (startBuffer[a+44]!=startBuffer[a+44+70]) count++;
		}	
		if (count>1) return false;
		// Check the session key contains at least 8 valid ITA3 characters
		count=0;
		for (a=44;a<(44+70);a=a+7)	{
			o=extractIntFromStart(a);
			if (checkITA3Char(o)==true)	count++;
		}
		if (count>=8) return true;
		else return false;
	}
	
	
	// Extract the first 44 bits of the start buffer as a long
	private long extractSyncAsLong ()	{
		int a,bc=0;
		long r=0;
		for (a=43;a>=0;a--)	{
			if (startBuffer[a]==true) r=r+(long)Math.pow(2.0,bc);
			bc++;
		}
		return r;
	}
	
	// Clear the start buffer
	private void clearStartBuffer ()	{
		int a;
		for (a=0;a<startBuffer.length;a++)	{
			startBuffer[a]=false;
		}
	}
	
	// Extract a session key from the start buffer
	private String extractSessionKey()	{
		StringBuffer sb=new StringBuffer();
		int a,o;
		sb.append("Session Key is ");
		for (a=44;a<(44+70);a=a+7)	{
			o=extractIntFromStart(a);
			if (checkITA3Char(o)==true)	{
				int c=retITA3Val(o);
				sb.append("0x"+Integer.toHexString(c)+" ");
			}
			else	{
				sb.append("<ERROR> ");
			}
		}
		return sb.toString();
	}
	
	// Extract an integer from the start buffer
	private int extractIntFromStart (int pos)	{
		int v=0;
		if (startBuffer[pos]==true) v=64;
		if (startBuffer[pos+1]==true) v=v+32;
		if (startBuffer[pos+2]==true) v=v+16;
		if (startBuffer[pos+3]==true) v=v+8;
		if (startBuffer[pos+4]==true) v=v+4;
		if (startBuffer[pos+5]==true) v=v+2;
		if (startBuffer[pos+6]==true) v++;
		return v;
	}
	
	
}
