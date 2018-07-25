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
	            }
	        });
	        checkRep();
	    }

	    public class TestPane extends JPanel {

			private static final long serialVersionUID = 1L;
			private Board board;

	        public TestPane(Board board) {
	        	this.board= board;
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
	            return new Dimension(this.board.HEIGHT * Board.L, this.board.WIDTH * Board.L);
	        }

	        @Override
	        protected void paintComponent(Graphics graphics) {
	            super.paintComponent(graphics);
	            Graphics2D g2d = (Graphics2D) graphics.create();
	            graphics.setColor(Color.BLACK);
	    		graphics.fillRect(0, 0, this.board.HEIGHT * Board.L, this.board.WIDTH * Board.L);
	    		
	    		final ImageObserver NO_OBSERVER_NEEDED = null;
	    		
	    		graphics.setColor(Color.BLUE);
	    		for (Ball ball : this.board.getBalls()) {
	    			final Vect anchor = ball.getAnchor().times(Board.L);
	    			
	    			
	    			g2d.drawImage(ball.generate(Board.L), (int) anchor.x(), (int) anchor.y(), NO_OBSERVER_NEEDED);
	    					
	    		}
	    		
	    		for (Gadget gadget : this.board.getGadgets()) {
	    			final int xAnchor = (int) gadget.position().x()*Board.L;
	    			final int yAnchor = (int) gadget.position().y()*Board.L;
	    			
	    			g2d.drawImage(gadget.generate(Board.L), xAnchor, yAnchor, NO_OBSERVER_NEEDED);
	    			
	    		}
	    		
	            g2d.dispose();
	        }

	    }

	}
