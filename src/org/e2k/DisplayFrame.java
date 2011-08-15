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
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;

public class DisplayFrame extends JFrame implements ActionListener {
	
	private JMenuBar menuBar=new JMenuBar();
	private Rivet theApp;
	public static final long serialVersionUID=1;
	private JStatusBar statusBar=new JStatusBar();
	public JScrollBar vscrollbar=new JScrollBar(JScrollBar.VERTICAL,0,1,0,2000);
	private JMenuItem exit_item,wavLoad_item,save_to_file;
	private JMenuItem XPA_item,XPA2_item,CROWD36_item;
	
	// Constructor
	public DisplayFrame(String title,Rivet theApp) {
		setTitle(title);
		this.theApp=theApp;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.WHITE);
		// Menu setup
		setJMenuBar(menuBar);
		// Main
		JMenu mainMenu=new JMenu("Main");
		mainMenu.add(wavLoad_item=new JMenuItem("Load a WAV File"));		
		wavLoad_item.addActionListener(this);
		mainMenu.add(save_to_file=new JRadioButtonMenuItem("Save to File",theApp.getLogging()));
		save_to_file.addActionListener(this);
		mainMenu.add(exit_item=new JMenuItem("Exit"));		
		exit_item.addActionListener(this);
		menuBar.add(mainMenu);
		// Modes
		JMenu modeMenu=new JMenu("Modes");
		modeMenu.add(CROWD36_item=new JRadioButtonMenuItem(theApp.MODENAMES[0],theApp.isCROWD36()));
		CROWD36_item.addActionListener(this);
		modeMenu.add(XPA_item=new JRadioButtonMenuItem(theApp.MODENAMES[1],theApp.isXPA()));
		XPA_item.addActionListener(this);
		modeMenu.add(XPA2_item=new JRadioButtonMenuItem(theApp.MODENAMES[2],theApp.isXPA2()));
		XPA2_item.addActionListener(this);
		menuBar.add(modeMenu);
		
		// Help
		JMenu helpMenu=new JMenu("Help");
		menuBar.add(helpMenu);
		
		// Add the vertical scrollbar
		add(vscrollbar,BorderLayout.EAST);
		// Add a listener for this
		vscrollbar.addAdjustmentListener(new MyAdjustmentListener());
		
		// Setup the status bar
		getContentPane().add(statusBar, java.awt.BorderLayout.SOUTH);
		statusBar.setLoggingStatus("Not Logging");
		statusBar.setStatusLabel("Idle");
		statusBar.setApp(theApp);
		
