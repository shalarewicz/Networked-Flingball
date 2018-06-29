package flingball;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.JPanel;

import flingball.gadgets.*;
import physics.Physics;
import physics.Vect;

public class Board extends JPanel{
	
	/**
	 * A board on which the game flingball can be played. Objects which can be 
	 * played on a flingball board include balls and gadgets. A gadget is any object 
	 * that implements the the Gadget interface (including Square, Triangle and
	 * Circle Bumpers, Absorbers, Portals and Flippers. 
	 * 
	 * A flingball board is a 20L x 20L grid with the origin in the upper
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
			
			//TODO Check for ball overlaps in bumpers allow portals and absorbers
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
		
		for (String key : keyUpTriggers.keySet()) {
			for (Gadget actionGadget : keyUpTriggers.get(key)) {
				assert gadgets.contains(actionGadget) : "Key Triggered gadget not in gadgets" + actionGadget;
			}
		}
		for (String key : keyDownTriggers.keySet()) {
			for (Gadget actionGadget : keyDownTriggers.get(key)) {
				assert gadgets.contains(actionGadget) : "Key Triggered gadget not in gadgets" + actionGadget;
			}
		}
		
		for (Board neighbor : neighbors.keySet()) {
			assert neighbor.neighbors.containsKey(this) : "Connection not symmetric" + this.NAME + neighbor.NAME;
			
			switch (neighbors.get(neighbor)) {
			case TOP:
				// If this board is connected to the neighboring board at the top then the neighboring board
				// must be connected to this board at the bottom
				assert neighbor.neighbors.get(this).equals(Neighbor.BOTTOM);
				break;
			case BOTTOM:
				assert neighbor.neighbors.get(this).equals(Neighbor.TOP);
				break;
			case LEFT:
				assert neighbor.neighbors.get(this).equals(Neighbor.RIGHT);
				break;
			case RIGHT:
				assert neighbor.neighbors.get(this).equals(Neighbor.LEFT);
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
	public final String NAME;
	public final int HEIGHT = 20;
	public final int WIDTH = 20;
	private final Wall TOP = new Wall("TOP", 0, 0, WIDTH, 0);
	private final Wall BOTTOM = new Wall("BOTTOM", 0, -HEIGHT, WIDTH, -HEIGHT);
	private final Wall LEFT = new Wall("LEFT", 0, 0, 0, -HEIGHT);
	private final Wall RIGHT = new Wall("RIGHT", WIDTH, 0, WIDTH, -HEIGHT);
	private final int[][] gadgetCoverage = new int[WIDTH + 1][HEIGHT + 1];
	
	/**
	 * A board is 20 L x 20 L wide. where L is the number of pixels. 
	 */
	static final int L = 40;
	
	// Default Constants
	private double gravity = DEFAULT_GRAVITY;
	private double friction1 = DEFAULT_FRICTION_1;
	private double friction2 = DEFAULT_FRICTION_2;

	// Objects on board
	private List<Gadget> gadgets = new ArrayList<Gadget>();
	private List<Ball> balls = new ArrayList<Ball>();
	private Map<Portal, List<String>> portals = new HashMap<Portal, List<String>>();
	private Set<Absorber> absorbers = new HashSet<Absorber>();
	private final Set<Wall> walls = new HashSet<Wall>(Arrays.asList(TOP, BOTTOM, LEFT, RIGHT));
	
	private ConcurrentMap<Board, Neighbor> neighbors = new ConcurrentHashMap<Board, Neighbor>();
	
	private ConcurrentMap<Gadget, List<Gadget>> triggers = new ConcurrentHashMap<Gadget, List<Gadget>>();
	private ConcurrentMap<Gadget, List<Action>> boardTriggers = new ConcurrentHashMap<Gadget, List<Action>>();
	
	private ConcurrentMap<String, List<Gadget>> keyUpTriggers = new ConcurrentHashMap<String, List<Gadget>>();
	private ConcurrentMap<String, List<Gadget>> keyDownTriggers = new ConcurrentHashMap<String, List<Gadget>>();
	//TODO Support board actions for keys? This could allow the player to spam the board
	private ConcurrentMap<String, List<Action>> keyUpBoardTriggers = new ConcurrentHashMap<String, List<Action>>();
	private ConcurrentMap<String, List<Action>> keyDownBoardTriggers = new ConcurrentHashMap<String, List<Action>>();
	
