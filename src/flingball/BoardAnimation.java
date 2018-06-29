package flingball;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import flingball.gadgets.Gadget;
import physics.Vect;

public class BoardAnimation {
	
	public final static long FRAME_RATE = 5; 
	/*
	 * TODO: AF()
	 * TODO: Rep Invariant
	 * TODO: Safety from rep exposure
	 */

	private void checkRep() {
		assert true;
	}
	

	    public BoardAnimation(Board board) {
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
	                frame.add(new TestPane(board));
	                frame.pack();
	                frame.setLocationRelativeTo(null);
	                frame.setVisible(true);
	                frame.addKeyListener(board.keyListener);
	                // TODO Remove if putting the listener in board works. 
//	                	new KeyAdapter() {
//							@Override public void keyReleased(KeyEvent e) {
//								board.onKeyUp(KeyNames.keyName.get(e.getKeyCode()));
//							}
//							@Override public void keyPressed(KeyEvent e) {
//								board.onKeyDown(KeyNames.keyName.get(e.getKeyCode()));
//							}
//	                	});
	            }
	        });
	        checkRep();
	    }

	    public class TestPane extends JPanel {

			private static final long serialVersionUID = 1L;
			private Board board;
			private final int L;

	        public TestPane(Board board) {
	        	this.board= board;
	        	this.L = board.L;
	            Timer timer = new Timer();
	            TimerTask play = new TimerTask() {
	                @Override
	                public void run() {
	                    board.play((double) FRAME_RATE / 1000);
	                    repaint();
	                }
	            };
	            timer.schedule(play, 0, FRAME_RATE);
	        }


	        @Override
	        public Dimension getPreferredSize() {
	            return new Dimension(this.board.HEIGHT * this.L, this.board.WIDTH * this.L);
	        }

	        @Override
	        protected void paintComponent(Graphics graphics) {
	            super.paintComponent(graphics);
	            Graphics2D g2d = (Graphics2D) graphics.create();
	            graphics.setColor(Color.BLACK);
	    		graphics.fillRect(0, 0, this.board.HEIGHT * this.L, this.board.WIDTH * this.L);
	    		
	    		final ImageObserver NO_OBSERVER_NEEDED = null;
	    		
	    		graphics.setColor(Color.BLUE);
	    		for (Ball ball : this.board.getBalls()) {
	    			final Vect anchor = ball.getAnchor().times(this.L);
	    			
	    			
	    			g2d.drawImage(ball.generate(this.L), (int) anchor.x(), (int) anchor.y(), NO_OBSERVER_NEEDED);
	    					
	    		}
	    		
	    		for (Gadget gadget : this.board.getGadgets()) {
	    			final int xAnchor = (int) gadget.position().x()*this.L;
	    			final int yAnchor = (int) gadget.position().y()*this.L;
	    			
	    			g2d.drawImage(gadget.generate(this.L), xAnchor, yAnchor, NO_OBSERVER_NEEDED);
	    			
	    		}
	    		
	    		//TODO REMOVE GRID
	    		for (int i = 1; i <= 20; i++) {
	    			g2d.setColor(Color.GREEN);
	    			g2d.drawLine(0, i*this.L, this.L * this.L, i*this.L);
	    			g2d.drawLine(i*this.L, 0, i*this.L, this.L * this.L);
	    			
	    			
	    		}
	            g2d.dispose();
	        }

	    }

	}
