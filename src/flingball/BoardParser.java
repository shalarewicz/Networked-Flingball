package flingball;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import edu.mit.eecs.parserlib.ParseTree;
import edu.mit.eecs.parserlib.Parser;
import edu.mit.eecs.parserlib.UnableToParseException;
import physics.Vect;
import flingball.gadgets.*;
// import edu.mit.eecs.parserlib.Visualizer;

public class BoardParser {


	public static void main(final String[] args) throws IOException{
		String test = "boards/sampleBoard.fb";
		try {
			Path filePath = Paths.get(test);
			Stream<String> fileIn = Files.lines(filePath);
			StringBuilder boardFile = new StringBuilder();
			fileIn.forEach(s -> boardFile.append(s+"\n"));
			fileIn.close();
			System.out.println("Input: \n" + boardFile);
			final Board board = BoardParser.parse(boardFile.toString());
			System.out.println("The constructed board is " + board);
			new BoardAnimation(board);
		} catch (IOException e) {
			System.out.println(test + " not found");
		} catch (UnableToParseException e) {
			System.out.println("Unable to parse " + test);
		}
			
	}
	
	private enum BoardGrammar {
		BOARD, COMMENT, COMMAND, BALL, BUMPER, SQUAREBUMPER, CIRCLEBUMPER, 
		TRIANGLEBUMPER, INTEGER, FLOAT, NAME, WHITESPACE, ORIENTATION, FRICTION2, FRICTION1, 
		GRAVITY, BOARDNAME, ABSORBER, ACTION, ACTIONTOTAKE, FLIPPER, LEFTFLIPPER, RIGHTFLIPPER, 
		PORTAL, KEYEVENT, KEYUP, KEYDOWN, KEY
	}

	private static Parser<BoardGrammar> parser = makeParser();
	
	private static Parser<BoardGrammar> makeParser() {
		try {
			final File grammarFile = new File("src/flingball/Board.g");
			return Parser.compile(grammarFile, BoardGrammar.BOARD);
		} catch (IOException e) {
			throw new RuntimeException("can't read the grammar file", e);
        } catch (UnableToParseException e) {
            throw new RuntimeException("the grammar has a syntax error", e);
        }
    }
		
		
	
	public static Board parse(final String input) throws UnableToParseException{
		final ParseTree<BoardGrammar> parseTree = parser.parse(input);
		
		// display the parse tree in a web browser, for debugging only
		//Visualizer.showInBrowser(parseTree);

        // make an AST from the parse tree
		final Board board = makeAbstractSyntaxTree(parseTree);
		board.connectPortals();
		return board;
	}
	
