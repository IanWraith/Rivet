package org.e2k;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import javax.swing.SwingUtilities;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;

public class Rivet {

	private DisplayModel display_model;
	private DisplayView display_view;
	private static Rivet theApp;
	private static DisplayFrame window;
	public String program_version="Rivet (Build 0) by Ian Wraith";
	public int vertical_scrollbar_value=0;
	public int horizontal_scrollbar_value=0;
	public boolean pReady=false;
	private int system=0;
	private WaveData waveData=new WaveData();
	private int[] grabInt=new int[1024];
	private int countLoad;
	private CircularDataBuffer circBuffer=new CircularDataBuffer();
	
	public static void main(String[] args) {
		theApp=new Rivet();
		SwingUtilities.invokeLater(new Runnable(){public void run(){theApp.createGUI();}});

	}
	
	// Setup the window //
	public void createGUI() {
		window=new DisplayFrame(program_version,this);
		Toolkit theKit=window.getToolkit();
		Dimension wndsize=theKit.getScreenSize();
		window.setBounds(wndsize.width/6,wndsize.height/6,2*wndsize.width/3,2*wndsize.height/3);
		window.addWindowListener(new WindowHandler());
		display_model=new DisplayModel();
		display_view=new DisplayView(this);
		display_model.addObserver(display_view);
		window.getContentPane().add(display_view,BorderLayout.CENTER);
		window.setVisible(true);
		// Make certain the program knows the GUI is ready
		pReady=true;
		}

	class WindowHandler extends WindowAdapter {
		public void windowClosing(WindowEvent e) {	
			}
		}

	public DisplayFrame getWindow()	{
		return window;	
		}

	public DisplayModel getModel() {
		return display_model;
		}

	public DisplayView getView() {
		return display_view;	
		}

	public void setSystem(int system) {
		this.system = system;
	}

	public int getSystem() {
		return system;
	}
	
	public boolean isCROWD36()	{
		if (system==0) return true;
		else return false;
	}
	
	public boolean isXPA()	{
		if (system==1) return true;
		else return false;
	}
	
	public boolean isXPA2()	{
		if (system==2) return true;
		else return false;
	}
	
	// Loads in a .WAV file
	public void loadWAVfile(String fileName)	{
		int a;
		boolean wavCom=true;
		long wavSize=0;
		try	{
			File wavFile=new File(fileName);
			AudioInputStream audioInputStream=AudioSystem.getAudioInputStream(wavFile);  
	    	waveData.bytesPerFrame=audioInputStream.getFormat().getFrameSize();
	    	waveData.sampleRate=audioInputStream.getFormat().getSampleRate();
	    	waveData.sampleSizeInBits=audioInputStream.getFormat().getSampleSizeInBits();
	    	waveData.channels=audioInputStream.getFormat().getChannels();
	    	waveData.endian=audioInputStream.getFormat().isBigEndian();
	    	// Keep grabbing from the WAV file until it has all been loaded
	    	while (wavCom==true)	{
	    		wavCom=grabWavBlock(audioInputStream);
	    		for (a=0;a<countLoad;a++)	{
	    			circBuffer.addToCircBuffer(grabInt[a]);
		    		wavSize++;
	    		}
	    	}
		}
		catch (Exception e)	{
			
		}
	
	}
	
	// Read in an int from a wav file
	private boolean grabWavBlock (AudioInputStream audioStream) {
	    // Decide how to handle the WAV data
	    // 16 bit LE
		if ((audioStream.getFormat().isBigEndian()==false)&&(audioStream.getFormat().getSampleSizeInBits()==16))	{
			return grabWavBlock16LE (audioStream); 
		}
	    // 8 bit LE
	    else if ((audioStream.getFormat().isBigEndian()==false)&&(audioStream.getFormat().getSampleSizeInBits()==8))	{
	    	return grabWavBlock8LE (audioStream); 
	    }
	    else return false;
	  }
	
	private boolean grabWavBlock16LE (AudioInputStream audioStream)	{
		int a,i=0;
		byte inBlock[]=new byte[2048];
		try	{
		    countLoad=audioStream.read(inBlock);
		    for (a=0;a<countLoad;a=a+2)	{
		    	grabInt[i]=LEconv16(inBlock[a],inBlock[a+1]);
		    	i++;
		    }
		   }
		   catch (Exception e)	{
			countLoad=i;
		    return false;
		   }
		 countLoad=i;
		 if (countLoad<1024) return false;
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
		byte inBlock[]=new byte[1024];
		int a;
		try	{
			countLoad=audioStream.read(inBlock);
			for (a=0;a<countLoad;a++)	{
				grabInt[a]=LEconv8(inBlock[a]);
			}
		}
		catch (Exception e)	{
			return false;
		}
		if (countLoad<1024) return false;
		 else return true;
	  }
	
	
	
}
