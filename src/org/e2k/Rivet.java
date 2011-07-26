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
	public final int MAXCIRC=1024*10;
	public int circBufferCounter=0;
	public int[] circDataBuffer=new int[MAXCIRC];
	private int grabInt;
	
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
	    	// Keep grabbing from the WAV file untill it has all been loaded
	    	while (wavCom==true)	{
	    		wavCom=grabWavBlock(audioInputStream);
	    		addToCircBuffer(grabInt);
	    		wavSize++;
	    	}
		}
		catch (Exception e)	{
			String s=e.toString();
		}
	
		int a=0;
		a++;
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
		byte inBlock[]=new byte[2];
		try	{
		    audioStream.read(inBlock);
		    grabInt=LEconv16(inBlock[0],inBlock[1]);
		   }
		   catch (Exception e)	{
		    return false;
		   }
		 return true;
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
		byte inBlock[]=new byte[1];
		try	{
			audioStream.read(inBlock);
			grabInt=LEconv8(inBlock[0]);
		}
		catch (Exception e)	{
			return false;
		}
	    return true;
	  }
	
	// Add data to the incoming data circular buffer
	private void addToCircBuffer (int i)	{
		circDataBuffer[circBufferCounter]=i;
		circBufferCounter++;
		if (circBufferCounter==MAXCIRC) circBufferCounter=0;
	}
	
}
