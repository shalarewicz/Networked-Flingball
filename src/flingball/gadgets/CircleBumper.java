/**
 * Author:
 * Date:
 */
package flingball.gadgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import physics.Circle;
import physics.Physics;
import physics.Vect;

public class CircleBumper implements Gadget {
	
	
	/**
	 * CircleBumper represents a circular bumper which can be played
	 * as a gadget on a flingball board. A CircleBumper has a radius of
	 * 0.5 L and its anchor is located in the upper left-hand corner of
	 * its bounding box, which has dimensions 1L x 1L
	 * 
	 * A CircleBumper has no default action but can trigger a provided Board Action
	 */
	
	private String name;
	private final int x, y;
	private final int WIDTH = 1;
	private final int HEIGHT = 1;
		
	private final double radius = 0.5;
	private double reflectionCoeff = Gadget.DEFAULT_REFLECTION_COEFF;
	private final Circle bumper;
	private String trigger = NO_TRIGGER;
	
	//TODO Add support for spinning bumpers which are turned on and off by actions
		//	private static final double SPIN_RATE = 5.0;
		//	private double spin = 0;
	
	
	
	/*
	 * AF(x, y, RADIUS, name, bumper, trigger) ::= 
	 * 		A circle with center x, y and radius r represented by bumper named name. Has trigger trigger
	 * 		and coefficient of reflection reflectionCoeff. Located at (x, -y) on flingball board. 
	 * Rep Invariant
	 * 		bumper has center (x,y) and radius radius. 
	 * 		0 <= reflectionCoeff <= 1
	 * Safety from rep exposure
	 * 		TODO
	 * 
	 * Thread Safety Argument
	 * 		TODO
	 */
	private void checkRep() {
		assert bumper.getCenter().x() == x + radius : "CircleBumper: " + name + " x coordinates inequal";
		assert bumper.getCenter().y() == y - radius : "CircleBumper: " + name + " y coordinates inequal";
		assert bumper.getRadius() == radius : "CircleBumper: " + name + " radius not = 0.5";
		assert reflectionCoeff <= 1.0 && reflectionCoeff >= 0.0 : "CircleBumper: " + name + " reflectionCoeff >= 0";
	}

	
	public CircleBumper(String name, int x, int y) {
		this.x = x;
		this.y = -y;
		this.name = name;
		this.bumper = new Circle(new Vect (x + radius, -y - radius), radius);
		checkRep();
	}
	

	@Override
	public Vect position() {
		return new Vect(x, -y);
	}

	@Override
	public String name() {
		return this.name;
	}
	
	@Override 
	public int height() {
		return this.HEIGHT;
	}
	
	@Override 
	public int width() {
		return this.WIDTH;
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
		return ball.timeUntilCircleCollision(bumper);
	}

	@Override
	public String getTrigger() {
		return this.trigger;
	}

	@Override
	public BufferedImage generate(int L) {
		BufferedImage output = new BufferedImage(L, L, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        graphics.setColor(Color.ORANGE);
        graphics.fillArc(0, 0, L, L, 0, 360);
        
        return output;
	}

	@Override
	public void reflectBall(Ball ball) {
		// TODO Add Spin support
//		if (this.spin == 0) {
			ball.reflectCircle(this.bumper);
//		} else {
//			ball.reflectRotatingCircle(this.bumper, this.spin, this.reflectionCoefficient);
//		}
	}
	
	@Override
	public String toString() {
		return "Circle Bumper:" + this.name + " " + this.position() //TODO SPIN SUPPORT + ", spin=" + this.spin;
	}
	
	@Override
	public int hashCode() {
		return this.x + this.y;
	}
	
	@Override
	public boolean equals(Object that) {
		return that instanceof CircleBumper && this.samePosition((CircleBumper) that);
	}

	private boolean samePosition(CircleBumper that) {
		return this.x == that.x && this.y == that.y;
 	}

	@Override
	public boolean ballOverlap(Ball ball) {
		double distance = Math.sqrt(Physics.distanceSquared(ball.getBoardCenter(), this.bumper.getCenter()));
		return distance < ball.getRadius() + this.radius;
		
	}

	@Override
	public void takeAction() {
		//TODO SPIN SUPPORT
//		if (this.spin == 0) {
//			this.spin = SPIN_RATE;
//		}
//		else {
//			this.spin = 0;
//		}
	}
	
	@Override
	public void setCoverage(int[][] coverage) {
		int x = (int) this.position().x();
		int y = (int) this.position().y();
		coverage[y][x] = 1;
	}
}
