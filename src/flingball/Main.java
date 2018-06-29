package flingball;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import edu.mit.eecs.parserlib.UnableToParseException;

public class Main {
	public static void main(final String[] args) throws IOException{
//		String file = args[0];
//		String file = "boards/simple_board.fb";
		String file = "boards/portals.fb";
		try {
			Path filePath = Paths.get(file);
			Stream<String> fileIn = Files.lines(filePath);
			StringBuilder boardFile = new StringBuilder();
			fileIn.forEach(s -> boardFile.append(s+"\n"));
			fileIn.close();
			System.out.println("Input: \n" + boardFile);
			final Board board = BoardParser.parse(boardFile.toString());
			System.out.println("The constructed board is " + board);
			new BoardAnimation(board);
		} catch (IOException e) {
			System.out.println(file + " not found");
		} catch (UnableToParseException e) {
			System.out.println("Unable to parse " + file);
			e.printStackTrace();
		}
			
	}
}
