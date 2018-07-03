package flingball;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TODO: put documentation for your class here
 */
public class FlingballServer {
    
	private final ServerSocket serverSocket;
	final ConcurrentMap<String, Set<String>> boards = new ConcurrentHashMap<String, Set<String>>();
	
	private final static int DEFAULT_PORT = 10987;
	/*
	 * Server needs to be able to 
	 * Connect boards
	 * Connect Portals
	 * Pass balls to other boards
	 * Terminate a connection
	 */
	
	private void checkRep() {
		//TODO check for board connection symmetry?
	}
	
	/**
	 * TODO: describe your main function's command line arguments here
	 */
	public static void main(String[] args) throws IOException {
		
		final int port;
		
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalild PORT", nfe);
			}
		} else {
			port = DEFAULT_PORT;
		}
		
		new FlingballServer(port).serve();
		
	}
	/**
	 * 
	 * @param port
	 * @throws IOException if an I/O error occurs when opening the socket.
	 */
	public FlingballServer(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.checkRep();
	}
	
	 /**
     * @return the port on which this server is listening for connections
     */
	public int port() {
		return this.serverSocket.getLocalPort();
	}
	
	/**
     * Run the server, listening for and handling client connections.
     * Never returns normally.
     * 
     * @throws IOException if an error occurs waiting for a connection
     */
	public void serve() throws IOException {
		while (true) {
			// Blocks until a request is accepted
			Socket s = this.serverSocket.accept();
			
			if (s.isConnected()) {
				
				// Get the name of the board
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				out.println("NAME?");
				String name;
				// Wait for the response
				while (true) {
					BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String input = in.readLine();
					String[] tokens = input.split(" ");
					if (!tokens[0].equals("NAME")) {
						// Invalid response
						out.println("Response not recognized");
						out.println("NAME?");
					} else {
						// Accept the response
						// TODO what if the name already exists?
						name = tokens[1];
						this.boards.put(name, ConcurrentHashMap.newKeySet());
						out.close();
						in.close();
						break;
					}
				}
        		
        		new Thread(() -> {
	        		Socket socket = s;
					try {
		        		// handle the client
		        		try {
		        			handleConnection(socket, name);
		        		} catch (IOException ioe) {
		        			ioe.printStackTrace(); // but do not stop serving
		        		} finally {
							socket.close();
		        		}
					} catch (IOException e) {
						System.out.println("Server Error" + e);
						e.printStackTrace();
					}
	        	}).start();
			}
		}
		
	}
	
	 /**
     * Handle a single client connection.
     * Returns when the client disconnects.
     * 
     * @param socket socket connected to client
     * @param clientID ID of the client being handled
     * @throws IOException if the connection encounters an error or closes unexpectedly
     */
    private void handleConnection(Socket socket, String clientID) throws IOException{
    	 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         try {
	    	for (String input = in.readLine(); input != null; input = in.readLine()) {
	        	try {
	        		String output = handleRequest(input, String.valueOf(clientID));
	        		if (output.equals("QUIT")) {
	        			this.boards.remove(clientID);
	        			socket.close();
	        			break;
	        		}
	        		
	        		// TODO Could instead use a BlockingQueue to send updates whenever they come in.
	        		// This removes the need for watch requests but also does not guarantee that the server 
	        		// sends a response. 
	        		for (String s : this.boards.keySet()) {
	        			out.println(s);
	        			// TODO Will this cause an iterator bug?
	        			this.boards.remove(s);
	        		}
	        		
	        		// Send requests to other boards to join/add ball etc.
	        		
	        	} catch (UnsupportedOperationException uoe) {
	        		out.println(uoe.getMessage() + ": Command not recognized");
	        	} catch (NoSuchElementException nse) {
	        		out.println(nse.getMessage());
	        	}
	        }
	     } finally {
	        out.close();
	        in.close();
	     }
	}
    
    /**
     * Handle a single client request and return the server response.
     * 
     * @param input message from client
     * @param id id of player making the request
     * @return output message to client
     * @throws NoSuchElementException if the request involves an unconnected board. 
     */
    private String handleRequest(String input, String id) throws NoSuchElementException {
    	 String[] tokens = input.split(" "); // TODO ignore extra whitespace
    	 
    	 String response = "SUCCESS";
    	 
    	 if (tokens[0].equals("quit")) {
    		 for (String board : this.boards.keySet()) {
    			 this.boards.get(board).add("QUIT " + id);
    		 }
    		 response = "QUIT";
    	 } 
    	 else if (tokens[0].equals("hjoin")) {
    		 // TODO Validate that both left and right are current boards then
    		 // send to JOIN responses
    		 String left = tokens[1];
    		 String right = tokens[2];
    		 if (this.boards.keySet().contains(left) && this.boards.keySet().contains(right)) {
    			 this.boards.get(left).add("JOIN " + right + " right");
    			 this.boards.get(right).add("JOIN " + left + " left");
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + left + " or " + right);
    		 }
    		 
    		 // TODO How to send response to player two who did not send join request?
    		 // Use a listener
    	 }
    	 else if (tokens[0].equals("vjoin")) {
    		// TODO Validate that both left and right are current boards then
    		 // send to JOIN responses
    		 String top = tokens[1];
    		 String bottom = tokens[2];
    		 if (this.boards.keySet().contains(top) && this.boards.keySet().contains(bottom)) {
    			 this.boards.get(top).add("JOIN " + bottom + " bottom");
    			 this.boards.get(bottom).add("JOIN " + top + " top");
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + top + " or " + bottom);
    		 }
    		 
    	 }
    	 else if (tokens[0].equals("addBall")) {
    		 String name = tokens[1];
    		 if (this.boards.keySet().contains(name)) {
    			 double x = Double.parseDouble(tokens[1]);
    			 double y = Double.parseDouble(tokens[2]);
    			 double vx = Double.parseDouble(tokens[3]);
    			 double vy = Double.parseDouble(tokens[4]);
    			 this.boards.get(name).add("ADD " + x + " " + y + " " + vx + " " + vy);
    		 }
    		 
    	 } 
    	 else if (tokens[0].equals("connect")) {
    		 throw new UnsupportedOperationException("connect not implemented");
    		 
    	 } else if (tokens[0].equals("watch")) {
    		 //TODO Listen for updates to your board then return unless a new request is received
    	 } else {
    		 throw new UnsupportedOperationException(input);
    	 }
    	 
    	 return response;
    	 
     	 
    }

    
}
