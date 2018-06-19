package flingball;

import java.awt.image.BufferedImage;

import physics.Circle;
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
	private final Gadget target;
	
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

	@Override
	public Vect position() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setReflectionCoefficient(double x) {
		// TODO Auto-generated method stub

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
	public void fireAll() {
		// TODO Auto-generated method stub

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
