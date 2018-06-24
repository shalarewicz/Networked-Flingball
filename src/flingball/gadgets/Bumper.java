package flingball.gadgets;

public interface Bumper extends Gadget {
	
	/**
	 * 
	 * @return The coefficient of reflection of the gadget
	 */
	public double getReflectionCoefficient();
	
	/**
	 * Sets the reflectionCoefficient for a gadget. The default value is 1.0 for all
	 * gadgets except flippers where the default is 0.95.
	 * @param sets the reflection coefficient of a gadget. 
	 */
	public void setReflectionCoefficient(double x);
	
}
