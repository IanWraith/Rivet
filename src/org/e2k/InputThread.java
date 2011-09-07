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
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PipedOutputStream;

public class InputThread extends Thread {
	
	private boolean run;
	private boolean audioReady;
	private boolean gettingAudio;
	private boolean loadingFile;
	private PipedOutputStream ps=new PipedOutputStream();
	private DataOutputStream outPipe=new DataOutputStream(ps);
	private File wavFile;
	private long fileSize;
	private AudioInputStream audioInputStream;
	public final int CHUNK_SIZE=32768;
    private long fileCounter;
    private String errorCause="None";
    private long sampleCounter=0;
   
	public InputThread (Rivet TtheApp) {
    	run=false;
    	audioReady=false;
    	gettingAudio=false;
    	loadingFile=false;
    	setPriority(Thread.MIN_PRIORITY);
        start();
        Thread.yield();
      }
    
    // Main
    public void run()	{
    	// Run continuously
    	for (;;)	{
    		// If it hasn't been already then setup the audio device
    		//if (audioReady==false) setupAudio();
    		// If the audio device is ready , the program wants to and we aren't already then
    		// get data from the audio device.
    		//if ((audioReady==true)&&(run==true)&&(gettingAudio==false)) getSample();
    		if (loadingFile==true) getFileData();
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
			waveData.bytesPerFrame=audioInputStream.getFormat().getFrameSize();
	    	waveData.sampleRate=audioInputStream.getFormat().getSampleRate();
	    	waveData.sampleSizeInBits=audioInputStream.getFormat().getSampleSizeInBits();
	    	waveData.channels=audioInputStream.getFormat().getChannels();
	    	waveData.endian=audioInputStream.getFormat().isBigEndian();
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
    		loadingFile=false;
    		try	{
    			// Flush anything remaining in the outpipe
    			outPipe.flush();
    			// Close the audio stream
    			audioInputStream.close();
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
    
}
