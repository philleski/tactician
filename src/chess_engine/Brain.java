package chess_engine;

import java.util.ArrayList;

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
	
	private float endgameFraction(Board board) {
		// Returns 0 if it's the start of the game, 1 if it's just two kings.
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
		return (blackMaterial + whiteMaterial) /
				(2 * this.FITNESS_START_NOKING);
	}

	private float fitness(Board board) {
		int blackBishopCount = numBitsSet(board.blackBishops);
		int blackKingCount = numBitsSet(board.blackKings);
		int blackKnightCount = numBitsSet(board.blackKnights);
		int blackQueenCount = numBitsSet(board.blackQueens);
		int blackRookCount = numBitsSet(board.blackRooks);
		int whiteBishopCount = numBitsSet(board.whiteBishops);
		int whiteKingCount = numBitsSet(board.whiteKings);
		int whiteKnightCount = numBitsSet(board.whiteKnights);
		int whiteQueenCount = numBitsSet(board.whiteQueens);
		int whiteRookCount = numBitsSet(board.whiteRooks);
				
		float blackMaterial =
			blackBishopCount * this.FITNESS_BISHOP +
			blackKingCount * this.FITNESS_KING +
			blackKnightCount * this.FITNESS_KNIGHT +
			blackQueenCount * this.FITNESS_QUEEN +
			blackRookCount * this.FITNESS_ROOK;
		float whiteMaterial =
			whiteBishopCount * this.FITNESS_BISHOP +
			whiteKingCount * this.FITNESS_KING +
			whiteKnightCount * this.FITNESS_KNIGHT +
			whiteQueenCount * this.FITNESS_QUEEN +
			whiteRookCount * this.FITNESS_ROOK;
		
		float endgameFraction = this.endgameFraction(board);
		
		// Since pawns can't be on the edge ranks.
		for(int rank = 1; rank < 7; rank++) {
			for(int centrality = 0; centrality < 4; centrality++) {
				// Magic number: a1-h1
				long rankMaskWhite = 0x00000000000000ffL << (rank * 8);
				// Magic number: a8-h8
				long rankMaskBlack = 0xff00000000000000L >>> (rank * 8);
				// Magic number: a1-a8
				long centralityMask = (0x0101010101010101L << centrality) |
						(0x0101010101010101L << (8 - centrality));
				
				float pawnFactor = (1 - endgameFraction) *
						this.FITNESS_PAWN_TABLE_OPENING[rank][centrality];
				pawnFactor += endgameFraction *
						this.FITNESS_PAWN_TABLE_ENDGAME[rank][centrality];
								
				blackMaterial += pawnFactor *
					numBitsSet(board.blackPawns & rankMaskBlack &
							centralityMask);
				whiteMaterial += pawnFactor *
					numBitsSet(board.whitePawns & rankMaskWhite &
							centralityMask);
			}
		}
		
		float fitness = 0;
		if(board.turn == Color.WHITE) {
			fitness = whiteMaterial - blackMaterial;
		}
		else {
			fitness = blackMaterial - whiteMaterial;
		}
		
		return fitness;
	}
	
	private float probeCapture(Board board, long target) {
		ArrayList<Move> lmf = board.legalMovesFast();
		float bestFitness = this.fitness(board);
		for(Move move : lmf) {
			if(move.destination != target) {
				continue;
			}
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float candidate = -this.probeCapture(copy, target);
			if(candidate > bestFitness) {
				bestFitness = candidate;
			}
		}
		return bestFitness;
	}

	private float alphabeta(Board board, int depth, float alpha, float beta) {
		// http://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning
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
		ArrayList<Move> lmf = board.legalMovesFast();
		if(lmf.size() <= 8) {
			boolean isMate = true;
			long kings = board.blackKings | board.whiteKings;
			for(Move move : lmf) {
				if((move.source & kings) == 0) {
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
					// Stalemate
					return 0;
				}
			}
		}
		
		for(Move move : lmf) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float fitness = 0;
			if(depth == 1 && (move.destination & copy.allPieces) != 0) {
				// This is to deal with the scenario where say the queen
				// captures a heavily guarded pawn right when the depth
				// expires.
				fitness = this.probeCapture(copy, move.destination);
			}
			else {
				fitness = this.alphabeta(copy, depth - 1, alpha, beta);
			}
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
	
	public Move getMove(Board board) {
		int depth = 0;
		float endgameFraction = this.endgameFraction(board);
		System.out.println("EF: " + endgameFraction);
		if(endgameFraction > 0.3) {
			depth = 5;
		}
		else {
			depth = 6;
		}
		System.out.println("Depth: " + depth);
		
		Move bestMove = null;
		float superlativeFitness = 0;
		if(depth % 2 == 0) {
			superlativeFitness = -FITNESS_LARGE;
		}
		else {
			superlativeFitness = FITNESS_LARGE;
		}
		for(Move move : board.legalMoves()) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float fitness = this.alphabeta(copy, depth - 1, -FITNESS_LARGE,
					FITNESS_LARGE);
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
	private float FITNESS_LARGE = 1000000000;   // FIXME - change to short int and get rid of this
	private float FITNESS_BISHOP = 333;
	private float FITNESS_KING = 1000000;
	private float FITNESS_KNIGHT = 320;
	private float FITNESS_PAWN = 100;
	private float FITNESS_QUEEN = 880;
	private float FITNESS_ROOK = 510;
	private float FITNESS_START_NOKING = 2 * FITNESS_ROOK +
		2 * FITNESS_KNIGHT + 2 * FITNESS_BISHOP + FITNESS_QUEEN +
		8 * FITNESS_PAWN;
	
	// It goes as [rank][centrality]. rank goes from 0 to 7 and is from the
	// perspective of that player. centrality goes from 0 (files a, h) to 3
	// (files d, e).
	private float[][] FITNESS_PAWN_TABLE_OPENING = {
		{0, 0, 0, 0},
		{90, 95, 105, 110},
		{90, 95, 105, 115},
		{90, 95, 110, 120},
		{97, 103, 117, 127},
		{106, 112, 125, 140},
		{117, 122, 134, 159},
		{0, 0, 0, 0}
	};
	private float[][] FITNESS_PAWN_TABLE_ENDGAME = {
		{0, 0, 0, 0},
		{120, 105, 95, 90},
		{120, 105, 95, 90},
		{125, 110, 100, 95},
		{133, 117, 107, 100},
		{145, 129, 116, 105},
		{161, 146, 127, 110},
		{0, 0, 0, 0}
	};
}
