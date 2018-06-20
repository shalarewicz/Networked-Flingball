package flingball.gadgets;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import flingball.Ball;
import physics.Vect;

public class AbsorberC implements Absorber {
	
	
	private String name;
	private final int x, y, width, height;
	private final Deque<Ball> balls = new LinkedList<Ball>();
	private String trigger = NO_TRIGGER;
	private final Set<Wall> walls = new HashSet<Wall>();
	
	
	/*
	 * AF(name, x, y, width, height, balls, walls) ::=
	 * 		An absorber has anchor (x, -y), width width and height height. An absorber
	 * 		has >= zero balls trapped in balls. The bounding box of the absorber is
	 * 		represented by walls. 
	 * 
	 * Rep Invariant ::= 
	 * 		Walls have endpoints [(x,y), (x+width, y)], [(x,y), (x, y+height)],
	 * 		[(x+width,y), (x+width, y+height)], [(x,y+height), (x+width, y+height)]
	 * 		Walls.size() = 4;
	 * 
	 * Safety from Rep Exposure:
	 * 		TODO
	 * 
	 * Thread Safety Argument:
	 * 		TODO Will need locks on balls or a synchronized list. 
	 */
	
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
		// TODO not used
		return 0;
	}

	@Override
	public void setReflectionCoefficient(double x) {
		// TODO not used
	}

	@Override
	public double collisionTime(Ball ball) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void reflectBall(Ball ball) {
		// TODO not used. balls are captured then released

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

	@Override
	public void fireAll() {
		// TODO Auto-generated method stub

	}

}
