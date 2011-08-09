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
	private static final int DISPLAYCOUNT=150;
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

}
