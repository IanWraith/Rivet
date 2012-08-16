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
import java.util.Date;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
	public String program_version="Rivet (Build 34) by Ian Wraith";
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
    public GW gwHandler=new GW(this);
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
	
	public final String MODENAMES[]={"CROWD36","XPA (10 Baud)","XPA2","XPA (20 Baud)","Experimental","CIS 36-50","FSK200/500","CCIR493-4","FSK200/1000","GW FSK"};
    
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
					addLine(crowd36Handler.toneResults(),Color.BLACK,plainFont);
				}
				else if (system==5)	{
					addLine(cis3650Handler.lineBuffer.toString(),Color.BLACK,plainFont);
				}
				else if (system==6)	{
					addLine(fsk200500Handler.getQuailty(),Color.BLACK,plainFont);
				}
				else if (system==8)	{
					addLine(fsk2001000Handler.getQuailty(),Color.BLACK,plainFont);
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
			// Experimental
			else if (system==5)	outLines=cis3650Handler.decode(circBuffer,waveData);
			// FSK200/500
			else if (system==6)	outLines=fsk200500Handler.decode(circBuffer,waveData);
			// CCIR493-4
			else if (system==7)	outLines=ccir493Handler.decode(circBuffer,waveData);
			// FSK200/1000
			else if (system==8)	outLines=fsk2001000Handler.decode(circBuffer,waveData);
			// GW
			else if (system==9) outLines=gwHandler.decode(circBuffer,waveData);
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
	
	// Write a string to the bit stream file
	public boolean bitStreamWrite(String fline) {
		try {
			bitStreamFile.write(fline);
		} catch (Exception e) {
			// We have a problem
			bitStreamOut=false;
			JOptionPane.showMessageDialog(null,"Error writing to the bit stream file.\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
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

	// Change the sound source
	public void setSoundCardInput(boolean s) {
		// Try to close the audio device if it is already in operation
		if (this.soundCardInput==true) inputThread.closeAudio();
		// If the soundcard is already running we need to close it
		if (s==false)	{
			this.soundCardInput=false;
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
			// CIS36-50
			else if (system==5)	{
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
			// FSK200/500 + FSK200/1000
			else if ((system==6)||(system==8))	{
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
			// CCIR493-4
			else if (system==7)	{
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
			// GW
			else if (system==9)	{
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
	}
	
	// Gets all the text on the screen and returns it as a string
	public String getAllText()	{
		String all=display_view.getText();
		return all;
	}
	
	// Allows the user to set the CROWD36 high sync tone number
	public void getCROWD36SyncHighTone ()	{
		 //Create a panel that will contain the sync number
		 JPanel panel=new JPanel();
		 //Set JPanel layout using GridLayout
		 panel.setLayout(new GridLayout(2,1));
		 //Create a label with text (Username)
		 JLabel label=new JLabel("High Sync Tone Number (0 to 33)");
		//Create text field that will use to enter the high sync tone
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

	public boolean isInvertSignal() {
		return invertSignal;
	}

	public void setInvertSignal(boolean invertSignal) {
		this.invertSignal = invertSignal;
		window.menuItemUpdate();
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
			saxHandler handler=new saxHandler();
			// Start the parser. It reads the file and calls methods of the handler.
			parser.parse(new File(filename),handler);
		}

	
	// This class handles the SAX events
	public class saxHandler extends DefaultHandler {
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
					
				}	
				
			}
		}
	
	// Change the invert setting
	public void changeInvertSetting ()	{
		if (invertSignal==true) invertSignal=false;
		else invertSignal=true;
		// TODO : Fix the menu not updating when the invert signal option is changed
		// Update the menu to show this
		//window.repaint();
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
	
}
