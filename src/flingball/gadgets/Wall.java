package flingball.gadgets;

import java.awt.image.BufferedImage;

import flingball.Ball;
import physics.Angle;
import physics.Circle;
import physics.LineSegment;
import physics.Physics;
import physics.Vect;

// Immutable Type representing a wall of a flingball board
public class Wall implements Gadget {

	
	private final String name;
	private final double x1, y1, x2, y2;
	
	private final double reflectionCoeff = Gadget.DEFAULT_REFLECTION_COEFF;
	
	private final LineSegment wall;
	private final Circle c1, c2;
	
	
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
	 * 		TODO
	 * Thread Safety Argument
	 * 		TODO 
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
	 * 
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
	public double getReflectionCoefficient() {
		return this.reflectionCoeff;
	}
	
	@Override
	public void setReflectionCoefficient(double x) {
		// Do Nothing
	}

	@Override
	public double collisionTime(Ball ball) {
		final double timeToWall = ball.timeUntilLineCollision(this.wall);
		final double timeToc1 = ball.timeUntilCircleCollision(c1);
		final double timeToc2 = ball.timeUntilCircleCollision(c2);
		
		return Math.min(timeToWall, Math.min(timeToc1, timeToc2));
	}

	@Override
	public String getTrigger() {
		return Gadget.NO_TRIGGER;
	}

	@Override
	public BufferedImage generate(int L) {
		return new BufferedImage(0, 0, BufferedImage.TYPE_3BYTE_BGR);
	}

	@Override
	public void reflectBall(Ball ball) {
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
	
	@Override
	public String toString() {
		return "Wall:" + this.name +"[<" + this.x1 + ", " + this.y1 + ">, <"+ this.x2 + ", " + this.y2 +">]";
	}


	@Override
	public boolean ballOverlap(Ball ball) {
		Vect ballCenter = ball.getCartesianCenter();
		Vect perpendicularPoint = Physics.perpendicularPoint(this.wall, ballCenter);
		System.out.println(ballCenter);
		System.out.println(perpendicularPoint);
		return Physics.distanceSquared(ballCenter, perpendicularPoint) >= 0;
	}

	@Override
	public void takeAction() {
		// do nothing
	}
	
	@Override
	public void setCoverage(int[][] coverage) {
		
		// do nothing
	}
	
	//TODO Spec
	public Wall rotateAround(Vect cor, Angle a) {
		Circle newC1 = Physics.rotateAround(this.c1, cor, a);
		Circle newC2 = Physics.rotateAround(this.c2, cor, a);
		
		return new Wall(this.name, newC1.getCenter().x(), newC1.getCenter().y(), newC2.getCenter().x(), newC2.getCenter().y());
	}
	
	//TODO Spec
	public double timeUntilRotatingWallCollision(Ball ball, Vect pivot, double angularVelocity) {
		return ball.timeUntilRotatingLineCollision(this.wall, pivot, angularVelocity);
	}
	
	//TODO Spec
	public void reflectBallRotating(Ball ball, Vect pivot, double angularVelocity, double reflectionCoeff) {
		ball.reflectRotatingLine(this.wall, pivot, angularVelocity, reflectionCoeff);
	}
}
