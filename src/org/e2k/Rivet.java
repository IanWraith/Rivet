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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Rivet {

	private static boolean RUNNING=true;
	private DisplayModel display_model;
	private DisplayView display_view;
	private static Rivet theApp;
	private static DisplayFrame window;
	public String program_version="Rivet (Build 66) by Ian Wraith";
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
    public FSK2001000 fsk2001000Handler=new FSK2001000(this,200);
    public CIS3650 cis3650Handler=new CIS3650(this);
    public CCIR493 ccir493Handler=new CCIR493(this);
    public RTTY rttyHandler=new RTTY(this);
    public GW gwHandler=new GW(this);
    public FSKraw fskHandler=new FSKraw(this);
    public RDFT rdftHandler=new RDFT(this);
    public AT3x04 at3x04Handler=new AT3x04(this);
    public InputThread inputThread=new InputThread(this);
    private DataInputStream inPipeData;
	private PipedInputStream inPipe;
	private CircularDataBuffer circBuffer=new CircularDataBuffer();
	private WaveData waveData=new WaveData();
	private boolean logging=false;
	public FileWriter file;
	public FileWriter bitStreamFile;
	private boolean debug=false;
	private boolean soundCardInput=false;
	private boolean wavFileLoadOngoing=false;
	private boolean invertSignal=false;
	private int soundCardInputLevel=0;
	private boolean soundCardInputTemp;
	private boolean bitStreamOut=false;
	private boolean viewGWChannelMarkers=true;
	private int bitStreamOutCount=0;
	private List<Trigger> listTriggers=new ArrayList<Trigger>();
	private int activeTriggerCount=0;
	private boolean pauseDisplay=false;
	private boolean autoScroll=true;
	private long lastUserScroll=0;
	
	// Mode names
	public final String MODENAMES[]={
			"CROWD36",
			"XPA (10 Baud)",
			"XPA2",
			"XPA (20 Baud)",
			"Experimental",
			"CIS 36-50",
			"FSK200/500",
			"CCIR493-4",
			"FSK200/1000",
			"GW FSK (100 Baud)",
			"Baudot",
			"FSK (Raw)",
			"RDFT"
			};
    
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
		// Calculate the screen position and size in the form x,y,width,height
		int x=wndsize.width/6;
		int y=wndsize.height/6;
		int width=2*wndsize.width/3;
		int height=2*wndsize.height/3;
		window.setBounds(x,y,width,height);
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
	
	public boolean isExperimental()	{
		if (system==4) return true;
		else return false;
	}
	
	public boolean isCIS3650()	{
		if (system==5) return true;
		else return false;
	}
	
	public boolean isFSK200500()	{
		if (system==6) return true;
		else return false;
		}
	
	public boolean isCCIR493()	{
		if (system==7) return true;
		else return false;
		}
	
	public boolean isFSK2001000()	{
		if (system==8) return true;
		else return false;
		}	
	
	public boolean isGW()	{
		if (system==9) return true;
		else return false;
	}
	
	public boolean isRTTY()	{
		if (system==10) return true;
		else return false;
	}
	
	public boolean isFSK()	{
		if (system==11) return true;
		else return false;
	}
	
	public boolean isRDFT()	{
		if (system==12) return true;
		else return false;
	}
		
	// Tell the input thread to start to load a .WAV file
	public void loadWAVfile(String fileName)	{
		String disp;
		disp=getTimeStamp()+" Loading file "+fileName;
		writeLine(disp,Color.BLACK,italicFont);
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
		// CIS36-50
		else if (system==5) cis3650Handler.setState(0);
		// FSK200/500
		else if (system==6) fsk200500Handler.setState(0);
		// CCIR493-4
		else if (system==7) ccir493Handler.setState(0);
		// FSK200/1000
		else if (system==8) fsk2001000Handler.setState(0);	
		// GW
		else if (system==9) gwHandler.setState(0);
		// RTTY
		else if (system==10) rttyHandler.setState(0);
		// FSK (raw)
		else if (system==11) fskHandler.setState(0);
		// RDFT
		else if (system==12) rdftHandler.setState(0);
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
					//if (crowd36Handler.getLineCount()>0) writeLine(crowd36Handler.getLineBuffer(),Color.BLACK,plainFont);
					writeLine(crowd36Handler.lowHighFreqs(),Color.BLACK,plainFont);
					writeLine(crowd36Handler.toneResults(),Color.BLACK,plainFont);
				}
				else if (system==5)	{
					//writeLine(cis3650Handler.lineBuffer.toString(),Color.BLACK,plainFont);
				}
				else if (system==6)	{
					writeLine(fsk200500Handler.getQuailty(),Color.BLACK,plainFont);
				}
				else if (system==8)	{
					writeLine(fsk2001000Handler.getQuailty(),Color.BLACK,plainFont);
				}
				
				// Once the buffer data has been read we are done
				String disp=getTimeStamp()+" WAV file loaded and analysis complete ("+Long.toString(inputThread.getSampleCounter())+" samples read)";
				writeLine(disp,Color.BLACK,italicFont);		
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
			// CROWD36
			if (system==0) crowd36Handler.decode(circBuffer,waveData);
			// XPA
			else if ((system==1)||(system==3)) xpaHandler.decode(circBuffer,waveData);
			// XPA2
			else if (system==2)	xpa2Handler.decode(circBuffer,waveData);
			// Experimental
			else if (system==4)	at3x04Handler.decode(circBuffer,waveData);
			// CIS36-50
			else if (system==5)	cis3650Handler.decode(circBuffer,waveData);
			// FSK200/500
			else if (system==6)	fsk200500Handler.decode(circBuffer,waveData);
			// CCIR493-4
			else if (system==7)	ccir493Handler.decode(circBuffer,waveData);
			// FSK200/1000
			else if (system==8)	fsk2001000Handler.decode(circBuffer,waveData);
			// GW
			else if (system==9) gwHandler.decode(circBuffer,waveData);
			// RTTY
			else if (system==10) rttyHandler.decode(circBuffer,waveData);
			// FSK (raw)
			else if (system==11) fskHandler.decode(circBuffer,waveData);
			// RDFT
			else if (system==12) rdftHandler.decode(circBuffer,waveData);
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
	
	// Get the average current volume and modify this so it is a number between 0 and 100
	// which is then passed to the progress indicator on the status bar
	private void updateVolumeBar ()	{
		// Calculate as a percentage of 18000 (the max value)
		// Reduce this to 3500 to give a more useful display
		int pval=(int)(((float)inputThread.returnVolumeAverage()/(float)3000.0)*(float)100);
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
	public boolean fileWriteLine(String fline) {
		try {
			file.write(fline);
			file.write("\r\n");
			file.flush();
		} catch (Exception e) {
			// Stop logging as we have a problem
			logging=false;
			JOptionPane.showMessageDialog(null,"Error writing to the log file in fileWriteLine().\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	// Write a line char to the logging file
	public boolean fileWriteChar(String ch) {
		try {
			file.write(ch);
		} catch (Exception e) {
			// Stop logging as we have a problem
			logging=false;
			JOptionPane.showMessageDialog(null,"Error writing to the log file in fileWriteChar().\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}	
	
	// Write a newline to the logging file
	public boolean fileWriteNewline() {
		try {
			file.write("\r\n");
		} catch (Exception e) {
			// Stop logging as we have a problem
			logging=false;
			JOptionPane.showMessageDialog(null,"Error writing to the log file in fileWriteNewline().\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}	
	// Write a string to the bit stream file
	public boolean bitStreamWrite(String fline) {
		try {
			// Have only 80 bits per line
			bitStreamOutCount++;
			if (bitStreamOutCount==80)	{
				fline=fline+"\n";
				bitStreamOutCount=0;
			}
			bitStreamFile.write(fline);
		} catch (Exception e) {
			// We have a problem
			bitStreamOut=false;
			JOptionPane.showMessageDialog(null,"Error writing to the bit stream file.\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
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

	// Change the sound source
	public void setSoundCardInput(boolean s) {
		// Try to close the audio device if it is already in operation
		if (this.soundCardInput==true) inputThread.closeAudio();
		// If the soundcard is already running we need to close it
		if (s==false)	{
			this.soundCardInput=false;
		}
		else	{
			// CROWD36 , XPA , XPA2 , CIS36-50 , FSK200/500 , FSK200/1000 , CCIR493-4 , GW , RTTY , RDFT , Experimental
			if ((system==0)||(system==1)||(system==2)||(system==3)||(system==4)||(system==5)||(system==6)||(system==8)||(system==7)||(system==9)||(system==10)||(system==11)||(system==12))	{
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
		// Experimental
		else if (system==4)	at3x04Handler.setState(0);
		// CIS36-50
		else if (system==5)	cis3650Handler.setState(0);
		// FSK200/500
		else if (system==6)	fsk200500Handler.setState(0);
		// CCIR493-4
		else if (system==7)	ccir493Handler.setState(0);
		// FSK200/1000
		else if (system==8)	fsk2001000Handler.setState(0);
		// GW
		else if (system==9) gwHandler.setState(0);
		// RTTY
		else if (system==10) rttyHandler.setState(0);
		// FSK (raw)
		else if (system==11) fskHandler.setState(0);
		// RDFT
		else if (system==12) rdftHandler.setState(0);
	}
	
	// Gets all the text on the screen and returns it as a string
	public String getAllText()	{
		String all=display_view.getText();
		return all;
	}
	
	// Allows the user to set the CROWD36 high sync tone number
	public void getCROWD36SyncHighTone ()	{
		 // Create a panel that will contain the sync number
		 JPanel panel=new JPanel();
		 // Set JPanel layout using GridLayout
		 panel.setLayout(new GridLayout(2,1));
		 // Create a label with text (Username)
		 JLabel label=new JLabel("High Sync Tone Number (0 to 33)");
		// Create text field that will use to enter the high sync tone
		 JTextField toneField=new JTextField(2);
		 toneField.setText(Integer.toString(crowd36Handler.getSyncHighTone()));
		 panel.add(label);
		 panel.add(toneField);
		 panel.setVisible(true);
		 // Show JOptionPane that will ask user for this information
		 int resp=JOptionPane.showConfirmDialog(window,panel,"Enter the CROWD36 High Sync Tone Number",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		 if (resp==JOptionPane.OK_OPTION)	{
			 String sval=new String (toneField.getText());
			 crowd36Handler.setSyncHighTone(Integer.parseInt(sval));
		 }	
	}
	
	// A dialog box to allow the user to set the FSK and RTTY options
	public void setRTTYOptions()	{
		 // Create a panel that contains the FSK and RTTY options
		 JPanel panel=new JPanel();
		 // Set JPanel layout using GridLayout
		 panel.setLayout(new GridLayout(26,1));
		 // Baud Rate
		 boolean baud45=false,baud50=false,baud75=false,baud100=false,baud200=false,baud600=false;
		 JLabel labelBaud=new JLabel("Baud Rate");	
		 if (rttyHandler.getBaudRate()==45.45) baud45=true;
		 else if (rttyHandler.getBaudRate()==50) baud50=true;
		 else if (rttyHandler.getBaudRate()==75) baud75=true;
		 else if (rttyHandler.getBaudRate()==100) baud100=true;
		 else if (rttyHandler.getBaudRate()==200) baud200=true;
		 else if (rttyHandler.getBaudRate()==600) baud600=true;
		 JRadioButton button45=new JRadioButton("45.45 Baud",baud45);
		 JRadioButton button50=new JRadioButton("50 Baud",baud50);
		 JRadioButton button75=new JRadioButton("75 Baud",baud75);
		 JRadioButton button100=new JRadioButton("100 Baud",baud100);
		 JRadioButton button200=new JRadioButton("200 Baud",baud200);
		 JRadioButton button600=new JRadioButton("600 Baud",baud600);
		 ButtonGroup baudGroup=new ButtonGroup();
		 baudGroup.add(button45);
		 baudGroup.add(button50);
		 baudGroup.add(button75);
		 baudGroup.add(button100);
		 baudGroup.add(button200);
		 baudGroup.add(button600);
		 // Shift 
		 boolean shift170=false,shift425=false,shift450=false,shift500=false,shift850=false;
		 boolean shift75=false,shift150=false,shift200=false,shift250=false,shift400=false,shift600=false,shift625=false,shift1000=false;
		 JLabel labelShift=new JLabel("Shift");		
		 if (rttyHandler.getShift()==75) shift75=true;
		 else if (rttyHandler.getShift()==150) shift150=true;
		 else if (rttyHandler.getShift()==170) shift170=true;
		 else if (rttyHandler.getShift()==200) shift200=true;
		 else if (rttyHandler.getShift()==250) shift250=true;
		 else if (rttyHandler.getShift()==400) shift400=true;
		 else if (rttyHandler.getShift()==425) shift425=true;
		 else if (rttyHandler.getShift()==450) shift450=true;
		 else if (rttyHandler.getShift()==500) shift500=true;
		 else if (rttyHandler.getShift()==600) shift600=true;
		 else if (rttyHandler.getShift()==625) shift625=true;
		 else if (rttyHandler.getShift()==850) shift850=true;
		 else if (rttyHandler.getShift()==1000) shift1000=true;
		 JRadioButton buttons75=new JRadioButton("75 Hz",shift75);
		 JRadioButton buttons150=new JRadioButton("150 Hz",shift150);
		 JRadioButton buttons170=new JRadioButton("170 Hz",shift170);
		 JRadioButton buttons200=new JRadioButton("200 Hz",shift200);
		 JRadioButton buttons250=new JRadioButton("250 Hz",shift250);
		 JRadioButton buttons400=new JRadioButton("400 Hz",shift400);
		 JRadioButton buttons425=new JRadioButton("425 Hz",shift425);
		 JRadioButton buttons450=new JRadioButton("450 Hz",shift450);
		 JRadioButton buttons500=new JRadioButton("500 Hz",shift500);
		 JRadioButton buttons600=new JRadioButton("600 Hz",shift600);
		 JRadioButton buttons625=new JRadioButton("625 Hz",shift625);
		 JRadioButton buttons850=new JRadioButton("850 Hz",shift850);
		 JRadioButton buttons1000=new JRadioButton("1000 Hz",shift1000);
		 ButtonGroup shiftGroup=new ButtonGroup();
		 shiftGroup.add(buttons75);
		 shiftGroup.add(buttons150);
		 shiftGroup.add(buttons170);
		 shiftGroup.add(buttons200);
		 shiftGroup.add(buttons250);
		 shiftGroup.add(buttons400);
		 shiftGroup.add(buttons425);
		 shiftGroup.add(buttons450);
		 shiftGroup.add(buttons500);
		 shiftGroup.add(buttons600);
		 shiftGroup.add(buttons625);
		 shiftGroup.add(buttons850);
		 shiftGroup.add(buttons1000);
		 // Stop Bits
		 boolean stop1=false,stop15=false,stop2=false,stop25=false;
		 JLabel labelStop=new JLabel("Stop Bits (Baudot only)");
		 if (rttyHandler.getStopBits()==1.0) stop1=true; 
		 else if (rttyHandler.getStopBits()==1.5) stop15=true; 
		 else if (rttyHandler.getStopBits()==2.0) stop2=true; 
		 else if (rttyHandler.getStopBits()==2.5) stop25=true; 
		 JRadioButton buttons1=new JRadioButton("1 Bit",stop1);
		 JRadioButton buttons15=new JRadioButton("1.5 Bits",stop15);
		 JRadioButton buttons2=new JRadioButton("2 Bits",stop2);
		 JRadioButton buttons25=new JRadioButton("2.5 Bits",stop25);
		 ButtonGroup stopGroup=new ButtonGroup();
		 stopGroup.add(buttons1);
		 stopGroup.add(buttons15);
		 stopGroup.add(buttons2);
		 stopGroup.add(buttons25);
		 // Add the components to the panel
		 // Baud Rate
		 panel.add(labelBaud);
		 panel.add(button45);
		 panel.add(button50);
		 panel.add(button75);
		 panel.add(button100);
		 panel.add(button200);
		 panel.add(button600);
		 // Shift
		 panel.add(labelShift);
		 panel.add(buttons75);
		 panel.add(buttons150);
		 panel.add(buttons170);
		 panel.add(buttons200);
		 panel.add(buttons250);
		 panel.add(buttons400);
		 panel.add(buttons425);
		 panel.add(buttons450);
		 panel.add(buttons500);
		 panel.add(buttons600);
		 panel.add(buttons625);
		 panel.add(buttons850);
		 panel.add(buttons1000);
		 // Stop Bits
		 panel.add(labelStop);
		 panel.add(buttons1);
		 panel.add(buttons15);
		 panel.add(buttons2);
		 panel.add(buttons25);
		 // Show JOptionPane that will ask user for this information
		 int resp=JOptionPane.showConfirmDialog(window,panel,"Baudot & FSK Options",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		 // If the user has clicked on the OK option then change values in the RTTY object
		 if (resp==JOptionPane.OK_OPTION)	{
			// Baud Rate
			if (button45.isSelected()==true)	{
				rttyHandler.setBaudRate(45.45);
				fskHandler.setBaudRate(45.45);
			}
			if (button50.isSelected()==true)	{
				rttyHandler.setBaudRate(50);
				fskHandler.setBaudRate(50);
			}
			if (button75.isSelected()==true)	{
				rttyHandler.setBaudRate(75);
				fskHandler.setBaudRate(75);
			}		
			if (button100.isSelected()==true)	{
				rttyHandler.setBaudRate(100);
				fskHandler.setBaudRate(100);
			}
			if (button200.isSelected()==true)	{
				rttyHandler.setBaudRate(200);
				fskHandler.setBaudRate(200);
			}
			if (button600.isSelected()==true)	{
				rttyHandler.setBaudRate(600);
				fskHandler.setBaudRate(600);
			}
			// Shift
			if (buttons75.isSelected()==true)	{
				rttyHandler.setShift(75);
				fskHandler.setShift(75);
			}			
			if (buttons150.isSelected()==true)	{
				rttyHandler.setShift(150);
				fskHandler.setShift(150);
			}			
			if (buttons170.isSelected()==true)	{
				rttyHandler.setShift(170);
				fskHandler.setShift(170);
			}
			if (buttons200.isSelected()==true)	{
				rttyHandler.setShift(200);
				fskHandler.setShift(200);
			}			
			if (buttons250.isSelected()==true)	{
				rttyHandler.setShift(250);
				fskHandler.setShift(250);
			}			
			if (buttons400.isSelected()==true)	{
				rttyHandler.setShift(400);
				fskHandler.setShift(400);
			}			
			if (buttons425.isSelected()==true)	{
				rttyHandler.setShift(425);
				fskHandler.setShift(425);
			}
			if (buttons450.isSelected()==true)	{
				rttyHandler.setShift(450);
				fskHandler.setShift(450);
			}
			if (buttons500.isSelected()==true)	{
				rttyHandler.setShift(500);
				fskHandler.setShift(500);
			}
			if (buttons600.isSelected()==true)	{
				rttyHandler.setShift(600);
				fskHandler.setShift(600);
			}
			if (buttons625.isSelected()==true)	{
				rttyHandler.setShift(625);
				fskHandler.setShift(625);
			}
			if (buttons850.isSelected()==true)	{
				rttyHandler.setShift(850);
				fskHandler.setShift(850);
			}
			if (buttons1000.isSelected()==true)	{
				rttyHandler.setShift(1000);
				fskHandler.setShift(1000);
			}
			// Stop Bits
			if (buttons1.isSelected()==true) rttyHandler.setStopBits(1.0);
			if (buttons15.isSelected()==true) rttyHandler.setStopBits(1.5);
			if (buttons2.isSelected()==true) rttyHandler.setStopBits(2.0);
			if (buttons25.isSelected()==true) rttyHandler.setStopBits(2.5);
		}
	}

	public boolean isInvertSignal() {
		return invertSignal;
	}

	public void setInvertSignal(boolean invertSignal) {
		this.invertSignal = invertSignal;
	}
	
	// Save the programs settings in the rivet_settings.xml file
	public void saveSettings()	{
		FileWriter xmlfile;
		String line;
		// Open the default file settings //
		try {
			xmlfile=new FileWriter("rivet_settings.xml");
			// Start the XML file //
			line="<?xml version='1.0' encoding='utf-8' standalone='yes'?><settings>";
			xmlfile.write(line);
			// Invert
			line="<invert val='";
			if (invertSignal==true) line=line+"TRUE";
			else line=line+"FALSE";
			line=line+"'/>";
			xmlfile.write(line);
			// Debug mode
			line="<debug val='";
			if (debug==true) line=line+"TRUE";
			else line=line+"FALSE";
			line=line+"'/>";
			xmlfile.write(line);
			// Mode
			line="<mode val='"+Integer.toString(system)+"'/>";
			xmlfile.write(line);
			// CROWD36 sync tone
			line="<c36tone val='"+Integer.toString(crowd36Handler.getSyncHighTone())+"'/>";
			xmlfile.write(line);
			// Soundcard Input Level
			line="<soundcard_level val='"+Integer.toString(soundCardInputLevel)+"'/>";
			xmlfile.write(line);
			// Soundcard Input
			if (soundCardInput==true) line="<soundcard_input val='1'/>";
			else line="<soundcard_input val='0'/>";
			xmlfile.write(line);
			// View GW Free Channel Markers
			if (viewGWChannelMarkers==true) line="<view_gw_markers val='1'/>";
			else line="<view_gw_markers val='0'/>";
			xmlfile.write(line);
			// RTTY & FSK
			// Baud
			line="<rttybaud val='"+Double.toString(rttyHandler.getBaudRate())+"'/>";
			xmlfile.write(line);
			// Shift
			line="<rttyshift val='"+Integer.toString(rttyHandler.getShift())+"'/>";
			xmlfile.write(line);
			// Stop bits
			line="<rttystop val='"+Double.toString(rttyHandler.getStopBits())+"'/>";
			xmlfile.write(line);			
			// Save the current audio source
			line="<audioDevice val='"+inputThread.getMixerName()+"'/>";
			xmlfile.write(line);
			// All done so close the root item //
			line="</settings>";
			xmlfile.write(line);
			// Flush and close the file //
			xmlfile.flush();
			xmlfile.close();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null,"Error : Unable to create the file rivet_settings.xml\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			}
		return;
	}
	
	// Read in the rivet_settings.xml file //
	public void readDefaultSettings() throws SAXException, IOException,ParserConfigurationException {
			// Create a parser factory and use it to create a parser
			SAXParserFactory parserFactory=SAXParserFactory.newInstance();
			SAXParser parser=parserFactory.newSAXParser();
			// This is the name of the file you're parsing
			String filename="rivet_settings.xml";
			// Instantiate a DefaultHandler subclass to handle events
			DefaultXMLFileHandler handler=new DefaultXMLFileHandler();
			// Start the parser. It reads the file and calls methods of the handler.
			parser.parse(new File(filename),handler);
		}

	
	// This class handles the rivet_settings.xml SAX events
	public class DefaultXMLFileHandler extends DefaultHandler {
			String value;
			
			public void endElement(String namespaceURI,String localName,String qName) throws SAXException {	
			}

			public void characters(char[] ch,int start,int length) throws SAXException {
				// Extract the element value as a string //
				String tval=new String(ch);
				value=tval.substring(start,(start+length));
			}
			
			// Handle an XML start element //
			public void startElement(String uri, String localName, String qName,Attributes attributes) throws SAXException {
				// Check an element has a value //
				if (attributes.getLength()>0) {
					// Get the elements value //
					String aval=attributes.getValue(0);
					// Debug mode //
					if (qName.equals("debug")) {
						if (aval.equals("TRUE")) setDebug(true);
						else setDebug(false);	
					}
					// Invert
					else if (qName.equals("invert")) {
						if (aval.equals("TRUE")) setInvertSignal(true);
						else setInvertSignal(false);	
					}
					// Mode
					else if (qName.equals("mode"))	{
						system=Integer.parseInt(aval);
					}
					// Crowd36 sync tone
					else if (qName.equals("c36tone"))	{
						crowd36Handler.setSyncHighTone(Integer.parseInt(aval));
					}
					// Soundcard input level
					else if (qName.equals("soundcard_level"))	{
						soundCardInputLevel=Integer.parseInt(aval);
						// Check if this is to high or to low
						if (soundCardInputLevel<-10) soundCardInputLevel=-10;
						else if (soundCardInputLevel>10) soundCardInputLevel=10;
					}
					// Soundcard input
					else if (qName.equals("soundcard_input"))	{
						if (Integer.parseInt(aval)==1) soundCardInputTemp=true;
						else soundCardInputTemp=false;
					}
					// View GW Free Channel Markers
					else if (qName.equals("view_gw_markers"))	{
						if (Integer.parseInt(aval)==1) viewGWChannelMarkers=true;
						else viewGWChannelMarkers=false;
					}
					// RTTY & FSK Options
					// Baud rate
					else if (qName.equals("rttybaud"))	{
						rttyHandler.setBaudRate(Double.parseDouble(aval));
						fskHandler.setBaudRate(Double.parseDouble(aval));
					}
					// Shift
					else if (qName.equals("rttyshift"))	{
						rttyHandler.setShift(Integer.parseInt(aval));
						fskHandler.setShift(Integer.parseInt(aval));
					}
					// Stop bits
					else if (qName.equals("rttystop"))	{
						rttyHandler.setStopBits(Double.parseDouble(aval));
					}
					// The audio input source
					else if (qName.equals("audioDevice"))	{
						if (inputThread.changeMixer(aval)==false) {
							JOptionPane.showMessageDialog(null,"Error changing mixer\n"+inputThread.getMixerErrorMessage(),"Rivet",JOptionPane.ERROR_MESSAGE);
						}
					}
					
				}	
				
			}
		}
	
	
	// Change the invert setting
	public void changeInvertSetting ()	{
		if (invertSignal==true) invertSignal=false;
		else invertSignal=true;
	}
	
	// Set the soundcard input level in the input thread
	public void setSoundCardLevel (int sli)	{
		soundCardInputLevel=sli;
		// Pass this to the input thread
		inputThread.setInputLevel(sli);
	}
	
	// Returns the current sound card input level
	public int getSoundCardLevel()	{
		return soundCardInputLevel;
	}
	
	public boolean issoundCardInputTemp()	{
		return soundCardInputTemp;
	}

	public boolean isBitStreamOut() {
		return bitStreamOut;
	}

	public void setBitStreamOut(boolean bitStreamOut) {
		this.bitStreamOut = bitStreamOut;
	}

	public boolean isViewGWChannelMarkers() {
		return viewGWChannelMarkers;
	}

	public void setViewGWChannelMarkers(boolean viewGWChannelMarkers) {
		this.viewGWChannelMarkers = viewGWChannelMarkers;
	}
	
	public void clearBitStreamCountOut()	{
		bitStreamOutCount=0;
	}
	
	// Adds a line to the display
	public void writeLine(String line,Color col,Font font) {
		if (line!=null)	{
			if (logging==true) fileWriteLine(line);
			if (pauseDisplay==false) display_view.addLine(line,col,font);
		}
	}
	
	// Adds a single char to the current line on the display
	public void writeChar (String ct,Color col,Font font)	{
		if (ct!=null)	{
			if (pauseDisplay==false) display_view.addChar(ct,col,font);
			if (logging==true) fileWriteChar(ct);
		}
	}
	
	// Clear the display screen
	public void clearScreen()	{
		display_view.clearScreen();
	}
	
	// Writes a new line to the screen
	public void newLineWrite()	{
		if (pauseDisplay==false) display_view.newLine();
		if (logging==true) fileWriteNewline();
	}

	public List<Trigger> getListTriggers() {
		return listTriggers;
	}

	public void setListTriggers(List<Trigger> listTriggers) {
		this.listTriggers = listTriggers;
		// Count the number of active triggers
		activeTriggerCount=0;
		int a;
		for (a=0;a<listTriggers.size();a++)	{
			if (listTriggers.get(a).isActive()==true) activeTriggerCount++;
		}
	}
	
	// Read in the trigger.xml file //
	public void readTriggerSettings() throws SAXException, IOException,ParserConfigurationException {
			// Create a parser factory and use it to create a parser
			SAXParserFactory parserFactory=SAXParserFactory.newInstance();
			SAXParser parser=parserFactory.newSAXParser();
			// This is the name of the file you're parsing
			String filename="trigger.xml";
			// Instantiate a DefaultHandler subclass to handle events
			TriggerXMLFileHandler handler=new TriggerXMLFileHandler();
			// Start the parser. It reads the file and calls methods of the handler.
			parser.parse(new File(filename),handler);
		}

	
	public int getActiveTriggerCount() {
		return activeTriggerCount;
	}

	// This class handles the rivet_settings.xml SAX events
	public class TriggerXMLFileHandler extends DefaultHandler {
			String value,description,sequence;
			int type,backward,forward;
			// Handle an XML start element
			public void endElement(String namespaceURI,String localName,String qName) throws SAXException {	
				// Look for a <trigger> end tag
				if (qName.equals("trigger"))	{
					// Put the values in a Trigger object
					Trigger trigger=new Trigger();
					trigger.setTriggerDescription(description);
					trigger.setTriggerSequence(sequence);
					trigger.setTriggerType(type);
					// If type 3 (GRAB) load the forward and backward values
					if (type==3)	{
						trigger.setForwardGrab(forward);
						trigger.setBackwardGrab(backward);
					}
					// Add this to the Trigger list
					listTriggers.add(trigger);
				}
			}

			public void characters(char[] ch,int start,int length) throws SAXException {
				// Extract the element value as a string //
				String tval=new String(ch);
				value=tval.substring(start,(start+length));
			}
			
			// Handle an XML start element //
			public void startElement(String uri, String localName, String qName,Attributes attributes) throws SAXException {
				// Check an element has a value //
				if (attributes.getLength()>0) {
					// Get the elements value //
					String aval=attributes.getValue(0);
					// Trigger Description //
					if (qName.equals("description")) {
						description=aval;
					}
					// Trigger Sequence
					if (qName.equals("sequence")) {
						sequence=aval;
					}					
					// Trigger Type
					if (qName.equals("type"))	{
						type=Integer.parseInt(aval);
					}
					// Forward grab value
					if (qName.equals("forward"))	{
						forward=Integer.parseInt(aval);
					}
					// Backward grab value
					if (qName.equals("backward"))	{
						backward=Integer.parseInt(aval);
					}
				}	
				
			}
		}
	
	// Save the current Trigger list to the file trigger.xml
	public boolean saveTriggerXMLFile () 	{
		try	{
			FileWriter xmlfile;
			String line;
			xmlfile=new FileWriter("trigger.xml");
			// Start the XML file //
			line="<?xml version='1.0' encoding='utf-8' standalone='yes'?>";
			xmlfile.write(line);
			line="\n<settings>";
			xmlfile.write(line);
			// Run through each Trigger object in the list
			int a;
			for (a=0;a<listTriggers.size();a++)	{
				line="\n <trigger>";
				xmlfile.write(line);
				// Description
				line="\n  <description val='"+listTriggers.get(a).getTriggerDescription()+"'/>";
				xmlfile.write(line);
				// Sequence
				line="\n  <sequence val='"+listTriggers.get(a).getTriggerSequence()+"'/>";
				xmlfile.write(line);
				// Type
				line="\n  <type val='"+Integer.toString(listTriggers.get(a).getTriggerType())+"'/>";
				xmlfile.write(line);
				// If a GRAB (type 3) then save the forward and backward values
				if (listTriggers.get(a).getTriggerType()==3)	{
					// Forward bits
					line="\n  <forward val='"+Integer.toString(listTriggers.get(a).getForwardGrab())+"'/>";
					xmlfile.write(line);
					// Backward bits
					line="\n  <backward val='"+Integer.toString(listTriggers.get(a).getBackwardGrab())+"'/>";
					xmlfile.write(line);	
				}
				line="\n </trigger>";
				xmlfile.write(line);
			}
			// All done so close the root item //
			line="\n</settings>";
			xmlfile.write(line);
			// Flush and close the file //
			xmlfile.flush();
			xmlfile.close();
			
		}
		catch (Exception e)	{
			debugDump("Error writing Triger.xml :"+e.toString());
			return false;
		}
		return true;
	}
	
	// Change the audio mixer
	public boolean changeMixer(String mixerName)	{
		// Tell the audio in thread to change its mixer
		return inputThread.changeMixer(mixerName);
	}

	public boolean isPauseDisplay() {
		return pauseDisplay;
	}

	public void setPauseDisplay(boolean pauseDisplay) {
		this.pauseDisplay = pauseDisplay;
	}

	public boolean isAutoScroll() {
		return autoScroll;
	}

	public void setAutoScroll(boolean autoScroll) {
		this.autoScroll = autoScroll;
	}	
	
	// Return the current height of the window
	public int getCurrentHeight ()	{
		return window.getBounds().height;
	}
	
	// Tell the window to scroll down by a set amount
	public void scrollDown(int v)	{
		window.scrollDown((v-window.getBounds().height)+200);
	}
	
	// Return if the vertical scroll bar is being adjusted
	public boolean isAdjusting()	{
		return window.isAdjusting();
	}

	public long getLastUserScroll() {
		return lastUserScroll;
	}

	public void setLastUserScroll(long lastUserScroll) {
		this.lastUserScroll = lastUserScroll;
	}
	
	
}
