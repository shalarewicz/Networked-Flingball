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
	
	final Set<ConnectionListener> connectionListeners =  ConcurrentHashMap.newKeySet();
	
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
				
				// Get the name of the board
				final String nameRequest = "NAME?";
				out.println(nameRequest);
				String name;
				
				// Wait for the response
				while (true) {
					String input = in.readLine();
					String[] tokens = input.split(" ");
					
					// If the response is not properly formatted re-send the request
					if (!tokens[0].equals("NAME")) {
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
									// Otherwise add the client to the server
									this.neighbors.put(name, new ConcurrentHashMap<Border, String>());
									this.boards.put(name, ConcurrentHashMap.newKeySet());
									this.outputStreams.put(name, out);
								}
							} catch (IOException e) {
								System.err.println("Client connection interupted. " + name + " will be removed from play");
								// If the connection is interrupted remove the client from the board. 
								this.removeClient(name);
								in.close();
								out.close();
							}
						}
						break;
					}
				}
				
				// Listen to command line input for h and v join commands. 
				// This allows users to configure connected boards if they have access to the server. 
				new Thread(() ->  {
					try {
						BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
						for (String command = stdIn.readLine(); command != null; command = stdIn.readLine()) {
							
							String join = command.split(" ")[0];
							
							if (join.equals("v") || join.equals("h")) {
								
								try {
									this.handleRequest(command, "");
								} catch (NoSuchElementException nse) {
									System.err.println("Board(s) not found");
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
							// TODO Can client sent a quit request?
							// ioe.printStackTrace(); // but do not stop serving
						}
						finally {
							this.removeClient(name);
							in.close();
							out.close();
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
		// prevent other boards from accessing clientData during removal
		synchronized (this.boards) {
			synchronized (this.neighbors) {
				synchronized (this.portals) {
					synchronized (this.outputStreams) {
						synchronized (this.connectionListeners) {
							// TODO Can I just say synchronized (this)
				
							this.boards.remove(id);
							
							// Revert walls of any board connected to this board
							for (Border border : this.neighbors.get(id).keySet()) {
								this.outputStreams.get(id).println("DISJOIN " + border);
								this.outputStreams.get(this.neighbors.get(id).get(border)).println("DISJOIN " +  border.complement());;
							}
							
							this.neighbors.remove(id);
							
							// Send disconnect Requests to all portals where the target was on this board. 
							for (String source : this.portals.keySet()) {
								if (source.contains(id + "/") || this.portals.get(source).contains(id + "/")) {
									this.portals.remove(source);
									this.outputStreams.get(id).println("DISCONNECT " + source.split("/")[1]);
								}
							}
							
							this.outputStreams.remove(id);
							
							// Remove this boards listeners
							for (ConnectionListener l : this.connectionListeners) {
								if (l.listener().equals(id)) {
									this.connectionListeners.remove(l);
								}
							}
						}
					}
				}
			}
		}
	}
	
	 /**
     * Handle a single client connection.
     * Returns when the client connection is interrupted. 
     * 
     * @param socket socket connected to client
     * @param clientID ID of the client being handled
     * @param in input stream for the socket. This stream is not closed by the method
     * @param out output stream for the socket. This stream is not closed by the method
     * @throws IOException if the connection encounters an error or closes unexpectedly
     */
    private void handleConnection(Socket socket, String clientID, BufferedReader in, PrintWriter out) throws IOException{
    	for (String input = in.readLine(); input != null; input = in.readLine()) {
        	try {
        		handleRequest(input, String.valueOf(clientID));
        		
        		this.sendBoardUpdates();
        		
        	} catch (UnsupportedOperationException uoe) {
        		out.println("Invalid request: " + uoe.getMessage());
        	} catch (NoSuchElementException nse) {
        		out.println(nse.getMessage());
        	}
        }
	}
    
    
    /**
     * Handle a single client request and sends a response back to the client and other 
     * clients if necessary.
     * 
     * @param input message from client
     * @param id id of player making the request
     * @throws NoSuchElementException if the request involves an unconnected board. 
     */
    private void handleRequest(String input, String id) throws NoSuchElementException {
    	 String[] tokens = input.split(" ");
    	 
    	 if (tokens[0].equals("h")) {
    		 String left = tokens[1];
    		 String right = tokens[2];
    		 
    		 if (this.boards.keySet().contains(left) && this.boards.keySet().contains(right)) {
    			 
    			 synchronized (this.neighbors ) { // h nameLeft nameRight
    				 
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
	    			 
	    			 // Document the connection in the rep
	    			 this.neighbors.get(left).put(Border.RIGHT, right);
	    			 this.neighbors.get(right).put(Border.LEFT, left);
    			 }
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + left + " or " + right);
    		 }
    		 
    	 }
    	 else if (tokens[0].equals("v")) { // v nameTop nameBottom
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
	    			 
	    			 // Document the connection in the rep
	    			 this.neighbors.get(top).put(Border.BOTTOM, bottom);
	    			 this.neighbors.get(bottom).put(Border.TOP, top);
    			 }
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + top + " or " + bottom);
    		 }
    		 
    	 }
    	 else if (tokens[0].equals("addBall")) { // addBall NEIGHBOR NAME X Y VX VY
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
    	 else if (tokens[0].equals("connect")) { // connect sourcePortal targetPortal targetBoard
    		 String source = tokens[1];
    		 String target = tokens[2];
    		 String targetBoard = tokens[3];
    		 
    		 synchronized (this.boards) {
    			 // Document the connection in the rep
    			 this.portals.put(id + "/" + source, targetBoard + "/" + target);
    			 
    			 
    			 // If the targetBoard is connected to the server then connect the portal
    			 if (this.boards.containsKey(targetBoard)) {
    				 outputStreams.get(id).println("CONNECT " + source + " " + targetBoard);
    			 } else {
    				 // Otherwise create a listener to wait for the target board to connect
	    			 this.connectionListeners.add(new ConnectionListener() {
	    				 @Override
	    				 public void onConnection() {
	    					 outputStreams.get(id).println("CONNECT " + source + " " + targetBoard);
	    				 }
	    				 
	    				 @Override
	    				 public String listeningFor() {
	    					 return targetBoard;
	    				 }
	    				 
	    				 @Override
	    				 public String listener() {
	    					 return id;
	    				 }
	    			 });
    			 }
    		 }
    		 
    	 } 
    	 
    	 else if (tokens[0].equals("teleport")) {// teleport sourcePortal ballName xVelocity yVelocity
    		 
    		 String source = tokens[1];
    		 
    		 String[] destination = this.portals.get(id + "/" + source).split("/");
    		 String target = destination[1];
    		 String targetBoard = destination[0];
    		 
    		 String ball = tokens[2];
			 String vx = tokens[3];
			 String vy = tokens[4];
			 
			 this.boards.get(targetBoard).add("TELEPORT " + target + " " + ball + " " + vx + " " + vy);
    	 }
    	 
    	 else if (tokens[0].equals("START")) { // Indicates that the Board is ready to start gameplay
    		 
    		 // let other boards know that this board is ready and portals can be connected. 
    		 this.notifyConnectionListeners(id);
    		 try {
    			 //TODO Remove this. Boards need time to connect their portas
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		 this.boards.get(id).add("READY");
    		 
    	 } 
    	 
    	 else {
    		 throw new UnsupportedOperationException(input);
    	 }
     	 
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
     * Notify boards that client name is ready to play. 
     * @param name
     */
    private void notifyConnectionListeners(String id) {
    	for (ConnectionListener listener : this.connectionListeners) {
    		if (id.equals(listener.listeningFor())) {
    			listener.onConnection();
    			// Listener is not removed to allow the client to disconnect and then
    			// reconnect while maintaining portal connections. 
    		}
    	}
    }
    private interface ConnectionListener {
    	
    	public void onConnection();
    	
    	public String listeningFor();
    	
    	public String listener();
    }
    
}
