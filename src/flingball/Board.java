package flingball;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import physics.Vect;

public class Board {
	
	/**
	 * A board on which the game flingball can be played. Objects which can be 
	 * played on a flingball board include balls and gadgets. A gadget is any object 
	 * that implements the the Gadget interface (including Square, Triangle and
	 * Circle Bumpers, Absorbers, Portals and Flippers. 
	 * 
	 * A flingabll board is a 20L x 20L grid with the origin in the upper
	 * left-hand corner. Gadgets are placed in one or more squares on the grid. 
	 * No two gadgets are allowed to occupy the same square on the grid. The default
	 * gadget limit for a board is 60. 
	 * 
	 * Balls on the flingball travel with a velocity between 0 L/s and 200 L/s.
	 * 
	 * A flingball board allows gadgets to to trigger the board actions specified by 
	 * the Action enumeration. These actions are in addition to the default actions
	 * for each Gadget and can override the default action. 
	 * 
	 * Flingball can also be played with multiple players via a hosted WebServer. 
	 * Clients have the ability to attach boards either horizontally or vertically
	 * to create a multi-player environment. If two boards are joined the name of 
	 * the joined board will appear on the respective wall. 
	 * 
	 * A flingball board accounts for both gravity and friction. Players have the
	 * ability to specify these values when creating a board. If not specified, the 
	 * default values are used. the default values are as follows:
	 * <ol>
	 * <li>Gravity = 25 L</li>
	 * <li>Friction 1 = 0.025 L</li>
	 * <li>Friction 2 = 0.025 L</li>
	 * </ol>
	 * 
	 * Flingball boards are generated from board files (.fb). Board files must 
	 * match the following format:
	 * 
	 * # Comment. Lines starting with # are ignored
	 * 
	 * # the board must be the first line in the file
	 * board name=NAME (gravity=FLOAT)? (friction1=FLOAT)? (friction2=FLOAT)?
	 * 
	 * ball name=NAME x=FLOAT y=FLOAT xVelocity=FLOAT yVelocity=FLOAT
	 * 
	 * squareBumper name=NAME x=INTEGER y=INTEGER
	 * circleBumper name=NAME x=INTEGER y=INTEGER
	 * triangleBumper name=NAME x=INTEGER y=INTEGER (orientation=0|90|180|270)?
	 * absorber name=NAME x=INTEGER y=INTEGER width=INTEGER height=INTEGER
	 * rightFlipper name=NAME x=INTEGER y=INTEGER (orientation=0|90|180|270)?
	 * leftFlipper name=NAME x=INTEGER y=INTEGER (orientation=0|90|180|270)?
	 * portal name=NAME x=INTEGER y=INTEGER (otherBoard=NAME)? otherPortal=NAME
	 * 
	 * fire trigger=NAME action=NAME
	 * 
	 * keydown key=KEY action=NAME
	 * keyup key=KEY action=NAME
	 * 
	 * INTEGER ::= [0-9]+
	 * FLOAT ::= -?([0-9]+.[0-9]*|.?[0-9]+)
	 * NAME ::= [A-Za-z_][A-Za-z_0-9]*
	 * 
	 */
	
	/*
	 * AF(height, width, gadgets, balls, triggers, neighbors) ::= 
	 * 		Flingball board of size width*L x height*L. The board contains all gadgets, balls and triggers. The board is
	 *  	connected to all neighbors. 
	 * Rep Invariant = 
	 * 		No two gadgets have same anchor
	 * 		All gadgets are entirely on the board
	 * 		All balls are entirely on the board or a neighboring board
	 * 		No two gadgets or balls have the same name
	 * 		No balls or gadgets overlap with each other. Excluding balls trapped in absorbers
	 * 		All keys and values in triggers are on the board
	 * 		All keys in boardTriggers are gadgets on the board
	 * 		All items in the lists of values in keyTriggers are on the board
	 * 		Each neighbor is connected to this board
	 * TODO: Safety from rep exposure
	 * 		coverage is exposed in gadget.setCoverage();
	 * TODO: Thread Safety Argument
	 */
	
