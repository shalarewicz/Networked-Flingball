package flingball.gadgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import flingball.Ball;
import flingball.BoardAnimation;
import flingball.Orientation;
import physics.Angle;
import physics.Circle;
import physics.Physics;
import physics.Vect;

public class RightFlipper implements Bumper {
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
	private final Orientation orientation;
	private String trigger = NO_TRIGGER; 
	private double reflectionCoeff = DEFAULT_FLIPPER_REFLECTION_COEFF;
	private Circle pivot, tail;
	private Wall port, starboard;
	
	
	private final int angularVelocity = 1080;
	private boolean rotating = false;
	private boolean rotated = false;
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
		assert slopePort == slopeStar || (Double.isNaN(slopePort) && Double.isNaN(slopeStar)): 
			"Walls of RightFlipper " + name + " are not parallel";
		
		final Vect portPivot = port.start();
		final Vect portTail = port.end();
		final Vect starPivot = starboard.start();
		final Vect starTail = starboard.end();
		final double pivotRadius = pivot.getRadius();
		final double tailRadius = tail.getRadius();
		final Vect pivotCenter = pivot.getCenter();
		final Vect tailCenter = tail.getCenter();
		
		assert pivotRadius == tailRadius && pivotRadius == RADIUS : name + ": Pivot/Tail radius != RADIUS";
		
		// Error tolerance of 0.001 is allowed to prevent floating point math errors
		// Check if port and starboard are tangential (distance from endpoints to center of circles equals radius)
		// Because lines are parallel only need to check and end point of each line. 
		assert Math.abs(Math.sqrt(portPivot.distanceSquared(pivotCenter)) - pivotRadius) < 0.001 : name + ": Port flipper wall bisects pivot";
		assert Math.abs(Math.sqrt(starPivot.distanceSquared(pivotCenter)) - pivotRadius) < 0.001 : name + ": Starboard flipper wall bisects pivot";
		
		// Check port and starboard same length
		assert Math.abs(portPivot.distanceSquared(portTail) - starPivot.distanceSquared(starTail)) < 0.001 : name + ": Port and Starboard different length";
		
		// Check distance between the centers of pivot and tail must be equal to the length of port/starboard
		assert Math.abs(portPivot.distanceSquared(portTail) - pivotCenter.distanceSquared(tailCenter)) < 0.001 : name + ": Length of wall greather than distance between centers";
		
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
			this.starboard = new Wall(name + ": port", x + WIDTH - RADIUS, -y - HEIGHT, x + RADIUS, -y - HEIGHT);
			this.port = new Wall(name + ": starboard", x + WIDTH - RADIUS, -y - HEIGHT + 2*RADIUS, x + RADIUS, -y - HEIGHT + 2*RADIUS);
			
