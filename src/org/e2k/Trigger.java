package org.e2k;

public class Trigger {
	
	private String triggerSequence;
	private String triggerDescription;
	private int triggerType=-1;
	
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
	
}
