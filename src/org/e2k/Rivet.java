package org.e2k;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.SwingUtilities;

public class Rivet {

	private DisplayModel display_model;
	private DisplayView display_view;
	private static Rivet theApp;
	private static DisplayFrame window;
	public String program_version="Rivet(Build 0) by Ian Wraith";
	public int vertical_scrollbar_value=0;
	public int horizontal_scrollbar_value=0;
	public boolean pReady=false;
	private int system=0;
	
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

}