	// Listeners
	public final KeyAdapter keyListener = new KeyAdapter() {
		@Override public void keyReleased(KeyEvent e) {
			onKey(KeyNames.keyName.get(e.getKeyCode()), keyUpTriggers, keyUpBoardTriggers);
		}
		@Override public void keyPressed(KeyEvent e) {
			onKey(KeyNames.keyName.get(e.getKeyCode()), keyDownTriggers, keyDownBoardTriggers);
		}
	};

	/**
	 * Locations for a neighboring board or a border wall
	 */
	private enum Neighbor {
		TOP, BOTTOM, LEFT, RIGHT
	}
	
	
	//TODO Factory methods why???

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
		// Set a sufficiently small foresight for the physics engine to avoid
		// mistimed flipper collisions.
		Physics.setForesight(0.0001);
		checkRep();
	}
	
	//Instance Methods
	
	/**
	 * Adds a gadget to the flingball board using the gadgets position. If the Gadget has a position
	 * not on the board, it is not added. 
	 * @param gadget gadget to be added
	 */
	public void addGadget(Gadget gadget) {
		//TODO Make Gadgets a set and assert addition to the set?
		// This depends on how equality works
		this.gadgets.add(gadget);
		this.setCoverage(gadget);
		checkRep();	
	}
	
	
	/**
	 * Adds a ball to the flingball board using the ball's position and velocity. If the ball has a position 
	 * not on the board, it is not added. If the ball has a velocity >= 200 L / s, the velocity is set to 200. 
	 * @param ball ball to be added. 
	 */
	public void addBall(Ball ball) {
		//TODO Start a new thread for each ball. this is done in play but what about balls added at a later point. 
		balls.add(ball);
		checkRep();
	}
	
	/**
	 * Adds a trigger and it's associated Action to the board. If the trigger is not the name of a 
	 * Gadget currently on the board, no action is added. 
	 * 
	 * @param trigger name of Gadget which serves as the trigger for the action
	 * @param action action to be taken when the trigger is triggered. 
	 */
	private void addAction(String trigger, Action action) {
		Gadget triggerGadget = getGadget(trigger);
		if (!boardTriggers.containsKey(triggerGadget)) {
			boardTriggers.put(triggerGadget, new ArrayList<Action>());
		}
		boardTriggers.get(triggerGadget).add(action);
	}
	
	/**
	 * Adds a trigger and it's associated action to the board. If the trigger is not the name of a 
	 * Gadget currently on the board, no action is added. 
	 * 
	 * @param trigger name of Gadget which serves as the trigger for the action
	 * @param action name of the Gadget whose action will be triggered 
	 */
	private void addAction(String trigger, String action) {
		Gadget triggerGadget = getGadget(trigger);
		Gadget actionGadget = getGadget(trigger);
		actionGadget.setTrigger(trigger);
		if (!triggers.containsKey(triggerGadget)) {
			triggers.put(triggerGadget, new ArrayList<Gadget>());
		}
		triggers.get(triggerGadget).add(getGadget(action));
	}
	
	/**
	 * Add an action associated with a key press or release
	 * @param key - key that is pressed or released
	 * @param action - name of a Gadget who's action is taken
	 * @param up - true if the action should be taken when the key is released. false if it should be taken when the key is pressed
	 */
	private void addKeyAction(String keyName, String action, boolean up) {
		Gadget actionGadget = getGadget(action);
		actionGadget.setTrigger(keyName);
		
		if (up) {
			if (!keyUpTriggers.containsKey(keyName)) {
				keyUpTriggers.put(keyName, new ArrayList<Gadget>());
			}
			keyUpTriggers.get(keyName).add(actionGadget);
		} else {
			if (!keyDownTriggers.containsKey(keyName)) {
				keyDownTriggers.put(keyName, new ArrayList<Gadget>());
			}
			keyDownTriggers.get(keyName).add(actionGadget);
			
		}
	}
	
	/**
	 * Add an action associated with a key press or release
	 * @param key - key that is pressed or released
	 * @param action - name of a Gadget who's action is taken
	 * @param up - true if the action should be taken when the key is released. false if it should be taken when the key is pressed
	 */
	private void addKeyAction(String keyName, Action action, boolean up) {
		
		if (up) {
			if (!keyUpBoardTriggers.containsKey(keyName)) {
				keyUpBoardTriggers.put(keyName, new ArrayList<Action>());
			}
			keyUpBoardTriggers.get(keyName).add(action);
		} else {
			if (!keyDownBoardTriggers.containsKey(keyName)) {
				keyDownBoardTriggers.put(keyName, new ArrayList<Action>());
			}
			keyDownBoardTriggers.get(keyName).add(action);
		}
	}
	
	public enum ActionType{
		BOARD, GADGET, KEYUP, KEYDOWN, KEYBOARDUP, KEYBOARDDOWN
	}
	
	/**
	 * If the provided string is a member of Action then the appropriate Action type is returned. 
	 * If not, then Action.NONE is returned. 
	 * @param action
	 * @return
	 */
	public static Action readAction(String action) {
		Action actionToTake;
		switch (action) {	
		case"FIRE_ALL": {
			actionToTake = Action.FIRE_ALL;
			break;
		}
		case"ADD_BALL": {
			actionToTake = Action.ADD_BALL;
			break;
		}
		case"ADD_SQUARE": {
			actionToTake = Action.ADD_SQUARE;
			break;
		}
		case"ADD_CIRCLE": {
			actionToTake = Action.ADD_CIRCLE;
			break;
		}
		case"ADD_TRIANGLE": {
			actionToTake = Action.ADD_TRIANGLE;
			break;
		}
		case"ADD_ABSORBER": {
			actionToTake = Action.ADD_ABSORBER;
			break;
		}
		case"REVERSE_BALLS": {
			actionToTake = Action.REVERSE_BALLS;
			break;
		}
		case"REMOVE_BALL": {
			actionToTake = Action.REMOVE_BALL;
			break;
		}
		case"REMOVE_GADGET": {
			actionToTake = Action.REMOVE_GADGET;
			break;
		}
		case"SPLIT": {
			actionToTake = Action.SPLIT;
			break;
		}
		default:{
			actionToTake = Action.NONE;
			break;
		}
		}
		return actionToTake;
	}
	
	
	/**
	 * 
	 * @param trigger
	 * @param action
	 * @param type
	 */
	public void addAction(String trigger, String action, ActionType type) {
		// TODO Action gets read twice. Can fix with two separate methods in Board or by changing method signature
		// to include Action boardAction and String actionGadget. This is kind of shitty and confusing to others though
		Action boardAction = readAction(action);
		switch (type) {
		case BOARD:
		{
			this.addAction(trigger, boardAction);
			break;
		}
		case GADGET:
		{
			this.addAction(trigger, action);
		}
		case KEYDOWN:
		{
			this.addKeyAction(trigger, action, false);
			break;
		}
		case KEYUP:
		{
			this.addKeyAction(trigger, action, true);
			break;
		}
		case KEYBOARDDOWN:
		{
			this.addKeyAction(trigger, boardAction, false);
			break;
		}
		case KEYBOARDUP:
		{
			this.addKeyAction(trigger, boardAction, true);
			break;
		}
		
		default:
			throw new RuntimeException("Should never get here. Invlalid ActionType: " + type);
		}
	}
	
	
	
	/**
	 * Connects two boards at the given Wall. The connection is symmetric. That is that if 
	 * Board A is connected to Board B a the TOP then Board B is automatically connected to Board A 
	 * at the BOTTOM
	 * 
	 * @param neighbor board being connected to this board
	 * @param border location on this board where the other board is connected
	 */
	public void addNeightbor(Board neighbor, Wall border) {
		//TODO
	}
	
	public List<Ball> getBalls() {
		List<Ball> result = new ArrayList<Ball>(this.balls);
		return result;
	}
	
	public List<Gadget> getGadgets() {
		List<Gadget> result = new ArrayList<Gadget>(gadgets);
		return result;
	}

	/**
	 * Sets the board into action for the given amount of time. All balls are moved taking into account the effects
	 * of gravity and friction. All actions which are triggered during this time are taken. 
	 * @param time length of time the board is played. 
	 */
	public void play(final double time) {
		
		//TODO Add an event queue so that actions that could not be performed are performed at the earliest possible moment
			for (Ball ball : balls) {
			//	new Thread(() ->  {
					// TODO Since we're using threads here play will continue.
					// need to change how BoardAnimation plays the game since play 
					// no longer needs to be called repeatedly. Also need to account
					// for newly added balls. 
	//				while (true) {
	//					try {
						moveOneBall(ball, time);
//							Thread.sleep(5L);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
					}
				//}, ball.name()).start();
//			}
//			for (int i = 0; i < this.balls.size(); i++) {
//					if (!balls.get(i).isTrapped()) {
//						moveOneBall(balls.get(i), time);
//					}
//				}
			checkRep();
	}

	/**
	 * Moves one ball on the board for the given amount of time accounting for the effects of friction
	 * and gravity. Any actions that are triggered during this time are taken. 
	 * @param ball
	 * @param time
	 */
	private void moveOneBall(Ball ball, final double time) {
		final Gadget NO_COLLISION = new Wall("NO_COLLISION", 0, 0, 0, 0);
		double collisionTime = Double.POSITIVE_INFINITY;
		Gadget nextGadget = NO_COLLISION;
		
		// Find the gadget with which the ball will collide next
		for (Gadget gadget : this.gadgets) {
			if (gadget.collisionTime(ball) < collisionTime) {
				//TODO This calculation does not account for gravity
				collisionTime = gadget.collisionTime(ball);
				nextGadget = gadget;
			}
		}
		
		// If the ball will not collide with the gadgets check the outer walls of the board. 
		if (nextGadget == NO_COLLISION) {
			for (Gadget wall : this.walls) {
				if (wall.collisionTime(ball) < collisionTime) {
					collisionTime = wall.collisionTime(ball);
					nextGadget = wall;
				}
			}
		}
		
		// TODO Ball Ball collisions should be calculated
		// If a ball will collide during the play time perform the collision. 
		if (collisionTime <= time && nextGadget != NO_COLLISION) {
			// Move ball to collision point
		// TODO Could this result in an infinite loop due to small math error?s - Yes
		//	while (collisionTime > 0) {
				ball.move(collisionTime, this.gravity, this.friction1, this.friction2);
//				collisionTime = nextGadget.collisionTime(ball);
//			}
			checkRep();
			nextGadget.reflectBall(ball);
			
			// Perform any actions triggered by the collision
			if (triggers.containsKey(nextGadget)) {
				for (Gadget gadget : triggers.get(nextGadget)) {
					//TODO - Triangle rotation needs to be delayed as rotation can cover the ball 
					// and invalidate the rep. Can use a new thread to do this maybe
					gadget.takeAction();
				}
			}
			if (boardTriggers.containsKey(nextGadget)) {
				for (Action action : boardTriggers.get(nextGadget)) {
					// Come back to this. It should be possible to do this without ball
				//	this.takeAction(action, ball);
				}
			}
			
			// Move ball during the rest of time after collision has occurred. 
			if (ball.getVelocity().length() > 0.0 && collisionTime > 0) {
				// TODO What about simultaneous collisions
				moveOneBall(ball, time - collisionTime);
			}
		} else {
			ball.move(time, this.gravity, this.friction1, this.friction2);
		}
	}

	/**
	 * Takes an action on the board. 
	 * @param action action to be taken. 
	 */
	private void takeAction(Action action) {
		//TODO
	}
	
	/**
	 * Adds a portal to the flingball board using the gadgets position. If the Gadget has a position
	 * not on the board, it is not added. 
	 * @param portal
	 * @param name
	 * @param board
	 */
	void addPortal(Portal portal, String name, String board) {
		portals.put(portal, Arrays.asList(name, board));
		this.addGadget(portal);
	}
	
	/**
	 * Connects all portals on the board
	 */
	void connectPortals() {
		for (Portal portal : portals.keySet()) {
			try {
				Board otherBoard = this.getBoard(portals.get(portal).get(1));
				Portal target = otherBoard.getPortal(portals.get(portal).get(0));
				portal.connect(target);
			} catch (NoSuchElementException e) {
				// Do nothing
			}	
		}
	}
	
	/**
	 * 
	 * @param name name of the gadget to be found
	 * @return Gadget with name name
	 * @throws RuntimeException if the Gadget is nor found. 
	 */
	private Gadget getGadget(String name) {
		for (Gadget g : gadgets) {
			if (name.equals(g.name())) {
				return g;
			}
		}
		throw new NoSuchElementException(name + "gadget not found");
	}
	
	/**
	 * 
	 * @param name name of the gadget to be found
	 * @return Gadget with name name
	 * @throws RuntimeException if the Gadget is nor found. 
	 */
	private Portal getPortal(String name) {
		for (Portal p : portals.keySet()) {
			if (name.equals(p.name())) {
				return p;
			}
		}
		throw new NoSuchElementException(name + "portal not found");
	}
	
	private Board getBoard(String name) {
		for (Board b : neighbors.keySet()) {
			if (name.equals(b.NAME)) {
				return b;
			}
		}
		throw new NoSuchElementException(name + "board not found");
	}
	
	
	
	
	
