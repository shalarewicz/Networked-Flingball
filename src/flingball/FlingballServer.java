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

import flingball.Board.Border;

/**
 * TODO: put documentation for your class here
 */
public class FlingballServer {
    
	private final ServerSocket serverSocket;
	// Map(Name, Server Responses)
	final ConcurrentMap<String, Set<String>> boards = new ConcurrentHashMap<String, Set<String>>();
	// Map(board name, Map(Connected border, connected board name))
	final ConcurrentMap<String, ConcurrentMap<Board.Border, String>> neighbors = new ConcurrentHashMap<String, ConcurrentMap<Board.Border, String>>();
	
	final ConcurrentMap<String, PrintWriter> outputStreams = new ConcurrentHashMap<String, PrintWriter>();
	
	private final static int DEFAULT_PORT = 10987;
	/*
	 * Server needs to be able to 
	 * Connect boards
	 * Connect Portals
	 * Pass balls to other boards
	 * Terminate a connection
	 */
	
	private void checkRep() {
		//TODO check for symmetrical board connections
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
		System.out.println("server listening  on " + this.port());
		while (true) {
			// Blocks until a request is accepted
			Socket s = this.serverSocket.accept();
			System.out.println("accepted a connection");
			if (s.isClosed()) {
				System.out.print("Socket is closed");
			}
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
					System.out.println("received command " + input);
					if (!tokens[0].equals("NAME")) {
						// Invalid response
						out.println("Response not recognized");
						out.println("NAME?");
					} else {
						// Accept the response
						// TODO what if the name already exists?
						System.out.println("handling command " + input);
						System.out.println("Socket open " + s.isClosed());
						name = tokens[1];
						if (this.boards.keySet().contains(name)) {
							System.out.println("Board name already exists closing connection");
							out.println("ERROR: Duplicate Board Name. Connection Terminated");
							//s.close();
						} else {
							this.neighbors.put(name, new ConcurrentHashMap<Board.Border, String>());
							this.boards.put(name, ConcurrentHashMap.newKeySet());
							this.outputStreams.putIfAbsent(name, out);
						}
						System.out.println("Closing i/o streams socket is closed? " + s.isClosed());
//						out.close();
//						in.close();
						System.out.println("Closed i/o streams socket is closed? " + s.isClosed());
						break;
					}
				}
				new Thread(() -> {
					System.out.println("socket closed in new thread? " + s.isClosed());
					try {
						// handle the client
						try {
							handleConnection(s, name);
						} catch (IOException ioe) {
							ioe.printStackTrace(); // but do not stop serving
						} finally {
							s.close();
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
    	System.out.println("handling connection");
		System.out.println(socket.isClosed());
    	 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         try {
	    	for (String input = in.readLine(); input != null; input = in.readLine()) {
	    		System.out.println("Received a command " + input);
	        	try {
	        		String output = handleRequest(input, String.valueOf(clientID));
	        		if (output.equals("QUIT")) {
	        			System.out.println("Closing the socket");
	        			this.boards.remove(clientID);
	        			socket.close();
	        			break;
	        		}
	        		
	        		// TODO Could instead use a BlockingQueue to send updates whenever they come in.
	        		// This removes the need for watch requests but also does not guarantee that the server 
	        		// sends a response. 
	        		// Sends updates to boards connected to the server if the above request warrants a change to 
	        		// a different board. (i.e. if two boards are joined or a ball is teleported. 
	        		System.out.print("Checking for board updates");
//	        		for (String board : this.boards.keySet()) {
//	        			System.out.print("Sending responses");
//	        			for (String response : this.boards.get(board)) {
//	        				out.println(this.boards.get(response));
//	        			}
//	        			this.boards.get(board).clear();
//	        			// TODO Will this cause an iterator bug? - Probably
//	        		}
	        		for (String board : this.outputStreams.keySet()) {
	        			PrintWriter boardOut = this.outputStreams.get(board);
	        			for (String response : this.boards.get(board)) {
	        				boardOut.println(response);
	        			}
	        			this.boards.get(board).clear();
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
    		 String left = tokens[1];
    		 String right = tokens[2];
    		 
    		 if (this.boards.keySet().contains(left) && this.boards.keySet().contains(right)) {
    			 
    			 this.boards.get(left).add("JOIN RIGHT");
    			 this.boards.get(right).add("JOIN LEFT");
    			 
    			 // Existing connections are overwritten
    			 // TODO Since existing connection is overwritten. need to send DISJOIN requests. 
    			 // DISJOIN NAME BORDER
    			 this.neighbors.get(left).put(Border.RIGHT, right);
    			 this.neighbors.get(right).put(Border.LEFT, left);
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + left + " or " + right);
    		 }
    		 
    		 // TODO How to send response to player two who did not send join request?
    		 // Use a listener
    	 }
    	 else if (tokens[0].equals("vjoin")) {
    		 String top = tokens[1];
    		 String bottom = tokens[2];
    		 
    		 if (this.boards.keySet().contains(top) && this.boards.keySet().contains(bottom)) {
    			 this.boards.get(top).add("JOIN BOTTOM");
    			 this.boards.get(bottom).add("JOIN TOP");
    			 
    			// Existing connections are overwritten
    			 // TODO Since existing connection is overwritten. need to send DISJOIN requests. 
    			 // DISJOIN NAME BORDER
    			 this.neighbors.get(top).put(Border.BOTTOM, bottom);
    			 this.neighbors.get(bottom).put(Border.TOP, top);
    		 } else {
    			 throw new NoSuchElementException("Board not found: " + top + " or " + bottom);
    		 }
    		 
    	 }
    	 else if (tokens[0].equals("addBall")) {
    		 // addBall NEIGHBOR NAME X Y VX VY
    		 String neighbor = tokens[1]; 
    		 final String name;
    		 switch (neighbor) {
    		 case "TOP": {
    			 name = this.neighbors.get(id).get(Border.TOP);
    			 break;
    		 }
    		 case "BOTTOM": {
    			 name = this.neighbors.get(id).get(Border.BOTTOM);
    			 break;
    		 }
    		 case "LEFT": {
    			 name = this.neighbors.get(id).get(Border.LEFT);
    			 break;
    		 }
    		 case "RIGHT": {
    			 name = this.neighbors.get(id).get(Border.RIGHT);
    			 break;
    		 }
    		 default: {
    			 //TODO Better way to do this?
    			 throw new UnsupportedOperationException("Ball cannot be moved through border " + neighbor);
    		 }
    		 }
    		 if (this.boards.keySet().contains(name)) {
    			 String ball = tokens[2];
    			 String x = tokens[3];
    			 String y = tokens[4];
    			 String vx = tokens[5];
    			 String vy = tokens[6];
    			 // Sent the addBall request to the connected board. 
    			 this.boards.get(name).add("ADD " + ball + " " + x + " " + y + " " + vx + " " + vy);
    		 } else {
    			 //TODO do nothing? Ball is lost
    		 }
    		 
    	 } 
    	 else if (tokens[0].equals("connect")) {
    		 throw new UnsupportedOperationException("connect not implemented");
    		 
    	 } else if (tokens[0].equals("watch")) {
    		 //TODO Listen for updates to your board then return unless a new request is received
    		 // This might not be necessary
    		 throw new UnsupportedOperationException("watch not implemented");
    	 } else {
    		 System.out.println("operation not supported " + input);
    		 throw new UnsupportedOperationException(input);
    	 }
    	 
    	 return response;
    	 
     	 
    }

    
}
