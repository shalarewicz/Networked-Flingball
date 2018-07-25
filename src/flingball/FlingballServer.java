package flingball;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * FlingballServer allows multiple clients play a game of networked flingball. Connected clients have the ability to connect boards 
 * via command line inputs:
 * 		h NAME_left NAME_right 
 * 		v NAME_top NAME_bottom
 * Note that a board's left outer wall can only be connected to a board's right outer wall. 
 * 
 * Join commands can be configured directly on the server or be be sent by a client. 
 */
public class FlingballServer {
    
	private final ServerSocket serverSocket;
	// Map(Name, Server Responses)
	final ConcurrentMap<String, Set<String>> boards = new ConcurrentHashMap<String, Set<String>>();
	// Map(board name, Map(Connected border, connected board name))
	final ConcurrentMap<String, ConcurrentMap<Border, String>> neighbors = new ConcurrentHashMap<String, ConcurrentMap<Border, String>>();
	
	final ConcurrentMap<String, PrintWriter> outputStreams = new ConcurrentHashMap<String, PrintWriter>();
	
	final ConcurrentMap<String, String> portals = new ConcurrentHashMap<String, String>();
	
	private final static int DEFAULT_PORT = 10987;
	/*
	 * AF() ::= Server listening on a server socket.
	 * 			boards ::= clients currently connected and pending responses
	 * 			neighbors ::= map of current board connections
	 * 			portals ::= map of portals connect to portals on another board. 
	 * 			outputStreams :: map of boards to the socket ouput stream. 
	 * 
	 * Rep Invariant ::=
	 * 		Board connections must be symmetric
	 * 
	 * Safety from rep exposure ::=
	 * 		port() returns a primitive int type
	 * 		all other methods return void
	 * 
	 * Thread Safety Argument ::=
	 * 		Only concurrent maps are used to prevent multiple threads accessing simultaneous client information. 
	 * 		When a client is removed or added a lock is first obtained on boards until all client informaiton mappings have been updated. 
	 * 
	 */
	
	private void checkRep() {
		// Check for symmetrical board connections. 
		for (String board : this.neighbors.keySet()) {
			for (Border border : this.neighbors.get(board).keySet()) {
				String connectedBoard = this.neighbors.get(board).get(border);
				assert this.neighbors.containsKey(connectedBoard) : "Connected Board must also be connected" + connectedBoard;
				assert this.neighbors.get(connectedBoard).get(border.complement()).equals(board) : "Connection must be symmetric" + board + " connected to " + connectedBoard;
			}
			
		}
	}
	
	/**
	 * FlingballServer [--port PORT]
	 * PORT is an integer in the range 0 to 65535 inclusive, specifying the port where the server should listen for incoming connections.
	 * The default port is 10987.
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
	 * Create a FlingballServer listening on port for incoming connections
	 * 
	 * @param port Port # where the server will listen for incoming connections. 0 to 6535 inclusive
	 * @throws IOException if an I/O error occurs when opening the socket.
	 */
	public FlingballServer(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.checkRep();
	}
	
