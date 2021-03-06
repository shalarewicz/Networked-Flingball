package flingball.gadgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import flingball.Ball;
import physics.Vect;

/**
 * An <code>absorber</code> is a gadget which can be placed on a flingball board. An absorber
 * is a rectangular gadget which is kL wide and mL tall where k,l <= 20. An 
 * absorber acts as a ball return mechanism during a game of flingball. When a <code>ball</code>
 * strikes an absorber the ball is held in the bottom right-hand corner of the 
 * absorber. There is no limit to the amount of balls an absorber can hold in 
 * this location. 
 * 
 * When an absorber's action is triggered one of it's captured balls (if any) will 
 * be shot towards the top of the board with a speed of 50 L/s. If the absorber is
 * not holding a ball or if the previously fired ball has not left the absorber, then
 * no action is taken. 
 * 
 * Absorber's can be made self-triggering by connecting its trigger to its own 
 * action. Balls that hit a self-triggering absorber will always leave the absorber. 
 * 
 */
public class Absorber implements Gadget {
	
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
	 * 		Only final immutable or primitive types are returned. 
	 * 
	 * Thread Safety Argument:
	 * 		A lock is obtained on balls before any modifications are made. 
	 *		All other fields are final or immutable and are never changed. 
	 *		takeAction() is a synchronized method which prevents multiple threads from taking simultaneous actions
	 */
	
	private void checkRep() {
		assert walls.size() == 4;
		Map<Vect, Integer> endPoints = new HashMap<Vect, Integer>();
		for (Wall wall : walls) {
			Vect start = wall.start();
			Vect end = wall.end();
			assert start.distanceSquared(end) == this.height * this.height ||
					start.distanceSquared(end) == this.width * this.width;
			
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
			// In a rectangle each vertex must be an end point of exactly two sides. Because walls
			// is a set there can be no duplicate entries therefore if a vertex exists twice, it 
			// was added by two distinct lines. So if each vertex occurs twice the four lines form a 
			// rectangle. Furthermore each line must lie on the perimeter as it has length equal to 
			// height or width
			assert endPoints.get(v) == 2 : "SquareBumper: " + name + " doesn't form a square";
		}
		assert endPoints.size() == 4;
		assert endPoints.containsKey(new Vect(x, y)) && endPoints.containsKey(new Vect(x + this.width, y - this.height));
		
	}
	
	/**
	 * Constructs a new Absorber with position (x,y) on a flingball board. 
	 * 
	 * @param name Name of the absorber
	 * @param x x-coordinate of the absorber on a flingball board
	 * @param y y-coordinate of the absorber on a flingball board
	 * @param width width of the absorber in L units
	 * @param height height of the absorber in L units
	 */
	public Absorber(String name, int x, int y, int width, int height) {
		this.x = x;
		this.y = -y;
		this.height = height;
		this.width = width;
		this.name = name;
		
		final Wall top = new Wall(name + " top", x, - y, x + width, -y);
		final Wall bottom = new Wall(name + " bottom", x, -y - height, x + width, -y-height);
		final Wall left = new Wall(name + " left", x, - y, x, -y-height);
		final Wall right = new Wall(name + " right", x + width, -y, x + width, -y-height);
		
		walls.addAll((Arrays.asList(top, bottom, left, right)));
		
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
		return this.height;
	}

	@Override
	public int width() {
		return this.width;
	}

	@Override
	public double collisionTime(Ball ball) {
		double collisionTime = Double.POSITIVE_INFINITY;
		if (!ballOverlap(ball)) {
			for (Wall wall : walls) {
				collisionTime = Math.min(collisionTime, wall.collisionTime(ball));
			}
		}
		return collisionTime;
	}

	@Override
	public void reflectBall(Ball ball) {
		ball.setVelocity(new Vect(0, 0));
		final double r = ball.getRadius();
		ball.setBoardPosition(new Vect(x + width - r, -y + height - r));
		ball.trap();
		synchronized (this.balls) {
			balls.addLast(ball);
		}
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
	// synchronized prevents other threads from firing balls at the same time. 
	public synchronized void takeAction() {
		// TODO Skip action if the previous ball has not left the absorber
		synchronized (this.balls) {
			if (!this.balls.isEmpty()) {
				// second lock necessary to avoid race conditions with methods such as generate or reflectVall
				Ball toFire = balls.removeFirst();
				toFire.release();
				toFire.setVelocity(new Vect(0, -50));
				double r = toFire.getRadius();
				toFire.setBoardPosition(new Vect(x + width - r, -(y - height + r))); 
			}
		}
	}

	@Override
	public boolean ballOverlap(Ball ball) {
		final double x = ball.getBoardCenter().x();
		final double y = ball.getBoardCenter().y();
		final double radius = ball.getRadius();
		final double aX = this.x;
		final double aY = -this.y;
		
		return ((x + radius > aX && x + radius < aX + this.width) ||
				(x - radius > aX && x - radius < aX + this.width) ) &&
				((y + radius > aY && y + radius < aY + this.height) ||
				(y - radius > aY && y - radius < aY + this.height));
	}

	@Override
	public BufferedImage generate(int L) {
		BufferedImage output = new BufferedImage(L*this.width, L*this.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        final Color outer = new Color(254, 26, 135); //pink
        final Color inner = new Color(220, 60, 83); // dark red
        
        graphics.setColor(outer);
        graphics.fillRoundRect(0, 0, width*L, height*L, 10, 10);
        
        graphics.setColor(inner);
        final int scaling = 5;
        graphics.fillRoundRect(L / scaling, L / scaling, this.width * L - (2*L / scaling), this.height * L - (2*L / scaling), 10, 10);
        
        synchronized (this.balls) {
	        if (this.balls.size() > 0) {
	        	graphics.setColor(Color.BLUE);
	        	Ball toDraw = balls.getFirst();
	        	final double diameter = toDraw.getRadius() * 2;
	        	
	        	final int xAnchor = (int) ((this.width - diameter) * L);
	        	final int yAnchor = (int) ((this.height - diameter) * L);
	        	
	        	final ImageObserver NO_OBSERVER_NEEDED = null;
	        	
	        	graphics.drawImage(toDraw.generate(L), xAnchor, yAnchor, NO_OBSERVER_NEEDED);
	        	
	        }
        }
        return output;
	}

	@Override
	public void setCoverage(int[][] coverage) {
		int x = (int) this.position().x();
		int y = (int) this.position().y();
		
		for (int j = y; j < y + this.height; j++) {
			for (int i = x; i < x + this.width; i++) {
				if (coverage[j][i] == 1) {
					throw new RuntimeException("Absorber Coverage overlaps with another gadget at [" 
							+ j +"]["+ i +"]");
				}
			coverage[j][i] = 1;
			}
		}
		coverage[y - 1][x + this.width() - 1] = 1;

	}

	/**
	 * Fires all balls currently trapped by the absorber. 
	 */
	public void fireAll() {
		new Thread(() -> {
			synchronized (this.balls) {
				while (!this.balls.isEmpty()) {
					this.takeAction();
				}
			}
		}).start();
	}

}
