package flingball.gadgets;

import java.awt.image.BufferedImage;

import flingball.Ball;
import flingball.Orientation;
import physics.Angle;
import physics.Circle;
import physics.Vect;

public class RightFlipper implements Gadget {
	/**
	 * A gadget which can be used on a flingball board. A right flipper has size 1 L x 2 L. 
	 * a Left Flippers action rotates the flipper about it's pivot point which is in the 
	 * northeast corner by default. The opposite end of the flipper is in the southeast
	 * corner. A flipper is bound by a box of size 2L x 2L and does not cross the boundary 
	 * of this box at any point. The anchor of a right flipper is found in the upper right-hand 
	 * corner of it's bounding box. 
	 * 
	 * A right flipper's action alternates between rotating the flipper 90 degrees in the
	 * clockwise direction and 90 degrees in the counterclockwise direction about it's pivot. 
	 * During this time a flipper rotates at a speed of 1080 degrees per second. 
	 * 
	 * A right flipper has a default coefficient of reflection of 0.95. However, if a ball collides
	 * with the flipper during its rotation the linear velocity of the flipper is taken into account.
	 * 
	 * A right flipper's orientation can be set in 90 degree intervals therefore a right flipper with 
	 * an orientation of 270 degrees would place the pivot in the northwest corner and the opposite 
	 * end int the northeast corner with rotation moving the end to the southwest corner. 
	 *
	 * 
	 */
	
	
	private final int x, y;
	private final String name;
	private final String trigger = NO_TRIGGER; //TODO Why is this final?
	private double reflectionCoeff = DEFAULT_FLIPPER_REFLECTION_COEFF;
	private  Circle pivot, tail;
	private  Wall port, starboard;
	private final int angularVelocity = 1080;
	private Orientation orientation = Orientation.ZERO;
	private boolean rotating = true;
	private Angle degreesRotated = Angle.ZERO;
	
	private final static double RADIUS = 0.25;
	private final static int HEIGHT = 2;
	private final static int WIDTH = 2;
	
	/*
	 * AF(name, x, y, pivot, tail, port starboard) ::= A flipper called name with anchor (x,-y) and an 
	 * ovular shape depicted below
	 * 
	 *   ____port______
	 *  /			   \	
	 * (tail	   pivot)
	 *  \__starboard___/
	 *  
	 * Rep Invariant ::= 
	 * 	 1. Port and starboard must be tangential to pivot ant tail 
	 *   2. Port and starboard are parallel to each other. 
	 * 	 3. Port and starboard must have equal lengths. 
	 * 	 4. The line connecting the endpoints of port and starboard must intersect the center of pivot or tail
	 * 	 5. The distance between the centers of pivot and tail must be equal to the length of port/starboard
	 *   6. Pivot and Tail have the same radius
	 * 	
	 * Safety from rep exposure:
	 * 		TODO
	 * 
	 * Thread Safety Argument
	 * 		TODO Immutable	
	 */
	
