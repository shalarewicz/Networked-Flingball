
package flingball.gadgets;

import java.awt.image.BufferedImage;

import flingball.Ball;

//TODO Create a separate interface for absorber and wall;
// Gadget interface, Bumper, Absorber, Wall interfaces which extend Gadget

/**
 * A Gadget is an object that can be placed on a flingball board or used to construct a flingball board. 
 * 
 * A gadgets position must have coordinates >= 0;
 */
import physics.*;

public interface Gadget {
	
	/**
	 * @return The anchor position of the gadget on the board. The anchor is located in  the 
	 * upper left of the bounding box of the gadget. 
	 */
	public Vect position();
	
	/**
	 * 
	 * @return The name of the gadget
	 */
	public String name();
	
	/**
	 * 
	 * @return The height of the bounding box of the gadget. 
	 */
	public int height();
	
	/**
	 * 
	 * @return The width of the bounding box of the gadget. 
	 */
	public int width();
	
	/**
	 * Calculates the time until a ball collides with the object.
	 * 
	 * @param ball A ball in the same 2D space. 
	 * @return the time in seconds until the ball collides with the gadget. Returns POSITIE_INFINITY
	 * if a collision will not occur. 
	 */
	public double collisionTime(Ball ball);
	
	/**
	 * Performs a collision between a ball and a gadget. 
	 * 
	 * @param ball ball which will collide with the gadget. 
	 */
	public void reflectBall(Ball ball);
	
	/**
	 * 
	 * @return the name of the gadget which is the trigger for this gadget
	 */
	public String getTrigger();
	
	/**
	 * If the gadget has an action associated with it, the action is taken. The board is updated with any side efffects. 
	 * 
	 * @return A gadget that has had it's action taken
	 */
	public void takeAction();
	
	/**
	 * Determines if a ball and a gadget overlap.
	 * TODO Remove this. Not necessary if new balls places in the center of grid spaces. Balls cannot have diameter > 1L
	 * @param ball
	 * @return true if the ball and gadget overlap
	 */
	public boolean ballOverlap(Ball ball);

		
	/**
	 * Generates an image of the gadget. 
	 * 
	 * @return a BufferedImage representation of the gadget. 
	 */
	public BufferedImage generate(int L);
	
	/**
	 * TODO exposes the rep of Board this can be handled when Absorber implements it's own interface
	 * Can fix this by placing random balls at the center of empty grid spaces on the board. 
	 * @param coverage
	 */
	void setCoverage(int[][] coverage);
		
	public final static String NO_TRIGGER = "NO_TRIGGER";
	public final static double DEFAULT_REFLECTION_COEFF = 1.0;
	public final static double DEFAULT_FLIPPER_REFLECTION_COEFF = 0.95;
	
	@Override
	public int hashCode();
	
	@Override
	public String toString();
	
	@Override
	public boolean equals(Object that);
}
