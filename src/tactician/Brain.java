package tactician;

import java.util.ArrayList;

import tactician.Board;

/**
 * This class is the engine that calculates the best move for a given board position. The central
 * method is {@link #getMove(Board)}. It does so by performing a depth-first search through a
 * portion of the game tree.
 * 
 * <p>Theoretically any two-player game with perfect information has a winning strategy that can be
 * determined by scanning the game tree, including chess. In practice the game tree for chess is far
 * too large to scan with our current computational resources. Instead we search to a given depth
 * listed in {@link #totalDepth} and then perform a static evaluation on each board position. Note
 * that {@link #totalDepth} is the number of plies, so if we calculate white's move and then black's
 * move that counts as two plies. The search efficiency can be dramatically improved through
 * alpha-beta pruning, which prunes branches of the search tree that are known ahead of time not to
 * lead to the optimal move. See {@link #alphabeta(Board, int, float, float)} for more details.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning">Alpha-Beta Pruning</a>
 * @author Phil Leszczynski
 */
public class Brain {
  /**
   * Initializes a brain by setting up the transposition table.
   */
  public Brain() {
    this.killerMoves = new Move[totalDepth + 1][2];
    this.transpositionTable = new TranspositionTable(transpositionTableSize);
  }
  
  /**
   * Unsets the array of killer moves indexed by depth. A killer move is one that produced a beta
   * cutoff in a previous branch of the game tree at the same depth.
   */
  private void unsetKillerMoves() {
    for(int i = 0; i < this.killerMoves.length; i++) {
      for(int j = 0; j < 2; j++) {
        this.killerMoves[i][j] = null;
      }
    }
  }
  
  /**
   * Inserts a killer move at the given depth and removes a previous killer move if necessary.
   * @param move the new killer move
   * @param depth the depth at which to store the killer move
   */
  private void insertKillerMove(Move move, int depth) {
    if(this.isKillerMove(move, depth)) {
      return;
    }
    this.killerMoves[depth][0] = this.killerMoves[depth][1];
    this.killerMoves[depth][1] = move;
  }
  
  /**
   * Checks whether the given move is a killer move for the given depth.
   * @param move the move to test
   * @param depth the depth at which to check for killer moves
   * @return true if the move is in the killer move list, false otherwise
   */
  private boolean isKillerMove(Move move, int depth) {
    return (move.equals(this.killerMoves[depth][0]) || move.equals(this.killerMoves[depth][1]));
  }
  
  /**
   * Given an ArrayList of fast legal moves, sorts them in the most beneficial way for the
   * alpha-beta search. As a general rule moves that are more likely to be optimal should be
   * searched first, as well as moves that are risky.
   * @param legalMovesFast the list of legal moves to sort
   * @param board the current board position
   * @param depth the current search depth
   * @param lastBestMove the move found in the transposition table at the current position, or null
   * @return a sorted ArrayList of the same fast legal moves
   */
  private ArrayList<Move> sortLegalMovesFast(ArrayList<Move> legalMovesFast, Board board,
      int depth, Move lastBestMove) {
    ArrayList<Move> transpositionTableMoves = new ArrayList<Move>();
    ArrayList<Move> captureMoves = new ArrayList<Move>();
    ArrayList<Move> killerMoves = new ArrayList<Move>();
    ArrayList<Move> noncaptureMoves = new ArrayList<Move>();
    for (Move move : legalMovesFast) {
      if (move.equals(lastBestMove)) {
        transpositionTableMoves.add(move);
      } else if (board.isCapture(move)) {
        captureMoves.add(move);
      } else if (this.isKillerMove(move, depth)) {
        killerMoves.add(move);
      } else {
        noncaptureMoves.add(move);
      }
    }
    ArrayList<Move> result = new ArrayList<Move>();
    result.addAll(transpositionTableMoves);
    result.addAll(captureMoves);
    result.addAll(killerMoves);
    result.addAll(noncaptureMoves);
    return result;
  }

