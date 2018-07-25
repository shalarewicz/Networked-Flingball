package flingball;

public enum Orientation {
	/**
	 * Gadgets can be rotated in 90 degree intervals from their default orientation. Rotation is performed in the clockwise direction 
	 */
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
	 */
	public static Orientation parseOrientation(String s) {
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
		}
		return o;
	}
	
	
}


