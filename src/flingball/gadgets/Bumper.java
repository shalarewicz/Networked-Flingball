package flingball.gadgets;

/**
 * Bumpers are a type of Gadget that will reflect a ball. Bumpers have all of the same properties of a 
 * Gadget but can also have a Coefficient of Reflection. Bumpers cannot store or transfer balls. 
 * The default value is 1.0 for all bumpers except flippers where the default is 0.95.
 * @author Stephan Halarewicz
 */
public interface Bumper extends Gadget {
	
	/**
	 * Returns the coefficient of reflection. 
	 * 
	 * @return The coefficient of reflection of this bumper. 
	 */
	public double getReflectionCoefficient();
	
	/**
	 * Sets the coefficient of reflection for this bumper. 
	 * @param x the new coefficient of reflection. 
	 */
	public void setReflectionCoefficient(double x);
	
	public final static double DEFAULT_REFLECTION_COEFF = 1.0;
	public final static double DEFAULT_FLIPPER_REFLECTION_COEFF = 0.95;
	
}
