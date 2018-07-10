package flingball;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
    	final String hst;
    	final String file;
    	try {
    		cmd = parser.parse(options, args);
    		System.out.println(cmd);
    		if (cmd.hasOption("port")) {
    			prt = Integer.parseInt(cmd.getOptionValue("port"));
    		} else {
    			prt = 10987;
    		}
    		
    		if (cmd.getArgList().size() > 0) {
    			file = cmd.getArgList().get(0);
    		} else {
    			file = "boards/default.fb";
    		}
    		
    		try {
    			Path filePath = Paths.get(file);
    			Stream<String> fileIn = Files.lines(filePath);
    			StringBuilder boardFile = new StringBuilder();
    			fileIn.forEach(s -> boardFile.append(s+"\n"));
    			fileIn.close();
    			System.out.println("Input: \n" + boardFile);
    			final Board board = BoardParser.parse(boardFile.toString());
    			new BoardAnimation(board);
    		
    		if (cmd.hasOption("host")) {
    			hst = cmd.getOptionValue("host");
    			Socket socket = new Socket(hst, prt);
    			System.out.println("opened a socket. is it closed?" + socket.isClosed());
    			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    			
    			board.addRequestListener(new RequestListener() {
    				
    				@Override
    				public void onRequest(String request) {
    					System.out.println("Sending request to server: " + request);
    					out.println(request);
    				}
    			});
    			
    			
    			new Thread(() ->  {
    				try {
    					for (String command = stdIn.readLine(); command != null; command = stdIn.readLine()) {
    						System.out.println("sending a command");
    						out.println(command);
    					}
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				} 
    			}).start();
    			
    			try {
    				for (String response = in.readLine(); response != null; response = in.readLine()) {
    					System.out.println("received a server request " + response );
    					if (response.equals("NAME?")) {
    						System.out.println("asking for your name");
    						out.println("NAME " + board.NAME);
    					} else if (response.split(" ")[0].equals("ERROR:")) {
    						System.out.println(response);
    						System.exit(1);
    					}
    					else {
    						board.handleResponse(response);
    					}
    				}
    				
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} 
    			System.out.println("in flingball, socket is closed " + socket.isClosed());
    			
    		} 
    		
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
    
    
    
}