	private void checkRep() {
		//TODO 
		Set<Vect> positions = new HashSet<Vect>();
		Set<String> names = new HashSet<String>();
		for (Gadget gadget : gadgets) {
			final Vect position = gadget.position();
			assert positions.add(position) : "Duplicate gadget positon: " + gadget;
			
			assert position.x() >= 0 : "x < 0: " + gadget;
			assert position.y() >= 0 : "y < 0: " + gadget;
			
			assert position.x() + gadget.width() <= WIDTH : "x + WIDTH > " + WIDTH + ": " + gadget;
			assert position.y() + gadget.height() <= HEIGHT : "x + HEIGHT > " + HEIGHT + ": " + gadget;
			
			assert names.add(gadget.name()) : "Duplicate name - gadget: " + gadget;
			
			//TODO Check for ball overlaps
		}
		
		for (Ball ball : balls) {
			assert names.add(ball.name()) : "Dublicate name - ball: " + ball;
			final Vect position = ball.getAnchor();
			
			assert position.x() >= 0 : "x < 0: " + ball;
			assert position.y() >= 0 : "y < 0: " + ball;
			
			//TODO Decide on how balls should be moved to other boards. Either a jump or smooth transition
			// checkRep would need to allow a ball simultaneously on two boards. 
			assert position.x() + ball.getRadius() * 2 <= WIDTH : "x + WIDTH > " + WIDTH + ": " + ball;
			assert position.y() + ball.getRadius() * 2 <= HEIGHT : "x + HEIGHT > " + HEIGHT + ": " + ball;
		}
		
		for (Gadget gadget : triggers.keySet()) {
			assert gadgets.contains(gadget) : "Trigger not in gadgets: " + gadget + " triggers " + triggers.get(gadget);
			for (Gadget actionGadget : triggers.get(gadget)) {
				assert gadgets.contains(actionGadget) : "Triggered gadget not in gadgets" + actionGadget;
			}
		}
		
		for (Gadget gadget : boardTriggers.keySet()) {
			assert gadgets.contains(gadget) : "Board trigger not in gadgets: " + gadget + " triggers " + boardTriggers.get(gadget);
		}
		
		for (KeyEvent key : keyTriggers.keySet()) {
			for (Gadget actionGadget : keyTriggers.get(key)) {
				assert gadgets.contains(actionGadget) : "Key Triggered gadget not in gadgets" + actionGadget;
			}
		}
		
		for (Board neighbor : neighbors.keySet()) {
			assert neighbor.neighbors.containsKey(this) : "Connection not symmetric" + this.NAME + neighbor.NAME;
			
			switch (neighbors.get(neighbor)) {
			case TOP:
				// If this board is connected to the neighboring board at the top then the neighboring board
				// must be connected to this board at the bottom
				assert neighbor.neighbors.get(this).equals(Wall.BOTTOM);
				break;
			case BOTTOM:
				assert neighbor.neighbors.get(this).equals(Wall.TOP);
				break;
			case LEFT:
				assert neighbor.neighbors.get(this).equals(Wall.RIGHT);
				break;
			case RIGHT:
				assert neighbor.neighbors.get(this).equals(Wall.LEFT);
				break;
			default:
				break;
			}
		}
	}
	
	// Default Values
	public static final int GADGET_LIMIT = 60;
	public static final double DEFAULT_GRAVITY = 25.0;
	public static final double DEFAULT_FRICTION_1 = 0.025;
	public static final double DEFAULT_FRICTION_2 = 0.025;
	
	// Board Params
	private final String NAME;
	private final int HEIGHT = 20;
	private final int WIDTH = 20;
	
	// Default Constants
	private double gravity = DEFAULT_GRAVITY;
	private double friction1 = DEFAULT_FRICTION_1;
	private double friction2 = DEFAULT_FRICTION_2;

	private List<Gadget> gadgets = new ArrayList<Gadget>();
	private List<Ball> balls = new ArrayList<Ball>();
	
