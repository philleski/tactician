package chess_engine;

import java.util.ArrayList;

import chess_engine.Board;
import chess_engine.IllegalMoveException;

public class Brain {
	static int TABLE_SIZE = 4 * 1024 * 1024;
	
	public Brain() {
		this.transpositionTable = new TranspositionTable(TABLE_SIZE);
	}
	
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
		return 1 - (blackMaterial + whiteMaterial) /
				(2 * this.FITNESS_START_NOKING);
	}
	
	private float fitnessKingSafety(Board board, Color color, float endgameFraction) {
		// The king should be in the corner at the beginning of the game
		// because it's safer there, but in the center at the end of the game
		// because it's a fighting piece and the opponent can't really
		// checkmate it anyway.
		int distanceFromHomeRank = 0;
		int kingIndex = 0;
		if(color == Color.WHITE) {
			kingIndex = board.whiteKingIndex;
			distanceFromHomeRank = (int)(kingIndex / 8);
		}
		else {
			kingIndex = board.blackKingIndex;
			distanceFromHomeRank = 7 - (int)(kingIndex / 8);
		}
		float rankFitness = -this.FITNESS_KING_RANK_FACTOR * distanceFromHomeRank * (0.7f - endgameFraction);
		float fileFitness = this.FITNESS_KING_FILE[kingIndex % 8] * (0.7f - endgameFraction);
				
		return rankFitness + fileFitness;
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
			fitness += this.fitnessKingSafety(board, Color.WHITE, endgameFraction) -
					this.fitnessKingSafety(board, Color.BLACK, endgameFraction);
		}
		else {
			fitness = blackMaterial - whiteMaterial;
			fitness += this.fitnessKingSafety(board, Color.BLACK, endgameFraction) -
					this.fitnessKingSafety(board, Color.WHITE, endgameFraction);
		}
						
		return fitness;
	}
	
	private float probeCapture(Board board, byte target) {
		ArrayList<Move> lmf = board.legalMovesFast(true);
		float bestFitness = this.fitness(board);
		Move candidateMove = null;
		float candidateValue = this.FITNESS_LARGE;
		if(board.turn == Color.WHITE) {
			for(Move move : lmf) {
				if(move.destination != target) {
					continue;
				}
				long sourceMask = 1L << move.source;
				if((sourceMask & board.whitePawns) != 0) {
					if(this.FITNESS_PAWN < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_PAWN;
					}
				}
				else if((sourceMask & board.whiteKnights) != 0) {
					if(this.FITNESS_KNIGHT < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_KNIGHT;
					}
				}
				else if((sourceMask & board.whiteBishops) != 0) {
					if(this.FITNESS_BISHOP < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_BISHOP;
					}
				}
				else if((sourceMask & board.whiteRooks) != 0) {
					if(this.FITNESS_ROOK < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_ROOK;
					}
				}
				else if((sourceMask & board.whiteQueens) != 0) {
					if(this.FITNESS_QUEEN < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_QUEEN;
					}
				}
				else {
					if(this.FITNESS_KING < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_KING;
					}
				}
			}
		}
		else {
			for(Move move : lmf) {
				if(move.destination != target) {
					continue;
				}
				long sourceMask = 1L << move.source;
				if((sourceMask & board.blackPawns) != 0) {
					if(this.FITNESS_PAWN < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_PAWN;
					}
				}
				else if((sourceMask & board.blackKnights) != 0) {
					if(this.FITNESS_KNIGHT < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_KNIGHT;
					}
				}
				else if((sourceMask & board.blackBishops) != 0) {
					if(this.FITNESS_BISHOP < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_BISHOP;
					}
				}
				else if((sourceMask & board.blackRooks) != 0) {
					if(this.FITNESS_ROOK < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_ROOK;
					}
				}
				else if((sourceMask & board.blackQueens) != 0) {
					if(this.FITNESS_QUEEN < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_QUEEN;
					}
				}
				else {
					if(this.FITNESS_KING < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_KING;
					}
				}
			}
		}
		if(candidateMove != null) {
			Board copy = new Board(board);
			try {
				copy.move(candidateMove);
			}
			catch(IllegalMoveException e) {
			}
			float candidateFitness = -this.probeCapture(copy, target);
			if(candidateFitness > bestFitness) {
				bestFitness = candidateFitness;
			}
		}
		return bestFitness;
	}

	private float alphabeta(Board board, int depth, float alpha, float beta) {
		// http://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning
		if(depth == 0) {
			TranspositionTable.TranspositionEntry entry =
					this.transpositionTable.get(board.positionHash);
			if(entry != null && entry.depth == 0) {
				return entry.fitness;
			}
			float fitness = this.fitness(board);
			this.transpositionTable.put(depth, board.positionHash,
					fitness,
					TranspositionTable.TranspositionType.NODE_EXACT);
			return fitness;
		}
		float superlativeFitness = 0;
		if(depth % 2 == 0) {
			superlativeFitness = -FITNESS_LARGE;
		}
		else {
			superlativeFitness = FITNESS_LARGE;
		}
				
		// Check for stalemate or checkmate.
		ArrayList<Move> lmf = board.legalMovesFast(false);
		if(lmf.size() <= 8) {
			boolean isMate = true;
			long kings = board.blackKings | board.whiteKings;
			for(Move move : lmf) {
				if(((1L << move.source) & kings) == 0) {
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
		
		Move killer = null;
		float killerFitness = 0;
		if(depth % 2 == 0) {
			killerFitness = FITNESS_LARGE;
		}
		else {
			killerFitness = -FITNESS_LARGE;
		}
		for(Move move : lmf) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			TranspositionTable.TranspositionEntry entry =
					this.transpositionTable.get(copy.positionHash);
			if(entry != null) {
				if(depth % 2 == 0 && entry.fitness < killerFitness) {
					killer = move;
				}
				else if(depth % 2 == 1 && entry.fitness > killerFitness) {
					killer = move;
				}
			}
		}
		if(killer != null) {
			ArrayList<Move> lmfNew = new ArrayList<Move>();
			lmfNew.add(killer);
			for(Move move : lmf) {
				// FIXME - have to deal with promotion
				if(move.source != killer.source && move.destination != killer.destination) {
					lmfNew.add(move);
				}
			}
			lmf = lmfNew;
		}
		
		boolean isCapture = false;
		long superlativePositionHash = 0;
		for(Move move : lmf) {
			Board copy = new Board(board);
			if(((1L << move.destination) & copy.allPieces) != 0) {
				isCapture = true;
			}
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float fitness = 0;
			TranspositionTable.TranspositionEntry entry =
					this.transpositionTable.get(copy.positionHash);
			boolean found = false;
			if(entry != null && entry.depth == depth) {
				if(entry.type == TranspositionTable.TranspositionType.NODE_EXACT) {
					fitness = entry.fitness;
					found = true;
				}
				else {
					if(depth % 2 == 0) {
						if(entry.type == TranspositionTable.TranspositionType.NODE_ALPHA) {
							if(entry.fitness > alpha) {
								alpha = entry.fitness;
								if(superlativeFitness < alpha) {
									superlativeFitness = entry.fitness;
									superlativePositionHash = copy.positionHash;
								}
								found = true;
							}
						}
					}
					else {
						if(entry.type == TranspositionTable.TranspositionType.NODE_BETA) {
							if(entry.fitness < beta) {
								beta = entry.fitness;
								if(superlativeFitness > beta) {
									superlativeFitness = entry.fitness;
									superlativePositionHash = copy.positionHash;
								}
								found = true;
							}
						}
					}
				}
			}
			if(!found) {
				if(depth == 1 && isCapture) {
					// This is to deal with the scenario where say the queen
					// captures a heavily guarded pawn right when the depth
					// expires.
					fitness = this.probeCapture(copy, move.destination);
				}
				else {
					fitness = this.alphabeta(copy, depth - 1, alpha, beta);
				}
			}
			// System.out.println("Count: " + exactCount + " " + alphaCount + " " + betaCount + " " + foundCount + " " + totalCount);
			if(depth % 2 == 0 && fitness > superlativeFitness) {
				superlativeFitness = fitness;
				superlativePositionHash = copy.positionHash;
				if(superlativeFitness > alpha) {
					alpha = superlativeFitness;
				}
				if(beta <= alpha) {
					break;
				}
			}
			else if(depth % 2 == 1 && fitness < superlativeFitness) {
				superlativeFitness = fitness;
				superlativePositionHash = copy.positionHash;
				if(superlativeFitness < beta) {
					beta = superlativeFitness;
				}
				if(beta <= alpha) {
					break;
				}
			}
		}
		if(superlativeFitness < alpha) {
			this.transpositionTable.put(depth, superlativePositionHash,
					superlativeFitness,
					TranspositionTable.TranspositionType.NODE_ALPHA);
		}
		else if(superlativeFitness > beta) {
			this.transpositionTable.put(depth, superlativePositionHash,
					superlativeFitness,
					TranspositionTable.TranspositionType.NODE_BETA);
		}
		else {
			this.transpositionTable.put(depth, superlativePositionHash,
					superlativeFitness,
					TranspositionTable.TranspositionType.NODE_EXACT);
		}
		return superlativeFitness;
	}
	
	private Move getMoveToDepth(Board board, int depth) {
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
	
	public Move getMove(Board board) {
		int depth = 0;
		float endgameFraction = this.endgameFraction(board);
		System.out.println("EF: " + endgameFraction);
		if(endgameFraction < 0.8) {
			depth = 4;
		}
		else {
			depth = 5;
		}
		System.out.println("Depth: " + depth);
		Move move = null;
		for(int d = 1; d <= depth; d++) {
			move = this.getMoveToDepth(board, d);
		}
		return move;
	}
	
	private TranspositionTable transpositionTable = null;
	
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
	private float FITNESS_KING_RANK_FACTOR = 50;
	private float[] FITNESS_KING_FILE = {0, 0, -50, -100, -100, -50, 0, 0};
}
