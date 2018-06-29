package flingball.gadgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import flingball.Ball;
import physics.Circle;
import physics.Physics;
import physics.Vect;

public class Portal implements Gadget {
	/**
	 * A gadget which can be used on a flingball board. A portal is a circular hole with diameter
	 * 1 L. A portal can teleport an object to another target portal located on the same board or 
	 * on a connected board. The ball will exit the target portal with the same velocity 
	 * that it entered the source portal. If no target portal exists then the ball passes over
	 * the portal unaffected. 
	 * 
	 * In order to teleport through a portal the ball must completely leave the portal and then 
	 * collide with the portal again. Therefore, if a portal is immediately adjacent to a bumper 
	 * and the ball collides with the bumper back towards portal the ball will not teleport again.
	 * However, if there is an empty space between the portal and bumper then the ball will teleport
	 * again. 
	 * 
	 * Portals do not have to be symmetrically connected. That is Portal A can be connected to
	 * Portal B, but Portal B can be connected to Portal C. 
	 * 
	 * If another gadget uses a portal as it's trigger then the action is only performed when
	 * a ball strikes the portal not when it exits over the portal. 
	 * 
	 * Portals cannot have an action. 
	 */
	private final static Portal UNCONNECTED = new Portal("UNCONNECTED", -1, -1);
	
	private final int x, y;
	private final String name;
	private final Circle portal;
	private Portal target = UNCONNECTED;
	
	private static final double RADIUS = 0.5;
	private static final int HEIGHT = 1;
	private static final int WIDTH = 1;
	
	
	/*
	 * AF(x, y, name, portal, target) ::= 
	 * 		A portal with anchor (x, -y) represented by the circle, portal, with diameter 1 L.
	 * 		Any ball that enters the portal will exit target. 
	 * Rep Invariant ::=
	 *  	portal has diameter 1 L
	 *  	target is a Portal object. 
	 *  Safety from rep exposure:
	 *  	TODO
	 *  Thread Safety Argument:
	 *  	TODO
	 */
	
	private void checkRep() {
		assert this.portal.getRadius() == RADIUS : name + ": Portal radius != 1: " + this.portal.getRadius();
		//assert this.target instanceof Portal : name + ": Portal is not connected to an object of type Portal";
	}
	
	/**
	 * Creates a an unconnected portal (x,y)
	 * @param name name of the  portal
	 * @param x x coordinate of upper left corner of the bounding box of the portal
	 * @param y y coordinate of upper left corner of the bounding box of the portal
	 */
	public Portal(String name, int x, int y) {
		this.x = x;
		this.y = -y;
		this.name = name;
		this.portal = new Circle(x + RADIUS, -y - RADIUS, RADIUS);
		this.checkRep();
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
	public double collisionTime(Ball ball) {
		double collisionTime = Double.POSITIVE_INFINITY;
		// if the portal is unconnected or self connected then the ball passes over the portal unchanged
		if (!(this.target.name.equals(this.name) && this.target.name.equals("UNCONNECTED"))) {
				collisionTime = ball.timeUntilCircleCollision(portal);
		}
		return collisionTime;
	}

	@Override
	public void reflectBall(Ball ball) {
		if (!this.target.name.equals(this.name) && !this.target.name.equals("UNCONNECTED")) {
			
			ball.setCartesianPosition(new Vect(target.portal.getCenter().x(), 
					target.portal.getCenter().y()));
		}
	}

	@Override
	public String getTrigger() {
		return Gadget.NO_TRIGGER;
	}
	
	@Override
	public void setTrigger(String trigger) {
		// Do nothing
	}

	@Override
	public void takeAction() {
		// Do nothing 
	}
	
	@Override
	public boolean ballOverlap(Ball ball) {
		if (this.target.equals(UNCONNECTED)) {
			return false;
		}
		double distance = Math.sqrt(Physics.distanceSquared(ball.getBoardCenter(), this.portal.getCenter()));
		return distance < ball.getRadius() + RADIUS;
	}

	@Override
	public BufferedImage generate(int L) {
		final int diameter = (int) (2 * RADIUS * L);
		BufferedImage output = new BufferedImage(diameter, diameter, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        graphics.setColor(Color.BLUE);
        graphics.fillArc(0, 0, diameter, diameter, 0, 360);
        graphics.setColor(Color.LIGHT_GRAY);
        final int innerRadius = diameter / 2;
        graphics.fillArc(innerRadius / 2, innerRadius / 2, innerRadius, innerRadius, 0, 360);
        
		return output;
	}

	@Override
	public void setCoverage(int[][] coverage) {
		int x = (int) this.position().x();
		int y = (int) this.position().y();
		if (coverage[y][x] == 1) {
			throw new RuntimeException("Portal coverage overlaps with another gadget at [" + y +"]["+ x +"]");
		}
		coverage[y][x] = 1;
	}

	/**
	 * Establishes a one way connection between this portal that. A ball that 
	 * collides with this portal will be teleported to that and exit with the 
	 * same velocity and direction. 
	 * @param that
	 */
	public void connect(Portal that) {
		this.target = that;
	}
	
	@Override
	public String toString() {
		//TODO include target board?
		return "{PORTAL: " + name + " " + this.position() + " connected to " + this.target.name + "}";
	}
	
	@Override
	public boolean equals(Object that) {
		return that instanceof Portal && this.sameParts((Portal) that);
	}

	private boolean sameParts(Portal that) {
		return this.name.equals(that.name) &&
				this.x == that.x &&
				this.y == that.y &&
				this.target == that.target;
	}
}
