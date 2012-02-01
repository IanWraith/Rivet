package org.e2k;

import java.util.Arrays;
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
	private double samplesPerSymbol36;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	public StringBuffer lineBuffer=new StringBuffer();
	private int highTone;
	private int lowTone;
	
	private int highBin;
	private int lowBin;
	private int timingCount;
	
	private double lowestDif;
	private int lowestPoint;
	
	private int centre;
	private long syncFoundPoint;
	private int syncState;
	private int buffer7=0;
	private int buffer21=0;
	private int characterCount;
	private int startCount;
	private int keyCount;
	private int key1[]=new int[10];
	private int key2[]=new int[10];
	private boolean syncBuffer[]=new boolean[44];
	private int syncBufferCounter=0;
	private final int ITA3VALS[]={26,25,76,28,56,19,97,82,112,35,11,98,97,84,70,74,13,100,42,69,50,73,37,22,21,49,67,88,14,38,104,7,52,41,44,81};
	private final String ITA3LETS[]={"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","<cr>","<lf>","<let>","<fig>"," ","<unperf>","<Request>","<Idle a>","<Idle b>","<0x51>"}; 
	private int totalCharacterCount=0;
	
	public CIS3650 (Rivet tapp)	{
		theApp=tapp;
	}
	
	// The main decode routine
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()>11025.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCIS 36-50 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			samplesPerSymbol36=samplesPerSymbol(36.0,waveData.getSampleRate());
			samplesPerSymbol50=samplesPerSymbol(50.0,waveData.getSampleRate());
			
			state=1;
			lineBuffer.delete(0,lineBuffer.length());
			syncState=0;
			buffer7=0;
			buffer21=0;
			characterCount=0;
			syncBufferCounter=0;
			timingCount=0;
			return null;
		}
		
		
		// Look for a 36 baud or a 50 baud alternating sequence
		if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return null;
			// Look for a 36 baud alternating sync sequence
			if ((syncState==0)&&(detect36Sync(circBuf,waveData)==true))	{
				outLines[0]=theApp.getTimeStamp()+" CIS 36-50 36 baud sync sequence found";
				syncState=1;
				return outLines;
			}
			// Look for a 50 baud alternating sync sequence
			if (detect50Sync(circBuf,waveData)==true)	{
				outLines[0]=theApp.getTimeStamp()+" CIS 36-50 50 baud sync sequence found";
				// Jump the next stage to acquire symbol timing
				state=3;
				timingCount=0;
				syncState=1;
				lowestDif=32768;
				lowestPoint=0;
				syncFoundPoint=sampleCount;
				return outLines;
			}
		}
			
		// Acquire symbol timing
		if (state==3)	{
			double vals[]=do64FFTBinRequest(circBuf,waveData,0,lowBin,highBin);
			double d;
			if (vals[0]>vals[1]) d=vals[0]-vals[1];
			else d=vals[1]-vals[0];
			
			if (d<lowestDif)	{
				lowestDif=d;
				lowestPoint=timingCount;
			}
			
			//String line=Integer.toString(timingCount)+","+Double.toString(vals[0])+","+Double.toString(vals[1])+","+Double.toString(d);
			//theApp.debugDump(line);

			timingCount++;
			sampleCount++;
			symbolCounter++;
			// Gather two symbols worth of energy values
			if (timingCount<(int)(samplesPerSymbol50*1)) return null;
			long perfectPoint=lowestPoint+syncFoundPoint+(int)samplesPerSymbol50;
			long samplesUntil=perfectPoint-sampleCount;
			// Calculate what the value of the symbol counter should be
			symbolCounter=(int)samplesPerSymbol50-samplesUntil;
			
			symbolCounter=symbolCounter+((int)samplesPerSymbol50/4);
			
			theApp.debugDump("##");
			
			
			
			state=4;
		}
		
		// Read in symbols
		if (state==4)	{
			if (symbolCounter>=(long)samplesPerSymbol50)	{
				
				
				double vals[]=do64FFTBinRequest(circBuf,waveData,0,lowBin,highBin);
				double d;
				if (vals[0]>vals[1]) d=vals[0]-vals[1];
				else d=vals[1]-vals[0];
				
				if (d<lowestDif)	{
					lowestDif=d;
					lowestPoint=timingCount;
				}
				
				String line=Double.toString(vals[0])+","+Double.toString(vals[1])+","+Double.toString(d);
				theApp.debugDump(line);

				
				
				symbolCounter=0;		
				boolean bit=getSymbolBit(circBuf,waveData,0);
				if (theApp.isDebug()==false)	{
					if (syncState==1)	{
						addToSyncBuffer(bit);
						// Check if the sync buffer holds a valid sync word
						if (syncValidCheck()==true)	{
							syncState=2;
							outLines[0]="Message Start";
							long header=syncBufferAsLong();
							outLines[1]="Sync 0x"+Long.toHexString(header);
							buffer21=0;
							buffer7=0;
							keyCount=0;
							startCount=0;			
							totalCharacterCount=0;
						}	
					}
					
					// Once we have the 44 bit sync sequence get the two 70 bit keys
					else if (syncState==2)	{
						addToBuffer7(bit);
						startCount++;
						if (startCount==7)	{
							if (keyCount<10) key1[keyCount]=buffer7;
							else key2[keyCount-10]=buffer7;
							if (keyCount==19)	{
								syncState=3;
								outLines[0]="Session Key is ";
								int a;
								for (a=0;a<10;a++)	{
									// Check the session key is made up of valid ITA3 numbers 
									if (checkITA3Char(key1[a])==true)	{
										int c=retITA3Val(key1[a]);
										outLines[0]=outLines[0]+"0x"+Integer.toHexString(c)+" ";
									}
									else	{
										outLines[0]=outLines[0]+"<ERROR> ";
									}
								}
								// Both keys should be the same
								if (!Arrays.equals(key1,key2)) outLines[0]=outLines[0]+" (ERROR SESSION KEY MISMATCH)"; 
							}
							else keyCount++;
							startCount=0;
						}
					}
					// Read in and display the main body of the message
					else if (syncState==3)	{
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
						outLines[0]="End of Message ("+Integer.toString(totalCharacterCount)+" characters in this message)";
						syncBufferCounter=0;
						state=1;
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
	
	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
	

	// Get the frequency at a certain symbol
	private int getSymbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int fr=do64FFT(circBuf,waveData,start);
		return fr;
	}
	
	// Return the bit value for a certain symbol
	private boolean getSymbolBit (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int f=getSymbolFreq(circBuf,waveData,start);
		boolean bit=freqDecision(f,centre,theApp.isInvertSignal());
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
	
	// See if the buffer holds a 36 baud alternating sequence
	private boolean detect36Sync(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol36*1;
		int f1=getSymbolFreq(circBuf,waveData,pos);
		if (f0==f1) return false;
		pos=(int)samplesPerSymbol36*2;
		int f2=getSymbolFreq(circBuf,waveData,pos);
		pos=(int)samplesPerSymbol36*3;
		int f3=getSymbolFreq(circBuf,waveData,pos);
		if (f3!=9999) return false;
		// Look for a 36 baud alternating sequence
		if ((f0==f2)&&(f1==f3)&&(f0!=f1)&&(f2!=f3))	{
			if (f0>f1)	{
				highTone=f0;
				lowTone=f1;
			}
			else	{
				highTone=f1;
				lowTone=f0;
			}
			centre=(highTone+lowTone)/2;
			int shift=highTone-lowTone;
			// Check for an incorrect shift
			if ((shift>300)||(shift<150)) return false;
			return true;
		}
		return false;
	}
	
	// See if the buffer holds a 50 baud alternating sequence
	private boolean detect50Sync(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0,b0,b1,b2,b3;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		b0=getFreqBin();
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol50*1;
		int f1=getSymbolFreq(circBuf,waveData,pos);
		b1=getFreqBin();
		if (f0==f1) return false;
		pos=(int)samplesPerSymbol50*2;
		int f2=getSymbolFreq(circBuf,waveData,pos);
		b2=getFreqBin();
		if (f1==f2) return false;
		pos=(int)samplesPerSymbol50*3;
		int f3=getSymbolFreq(circBuf,waveData,pos);
		b3=getFreqBin();
		// Look for a 50 baud alternating sequence
		if ((f0==f2)&&(f1==f3)&&(f0!=f1)&&(f2!=f3))	{
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
			pos=(int)samplesPerSymbol50*4;
			int f4=getSymbolFreq(circBuf,waveData,pos);
			pos=(int)samplesPerSymbol50*5;
			int f5=getSymbolFreq(circBuf,waveData,pos);
			if ((f3!=f5)||(f2!=f4)) return false;	
			centre=(highTone+lowTone)/2;
			int shift=highTone-lowTone;
			// Check for an incorrect shift
			if ((shift>300)||(shift<150)) return false;
			return true;
		}
	return false;
	}
	
	// Add a bit to the 44 bit sync buffer
	private void addToSyncBuffer (boolean bit)	{
		int a;
		// Move all bits one bit to the left
		for (a=1;a<syncBuffer.length;a++)	{
			syncBuffer[a-1]=syncBuffer[a];
		}
		int last=syncBuffer.length-1;
		syncBuffer[last]=bit;
		syncBufferCounter++;
	}
	
	// Return true if this appears to be a valid sync word
	private boolean syncValidCheck ()	{
		int a,count=0;
		if (syncBufferCounter<(syncBuffer.length-1)) return false;
		for (a=0;a<syncBuffer.length;a++)	{
			if (syncBuffer[a]==true) count++;
		}
		
	
		
		//int mid=syncBufferMiddleAsInt();
		//if ((mid!=0xeb)&&(mid!=14)) return false;
		
		
		// If count is 23 and the first three bits are true this OK but we are inverted
		if ((count==23)&&(syncBuffer[0]==true)&&(syncBuffer[1]==true)&&(syncBuffer[2]==true))	{
			// Change the invert setting
			theApp.changeInvertSetting();
			// Invert the complete sync buffer to reflect the change
			syncBufferInvert();
		
			int mid=syncBufferMiddleAsInt();
			if (mid!=235) return false;
			
			return true;
		}
		// If the count is 21 and the first three bits are false then we are all OK
		else if ((count==21)&&(syncBuffer[0]==false)&&(syncBuffer[1]==false)&&(syncBuffer[2]==false))	{
			
			int mid=syncBufferMiddleAsInt();
			if (mid!=235) return false;
			
			return true;
		}
		// No match
		else return false;
	}
	
	// Return the sync buffer a long
	private long syncBufferAsLong ()	{
		int a,bc=0;
		long r=0;
		for (a=(syncBuffer.length-1);a>=0;a--)	{
			if (syncBuffer[a]==true) r=r+(long)Math.pow(2.0,bc);
			bc++;
		}
		return r;
	}
	
	// Invert the sync buffer
	private void syncBufferInvert ()	{
		int a;
		for (a=0;a<syncBuffer.length;a++)	{
			if (syncBuffer[a]==true) syncBuffer[a]=false;
			else syncBuffer[a]=true;
		}
	}
	
	
	private int syncBufferMiddleAsInt ()	{
		int a,bc=7,r=0;
		for (a=20;a<28;a++)	{
			if (syncBuffer[a]==true) r=r+(int)Math.pow(2.0,bc);
			bc--;
		}
		return r;
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
	
	
}