	 /**
	  * Obtain the port on which this server is listening for connections.
     * @return the port number
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
		System.err.println("Server will listen on " + this.port());
		
		while (true) {
			// Blocks until a request is accepted
			Socket s = this.serverSocket.accept();
			if (s.isConnected()) {
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				
				final String nameRequest = "NAME?";
				// Get the name of the board
				out.println(nameRequest);
				String name;
				
				// Wait for the response
				while (true) {
					String input = in.readLine();
					String[] tokens = input.split(" ");
					if (!tokens[0].equals("NAME")) {
						// If the response is not properly formatted re-send the request
						out.println(nameRequest);
					} else {
						// Otherwise if the board name is already in use send an error message to the client 
						// and terminate the connection. 
						name = tokens[1];
						synchronized (this.boards) {
							try {
								if (this.boards.keySet().contains(name)) {
									out.println("ERROR: Duplicate Board Name. Connection Terminated");
									s.close();
								} else {
									this.neighbors.put(name, new ConcurrentHashMap<Border, String>());
									this.boards.put(name, ConcurrentHashMap.newKeySet());
									this.outputStreams.put(name, out);
								}
							} catch (IOException e) {
								e.printStackTrace();
								// If the connection is interrupted remove the client from the board. 
								this.removeClient(name);
							}
						}
						break;
					}
				}
				// Listen to command line input for h and v join commands. 
				new Thread(() ->  {
					try {
						BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
						for (String command = stdIn.readLine(); command != null; command = stdIn.readLine()) {
							String join = command.split(" ")[0];
							if (join.equals("v") || join.equals("h")) {
								
								try {
									this.handleRequest(command, "");
								} catch (NoSuchElementException nse) {
									System.out.println("Board(s) not found");
								}
								this.sendBoardUpdates();
							} else {
								System.err.println("'" + join + "' is not a valid command.");
							}
								
						}
					} catch (IOException e) {
						//Do not stop listening
						e.printStackTrace();
					} 
				}).start();
				
				
				// Handle the client
				new Thread(() -> {
					try {
						try {
							handleConnection(s, name, in, out);
						} catch (IOException ioe) {
							System.err.println("Connection Lost for " + name);
							ioe.printStackTrace(); // but do not stop serving
						}
						finally {
							this.removeClient(name);
							s.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}).start();
        		
			}
		}
		
	}
	
	/**
	 * Disconnect all boards which were connected to the board and remove the lost board from the server. 
	 * @param id id of client being removed.
	 */
	private void removeClient(String id) {
		// prevents other boards from accessing clientData
		synchronized (this.boards) {
			this.boards.remove(id);
			// Obtain a lock on neighbors so no other threads can send a ball or connect request while connections 
			// are broken. 
			synchronized (this.neighbors) {
				for (Border border : this.neighbors.get(id).keySet()) {
					this.outputStreams.get(id).println("DISJOIN " + border);
					this.outputStreams.get(this.neighbors.get(id).get(border)).println("DISJOIN " +  border.complement());;
				}
				this.neighbors.remove(id);
			}
			synchronized (this.portals) {
				for (String source : this.portals.keySet()) {
					if (source.contains(id + "/") || this.portals.get(source).contains(id + "/")) {
						this.portals.remove(source);
						this.outputStreams.get(id).println("DISCONNECT " + source.split("/")[1]);
					}
				}
			}
			this.outputStreams.remove(id);
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
    private void handleConnection(Socket socket, String clientID, BufferedReader in, PrintWriter out) throws IOException{
    	for (String input = in.readLine(); input != null; input = in.readLine()) {
        	try {
        		handleRequest(input, String.valueOf(clientID));
        		
        		// If the client quits remove the client from connected server play
        		if (input.equals("quit")) {
        			this.removeClient(clientID);
        			socket.close();
        		}
        		
        		this.sendBoardUpdates();
        		
        	} catch (UnsupportedOperationException uoe) {
        		out.println("Invalid request: " + uoe.getMessage());
        	} catch (NoSuchElementException nse) {
        		out.println(nse.getMessage());
        	}
        }
        out.close();
        in.close();
	}
    
    /**
     * Send any responses to all connected boards
     */
    private void sendBoardUpdates() {
    	// Sends updates to boards connected to the server (i.e. if two boards are joined or a ball is teleported.)
    	for (String board : this.outputStreams.keySet()) {
			PrintWriter boardOut = this.outputStreams.get(board);
			for (String response : this.boards.get(board)) {
				boardOut.println(response);
			}
			this.boards.get(board).clear();
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
    private void handleRequest(String input, String id) throws NoSuchElementException {
    	 String[] tokens = input.split(" ");
    	 
    	 if (tokens[0].equals("h")) {
    		 String left = tokens[1];
    		 String right = tokens[2];
    		 
    		 if (this.boards.keySet().contains(left) && this.boards.keySet().contains(right)) {
    			 
    			 synchronized (this.neighbors ) {
    				 
    				 // If the boards are not already connected remove the existing connection
    				 if (this.neighbors.containsKey(left) && this.neighbors.get(left).containsKey(Border.RIGHT) && !this.neighbors.get(left).get(Border.RIGHT).equals(right)) {
    					 this.boards.get(left).add("DISJOIN RIGHT");
    					 this.boards.get(right).add("DISJOIN LEFT");
    					 this.neighbors.get(left).remove(Border.RIGHT);  
    					 this.neighbors.get(right).remove(Border.LEFT);  
    				 }
    					 
    				 // Send join requests to the newly boards
	    			 this.boards.get(left).add("JOIN RIGHT");
	    			 this.boards.get(right).add("JOIN LEFT");
	    			 this.neighbors.get(left).put(Border.RIGHT, right);
	    			 this.neighbors.get(right).put(Border.LEFT, left);
    			 }
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + left + " or " + right);
    		 }
    		 
    	 }
    	 else if (tokens[0].equals("v")) {
    		 String top = tokens[1];
    		 String bottom = tokens[2];
    		 
    		 if (this.boards.keySet().contains(top) && this.boards.keySet().contains(bottom)) {
    			 synchronized (this.neighbors ) {
    				 // If the boards are not already connected remove the existing connection
    				 if (this.neighbors.containsKey(top) && this.neighbors.get(top).containsKey(Border.BOTTOM) && !this.neighbors.get(top).get(Border.BOTTOM).equals(bottom)) {
    					 this.boards.get(top).add("DISJOIN BOTTOM");
    					 this.boards.get(bottom).add("DISJOIN TOP");
    					 this.neighbors.get(top).remove(Border.BOTTOM);  
    					 this.neighbors.get(bottom).remove(Border.TOP);
    				 }	    	
    				 
	    			 // Send join requests to the newly boards
	    			 this.boards.get(top).add("JOIN BOTTOM");
	    			 this.boards.get(bottom).add("JOIN TOP");
	    			 this.neighbors.get(top).put(Border.BOTTOM, bottom);
	    			 this.neighbors.get(bottom).put(Border.TOP, top);
    			 }
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + top + " or " + bottom);
    		 }
    		 
    	 }
    	 else if (tokens[0].equals("addBall")) {
    		 // addBall NEIGHBOR NAME X Y VX VY
    		 String neighbor = tokens[1]; 
    		 synchronized (this.neighbors) {
	    		 final String name = this.neighbors.get(id).get(Border.fromString(neighbor));
	    		 
	    		 if (this.boards.keySet().contains(name)) {
	    			 String ball = tokens[2];
	    			 String x = tokens[3];
	    			 String y = tokens[4];
	    			 String vx = tokens[5];
	    			 String vy = tokens[6];
	    			 
	    			 // Send the addBall request to the connected board. 
	    			 this.boards.get(name).add("ADD " + ball + " " + x + " " + y + " " + vx + " " + vy);
	    		 } else {
	    			 throw new NoSuchElementException(neighbor + "connected Board not found. Ball lost.");
	    		 }
    		 }
    		 
    	 } 
    	 else if (tokens[0].equals("connect")) {
    		 // connect sourcePortal targetPortal targetBoard
    		 String source = tokens[1];
    		 String target = tokens[2];
    		 String targetBoard = tokens[3];
    		 synchronized (this.boards) {
    			 this.portals.put(id + "/" + source, targetBoard + "/" + target);
    			 this.boards.get(id).add("CONNECT " + source);
    		 }
    		 
    	 } 
    	 
    	 else if (tokens[0].equals("teleport")) {
    		 // teleport sourcePortal ballName xVelocity yVelocity
    		 String source = tokens[1];
    		 
    		 String[] destination = this.portals.get(id + "/" + source).split("/");
    		 String target = destination[1];
    		 String targetBoard = destination[0];
    		 
    		 String ball = tokens[2];
			 String vx = tokens[3];
			 String vy = tokens[4];
			 
			 this.boards.get(targetBoard).add("TELEPORT " + target + " " + ball + " " + vx + " " + vy);
    	 }
    	 
    	 else if (tokens[0].equals("START")) {
    		 // Indicates the server is ready to play with a board. This is used to allow for all portal connections to be established before play is started. 
    		 this.boards.get(id).add("READY");
    	 } 
    	 
    	 else {
    		 throw new UnsupportedOperationException(input);
    	 }
     	 
    }
    
}
