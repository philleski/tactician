package chess_engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import chess_engine.Board;
import chess_engine.IllegalMoveException;

public class Brain {
	static int TABLE_SIZE = 4 * 1024 * 1024;
	
	public Brain() {
		this.transpositionTable = new TranspositionTable(TABLE_SIZE);
		
		this.FITNESS_PIECE.put(Piece.BISHOP, 333f);
		this.FITNESS_PIECE.put(Piece.KING, 1000000f);
		this.FITNESS_PIECE.put(Piece.KNIGHT, 320f);
		this.FITNESS_PIECE.put(Piece.PAWN, 100f);
		this.FITNESS_PIECE.put(Piece.QUEEN, 880f);
		this.FITNESS_PIECE.put(Piece.ROOK, 510f);
		
		this.FITNESS_START_NOKING =
			2 * FITNESS_PIECE.get(Piece.ROOK) +
			2 * FITNESS_PIECE.get(Piece.KNIGHT) +
			2 * FITNESS_PIECE.get(Piece.BISHOP) +
			FITNESS_PIECE.get(Piece.QUEEN) +
			8 * FITNESS_PIECE.get(Piece.PAWN);
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
		
		float blackMaterial = 0;
		float whiteMaterial = 0;
		for(Map.Entry<Piece, Float> entry : this.FITNESS_PIECE.entrySet()) {
			Piece piece = entry.getKey();
			if(piece == Piece.KING) {
				continue;
			}
			int blackPieceCount = numBitsSet(board.bitboards.get(Color.BLACK).get(piece).data);
			int whitePieceCount = numBitsSet(board.bitboards.get(Color.WHITE).get(piece).data);
			blackMaterial += blackPieceCount * this.FITNESS_PIECE.get(piece);
			whiteMaterial += whitePieceCount * this.FITNESS_PIECE.get(piece);
		}
		
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
		float blackMaterial = 0;
		float whiteMaterial = 0;
		for(Map.Entry<Piece, Float> entry : this.FITNESS_PIECE.entrySet()) {
			Piece piece = entry.getKey();
			if(piece == Piece.PAWN) {
				continue;
			}
			int blackPieceCount = numBitsSet(board.bitboards.get(Color.BLACK).get(piece).data);
			int whitePieceCount = numBitsSet(board.bitboards.get(Color.WHITE).get(piece).data);
			blackMaterial += blackPieceCount * this.FITNESS_PIECE.get(piece);
			whiteMaterial += whitePieceCount * this.FITNESS_PIECE.get(piece);
		}
		
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
					numBitsSet(board.bitboards.get(Color.BLACK).get(Piece.PAWN).data &
						rankMaskBlack & centralityMask);
				whiteMaterial += pawnFactor *
					numBitsSet(board.bitboards.get(Color.WHITE).get(Piece.PAWN).data & 
						rankMaskWhite & centralityMask);
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
		for(Move move : lmf) {
			if(move.destination != target) {
				continue;
			}
			long sourceMask = 1L << move.source;
			for(Map.Entry<Piece, Float> entry : this.FITNESS_PIECE.entrySet()) {
				Piece piece = entry.getKey();
				if((sourceMask & board.bitboards.get(board.turn).get(piece).data) != 0) {
					if(this.FITNESS_PIECE.get(piece) < candidateValue) {
						candidateMove = move;
						candidateValue = this.FITNESS_PIECE.get(piece);
						break;
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
			long kings = board.bitboards.get(Color.BLACK).get(Piece.KING).data |
					board.bitboards.get(Color.WHITE).get(Piece.KING).data;
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
		
		boolean isCapture = false;
		long superlativePositionHash = 0;
		for(Move move : lmf) {
			Board copy = new Board(board);
			if(((1L << move.destination) & copy.allPieces.data) != 0) {
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
	private Map<Piece, Float> FITNESS_PIECE = new HashMap<Piece, Float>();
	private float FITNESS_START_NOKING = 0;
	
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
