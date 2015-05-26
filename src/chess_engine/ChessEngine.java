package chess_engine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import chess_engine.Board;
import chess_engine.Brain;

public class ChessEngine {
	public static void main(String[] args) {
		int PLAYER_TURN = 0;
		Board board = new Board();
		ArrayList<Board> boardHistory = new ArrayList<Board>();
		Brain brain = new Brain();
		boolean moveIsLegal = false;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String input = "";
		System.out.println("Choose a side: white or black.");
		while(!input.equalsIgnoreCase("white") && !input.equalsIgnoreCase("black")) {
			try {
				input = reader.readLine();
			} catch(IOException e) {
			}
		}
		if(input.equals("white")) {
			PLAYER_TURN = Board.WHITE;
		}
		else {
			PLAYER_TURN = Board.BLACK;
		}
		boardHistory.add(new Board(board));
		System.out.println(board.repr());
		while(true) {
			if(board.turn == PLAYER_TURN) {
				moveIsLegal = false;
				while(!moveIsLegal) {
					try {
						input = reader.readLine();
					} catch(IOException e) {
					}
					if(input.equalsIgnoreCase("undo")) {
						if(boardHistory.size() < 2) {
							System.out.println("Not enough history to undo.");
							continue;
						}
						boardHistory.remove(boardHistory.size() - 1);
						boardHistory.remove(boardHistory.size() - 1);
						board = new Board(boardHistory.get(boardHistory.size() - 1));
						System.out.println(board.repr());
						continue;
					}
					try {
						board.move(input);
					}
					catch(IllegalMoveException e) {
						System.out.println(e);
						continue;
					}
					moveIsLegal = true;
				}
				boardHistory.add(new Board(board));
				System.out.println(board.repr());
				int isOver = board.isOver();
				if(isOver == board.OVER_CHECKMATE) {
					System.out.println("Checkmate!! Player wins.\n\n");
					break;
				}
				else if(isOver == board.OVER_STALEMATE) {
					System.out.println("Stalemate!!\n\n");
					break;
				}
			}
			else {
				moveIsLegal = false;
				long[] move = null;
				String moveRepr = "";
				while(!moveIsLegal) {
					long startTime = System.nanoTime(); 
					move = brain.getMove(board);
					long elapsedTime = System.nanoTime() - startTime;
					moveRepr = board.moveToAlgebraic(move);
					System.out.println("Move: " + moveRepr);
					try {
						board.move(move);
					}
					catch(IllegalMoveException e) {
						System.out.println("Opponent tried to make a move that was not legal.");
						continue;
					}
					float elapsedTimeSeconds = (float)(int)((float)elapsedTime / 100000000) / 10;
					System.out.println("Elapsed time: " + elapsedTimeSeconds + "s");
					moveIsLegal = true;
				}
				boardHistory.add(new Board(board));
				System.out.println(board.repr());
				int isOver = board.isOver();
				if(isOver == board.OVER_CHECKMATE) {
					System.out.println("Checkmate!! Computer wins.\n\n");
					break;
				}
				else if(isOver == board.OVER_STALEMATE) {
					System.out.println("Stalemate!!\n\n");
					break;
				}
			}
		}
	}
}
