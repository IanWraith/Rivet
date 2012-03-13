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
	private int totalErrorCount=0;
	private int highBin;
	private int lowBin;
	private int b7Count;
	private int countSinceSync;
	private String startLine;
	private String syncLine;
	private String sessionLine;
	
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
			samplesPerSymbol36=samplesPerSymbol(36.0,waveData.getSampleRate());
			samplesPerSymbol50=samplesPerSymbol(50.0,waveData.getSampleRate());
			state=1;
			lineBuffer.delete(0,lineBuffer.length());
			syncState=0;
			buffer7=0;
			buffer21=0;
			characterCount=0;
			syncBufferCounter=0;
			theApp.setStatusLabel("Sync Hunt");
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
				// Jump the next stage to acquire symbol timing
				state=2;
				totalErrorCount=0;
				totalCharacterCount=0;
				syncState=1;
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
				if (b7Count==4)	{
					buffer7=buffer7&0xF;
					// Look for 0101 (5) or 1010 (10)
					if ((buffer7==5)||(buffer7==10))	{
						state=3;
						if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CIS 36-50 50 baud sync sequence found";
						b7Count=0;
						countSinceSync=0;
						theApp.setStatusLabel("50 Baud Sync Found");
					}
					else state=0;
				}
			}
		}
			
		// Read in symbols
		if (state==3)	{
			// Only demodulate a bit every samplesPerSymbol50 samples
			if (symbolCounter>=(long)samplesPerSymbol50)	{		
				// Demodulate a single bit
				boolean bit=getSymbolFreqBin(circBuf,waveData,0);
				// Increment the count since sync
				countSinceSync++;
				// If no sync work has been found in 500 bits then go back to hunting
				if ((syncState==1)&&(countSinceSync==500))	{
					state=1;
					if (theApp.isDebug()==true) outLines[0]=theApp.getTimeStamp()+" CIS 36-50 50 baud sync timeout";
				}
				if (theApp.isDebug()==false)	{
					if (syncState==1)	{
						addToSyncBuffer(bit);
						// Check if the sync buffer holds a valid sync word
						if (syncValidCheck()==true)	{
							syncState=2;
							theApp.setStatusLabel("Decoding Message");
							startLine=theApp.getTimeStamp()+" Message Start";
							long header=syncBufferAsLong();
							syncLine="Sync 0x"+Long.toHexString(header);
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
								sessionLine="Session Key is ";
								int a;
								for (a=0;a<10;a++)	{
									// Check the session key is made up of valid ITA3 numbers 
									if (checkITA3Char(key1[a])==true)	{
										int c=retITA3Val(key1[a]);
										sessionLine=sessionLine+"0x"+Integer.toHexString(c)+" ";
									}
									else	{
										sessionLine=sessionLine+"<ERROR> ";
									}
								}
								// Both keys should be the same
								if (!Arrays.equals(key1,key2))	{
									state=1;
								}
								else	{
									outLines[0]=startLine;
									outLines[1]=syncLine;
									outLines[2]=sessionLine;
									theApp.setStatusLabel("Incoming message");
								}
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
						double err=((double)totalErrorCount/(double)totalCharacterCount)*100.0;
						outLines[0]="End of Message ("+Integer.toString(totalCharacterCount)+" characters in this message "+Double.toString(err)+"% of these contained errors)";
						syncBufferCounter=0;
						state=2;
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
		int fr=do80FFT(circBuf,waveData,start);
		return fr;
	}
	
	// Return the symbol frequency given the bins that hold the possible tones
	private boolean getSymbolFreqBin (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		boolean bit;
		double early[]=do80FFTBinRequest(circBuf,waveData,start,lowBin,highBin);
		double earlyE=getComponentDC();
		start=start+((int)samplesPerSymbol50/2);
		double late[]=do80FFTBinRequest(circBuf,waveData,start,lowBin,highBin);
		double lateE=getComponentDC();
		// Set the symbolCounter value from the early/late gate value
		symbolCounter=Comparator(earlyE,lateE,25.0);
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
			int shift=highTone-lowTone;
			// Check for an incorrect shift
			if ((shift>300)||(shift<150)) return false;
			return true;
		}
		return false;
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
		int shift=highTone-lowTone;
		// Check for an incorrect shift
		if ((shift>300)||(shift<100)) return false;
		return true;
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
