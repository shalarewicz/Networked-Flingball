/**
 * Author:
 * Date:
 */
package flingball;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import physics.*;

public class Ball {
	
	/**
	 * An immutable class representing a ball for use on a flingball board. A ball has a radius > 0;
	 */ 
	public final static double DEFAULT_RADIUS = 0.25;
	
	
	private Vect boardCenter, cartesianCenter, boardVelocity, cartesianVelocity, anchor;
	private double radius = DEFAULT_RADIUS;
	private final String NAME;
	private boolean trapped = false;
	
	//TODO Add support for rotating Wall and Ball collisions
	
	
	/*
	 * AF(position, velocity, radius) ::= A ball with radius r, center = position, moving with velocity velocity. )
	 * Rep Invariant
	 * 	radius > 0 - A ball with 0 radius cannot exist as it would not collide with a gadget
	 * 	0 <= |velocity| <= 200
	 * Safety from rep exposure
	 * 	timeUntilCollision and reflect methods return new Ball objects
	 * 	getVelocity and getPosition return immutable Vect
	 */

	private void checkRep() {
		assert radius >= 0;
		assert Math.abs(this.boardVelocity.length()) <= 200 : this.NAME + "velocity = " + this.boardVelocity.length();
		assert Math.abs(this.boardVelocity.length()) >= 0;
	}
	
	/**
	 * Creates a new Ball for use on a flingBall board
	 * @param name name of the ball
	 * @param center center of the ball
	 * @param velocity velocity of the ball
	 */
	public Ball(String name, Vect center, Vect velocity) {
		this.NAME = name;
		this.boardCenter = center;
		this.cartesianCenter = new Vect(this.boardCenter.x(), -this.boardCenter.y());
		this.anchor = new Vect(center.x() - radius, center.y() - radius);
		
		this.boardVelocity = velocity;
		this.cartesianVelocity = new Vect(velocity.x(), -velocity.y());
		
		checkRep();
	}
	
	/**
	 * Creates a new Ball for use on a flingBall board
	 * @param name name of the ball
	 * @param center center of the ball
	 * @param velocity velocity of the ball
	 * @param radius radius of the ball. Must be greater than zero
	 */
	public Ball(String name, Vect center, Vect velocity, double radius) {
		this.NAME = name;
		this.boardCenter = center;
		this.cartesianCenter = new Vect(this.boardCenter.x(), -this.boardCenter.y());
		this.anchor = new Vect(center.x() - radius, center.y() - radius);
		
		this.boardVelocity = velocity;
		this.cartesianVelocity = new Vect(velocity.x(), -velocity.y());
		this.radius = radius;
		
		checkRep();
	}
	
	/**
	 * Given the coordinate of the center of a ball on a flingball board. Returns the center in Cartesian space.
	 * 
	 * @param center center of the ball on a flingabll board
	 * @return center of the ball in Cartesian Space. 
	 */
	private static Vect getCartesianCenterFromBoardCenter(Vect boardCenter) {
		return new Vect(boardCenter.x(), -boardCenter.y());
	}
	
	private static Vect getBoardCenterFromCartesianCenter(Vect cartesianCenter) {
		return new Vect(cartesianCenter.x(), -cartesianCenter.y());
	}
	
	/**
	 * Computes the anchor of the bounding box of the ball. The anchor is the top left corner of the bounding box.
	 * @return anchor of the ball on a flingBall board. 
	 */
	private Vect getAnchorFromCartesianCenter(Vect cartesianCenter) {
		return new Vect(cartesianCenter.x() - this.radius, -cartesianCenter.y() - this.radius);
	}
	
	private Vect getAnchorFromBoardCenter(Vect boardCenter) {
		return new Vect(boardCenter.x() - this.radius, boardCenter.y() - this.radius);
	}
	
	/**
	 * Moves the ball the distance it would travel during time time given the current velocity of the ball. Does not account
	 * for the effects of gravity or friction. 
	 * 
	 * @param time time in seconds during which the ball is moving. 
	 * @return A new ball in the end location. 
	 */
	
	public void move(double time) {
		// distance = position + velocity * t
		Vect newCenter = this.cartesianVelocity.times(time).plus(this.cartesianCenter);
		
		//Round Decimal to avoid floating point math errors after ~15 decimal places
		DecimalFormat df = new DecimalFormat("#.###########");
		df.setRoundingMode(RoundingMode.HALF_EVEN);
		double newXCenter = Double.parseDouble(df.format(newCenter.x()));
		double newYCenter = Double.parseDouble(df.format(newCenter.y()));
		
		Vect roundedCenter = new Vect(newXCenter, newYCenter);
		
		this.cartesianCenter = roundedCenter;
		this.boardCenter = getBoardCenterFromCartesianCenter(roundedCenter);
		this.anchor = getAnchorFromCartesianCenter(roundedCenter);
	}
	
