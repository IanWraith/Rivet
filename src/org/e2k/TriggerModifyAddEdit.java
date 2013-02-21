package org.e2k;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class TriggerModifyAddEdit extends JDialog implements ActionListener {
	
	public static final long serialVersionUID=1;
	private Trigger trigger;
	private JTextField descriptionField;
	private JComboBox<String> triggerTypeCombo=new JComboBox<String>();
	private JButton okButton=new JButton("OK");
	private JButton cancelButton=new JButton("Cancel");
	private boolean changedTriggers=false;
	
	public TriggerModifyAddEdit (JDialog mf,Trigger Ttrigger)	{
		super(mf,"Add or Edit Trigger",true);	
		this.setSize(300,400);
		trigger=Ttrigger;
		// Position the dialog box in the centre of the screen
		final Toolkit toolkit=Toolkit.getDefaultToolkit();
		final Dimension screenSize=toolkit.getScreenSize();
		final int x=(screenSize.width-this.getWidth())/2;
		final int y=(screenSize.height-this.getHeight())/2;
		this.setLocation(x,y);
		this.setLayout(new GridLayout(9,1));
		// Trigger description
		JLabel labelDescription=new JLabel("Trigger Description : ");
		this.add(labelDescription);
		descriptionField=new JTextField(50);
		if (trigger!=null) descriptionField.setText(trigger.getTriggerDescription());
		this.add(descriptionField);
		// Trigger type combo box
		JLabel labelCombo=new JLabel("Trigger Type : ");
		this.add(labelCombo);
		// Populate the trigger type combo box
		triggerTypeCombo.addItem("Start");
		triggerTypeCombo.addItem("End");
		triggerTypeCombo.addItem("Grab");
		triggerTypeCombo.addActionListener(this);
		this.add(triggerTypeCombo);
		
		// TODO : Add a JTextArea component for the user to enter the Trigger Sequence
		
		// Gap
		this.add(new JLabel(""));
		// The OK and Cancel buttons
		this.add(okButton);
		okButton.addActionListener(this);
		this.add(cancelButton);
		cancelButton.addActionListener(this);
		
		
		// If this is an edit set the values of the various fields to the selected Trigger
		if (trigger!=null)	{
			// Description
			descriptionField.setText(trigger.getTriggerDescription());
			// Type
			triggerTypeCombo.setSelectedIndex(trigger.getTriggerType()-1);
			
		}
		
		this.setVisible(true);	
	}
	
	
	// Handle all actions
	public void actionPerformed (ActionEvent event) {
			String eventName=event.getActionCommand();	
	}
	
	
	// Inform the other dialog box that the trigger has been changed
	public boolean isChangedTriggers() {
			return changedTriggers;
	}

}
