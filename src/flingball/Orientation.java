package flingball;

import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * A <code>Gadget</code> can be rotated in 90 degree intervals from their default orientation. There are four possible
 * <code>orientation</code> values: <code>ZERO, NINETY, ONE_EIGHTY</code> and <code>TWO_SEVENTY</code>
 * Rotation is performed in the clockwise direction.
 * @author Stephan Halarewicz
 */
public enum Orientation {
	ZERO,
	NINETY,
	ONE_EIGHTY,
	TWO_SEVENTY; 
	
	/**
	 * Parses the provided string for an Orientation. Leading and trailing whitespace is ignored. 
	 * 
	 * "0" = ZERO
	 * "90" = NINETY
	 * "180" = ONE_EIGHTY
	 * "270" =  TWO_SEVENTY
	 * 
	 * @param s string to be parsed
	 * @return the Orientation specified by s or Orientation.ZERO if no match is found. 
	 * @throws UnableToParseException if the string cannot be parsed. 
	 */
	public static Orientation parseOrientation(String s) throws UnableToParseException {
		Orientation o = Orientation.ZERO;
		switch (Integer.parseInt(s.trim())) {
			case 0: {
				o = Orientation.ZERO; 
					break;
				}
			case 90: {
					o = Orientation.NINETY; 
					break;
				}
			case 180: {
				o = Orientation.ONE_EIGHTY; 
				break;
			}
			case 270: {
				o = Orientation.TWO_SEVENTY; 
				break;
			}
			default:
				throw new UnableToParseException("Cannot parse: " + s);
		}
		return o;
	}
	
	
}