	private static Board makeAbstractSyntaxTree(final ParseTree<BoardGrammar> parseTree) {
		//TODO Restructure this methods by creating a second makeAST method where one parameter is a board. 
		// This will get rid of any confusion when it coes to nesting. 
		switch (parseTree.name()) {
        case BOARD: //  BOARD ::= boardName '\n'* ((comment '\n'*) | (command '\n'*))* ;
            {
            	List<ParseTree<BoardGrammar>> children = parseTree.children();
            	
            	Board board = makeAbstractSyntaxTree(children.get(0));
            	for (int i = 1; i < children.size(); i++) {
            		
            		final ParseTree<BoardGrammar> child = children.get(i);
            		List<ParseTree<BoardGrammar>> grandChildren = child.children();
            		
            		switch (children.get(i).name()) {
            		case COMMENT: // comment ::= '#' [A-Za-z0-9\.'=]* '\n';
            			continue;
            		
            		case COMMAND: // command ::= BALL | BUMPER | ABSORBER | ACTION;
            		{
            			
            			ParseTree<BoardGrammar> grandChild = grandChildren.get(0);
            			List<ParseTree<BoardGrammar>> greatGrandChildren = grandChild.children();
            			switch (grandChild.name()) {
            			
            			case BALL: //BALL ::= 'ball' 'name' '=' NAME 'x' '='FLOAT 'y' '='FLOAT 'xVelocity' '=' FLOAT 'yVelocity' '=' FLOAT '\n';
            			{
            				
            				final String name = greatGrandChildren.get(0).text();
            				final double cx = Double.parseDouble(greatGrandChildren.get(1).text());
            				final double cy = Double.parseDouble(greatGrandChildren.get(2).text());
            				final double vx = Double.parseDouble(greatGrandChildren.get(3).text());
            				final double vy = Double.parseDouble(greatGrandChildren.get(4).text());
            				final Ball ball = new Ball(name, new Vect(cx, cy), new Vect(vx, vy));
            				board.addBall(ball);
            				continue;
            			}
            			
            			case BUMPER: //BUMPER ::= triangleBumper | circleBumper | squareBumper;
            			{
            				
            				ParseTree<BoardGrammar> greatGrandChild = greatGrandChildren.get(0);
            				List<ParseTree<BoardGrammar>> bumperProperties = greatGrandChild.children();
            				switch (greatGrandChild.name()) {
            				case SQUAREBUMPER: //squareBumper ::= 'squareBumper name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER '\n';
            				{
            					String name = bumperProperties.get(0).text();
            					final int x = Integer.parseInt(bumperProperties.get(1).text());
            					final int y = Integer.parseInt(bumperProperties.get(2).text());
            					
            					Gadget bumper = new SquareBumper(name, x, y);
            					board.addGadget(bumper);
            					continue;
            				}
            				case CIRCLEBUMPER: // circleBumper ::= 'circleBumper name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER '\n';
            				{
            					String name = bumperProperties.get(0).text();
            					final int x = Integer.parseInt(bumperProperties.get(1).text());
            					final int y = Integer.parseInt(bumperProperties.get(2).text());
            					
            					Gadget bumper = new CircleBumper(name, x, y);
            					board.addGadget(bumper);
            					continue;
            				}
            				case TRIANGLEBUMPER: // triangleBumper ::= 'triangleBumper name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('orientation' '=' ORIENTATION)? '\n';
            				{
            					Gadget bumper;
            					String name = bumperProperties.get(0).text();
            					final int x = Integer.parseInt(bumperProperties.get(1).text());
            					final int y = Integer.parseInt(bumperProperties.get(2).text());
            					if (bumperProperties.size() > 2) {
            						Orientation o = Orientation.ZERO;
            						switch (bumperProperties.get(bumperProperties.size()-1).text()) {
            						case "0": o = Orientation.ZERO; break;
            						case "90": o = Orientation.NINETY; break;
            						case "180": o = Orientation.ONE_EIGHTY; break;
            						case "270": o = Orientation.TWO_SEVENTY; break;
            						}
            						bumper = new TriangleBumper(name, x, y, o);
            					}
            					else {            	        	
            						bumper = new TriangleBumper(name, x, y, Orientation.ZERO);
            					}
            					board.addGadget(bumper);
            					continue;
            				}
            				default:
            					System.out.println("Could not match BUMPER " + greatGrandChild.name());
            					throw new RuntimeException("Should never get here");
            				}
            			}
            			case ABSORBER: //ABSORBER ::= 'absorber' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER 'width' '=' INTEGER 'height' '=' INTEGER '\n';
            			{
            				String name = greatGrandChildren.get(0).text();
            				int x = Integer.parseInt(greatGrandChildren.get(1).text());
            				int y = Integer.parseInt(greatGrandChildren.get(2).text());
            				int width = Integer.parseInt(greatGrandChildren.get(3).text());
            				int height = Integer.parseInt(greatGrandChildren.get(4).text());
            				board.addGadget(new Absorber(name, x, y, width, height));
            				continue;
            			}
            			case FLIPPER: //FLIPPER ::= LEFTFLIPPER | RIGHTFLIPPER;
            			{
            				ParseTree<BoardGrammar> greatGrandChild = greatGrandChildren.get(0);
            				List<ParseTree<BoardGrammar>> flipperProperties = greatGrandChild.children();
            				
            				switch (greatGrandChild.name()) {
            				case LEFTFLIPPER: // LEFTFLIPPER ::= 'leftFlipper' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('orientation' '=' ORIENTATION)? '\n';
            				{
            					String name = flipperProperties.get(0).text();
            					int x = Integer.parseInt(flipperProperties.get(1).text());
            					int y = Integer.parseInt(flipperProperties.get(2).text());
            					Orientation o = Orientation.ZERO;
            					if (flipperProperties.size() > 3) {
            						switch (flipperProperties.get(flipperProperties.size()-1).text()) {
            						//	TODO Add a helper method to read these. This code repeats. 
	            						case "0": o = Orientation.ZERO; break;
	            						case "90": o = Orientation.NINETY; break;
	            						case "180": o = Orientation.ONE_EIGHTY; break;
	            						case "270": o = Orientation.TWO_SEVENTY; break;
            						}
            					}
            					board.addGadget(new LeftFlipper(name, x, y, o));
            					
            				}
            				case RIGHTFLIPPER: // RIGHTFLIPPER ::= 'rightFlipper' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('orientation' '=' ORIENTATION)? '\n';
            				{
            					String name = flipperProperties.get(0).text();
            					int x = Integer.parseInt(flipperProperties.get(1).text());
            					int y = Integer.parseInt(flipperProperties.get(2).text());
            					Orientation o = Orientation.ZERO;
            					if (flipperProperties.size() > 3) {
            						switch (flipperProperties.get(flipperProperties.size()-1).text()) {
            						//	TODO Add a helper method to read these. This code repeats. 
	            						case "0": o = Orientation.ZERO; break;
	            						case "90": o = Orientation.NINETY; break;
	            						case "180": o = Orientation.ONE_EIGHTY; break;
	            						case "270": o = Orientation.TWO_SEVENTY; break;
            						}
            					}
            					board.addGadget(new RightFlipper(name, x, y, o));
            				}
            				default:
            				{
            					System.out.println("Could not match FLIPPER " + greatGrandChild.name());
            					throw new RuntimeException("Should never get here");
            				}
            				}
            			}
            			
            			case PORTAL: // PORTAL ::= 'portal' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('otherBoard' '=' NAME)? 'otherPortal' '=' NAME '\n';
            			{
            				ParseTree<BoardGrammar> greatGrandChild = greatGrandChildren.get(0);
            				List<ParseTree<BoardGrammar>> portalProperties = greatGrandChild.children();
            				
            				String name = portalProperties.get(0).text();
        					int x = Integer.parseInt(portalProperties.get(1).text());
        					int y = Integer.parseInt(portalProperties.get(2).text());
        					String otherBoard = "";
        					String target = portalProperties.get(portalProperties.size() - 1).text();
        					if (portalProperties.size() > 4) {
        						otherBoard = portalProperties.get(3).text();
        					}
        					Portal portal = new Portal(name, x, y);
        					// TODO is this necessary? addPortal can just do it. 
        					// board.addGadget(portal);
        					board.addPortal(portal, target, otherBoard);
            			}
            			
            			case KEYEVENT: //KEYEVENT ::= KEYDOWN | KEYUP;
            			{
            				ParseTree<BoardGrammar> greatGrandChild = greatGrandChildren.get(0);
            				List<ParseTree<BoardGrammar>> keyProperties = greatGrandChild.children();
            				String key = keyProperties.get(0).text();
            				String action = keyProperties.get(1).text();
            				
            				switch (greatGrandChild.name()) {
            				case KEYUP:	// KEYUP ::= 'keyup' 'key' '=' KEY 'action' '=' NAME '\n';
            				{
            					board.addKeyAction(key, action, true);
            					
            				}
            				case KEYDOWN:  // KEYDOWN ::= 'keydown' 'key' '=' KEY 'action' '=' NAME '\n';
            				{
            					board.addKeyAction(key, action, false);
            				}
            				default:
            				{
            					System.out.println("Could not match KEY " + greatGrandChild.name());
            					throw new RuntimeException("Should never get here");
            				}
            				}
            			}
            			case ACTION: // ACTION ::= 'fire' 'trigger' '=' NAME 'action' '=' (NAME | ACTIONTOTAKE) '\n';
            			{
            				String trigger = greatGrandChildren.get(0).text();
            				String action = greatGrandChildren.get(1).text();
            				Action actionToTake;
        					switch (action) {
        					case "FIRE_ALL":{
        						actionToTake = Action.FIRE_ALL;
        						break;
        					}
        					case "ADD_BALL":{
        						actionToTake = Action.ADD_BALL;
        						break;
        					}
        					case "ADD_SQUARE":{
        						actionToTake = Action.ADD_SQUARE;
        						break;
        					}
        					case "ADD_CIRCLE":{
        						actionToTake = Action.ADD_CIRCLE;
        						break;
        					}
        					case "ADD_TRIANGLE":{
        						actionToTake = Action.ADD_TRIANGLE;
        						break;
        					}
        					case "ADD_ABSORBER":{
        						actionToTake = Action.ADD_ABSORBER;
        						break;
        					}
        					case "REVERSE_BALLS":{
        						actionToTake = Action.REVERSE_BALLS;
        						break;
        					}
        					default:{
        						// action is a gadget
        						board.addAction(trigger, action);;
        						continue;
        					}
        					}
        					// action is a Board Action
        					
            				board.addAction(trigger, actionToTake);
            				continue;
            			}
            			default:
            				System.out.println("Could not match COMMAND: " + grandChild.name());
        					throw new RuntimeException("Should never get here");
        				}
            			}
            		default:
        				System.out.println("Could not match " + child.name());
    					throw new RuntimeException("Should never get here");
            		}// End Switch
            	} // End for loop
            	
            	return board;
            }
        case BOARDNAME: // boardName ::='board name''='NAME (GRAVITY)? (FRICTION1)? (FRICTION2)? '\n';
        {
        	List<ParseTree<BoardGrammar>> children = parseTree.children();
        	double gravity = Board.DEFAULT_GRAVITY;
        	double friction1 = Board.DEFAULT_FRICTION_1;
        	double friction2 = Board.DEFAULT_FRICTION_2;
        	String name = children.get(0).text();
        	if (children.size() > 1) {
	        	for (int i = 1; i < children.size(); i++) {
	        		ParseTree<BoardGrammar> child = children.get(i);
	        		switch (child.name()) {
	        		case GRAVITY:{
	        			gravity = Double.parseDouble(child.children().get(0).text());
	        			continue;
	        		}
	        		case FRICTION1:{
	        			friction1 = Double.parseDouble(child.children().get(0).text());
	        			continue;
	        		}
	        		case FRICTION2:{
	        			friction2 = Double.parseDouble(child.children().get(0).text());
	        			continue;
	        		}
	        		default:
	        			makeAbstractSyntaxTree(child);
	    		}
	        	}
        	}
        	return new Board(name, gravity, friction1, friction2);
        }
                	
		default:
			System.out.println("Could not match " + parseTree.name());
			throw new RuntimeException("Should never get here");
		}
	}



}
