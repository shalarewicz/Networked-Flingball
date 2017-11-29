package flingball;

import static org.junit.Assert.*;
import org.junit.Test;

import edu.mit.eecs.parserlib.Parser;

public class ParserLibVersionTest {
    
    
    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }
    
    @Test
    public void testParserLibVersion() {
        assertTrue("parserlib.jar needs to be version 3.0.x", Parser.VERSION.startsWith("3.0"));
    }
    
}
