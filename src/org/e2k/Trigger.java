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

// Trigger types
// 1 - Start 
// 2 - End
// 3 - Grab

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
	
	// Return backwardGrab values as a string
	public String getBackwardBitsString (CircularBitSet cBitSet)	{
		// Extract the trigger string and backward grap bits
		String cur=cBitSet.extractSequence(triggerSequence.length()+backwardGrab);
		// Chop away the trigger string leaving just the prior bits
		return cur.substring(0,backwardGrab);
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
