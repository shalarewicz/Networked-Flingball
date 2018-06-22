package flingball.gadgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.sound.sampled.Line;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import flingball.Ball;
import flingball.Orientation;
import physics.Angle;
import physics.Circle;
import physics.Physics;
import physics.Vect;

public class LeftFlipper implements Gadget {
	/**
	 * A gadget which can be used on a flingball board. A left flipper has size 1 L x 2 L. 
	 * a Left Flippers action rotates the flipper about it's pivot point which is in the 
	 * northwest corner by default. The opposite end of the flipper is in the southwest
	 * corner. A flipper is bound by a box of size 2L x 2L and does not cross the boundary 
	 * of this box at any point. The anchor of a left flipper is found in the upper left-hand 
	 * corner of it's bounding box. 
	 * 
	 * A left flipper's action alternates between rotating the flipper 90 degrees in the
	 * counterclockwise direction and 90 degrees in the clockwise direction about it's pivot. 
	 * During this time a flipper rotates at a speed of 1080 degrees per second. During rotation
	 * the rotation cannot switch directions. Furthermore, any action taken on flipper during its 
	 * rotation has no effect. If a ball impacts a flipper during its rotation there the speed of 
	 * rotation remains constant.  
	 * 
	 * A left flipper has a default coefficient of reflection of 0.95. However, if a ball collides
	 * with the flipper during its rotation the linear velocity of the flipper is taken into account.
	 * 
	 * A left flipper's orientation can be set in 90 degree intervals therefore a left flipper with 
	 * an orientation of 270 degrees would place the pivot in the southwest corner and the opposite 
	 * end int the southeast corner with rotation moving the end to the northwest corner. 
	 *
	 * 
	 */
	
	
	private final int x, y;
	private final String name;
	private final String trigger = NO_TRIGGER; //TODO Why is this final?
	private double reflectionCoeff = DEFAULT_FLIPPER_REFLECTION_COEFF;
	private  Circle pivot, tail;
	private  Wall port, starboard;
	private final int angularVelocity = 1080;
	private Orientation orientation = Orientation.ZERO;
	private final boolean rotating = false;
	private final int degreesRotated = 0;
	
	private final static double RADIUS = 0.25;
	private final static int HEIGHT = 2;
	private final static int WIDTH = 2;
	
	/*w
	 * AF(name, x, y, pivot, tail, port starboard) ::= A flipper called name with anchor (x,-y) and an 
	 * ovular shape depicted below
	 * 
	 *   __starboard___
	 *  /			   \	
	 * (pivot	    tail)
	 *  \____port______/
	 *  
	 * Rep Invariant ::= 
	 * 	 1. Port and starboard must be tangential to pivot ant tail 
	 *   2. Port and starboard are parallel to each other. 
	 * 	 3. Port and starboard must have equal lengths. 
	 * 	 4. The line connecting the endpoints of port and starboard must intersect the center of pivot or tail
	 * 	 5. The distance between the centers of pivot and tail must be equal to the length of port/starboard
	 *   6. Pivot and Tail have the same radius
	 * 	
	 * Safety from rep exposure:
	 * 		TODO
	 * 
	 * Thread Safety Argument
	 * 		TODO Immutable	
	 */
	
