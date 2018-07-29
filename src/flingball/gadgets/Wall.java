package flingball.gadgets;

import java.awt.image.BufferedImage;

import flingball.Ball;
import physics.Angle;
import physics.Circle;
import physics.LineSegment;
import physics.Physics;
import physics.Vect;

/** Immutable Type representing a wall on a flingball board. A wall is formed by a straight line
 *  joining two points, start and end and has height zero. The width of a wall is defined by 
 *  the distance between start and end. Balls cannot pass through a wall. 
 *  Walls can be used as the outer walls of a flingball board or the walls of a gadget. 
 *  
 * @author Stephan Halarewicz
 */
public class Wall implements Gadget {

	
	private final String name;
	private final double x1, y1, x2, y2;
	
	private final double reflectionCoeff = Bumper.DEFAULT_REFLECTION_COEFF;
	
	private final LineSegment wall;
	private final Circle c1, c2;
	
	
	/**
	 * Returns a new wall between the starting point (x1, y1) and the end point (x2, y2)
	 * @param name name given to the wall
	 * @param x1 x-coordinate of the starting point of the wall
	 * @param y1 y-coordinate of the starting point of the wall
	 * @param x2 x-coordinate of the end point of the wall
	 * @param y2 y-coordinate of the end point of the wall
	 */
	public Wall(String name, double x1, double y1, double x2, double y2) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.name = name;
		
