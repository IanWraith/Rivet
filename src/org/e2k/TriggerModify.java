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
	private boolean changedTriggers=false;
	
	public TriggerModify (JFrame mf,Rivet theApp)	{
		super(mf,"Modify Triggers",true);
		TtheApp=theApp;	
		this.setSize(300,400);
		// Position the dialog box in the centre of the screen
		final Toolkit toolkit=Toolkit.getDefaultToolkit();
		final Dimension screenSize=toolkit.getScreenSize();
		final int x=(screenSize.width-this.getWidth())/2;
		final int y=(screenSize.height-this.getHeight())/2;
		this.setLocation(x,y);
		this.setLayout(new GridLayout(9,1));
		// Get a current list of triggers
		triggerList=TtheApp.getListTriggers();
		// Add a label to describe the Trigger select combo box
		JLabel labelCombo=new JLabel("Trigger Select : ");
		this.add(labelCombo);
		// Create a combo box and add it to the dialog
		populateTriggerCombo();
		this.add(triggerComboList);
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
		this.setVisible(true);		
	}
	
	// Create a uneditable combo box showing the current triggers
	private void populateTriggerCombo ()	{
		// Populate a combo box with a trigger list in it
		if (triggerList!=null)	{
			int a;
			for (a=0;a<triggerList.size();a++)	{
				triggerComboList.addItem(triggerList.get(a).getTriggerDescription());
			}
		}
	}
	

	// Handle all actions
	public void actionPerformed (ActionEvent event) {
		String eventName=event.getActionCommand();	
		// OK
		if (eventName.equals("OK"))	{
			// Transfer the trigger this to the main program
			TtheApp.setListTriggers(triggerList);
			changedTriggers=true;
			dispose();
		}
		// Cancel
		else if (eventName.equals("Cancel"))	{
			changedTriggers=false;
			dispose();
		}
		// Delete a trigger
		else if (eventName.equals("Delete the selected Trigger"))	{
			deleteTrigger();
		}
		// Edit a trigger
		else if (eventName.equals("Edit the selected Trigger"))	{
			// Find the name of the selected Trigger
			String selectedTriggerName=(String) triggerComboList.getSelectedItem();
			// Get the list index of this trigger
			int index=getTriggerIndex(selectedTriggerName);
			// Check if anything has been selected
			if (index==-1)	{
				JOptionPane.showMessageDialog(null,"Please select the Trigger you wish to edit.","Rivet", JOptionPane.ERROR_MESSAGE);
				return;
			}
			TriggerModifyAddEdit triggerMod=new TriggerModifyAddEdit(this,triggerList.get(index));
			// Has a trigger changed ?
			if (triggerMod.isChangedTriggers()==true)	{
				// Remove the old trigger
				triggerList.remove(index);
				// Add the edited trigger
				triggerList.add(triggerMod.exposeTrigger());
				// Ensure the object knows that changes have been made
				changesMade();
			}	
		}
		// Add a trigger
		else if (eventName.equals("Add a new Trigger"))	{
			TriggerModifyAddEdit triggerMod=new TriggerModifyAddEdit(this,null);
			// Has a trigger changed ?
			if (triggerMod.isChangedTriggers()==true)	{
				triggerList.add(triggerMod.exposeTrigger());
				// Ensure the object knows that changes have been made
				changesMade();
			}
		}	
	}
	
	// Delete a trigger
	private void deleteTrigger ()	{
		// Find the name of the selected Trigger
		String selectedTriggerName=(String) triggerComboList.getSelectedItem();
		// Get the list index of this trigger
		int index=getTriggerIndex(selectedTriggerName);
		// Check if anything has been selected
		if (index==-1)	{
			JOptionPane.showMessageDialog(null,"Please select the Trigger you wish to delete.","Rivet", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// Check the user really wants to do this
		String chkMsg="Are you sure you wish to delete the trigger : "+selectedTriggerName;
		if (JOptionPane.showConfirmDialog(this,chkMsg,"Rivet",JOptionPane.YES_NO_OPTION)==0)	{
			// Delete the trigger
			triggerList.remove(index);
			// Ensure the object knows that changes have been made
			changesMade();
		}
	}

	// Inform the main program that the triggers have changed
	public boolean isChangedTriggers() {
		return changedTriggers;
	}
	
	// Trigger changes have been made
	private void changesMade()	{
		// Record this so the main program knows changes have been made
		this.changedTriggers=true;
		// Redraw the trigger combo box
		triggerComboList.removeAllItems();
		populateTriggerCombo();
	}

	// Match a trigger name with its list index else return -1 if nothing found
	private int getTriggerIndex (String tName)	{
		int a;
		for (a=0;a<triggerList.size();a++)	{
			if (triggerList.get(a).getTriggerDescription().equals(tName)) return a;
		}
		return -1;
	}
	

}
