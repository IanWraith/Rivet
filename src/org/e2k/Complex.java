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

final public class Complex {
	
	private final double real;
	private final double imag;
	
	public Complex () {
        this(0.0,0.0);
    }

    public Complex (double r) {
        this(r,0.0);
    }

    public Complex (double r,double i) {
         this.real=r;
         this.imag=i;
    }

    public Complex add(Complex addend) {
        return new Complex((this.real+addend.real),(this.imag+addend.imag));
    }
    
    public double getReal ()	{
    	return this.real;
    }
    
    public double getImag ()	{
    	return this.imag;
    }
    
    public double getMagnitude ()	{
    	return Math.sqrt(Math.pow(this.real,2.0)+Math.pow(this.imag,2.0));
    }
    
    public double getPhase ()	{
    	return Math.atan((imag/real));
    }

}