  /**
   * Evaluates the position at an alpha-beta leaf node. Instead of immediately doing a static
   * evaluation through the {@link #fitness(Board)} method, we first exhaust the possibility of any
   * immediate captures. This is due to the horizon effect where we can otherwise get undesirable
   * behavior. For example we want to avoid a queen capturing a guarded pawn at the leaf node and
   * thinking it's a good move since the recapture of the queen is just outside the search depth.
   * Note this implementation is different from the standard because it only considers recaptures on
   * the same destination square.
   * 
   * @see <a href="http://chessprogramming.wikispaces.com/Quiescence+Search"> Quiescence Search</a>
   * @param board the board with which to perform the quiescent search
   * @param alpha the running alpha score tracked by {@link #alphabeta(Board, int, float, float)}
   * @param beta the running beta score tracked by {@link #alphabeta(Board, int, float, float)}
   * @param target the 64-bit long mask containing the capture target if any, see {@link Bitboard}
   *        for the 64-bit mask implementation
   * @return the quiescent search score subject to the standard alpha-beta pruning negamax
   *         implementation, see {@link #alphabeta(Board, int, float, float)} for more details
   */
  private float quiescentSearch(Board board, float alpha, float beta, long target) {
    float fitness = evaluation.fitness(board);
    if (fitness >= beta) {
      return beta;
    }
    if (fitness > alpha) {
      alpha = fitness;
    }
    // The player whose king gets captured first loses, even if the other king gets captured next
    // turn. Reduce the benefit by each move of the game to incentivize checkmate as quickly as
    // possible.
    if (board.bitboards.get(board.turn).get(Piece.KING).isEmpty()) {
      return -Evaluation.FITNESS_LARGE + board.fullMoveCounter * Evaluation.FITNESS_MOVE;
    }
    ArrayList<Move> lmf = this.sortLegalMovesFast(board.legalMovesFast(true), board, 0, null);
    for (Move move : lmf) {
      long moveTarget = 1L << move.destination;
      if (target != -1 && moveTarget != target) {
        // Only probe captures happening on the same square.
        continue;
      }
      Board copy = new Board(board);
      copy.move(move);
      fitness = -this.quiescentSearch(copy, -beta, -alpha, moveTarget);
      if (fitness >= beta) {
        return beta;
      }
      if (fitness > alpha) {
        alpha = fitness;
      }
    }
    return alpha;
  }