			break;
		}
		case ONE_EIGHTY: {
			this.tail = new Circle(x + RADIUS, -y - RADIUS, RADIUS);
			this.pivot = new Circle(x + RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.starboard = new Wall(name + ":port", (double) x, -y - HEIGHT + RADIUS, (double) x, -y - RADIUS);
			this.port = new Wall(name + ":starboard", x + 2*RADIUS, -y - HEIGHT + RADIUS, x + 2 * RADIUS, -y - RADIUS);
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
			
			throw new RuntimeException("Should never get here. Invalid RightFlipper Orientation");
		}
		
		}
		checkRep();
		
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
		return new Vect(this.x, -this.y);
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	@Override
	public int width() {
		return WIDTH;
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
		double collisionTime = ball.timeUntilCircleCollision(this.pivot);
		if (this.rotating && !this.rotated) {
			//Clockwise rotation from default orientation to rotated orientation
			collisionTime = Math.min(ball.timeUntilRoatatingCircleCollision(this.tail, this.pivot.getCenter(), -angularVelocity), collisionTime);
			collisionTime = Math.min(this.port.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), -angularVelocity), collisionTime);
			collisionTime = Math.min(this.port.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), -angularVelocity),collisionTime);
			
		} else if (this.rotating) {
			//Clockwise rotation from default orientation to rotated orientation
			collisionTime = Math.min(ball.timeUntilRoatatingCircleCollision(this.tail, this.pivot.getCenter(), angularVelocity), collisionTime);
			collisionTime = Math.min(this.port.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), angularVelocity), collisionTime);
			collisionTime = Math.min(this.port.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), angularVelocity),collisionTime);
			
		} else {
			//No rotation
			collisionTime = Math.min(ball.timeUntilCircleCollision(tail), collisionTime);
			collisionTime =  Math.min(this.port.collisionTime(ball), collisionTime);
			collisionTime = Math.min(this.starboard.collisionTime(ball), collisionTime);
		}
		return collisionTime;
	}

	@Override
	public void reflectBall(Ball ball) {
		double collisionTime = this.collisionTime(ball);
		if (collisionTime == ball.timeUntilCircleCollision(this.pivot)) {
			ball.reflectCircle(this.pivot);
			return;
		}
		
		// TODO Should probably go with whatever wall has the smallest collision time. 
		if (this.rotating && !this.rotated) {
			//Clockwise rotation from default orientation to rotated orientation
			if (collisionTime == ball.timeUntilRoatatingCircleCollision(tail, pivot.getCenter(), -angularVelocity)) {
				ball.reflectRotatingCircle(tail, pivot.getCenter(), -angularVelocity, reflectionCoeff);
			} else if (collisionTime == port.timeUntilRotatingWallCollision(ball, pivot.getCenter(), -angularVelocity)) {
				this.port.reflectBallRotating(ball, pivot.getCenter(), -angularVelocity, reflectionCoeff);
			} else if (collisionTime == starboard.timeUntilRotatingWallCollision(ball, pivot.getCenter(), -angularVelocity)) {
				this.starboard.reflectBallRotating(ball, pivot.getCenter(), -angularVelocity, reflectionCoeff);
			} else {
				throw new RuntimeException("Rotating RightFlipper counterclockwise reflection should never get here");
			}
			
		} else if (this.rotating) {
			//Counterclockwise rotation from default orientation to rotated orientation
			if (collisionTime == ball.timeUntilRoatatingCircleCollision(tail, pivot.getCenter(), angularVelocity)) {
				ball.reflectRotatingCircle(tail, pivot.getCenter(), angularVelocity, reflectionCoeff);
			} else if (collisionTime == port.timeUntilRotatingWallCollision(ball, pivot.getCenter(), angularVelocity)) {
				this.port.reflectBallRotating(ball, pivot.getCenter(), angularVelocity, reflectionCoeff);
			} else if (collisionTime == starboard.timeUntilRotatingWallCollision(ball, pivot.getCenter(), angularVelocity)) {
				this.starboard.reflectBallRotating(ball, pivot.getCenter(), angularVelocity, reflectionCoeff);
			} else {
				throw new RuntimeException("Rotating RightFlipper clockwise reflection should never get here");
			}
		}
		else {
			// Flipper is not rotating
			if (collisionTime == ball.timeUntilCircleCollision(tail)) {
				ball.reflectCircle(tail);
			} else if (collisionTime == port.collisionTime(ball)){
				this.port.reflectBall(ball);
			} else if (collisionTime == starboard.collisionTime(ball)){
				this.starboard.reflectBall(ball);
			} else {
				throw new RuntimeException("RightFlipper " + name + " reflection should never get here");
			}
			
		}

	}
	
	@Override
	public String getTrigger() {
		return this.trigger;
	}
	
	@Override
	public void setTrigger(String trigger) {
		this.trigger = trigger;
	}
	@Override
	public void takeAction() {
		if (this.rotating) return; 
		new Thread(() ->  {
			if (!this.rotating) {
				this.rotating = true;
				Angle degreeIncrement = new Angle(Math.PI / 180 * this.angularVelocity * BoardAnimation.FRAME_RATE / 1000); 
				Angle totalRotationAngle = Angle.DEG_90;
				
				if (!this.rotated) {
					totalRotationAngle = Angle.DEG_270;
					degreeIncrement = Angle.RAD_PI.plus(Angle.RAD_PI).minus(degreeIncrement);
				}
				final Circle finalTail = Physics.rotateAround(this.tail, this.pivot.getCenter(), totalRotationAngle);
				final Wall finalPort = this.port.rotateAround(this.pivot.getCenter(), totalRotationAngle);
				final Wall finalStarboard = this.starboard.rotateAround(this.pivot.getCenter(), totalRotationAngle);
				
				while (true) {
					if (degreesRotated.plus(degreeIncrement).compareTo(Angle.RAD_PI_OVER_TWO) >= 0) {
						this.tail = finalTail;
						this.port = finalPort;
						this.starboard = finalStarboard;
						this.rotated = !this.rotated;
						this.rotating = false;
						//TODO reset degrees rotated to zero?
						break;
					} else {
						this.rotate(degreeIncrement);
					}
					try {
						Thread.sleep(BoardAnimation.FRAME_RATE);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}).start();
	}
	
	/**
	 * Rotates at a constant angular velocity of 1080 degrees per second to a position 90 degrees away from its starting position 
	 * in alternating counterclockwise and clockwise directions. 
	 * @param angle by which the flipper is rotated
	 */
	private void rotate(Angle angle) {
			// Rotate counterclockwise
			this.tail = Physics.rotateAround(this.tail, this.pivot.getCenter(), angle);
			this.port = port.rotateAround(this.pivot.getCenter(), angle);
			this.starboard = starboard.rotateAround(this.pivot.getCenter(), angle);
			this.degreesRotated = this.degreesRotated.plus(angle);
			checkRep();
			
	}

	

	@Override
	public boolean ballOverlap(Ball ball) {
		return Physics.distanceSquared(ball.getCartesianCenter(), this.tail.getCenter()) >= ball.getRadius() &&
			   Physics.distanceSquared(ball.getCartesianCenter(), this.pivot.getCenter()) >= ball.getRadius();
	}

	@Override
	public BufferedImage generate(int L) {
		BufferedImage output = new BufferedImage(2 * L, 2 * L, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        graphics.setColor(Color.ORANGE);
        
        
        // Use anchors to shift positions so an image can be drawn on a 2 x 2 grid
        final int xAnchor = this.x;
        final int yAnchor = -this.y;
        
        int xpivot = (int) ((this.pivot.getCenter().x() - xAnchor - RADIUS) * L);
        int ypivot = (int) ((-this.pivot.getCenter().y() - yAnchor- RADIUS) * L);
        graphics.fillArc(xpivot, ypivot, (int) (RADIUS * 2 * L), (int) (RADIUS * 2 * L), 0, 360);
        
        int xtail = (int) ((this.tail.getCenter().x() - xAnchor - RADIUS) * L);
        int ytail = (int) ((-this.tail.getCenter().y() - yAnchor - RADIUS) * L);
        graphics.fillArc(xtail, ytail, (int) (RADIUS * 2 * L), (int) (RADIUS * 2 * L), 0, 360);
        
        final int[] xPoints = {
        		(int) ((this.port.start().x() - xAnchor) * L), 
        		(int) ((this.port.end().x() - xAnchor) * L),
        		(int) ((this.starboard.end().x() - xAnchor) * L), 
        		(int) ((this.starboard.start().x() - xAnchor) * L) 
        		};
        final int[] yPoints = {
        		(int) ((-this.port.start().y() - yAnchor) * L), 
        		(int) ((-this.port.end().y() - yAnchor) * L),
        		(int) ((-this.starboard.end().y() - yAnchor) * L), 
        		(int) ((-this.starboard.start().y() - yAnchor) * L)
        		};
        graphics.fillPolygon(xPoints, yPoints, 4);
        return output;
	}

	@Override
	public void setCoverage(int[][] coverage) {
		int x = (int) this.position().x();
		int y = (int) this.position().y();
		coverage[y][x] = 1;
		for (int i = x; x < x + WIDTH; i++) {
			for (int j = y; j < y + HEIGHT; j++) {
				assert coverage[j][i] != 1 : name + ": RightFlipper coverage overlap";
				coverage[j][i] = 1;
			}
		}
	}

	@Override
	public String toString() {
		return "RightFlipper: " + name + " " + this.position();
	}
	
	@Override 
	public boolean equals(Object that) {
		return that instanceof RightFlipper && this.sameParts((RightFlipper) that);
	}

	private boolean sameParts(RightFlipper that) {
		return this.x == that.x &&
				this.y == that.y &&
				this.orientation == that.orientation;
	}

}