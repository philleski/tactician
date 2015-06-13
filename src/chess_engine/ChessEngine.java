package chess_engine;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import chess_engine.Board;
import chess_engine.Brain;

public class ChessEngine {
	static Board board = new Board();
	static Brain brain = new Brain();
	
	public static void logClear() {
		try {
			new File("/Users/philip.leszczynski/chess.log").delete();
		}
		catch(Exception e) {
		}
	}
	
	public static void log(String line) {
		try {
			FileWriter writer = new FileWriter("/Users/philip.leszczynski/chess.log", true);
			writer.write(line + "\n");
			writer.close();
		}
		catch(IOException e) {
		}
	}
	
	public static void respond(String line) {
		log("> " + line);
		System.out.println(line);
	}
	
	public static void interpretUCICommand(String line) {
		log("< " + line);
		// Implements https://en.wikipedia.org/wiki/Universal_Chess_Interface
		if(line.equals("uci")) {
			// Command to start the engine
			respond("id name Phil");
			respond("id author Phil Leszczynski");
			respond("uciok");
		}
		else if(line.equals("isready")) {
			// Giving the engine time to initialize.
			respond("readyok");
		}
		else if(line.startsWith("position fen ")) {
			String fenstring = line.substring(13);
			board.setPositionFenstring(fenstring);
			log(board.repr());
		}
		else if(line.startsWith("go ")) {
			long[] move = brain.getMove(board);
			String moveLongAlgebraic = board.moveToLongAlgebraic(move);
			respond("bestmove " + moveLongAlgebraic);
		}
	}
	
	public static void main(String[] args) {
		logClear();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		while(true) {
			try {
				line = reader.readLine();
			}
			catch(IOException e) {
			}
			interpretUCICommand(line);
		}
	}
}
