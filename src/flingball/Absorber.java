package flingball;

public interface Absorber extends Gadget {
	
	/**
	 * An absorber is a gadget which can be placed on a flingball board. An absorber
	 * is a rectangular gadget which is kL wide and mL tall where k,l <= 20. An 
	 * absorber acts as a ball return mechanism during a game of flingball. When a ball
	 * strikes an absorber the ball is held in the bottom right-hand corner of the 
	 * absorber. There is no limit to the amount of balls an absorber can hold in 
	 * this location. 
	 * 
	 * When an absorber's action is triggered one of it's captured balls (if any) will 
	 * be shot towards the top of the board with a speed of 50 L/s. If the absorber is
	 * not holding a ball or if the previously fired ball has not left the absorber, then
	 * no action is taken. 
	 * 
	 * Absorber's can be made self-triggering by connecting its trigger to its own 
	 * action. Balls that hit a self-triggering absorber will always leave the absorber. 
	 * 
	 */
	
	/**
	 * Fires all balls currently trapped by the absorber
	 */
	public void fireAll();
	
	//TODO Use a factory method to avoid AbsorberC annoyance?
}
