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

import java.io.File;

//This class extends filechoose so only .bsf files can be selected
public class BitStreamFileFilter extends javax.swing.filechooser.FileFilter {
	public boolean accept(File f) {
		// if it is a directory -- we want to show it so return true.
		if (f.isDirectory())
			return true;
		// get the extension of the file
		String extension = getExtension(f);
		// check to see if the extension is equal to "bsf"
		if (extension.equals("bsf"))
			return true;
		// default -- fall through. False is return on all
		// occasions except:
		// a) the file is a directory
		// b) the file's extension is what we are looking for.
		return false;
	}

	/**
	 * Again, this is declared in the abstract class The description of this
	 * filter
	 */
	public String getDescription() {
		return "BSF files";
	}

	/**
	 * Method to get the extension of the file, in lowercase
	 */
	private String getExtension(File f) {
		String s=f.getName();
		int i=s.lastIndexOf('.');
		if (i>0&&i<s.length()-1) return s.substring(i+1).toLowerCase();
		else return "";
	}
}