//	//TODO Remove replaced by onKey
//	void onKeyUp(String key) {
//		for (String k : keyUpTriggers.keySet()) {
//			for (Gadget g : keyUpTriggers.get(k)) {
//				if (k.equals(key)) {
//					g.takeAction();
//				}
//			}
//		}
//		for (String k : keyUpBoardTriggers.keySet()) {
//			for (Action a : keyUpBoardTriggers.get(k)) {
//				if (k.equals(key)) {
//					this.takeAction(a);
//				}
//			}
//		}
//	}
//	
//	//TODO Remove replaced by onKey
//	void onKeyDown(String key) {
//		for (String k : keyDownTriggers.keySet()) {
//			for (Gadget g : keyUpTriggers.get(k)) {
//				if (k.equals(key)) {
//					g.takeAction();
//				}
//			}
//		}
//		for (String k : keyDownBoardTriggers.keySet()) {
//			for (Action a : keyUpBoardTriggers.get(k)) {
//				if (k.equals(key)) {
//					this.takeAction(a);
//				}
//			}
//		}
//	}
	
	
	/**
	 * Triggers the actions associated with key in keyTriggers and keyBoardTriggers
	 * @param key that is pressed or released
	 * @param keyTriggers map of all trigger to gadget mappings on the board
	 * @param keyBoardTriggers map of all trigger to board Action mappings on the board. 
	 */
	private void onKey(String key, Map<String, List<Gadget>> keyTriggers, 
			Map<String, List<Action>> keyBoardTriggers) {
		for (String k : keyTriggers.keySet()) {
			for (Gadget g : keyTriggers.get(k)) {
				if (k.equals(key)) {
					g.takeAction();
				}
			}
		}
		for (String k : keyBoardTriggers.keySet()) {
			for (Action a : keyBoardTriggers.get(k)) {
				if (k.equals(key)) {
					this.takeAction(a);
				}
			}
		}
	}
	
	
	private void setCoverage(Gadget gadget) {
		// TODO This is a good place to practice the visitor method since absorbers cover a different area
		final int height = gadget.height();
		final int width = gadget.width();
		final int x = (int) gadget.position().x();
		final int y = (int) gadget.position().y();
		
		for (int i = x; i < x + width; i++) {
			for (int j = y; j < y + height; j++) {
				this.gadgetCoverage[i][j] = 1;
			}
		}
		
		if (gadget instanceof Absorber) {
			this.gadgetCoverage[x + width - 1][y + height -1] = 1;
		}
	}
	
	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, this.WIDTH * L, this.HEIGHT * L);
		
		final ImageObserver NO_OBSERVER_NEEDED = null;
		
		graphics.setColor(Color.BLUE);
		for (Ball ball : balls) {
			final Vect anchor = ball.getAnchor().times(L);
			
			graphics.drawImage(ball.generate(L), (int) anchor.x(), (int) anchor.y(), NO_OBSERVER_NEEDED);
					
		}
		
		for (Gadget gadget : gadgets) {
			final int xAnchor = (int) gadget.position().x()*L;
			final int yAnchor = (int) gadget.position().y()*L;
			
			graphics.drawImage(gadget.generate(L), xAnchor, yAnchor, NO_OBSERVER_NEEDED);
			
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
