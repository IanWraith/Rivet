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

import javax.swing.JComponent;
import java.util.Observer;
import java.util.Observable;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class DisplayView extends JComponent implements Observer {
	
	public static final long serialVersionUID=1;
	private static final int DISPLAYCOUNT=130;
	private String display_string[]=new String[DISPLAYCOUNT];
	private Color displayColour[]=new Color[DISPLAYCOUNT];
	private Font displayFont[]=new Font[DISPLAYCOUNT];
	private int displayCounter=0;
	private Rivet theApp;	
	
	public DisplayView (Rivet theApp) {
		this.theApp=theApp;	
	}
			
	public void update (Observable o,Object rectangle)	{			
	}
			
	// Draw the main screen //
	public void paint (Graphics g) {
		int count=0,pos=20,i;
		i=displayCounter;
		Graphics2D g2D=(Graphics2D)g;	
		// Draw in the lines on the screen
		// taking account of the fact that the data is stored in a circular buffer
		// we need to display the oldest line stored first and then go backwards from
		// that point onwards
		while(count<DISPLAYCOUNT)	{
			// Only display info if something is stored in the display string
			if (display_string[i]!=null)	{
				g.setColor(displayColour[i]);
				g.setFont(displayFont[i]);
				g2D.drawString(display_string[i],(5-theApp.horizontal_scrollbar_value),(pos-theApp.vertical_scrollbar_value));	
				pos=pos+20;
			}	
			i++;
			if (i>=DISPLAYCOUNT) i=0;
			count++;
		}
	}
	
	// Add a line to the display circular buffer //
	public void add_line (String line,Color tcol,Font tfont) {
		display_string[displayCounter]=line;
		displayColour[displayCounter]=tcol;
		displayFont[displayCounter]=tfont;
		// Increment the circular buffer
		displayCounter++;
		// Check it hasn't reached its maximum size
		if (displayCounter==DISPLAYCOUNT) displayCounter=0;
		repaint();
	}
	
	// Gets all the text on the screen and returns it as a string
	public String getText()	{
		StringBuffer buffer=new StringBuffer();
		int i=displayCounter,count=0;
		while(count<DISPLAYCOUNT)	{
			if (display_string[i]!=null)	{
				buffer.append(display_string[i]);
				buffer.append("\n");
			}	
			i++;
			if (i>=DISPLAYCOUNT) i=0;
			count++;
		}
		return buffer.toString();
	}

}
