package flingball.gadgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import flingball.Ball;
import physics.Circle;
import physics.Physics;
import physics.Vect;

public class Portal implements Gadget {
	// TODO: Might be worth creating a separate interface for portals. This would allow for a shuffling
	// of portal connections, changing or setting target portals. 
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
	
	private final int x, y;
	private final String name;
	private final Circle portal;
	private Portal target;
	
	private static final double RADIUS = 0.5;
	private static final int HEIGHT = 1;
	private static final int WIDTH = 1;
	
	private final static Portal UNCONNECTED = new Portal("UNCONNECTED", -1, -1);
	
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
	 *  	This is an immutable class
	 */
	
	private void checkRep() {
		assert this.portal.getRadius() == 1 : name + ": Portal radius != 1: " + this.portal.getRadius();
		assert this.target instanceof Portal : name + ": Portal is not connected to an object of type Portal";
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
		this.portal = new Circle(x, -y, RADIUS);
		this.target = UNCONNECTED;
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
	public double getReflectionCoefficient() {
		// TODO Not used
		throw new UnsupportedOperationException("Portal does not have a reflection coefficient");
	}

	@Override
	public void setReflectionCoefficient(double x) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Portal does not have a reflection coefficient");
	}

	@Override
	public double collisionTime(Ball ball) {
		final double collisionTime;
		
		// This prevents collisions were a ball is currently being teleported and currently overlaps with a portal. 
		if (ball.getCartesianCenter().distanceSquared(this.portal.getCenter()) > (RADIUS + ball.getRadius())) {
			collisionTime = ball.timeUntilCircleCollision(portal);
		} else {
			collisionTime = Double.POSITIVE_INFINITY;
		}
		return collisionTime;
	}

	@Override
	public void reflectBall(Ball ball) {
		if (!this.target.equals(UNCONNECTED)) {
			ball.setCartesianPosition((this.target.portal.getCenter()));
		}
		// TODO Auto-generated method stub
		

	}

	@Override
	public String getTrigger() {
		return Gadget.NO_TRIGGER;
	}

	@Override
	public void takeAction() {
		// Do nothing
		// TODO Not used
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
        graphics.fillArc(0, 0, diameter / 2, diameter / 2, 0, 360);
        
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
	 * Connects this portal to another portal. 
	 * @param p
	 */
	public void connect(Portal p) {
		this.target = p;
	}
	
	@Override
	public String toString() {
		//TODO include target board?
		return "{PORTAL: name " + this.position() + this.target + "}";
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
