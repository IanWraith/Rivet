package org.e2k;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;

public class TriggerModify extends JDialog implements ActionListener {
	
	public static final long serialVersionUID=1;
	private Rivet TtheApp;
	private List<Trigger> triggerList=new ArrayList<Trigger>();
	private JComboBox<String> triggerComboList=new JComboBox<String>();
	
	
	public TriggerModify ()	{
		// Ensure the elements of the status bar are displayed from the left
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		this.add(triggerComboList);
	
	}
	
	public void setup (Rivet theApp)	{
		TtheApp=theApp;
		this.setSize(400, 400);
		this.setVisible(true);
		// Get a current list of triggers
		triggerList=TtheApp.getListTriggers();
		// Create a combo box
		createTriggerCombo();
	}
	
	
	// Create a uneditable combo box showing the current triggers
	private void createTriggerCombo ()	{
		// Populate a combo box with a trigger list in it
		if (triggerList!=null)	{
			int a;
			for (a=0;a<triggerList.size();a++)	{
				triggerComboList.addItem(triggerList.get(a).getTriggerDescription());
			}
			this.add(triggerComboList);
		}
		
	}
	

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
	}

}
