package flingball;

import java.util.NoSuchElementException;

/**
 * Possible positions of an outer wall on a flingball board. 
 * @author Stephan Halarewicz
 *
 */
enum Border {
	TOP, BOTTOM, LEFT, RIGHT;
	
	/**
	 * Returns the complement of this Border. 
	 * Complements are as follows
	 * TOP - BOTTOM
	 * LEFT - RIGHT
	 * @return the complement of this border
	 */
	Border complement() {
		switch (this) {
		case BOTTOM:
			return TOP;
		case LEFT:
			return RIGHT;
		case RIGHT:
			return LEFT;
		case TOP:
			return BOTTOM;
		default:
			throw new RuntimeException("Should never get here. Invalid border " + this.toString());
		}
	}
	
	/**
	 * Parses a string for a Border. Valid values include "TOP", "BOTTOM", "LEFT" and "RIGHT"
	 * @param s <code>String</code> to be read
	 * @return the <code>Border</code> which matches the string. 
	 * @throws NoSuchElementException if the string could not be parsed. 
	 */
	static Border fromString(String s) throws NoSuchElementException{
		switch (s) {
		case "TOP": {
			 return TOP;
		 }
		 case "BOTTOM": {
			return BOTTOM;
		 }
		 case "LEFT": {
			 return LEFT;
		 }
		case "RIGHT": {
			return RIGHT;
		}
		default:
			throw new NoSuchElementException("String does not match a border");
		}
	}
}
