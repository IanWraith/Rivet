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

import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * @author Andy
 * A wrapper class for switching between audio inputs (mixers)
 *
 */
class AudioMixer{
	public String description;
	public Mixer mixer;
	public TargetDataLine line;
	public Line.Info lineInfo;
	public AudioFormat format = null;
	private String errorMsg;
	
	public AudioMixer () {
		audioDebugDump("Start up");
		setDefaultLine();
	}
	
	public AudioMixer(String x, Mixer m, Line.Info l){
		this.description = x;
		this.mixer = m;
		this.lineInfo = l;
	}
	
	/**
	 * Setters and getters
	 */
	public Mixer getMixer() {
		return mixer;
	}

	public void setMixer(Mixer mixer) {
		this.mixer = mixer;
	}
	
	public TargetDataLine getLine() {
		return line;
	}

	public void setLine(TargetDataLine line) {
		this.line = line;
	}
	
	/**
	 * Set the audio format required
	 * @return
	 */
	public void setAudioFormat (AudioFormat tformat) {
		this.format=tformat;
	}
	
	private AudioFormat getFormat()	{
		if (this.format==null)	return new AudioFormat(8000,16,1,true,true);
		else return this.format;
	}
	
	/**
	 * Set the default line for the default mixer
	 */
	public void setDefaultLine(){
	    Mixer mx = AudioSystem.getMixer(null);	//default mixer
	    this.setMixer(mx);
		DataLine.Info info = getDataLineInfo();	
		try	{
			this.line = (TargetDataLine) AudioSystem.getLine(info);
		}catch(LineUnavailableException ex){
			String err="setDefaultLine() : "+ex.getMessage();
			audioDebugDump(err);
		}
	}
	
	
	/**
	 * Get the DataLine.info object for the TargetDataLine 
	 * @return
	 */
	private DataLine.Info getDataLineInfo(){
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, this.format); // format is an AudioFormat object
		if (!AudioSystem.isLineSupported(info)) {
			// Record the error
			audioDebugDump("getDataLineInfo() : Error");
		}
		return info;
	}
	
	/**
	 * Gets a data line for the specified mixer
	 * @param mix
	 * @return
	 */
	public Line getDataLineForMixer(){
		TargetDataLine line = null;
		try {
			line=(TargetDataLine)this.mixer.getLine(getDataLineInfo());
		} catch (LineUnavailableException e) {
			// Record the error
			String err="getDataLineForMixer() : "+e.getMessage();
			audioDebugDump(err);
		}
		
		return line;
	}
	
	/**
	 * Open the current line
	 */
	public boolean openLine(){
		try {
			this.line.open(getFormat());
			return true;
		} catch (LineUnavailableException e) {
			// Record the error
			errorMsg="openLine() : "+e.getMessage();
			audioDebugDump(errorMsg);
			return false;
		}
	}
	
	/**
	 * Change the mixer and restart the TargetDataLine
	 * @param mixerName
	 */
	public boolean changeMixer(String mixerName) {
		Mixer mx=null;
		try	{
			//stop current line
			this.line.stop();
			this.line.close();
			this.line.flush();
			//set the new mixer and line
			mx=AudioSystem.getMixer(getMixerInfo(mixerName));
			this.setMixer(mx);
			this.line=(TargetDataLine) getDataLineForMixer();
			//restart
			if (openLine()==false) return false;
			this.line.start();
		}
		catch (Exception e)	{
			// Record the exception
			errorMsg="changeMixer() : "+e.getMessage();
			// then if a mixer has been obtained then display some information about it
			if (mx!=null)	{
				Mixer.Info mInfo=mx.getMixerInfo();
				errorMsg=errorMsg+"\nMixer Name : "+mInfo.getName()+"\nMixer Description : "+mInfo.getDescription();
			}
			audioDebugDump(errorMsg);
			return false;
		}
		return true;
	}
	
	/**
	 * Get the MixerInfo based on the mixer name
	 * @param mixerName
	 * @return
	 */
	public Mixer.Info getMixerInfo(String mixerName){
		Mixer.Info mixers[]=AudioSystem.getMixerInfo();
		audioDebugDump("getMixerInfo() : Hunting for "+mixerName);
		// Iterate through the mixers and display TargetLines
		int i;
		for (i=0;i<mixers.length;i++){
			Mixer m=AudioSystem.getMixer(mixers[i]);
			audioDebugDump("getMixerInfo() : Found "+m.getMixerInfo().getName()+" + "+m.getMixerInfo().getDescription());
			// Ensure that only sound capture devices can be selected
			boolean isCaptureDevice=m.getMixerInfo().getDescription().endsWith("Capture");
			if ((m.getMixerInfo().getName().equals(mixerName))&&(isCaptureDevice==true)){
				audioDebugDump("getMixerInfo() : Match !");
				return m.getMixerInfo();
			}
		}
		//if no mixer found, returns null which is the default mixer on the machine
		audioDebugDump("getMixerInfo() : Nothing found !");
		return null;
	}

	// Return any error message
	public String getErrorMsg() {
		return errorMsg;
	}
	
	public void stopAudio ()	{
		if (this.line.isOpen()==true)	{
			this.line.stop();
			this.line.close();
		}
	}
	
	// Record audio mixer errors
	private void audioDebugDump (String line)	{
	    try	{
	    	Date now=new Date();
			DateFormat df=DateFormat.getTimeInstance();
	    	FileWriter dfile=new FileWriter("audioDebug.txt",true);
	    	dfile.write(df.format(now)+" "+line);
	    	dfile.write("\r\n");
	    	dfile.flush();  
	    	dfile.close();
	    	}
	    catch (Exception e)	{
	    		System.err.println("Error: " + e.getMessage());
	    	}
		}
	

}
