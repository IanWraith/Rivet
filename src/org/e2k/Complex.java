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
    
    public double returnFull ()	{
    	return Math.sqrt(Math.pow(this.real,2.0)+Math.pow(this.imag,2.0));
    }

}