	private void checkRep() {
		/*
		 * Rep Invariant is satisfied if 
		 * 		pivot and tail have same radius (6)
		 * 		port and starboard are parallel (2) and separated by 2*radius and of equal length (3)
		 * 		endpoints of port and starboard are radius away from centers of pivot/tail
		 * 		pivot/tail centers are separated by length of port/starboard (5)
		 * 
		 * 1. is satisfied as port/starboard are parallel and 2*radius apart and the endpoints of each line 
		 *    are radius away from the center of pivot/tail respectively
		 * 4. As port/starboard are parallel of equal length, tangential to pivot tail and separated by 2*radius
		 *    then the line connecting the endpoints muct bisect the center of pivot/tail respectively. 
		 */
		// Check port and starboard are parallel (equal slopes)
		final double slopePort = slope(port);
		final double slopeStar = slope(starboard);
		assert slopePort == slopeStar : "Walls of RightFlipper " + name + " are not parallel";
		
		final Vect portPivot = port.start();
		final Vect portTail = port.end();
		final Vect starPivot = port.start();
		final Vect starTail = port.end();
		final double pivotRadius = pivot.getRadius();
		final double tailRadius = tail.getRadius();
		final Vect pivotCenter = pivot.getCenter();
		final Vect tailCenter = tail.getCenter();
		
		// Check if port and starboard are tangential (distance from endpoints to center of circles equals radius)
		assert Math.sqrt(portPivot.distanceSquared(pivotCenter)) == pivotRadius : name + ": Port flipper wall bisects pivot";
		assert Math.sqrt(portTail.distanceSquared(tailCenter)) == tailRadius : name + ": Port flipper wall bisects tail";
		//TODO Because port/starboard are parallel this second check is unnecessary??
		assert Math.sqrt(starPivot.distanceSquared(pivotCenter)) == pivotRadius : name + ": Starboard flipper wall bisects pivot";
		assert Math.sqrt(starPivot.distanceSquared(tailCenter)) == tailRadius : name + ": Starboard flipper wall bisects tail";
		
		// Check pivot and tail same size
		assert pivotRadius == tailRadius : name + ": Pivot and Tail different sizes";
		
		// Check port and starboard same length
		assert portPivot.distanceSquared(portTail) == starPivot.distanceSquared(starTail) : name + ": Port and Starboard different length";
		
		// Check that distance between the lines is equal to the diameter of pivot/tail. 
		assert Math.sqrt(portPivot.distanceSquared(starPivot)) == pivotRadius * 2;
		
		// Check distance between the centers of pivot and tail must be equal to the length of port/starboard
		assert portPivot.distanceSquared(portTail) == pivotCenter.distanceSquared(tailCenter);
		
	}
	
	
	public RightFlipper(String name, int x, int y, Orientation o) {
		this.name = name;
		this.x = x;
		this.y = -y;
		this.orientation = o;
		
		switch (o) {
		case ZERO: {
			this.tail = new Circle(x + WIDTH - RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.pivot = new Circle(x + WIDTH - RADIUS, -y - RADIUS, RADIUS);
			this.starboard = new Wall(name + ": port", x + WIDTH, -y - RADIUS, x + WIDTH, -y - HEIGHT + RADIUS);
			this.port = new Wall(name + ": starboard", x + WIDTH - 2*RADIUS, -y - RADIUS, x + WIDTH - 2*RADIUS, -y -2 + RADIUS);
			
			break;
		}
		case NINETY: {
			this.tail = new Circle(x + RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.pivot = new Circle(x + WIDTH - RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.starboard = new Wall(name + ": port", x + RADIUS, -y - HEIGHT, x + WIDTH - RADIUS, -y - HEIGHT);
			this.port = new Wall(name + ": starboard", x + RADIUS, -y - HEIGHT + 2*RADIUS, x + WIDTH - RADIUS, -y - HEIGHT + 2*RADIUS);
			
			break;
		}
		case ONE_EIGHTY: {
			this.tail = new Circle(x + RADIUS, -y - RADIUS, RADIUS);
			this.pivot = new Circle(x + RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.starboard = new Wall(name + ":port", (double) x, -y -RADIUS, (double) x, -y - HEIGHT + RADIUS);
			this.port = new Wall(name + ":starboard", x + 2*RADIUS, -y -RADIUS, x + 2 * RADIUS, -y - HEIGHT + RADIUS);
			break;
		}
		case TWO_SEVENTY: {
			this.tail = new Circle(x + WIDTH - RADIUS, -y - RADIUS, RADIUS);
			this.pivot = new Circle(x + RADIUS, -y - RADIUS, RADIUS);
			this.starboard = new Wall(name + ": port", x + RADIUS, -y - 2*RADIUS, x + WIDTH - RADIUS, -y - 2*RADIUS);
			this.port = new Wall(name + ": starboard", x + RADIUS, (double) -y, x + WIDTH - RADIUS, (double) -y);
			break;
		}
		default: {
			
			throw new RuntimeException("Should never get here. Invalid LeftFlipper Orientation");
		}
		}
		
	}
	
	/**
	 * Returns the slope of a wall
	 * @param wall 
	 * @return the slope
	 */
	private double slope (Wall wall) {
		final double x1 = wall.start().x();
		final double x2 = wall.end().x();
		final double y1 = wall.start().x();
		final double y2 = wall.end().x();
		
		return (y2 - y1) / (x2 - x1);
	}
	
	@Override
	public Vect position() {
		return new Vect(x, y);
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public int height() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int width() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getReflectionCoefficient() {
		return this.reflectionCoeff;
	}

	@Override
	public void setReflectionCoefficient(double x) {
		this.reflectionCoeff = x;

	}

	@Override
	public double collisionTime(Ball ball) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void reflectBall(Ball ball) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTrigger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void takeAction() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean ballOverlap(Ball ball) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public BufferedImage generate(int L) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCoverage(int[][] coverage) {
		// TODO Auto-generated method stub

	}

}