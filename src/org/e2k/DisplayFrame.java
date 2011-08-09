package org.e2k;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;

public class DisplayFrame extends JFrame implements ActionListener {
	
	private JMenuBar menuBar=new JMenuBar();
	private Rivet theApp;
	public static final long serialVersionUID=1;
	private JStatusBar statusBar=new JStatusBar();
	public JScrollBar vscrollbar=new JScrollBar(JScrollBar.VERTICAL,0,1,0,2000);
	private JMenuItem exit_item,wavLoad_item;
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
		// Exit 
		if (event_name=="Exit") {
			// Stop the program //
			System.exit(0);	
		}
		
		menuItemUpdate();
		statusBarUpdate();
	}
	
	private void menuItemUpdate()	{
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

}
