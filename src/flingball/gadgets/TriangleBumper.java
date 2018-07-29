package flingball.gadgets;

import java.awt.Color;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import flingball.Ball;
import flingball.Orientation;
import physics.Vect;

/**
 * TriangleBumper represents a triangle bumper which can be played
 * as a gadget on a flingball board. A TriangleBumper has a height and 
 * width of 1 L and its anchor is located in the upper left-hand corner of it's bounding box. 
 * 
 * A TriangleBumper can have one of four Orientations. The default Orientation for a TriangleBumper
 * is 0 degrees and places one corner in the northeast, one corner in the northwest, and the last 
 * corner in the southwest. The diagonal goes from the southwest corner to the northeast corner. 
 * The TriangleBumper can then be rotated 90 degrees clockwise to be put in the next Orientation
 * 
 * A TriangleBumper has no default action but can trigger a provided Board Action
 * @author Stephan Halarewicz
 */
public class TriangleBumper implements Bumper {
	
	
	private String name;
	private final int x, y;
	private int HEIGHT = 1;
	private int WIDTH = 1;
	
	private Orientation orientation = Orientation.ZERO;
	private Set<Wall> walls;
	
	private String trigger = Gadget.NO_TRIGGER;
	private Double reflectionCoeff = Bumper.DEFAULT_REFLECTION_COEFF;
	
	/*
	 * AF(x, y , orientation) - Triangle pumper with anchor x, -y and orientation named name
	 * 		The shape is represented by the walls in wall. 
	 * 
	 * Rep Invariant
	 * 		walls.size() == 3
	 * 		for each endpoint of a wall there exists only one other wall with that endpoint
	 * 		0 <= reflectionCoeff <= 1
	 * 
	 * Safety from rep exposure
	 * 		Only immutable or primitive types are ever returned. 
	 * Thread Safety Argument
	 * 		All fields except trigger and reflectionCoeff are final or immutable. 
	 * 		The elements of the set walls are never modified and Wall is immutable. 
	 * 		Locks are obtained before using or changing trigger or reflectionCoeff
	 */

