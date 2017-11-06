package tactician;

/**
 * This class is a transposition table that memoizes positions already visited. This way if we
 * encounter the same position through a different series of moves through the depth-first search,
 * we can retrieve the results of the calculations from the previous encounter. A key concept is
 * the position hash, a Zobrist hash containing the information about pawns and kings for each
 * player. See {@link PositionHasher} for more details.
 * 
 * <p>Memory efficiency is important for the main transposition table, so we pack transposition
 * entries as a pair of 64-bit long values in {@link #data} rather than storing
 * {@link TranspositionEntry} objects directly. The first 64-bit long contains the position hash.
 * The second 64-bit long uses its high 32 bits to store the fitness or node score in centipawns as
 * a float. The next highest 8 bits store the best move's source square, as it ranges from 0-63.
 * The next highest 8 bits store the best move's destination square, also 0-63. The next highest 4
 * bits store the promotion piece (1 if bishop, rook, knight, queen respectively); the next highest
 * 4 bits store the node type (1 for not used, All, Cut, PV respectively). Finally the lowest 8
 * bits store the search depth, or the distance to the leaf nodes, as a byte.
 * 
 * @author Phil Leszczynski
 */
public class TranspositionTable {
  /**
   * This enum lists the three node types that can be classified through an alpha-beta search. See
   * {@link Brain#alphabeta(Board, int, float, float)} for more details about our use of alpha-beta
   * pruning. PV, or principal variation, nodes are the ones where all moves had a score between
   * alpha and beta. They are also used to determine the sequence of best moves for each player.
   * Cut nodes are the ones where a child move scored higher than beta, or the child move is so
   * good that the opponent would not allow the parent node to be played. All nodes are the ones
   * where all child moves were searched but none exceeded alpha, or in other words none of the
   * moves were good enough to affect play at the root node.
   * 
   * @see <a href="http://chessprogramming.wikispaces.com/Node+Types">Node Types</a>
   * @author Phil Leszczynski
   */
  public enum TranspositionType {
    NODE_PV, NODE_CUT, NODE_ALL
  }

  /**
   * This class contains entries in the transposition table. It stores the depth of the alpha-beta
   * search, the position hash, the fitness or score of the move from the player's perspective at
   * that node, the best move for the player at that position, and the type of node.
   * 
   * @author Phil Leszczynski
   */
  public class TranspositionEntry {
    /**
     * Initializes a transposition entry.
     * 
     * @param depth the depth of the alpha-beta search remaining, or the number of steps away from
     *        a leaf node
     * @param positionHash the Zobrist hash of the position
     * @param fitness the score at the node from the moving player's perspective in centipawns
     * @param bestMove the best move for the player at the node
     * @param type the type of node: PV, Cut, or All
     */
    public TranspositionEntry(int depth, long positionHash, float fitness, Move bestMove,
        TranspositionType type) {
      this.depth = depth;
      this.positionHash = positionHash;
      this.fitness = fitness;
      this.bestMove = bestMove;
      this.type = type;
    }

    /** Returns a string summarizing the transposition entry. */
    @Override
    public String toString() {
      String result = "Depth=" + this.depth + ", ";
      result += "Fitness=" + this.fitness + ", ";
      result += "BestMove=" + this.bestMove + ", ";
      result += "Type=" + this.type;
      return result;
    }

    /**
     * The depth of the alpha-beta search remaining, or the number of moves away from a leaf node.
     */
    public int depth;

    /** The Zobrist hash of the board position. */
    public long positionHash;

    /** The score of the node from the moving player's perspective in centipawns. */
    public float fitness;

    /** The type of node: PV, Cut, or All. */
    public TranspositionType type;

    /** The best move found that can be played from the node. */
    public Move bestMove;
  }

  /**
   * Initializes a transposition table. Note that each transposition entry is packed into a pair of
   * 64-bit long values. See the class definition for how the packing is done.
   * 
   * @param size the number of entries in the transposition table (Note: NOT bytes, though bytes
   *        are often quoted when referring to a chess engine's transposition table.)
   */
  public TranspositionTable(int size) {
    this.size = size;
    this.data = new long[2 * this.size];
  }

