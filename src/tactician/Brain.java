package tactician;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import chess_engine.Board;

/**
 * <p>This class is the engine that calculates the best move for a given board position. The
 * central method is {@link #getMove(Board)}. The two main concepts for making this calculation
 * are depth-first search through a portion of the game tree, and static evaluation.
 * 
 * <p>Theoretically any two-player game with perfect information has a winning strategy that can
 * be determined by scanning the game tree, including chess. In practice the game tree for chess
 * is far too large to scan with our current computational resources. Instead we search to a given
 * depth listed in {@link #totalDepth} and then perform a static evaluation on each board position.
 * Note that {@link #totalDepth} is the number of plies, so if we calculate white's move and then
 * black's move that counts as two plies. The search efficiency can be dramatically improved
 * through alpha-beta pruning, which prunes branches of the search tree that are known ahead of
 * time not to lead to the optimal move. See {@link #alphabeta(Board, int, float, float)} for more
 * details.
 * 
 * <p>The goal of static evaluation is to estimate how good a board position is for the player to
 * move. It is how we calculate the values of the leaf nodes in the alpha-beta search. For example
 * there is the classic rule of thumb that a queen is worth 9 pawns, a rook is worth 5, and a
 * bishop and knight are each worth 3. This way to count material provides a good estimate; we also
 * use factors such as pawn structure, king safety, and piece activity. As a general rule the
 * fitness evaluations are in centipawns, with a pawn being worth 100. See {@link #fitness(Board)}
 * for more details.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning">Alpha-Beta Pruning</a>
 * @author Phil Leszczynski
 */
public class Brain {
	/**
	 * Initializes a brain. Creates the transposition tables, sets the material weights for each
	 * piece, sets the material weight for the starting position, and sets several helper variables
	 * related to king safety.
	 */
	public Brain() {
		this.transpositionTable = new TranspositionTable(TRANSPOSITION_TABLE_SIZE);
		this.pawnKingHashTable = new PawnKingHashTable(PAWN_KING_TABLE_SIZE);
		
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
		
		this.pawnShieldQueenside = new HashMap<Color, Bitboard>();
		this.pawnShieldQueensideForward = new HashMap<Color, Bitboard>();
		this.pawnShieldKingside = new HashMap<Color, Bitboard>();
		this.pawnShieldKingsideForward = new HashMap<Color, Bitboard>();
		
		this.pawnShieldQueenside.put(Color.WHITE, new Bitboard("a2", "b2", "c2"));
		this.pawnShieldQueensideForward.put(Color.WHITE, new Bitboard("a3", "b3", "c3"));
		this.pawnShieldKingside.put(Color.WHITE, new Bitboard("f2", "g2", "h2"));
		this.pawnShieldKingsideForward.put(Color.WHITE, new Bitboard("f3", "g3", "h3"));
		
		this.pawnShieldQueenside.put(Color.BLACK,
			this.pawnShieldQueenside.get(Color.WHITE).flip());
		this.pawnShieldQueensideForward.put(Color.BLACK,
			this.pawnShieldQueensideForward.get(Color.WHITE).flip());
		this.pawnShieldKingside.put(Color.BLACK,
			this.pawnShieldKingside.get(Color.WHITE).flip());
		this.pawnShieldKingsideForward.put(Color.BLACK,
			this.pawnShieldKingsideForward.get(Color.WHITE).flip());
	}
	
	/**
	 * Returns the fraction of the opponent's material that has been removed. At the start of the
	 * game this returns 1.0f; if the opponent has just a king left this returns 0.0f.
	 * @param board the board to evaluate the endgame fraction
	 * @return the fraction of the way we are into the endgame, judging by the fraction of the
	 *         opponent's material that is still on the board
	 */
	public float endgameFraction(Board board) {
		float material = 0;
		for(Map.Entry<Piece, Float> entry : this.FITNESS_PIECE.entrySet()) {
			Piece piece = entry.getKey();
			if(piece == Piece.KING) {
				continue;
			}
			int pieceCount = board.bitboards.get(Color.flip(board.turn)).get(piece).numOccupied();
			material += pieceCount * this.FITNESS_PIECE.get(piece);
		}
		return 1 - material / this.FITNESS_START_NOKING;
	}
	
