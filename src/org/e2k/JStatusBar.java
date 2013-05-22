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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JStatusBar extends JPanel implements ChangeListener {
	
	public static final long serialVersionUID=1;
	private JLabel logMode=new JLabel();
	private JLabel statusLabel=new JLabel();
	private JLabel modeLabel=new JLabel();
	private JProgressBar volumeBar=new JProgressBar(0,100);
	private Rivet TtheApp;
	private JSlider inputLevelSlider;
	private JButton clearDisplayButton=new JButton("Clear Display");
	private JButton pauseDisplayButton=new JButton("Pause Display");
	
	public JStatusBar() {
		final int FONTSIZE=20;
		final String FONTTYPE="Times New Roman";
		Border paddingBorder=BorderFactory.createEmptyBorder(10,10,10,10);
		Border border=BorderFactory.createLineBorder(Color.BLUE);
		logMode.setHorizontalAlignment(SwingConstants.LEFT);
		logMode.setBorder(BorderFactory.createCompoundBorder(border,paddingBorder));
		logMode.setFont(new Font(FONTTYPE,Font.BOLD,FONTSIZE));
		logMode.setBackground(Color.YELLOW);
		logMode.setOpaque(true);
		logMode.updateUI();
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(border,paddingBorder));
		statusLabel.setFont(new Font(FONTTYPE,Font.BOLD,FONTSIZE));
		statusLabel.setBackground(Color.YELLOW);
		statusLabel.setOpaque(true);
		statusLabel.updateUI();
		modeLabel.setHorizontalAlignment(SwingConstants.LEFT);
		modeLabel.setBorder(BorderFactory.createCompoundBorder(border,paddingBorder));
		modeLabel.setFont(new Font(FONTTYPE,Font.BOLD,FONTSIZE));
		modeLabel.setBackground(Color.YELLOW);
		modeLabel.setOpaque(true);
		modeLabel.updateUI();
		// Input level slider
		inputLevelSlider=new JSlider(JSlider.HORIZONTAL,-10,10,1);
		inputLevelSlider.addChangeListener(this);
		inputLevelSlider.setMajorTickSpacing(5);
		inputLevelSlider.setMinorTickSpacing(1);
		inputLevelSlider.setPaintTicks(true);
		inputLevelSlider.setPaintLabels(true);
		inputLevelSlider.setBorder(BorderFactory.createTitledBorder("Input Level Adjust"));
		// Buttons
		clearDisplayButton.addActionListener(new ButtonListener());
		pauseDisplayButton.addActionListener(new ButtonListener());
		// Give the volume progress bar a border //
		volumeBar.setBorder(BorderFactory.createTitledBorder("Input Level"));
		volumeBar.setForeground(Color.GREEN);
		// Create a pane to hold just the buttons so we can stack them on top of each other
		JPanel buttonPane=new JPanel(new BorderLayout());
		buttonPane.add(clearDisplayButton,BorderLayout.NORTH);
		buttonPane.add(pauseDisplayButton,BorderLayout.SOUTH);
		// Ensure the elements of the status bar are displayed from the left
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		this.add(buttonPane,BorderLayout.CENTER);
		this.add(volumeBar,BorderLayout.CENTER);
		this.add(inputLevelSlider,BorderLayout.CENTER);
		this.add(modeLabel,BorderLayout.CENTER);
		this.add(logMode,BorderLayout.CENTER);
		this.add(statusLabel,BorderLayout.CENTER);
	}
	
	// Sets the logging label text
	public void setLoggingStatus(String text) {
		logMode.setText(text);
	}

	// Sets the status label
	public void setStatusLabel (String st)	{
		statusLabel.setText(st);
	}
	
	// Set the volume bar display
	public void setVolumeBar(int val) {
		// Check that val is never to large
		if (val>100) val=100;
		// Update the volume bar
		volumeBar.setValue(val);
		volumeBar.repaint();
	}
	
	public void setModeLabel (String label)	{
		modeLabel.setText(label);
	}
	

	public void setApp (Rivet theApp)	{
		TtheApp=theApp;
	}
	
	// This is called when the slider changes
	public void stateChanged(ChangeEvent e) {
        JSlider source=(JSlider)e.getSource();
        if (!source.getValueIsAdjusting()) {
        	int level=(int)source.getValue();
        	TtheApp.setSoundCardLevel(level);
        }
	}

	// This allows the main program to set the slider value
	public void setSoundCardInput (int scl)	{
		inputLevelSlider.setValue(scl);
	}
	
	
	// This class listens for button events
	class ButtonListener implements ActionListener {
		  ButtonListener() {
		  }

		  public void actionPerformed(ActionEvent e) {
			// The user wants to clear the display
			if (e.getActionCommand().equals("Clear Display")) {
		    	if (TtheApp!=null) TtheApp.clearScreen();
		    }
			// The user wants to pause the display
			if (e.getActionCommand().equals("Pause Display")) {
		    	if (TtheApp!=null) TtheApp.setPauseDisplay(true);
		    	pauseDisplayButton.setText("Restart Display");
		    }
			// The user wants to restart the display
			if (e.getActionCommand().equals("Restart Display")) {
		    	if (TtheApp!=null) TtheApp.setPauseDisplay(false);
		    	pauseDisplayButton.setText("Pause Display");
		    }		   
			
		  }
		}	

}