	private void checkRep() {
		/*
		 * Rep Invariant is satisfied if 
		 * 		pivot and tail have same radius (6)
		 * 		port and starboard are parallel (2) and separated by 2*radius and of equal length (3)
		 * 		endpoints of port and starboard are radius away from centers of pivot/tail
		 * 		pivot/tail centers are separated by length of port/starboard (5)
		 * 
		 * 1. is satisfied as port/starboard are parallel and 2*radius apart and the endpoints of each line 
		 *    are radius away from the center of pivot/tail respectively
		 * 4. As port/starboard are parallel of equal length, tangential to pivot tail and separated by 2*radius
		 *    then the line connecting the endpoints muct bisect the center of pivot/tail respectively. 
		 */
		// Check port and starboard are parallel (equal slopes)
		final double slopePort = slope(port);
		final double slopeStar = slope(starboard);
		assert slopePort == slopeStar : "Walls of LeftFlipper " + name + " are not parallel";
		
		final Vect portPivot = port.start();
		final Vect portTail = port.end();
		final Vect starPivot = port.start();
		final Vect starTail = port.end();
		final double pivotRadius = pivot.getRadius();
		final double tailRadius = tail.getRadius();
		final Vect pivotCenter = pivot.getCenter();
		final Vect tailCenter = tail.getCenter();
		
		// Check if port and starboard are tangential (distance from endpoints to center of circles equals radius)
		assert Math.sqrt(portPivot.distanceSquared(pivotCenter)) == pivotRadius : name + ": Port flipper wall bisects pivot";
		assert Math.sqrt(portTail.distanceSquared(tailCenter)) == tailRadius : name + ": Port flipper wall bisects tail";
		//TODO Because port/starboard are parallel this second check is unnecessary??
		assert Math.sqrt(starPivot.distanceSquared(pivotCenter)) == pivotRadius : name + ": Starboard flipper wall bisects pivot";
		assert Math.sqrt(starPivot.distanceSquared(tailCenter)) == tailRadius : name + ": Starboard flipper wall bisects tail";
		
		// Check pivot and tail same size
		assert pivotRadius == tailRadius : name + ": Pivot and Tail different sizes";
		
		// Check port and starboard same length
		assert portPivot.distanceSquared(portTail) == starPivot.distanceSquared(starTail) : name + ": Port and Starboard different length";
		
		// Check that distance between the lines is equal to the diameter of pivot/tail. 
		assert Math.sqrt(portPivot.distanceSquared(starPivot)) == pivotRadius * 2;
		
		// Check distance between the centers of pivot and tail must be equal to the length of port/starboard
		assert portPivot.distanceSquared(portTail) == pivotCenter.distanceSquared(tailCenter);
		
	}
	
	public LeftFlipper(String name, int x, int y) {
		this.name = name;
		this.x = x;
		this.y = -y;
		
		this.pivot = new Circle(x + RADIUS, -y - RADIUS, RADIUS);
		this.tail = new Circle(x + RADIUS, -y - 2 + RADIUS, RADIUS);
		this.port = new Wall(name + ":port", (double) x, -y -RADIUS, (double) x, -y -2 + RADIUS);
		this.starboard = new Wall(name + ":starboard", x + 2 * RADIUS, -y -RADIUS, x + 2 * RADIUS, -y -2 + RADIUS);

	//	this.checkRep();
		
	}
	