		statusBarUpdate();
		}

	
	// Handle messages from the scrollbars
	class MyAdjustmentListener implements AdjustmentListener  {
		public void adjustmentValueChanged(AdjustmentEvent e) {
			// Vertical scrollbar
			if (e.getSource()==vscrollbar) {
				theApp.vertical_scrollbar_value=e.getValue();
				repaint();   
			}
			
		}
	 }
	
	// Handle all menu events
	public void actionPerformed (ActionEvent event) {
		String event_name=event.getActionCommand();
		// Run through all the mode names
		for (int a=0;a<theApp.MODENAMES.length;a++)	{
			if (event_name==theApp.MODENAMES[a]) theApp.setSystem(a);
		}
		// Load a WAV file
		if (event_name=="Load a WAV File")	{
			String fileName=loadDialogBox();
			if (fileName!=null) theApp.loadWAVfile(fileName);
		}
		// Save to File
		if (event_name=="Save to File")	{		
			if (theApp.getLogging()==false)	{
				if (saveDialogBox()==false)	{
					menuItemUpdate();
					return;
				}
				theApp.setLogging(true);
				statusBar.setLoggingStatus("Logging");
			}
			 else	{
				 closeLogFile();
			 }
		}	
		// Exit 
		if (event_name=="Exit") {
			// If logging then close the log file
			if (theApp.getLogging()==true) closeLogFile();
			// Stop the program //
			System.exit(0);	
		}
		
		menuItemUpdate();
		statusBarUpdate();
	}
	
	private void menuItemUpdate()	{
		save_to_file.setSelected(theApp.getLogging());
		CROWD36_item.setSelected(theApp.isCROWD36());
		XPA_item.setSelected(theApp.isXPA());
		XPA2_item.setSelected(theApp.isXPA2());
	}
	
	// Display a dialog box so the user can select a WAV file they wish to process
	public String loadDialogBox ()	{
		String file_name;
		// Bring up a dialog box that allows the user to select the name
		// of the WAV file to be loaded
		JFileChooser fc=new JFileChooser();
		// The dialog box title //
		fc.setDialogTitle("Select a WAV file to load");
		// Start in current directory
		fc.setCurrentDirectory(new File("."));
		// Don't all types of file to be selected //
		fc.setAcceptAllFileFilterUsed(false);
		// Only show .wav files //
		fc.setFileFilter(new WAVfileFilter());
		// Show open dialog; this method does not return until the
		// dialog is closed
		int returnval=fc.showOpenDialog(this);
		// If the user has selected cancel then quit
		if (returnval==JFileChooser.CANCEL_OPTION) return null;
		// Get the file name an path of the selected file
		file_name=fc.getSelectedFile().getPath();
		return file_name;
	}
	
	private void statusBarUpdate()	{
		statusBar.setModeLabel(theApp.MODENAMES[theApp.getSystem()]);
	}
	
	public void progressBarUpdate (int v)	{
		statusBar.setVolumeBar(v);
	}
	
	public void setStatusLabel (String st)	{
		statusBar.setStatusLabel(st);
	}
	
	// Close the log file
	public void closeLogFile()	{
		 theApp.setLogging(false);
		 statusBar.setLoggingStatus("Not Logging");
		 try 	{
			 // Close the file
			 theApp.file.flush();
			 theApp.file.close();
		 }
		 catch (Exception e)	{
			 JOptionPane.showMessageDialog(null,"Error closing Log file","DMRDecode", JOptionPane.INFORMATION_MESSAGE);
		 }
	}
	
	// Display a dialog box so the user can select a location and name for a log file
	public boolean saveDialogBox ()	{
		if (theApp.getLogging()==true) return false;
		String file_name;
		// Bring up a dialog box that allows the user to select the name
		// of the saved file
		JFileChooser fc=new JFileChooser();
		// The dialog box title //
		fc.setDialogTitle("Select the log file name");
		// Start in current directory
		fc.setCurrentDirectory(new File("."));
		// Don't all types of file to be selected //
		fc.setAcceptAllFileFilterUsed(false);
		// Only show .txt files //
		fc.setFileFilter(new TextFileFilter());
		// Show save dialog; this method does not return until the
		// dialog is closed
		int returnval=fc.showSaveDialog(this);
		// If the user has selected cancel then quit
		if (returnval==JFileChooser.CANCEL_OPTION) return false;
		// Get the file name an path of the selected file
		file_name=fc.getSelectedFile().getPath();
		// Does the file name end in .txt ? //
		// If not then automatically add a .txt ending //
		int last_index=file_name.lastIndexOf(".txt");
		if (last_index!=(file_name.length()-4)) file_name=file_name + ".txt";
		// Create a file with this name //
		File tfile=new File(file_name);
		// If the file exists ask the user if they want to overwrite it
		if (tfile.exists()) {
			int response = JOptionPane.showConfirmDialog(null,
					"Overwrite existing file?", "Confirm Overwrite",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.CANCEL_OPTION) return false;
		}
		// Open the file
		try {
			theApp.file=new FileWriter(tfile);
			// Write the program version as the first line of the log
			String fline=theApp.program_version+"\r\n";
			theApp.file.write(fline);
			
		} catch (Exception e) {
			System.out.println("\nError opening the logging file");
			return false;
		}
		theApp.setLogging(true);
		return true;
	}

}