	/**
	 * Returns an evaluation of the given player's king safety in centipawns. As a general rule the
	 * king should be near one of the home corners early in the game because it is safer there. But
	 * if the opponent has little material remaining the king should be active and close to the
	 * center of the board. Additionally we have a bonus for an intact pawn shield in front of a
	 * castled king early on in the game as this hinders the opponent's attack.
	 * @param board the board to evaluate the king safety
	 * @param color the player whose king safety to evaluate
	 * @param endgameFraction the endgame fraction as calculated in
	 *        {@link #endgameFraction(Board)}. Performance is important here and we want to save
	 *        resources on the calculation when possible.
	 * @return the given player's king safety score in centipawns
	 */
	public float fitnessKingSafety(Board board, Color color, float endgameFraction) {
		int distanceFromHomeRank = 0;
		int kingIndex = board.bitboards.get(color).get(Piece.KING).numEmptyStartingSquares();
		if(color == Color.WHITE) {
			distanceFromHomeRank = (int)(kingIndex / 8);
		}
		else {
			distanceFromHomeRank = 7 - (int)(kingIndex / 8);
		}
		float rankFitness = -this.FITNESS_KING_RANK_FACTOR *
			distanceFromHomeRank * (0.6f - endgameFraction);
		float fileFitness = this.FITNESS_KING_FILE[kingIndex % 8] *
			(0.6f - endgameFraction);
		
		float openFilePenalty = 0;
		float pawnShieldPenalty = 0;
		if(endgameFraction < 0.7) {
			int protectorsHome = 3;
			int protectorsOneStep = 0;
			if(color == Color.WHITE) {
				if(kingIndex % 8 <= 2) {
					protectorsHome = board.bitboards.get(color)
						.get(Piece.PAWN)
						.intersection(pawnShieldQueenside.get(color))
						.numOccupied();
					protectorsOneStep = board.bitboards.get(color)
						.get(Piece.PAWN)
						.intersection(pawnShieldQueensideForward.get(color))
						.numOccupied();
				} else if(kingIndex % 8 >= 5) {
					protectorsHome = board.bitboards.get(color)
						.get(Piece.PAWN)
						.intersection(pawnShieldKingside.get(color))
						.numOccupied();
					protectorsOneStep = board.bitboards.get(color)
						.get(Piece.PAWN)
						.intersection(pawnShieldKingsideForward.get(color))
						.numOccupied();
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
				// Don't have an open file penalty before castling, as we may get opportunities to
				// capture pawns in the center.
				Bitboard file = Bitboard.bitboardFromFile(kingIndex % 8);
				if(!board.bitboards.get(color).get(Piece.PAWN).intersects(file)) {
					openFilePenalty = 150 * (1 - endgameFraction);
				}
			}
		}
		
		return rankFitness + fileFitness - pawnShieldPenalty - openFilePenalty;
	}
	
	/**
	 * Returns an evaluation of the given player's rook placement bonus on open files. As a rule of
	 * thumb a rook is more powerful on an open file containing no pawns as it controls a lot of
	 * squares. It is also powerful on a semi-open file containing only opposing pawns, as it
	 * applies pressure preventing the pawn(s) from advancing.
	 * @param board the board to evaluate rook placement on open files
	 * @param color the player whose rook placement to evaluate
	 * @param endgameFraction the endgame fraction as calculated in
	 *        {@link #endgameFraction(Board)}. Performance is important here and we want to save
	 *        resources on the calculation when possible.
	 * @return the given player's rook open file bonus in centipawns
	 */
	public float fitnessRookFiles(Board board, Color color, float endgameFraction) {
		// Assign a bonus for a rook being on an open file (one with no pawns)
		// or a semi-open file (one with only enemy pawns).
		float result = 0;
		long rooks = board.bitboards.get(color).get(Piece.ROOK).getData();
		long myPawns = board.bitboards.get(color).get(Piece.PAWN).getData();
		long oppPawns = board.bitboards.get(Color.flip(color)).get(Piece.PAWN).getData();
		while(rooks != 0) {
			int rookIndex = Long.numberOfTrailingZeros(rooks);
			long rook = 1L << rookIndex;
			rooks ^= rook;
			Bitboard rookFile = Bitboard.bitboardFromFile(rookIndex % 8);
			if(!rookFile.intersects(myPawns)) {
				if(!rookFile.intersects(oppPawns)) {
					result += this.FITNESS_ROOK_OPEN_FILE;
				} else {
					result += this.FITNESS_ROOK_SEMIOPEN_FILE;
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns an evaluation of the given player's bonus for retaining castling rights, as well as
	 * pawn shield integrity on the respective flanks. The right to castle is important early in
	 * the game as it puts the king in safety and helps activate the rook. We also take the pawn
	 * shield on the wings into account, since castling would not be as useful if the king is
	 * vulnerable to attack.
	 * @param board the board to evaluate castling rights
	 * @param color the player whose castling rights to evaluate
	 * @param endgameFraction the endgame fraction as calculated in
	 *        {@link #endgameFraction(Board)}. Performance is important here and we want to save
	 *        resources on the calculation when possible.
	 * @return the given player's castle rights bonus in centipawns
	 */
	public float fitnessCastleRights(Board board, Color color, float endgameFraction) {
		if(endgameFraction > 0.5) {
			return 0;
		}
		
		float result = 0;
		boolean castleRightQueenside = board.castleRights.get(color).get(Castle.QUEENSIDE);
		boolean castleRightKingside = board.castleRights.get(color).get(Castle.KINGSIDE);
		if(castleRightQueenside) {
			result += this.FITNESS_CASTLE_RIGHT_QUEENSIDE;
		}
		if(castleRightKingside) {
			result += this.FITNESS_CASTLE_RIGHT_KINGSIDE;
		}

		int numPawnsQueenside = board.bitboards.get(color).get(Piece.PAWN)
			.intersection(this.pawnShieldQueenside.get(color)).numOccupied();
		int numPawnsKingside = board.bitboards.get(color).get(Piece.PAWN)
			.intersection(this.pawnShieldKingside.get(color)).numOccupied();
		
		result -= 10 * (3 - numPawnsQueenside);
		result -= 25 * (3 - numPawnsKingside);
		
		result *= (1 - 2 * endgameFraction);
		
		return result;
	}
	
	/**
	 * <p>Returns the static evaluation fitness score for a given board. In general we evaluate
	 * various bonuses and penalties both for the side to move and the opponent. The result is the
	 * score from the player to move's perspective, i.e. the player's fitness minus the opponent's
	 * fitness.
	 * 
	 * <p>To compute the fitness we take the following into account: material on board, a bonus for
	 * the bishop pair, a penalty for doubled and isolated pawns, a bonus for passed pawns, a score
	 * for king safety, and a bonus for rook placement on open files.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Glossary_of_chess#Bishop_pair">Bishop Pair</a>
	 * @param board the board with which to perform a static evaluation
	 * @return the static evaluation fitness score for the board
	 */
	public float fitness(Board board) {
		float fitness = 0;
		Color turnFlipped = Color.flip(board.turn);
		for(Map.Entry<Piece, Float> entry : this.FITNESS_PIECE.entrySet()) {
			Piece piece = entry.getKey();
			if(piece == Piece.PAWN) {
				continue;
			}
			int myPieceCount = board.bitboards.get(board.turn).get(piece).numOccupied();
			int oppPieceCount = board.bitboards.get(turnFlipped).get(piece).numOccupied();
			fitness += (myPieceCount - oppPieceCount) * this.FITNESS_PIECE.get(piece);
			if(piece == Piece.BISHOP) {
				if(myPieceCount >= 2) {
					fitness += this.FITNESS_BISHOP_PAIR_BONUS;
				}
				if(oppPieceCount >= 2) {
					fitness -= this.FITNESS_BISHOP_PAIR_BONUS;
				}
			}
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
		
		PawnKingHashTable.PawnHashTableEntry entry =
			this.pawnKingHashTable.get(board.positionHashPawnsKings);
		if(entry == null) {
			this.pawnKingHashTable.put(
				board.positionHashPawnsKings,
				board.bitboards.get(Color.WHITE).get(Piece.PAWN).getData(),
				board.bitboards.get(Color.BLACK).get(Piece.PAWN).getData(),
				board.bitboards.get(Color.WHITE).get(Piece.KING).numEmptyStartingSquares(),
				board.bitboards.get(Color.BLACK).get(Piece.KING).numEmptyStartingSquares()
			);
			entry = this.pawnKingHashTable.get(board.positionHashPawnsKings);
		}
		
		float doubledPawnPenalty = 15 * (entry.numDoubledPawnsWhite -
			entry.numDoubledPawnsBlack);
		float isolatedPawnPenalty = 15 * (entry.numIsolatedPawnsWhite -
			entry.numIsolatedPawnsBlack);
		float passedPawnBonus = 30 * (entry.numPassedPawnsWhite -
			entry.numPassedPawnsBlack);
		if(board.turn == Color.BLACK) {
			doubledPawnPenalty *= -1;
			isolatedPawnPenalty *= -1;
			passedPawnBonus *= -1;
		}
		fitness -= doubledPawnPenalty;
		fitness -= isolatedPawnPenalty;
		fitness += passedPawnBonus;
		
		// Since pawns can't be on the edge ranks.
		for(int rank = 1; rank < 7; rank++) {
			Bitboard rankBitboard = Bitboard.bitboardFromRank(rank);
			for(int centrality = 0; centrality < 4; centrality++) {
				Bitboard centralityBitboard =
					Bitboard.bitboardFromFile(centrality).union(
						Bitboard.bitboardFromFile(8 - centrality));
				float pawnFactor = (1 - endgameFraction) *
					this.FITNESS_PAWN_TABLE_OPENING[rank][centrality];
				pawnFactor += endgameFraction *
					this.FITNESS_PAWN_TABLE_ENDGAME[rank][centrality];
				
				int myPawnsOnRank = pawnBitboardRelativeToMe
					.intersection(rankBitboard)
					.intersection(centralityBitboard)
					.numOccupied();
				int oppPawnsOnRank = pawnBitboardRelativeToOpp
					.intersection(rankBitboard)
					.intersection(centralityBitboard)
					.numOccupied();
				
				fitness += pawnFactor * (myPawnsOnRank - oppPawnsOnRank);
			}
		}
		
		fitness += this.fitnessKingSafety(board, board.turn, endgameFraction) -
			this.fitnessKingSafety(board, turnFlipped, endgameFraction);
		fitness += this.fitnessRookFiles(board, board.turn, endgameFraction) -
			this.fitnessRookFiles(board, turnFlipped, endgameFraction);
		fitness += this.fitnessCastleRights(board, board.turn, endgameFraction) -
			this.fitnessCastleRights(board, turnFlipped, endgameFraction);
						
		return fitness;
	}
	
	/**
	 * Evaluates the position at an alpha-beta leaf node. Instead of immediately doing a static
	 * evaluation through the {@link #fitness(Board)} method, we first exhaust the possibility of
	 * any immediate captures. This is due to the horizon effect where we can otherwise get
	 * undesirable behavior. For example we want to avoid a queen capturing a guarded pawn at the
	 * leaf node and thinking it's a good move since the recapture of the queen is just outside the
	 * search depth. Note this implementation is different from the standard because it only
	 * considers recaptures on the same destination square.
	 * @see <a href="http://chessprogramming.wikispaces.com/Quiescence+Search">
	 *      Quiescence Search</a>
	 * @param board the board with which to perform the quiescent search
	 * @param alpha the running alpha score tracked by {@link #alphabeta(Board, int, float, float)}
	 * @param beta the running beta score tracked by {@link #alphabeta(Board, int, float, float)}
	 * @param target the 64-bit long mask containing the capture target if any, see
	 *        {@link Bitboard} for the 64-bit mask implementation
	 * @return the quiescent search score subject to the standard alpha-beta pruning negamax
	 *         implementation, see {@link #alphabeta(Board, int, float, float)} for more details
	 */
	private float quiescentSearch(Board board, float alpha, float beta, long target) {
		float fitness = this.fitness(board);
		if(fitness >= beta) {
			return beta;
		}
		if(fitness > alpha) {
			alpha = fitness;
		}
		// The player whose king gets captured first loses, even if the other
		// king gets captured next turn.
		if(board.bitboards.get(board.turn).get(Piece.KING).isEmpty()) {
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
			copy.move(move);
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
	
	/**
	 * <p>Performs a recursive alpha-beta depth-first search to a given depth. Runs a quiescent
	 * search through {@link #quiescentSearch(Board, float, float, long)} at the leaf nodes.
	 * 
	 * <p>Alpha-beta pruning is a modified depth-first search that prunes branches that are certain
	 * to not lead to the optimal move. To paraphrase the Chess Programming article below, consider
	 * a depth-first search of two where the first player has two legal moves M1 and M2. After
	 * scanning the leaf nodes below M1, it is determined to lead to an even position. Now for M2
	 * the opponent can respond with N1, N2, or N3. Suppose that N1 captures the first player's
	 * rook. Then we know the first player will play M1 instead of M2 because M2 is known to lead
	 * to a less desirable outcome. It doesn't matter whether say N2 captures the first player's
	 * queen, as we already know that M2 will not be played. So we do not need to evaluate N2 nor
	 * N3.
	 * 
	 * <p>In the example above the "approximately even" score is tracked by the variable alpha, and
	 * it would be set to around 0. It is updated when a better move for the current player is
	 * found. We also track beta which is the negative of the opponent's alpha. So in a sense if we
	 * find a move with a score higher than beta it is "too good to be true" and the opponent would
	 * not allow it, so we can safely prune the parent move. We use the negamax framework here,
	 * where roughly speaking each player's beta is the negative of the opponent's alpha. The
	 * Negamax article below outlines the process more fully.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning">
	 *      Alpha-Beta Pruning</a>
	 * @see <a href="http://chessprogramming.wikispaces.com/Alpha-Beta">Alpha-Beta</a>
	 * @see <a href="https://en.wikipedia.org/wiki/Negamax">Negamax</a>
	 * @param board the board for which to perform the search
	 * @param depth the depth in plies for which to search
	 * @param alpha the score of the best move found for the current player
	 * @param beta the highest score the opponent would allow, or the negative of the opponent's
	 *        alpha
	 * @return the evaluation of the board position to the given depth in centipawns
	 */
	public float alphabeta(Board board, int depth, float alpha, float beta) {
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
					return board.fullMoveCounter * this.FITNESS_MOVE - this.FITNESS_LARGE;
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
			copy.move(move);
			float fitness = -this.alphabeta(copy, depth - 1, -beta, -alpha);
			if(fitness >= beta) {
				this.transpositionTable.put(depth, board.positionHash, beta, bestMove,
					TranspositionTable.TranspositionType.NODE_CUT);
				return beta;
			}
			if(fitness > alpha) {
				nodeType = TranspositionTable.TranspositionType.NODE_PV;
				bestMove = move;
				alpha = fitness;
			}
		}
		this.transpositionTable.put(depth, board.positionHash, alpha, bestMove, nodeType);
		return alpha;
	}
	
	/**
	 * Determines the best move to play on a board to a given depth using
	 * {@link #alphabeta(Board, int, float, float)}. Note that this does not include iterative
	 * deepening, so in a real game {@link #getMove(Board)} should be used instead.
	 * @param board the board to analyze
	 * @param depth the search depth in plies
	 * @return the best move to play on the given board
	 */
	public Move getMoveToDepth(Board board, int depth) {
		Move bestMove = null;
		float alpha = -FITNESS_LARGE;
		float beta = FITNESS_LARGE;
		for(Move move : board.legalMoves()) {
			Board copy = new Board(board);
			copy.move(move);
			float fitness = -this.alphabeta(copy, depth - 1, -beta, -alpha);
			if(fitness > alpha || bestMove == null) {
				bestMove = move;
				alpha = fitness;
			}
		}
		return bestMove;
	}
	
	/**
	 * Returns the principal variation stemming from a given board and move, as determined by the
	 * transposition table. In other words it is the engine's best guess for the next player's move
	 * followed by the original player's move after that, and so on. This can be helpful for
	 * debugging; if the engine makes an unusual move, then seeing the principal variation can help
	 * us understand its reasoning. Note that we try to get the principal variation to
	 * {@link #totalDepth} moves but this is not always possible. For example the transposition
	 * table entry in the middle may have been overridden due to a hash collision and in that case
	 * we cannot look at the fully calculated principal variation.
	 * @param board the board for which to calculate the principal variation
	 * @param move the starting move for which to calculate the principal variation
	 * @return an ArrayList of moves listing the principal variation after the given move
	 */
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
			copy.move(pvMove);
			TranspositionTable.TranspositionEntry entry =
				this.transpositionTable.get(copy.positionHash);
			if(entry == null || entry.bestMove == null) {
				break;
			}
			principalVariation.add(entry.bestMove);
		}
		return principalVariation;
	}
	
	/**
	 * Determines the best move for a given board. This is done through iterative deepening to a
	 * depth of {@link #totalDepth} plies. It may seem counter-intuitive to perform the search for
	 * all depths up to {@link #totalDepth} and throw out all results except the last. But research
	 * has shown this process is actually faster because it sets entries in the transposition table
	 * that help with move ordering.
	 * @see <a href="https://en.wikipedia.org/wiki/Iterative_deepening_depth-first_search">
	 *      Iterative Deepening</a>
	 * @param board the board for which to get the best move
	 * @return the best move to play according to the engine
	 */
	public Move getMove(Board board) {
		Move move = null;
		for(int d = 1; d <= this.totalDepth; d++) {
			move = this.getMoveToDepth(board, d);
		}
		return move;
	}
	
	private int totalDepth = 5;
	
	private TranspositionTable transpositionTable = null;
	private PawnKingHashTable pawnKingHashTable = null;
	
	private float FITNESS_LARGE = 1000000000;
	// To checkmate as quickly as possible, put in a penalty for waiting.
	private float FITNESS_MOVE = 10000;
	private Map<Piece, Float> FITNESS_PIECE = new HashMap<Piece, Float>();
	private float FITNESS_START_NOKING = 0;
	
	private float FITNESS_ROOK_OPEN_FILE = 50;
	private float FITNESS_ROOK_SEMIOPEN_FILE = 25;
	
	private float FITNESS_CASTLE_RIGHT_QUEENSIDE = 15;
	private float FITNESS_CASTLE_RIGHT_KINGSIDE = 30;
	
	private float FITNESS_BISHOP_PAIR_BONUS = 50;
	
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
	private float[] FITNESS_KING_FILE = {0, 0, -90, -180, -180, -90, 0, 0};
	
	private static int TRANSPOSITION_TABLE_SIZE = 32 * 1024 * 1024;
	private static int PAWN_KING_TABLE_SIZE = 64 * 1024;
	
	private Map<Color, Bitboard> pawnShieldQueenside;
	private Map<Color, Bitboard> pawnShieldQueensideForward;
	private Map<Color, Bitboard> pawnShieldKingside;
	private Map<Color, Bitboard> pawnShieldKingsideForward;
}