	/**
	 * Moves the ball the distance it would travel during time time given the current velocity of the ball while accounting for both gravity and friction. 
	 * 
	 * @param time time in seconds during which the ball will travel. 
	 * @param gravity - Gravity constant for the gives space in L / s^-2
	 * @param mu - Coefficient of friction for the flingball board in s^-1
	 * @param mu2 - Coefficient of friction for the flingball board in L^-1
	 * @return A new ball in the end location moving with velocity affected by gravity and friction
	 */
	public void move(double time, double gravity, double mu, double mu2) {
		// let a = acceleration
		// displacement = position + velocity * t + at^2
		// delta_v = at
		// v_new_f = v_old * (1 - mu*delta_t - mu2*|v_old|*delta_t) 
		
		double v_initial = this.cartesianVelocity.length();
		
		//Change in velocity due to gravity = gravity * time
		final Vect deltaVGravity = new Vect(Angle.DEG_270, gravity).times(time);
		
		// Final velocity as a result of friction
		double vFinalFriction = v_initial * (1 - mu*time - mu2 * time * Math.abs(v_initial));
		
		//Change in velocity due to friction = a_f * t (where a_f is acceleration due to friction)
		Vect deltaVFriction = new Vect(this.cartesianVelocity.angle().plus(Angle.DEG_180), Math.abs(vFinalFriction - v_initial));
		
		// Net change in velocity = a*t
		Vect deltaV = deltaVGravity.plus(deltaVFriction);
		
		DecimalFormat vf = new DecimalFormat("#.##");
		vf.setRoundingMode(RoundingMode.HALF_UP);
		Vect newVelocity = this.cartesianVelocity.plus(deltaV);
		
		double newVx = Double.parseDouble(vf.format(newVelocity.x()));
		double newVy = Double.parseDouble(vf.format(newVelocity.y()));
		
		Vect correctedVelocity = new Vect(newVx, newVy);
		
		
		// displacement = v_i*t + a*t*t = v_i*t + deltaV * t
		Vect displacement = this.cartesianVelocity.times(time).plus(deltaV.times(time));
		
		Vect newCartesianCenter = this.cartesianCenter.plus(displacement);
		
		//Round displacement to avoid floating point errors which lead to ball being off the board. 
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		
		double newCX = Double.parseDouble(df.format(newCartesianCenter.x()));
		double newCY = Double.parseDouble(df.format(newCartesianCenter.y()));
		
		Vect correctedCartesianCenter = roundedVector(new Vect(newCX, newCY), this.radius);
		
		Vect newBoardCenter = getBoardCenterFromCartesianCenter(correctedCartesianCenter);
		this.cartesianCenter = correctedCartesianCenter;
		this.boardCenter = newBoardCenter;
		this.cartesianVelocity = correctedVelocity;
		this.boardVelocity = convertVelocity(correctedVelocity);
		this.anchor = getAnchorFromCartesianCenter(correctedCartesianCenter);
		
	}
	
	/**
	 * If the ball is off a 20x20 grid it sets is location to the point on the grid border perpendicular to the ball's center
	 * @param v
	 * @param radius
	 * @return
	 */
	private static Vect roundedVector(Vect v, double radius) {
		double vx = v.x();
		double vy = v.y();
		
		if (vx - radius < 0.0) vx = radius;
		if (vx + radius> 20.0) vx = 20 - radius;
		if (vy + radius > 0.0) vy = -radius;
		if (vy - radius < -20.0) vy = -20 + radius;
		
		return new Vect (vx, vy);
	}
	
	
	
	/**
	 * 
	 * @return The position of the origin (top left) of the bounding box of the ball
	 */
	public Vect getAnchor() {
		return this.anchor;
	}
	
	/**
	 * 
	 * @return The position of the center of the ball on a flingball board.
	 */
	public Vect getBoardCenter() {
		return this.boardCenter;
	}
	
	/**
	 * 
	 * @return The position of the center of the ball in Cartesian Space.
	 */
	public Vect getCartesianCenter() {
		return this.cartesianCenter;
	}
	