	private void checkRep() {
		assert this.walls.size() == 3;
		Map<Vect, Integer> endPoints = new HashMap<Vect, Integer>();
		for (Wall wall : walls) {
			Vect start = wall.start();
			Vect end = wall.end();
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
		for (Vect v : endPoints.keySet()) {
			// In a triangle each vertex must be an end point of exactly two sides. If not then
			// a triangle is not formed. This does not work for squares
			assert endPoints.get(v) == 2 : "TriangleBumper: " + name + " doesn't form a triangle";
		}
		
		assert endPoints.size() == 3;
		assert 0 <= reflectionCoeff && reflectionCoeff <=1 : "SquareBumper: " + name + " invalid reflection coefficient";
	}
	
	/**
	 * Create a new triangle bumper at the position (x,y) on a flingball board with
	 * <code>Orientation ZERO</code>.
	 * @param name Name of the square bumper
	 * @param x x-coordinate of the bumper on a flingball board
	 * @param y y-coordinate of the bumper on a flingball board
	 */
	public TriangleBumper(String name, int x, int y) {
		this.x = x;
		this.y = -y;
		this.name = name;
		
		final Wall side1 = new Wall(name + " side1", x, -y, x+1, -y);
		final Wall side2 = new Wall(name + " side2", x+1, -y, x+1, -y-1);
		final Wall hypotenuse = new Wall(name + " hypotenuse", x, -y, x+1, -y-1);
		walls = new HashSet<Wall>(Arrays.asList(side1, side2, hypotenuse));
		checkRep();
	}
	
	/**
	 * Create a new triangle bumper at the position (x,y) on a flingball board with 
	 * the specified <code>Orientation</code>.
	 * @param name Name of the square bumper
	 * @param x x-coordinate of the bumper on a flingball board
	 * @param y y-coordinate of the bumper on a flingball board
	 * @param orientation Orientation of the Triangle Bumper.
	 */
	public TriangleBumper(String name, int x, int y, Orientation orientation) {
		this.x = x;
		this.y = -y;
		this.orientation = orientation;
		this.name = name;
		
		switch (this.orientation) {
		case ZERO:{
			final Wall side1 = new Wall(name + " side1", x, -y, x+1, -y);
			final Wall side2 = new Wall(name + " side2", x, -y, x, -y-1);
			final Wall hypotenuse = new Wall(name + " hypotenuse", x, -y-1, x+1, -y);
			walls = new HashSet<Wall>(Arrays.asList(side1, side2, hypotenuse));
			checkRep();
			return;
		}
		case NINETY: {
			final Wall side1 = new Wall(name + " side1", x+1, -y, x+1, -y-1);
			final Wall side2 = new Wall(name + " side2", x+1, -y, x, -y);
			final Wall hypotenuse = new Wall(name + " hypotenuse", x, -y, x+1, -y-1);
			walls = new HashSet<Wall>(Arrays.asList(side1, side2, hypotenuse));
			checkRep();
			return;
		}		
		case ONE_EIGHTY: {
			final Wall side1 = new Wall(name + " side1", x+1, -y-1, x+1, -y);
			final Wall side2 = new Wall(name + " side2", x+1, -y-1, x, -y-1);
			final Wall hypotenuse = new Wall(name + " hypotenuse", x, -y-1, x+1, -y);
			walls = new HashSet<Wall>(Arrays.asList(side1, side2, hypotenuse));
			checkRep();
			return;
		}
		
		case TWO_SEVENTY: {
			final Wall side1 = new Wall(name + " side1", x, -y-1, x, -y);
			final Wall side2 = new Wall(name + " side2", x, -y-1, x+1, -y-1);
			final Wall hypotenuse = new Wall(name + " hypotenuse", x, -y, x+1, -y-1);
			walls = new HashSet<Wall>(Arrays.asList(side1, side2, hypotenuse));
			checkRep();
			return;
		}
		
		default: throw new RuntimeException("Should never get here. Cannot make triangle bumper");
		}
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
		return WIDTH;
	}

	@Override
	public double getReflectionCoefficient() {
		synchronized (this.reflectionCoeff) {
			return this.reflectionCoeff;
		}
	}
	
	@Override
	public void setReflectionCoefficient(double x) {
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
	public void takeAction() {
		Orientation orientation;
		switch (this.orientation) {
		case ZERO:
			orientation = Orientation.NINETY;
			break;
		case NINETY:
			orientation = Orientation.TWO_SEVENTY;
			break;
		case ONE_EIGHTY:
			orientation = Orientation.ONE_EIGHTY;
			break;
		case TWO_SEVENTY:
			orientation = Orientation.ZERO;
			break;
		default:
			throw new RuntimeException("Should never get here");
		}
		
		TriangleBumper newTriangle = new TriangleBumper(this.name, this.x, -this.y, orientation);
		this.walls = newTriangle.walls;
		this.orientation = newTriangle.orientation;
	}
	
		@Override
	public BufferedImage generate(int L) {
		BufferedImage output = new BufferedImage(L, L, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        graphics.setColor(Color.GREEN);
        switch (this.orientation) {
        case ZERO:{
        	final int[] xPoints = {0, L, 0};
        	final int[] yPoints = {0, 0, L};
        	graphics.fillPolygon(xPoints, yPoints, 3);
        	return output;
        }
        case NINETY:{
        	final int[] xPoints = {0, L, L};
        	final int[] yPoints = {0, 0, L};
        	graphics.fillPolygon(xPoints, yPoints, 3);
        	return output;
        }
        case ONE_EIGHTY:{
        	final int[] xPoints = {L, L, 0};
        	final int[] yPoints = {0, L, L};
        	graphics.fillPolygon(xPoints, yPoints, 3);
        	return output;
        }
        case TWO_SEVENTY:{
        	final int[] xPoints = {0, L, 0};
        	final int[] yPoints = {0, L, L};
        	graphics.fillPolygon(xPoints, yPoints, 3);
        	return output;
        }
        default:
        	throw new RuntimeException("Should never get here. Cannot generate triangle bunper");
        
        }
        
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
	public boolean ballOverlap(Ball ball) {
		Vect anchor = ball.getAnchor();
		Double d = ball.getRadius() * 2;
		Area ballShape = new Area (new Arc2D.Double(anchor.x(), anchor.y(), d, d, 0, 360, Arc2D.CHORD));
		 switch (this.orientation) {
	        case ZERO:{
	        	final int[] xPoints = {x, x+1, x};
	        	final int[] yPoints = {y, y, y-1};
	        	Area triangle = new Area (new Polygon(xPoints, yPoints, 3));
	        	ballShape.intersect(triangle);
	        	return !ballShape.isEmpty();
	        }
	        case NINETY:{
	        	final int[] xPoints = {x, x+1, x+1};
	        	final int[] yPoints = {y, y, y-1};
	        	Area triangle = new Area (new Polygon(xPoints, yPoints, 3));
	        	ballShape.intersect(triangle);
	        	return !ballShape.isEmpty();
	        }
	        case ONE_EIGHTY:{
	        	final int[] xPoints = {x+1, x+1, x};
	        	final int[] yPoints = {y, y-1, y-1};
	        	Area triangle = new Area (new Polygon(xPoints, yPoints, 3));
	        	ballShape.intersect(triangle);
	        	return !ballShape.isEmpty();
	        }
	        case TWO_SEVENTY:{
	        	final int[] xPoints = {x, x+1, x};
	        	final int[] yPoints = {y, y-1, y-1};
	        	Area triangle = new Area (new Polygon(xPoints, yPoints, 3));
	        	ballShape.intersect(triangle);
	        	return !ballShape.isEmpty();
	        }
	        default:
	        	throw new RuntimeException("Should never get here. Cannot generate triangle bunper");
	        
	        }
	}
	@Override
	public String toString() {
		return "Triangle Bumper{" + this.name + " " + this.position() + " " + this.orientation +"}";
	}
	
	@Override
	public void setCoverage(int[][] coverage) {
		int x = (int) this.position().x();
		int y = (int) this.position().y();
		coverage[y][x] = 1;
	}
	
}
