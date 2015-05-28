package chess_engine;

import java.util.*;
import chess_engine.Board;
import chess_engine.IllegalMoveException;

public class Brain {
	private static int numBitsSet(long x) {
		// Taken from http://en.wikipedia.org/wiki/Hamming_weight
		int count;
		for(count = 0; x != 0; count++) {
			x &= x - 1;
		}
		return count;
	}
	
	private boolean isEndgame(Board board) {
		int blackBishopCount = numBitsSet(board.blackBishops);
		int blackKnightCount = numBitsSet(board.blackKnights);
		int blackPawnCount = numBitsSet(board.blackPawns);
		int blackQueenCount = numBitsSet(board.blackQueens);
		int blackRookCount = numBitsSet(board.blackRooks);
		int whiteBishopCount = numBitsSet(board.whiteBishops);
		int whiteKnightCount = numBitsSet(board.whiteKnights);
		int whitePawnCount = numBitsSet(board.whitePawns);
		int whiteQueenCount = numBitsSet(board.whiteQueens);
		int whiteRookCount = numBitsSet(board.whiteRooks);
		
		float blackMaterial =
			blackBishopCount * this.FITNESS_BISHOP +
			blackKnightCount * this.FITNESS_KNIGHT +
			blackPawnCount * this.FITNESS_PAWN +
			blackQueenCount * this.FITNESS_QUEEN +
			blackRookCount * this.FITNESS_ROOK;
		float whiteMaterial =
			whiteBishopCount * this.FITNESS_BISHOP +
			whiteKnightCount * this.FITNESS_KNIGHT +
			whitePawnCount * this.FITNESS_PAWN +
			whiteQueenCount * this.FITNESS_QUEEN +
			whiteRookCount * this.FITNESS_ROOK;
		if(blackMaterial <= 1300 && whiteMaterial <= 1300) {
			return true;
		}
		return false;
	}
	
	private float fitness(Board board) {
		int blackBishopCount = numBitsSet(board.blackBishops);
		int blackKingCount = numBitsSet(board.blackKings);
		int blackKnightCount = numBitsSet(board.blackKnights);
		int blackPawnCount = numBitsSet(board.blackPawns);
		int blackQueenCount = numBitsSet(board.blackQueens);
		int blackRookCount = numBitsSet(board.blackRooks);
		int whiteBishopCount = numBitsSet(board.whiteBishops);
		int whiteKingCount = numBitsSet(board.whiteKings);
		int whiteKnightCount = numBitsSet(board.whiteKnights);
		int whitePawnCount = numBitsSet(board.whitePawns);
		int whiteQueenCount = numBitsSet(board.whiteQueens);
		int whiteRookCount = numBitsSet(board.whiteRooks);
				
		float blackMaterial =
			blackBishopCount * this.FITNESS_BISHOP +
			blackKingCount * this.FITNESS_KING +
			blackKnightCount * this.FITNESS_KNIGHT +
			blackPawnCount * this.FITNESS_PAWN +
			blackQueenCount * this.FITNESS_QUEEN +
			blackRookCount * this.FITNESS_ROOK;
		float whiteMaterial =
			whiteBishopCount * this.FITNESS_BISHOP +
			whiteKingCount * this.FITNESS_KING +
			whiteKnightCount * this.FITNESS_KNIGHT +
			whitePawnCount * this.FITNESS_PAWN +
			whiteQueenCount * this.FITNESS_QUEEN +
			whiteRookCount * this.FITNESS_ROOK;
		
		float fitness = 0;
		if(board.turn == board.WHITE) {
			fitness = whiteMaterial - blackMaterial;
		}
		else {
			fitness = blackMaterial - whiteMaterial;
		}
		
		return fitness;
	}

	// http://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning
	private float alphabeta(Board board, int depth, float alpha, float beta) {
		if(depth == 0) {
			return this.fitness(board);
		}
		float superlativeFitness = 0;
		if(depth % 2 == 0) {
			superlativeFitness = -FITNESS_LARGE;
		}
		else {
			superlativeFitness = FITNESS_LARGE;
		}
		
		// Check for stalemate or checkmate.
		ArrayList<long[]> lmf = board.legalMovesFast();
		if(lmf.size() <= 8) {
			boolean isMate = true;
			long kings = board.blackKings | board.whiteKings;
			for(long[] move : lmf) {
				long source = move[0];
				if((source & kings) == 0) {
					isMate = false;
					break;
				}
			}
			if(isMate) {
				if(board.isInCheck()) {
					// Checkmate
					return -FITNESS_LARGE;
				}
				else {
					return 0;
				}
			}
		}
		
		for(long[] move : lmf) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float fitness = this.alphabeta(copy, depth - 1, alpha, beta);
			if(depth % 2 == 0 && fitness > superlativeFitness) {
				superlativeFitness = fitness;
				if(superlativeFitness > alpha) {
					alpha = superlativeFitness;
				}
				if(beta <= alpha) {
					break;
				}
			}
			else if(depth % 2 == 1 && fitness < superlativeFitness) {
				superlativeFitness = fitness;
				if(superlativeFitness < beta) {
					beta = superlativeFitness;
				}
				if(beta <= alpha) {
					break;
				}
			}
		}
		return superlativeFitness;
	}
	
	public long[] getMove(Board board) {
		int depth = 0;
		if(this.isEndgame(board)) {
			depth = 7;
		}
		else {
			depth = 6;
		}
		System.out.println("Depth: " + depth);
		
		long[] bestMove = null;
		float superlativeFitness = 0;
		if(depth % 2 == 0) {
			superlativeFitness = -FITNESS_LARGE;
		}
		else {
			superlativeFitness = FITNESS_LARGE;
		}
		for(long[] move : board.legalMoves()) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float fitness = this.alphabeta(copy, depth - 1, -FITNESS_LARGE, FITNESS_LARGE);
			fitness += Math.random() * 0.01;
			if(depth % 2 == 0 && fitness > superlativeFitness) {
				superlativeFitness = fitness;
				bestMove = move;
			}
			else if(depth % 2 == 1 && fitness < superlativeFitness) {
				superlativeFitness = fitness;
				bestMove = move;
			}
		}
		return bestMove;
	}
	
	// These are all in centipawns.
	private float FITNESS_LARGE = 1000000000;   // A large value used for initialization.
	private float FITNESS_BISHOP = 333;
	private float FITNESS_KING = 1000000;
	private float FITNESS_KNIGHT = 320;
	private float FITNESS_PAWN = 100;
	private float FITNESS_QUEEN = 880;
	private float FITNESS_ROOK = 510;
}