	/**
	 * 
	 * @return Radius of the ball
	 */
	public double getRadius() {
		return this.radius;
	}
	
	/**
	 * 
	 * @return The current velocity of the ball on the flingball board.
	 */
	public Vect getVelocity() {
		return this.boardVelocity;
	}
	
	/**
	 * Converts velocity from Cartesian to flingball space and vice versa
	 * @param v velocity to be converted
	 * @return new velocity
	 */
	private static Vect convertVelocity(Vect v) {
		return new Vect(v.x(), -v.y());
	}
	/**
	 * 
	 * @param v The new velocity of the ball
	 * @return A ball with velocity v
	 */
	public void setVelocity(Vect v) {
		this.boardVelocity = v;
		this.cartesianVelocity = getCartesianCenterFromBoardCenter(v);
		checkRep();
	}
	
	/**
	 * @param L the dimension of the board 20L x 20L pixels wide
	 * @return An image of the ball
	 */
	public BufferedImage generate(int L) {
		final int diameter = (int) (2*radius*L);
		BufferedImage output = new BufferedImage(diameter, diameter, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        graphics.setColor(Color.BLUE);
        graphics.fillArc(0, 0, diameter, diameter, 0, 360);
        
		return output;
	}
	

	/**
	 * Calculates the time in seconds until the ball may collide with a line segment. If no collision will occur Double.POSITIVE_INFINITY is 
	 * returned. 
	 * 
	 * @param line Line with which the ball may collide
	 * @return Collision time in seconds or POSITIVE_INFINITY if no collision will occur
	 */
	public double timeUntilLineCollision(LineSegment line) {
		return Physics.timeUntilWallCollision(line, new Circle(this.cartesianCenter, this.radius), this.cartesianVelocity);
	}
	
	/**
	 * Calculates the time in seconds until the ball may collide with a circle. If no collision will occur Double.POSITIVE_INFINITY is 
	 * returned. 
	 * 
	 * @param circle circle with which the ball may collide
	 * @return Collision time in seconds or POSITIVE_INFINITY if no collision will occur
	 */
	public double timeUntilCircleCollision(Circle circle) {
		return Physics.timeUntilCircleCollision(circle, new Circle(this.cartesianCenter, this.radius), this.cartesianVelocity);
	}
	
	/**
	 * Calculates the time in seconds until the ball may collide with a rotating line segment. If no collision 
	 * will occur Double.POSITIVE_INFINITY is returned. 
	 * 
	 * @param line Line with which the ball may collide
	 * @return Collision time in seconds or POSITIVE_INFINITY if no collision will occur
	 */
	public double timeUntilRotatingLineCollision(LineSegment line, Vect pivot, double angularVelocity) {
		return Physics.timeUntilRotatingWallCollision(line, pivot, angularVelocity, 
				new Circle(this.cartesianCenter, this.radius), this.cartesianVelocity);
	}
	
	/**
	 * Calculates the time in seconds until the ball may collide with a rotating circle. If no collision 
	 * will occur Double.POSITIVE_INFINITY is returned. 
	 * 
	 * @param circle circle with which the ball may collide
	 * @return Collision time in seconds or POSITIVE_INFINITY if no collision will occur
	 */
	public double timeUntilRoatatingCircleCollision(Circle circle, Vect pivot, double angularVelocity) {
		return Physics.timeUntilRotatingCircleCollision(circle, pivot, angularVelocity, 
				new Circle(this.cartesianCenter, this.radius), 
				this.cartesianVelocity);
	}
	
	/**
	 * Returns a ball that has perfectly elastic collision with a line,
	 * 
	 * @param line with which the ball is colliding
	 * @return A ball that has collided with the line. 
	 */
	public void reflectLine(LineSegment line) {
		this.cartesianVelocity = Physics.reflectWall(line, this.cartesianVelocity);
		this.boardVelocity = convertVelocity(this.cartesianVelocity);
		checkRep();
	}
	
	/**
	 * Returns a ball that has collided with a line segment accounting for the lines coefficient of reflection
	 * @param line line with which the ball collides
	 * @param reflectionCoeff - Coefficient of reflection for the line
	 * @return A ball which has collided with the line
	 */
	public void reflectLine(LineSegment line, double reflectionCoeff) {
		this.cartesianVelocity = Physics.reflectWall(line, this.cartesianVelocity, reflectionCoeff);
		this.boardVelocity = convertVelocity(this.cartesianVelocity);
		checkRep();
	}
	
	/**
	 * Returns a ball that has collided with a rotating line segment accounting for the lines coefficient of reflection
	 * @param line line with which the ball collides
	 * @param pivot the point about which the line is rotating
	 * @param angularVelocity the angular velocity of the rotating line
	 * @param reflectionCoeff Coefficient of reflection for the line
	 * @return A ball which has collided with the line
	 */
	public void reflectRotatingLine(LineSegment line, Vect pivot, double angularVelocity, double reflectionCoeff) {
		this.cartesianVelocity = Physics.reflectRotatingWall(line, pivot, angularVelocity,
					new Circle(this.cartesianCenter, this.radius), this.cartesianVelocity, reflectionCoeff);
		if (this.cartesianVelocity.length() > 200) {
			this.cartesianVelocity = new Vect(this.cartesianVelocity.angle(), 190);
		}
		this.boardVelocity = convertVelocity(this.cartesianVelocity);
		checkRep();
	}
	
	/**
	 * Returns a ball that has perfect elastic collision with a circle
	 * @param circle circle with which the ball is colliding
	 * @return A ball that has collided with the circle
	 */
	public void reflectCircle(Circle circle) {
		this.cartesianVelocity = Physics.reflectCircle(circle.getCenter(), this.cartesianCenter, cartesianVelocity);
		this.boardVelocity = convertVelocity(this.cartesianVelocity);
		checkRep();
	}
	
	/**
	 * Returns a ball that has collided with a circle accounting for the lines coefficient of reflection
	 * @param circle circle with which the ball collides
	 * @param reflectionCoeff - Coefficient of reflection for the circle
	 * @return A ball which has collided with the circle
	 */
	public void reflectCircle(Circle circle, double reflectionCoeff) {
		this.cartesianVelocity = Physics.reflectCircle(circle.getCenter(), this.cartesianCenter, 
				cartesianVelocity, reflectionCoeff);
		this.boardVelocity = convertVelocity(this.cartesianVelocity);
		checkRep();
	}
	
	public void reflectRotatingCircle(Circle circle, Vect pivot, double angularVelocity, double relectionCoeff) {
		this.cartesianVelocity = Physics.reflectRotatingCircle(circle, pivot, angularVelocity, 
				new Circle(this.cartesianCenter, this.radius), this.cartesianVelocity, relectionCoeff);
		if (this.cartesianVelocity.length() > 200) {
			this.cartesianVelocity = new Vect(this.cartesianVelocity.angle(), 190);
		}
		this.boardVelocity = convertVelocity(this.cartesianVelocity);
		
		checkRep();
	}
	
	@Override
	public boolean equals(Object that) {
		return that instanceof Ball && this.sameParts((Ball) that);
	}

	private boolean sameParts(Ball that) {
		return this.boardVelocity == that.boardVelocity &&
				this.boardCenter == that.boardCenter &&
				this.radius == that.radius &&
				this.NAME == that.NAME;
	}
	
	@Override
	public String toString() {
		return "Ball{name=" + this.NAME + ", center=" + this.boardCenter + ", velocity=" + 
				this.boardVelocity + ", radius=" + this.radius + "}";
	}
	
	@Override
	public int hashCode() {
		return NAME.hashCode() + this.cartesianCenter.hashCode();
	}

	/**
	 * 
	 * @return name of the ball
	 */
	public String name() {
		return this.NAME;
	}

	/**
	 * 
	 * @return true if the ball is currently trapped;
	 */
	public boolean isTrapped() {
		return this.trapped ;
	}
	
	/**
	 * Traps the ball
	 */
	public void trap() {
		this.trapped = true;
	}
	
	/**
	 * Releases the ball
	 */
	public void release() {
		this.trapped = false;
	}
	
	/**
	 * Sets the position of the ball's center on a flingball board
	 * 
	 * @param vect new position of the ball. 
	 */
	public void setBoardPosition(Vect vect) {
		this.boardCenter = vect;
		this.cartesianCenter = getCartesianCenterFromBoardCenter(this.boardCenter);
		this.anchor = getAnchorFromBoardCenter(this.boardCenter);
	}
	
	/**
	 * Sets the position of the ball's center in Cartesian space
	 * 
	 * @param vect new position of the ball. 
	 */
	public void setCartesianPosition(Vect vect) {
		this.cartesianCenter = vect;
		this.boardCenter = getBoardCenterFromCartesianCenter(this.cartesianCenter);
		this.anchor = this.getAnchorFromCartesianCenter(this.cartesianCenter);
	}
	
}

