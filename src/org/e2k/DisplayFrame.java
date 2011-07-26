package org.e2k;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class DisplayFrame extends JFrame implements ActionListener {
	
	private JMenuBar menuBar=new JMenuBar();
	private Rivet theApp;
	public static final long serialVersionUID=1;
	public JScrollBar vscrollbar=new JScrollBar(JScrollBar.VERTICAL,0,1,0,2000);
	private JMenuItem exit_item;
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
		mainMenu.add(exit_item=new JMenuItem("Exit"));		
		exit_item.addActionListener(this);
		menuBar.add(mainMenu);
		
		// System
		JMenu systemMenu=new JMenu("System");
		systemMenu.add(CROWD36_item=new JRadioButtonMenuItem("CROWD36",theApp.isCROWD36()));
		CROWD36_item.addActionListener(this);
		systemMenu.add(XPA_item=new JRadioButtonMenuItem("XPA",theApp.isXPA()));
		XPA_item.addActionListener(this);
		systemMenu.add(XPA2_item=new JRadioButtonMenuItem("XPA2",theApp.isXPA2()));
		XPA2_item.addActionListener(this);
		
		menuBar.add(systemMenu);
		
		// Help
		JMenu helpMenu=new JMenu("Help");
		menuBar.add(helpMenu);
		
		// Add the vertical scrollbar
		add(vscrollbar,BorderLayout.EAST);
		// Add a listener for this
		vscrollbar.addAdjustmentListener(new MyAdjustmentListener());
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
		
		// CROWD36
		if (event_name=="CROWD36")	{
			theApp.setSystem(0);
		}
		// XPA
		if (event_name=="XPA")	{
			theApp.setSystem(1);
		}
		// XPA2
		if (event_name=="XPA2")	{
			theApp.setSystem(2);
		}
		
		// Exit 
		if (event_name=="Exit") {
			// Stop the program //
			System.exit(0);	
		}
		
		menuItemUpdate();
	}
	
	private void menuItemUpdate()	{
		CROWD36_item.setSelected(theApp.isCROWD36());
		XPA_item.setSelected(theApp.isXPA());
		XPA2_item.setSelected(theApp.isXPA2());
	}

}
