package flingball;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.commons.cli.*;

import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * TODO: put documentation for your class here
 * If no HOST is provided, then the client runs in single-machine play mode, running a board and allowing the user to play it without any network connection to any other board.
 * 
 * If FILE is not provided, then your Flingball client should run the default benchmark board as described in the phase 1 specification.
 */
public class Flingball {
    
    /**
     * Usage:
     * Flingball [--host HOST] [--port PORT] [FILE]
     * HOST is an optional hostname or IP address of the server to connect to. 
     * PORT is an optional integer in the range 0 to 65535 inclusive, specifying the port where the server is listening for incoming connections. The default port is 10987.
     * FILE is an optional argument specifying a file pathname of the Flingball board that this client should run. 
     */
    public static void main(String[] args) {
    	Options options = new Options();
    	
    	Option host = new Option("h", "host", true, "hostname or ip adddress of server"); 
    	Option port = new Option("p", "port", true, "port where server is listening");
    	
    	options.addOption(host);
    	options.addOption(port);
    	
    	CommandLineParser parser = new DefaultParser();
    	HelpFormatter formatter = new HelpFormatter();
    	CommandLine cmd;
    	
    	final int prt;
    	final String file;
    	
    	try {
    		// Check if a port # was provided. If not use the default port. 
    		cmd = parser.parse(options, args);
    		if (cmd.hasOption("port")) {
    			prt = Integer.parseInt(cmd.getOptionValue("port"));
    		} else {
    			prt = 10987;
    		}
    		
    		// Check if a board file was provided. If not use the default board
    		if (cmd.getArgList().size() > 0) {
    			file = cmd.getArgList().get(0);
    		} else {
    			file = "boards/default.fb";
    		}
    		
    		// Create the flingball board and start to play. 
    		try {
    			Board board = readFile(file);
    		
    			if (cmd.hasOption("host")) {
    				try {
    					connect(board, prt, cmd.getOptionValue("host"));
    				} catch (UnknownHostException uhe) {
    					System.err.println("Could not connect to host " + cmd.getOptionValue("host"));
    					System.err.println("Plaing in single player");
    				} catch (IOException ioe) {
    					System.err.println("Server Connection interupted");
    				} 
	    		}
    			
    			board.connectPortals();
    			new BoardAnimation(board);
    		
    		} catch (IOException e) {
    			System.out.println(file + " not found");
    		} catch (UnableToParseException e) {
    			System.out.println("Unable to parse " + file);
    			e.printStackTrace();
    		}
    		
    	} catch (ParseException e) {
    		System.out.println(e.getMessage());
    		formatter.printHelp("utility-name", options);

            System.exit(1);
    	}
			
	}
    
    private static Board readFile(String file) throws IOException, UnableToParseException{
			Path filePath = Paths.get(file);
			Stream<String> fileIn = Files.lines(filePath);
			StringBuilder boardFile = new StringBuilder();
			fileIn.forEach(s -> boardFile.append(s + "\n"));
			fileIn.close();
			System.out.println("Input: \n" + boardFile);
			final Board board = BoardParser.parse(boardFile.toString());
			return board;
    }
    
    
    private static void connect(Board board, final int prt, String hostAdress) throws UnknownHostException, IOException {
		Socket socket = new Socket(hostAdress, prt);
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		
		
		// Add a listener for sending requests to the server when the board changes. For example, 
		// if a ball moves to another board. 
		board.addRequestListener(new RequestListener() {
			@Override
			public void onRequest(String request) {
				out.println(request);
			}
		});
		
		// Listen to command line input for h and v join commands. 
		new Thread(() ->  {
			try {
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
				for (String command = stdIn.readLine(); command != null; command = stdIn.readLine()) {
					String join = command.split(" ")[0];
					if (join.equals("v") || join.equals("h")) {
						out.println(command);
					} else {
						System.err.println("'" + join + "' is not a valid command.");
					}
						
				}
			} catch (IOException e) {
				//Do not stop listening
				e.printStackTrace();
			} 
		}).start();
		
		// Listen for server responses and send them to the board for processing
		try {
			for (String response = in.readLine(); response != null; response = in.readLine()) {
				if (response.equals("NAME?")) {
					out.println("NAME " + board.NAME);
					
					// Connect portal to portals on other boards. 
					for (String portalConnection : board.connectPortals()) {
						out.println(portalConnection);
					}
					out.println("START");
				}
				else if (response.equals("READY")) {
					//Start the game
						new BoardAnimation(board);
					
				}
				else if (response.split(" ")[0].equals("ERROR:")) {
					System.out.println(response);
					System.exit(1);
				}
				else {
					board.handleResponse(response);
				}
			}
			
		} catch (IOException e) {
			// Do not stop listening
			e.printStackTrace();
		}  finally {
			socket.close();
		}
    }
    
}
