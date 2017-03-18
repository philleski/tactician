package tactician;

import java.util.ArrayList;

/**
 * <p>
 * This class checks whether castling is allowed for a given side and direction. This is determined
 * through the {@link #opponentPiecePrecludesCastling(long, long)} method.
 * 
 * <p>
 * For example suppose white wants to castle kingside. In chess one is forbidden to castle if the
 * king moves through a check. This class helps us determine whether there is a black bishop or
 * queen attacking the f1-square from the northwest, for example, which would prevent us from
 * castling. This is done by generating {@link #masks} which represent the f1 square, the e2
 * square, and so on, up to the a6 square. {@link #maskTotal} is generated as well as the union of
 * all masks. If there is a black bishop or queen on any of these masks it prevents white from
 * castling kingside if there are no pieces closer to the f1 square.
 * 
 * @author Phil Leszczynski
 */
public class CastleRay {
  /**
   * Initializes a castle ray with a given starting mask, ending mask, and step size. The step size
   * is the number of steps needed to move to the right, wrapping around to the next rank, to get
   * to each successive mask. For example the step size moving up is 8, the step size for moving to
   * the left is -1, and the step size for moving northeast is 9.
   * 
   * @param maskStart the starting mask for which to test for opposing pieces
   * @param maskEnd the ending mask for which to test for opposing pieces
   * @param stepSize the number of steps to the right to make between each successive mask
   */
  public CastleRay(long maskStart, long maskEnd, int stepSize) {
    this.maskStart = maskStart;
    this.maskEnd = maskEnd;
    this.stepSize = stepSize;

    this.masks = new ArrayList<Long>();
    this.maskTotal = 0;
    long mask = this.maskStart;
    this.masks.add(mask);
    this.maskTotal |= mask;
    while (true) {
      // We couldn't do a conventional while-loop because if we increment
      // the mask above the bounds of a 64-bit long it could get us in
      // trouble.
      if (mask == this.maskEnd) {
        this.masks.add(mask);
        this.maskTotal |= mask;
        break;
      }
      if (this.stepSize > 0) {
        mask <<= this.stepSize;
      } else {
        mask >>>= (-this.stepSize);
      }
      this.masks.add(mask);
      this.maskTotal |= mask;
    }
  }

  /**
   * Initializes a castle ray with given starting and ending squares, and step size. See
   * {@link CastleRay#CastleRay(long, long, int)} for more details.
   * 
   * @param squareStart the starting square for which to test for opposing pieces
   * @param squareEnd the ending square for which to test for opposing pieces
   * @param stepSize the number of steps to the right to make between each successive mask
   */
  public CastleRay(String squareStart, String squareEnd, int stepSize) {
    this(new Bitboard(squareStart).getData(), new Bitboard(squareEnd).getData(), stepSize);
  }

  /**
   * Returns a new CastleRay with all of the masks flipped vertically and the step size modified
   * accordingly. The typical usage is that when we have a castle ray used to test castling for
   * white then we can generate the mirror image castle ray for black. For example suppose we have a
   * castle ray for white containing the squares f1, g2, h3. Then we would return a castle ray for
   * black containing the squares f8, g7, h6.
   * 
   * @return the mirror image CastleRay for the opposing player
   */
  public CastleRay flip() {
    long maskStart = new Bitboard(this.maskStart).flip().getData();
    long maskEnd = new Bitboard(this.maskEnd).flip().getData();
    int stepSize = 0;
    if (Math.abs(this.stepSize) == 1) {
      stepSize = this.stepSize;
    } else if (this.stepSize > 0) {
      stepSize = this.stepSize - 16;
    } else {
      stepSize = this.stepSize + 16;
    }
    return new CastleRay(maskStart, maskEnd, stepSize);
  }

  /**
   * Determines whether this castle ray prevents the player from castling, given the position of an
   * opposing piece and the positions of the other pieces on the board. If the opposing piece is not
   * in the castle ray, then the ray cannot prevent castling. Otherwise if the other pieces on the
   * board are not in the castle ray, then we know the ray does prevent castling. Otherwise we need
   * to check whether the opposing piece or one of the other pieces is closer to the mask; if it's
   * the opposing piece then castling is prevented, but if it's one of the other pieces then the ray
   * does not prevent castling.
   * 
   * @param oppPieceMask a 64-bit long specifying the square of the opposing piece
   * @param otherPieces a 64-bit long representing the squares of the other pieces on the board, see
   *        {@link Bitboard} for a summary of the long mask implementation.
   * @return true if the castle ray prevents the player from castling, false otherwise
   */
  public boolean opponentPiecePrecludesCastling(long oppPieceMask, long otherPieces) {
    if ((this.maskTotal & oppPieceMask) == 0) {
      return false;
    }
    if ((this.maskTotal & otherPieces) == 0) {
      return true;
    }
    for (long mask : this.masks) {
      if ((mask & oppPieceMask) != 0) {
        return true;
      } else if ((mask & otherPieces) != 0) {
        return false;
      }
    }
    return false;
  }

  private long maskStart;
  private long maskEnd;
  private int stepSize;
  private ArrayList<Long> masks;
  private long maskTotal;
}
