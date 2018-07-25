package flingball;

/**
 * 
 * @author Stephan Halarewicz
 *
 *A list of actions that can be set on a flingball board
 *
 *<ol>
 *	<lli>FIRE_ALL - Fires all trapped balls.</li>
 *	<li>ADD_BALL - Adds a ball with random position and velocity to the board</li>
 *	<li>REVERSE_BALLS - Reverses the direction of all balls on the board except for the triggering ball.</li>
 * 	<li>SPLIT - Splits the triggering ball into 3.</li>
 *	<li>ADD_SQUARE - Adds a square bumper with random position to the board</li>
 *	<li>ADD_CIRCLE - Adds a circle bumper with random position to the board</li>
 *	<li>ADD_TRIANGLE - Adds a triangle bumper with random position to the board</li>
 *	<li>ADD_ABSORBER - Adds an absorber with random position to the board. Maximum width is two and length is 5</li>
 *	<li>REMOVE_GADGET - Removes a random Gadget from the board</li>
 *	<li>REMOVE_BALL - Removes a random ball from the board</li>
 *</ol>
 *
 */
public enum Action {
	FIRE_ALL, ADD_BALL, ADD_SQUARE, ADD_CIRCLE, ADD_TRIANGLE, ADD_ABSORBER, REVERSE_BALLS,
	REMOVE_BALL, REMOVE_GADGET, SPLIT, NONE;
	
	/**
	 * If the provided string is a member of Action then the appropriate Action type is returned. 
	 * If not, then Action.NONE is returned. 
	 * @param action
	 * @return
	 */
	public static Action fromString(String action) {
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
}


