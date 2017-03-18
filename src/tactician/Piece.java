package tactician;

/**
 * This enum lists the six types of pieces available in the game of chess.
 * 
 * @author Phil Leszczynski
 */
public enum Piece {
  BISHOP, KING, KNIGHT, PAWN, QUEEN, ROOK;

  /**
   * Returns the initial of the piece that is used in algebraic notation and throughout chess
   * literature. It is simply the capital first letter of the name of the piece, except the knight
   * has initial 'N' so that it is not confused with the king.
   * 
   * @return the initial of the piece
   */
  public char initial() {
    if (this == Piece.KNIGHT) {
      return 'N';
    }
    return this.name().charAt(0);
  }

  /**
   * Given an initial, returns the type of piece it corresponds to. See {@link #initial()} for more
   * details.
   * 
   * @param initial the initial of the piece, accepts uppercase or lowercase
   * @return the type of piece corresponding to the initial
   */
  public static Piece initialToPiece(char initial) {
    initial = Character.toUpperCase(initial);
    for (Piece piece : Piece.values()) {
      if (piece.initial() == initial) {
        return piece;
      }
    }
    return null;
  }
}
