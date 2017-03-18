package tactician;

/**
 * This class represents one of the 64 squares on the chessboard. A chessboard has 8 ranks, or
 * horizontal rows from each player's perspective, going from 1 to 8. Rank 1 is closest to the
 * white player and rank 8 is closest to the black player. The board also has 8 files, or vertical
 * columns from the players' perspectives, going from a to h. The a-file is to the white player's
 * left and the black player's right. Squares are named with the file followed by rank, for example
 * e7.
 * 
 * <p>A key concept here is the indexing scheme where we represent a square on the board as an
 * integer from 0-63. The a1, b1, ..., h1 squares have indices 0-7 respectively. The a2, b2, ...,
 * h2 squares have indices 8-15 respectively. And so on until the h8 square has index 63.
 * {@link #index} uses this scheme.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Chessboard">Chessboard</a>
 * @author Phil Leszczynski
 */
public class Square {
  /**
   * Initializes a square based on its two-character name. The name must be the file, lowercase a-h,
   * followed by the rank, 1-8.
   * 
   * @param name the name of the square, e.g. "g6"
   */
  public Square(String name) {
    int file = Integer.parseInt("" + (name.charAt(0) - 96));
    int rank = Integer.parseInt(name.substring(1, 2));
    this.index = (file - 1) + 8 * (rank - 1);
  }

  /**
   * Initializes a square based on its index, 0-63. See the class definition for more details.
   * 
   * @param index the index of the square, 0-63
   */
  public Square(int index) {
    this.index = index;
  }

  /**
   * Returns the index of the square.
   * 
   * @return the index of the square, 0-63
   */
  public int getIndex() {
    return this.index;
  }

  /**
   * Returns the 64-bit long mask corresponding to the {@link Bitboard} with only the current square
   * occupied.
   * 
   * @return the 64-bit long mask corresponding to the square
   */
  public long getMask() {
    return 1L << this.getIndex();
  }

  /**
   * Returns the name of the file corresponding to the square, 'a'-'h'. See the class definition for
   * more details.
   * 
   * @return the name of the file the square is on
   */
  public char getFile() {
    return (char) (this.index % 8 + 97);
  }

  /**
   * Returns the name of the rank corresponding to the square, '1'-'8'. See the class definition for
   * more details.
   * 
   * @return the name of the rank the square is on
   */
  public char getRank() {
    return Integer.toString((this.index / 8) + 1).charAt(0);
  }

  /**
   * Returns the two-character name of the square, or the file and rank. For example "d2".
   * 
   * @return the name of the square
   */
  public String getName() {
    return "" + this.getFile() + this.getRank();
  }

  /** The index of the square, 0-63. */
  private int index;
}
