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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JOptionPane;
import java.io.DataOutputStream;
import java.io.File;
import java.io.PipedOutputStream;

public class InputThread extends Thread {
	private boolean audioReady;
	private boolean gettingAudio;
	private boolean loadingFile;
	private PipedOutputStream ps=new PipedOutputStream();
	private DataOutputStream outPipe=new DataOutputStream(ps);
	private File wavFile;
	private long fileSize;
	public final int CHUNK_SIZE=32768;
    private long fileCounter;
    private String errorCause="None";
    private long sampleCounter=0;
    private static int VOLUMEBUFFERSIZE=100;
	private int volumeBuffer[]=new int[VOLUMEBUFFERSIZE];
	private int volumeBufferCounter=0;
	private static int ISIZE=4096;
	private byte buffer[]=new byte[ISIZE+1];
	private int inputLevel=0;
	private Rivet theApp; 
	private AudioMixer audioMixer;
	private AudioInputStream audioInputStream;
	
 
	public InputThread (Rivet TtheApp) {
		audioMixer=new AudioMixer();
    	audioReady=false;
    	gettingAudio=false;
    	loadingFile=false;
    	theApp=TtheApp;
    	setPriority(Thread.MIN_PRIORITY);
        start();
        Thread.yield();
      }
    
    // Main
    public void run()	{
    	// Run continuously
    	for (;;)	{
    		// If the audio device is ready , the program wants to and we aren't already then
    		// get data from the audio device.
    		if ((audioReady==true)&&(loadingFile==false)&&(gettingAudio==false)) getSample();
    		else if (loadingFile==true) getFileData();
    		// Add the following so the thread doesn't eat all of the CPU time
    		else	{
    			try	{sleep(1);}
    			catch (Exception e)	{JOptionPane.showMessageDialog(null,"Error in run()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);}
    			}
    		}
    }
    
    public WaveData startFileLoad (String fileName)	{
    	WaveData waveData=new WaveData();
    	try	{
    		wavFile=new File(fileName);
    		fileSize=wavFile.length();
    		fileCounter=0;
    		sampleCounter=0;
			audioInputStream=AudioSystem.getAudioInputStream(wavFile);  
			waveData.setBytesPerFrame(audioInputStream.getFormat().getFrameSize());
	    	waveData.setSampleRate(audioInputStream.getFormat().getSampleRate());
	    	waveData.setSampleSizeInBits(audioInputStream.getFormat().getSampleSizeInBits());
	    	waveData.setChannels(audioInputStream.getFormat().getChannels());
	    	waveData.setEndian(audioInputStream.getFormat().isBigEndian());
	    	theApp.setSoundCardInputOnly(false);
    		loadingFile=true;
    	}
    	catch (Exception e)	{
    		JOptionPane.showMessageDialog(null,"Error in startFileLoad()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
    		return null;
    	}
    	return waveData;
    }
    
    private boolean getFileData ()	{
    	// Load the .WAV file until it has all been read
    	if (grabWavBlock()==false)	{
    		try	{
    			// Close the audio stream
    			audioInputStream.close();
    			// Make sure the program knows the WAV file load operation is over
    			loadingFile=false;
    		}
    		catch (Exception e)	{
    			errorCause=e.toString();
    			JOptionPane.showMessageDialog(null,"Error in getFileData()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
    			return false;
    		}
    	}
    	return true;
    }
    
    // Read in an int from a wav file
	private boolean grabWavBlock () {
	    // Decide how to handle the WAV data
	    // 16 bit LE
		if ((audioInputStream.getFormat().isBigEndian()==false)&&(audioInputStream.getFormat().getSampleSizeInBits()==16))	{
			return grabWavBlock16LE (audioInputStream); 
		}
	    // 8 bit LE
	    else if ((audioInputStream.getFormat().isBigEndian()==false)&&(audioInputStream.getFormat().getSampleSizeInBits()==8))	{
	    	return grabWavBlock8LE (audioInputStream); 
	    }
	    else return false;
	  }
	
	// Handle 16 bit little endian WAV files
	private boolean grabWavBlock16LE (AudioInputStream audioStream)	{
		int a,countLoad;
		byte inBlock[]=new byte[CHUNK_SIZE];
		try	{
		    countLoad=audioStream.read(inBlock);
		    for (a=0;a<countLoad;a=a+2)	{
		    	outPipe.writeInt(LEconv16(inBlock[a],inBlock[a+1]));
		    	fileCounter=fileCounter+2;
		    	sampleCounter++;
		    }
		   }
		   catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in grabWavBlock16LE()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
		    return false;
		   } 
		 if (countLoad<CHUNK_SIZE) return false;
		 else return true;
		 }

	// Convert a 16 bit value from being little endian
	private int LEconv16 (Byte a,Byte b)	{
		return (a&0xFF|b<<8);
	  }
	
	// Convert an 8 bit Java Byte to an Integer
	private int LEconv8 (Byte a)	{
	    return ((a&0xff)-128);
	  }
	
	// Handle 8 bit LE WAV files
	private boolean grabWavBlock8LE (AudioInputStream audioStream)	{
		byte inBlock[]=new byte[CHUNK_SIZE];
		int a,countLoad;
		try	{
			countLoad=audioStream.read(inBlock);
			for (a=0;a<countLoad;a++)	{
				outPipe.writeInt(LEconv8(inBlock[a]));
				fileCounter++;
				sampleCounter++;
			}
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in grabWavBlock8LE()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (countLoad<CHUNK_SIZE) return false;
		 else return true;
	  }
	
	// Return the PipedOutputSteam object so it can be connected to
    public PipedOutputStream getPipedWriter() {
        return this.ps;
      }
    
    public boolean getLoadingFileState()	{
    	return this.loadingFile;
    }
    
    public int returnFileLoadPercentage()	{
    	double percentage=((double)fileCounter/(double)fileSize)*100.0;
    	return (int)percentage;
    }
    
    public String getErrorCause ()	{
    	return this.errorCause;
    }
    
    // Allow the main thread to stop the file reading
    public boolean stopReadingFile ()	{
    	loadingFile=false;
		try	{
			// Close the audio stream
			audioInputStream.close();
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in stopReadingFile()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
    }
    
    // Return the sample counter
    public long getSampleCounter()	{
    	return this.sampleCounter;
    }
    
    // Set the input level
    public void setInputLevel (int il)	{
    	this.inputLevel=il;
    }
    
    // Setup the input audio device
    public void setupAudio (WaveData waveData)	{
		  try {
			  // If the audio is already setup then close it
			  if (audioReady==true)	{
				  closeAudio(); 
				  return;
			  }
			  sampleCounter=0;
			  // If it is running stop the audio so it can be changed
			  audioMixer.stopAudio();
			  // Sample according to the the WaveData objects parameters
			  AudioFormat format=new AudioFormat((int)waveData.getSampleRate(),waveData.getSampleSizeInBits(),waveData.getChannels(),true,waveData.isEndian());
			  audioMixer.setAudioFormat(format);
			  audioMixer.setDefaultLine();
			  audioMixer.openLine();
			  audioMixer.line.start();
			  audioReady=true;
			  loadingFile=false;
		  } catch (Exception e) {
			  String err="Fatal error in setupAudio()\n"+e.getMessage();
			  JOptionPane.showMessageDialog(null,err,"Rivet",JOptionPane.ERROR_MESSAGE);
			  System.exit(0);
	   		}
     }
    
    // Close the audio device
    public boolean closeAudio ()	{
    	try	{
    		audioMixer.line.close();
    		audioReady=false;
    		return true;
    	}
    	catch (Exception e)	{
    		String err="Error in closeAudio()\n"+e.getMessage();
			JOptionPane.showMessageDialog(null,err,"Rivet",JOptionPane.ERROR_MESSAGE);
			return false;
    	}
    }
    
    // Add this sample to the circular volume buffer
    private void addToVolumeBuffer (int tsample)	{
    	volumeBuffer[volumeBufferCounter]=tsample;
    	volumeBufferCounter++;
    	if (volumeBufferCounter==VOLUMEBUFFERSIZE) volumeBufferCounter=0;
    }
    
    // Return the average volume over the last VOLUMEBUFFERSIZE samples
    public int returnVolumeAverage ()	{
    	long va=0;
    	int a,volumeAverage=0;
    	for (a=0;a<VOLUMEBUFFERSIZE;a++)	{
    		va=va+Math.abs(volumeBuffer[a]);
    	}
    	volumeAverage=(int)va/VOLUMEBUFFERSIZE;	
    	return volumeAverage;
    }
    
    // Read in 2 bytes from the audio source combine them together into a single integer
    // then write that into the sound buffer
    private void getSample ()	{
    	// Tell the main thread we getting audio
    	gettingAudio=true;
    	int a,sample,count,total=0;
    	// READ in ISIZE bytes and convert them into ISIZE/2 integers
    	// Doing it this way reduces CPU loading
		try	{
				while (total<ISIZE)	{
					count=audioMixer.line.read(buffer,0,ISIZE);				
					total=total+count;
			  		}
			  	} catch (Exception e)	{
			  		String err=e.getMessage();
			  		JOptionPane.showMessageDialog(null,err,"Rivet", JOptionPane.ERROR_MESSAGE);
			  	}
		// Get the required number of samples
		for (a=0;a<ISIZE;a=a+2)	{
			sample=(buffer[a]<<8)+buffer[a+1];
			// If inputLevel is positive then multiply the sample with it
			// If it is negatibe then divide the sample by it
			if (inputLevel>0) sample=sample*inputLevel;
			else if (inputLevel<0) sample=sample/Math.abs(inputLevel);
			// Add this sample to the circular volume buffer
			addToVolumeBuffer(sample);
			try		{
				// Put the sample into the output pipe
				outPipe.writeInt(sample);
				sampleCounter++;
				}
				catch (Exception e)	{
					String err=e.getMessage();
					JOptionPane.showMessageDialog(null,err,"Rivet", JOptionPane.ERROR_MESSAGE);
				}
		}
		// The the main thread we have stopped fetching audio
		gettingAudio=false;	
    }
    
    // Tell the main program if the audio interface is setup
    public boolean getAudioReady()	{
    	return this.audioReady;
    }
    
	public boolean changeMixer(String mixerName)	{
		return audioMixer.changeMixer(mixerName);
	}   
    
	public String getMixerName()	{
		return audioMixer.getMixer().getMixerInfo().getName();
	}
	
	public String getMixerErrorMessage() {
		return audioMixer.getErrorMsg();
	}    
	

	
    
}
