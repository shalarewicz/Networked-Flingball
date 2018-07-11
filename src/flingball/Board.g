@skip whitespace {
	
	BOARD ::= boardName '\n'* ((comment '\n'*) | (command '\n'*))* ;
	boardName ::='board' 'name' '='NAME (GRAVITY)? (FRICTION1)? (FRICTION2)? '\n';
	comment ::= '#' [\-A-Za-z0-9\.,_'!=]* '\n';
	command ::= BALL | BUMPER | ABSORBER | FLIPPER | PORTAL | KEYEVENT | ACTION;
	
	BALL ::= 'ball' 'name' '=' NAME 'x' '='FLOAT 'y' '='FLOAT 'xVelocity' '=' FLOAT 'yVelocity' '=' FLOAT '\n';
	ABSORBER ::= 'absorber' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER 'width' '=' INTEGER 'height' '=' INTEGER '\n';
	
	ACTION ::= 'fire' 'trigger' '=' NAME 'action' '=' (NAME | ACTIONTOTAKE) '\n';
	ACTIONTOTAKE ::= 'FIRE' | 'ROTATE'|'SPIN' | 'FIRE_ALL' | 'ADD_BALL' | 'ADD_SQUARE' | 
					 'ADD_CIRCLE' | 'ADD_TRIANGLE' | 'ADD_ABSORBER' | 'REVERSE_BALLS';
	
	BUMPER ::= triangleBumper | circleBumper | squareBumper;
	squareBumper ::= 'squareBumper name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER '\n';
	circleBumper ::= 'circleBumper name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER '\n';
	triangleBumper ::= 'triangleBumper name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('orientation' '=' ORIENTATION)? '\n';
	
	FLIPPER ::= LEFTFLIPPER | RIGHTFLIPPER;
	LEFTFLIPPER ::= 'leftFlipper' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('orientation' '=' ORIENTATION)? '\n';
	RIGHTFLIPPER ::= 'rightFlipper' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('orientation' '=' ORIENTATION)? '\n';
	
	PORTAL ::= 'portal' 'name' '=' NAME 'x' '=' INTEGER 'y' '=' INTEGER ('otherBoard' '=' NAME)? 'otherPortal' '=' NAME '\n';
	
	KEYEVENT ::= KEYDOWN | KEYUP;
	KEYDOWN ::= 'keydown' 'key' '=' KEY 'action' '=' NAME '\n';
	KEYUP ::= 'keyup' 'key' '=' KEY 'action' '=' NAME '\n';
	
	GRAVITY ::= 'gravity' '='  FLOAT;
	FRICTION1 ::= 'friction1' '='  FLOAT;
	FRICTION2 ::= 'friction2' '='  FLOAT;
	ORIENTATION ::= '0' | '90' | '180' | '270';
	
}

NAME ::= [A-Za-z_][A-Za-z_0-9]*;
INTEGER ::= [0-9]+;
FLOAT ::= '-'?([0-9]+'.'[0-9]*|'.'?[0-9]+);

KEY ::= [a-z] | [0-9] | 'shift' | 'ctrl' | 'alt' | 'meta' | 'space' | 'left' | 'right' | 'up' | 'down' | 'minus' | 'equals' | 'backspace'
        | 'openbracket' | 'closebracket' | 'backslash' | 'semicolon' | 'quote' | 'enter' | 'comma' | 'period' | 'slash';
whitespace ::= [' '\t\r]+;