	private ConcurrentMap<Board, Wall> neighbors = new ConcurrentHashMap<Board, Wall>();
	
	private ConcurrentMap<Gadget, List<Gadget>> triggers = new ConcurrentHashMap<Gadget, List<Gadget>>();
	private ConcurrentMap<Gadget, List<Action>> boardTriggers = new ConcurrentHashMap<Gadget, List<Action>>();
	
	// TODO KeyName vs KeyEvent
	private ConcurrentMap<KeyEvent, List<Gadget>> keyTriggers = new ConcurrentHashMap<KeyEvent, List<Gadget>>();
	//TODO Support board actions for keys? This could allow the player to spam the board
	private ConcurrentMap<KeyEvent, List<Action>> keyBoardTriggers = new ConcurrentHashMap<KeyEvent, List<Action>>();
	
	/**
	 * Locations for a neighboring board or a border wall
	 */
	private enum Wall {
		TOP, BOTTOM, LEFT, RIGHT
	}
	
	/**
	 * List of actions which can be performed on the board.
	 */
//	public enum Action {
//		FIRE_ALL, ADD_BALL, ADD_SQUARE, ADD_CIRCLE, ADD_TRIANGLE, ADD_ABSORBER, REVERSE_BALLS
//		//TODO REMOVE_BALL, REMOVE_BUMPER, REMOVE_ABSORBER, SPLIT
//	}
	
	//TODO FActory methods

	/**
	 * Constructs a blank board with the provided name and constants
	 * @param name name of the board
	 * @param gravity value for gravity on the board
	 * @param friction1 value of mu1
	 * @param friction2 value of mu2
	 */
	public Board(String name, double gravity, double friction1, double friction2) {
		this.NAME = name;
		this.gravity = gravity;
		this.friction1 = friction1;
		this.friction2 = friction2;
		checkRep();
	}
	
	//Instance Methods
	
	/**
	 * Adds a gadget to the flingball board using the gadgets position. If the Gadget has a position
	 * not on the board, it is not added. 
	 * @param gadget gadget to be added
	 */
	public void addGadget(Gadget gadget) {
		//TODO
	}
	
	/**
	 * Adds a ball to the flingball board using the ball's position and velocity. If the ball has a position 
	 * not on the board, it is not added. If the ball has a velocity >= 200 L / s, the velocity is set to 200. 
	 * @param ball ball to be added. 
	 */
	public void addBall(Ball ball) {
		//TODO
	}
	
	/**
	 * Adds a trigger and it's associated action to the board. If the trigger is not the name of a 
	 * Gadget currently on the board, no action is added. 
	 * 
	 * @param trigger name of Gadget which serves as the trigger for the action
	 * @param action action to be taken when the trigger is triggered. 
	 */
	public void addAction(String trigger, Action action) {
		//TODO
	}
	
	
	/**
	 * Connects two boards at the given Wall. The conneection is symmetric. That is that if 
	 * Board A is connected to Board B a the TOP then Board B is automatically connected to Board A 
	 * at the BOTTOM
	 * 
	 * @param neighbor board being connected to this board
	 * @param border location on this board where the other board is connected
	 */
	public void addNeightbor(Board neighbor, Wall border) {
		//TODO
	}

	/**
	 * Sets the board into action for the given amount of time. All balls are moved taking into account the effects
	 * of gravity and friction. All actions which are triggered during this time are taken. 
	 * @param time length of time the board is played. 
	 */
	private void play(final double time) {
		//TODO Consider starting a new thread for each Ball on the board. This allows for simultaneous motion and 
		// easier calculation of ball on ball collisions
	}

	/**
	 * Moves one ball on the board for the given amount of time accounting for the effects of friction
	 * and gravity. Any actions that are triggered during this time are taken. 
	 * @param ball
	 * @param time
	 */
	private void moveOneBall(Ball ball, final double time) {
		//TODO
	}

	/**
	 * Takes an action on the board. 
	 * @param action action to be taken. 
	 */
	private void takeAction(Action action) {
		//TODO
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
}
