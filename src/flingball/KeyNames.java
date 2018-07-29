package flingball;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An immutable class used to easily interpret a <code>KeyEvent</code>.
 * @author Stephan Halarewicz
 */
public class KeyNames {

    public final static Map<Integer,String> keyName;
    static {
        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(KeyEvent.VK_A, "a");
        map.put(KeyEvent.VK_B, "b");
        map.put(KeyEvent.VK_C, "c");
        map.put(KeyEvent.VK_D, "d");
        map.put(KeyEvent.VK_E, "e");
        map.put(KeyEvent.VK_F, "f");
        map.put(KeyEvent.VK_G, "g");
        map.put(KeyEvent.VK_H, "h");
        map.put(KeyEvent.VK_I, "i");
        map.put(KeyEvent.VK_J, "j");
        map.put(KeyEvent.VK_K, "k");
        map.put(KeyEvent.VK_L, "l");
        map.put(KeyEvent.VK_M, "m");
        map.put(KeyEvent.VK_N, "n");
        map.put(KeyEvent.VK_O, "o");
        map.put(KeyEvent.VK_P, "p");
        map.put(KeyEvent.VK_Q, "q");
        map.put(KeyEvent.VK_R, "r");
        map.put(KeyEvent.VK_S, "s");
        map.put(KeyEvent.VK_T, "t");
        map.put(KeyEvent.VK_U, "u");
        map.put(KeyEvent.VK_V, "v");
        map.put(KeyEvent.VK_W, "w");
        map.put(KeyEvent.VK_X, "x");
        map.put(KeyEvent.VK_Y, "y");
        map.put(KeyEvent.VK_Z, "z");
        map.put(KeyEvent.VK_0, "0");
        map.put(KeyEvent.VK_1, "1");
        map.put(KeyEvent.VK_2, "2");
        map.put(KeyEvent.VK_3, "3");
        map.put(KeyEvent.VK_4, "4");
        map.put(KeyEvent.VK_5, "5");
        map.put(KeyEvent.VK_6, "6");
        map.put(KeyEvent.VK_7, "7");
        map.put(KeyEvent.VK_8, "8");
        map.put(KeyEvent.VK_9, "9");
        map.put(KeyEvent.VK_SHIFT, "shift");
        map.put(KeyEvent.VK_CONTROL, "ctrl");
        map.put(KeyEvent.VK_ALT, "alt");
        map.put(KeyEvent.VK_META, "meta");
        map.put(KeyEvent.VK_SPACE, "space");
        map.put(KeyEvent.VK_LEFT, "left");
        map.put(KeyEvent.VK_RIGHT, "right");
        map.put(KeyEvent.VK_UP, "up");
        map.put(KeyEvent.VK_DOWN, "down");
        map.put(KeyEvent.VK_MINUS, "minus");
        map.put(KeyEvent.VK_EQUALS, "equals");
        map.put(KeyEvent.VK_BACK_SPACE, "backspace");
        map.put(KeyEvent.VK_OPEN_BRACKET, "openbracket");
        map.put(KeyEvent.VK_CLOSE_BRACKET, "closebracket");
        map.put(KeyEvent.VK_BACK_SLASH, "backslash");
        map.put(KeyEvent.VK_SEMICOLON, "semicolon");
        map.put(KeyEvent.VK_QUOTE, "quote");
        map.put(KeyEvent.VK_ENTER, "enter");
        map.put(KeyEvent.VK_COMMA, "comma");
        map.put(KeyEvent.VK_PERIOD, "period");
        map.put(KeyEvent.VK_SLASH, "slash");
        keyName = Collections.unmodifiableMap(map);
    }

}
