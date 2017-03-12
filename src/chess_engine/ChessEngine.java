package chess_engine;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import chess_engine.Board;
import chess_engine.Brain;

public class ChessEngine {
	static Board board = new Board();
	static Brain brain = new Brain();
	static NotationHelper notationHelper = new NotationHelper();
	
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
	
	public static void logPrincipalVariation(Brain brain, Board board, Move move) {
		ArrayList<Move> principalVariation = brain.getPrincipalVariation(board, move);
		ArrayList<String> movesAlgebraic = new ArrayList<String>();
		for(Move pvMove : principalVariation) {
			movesAlgebraic.add(notationHelper.moveToAlgebraic(board, pvMove));
			board.move(pvMove);
		}
		log("PV: " + movesAlgebraic);
	}
	
	public static void interpretUCICommand(String line) {
		// Implements https://en.wikipedia.org/wiki/Universal_Chess_Interface
		log("< " + line);
		if(line.equals("uci")) {
			respond("id name Phil");
			respond("id author Phil Leszczynski");
			respond("uciok");
		}
		else if(line.equals("isready")) {
			respond("readyok");
		}
		else if(line.startsWith("position fen ")) {
			String fenstring = line.substring(13);
			log(fenstring);
			if(board.enPassantTarget != 0) {
				log("EP Target: " + NotationHelper.coordToSquare(board.enPassantTarget));
			}
			else {
				log("EP Target: None");
			}
			board.setPositionFenstring(fenstring);
			log(board.toString());
		}
		else if(line.startsWith("go ")) {
			Move move = brain.getMove(board);
			String moveLongAlgebraic = notationHelper.moveToLongAlgebraic(board, move);
			logPrincipalVariation(brain, board, move);
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