	public LeftFlipper(String name, int x, int y, Orientation o) {
		this.name = name;
		this.x = x;
		this.y = -y;
		this.orientation = o;
		
		switch (o) {
		case ZERO: {
			this.pivot = new Circle(x + RADIUS, -y - RADIUS, RADIUS);
			this.tail = new Circle(x + RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.port = new Wall(name + ":port", (double) x, -y -RADIUS, (double) x, -y - HEIGHT + RADIUS);
			this.starboard = new Wall(name + ":starboard", x + 2*RADIUS, -y -RADIUS, x + 2 * RADIUS, -y - HEIGHT + RADIUS);
			break;
		}
		case NINETY: {
			this.pivot = new Circle(x + WIDTH - RADIUS, -y - RADIUS, RADIUS);
			this.tail = new Circle(x + RADIUS, -y - RADIUS, RADIUS);
			this.port = new Wall(name + ": port", x + RADIUS, -y - 2*RADIUS, x + WIDTH - RADIUS, -y - 2*RADIUS);
			this.starboard = new Wall(name + ": starboard", x + RADIUS, (double) -y, x + WIDTH - RADIUS, (double) -y);
			break;
		}
		case ONE_EIGHTY: {
			this.pivot = new Circle(x + WIDTH - RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.tail = new Circle(x + WIDTH - RADIUS, -y - RADIUS, RADIUS);
			this.port = new Wall(name + ": port", x + WIDTH, -y - RADIUS, x + WIDTH, -y - HEIGHT + RADIUS);
			this.starboard = new Wall(name + ": starboard", x + WIDTH - 2*RADIUS, -y - RADIUS, x + WIDTH - 2*RADIUS, -y -2 + RADIUS);
			
			break;
		}
		case TWO_SEVENTY: {
			this.pivot = new Circle(x + RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.tail = new Circle(x + WIDTH - RADIUS, -y - HEIGHT + RADIUS, RADIUS);
			this.port = new Wall(name + ": port", x + RADIUS, -y - HEIGHT, x + WIDTH - RADIUS, -y - HEIGHT);
			this.starboard = new Wall(name + ": starboard", x + RADIUS, -y - HEIGHT + 2*RADIUS, x + WIDTH - RADIUS, -y - HEIGHT + 2*RADIUS);
			
			break;
		}
		default: {
			
			throw new RuntimeException("Should never get here. Invalid LeftFlipper Orientation");
		}
		}
	//	this.checkRep();
	}
	
	
	/**
	 * Returns the slope of a wall
	 * @param wall 
	 * @return the slope
	 */
	private double slope (Wall wall) {
		final double x1 = wall.start().x();
		final double x2 = wall.end().x();
		final double y1 = wall.start().x();
		final double y2 = wall.end().x();
		
		return (y2 - y1) / (x2 - x1);
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
		return this.reflectionCoeff;
	}

	@Override
	public void setReflectionCoefficient(double x) {
		this.reflectionCoeff = x;

	}

	@Override
	public double collisionTime(Ball ball) {
		double collisionTime = ball.timeUntilCircleCollision(this.pivot);
		if (this.rotating) {
			collisionTime = Math.min(ball.timeUntilRoatatingCircleCollision(this.tail, this.pivot.getCenter(), this.angularVelocity), collisionTime);
			collisionTime = Math.min(this.port.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), angularVelocity), collisionTime);
			collisionTime = Math.min(this.port.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), angularVelocity),collisionTime);
			
		} else {
			collisionTime = Math.min(ball.timeUntilCircleCollision(tail), collisionTime);
			collisionTime =  Math.min(this.port.collisionTime(ball), collisionTime);
			collisionTime = Math.min(this.starboard.collisionTime(ball), collisionTime);
		}
		return collisionTime;
	}

	@Override
	public void reflectBall(Ball ball) {
		// TODO Auto-generated method stub
		double collisionTime = this.collisionTime(ball);
		if (collisionTime == ball.timeUntilCircleCollision(this.pivot)) {
			ball.reflectCircle(this.pivot);
			return;
		}
		if (this.rotating) {
			//TODO Don't forget about when the ball hits a flipper moving away from it
			if (collisionTime == ball.timeUntilRoatatingCircleCollision(this.tail, this.pivot.getCenter(), angularVelocity)) {
				ball.reflectRotatingCircle(this.tail, this.pivot.getCenter(), angularVelocity, this.reflectionCoeff);
			} else if (collisionTime == this.port.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), this.angularVelocity)) {
				this.port.reflectBallRotating(ball, this.pivot.getCenter(), angularVelocity, reflectionCoeff);
			} else if (collisionTime == this.starboard.timeUntilRotatingWallCollision(ball, this.pivot.getCenter(), this.angularVelocity)) {
				this.starboard.reflectBallRotating(ball, this.pivot.getCenter(), angularVelocity, reflectionCoeff);
			} else {
				throw new RuntimeException("Rotating LeftFlipper reflection should never get here");
			}
			
		} else {
			
			if (collisionTime == ball.timeUntilCircleCollision(tail)) {
				ball.reflectCircle(this.tail);
			} else if (collisionTime == this.port.collisionTime(ball)){
				this.port.reflectBall(ball);
			} else if (collisionTime == this.port.collisionTime(ball)){
				this.starboard.reflectBall(ball);
			} else {
				throw new RuntimeException("Rotating LeftFlipper reflection should never get here");
			}
			
		}

	}

	@Override
	public String getTrigger() {
		return this.trigger;
	}

	@Override
	public void takeAction() {
		// TODO this is instantaneous. Need to break it down to 1080 degrees /s 
		// TODO Just set it in motion and let it do it's own thing just like a ball. 
		// this will need to be done by board. 
		// 1080 /90 --> 0.08333 s of rotation. Frame rate is 0.005 s
		// 1080 * 0.005 = 5.4 degrees per animation
		this.rotate(new Angle(Math.PI / 180 * this.angularVelocity * 0.005));//TODO Add frame rate
		// TODO Use a while loop either in board or here (in a separate thread) to keep rotating
		// Flippers will need their own thread for rotation. As long as the type is threadsafe animation 
		// which has its own thread will be able to graph the correct values. 
		// Flipper will also need a thread to listen for key events This may work better on board. 
	}
	
	/**
	 * Rotates at a constant angular velocity of 1080 degrees per second to a position 90 degrees away from its starting position 
	 * in alternating counterclockwise and clockwise directions. 
	 * @param angle by which the flipper is rotated
	 */
	private void rotate(Angle angle) {
			// Rotate counterclockwise
			this.tail = Physics.rotateAround(this.tail, this.pivot.getCenter(), angle);
			//TODO rotate walls. 
			this.port = port.rotateAround(this.pivot.getCenter(), angle);
			this.starboard = starboard.rotateAround(this.pivot.getCenter(), angle);
	}

	@Override
	public boolean ballOverlap(Ball ball) {
		// TODO Auto-generated method stub
		return Physics.distanceSquared(ball.getCartesianCenter(), this.tail.getCenter()) >= ball.getRadius() &&
			   Physics.distanceSquared(ball.getCartesianCenter(), this.pivot.getCenter()) >= ball.getRadius();
			   
			   // TODO Use graphics2d.hit()
	}


	@Override
	public BufferedImage generate(int L) {
		BufferedImage output = new BufferedImage(500, 500, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = (Graphics2D) output.getGraphics();
        
        graphics.setColor(Color.ORANGE);
        graphics.fillArc((int) ((this.pivot.getCenter().x() - RADIUS) * L), (int) ((-this.pivot.getCenter().y() - RADIUS) * L), 
        		(int) (RADIUS * 2 * L), (int) (RADIUS * 2 * L), 0, 360);
        graphics.fillArc((int) ((this.tail.getCenter().x() - RADIUS) * L), (int) ((-this.tail.getCenter().y() - RADIUS) * L), 
        		(int) (RADIUS * 2 * L), (int) (RADIUS * 2 * L), 0, 360);
        System.out.println(-this.tail.getCenter().y());
        System.out.println(this.port);
        System.out.println(this.starboard);
        System.out.println(this.tail);
        System.out.println(this.pivot);
        final int[] xPoints = {(int) (this.port.start().x() * L), (int) (this.port.end().x() * L),(int) (this.starboard.end().x() * L), (int) (this.starboard.start().x() * L) };
        final int[] yPoints = {(int) (-this.port.start().y() * L), (int) (-this.port.end().y() * L),(int) (-this.starboard.end().y() * L), (int) (-this.starboard.start().y() * L) };
        graphics.fillPolygon(xPoints, yPoints, 4);
        return output;
	}
	
//	public static void main(String[] args) {
//		LeftFlipper flipper = new LeftFlipper("test", 0, 0, Orientation.TWO_SEVENTY);
//		flipper.rotate(Angle.RAD_PI_OVER_FOUR);
//		JFrame frame = new JFrame("Flipper Test");
//		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
//		frame.add(new JLabel(new ImageIcon(flipper.generate(100))));
//		frame.pack();
//	    frame.setVisible(true);
//	}

	@Override
	public void setCoverage(int[][] coverage) {
		int x = (int) this.position().x();
		int y = (int) this.position().y();
		coverage[y][x] = 1;
		for (int i = x; x < x + WIDTH; i++) {
			for (int j = y; j < y + HEIGHT; j++) {
				assert coverage[j][i] != 1 : name + ": LeftFlipper coverage overlap";
				coverage[j][i] = 1;
			}
		}
	}

}