  /**
   * Inserts a record into the transposition hash table. See the class definition for how the
   * packing is done from transposition entries to pairs of 64-bit longs.
   * 
   * @param depth the depth of the alpha-beta search remaining, or the number of steps away from a
   *        leaf node
   * @param positionHash the Zobrist hash of the position
   * @param fitness the score at the node from the moving player's perspective in centipawns
   * @param bestMove the best move for the player at the node
   * @param type the type of node: PV, Cut, or All
   */
  public void put(int depth, long positionHash, float fitness, Move bestMove,
      TranspositionType type) {
    long contents = 0;
    contents |= ((long) Float.floatToIntBits(fitness)) << 32;
    if (bestMove != null) {
      contents |= (long) (bestMove.source << 24);
      contents |= (long) (bestMove.destination << 16);
      if (bestMove.promoteTo == Piece.QUEEN) {
        contents |= 0x0000000000001000L;
      } else if (bestMove.promoteTo == Piece.KNIGHT) {
        contents |= 0x0000000000002000L;
      } else if (bestMove.promoteTo == Piece.ROOK) {
        contents |= 0x0000000000004000L;
      } else if (bestMove.promoteTo == Piece.BISHOP) {
        contents |= 0x0000000000008000L;
      }
    }
    if (type == TranspositionType.NODE_PV) {
      contents |= 0x0000000000000100L;
    } else if (type == TranspositionType.NODE_CUT) {
      contents |= 0x0000000000000200L;
    } else if (type == TranspositionType.NODE_ALL) {
      contents |= 0x0000000000000400L;
    }
    contents |= (long) (byte) depth;
    int index = this.index(positionHash);
    this.data[index] = positionHash;
    this.data[index + 1] = contents;
  }

  /**
   * Retrieves a record from the transposition hash table, unpacks it, and returns it as a
   * {@link TranspositionEntry}. See the class definition for how the packing is done from
   * transposition entries to pairs of 64-bit longs.
   * 
   * @param positionHash the Zobrist hash of the position
   * @return the transposition entry corresponding to the position hash in the table, or null if
   *         none was found
   */
  public TranspositionEntry get(long positionHash) {
    int index = this.index(positionHash);
    long positionHashFound = this.data[index];
    if (positionHash != positionHashFound) {
      return null;
    }
    long contents = this.data[index + 1];
    float fitness = Float.intBitsToFloat((int) (contents >>> 32));
    Move bestMove = null;
    if ((contents & 0x0000000011111000) != 0) {
      bestMove = new Move((int) (byte) (contents >>> 24), (int) (byte) (contents >>> 16));
      if ((contents & 0x0000000000001000L) != 0) {
        bestMove.promoteTo = Piece.QUEEN;
      } else if ((contents & 0x0000000000002000L) != 0) {
        bestMove.promoteTo = Piece.KNIGHT;
      } else if ((contents & 0x0000000000004000L) != 0) {
        bestMove.promoteTo = Piece.ROOK;
      } else if ((contents & 0x0000000000008000L) != 0) {
        bestMove.promoteTo = Piece.BISHOP;
      }
    }
    TranspositionType type = null;
    if ((contents & 0x0000000000000100L) != 0) {
      type = TranspositionType.NODE_PV;
    } else if ((contents & 0x0000000000000200L) != 0) {
      type = TranspositionType.NODE_CUT;
    } else if ((contents & 0x0000000000000400L) != 0) {
      type = TranspositionType.NODE_ALL;
    }
    int depth = (int) (byte) (contents);
    return new TranspositionEntry(depth, positionHash, fitness, bestMove, type);
  }

  /**
   * Returns the array index where the position hash is found in {@link #data}.
   * 
   * @param positionHash the Zobrist hash describing the position
   * @return the array index where the position hash is found
   */
  private int index(long positionHash) {
    // Unset the integer sign bit; the modulus is 2 * this.size - 1 because we're storing the
    // transposition entry in two longs. The -1 is because it's the index of the first datapoint.
    return ((int) positionHash & 0x7fffffff) % (2 * this.size - 1);
  }

  /** The size of the transposition table in number of entries (NOT bytes). */
  private int size;

  /**
   * The array containing the hash table. Note its length is twice {@link #size} since each entry
   * contains a pair of 64-bit long values. See the class definition for more details about how
   * transposition entries are packed here.
   */
  private long[] data;
}
