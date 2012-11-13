package org.e2k;

public class Trigger {
	
	private String triggerSequence;
	private String triggerDescription;
	private int triggerType=-1;
	private boolean active=false;
	private int forwardGrab=0;
	private int backwardGrab=0;
	
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
	
	// Return a description of the trigger type
	public String getTypeDescription()	{
		if (this.triggerType==1) return " (START)";
		else if (this.triggerType==2) return " (END)";
		else if (this.triggerType==3) return " (GRAB)";
		else return " (UNKNOWN)";
	}

	public int getForwardGrab() {
		return forwardGrab;
	}

	public void setForwardGrab(int forwardGrab) {
		this.forwardGrab = forwardGrab;
	}

	public int getBackwardGrab() {
		return backwardGrab;
	}

	public void setBackwardGrab(int backwardGrab) {
		this.backwardGrab = backwardGrab;
	}
	
	
}