		this.wall = new LineSegment(x1, y1, x2, y2);
		this.c1 = new Circle(x1, y1, 0);
		this.c2 = new Circle(x2, y2, 0);
		this.checkRep();
	}
	

	/*
	 * AF(x1, y1, x2, y2, name) = a line segment from (x1, y1) to (x2, y2) with Name name and 
	 * Rep Invariant ::= 
	 * 		The center of c1 should be at (x1, y1) and likewise for c2. 
	 * 		0 <= reflectionCoeff <= 1 This is true since the reflection coefficient is final and set to default value of 1
	 * Safety from rep exposure
	 * 	 Methods return a new object, such as a Vect and not any parts of the rep. 
	 * Thread Safety Argument
	 * 		Walls are immutable
	 */
	
	private void checkRep() {
		assert new Vect(x1, y1).equals(c1.getCenter()) && new Vect (x2, y2).equals(c2.getCenter());
	}
	
	@Override
	public Vect position() {
		final double xAnchor, yAnchor;
		
		if (x1 == x2) {
			xAnchor = x1;
			// line not diagonal
			if (y1 < y2) yAnchor = y2;
			else yAnchor = y1;
			
		}
		else if (y1 == y2) {
			yAnchor = y1;
			if (x1 <  x2) xAnchor = x1;
			else xAnchor = x2;
		}
		else {
			// line is diagonal
			if (x1 < x2 && y1 < y2) {
				xAnchor = x1;
				yAnchor = y2;
			}
			else if (x1 < x2 && y1 > y2) {
				xAnchor = x1;
				yAnchor = y1;
			}
			else if (x1 > x2 && y1 < y2){
				xAnchor = x2;
				yAnchor = y2;
			}
			else {
				xAnchor = x2;
				yAnchor = y1;
			}
		}
		return new Vect((int) xAnchor, (int) yAnchor);
	}
	
	/**
	 * @return starting point of the wall
	 */
	Vect start() {
		return new Vect(x1, y1);
	}
	
	/**
	 * 
	 * @return end point of the wall
	 */
	Vect end() {
		return new Vect(x2, y2);
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public int height() {
		return 0;
	}
	
	@Override
	public int width() {
		return (int) Math.sqrt(Physics.distanceSquared(x1, y1, x2, y2));
	}

	@Override
	public double collisionTime(Ball ball) {
		synchronized (this) {
			final double timeToWall = ball.timeUntilLineCollision(this.wall);
			final double timeToc1 = ball.timeUntilCircleCollision(c1);
			final double timeToc2 = ball.timeUntilCircleCollision(c2);
			
			return Math.min(timeToWall, Math.min(timeToc1, timeToc2));
		}
	}
	
	/**
	 * Calculates the time in seconds until a <code>ball</code> may collide with a <code>wall</code> rotating around the center of rotation, <code>pivot</code>.
	 * If no collision will occur Double.POSITIVE_INFINITY is returned.
	 * @param ball Ball with which the wall may collide
	 * @param pivot The point representing the center of rotation
	 * @param angularVelocity Speed in degrees / seconds that the <code>wall</code> is rotating.
	 * @return Collision time in seconds or POSITIVE_INFINITY if no collision will occu
	 */
	public double timeUntilRotatingWallCollision(Ball ball, Vect pivot, double angularVelocity) {
		synchronized (this) {
			return ball.timeUntilRotatingLineCollision(this.wall, pivot, angularVelocity);
		}
	}

	@Override
	public String getTrigger() {
		return Gadget.NO_TRIGGER;
	}
	
	@Override
	public void setTrigger(String trigger) {
		// Do Nothing walls cannot have action.
	}

	@Override
	public BufferedImage generate(int L) {
		// Returns an empty image as walls have zero thickness
		return new BufferedImage(0, 0, BufferedImage.TYPE_3BYTE_BGR);
	}

	@Override
	public void reflectBall(Ball ball) {
		synchronized (this) {
			final double timeToWall = ball.timeUntilLineCollision(this.wall);
			final double timeToc1 = ball.timeUntilCircleCollision(c1);
			final double timeToc2 = ball.timeUntilCircleCollision(c2);
			
			double collisionTime = Math.min(timeToWall, Math.min(timeToc1, timeToc2));
			
			if (collisionTime == timeToWall) {
				ball.reflectLine(this.wall, this.reflectionCoeff);	
			}
			else if (collisionTime == timeToc1) {
				ball.reflectCircle(c1, this.reflectionCoeff);
			}
			else {
				ball.reflectCircle(c2, this.reflectionCoeff);
				
			}
		}
		
	}
	
	/**
	 * Returns a ball that has collided with a rotating wall accounting for the lines coefficient of reflection.
	 * @param ball <code>ball</code> that will collide with the rotating wall
	 * @param pivot The point representing the center of rotation
	 * @param angularVelocity Speed in degrees / seconds that the <code>wall</code> is rotating.
	 * @param reflectionCoeff Coefficient of reflection of the wall
	 */
	public void reflectBallRotating(Ball ball, Vect pivot, double angularVelocity, double reflectionCoeff) {
		synchronized (this) {
			ball.reflectRotatingLine(this.wall, pivot, angularVelocity, reflectionCoeff);
		}
	}
	
	@Override
	public String toString() {
		return "Wall:" + this.name +"[<" + this.x1 + ", " + this.y1 + ">, <"+ this.x2 + ", " + this.y2 +">]";
	}


	@Override
	public boolean ballOverlap(Ball ball) {
		Vect ballCenter = ball.getCartesianCenter();
		Vect perpendicularPoint = Physics.perpendicularPoint(this.wall, ballCenter);
		if (perpendicularPoint == null) {
			return Math.sqrt(Physics.distanceSquared(ballCenter, this.wall.p1())) >= ball.getRadius() && 
					Math.sqrt(Physics.distanceSquared(ballCenter, this.wall.p2())) >= ball.getRadius();
		} else {
			return Math.sqrt(Physics.distanceSquared(ballCenter, perpendicularPoint)) >= ball.getRadius();
		}
	}

	/**
	 * Rotates the wall represented by <code>wall</code> by <code>a</code> around the center of rotation, <code>cor</code>, and returns the result.
	 * @param cor The point representing the center of rotation
	 * @param a the amount by which to rotate <code>wall</code> counterclockwise
	 * @return wall <code>Wall</code> rotated counterclockwise around cor by <code>a</code>
	 */
	public Wall rotateAround(Vect cor, Angle a) {
		synchronized (this) {
			Circle newC1 = Physics.rotateAround(this.c1, cor, a);
			Circle newC2 = Physics.rotateAround(this.c2, cor, a);
			
			return new Wall(this.name, newC1.getCenter().x(), newC1.getCenter().y(), newC2.getCenter().x(), newC2.getCenter().y());
		}
	}
	
	@Override
	public void takeAction() {
		// do nothing
	}
	
	@Override
	public void setCoverage(int[][] coverage) {
		// do nothing
	}
	
	
}
