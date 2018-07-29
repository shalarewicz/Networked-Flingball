package flingball;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.ImageObserver;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import flingball.gadgets.Gadget;
import physics.Vect;

/**
 * Begins play and animates a flingball board with specified value for L. By default the board will 
 * animate with a frame rate of 5 miliseconds. The value of L represents the number of 
 * pixels for one board unit. Board play begins when the new <code>BoardAnimation</code> object is created. 
 * @author Stephan Halarewicz
 */
public class BoardAnimation {
	
	public static final int DEFAULT_L = 40; 
	
	private static final long FRAME_RATE = 5; 
	private final Board board;
	private final int L;
	
	/*
	 * AF(board) ::= Displays and animates a flingball board at FRAME_RATE
	 * Rep Invariant ::= true
	 * 
	 * Safety from rep exposure
	 *	 Only the final static field FRAME_RATE is ever returned.  
	 *
	 */

	private void checkRep() {
		assert true;
	}
	
	/**
	 * Return the current frame rate of animation. 
	 * @return the frame rate at which the flingball board is animated. 
	 */
	public static long getFrameRate() {
		return FRAME_RATE;
	}

	/**
	 * Displays and begins play for the provided flingball board represented by <code>board</code> 
	 * with the specified value of L. The displayed board will be <code>board.WIDTH() * L</code> wide and 
	 * <code>board.HEIGHT</code> pixels tall.
	 * 
	 * @param board The board which will be displayed
	 * @param L The number of pixels that each unit L represents. 
	 */
    public BoardAnimation(Board board, int L) {
    	this.board = board;
    	this.L = L;
        EventQueue.invokeLater(new Runnable() {

			@Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace(); 
                }

                JFrame frame = new JFrame("Flingball");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new Animation());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.addKeyListener(board.keyListener);
            }
        });
        checkRep();
    }

    /**
     * The Animation class draws and re-draws the flingball board specified in BoardAnimation
     * @author Stephan Halarewicz
     */
    private class Animation extends JPanel {

		private static final long serialVersionUID = 1L;

		/**
		 * Starts play on the flingball board and begins the animation process. 
		 */
        private Animation() {
            Timer timer = new Timer();
            board.play((double) FRAME_RATE / 1000);
            TimerTask play = new TimerTask() {
                @Override
                public void run() {
                    repaint();
                }
            };
            timer.schedule(play, 0, FRAME_RATE);
        }


        @Override
        public Dimension getPreferredSize() {
            return new Dimension(board.HEIGHT * L, board.WIDTH * L);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2d = (Graphics2D) graphics.create();
            graphics.setColor(Color.BLACK);
    		graphics.fillRect(0, 0, board.HEIGHT * L, board.WIDTH * L);
    		
    		final ImageObserver NO_OBSERVER_NEEDED = null;
    		
    		for (Gadget gadget : board.getGadgets()) {
    			final int xAnchor = (int) gadget.position().x()*L;
    			final int yAnchor = (int) gadget.position().y()*L;
    			
    			g2d.drawImage(gadget.generate(L), xAnchor, yAnchor, NO_OBSERVER_NEEDED);
    			
    		}
    		
    		graphics.setColor(Color.BLUE);
    		for (Ball ball : board.getBalls()) {
    			final Vect anchor = ball.getAnchor().times(L);
    			
    			
    			g2d.drawImage(ball.generate(L), (int) anchor.x(), (int) anchor.y(), NO_OBSERVER_NEEDED);
    					
    		}
    		
    		
            g2d.dispose();
        }

	    }

	}
