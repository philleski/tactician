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
	
	public float endgameFraction(Board board) {
		// Returns 0 if the opponent has all the pieces, 1 if just the king.
		float material = 0;
		for(Map.Entry<Piece, Float> entry : this.FITNESS_PIECE.entrySet()) {
			Piece piece = entry.getKey();
			if(piece == Piece.KING) {
				continue;
			}
			int pieceCount = numBitsSet(board.bitboards.get(
				Color.flip(board.turn)).get(piece).data);
			material += pieceCount * this.FITNESS_PIECE.get(piece);
		}
		
		return 1 - material / this.FITNESS_START_NOKING;
	}
	
	public float fitnessKingSafety(Board board, Color color, float endgameFraction) {
		// The king should be in the corner at the beginning of the game
		// because it's safer there, but in the center at the end of the game
		// because it's a fighting piece and the opponent can't really
		// checkmate it anyway.
		int distanceFromHomeRank = 0;
		int kingIndex = Long.numberOfTrailingZeros(
			board.bitboards.get(color).get(Piece.KING).data);
		if(color == Color.WHITE) {
			distanceFromHomeRank = (int)(kingIndex / 8);
		}
		else {
			distanceFromHomeRank = 7 - (int)(kingIndex / 8);
		}
		float rankFitness = -this.FITNESS_KING_RANK_FACTOR * distanceFromHomeRank * (0.7f - endgameFraction);
		float fileFitness = this.FITNESS_KING_FILE[kingIndex % 8] * (0.7f - endgameFraction);
		
		float openFilePenalty = 0;
		float pawnShieldPenalty = 0;
		if(endgameFraction < 0.7) {
			int protectorsHome = 3;
			int protectorsOneStep = 0;
			if(color == Color.WHITE) {
				if(kingIndex % 8 <= 2) {
					protectorsHome = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x0000000000000700L);
					protectorsOneStep = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x0000000000070000L);
				} else if(kingIndex % 8 >= 5) {
					protectorsHome = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x000000000000E000L);
					protectorsOneStep = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x0000000000E00000L);					
				}
			} else {
				if(kingIndex % 8 <= 2) {
					protectorsHome = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x0007000000000000L);
					protectorsOneStep = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x0000070000000000L);
				} else if(kingIndex % 8 >= 5) {
					protectorsHome = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x00E0000000000000L);
					protectorsOneStep = numBitsSet(
						board.bitboards.get(color).get(Piece.PAWN).data & 0x0000E00000000000L);
				}
			}
			if(protectorsHome + protectorsOneStep == 2) {
				pawnShieldPenalty = 25 * protectorsHome + 50 * protectorsOneStep;
			} else if(protectorsHome + protectorsOneStep == 1) {
				pawnShieldPenalty = 50 * protectorsHome + 75 * protectorsOneStep;
			} else if(protectorsHome + protectorsOneStep == 0) {
				pawnShieldPenalty = 150;
			}
			pawnShieldPenalty *= (1 - endgameFraction);
			
			if(kingIndex % 8 <= 2 || kingIndex % 8 >= 5) {
				// Don't have an open file penalty before castling, as we may
				// get opportunities to capture pawns in the center.
				long file = 0x0101010101010101L << (kingIndex % 8);
				if((board.bitboards.get(color).get(Piece.PAWN).data & file) == 0) {
					openFilePenalty = 150 * (1 - endgameFraction);
				}
			}
		}
		
		return rankFitness + fileFitness - pawnShieldPenalty - openFilePenalty;
	}
	
	public float fitness(Board board) {
		float fitness = 0;
		Color turnFlipped = Color.flip(board.turn);
		for(Map.Entry<Piece, Float> entry : this.FITNESS_PIECE.entrySet()) {
			Piece piece = entry.getKey();
			if(piece == Piece.PAWN) {
				continue;
			}
			int myPieceCount = numBitsSet(board.bitboards.get(board.turn).get(piece).data);
			int oppPieceCount = numBitsSet(board.bitboards.get(turnFlipped).get(piece).data);
			fitness += (myPieceCount - oppPieceCount) * this.FITNESS_PIECE.get(piece);
		}
		
		float endgameFraction = this.endgameFraction(board);
		
		Bitboard pawnBitboardRelativeToMe =
			board.bitboards.get(board.turn).get(Piece.PAWN);
		Bitboard pawnBitboardRelativeToOpp =
			board.bitboards.get(turnFlipped).get(Piece.PAWN);
		if(board.turn == Color.WHITE) {
			pawnBitboardRelativeToOpp = pawnBitboardRelativeToOpp.flip();
		} else {
			pawnBitboardRelativeToMe = pawnBitboardRelativeToMe.flip();
		}
		
		// Since pawns can't be on the edge ranks.
		for(int rank = 1; rank < 7; rank++) {
			long rankMask = 0x00000000000000ffL << (rank * 8);
			for(int centrality = 0; centrality < 4; centrality++) {
				long centralityMask = (0x0101010101010101L << centrality) |
						(0x0101010101010101L << (8 - centrality));
				
				float pawnFactor = (1 - endgameFraction) *
						this.FITNESS_PAWN_TABLE_OPENING[rank][centrality];
				pawnFactor += endgameFraction *
						this.FITNESS_PAWN_TABLE_ENDGAME[rank][centrality];
				
				int myPawnsOnRank = numBitsSet(pawnBitboardRelativeToMe.data &
					rankMask & centralityMask);
				int oppPawnsOnRank = numBitsSet(pawnBitboardRelativeToOpp.data &
					rankMask & centralityMask);
				
				fitness += pawnFactor * (myPawnsOnRank - oppPawnsOnRank);
			}
		}
		
		fitness += this.fitnessKingSafety(board, board.turn, endgameFraction) -
				this.fitnessKingSafety(board, turnFlipped, endgameFraction);
						
		return fitness;
	}
	
	private float quiescentSearch(Board board, float alpha, float beta, long target) {
		// http://chessprogramming.wikispaces.com/Quiescence+Search
		float fitness = this.fitness(board);
		if(fitness >= beta) {
			return beta;
		}
		if(fitness > alpha) {
			alpha = fitness;
		}
		// The player whose king gets captured first loses, even if the other
		// king gets captured next turn.
		if(board.bitboards.get(board.turn).get(Piece.KING).data == 0L) {
			return -FITNESS_LARGE;
		}
		ArrayList<Move> lmf = board.legalMovesFast(true);
		for(Move move : lmf) {
			long moveTarget = 1L << move.destination;
			if(target != -1 && moveTarget != target) {
				// Only probe captures happening on the same square.
				continue;
			}
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			fitness = -this.quiescentSearch(copy, -beta, -alpha, moveTarget);
			if(fitness >= beta) {
				return beta;
			}
			if(fitness > alpha) {
				alpha = fitness;
			}
		}
		return alpha;
	}
	
	public float alphabeta(Board board, int depth, float alpha, float beta) {
		// http://chessprogramming.wikispaces.com/Alpha-Beta
		if(depth == 0) {
			return this.quiescentSearch(board, alpha, beta, -1);
		}
		TranspositionTable.TranspositionEntry entry =
				this.transpositionTable.get(board.positionHash);
		Move lastBestMove = null;
		if(entry != null) {
			if(entry.depth == depth) {
				if(entry.type == TranspositionTable.TranspositionType.NODE_PV) {
					return entry.fitness;
				} else if(entry.type == TranspositionTable.TranspositionType.NODE_CUT) {
					beta = entry.fitness;
				} else if(entry.type == TranspositionTable.TranspositionType.NODE_ALL) {
					alpha = entry.fitness;
				}
				if(alpha >= beta) {
					return entry.fitness;
				}
			}
			lastBestMove = entry.bestMove;
		}
		ArrayList<Move> lmf = board.legalMovesFast(false);
		if(lmf.size() <= 8 || board.isInCheck()) {
			// Check for stalemate or checkmate.
			ArrayList<Move> legalMoves = board.legalMoves();
			if(legalMoves.size() == 0) {
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
		if(lastBestMove != null) {
			ArrayList<Move> lmf2 = new ArrayList<Move>();
			boolean found = false;
			for(Move move : lmf) {
				if(move.equals(lastBestMove)) {
					found = true;
				} else {
					lmf2.add(move);
				}
			}
			if(found) {
				lmf2.add(0, lastBestMove);
				lmf = lmf2;
			}
		}
		TranspositionTable.TranspositionType nodeType =
				TranspositionTable.TranspositionType.NODE_ALL;
		Move bestMove = null;
		for(Move move : lmf) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float fitness = -this.alphabeta(copy, depth - 1, -beta, -alpha);
			if(fitness >= beta) {
				this.transpositionTable.put(depth, board.positionHash,
						beta, bestMove,
						TranspositionTable.TranspositionType.NODE_CUT);
				return beta;
			}
			if(fitness > alpha) {
				nodeType = TranspositionTable.TranspositionType.NODE_PV;
				bestMove = move;
				alpha = fitness;
			}
		}
		this.transpositionTable.put(depth, board.positionHash, alpha, bestMove,
				nodeType);
		return alpha;
	}
	
	public Move getMoveToDepth(Board board, int depth) {
		Move bestMove = null;
		float alpha = -FITNESS_LARGE;
		float beta = FITNESS_LARGE;
		for(Move move : board.legalMoves()) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			float fitness = -this.alphabeta(copy, depth - 1, -beta, -alpha);
			if(fitness > alpha || bestMove == null) {
				bestMove = move;
				alpha = fitness;
			}
		}
		return bestMove;
	}
	
	public ArrayList<Move> getPrincipalVariation(Board board, Move move) {
		ArrayList<Move> principalVariation = new ArrayList<Move>();
		principalVariation.add(move);
		Board copy = new Board(board);
		for(int d = 1; d < this.totalDepth; d++) {
			copy = new Board(copy);
			ArrayList<Move> legalMoves = copy.legalMoves();
			Move pvMove = principalVariation.get(principalVariation.size() - 1);
			boolean moveFound = false;
			for(Move legalMove : legalMoves) {
				if(legalMove.equals(pvMove)) {
					moveFound = true;
					break;
				}
			}
			if(!moveFound) {
				principalVariation.remove(principalVariation.size() - 1);
				break;
			}
			try {
				copy.move(pvMove);
			}
			catch(IllegalMoveException e) {
			}
			TranspositionTable.TranspositionEntry entry =
					this.transpositionTable.get(copy.positionHash);
			if(entry == null || entry.bestMove == null) {
				break;
			}
			principalVariation.add(entry.bestMove);
		}
		return principalVariation;
	}
	
	public Move getMove(Board board) {
		Move move = null;
		for(int d = 1; d <= this.totalDepth; d++) {
			move = this.getMoveToDepth(board, d);
		}
		return move;
	}
	
	public int totalDepth = 5;
	
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
	private float FITNESS_KING_RANK_FACTOR = 75;
	private float[] FITNESS_KING_FILE = {0, 0, -75, -150, -150, -75, 0, 0};
}
