package org.e2k;

public class Trigger {
	
	private String triggerSequence;
	private String triggerDescription;
	private int triggerType=-1;
	private boolean active=false;
	
	public String getTriggerSequence() {
		return triggerSequence;
	}
	
	public void setTriggerSequence(String triggerSequence) {
		this.triggerSequence = triggerSequence;
	}
	
	public String getTriggerDescription() {
		return triggerDescription;
	}
	
	public void setTriggerDescription(String triggerDescription) {
		this.triggerDescription = triggerDescription;
	}
		
	public int getTriggerType() {
		return triggerType;
	}
	
	public void setTriggerType(int triggerType) {
		this.triggerType = triggerType;
	}
	
	// Return true if this triggers sequence appears in the circularBitSet
	public boolean triggerMatch(CircularBitSet cBitSet)	{
		// If this Trigger isn't active return false straight away
		if (active==false) return false;
		// Get the last sLength bits from the circular buffer
		String cur=cBitSet.extractSequence(triggerSequence.length());
		// Is this the same as the trigger sequence ?
		if (triggerSequence.equals(cur)) return true;
		else return false;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	
}