  /**
   * Performs a recursive alpha-beta depth-first search to a given depth. Runs a quiescent search
   * through {@link #quiescentSearch(Board, float, float, long)} at the leaf nodes.
   * 
   * <p>Alpha-beta pruning is a modified depth-first search that prunes branches that are certain
   * to not lead to the optimal move. To paraphrase the Chess Programming article below, consider a
   * depth-first search of two where the first player has two legal moves M1 and M2. After scanning
   * the leaf nodes below M1, it is determined to lead to an even position. Now for M2 the opponent
   * can respond with N1, N2, or N3. Suppose that N1 captures the first player's rook. Then we know
   * the first player will play M1 instead of M2 because M2 is known to lead to a less desirable
   * outcome. It doesn't matter whether say N2 captures the first player's queen, as we already
   * know that M2 will not be played. So we do not need to evaluate N2 nor N3.
   * 
   * <p>In the example above the "approximately even" score is tracked by the variable alpha, and
   * it would be set to around 0. It is updated when a better move for the current player is found.
   * We also track beta which is the negative of the opponent's alpha. So in a sense if we find a
   * move with a score higher than beta it is "too good to be true" and the opponent would not
   * allow it, so we can safely prune the parent move. We use the negamax framework here, where
   * roughly speaking each player's beta is the negative of the opponent's alpha. The Negamax
   * article below outlines the process more fully.
   * 
   * @see <a href="https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning"> Alpha-Beta Pruning</a>
   * @see <a href="http://chessprogramming.wikispaces.com/Alpha-Beta">Alpha-Beta</a>
   * @see <a href="https://en.wikipedia.org/wiki/Negamax">Negamax</a>
   * @param board the board for which to perform the search
   * @param depth the depth in plies for which to search
   * @param alpha the score of the best move found for the current player
   * @param beta the highest score the opponent would allow, or the negative of the opponent's alpha
   * @return the evaluation of the board position to the given depth in centipawns
   */
  public float alphabeta(Board board, int depth, float alpha, float beta) {
    if (depth == 0) {
      return this.quiescentSearch(board, alpha, beta, -1);
    }
    TranspositionTable.TranspositionEntry entry = this.transpositionTable.get(board.positionHash);
    Move lastBestMove = null;
    if (entry != null) {
      if (entry.depth == depth) {
        if (entry.type == TranspositionTable.TranspositionType.NODE_PV) {
          return entry.fitness;
        } else if (entry.type == TranspositionTable.TranspositionType.NODE_CUT) {
          beta = entry.fitness;
        } else if (entry.type == TranspositionTable.TranspositionType.NODE_ALL) {
          alpha = entry.fitness;
        }
        if (alpha >= beta) {
          return entry.fitness;
        }
      }
      lastBestMove = entry.bestMove;
    }
    ArrayList<Move> lmf = this.sortLegalMovesFast(board.legalMovesFast(false), board, depth,
        lastBestMove);
    // Special case where the king is captured and there are no pieces remaining for the side to
    // move. We still want to discount the fitness by how many moves it took to get there.
    if(lmf.size() == 0) {
      return -Evaluation.FITNESS_LARGE + board.fullMoveCounter * Evaluation.FITNESS_MOVE;
    }
    TranspositionTable.TranspositionType nodeType = TranspositionTable.TranspositionType.NODE_ALL;
    Move bestMove = null;
    for (Move move : lmf) {
      Board copy = new Board(board);
      copy.move(move);
      float fitness = -this.alphabeta(copy, depth - 1, -beta, -alpha);
      if (fitness >= beta) {
        this.transpositionTable.put(depth, board.positionHash, beta, bestMove,
            TranspositionTable.TranspositionType.NODE_CUT);
        this.insertKillerMove(move, depth);
        return beta;
      }
      if (fitness > alpha) {
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
   * 
   * @param board the board to analyze
   * @param depth the search depth in plies
   * @return the best move to play on the given board
   */
  public Move getMoveToDepth(Board board, int depth) {
    Move bestMove = null;
    float alpha = -Evaluation.FITNESS_LARGE;
    float beta = Evaluation.FITNESS_LARGE;
    this.unsetKillerMoves();
    for (Move move : board.legalMoves()) {
      Board copy = new Board(board);
      copy.move(move);
      float fitness = -this.alphabeta(copy, depth - 1, -beta, -alpha);
      if (fitness > alpha || bestMove == null) {
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
   * debugging; if the engine makes an unusual move, then seeing the principal variation can help us
   * understand its reasoning. Note that we try to get the principal variation to
   * {@link #totalDepth} moves but this is not always possible. For example the transposition table
   * entry in the middle may have been overridden due to a hash collision and in that case we cannot
   * look at the fully calculated principal variation.
   * 
   * @param board the board for which to calculate the principal variation
   * @param move the starting move for which to calculate the principal variation
   * @return an ArrayList of moves listing the principal variation after the given move
   */
  public ArrayList<Move> getPrincipalVariation(Board board, Move move) {
    ArrayList<Move> principalVariation = new ArrayList<Move>();
    principalVariation.add(move);
    Board copy = new Board(board);
    for (int d = 1; d < this.totalDepth; d++) {
      copy = new Board(copy);
      ArrayList<Move> legalMoves = copy.legalMoves();
      Move pvMove = principalVariation.get(principalVariation.size() - 1);
      boolean moveFound = false;
      for (Move legalMove : legalMoves) {
        if (legalMove.equals(pvMove)) {
          moveFound = true;
          break;
        }
      }
      if (!moveFound) {
        principalVariation.remove(principalVariation.size() - 1);
        break;
      }
      copy.move(pvMove);
      TranspositionTable.TranspositionEntry entry = this.transpositionTable.get(copy.positionHash);
      if (entry == null || entry.bestMove == null) {
        break;
      }
      principalVariation.add(entry.bestMove);
    }
    return principalVariation;
  }

  /**
   * Determines the best move for a given board. This is done through iterative deepening to a depth
   * of {@link #totalDepth} plies. It may seem counter-intuitive to perform the search for all
   * depths up to {@link #totalDepth} and throw out all results except the last. But research has
   * shown this process is actually faster because it sets entries in the transposition table that
   * help with move ordering.
   * 
   * @see <a href="https://en.wikipedia.org/wiki/Iterative_deepening_depth-first_search"> Iterative
   *      Deepening</a>
   * @param board the board for which to get the best move
   * @return the best move to play according to the engine
   */
  public Move getMove(Board board) {
    Move move = null;
    for (int d = 1; d <= this.totalDepth; d++) {
      move = this.getMoveToDepth(board, d);
    }
    return move;
  }

  private int totalDepth = 6;
  private Evaluation evaluation = new Evaluation();
  private Move[][] killerMoves = null;
  private TranspositionTable transpositionTable = null;
  private static int transpositionTableSize = 32 * 1024 * 1024;
}
