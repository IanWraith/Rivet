package org.e2k;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class TriggerModifyAddEdit extends JDialog implements ActionListener {
	
	public static final long serialVersionUID=1;
	private Trigger trigger;
	private JTextField descriptionField;
	private JTextField sequenceField;
	private JTextField forwardGrabField;
	private JTextField backwardGrabField;
	private JComboBox<String> triggerTypeCombo=new JComboBox<String>();
	private JButton okButton=new JButton("OK");
	private JButton cancelButton=new JButton("Cancel");
	private boolean changedTriggers=false;
	
	public TriggerModifyAddEdit (JDialog mf,Trigger Ttrigger)	{
		super(mf,"Add or Edit Trigger",true);	
		this.setSize(650,200);
		// Position the dialog box in the centre of the screen
		final Toolkit toolkit=Toolkit.getDefaultToolkit();
		final Dimension screenSize=toolkit.getScreenSize();
		final int x=(screenSize.width-this.getWidth())/2;
		final int y=(screenSize.height-this.getHeight())/2;
		this.setLocation(x,y);
		this.setLayout(new GridLayout(6,2));
		// Trigger description
		JLabel labelDescription=new JLabel("Trigger Description : ");
		labelDescription.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(labelDescription);
		descriptionField=new JTextField(50);
		this.add(descriptionField);
		// Trigger type combo box
		JLabel labelCombo=new JLabel("Trigger Type : ");
		labelCombo.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(labelCombo);
		// Populate the trigger type combo box
		triggerTypeCombo.addItem("Start");
		triggerTypeCombo.addItem("End");
		triggerTypeCombo.addItem("Grab");
		triggerTypeCombo.addActionListener(this);
		this.add(triggerTypeCombo);
		// The sequence field
		JLabel sequenceDescription=new JLabel("Trigger Sequence : ");
		sequenceDescription.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(sequenceDescription);
		sequenceField=new JTextField(150);
		this.add(sequenceField);
		// Forward grab value
		JLabel forwardGrabDescription=new JLabel("Forward Grab (bits) : ");
		forwardGrabDescription.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(forwardGrabDescription);
		forwardGrabField=new JTextField(3);
		this.add(forwardGrabField);
		// Backward grab value
		JLabel backwardGrabDescription=new JLabel("Backward Grab (bits) : ");
		backwardGrabDescription.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(backwardGrabDescription);
		backwardGrabField=new JTextField(3);
		this.add(backwardGrabField);
		// The OK and Cancel buttons
		this.add(okButton);
		okButton.addActionListener(this);
		this.add(cancelButton);
		cancelButton.addActionListener(this);
		// Populate the fields
		// If this is an edit set the values of the various fields to the selected Trigger
		if (Ttrigger!=null)	{
			// Copy the trigger object
			trigger=Ttrigger;
			// Description
			descriptionField.setText(trigger.getTriggerDescription());
			// Type
			triggerTypeCombo.setSelectedIndex(trigger.getTriggerType()-1);
			// Sequence
			sequenceField.setText(trigger.getTriggerSequence());
			// Forward Grab
			forwardGrabField.setText(Integer.toString(trigger.getForwardGrab()));
			// Backward Grab
			backwardGrabField.setText(Integer.toString(trigger.getBackwardGrab()));
		}
		else	{
			// Create a new trigger object
			trigger=new Trigger();
			// Description
			descriptionField.setText("Trigger Name");
			// Forward Grab
			forwardGrabField.setText("0");
			// Backward Grab
			backwardGrabField.setText("0");
		}
		// Display the dialog box
		this.setVisible(true);	
	}
	
	// Handle all actions
	public void actionPerformed (ActionEvent event) {
			String eventName=event.getActionCommand();	
			// Cancel
			if (eventName.equals("Cancel"))	{
				changedTriggers=false;
				dispose();
			}
			// OK
			if (eventName.equals("OK"))	{	
				// Check the entries
				if (checkTrigger()==true)	{
					changedTriggers=true;
					dispose();
				}
			}
	}
	
	// Inform the other dialog box that the trigger has been changed
	public boolean isChangedTriggers() {
			return changedTriggers;
	}
	
	// Check that the users entry is OK
	boolean checkTrigger ()	{
		int forwardGrab=0,backwardGrab=0;
		// Description
		String description=descriptionField.getText();
		if ((description==null)||(description.length()<1))	{
			JOptionPane.showMessageDialog(null,"You must enter a description for a trigger","Rivet", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		// Sequence
		String sequence=sequenceField.getText();
		if ((sequence==null)||(sequence.length()<4))	{
			JOptionPane.showMessageDialog(null,"You must enter a binary sequence of 3 or more bits for a valid trigger","Rivet", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		// Only need to get the forward and backward values if the trigger is a grab
		int triggerType=triggerTypeCombo.getSelectedIndex()+1;
		if (triggerType==3)	{
			// Forward grab
			String forward=forwardGrabField.getText();
			try	{
				forwardGrab=Integer.parseInt(forward);
			}
			catch (Exception e)	{
				JOptionPane.showMessageDialog(null,"A Grab Trigger must have a numeric forward grab value","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check the forward value is positive
			if (forwardGrab<0)	{
				JOptionPane.showMessageDialog(null,"A Grab Trigger must not contain a negative numeric forward grab value","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Backward grab
			String backward=backwardGrabField.getText();
			try	{
				backwardGrab=Integer.parseInt(backward);
			}
			catch (Exception e)	{
				JOptionPane.showMessageDialog(null,"A Grab Trigger must have a numeric backward grab value","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check the forward value is positive
			if (backwardGrab<0)	{
				JOptionPane.showMessageDialog(null,"A Grab Trigger must not contain a negative numeric backward grab value","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		}
		// Populate the trigger object
		trigger.setTriggerDescription(description);
		trigger.setTriggerSequence(sequence);
		trigger.setTriggerType(triggerType);
		trigger.setForwardGrab(forwardGrab);
		trigger.setBackwardGrab(backwardGrab);
		// All done
		return true;
	}
	
	// Return the added or edited trigger to the main dialog box
	public Trigger exposeTrigger()	{
		return this.trigger;
	}
	

}
