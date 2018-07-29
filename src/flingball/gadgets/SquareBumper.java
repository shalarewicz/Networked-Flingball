package flingball.gadgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import flingball.Ball;
import physics.Vect;

/**
 * SquareBumper represents a square bumper which can be played
 * as a gadget on a flingball board. A SquareBumper has a height and 
 * width of 1 L and its anchor is located in the upper left-hand corner 
 * 
 * A SquareBumper has no default action but can trigger a provided Board Action
 * @author Stephan Halarewicz
 */
public class SquareBumper implements Bumper {
	
	private final String name;
	private final int x, y;
	private final int WIDTH = 1;
	private final int HEIGHT = 1;
	
	private Double reflectionCoeff = Bumper.DEFAULT_REFLECTION_COEFF;
	private String trigger = Gadget.NO_TRIGGER;

	private final Set<Wall> walls;
	

	/*
	 * AF(x, y, RADIUS, name, bumper, trigger) ::= 
	 * 		A square with position x, -y on flingball board named name. Has trigger trigger
	 * 		and coefficient of reflection reflectionCoeff. 
	 * Rep Invariant
	 * 		walls.size() == 4
	 * 		Walls have endpoints [(x,y), (x+width, y)], [(x,y), (x, y+height)],
	 * 			[(x+width,y), (x+width, y+height)], [(x,y+height), (x+width, y+height)]
	 * 		0 <= reflectionCoeff <= 1
	 * Safety from rep exposure
	 * 		Only immutable or primitive types are ever returned. 
	 * Thread Safety Argument
	 * 		All fields except trigger and reflectionCoeff are final or immutable. 
	 * 		The elements of the set walls are never modified and Wall is immutable. 
	 * 		Locks are obtained before using or changing trigger or reflectionCoeff
	 * 		
	 */
	
	private void checkRep() {
		assert walls.size() == 4;
		Map<Vect, Integer> endPoints = new HashMap<Vect, Integer>();
		for (Wall wall : walls) {
			Vect start = wall.start();
			Vect end = wall.end();
			assert start.distanceSquared(end) == 1;
			
			if (endPoints.containsKey(start)) {
				endPoints.put(start, endPoints.get(start) +1);
			} else {
				endPoints.put(start, 1);
			}
			if (endPoints.containsKey(end)) {
				endPoints.put(end, endPoints.get(end) +1);
			} else {
				endPoints.put(end, 1);
			}
		}
		assert endPoints.size() == 4;
		for (Vect v : endPoints.keySet()) {
			// In a square each vertex must be an end point of exactly two sides. Because walls
			// is a set there can be no duplicate entries so therefore if each vertex occurs twice
			// then a square is formed. 
			assert endPoints.get(v) == 2 : "SquareBumper: " + name + " doesn't form a square";
		}
		assert endPoints.size() == 4;
		assert endPoints.containsKey(new Vect(x, y)) && endPoints.containsKey(new Vect(x + 1, y - 1));
		
		synchronized (this.reflectionCoeff) {
			assert 0 <= reflectionCoeff && reflectionCoeff <=1 : "SquareBumper: " + name + " invalid reflection coefficient";
		}
	}
	
	/**
	 * Create a new square bumper at the position (x,y) on a flingball board
	 * @param name Name of the square bumper
	 * @param x x-coordinate of the bumper on a flingball board
	 * @param y y-coordinate of the bumper on a flingball board
	 */
	public SquareBumper(String name, int x, int y) {
		this.name = name;
		this.x = x;
		this.y = -y;
		
		//Bounding walls
		final Wall top = new Wall(name + " top", x, - y, x + 1, -y);
		final Wall bottom = new Wall(name + " bottom", x, -y-1, x+1, -y-1);
		final Wall left = new Wall(name + " left", x, - y, x, -y-1);
		final Wall right = new Wall(name + " right", x+1, -y, x+1, -y-1);
		
		this.walls = new HashSet<Wall>(Arrays.asList(top, bottom, left, right));
		this.checkRep();
		
	}
	
	/**
	 * Create a new square bumper at the position represented by <code>position</code> on a 
	 * flingball board
	 * @param name Name of the square bumper.
	 * @param position position on a flingball board. 
	 */
	public SquareBumper(String name, Vect position) {
		this.name = name;
		int x = (int) position.x();
		int y = (int) -position.y();
		this.x = x;
		this.y = y;
		
		//Bounding walls
		final Wall top = new Wall(name + " top", x, - y, x + 1, -y);
		final Wall bottom = new Wall(name + " bottom", x, -y-1, x+1, -y-1);
		final Wall left = new Wall(name + " left", x, - y, x, -y-1);
		final Wall right = new Wall(name + " right", x+1, -y, x+1, -y-1);
		
		this.walls = new HashSet<Wall>(Arrays.asList(top, bottom, left, right));
		this.checkRep();
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
		synchronized (this.reflectionCoeff) {
			return this.reflectionCoeff;
		}
	}
	
	@Override
	public synchronized void setReflectionCoefficient(double x) {
		synchronized (this.reflectionCoeff) {
			this.reflectionCoeff = x;
		}
	}

	@Override
	public double collisionTime(Ball ball) {
		double collisionTime = Double.POSITIVE_INFINITY;
		for (Wall wall : walls) {
			collisionTime = Math.min(collisionTime, wall.collisionTime(ball));
		}
		return collisionTime;
	}

	@Override
	public String getTrigger() {
		synchronized (this.trigger) {
			return this.trigger;
		}
	}
	
	@Override
	public void setTrigger(String trigger) {
		synchronized (this.trigger) {
			this.trigger = trigger;
		}
	}

	@Override
	public BufferedImage generate(int L) {
		
		BufferedImage output = new BufferedImage(L, L, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, L, L);
        
        return output;
		
	}

	@Override
	public void reflectBall(Ball ball) {
		double collisionTime = this.collisionTime(ball);
		for (Wall wall : walls) {
			if (wall.collisionTime(ball) == collisionTime) {
				wall.reflectBall(ball);
			}
		}
	}
	
	@Override
	public String toString() {
		return "Square Bumper:" + this.name + " " + this.position();
	}
	
	@Override
	public int hashCode() {
		return this.x + this.y;
	}
	
	@Override
	public boolean equals(Object that) {
		return that instanceof SquareBumper && this.samePosition((SquareBumper) that);
	}

	private boolean samePosition(SquareBumper that) {
		return this.x == that.x && this.y == that.y;
 	}
	
	@Override
	public boolean ballOverlap(Ball ball) {
		final double x = ball.getBoardCenter().x();
		final double y = ball.getBoardCenter().y();
		final double radius = ball.getRadius();
		final double aX = this.position().x();
		final double aY = this.position().y();
		
		if (((x + radius > aX && x + radius < aX + WIDTH) ||
				(x - radius > aX && x - radius < aX + this.WIDTH) ) &&
				((y + radius > aY && y + radius < aY + this.HEIGHT) ||
				(y - radius > aY && y - radius < aY + this.HEIGHT))) {
		}
		
		return ((x + radius > aX && x + radius < aX + this.width()) ||
				(x - radius > aX && x - radius < aX + this.width()) ) &&
				((y + radius > aY && y + radius < aY + this.height()) ||
				(y - radius > aY && y - radius < aY + this.height()));
	}

	@Override
	public void takeAction() {
		// do nothing
	}

	@Override
	public void setCoverage(int[][] coverage) {
		int x = (int) this.position().x();
		int y = (int) this.position().y();
		coverage[y][x] = 1;
	}
}
