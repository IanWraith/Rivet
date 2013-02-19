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
	private JButton addTriggerButton=new JButton("Add a new Trigger");
	private JButton deleteTriggerButton=new JButton("Delete the selected Trigger");
	private JButton editTriggerButton=new JButton("Edit the selected Trigger");
	private JButton okButton=new JButton("OK");
	private JButton cancelButton=new JButton("Cancel");
	
	public TriggerModify ()	{
	
	}
	
	public void setup (Rivet theApp)	{
		TtheApp=theApp;
		this.setSize(300,400);
		// Position the dialog box in the centre of the screen
		final Toolkit toolkit=Toolkit.getDefaultToolkit();
		final Dimension screenSize=toolkit.getScreenSize();
		final int x=(screenSize.width-this.getWidth())/2;
		final int y=(screenSize.height-this.getHeight())/2;
		this.setLocation(x,y);
		this.setVisible(true);
		this.setLayout(new GridLayout(9,1));
		this.setTitle("Add or Modify Triggers");
		// Get a current list of triggers
		triggerList=TtheApp.getListTriggers();
		// Add a label to describe the Trigger select combo box
		JLabel labelCombo=new JLabel("Trigger Select : ");
		this.add(labelCombo);
		// Create a combo box
		createTriggerCombo();
		// Add a gap between the combo box and the next buttons
		this.add(new JLabel(""));
		// Function Buttons
		this.add(addTriggerButton);
		addTriggerButton.addActionListener(this);
		this.add(deleteTriggerButton);
		deleteTriggerButton.addActionListener(this);
		this.add(editTriggerButton);
		editTriggerButton.addActionListener(this);
		// Another gap
		this.add(new JLabel(""));
		// The OK and Cancel buttons
		this.add(okButton);
		okButton.addActionListener(this);
		this.add(cancelButton);
		cancelButton.addActionListener(this);
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
	

	// Handle all actions
	public void actionPerformed (ActionEvent event) {
		String eventName=event.getActionCommand();	
		// OK
		if (eventName.equals("OK"))	{
			// Transfer the trigger this to the main program
			TtheApp.setListTriggers(triggerList);
			this.setVisible(false);
		}
		// Cancel
		else if (eventName.equals("Cancel"))	{
			this.setVisible(false);
		}
		// Delete a trigger
		else if (eventName.equals("Delete the selected Trigger"))	{
			deleteTrigger();
		}
	
		
	}
	
	// Delete a trigger
	private void deleteTrigger ()	{
		// Find the name of the selected Trigger
		String selectedTriggerName=(String) triggerComboList.getSelectedItem();
		
		
	}
	
	

}
