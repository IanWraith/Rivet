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

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.PipedInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Rivet {

	private static boolean RUNNING=true;
	private DisplayModel display_model;
	private DisplayView display_view;
	private static Rivet theApp;
	private static DisplayFrame window;
	public String program_version="Rivet (Build 9) by Ian Wraith";
	public int vertical_scrollbar_value=0;
	public int horizontal_scrollbar_value=0;
	public boolean pReady=false;
	private int system=1;
	public final Font plainFont=new Font("SanSerif",Font.PLAIN,12);
	public final Font boldFont=new Font("SanSerif",Font.BOLD,12);
	public final Font italicFont=new Font("SanSerif",Font.ITALIC,12);
    public XPA xpaHandler=new XPA(this,10);	
    public XPA2 xpa2Handler=new XPA2(this);	
    public CROWD36 crowd36Handler=new CROWD36(this,40);	
    public FSK200500 fsk200500Handler=new FSK200500(this,200);
    public InputThread inputThread=new InputThread(this);
    private DataInputStream inPipeData;
	private PipedInputStream inPipe;
	private CircularDataBuffer circBuffer=new CircularDataBuffer();
	private WaveData waveData=new WaveData();
	private boolean logging=false;
	public FileWriter file;
	private boolean debug=false;
	private boolean soundCardInput=false;
	private boolean wavFileLoadOngoing=false;
	
	public final String MODENAMES[]={"CROWD36","XPA (10 Baud)","XPA2","XPA (20 Baud)"};//"FSK200/500"
    
	public static void main(String[] args) {
		theApp=new Rivet();
		SwingUtilities.invokeLater(new Runnable(){public void run(){theApp.createGUI();}});
		// Get data from the sound card thread
		try	{
			// Connected a piped input stream to the piped output stream in the thread
			theApp.inPipe=new PipedInputStream(theApp.inputThread.getPipedWriter());
			// Now connect a data input stream to the piped input stream
			theApp.inPipeData=new DataInputStream(theApp.inPipe);
			}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in main()","Rivet", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
			}
		// The main loop
		while (RUNNING)	{
			if ((theApp.wavFileLoadOngoing==true)&&(theApp.pReady==true)) theApp.getWavData();
			else if ((theApp.inputThread.getAudioReady()==true)&&(theApp.pReady==true)) theApp.getAudioData();
			else	{
				// Add the following so the thread doesn't eat all of the CPU time
				try	{Thread.sleep(1);}
				catch (Exception e)	{JOptionPane.showMessageDialog(null,"Error in main2()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);}
			}
		}
		
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
		this.system=system;
		// 10 Baud XPA
		if (system==1) xpaHandler.setBaudRate(10);
		// 20 Baud XPA
		else if (system==3) xpaHandler.setBaudRate(20);
	}

	public int getSystem() {
		return system;
	}
	
	public boolean isCROWD36()	{
		if (system==0) return true;
		else return false;
	}
	
	public boolean isXPA_10()	{
		if (system==1) return true;
		else return false;
	}
	
	public boolean isXPA_20()	{
		if (system==3) return true;
		else return false;
	}
	
	public boolean isXPA2()	{
		if (system==2) return true;
		else return false;
	}
	
	//public boolean isFSK200500()	{
		//if (system==3) return true;
		//else return false;
	//}
	
	// Tell the input thread to start to load a .WAV file
	public void loadWAVfile(String fileName)	{
		String disp;
		disp=getTimeStamp()+" Loading file "+fileName;
		addLine(disp,Color.BLACK,plainFont);
		waveData=inputThread.startFileLoad(fileName);
		// Make sure the program knows this data is coming from a file
		waveData.setFromFile(true);
		// Clear the data buffer
		circBuffer.setBufferCounter(0);
		// Reset the system objects
		// CROWD36
		if (system==0) crowd36Handler.setState(0);
		// XPA
		else if ((system==1)||(system==3)) xpaHandler.setState(0);
		// XPA2
		else if (system==2) xpa2Handler.setState(0);
		// FSK200/500
		//else if (system==3) fsk200500Handler.setState(0);
		// Ensure the program knows we have a WAV file load ongoing
		wavFileLoadOngoing=true;
	}
	
	// This is called when the input thread is busy getting data from a WAV file
	private void getWavData()	{
		// Get the sample from the input thread
		try	{
			// Add the data from the thread pipe to the circular buffer
			circBuffer.addToCircBuffer(inPipeData.readInt());
			// Process this data
			processData();
    		// Update the progress bar
    		updateProgressBar();
			// Check if the file has now all been read but we need to process all the data in the buffer
			if (inputThread.getLoadingFileState()==false)	{
				int a;
				for (a=0;a<circBuffer.retMax();a++)	{
					processData();
					// Keep adding null data to the circular buffer to move it along
					circBuffer.addToCircBuffer(0);
				}
				// Check if there is anything left to display
				if (system==0)	{
					if (crowd36Handler.getLineCount()>0) addLine(crowd36Handler.getLineBuffer(),Color.BLACK,plainFont);
					addLine(crowd36Handler.lowHighFreqs(),Color.BLACK,plainFont);
				}
				// Once the buffer data has been read we are done
				String disp=getTimeStamp()+" WAV file loaded and analysis complete ("+Long.toString(inputThread.getSampleCounter())+" samples read)";
				addLine(disp,Color.BLACK,plainFont);		
				wavFileLoadOngoing=false;
				}
			}
		catch (Exception e)	{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,"Error in getWavData()","Rivet", JOptionPane.ERROR_MESSAGE);
			}	
	}
	
	// This is called when the input thread is busy getting data from the sound card
	private void getAudioData()	{
			// Get the sample from the input thread
			try	{
				// Add the data from the thread pipe to the circular buffer
				circBuffer.addToCircBuffer(inPipeData.readInt());
				// Process this data
				processData();
	    		// Update the volume bar every 50 samples
	    		if (inputThread.getSampleCounter()%50==0) updateVolumeBar();
				}
			catch (Exception e)	{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null,"Error in getAudioData()","Rivet", JOptionPane.ERROR_MESSAGE);
				}	
		}
	
			
	// A central data processing class
	private void processData ()	{		
		try	{
			int a;
			String outLines[]=new String[2];
			// CROWD36
			if (system==0) outLines=crowd36Handler.decode(circBuffer,waveData);
			// XPA
			else if ((system==1)||(system==3)) outLines=xpaHandler.decode(circBuffer,waveData);
			// XPA2
			else if (system==2)	outLines=xpa2Handler.decode(circBuffer,waveData);
			// FSK200/500
			//else if (system==3)	outLines=fsk200500Handler.decode(circBuffer,waveData);
			// Return if nothing at all has been returned
			if (outLines==null) return;
			// Display the decode objects output
			for (a=0;a<outLines.length;a++)	{
				// If there is a line to display then show it
				if (outLines[a]!=null)	addLine(outLines[a],Color.BLACK,plainFont);
			}
		}
		catch (Exception e){
			StringWriter sw=new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String str=sw.toString();
			JOptionPane.showMessageDialog(null,"Error in processData()\n"+str,"Rivet", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	// Write a line to the debug file
	public void debugDump (String line)	{
	    try	{
	    	FileWriter dfile=new FileWriter("debug.csv",true);
	    	dfile.write(line);
	    	dfile.write("\r\n");
	    	dfile.flush();  
	    	dfile.close();
	    	}catch (Exception e)	{
	    		System.err.println("Error: " + e.getMessage());
	    		}
		}
	
	// Return a time stamp
	public String getTimeStamp() {
		Date now=new Date();
		DateFormat df=DateFormat.getTimeInstance();
		return df.format(now);
	}
	
	private void updateProgressBar ()	{
		window.progressBarUpdate(inputThread.returnFileLoadPercentage());
	}
	
	private void updateVolumeBar ()	{
		// Calculate as a percentage of 18000 (the max value)
		int pval=(int)(((float)inputThread.returnVolumeAverage()/(float)18000.0)*(float)100);
		window.progressBarUpdate(pval);
	}
	
	public void setStatusLabel (String st)	{
		window.setStatusLabel(st);
	}

	public void setLogging(boolean logging) {
		this.logging = logging;
	}

	public boolean getLogging() {
		return logging;
	}
	
	// Write to a string to the logging file
	public boolean fileWrite(String fline) {
		// Add a CR to the end of each line
		fline=fline+"\r\n";
		try {
			file.write(fline);
			file.flush();
		} catch (Exception e) {
			// Stop logging as we have a problem
			logging=false;
			JOptionPane.showMessageDialog(null,"Error writing to the log file.\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	// Adds a line to the display
	public void addLine(String line,Color col,Font font) {
		if (logging==true) fileWrite(line);
		display_view.add_line(line,col,font);
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isSoundCardInput() {
		return soundCardInput;
	}

	public void setSoundCardInput(boolean s) {
		// If the soundcard is already running we need to close it
		if (this.soundCardInput==true)	{
			// Try to close the audio device
			if (inputThread.closeAudio()==true) this.soundCardInput=false;
		}
		else	{
			// CROWD36
			if (system==0)	{
				WaveData waveSetting=new WaveData();
				waveSetting.setChannels(1);
				waveSetting.setEndian(true);
				waveSetting.setSampleSizeInBits(16);
				waveSetting.setFromFile(false);
				waveSetting.setSampleRate(8000.0);
				waveSetting.setBytesPerFrame(2);
				inputThread.setupAudio(waveSetting); 
				waveData=waveSetting;
				this.soundCardInput=true;	
			}
			// XPA or XPA2
			else if ((system==1)||(system==2)||(system==3))	{
				WaveData waveSetting=new WaveData();
				waveSetting.setChannels(1);
				waveSetting.setEndian(true);
				waveSetting.setSampleSizeInBits(16);
				waveSetting.setFromFile(false);
				waveSetting.setSampleRate(11025.0);
				waveSetting.setBytesPerFrame(2);
				inputThread.setupAudio(waveSetting); 
				waveData=waveSetting;
				this.soundCardInput=true;	
			}	
			// FSK200/500
			else if (system==4)	{
				WaveData waveSetting=new WaveData();
				waveSetting.setChannels(1);
				waveSetting.setEndian(true);
				waveSetting.setSampleSizeInBits(16);
				waveSetting.setFromFile(false);
				waveSetting.setSampleRate(11025.0);
				waveSetting.setBytesPerFrame(2);
				inputThread.setupAudio(waveSetting); 
				waveData=waveSetting;
				this.soundCardInput=true;	
			}	
		}
	}
	
	public void setSoundCardInputOnly(boolean s)	{
		this.soundCardInput=s;
	}
	
	// Reset the decoder state
	public void resetDecoderState()	{
		// CROWD36
		if (system==0) crowd36Handler.setState(0);
		// XPA
		else if ((system==1)||(system==3)) xpaHandler.setState(0);
		// XPA2
		else if (system==2)	xpa2Handler.setState(0);
		// FSK200/500
		//else if (system==3)	fsk200500Handler.setState(0);
	}
	
}
