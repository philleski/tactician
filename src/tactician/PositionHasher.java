package tactician;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class implements a Zobrist hash that is used to map a chess position to a 64-bit long.
 * Zobrist hashing works by assigning a 64-bit long to each piece type and square. This hash is
 * typically used by first assigning 0L to a positionHash variable. When a piece moves onto or off
 * of a given square, positionHash is xor'd with the specific mask corresponding to that piece and
 * square. That way if the same position comes about through a different series of moves it will
 * generate the same position hash. This is very helpful for memoization: storing what was learned
 * about a position and retrieving it when the position is encountered again.
 * 
 * <p>We also store other information besides the piece for each square, taking advantage of the
 * fact that pawns cannot be on the first nor eighth ranks. The en passant file is stored on the
 * first rank of the white pawn mask. The player to move, white or black, is stored on the a8
 * square of the white pawn mask. The castle rights (white and black, kingside and queenside, a
 * total of 16 possibilities) are stored on the first and eighth ranks of the black pawn mask.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Zobrist_hashing">Zobrist Hashing</a>
 * @author Phil Leszczynski
 */
public class PositionHasher {
  /**
   * Initializes a position hasher. Generates a seeded random number generator so that it can be
   * "messy" and map two inputs that are closed together to completely different outputs. However
   * we want the same hash values every time the program is run to help with debugging. Uses the
   * random number generator to create masks for piece types, the player to move, the en passont
   * target squares, and the castling rights.
   */
  public PositionHasher() {
    Random generator = new Random(0);
    this.masks = new HashMap<Color, Map<Piece, Long[]>>();
    for (Color color : Color.values()) {
      Map<Piece, Long[]> colorMask = new HashMap<Piece, Long[]>();
      for (Piece piece : Piece.values()) {
        if (piece == null) {
          continue;
        }
        Long[] pieceMasks = new Long[64];
        for (int i = 0; i < 64; i++) {
          pieceMasks[i] = generator.nextLong();
        }
        colorMask.put(piece, pieceMasks);
      }
      this.masks.put(color, colorMask);
    }
  }

  /**
   * Returns the mask for a given color and piece on a given square. For example this can be used
   * for the white bishop on e3. The idea is every time a bishop either gets onto or leaves e3, we
   * xor the running position hash with this value.
   * 
   * @param color the color of the piece to track
   * @param piece the piece to track
   * @param index the index of the square the piece is on, 0-63
   * @return the generated mask corresponding to the color, piece, and square
   */
  public long getMask(Color color, Piece piece, int index) {
    return this.masks.get(color).get(piece)[index];
  }

  /**
   * Returns the mask for a given color, a piece, and two different squares. This is a convenience
   * method that simply xors the two, and it is typically used by a piece moving from one square to
   * another.
   * 
   * @param color the color of the piece to track
   * @param piece the piece to track
   * @param index the index of the start square the piece is on, 0-63
   * @param index2 the index of the end square the piece is on, 0-63
   * @return the generated mask corresponding to the color, piece, and two squares
   */
  public long getMask(Color color, Piece piece, int index, int index2) {
    return this.getMask(color, piece, index) ^ this.getMask(color, piece, index2);
  }

  /**
   * Returns the mask for an en passant target square. Note that we only need to take into account
   * the file of the en passant square since for example e3 and e6 can be disambiguated by tracking
   * the turn mask through {@link #getMaskTurn()} in the position hash. We use the white pawn
   * mask's first rank for storage since pawns cannot be on either the first nor eighth ranks.
   * 
   * @param enPassantTarget the value of the destination square for en passant capture on the next
   *        turn
   * @return the generated mask corresponding to the en passant target
   */
  public long getMaskEnPassantTarget(long enPassantTarget) {
    int index = Long.numberOfTrailingZeros(enPassantTarget);
    return this.getMask(Color.WHITE, Piece.PAWN, index % 8);
  }

  /**
   * Returns the mask for castle rights. We use the black pawn mask's first and eighth ranks for
   * storage since pawns cannot be on either the first nor eighth ranks. If white kingside is set
   * we use the eighth rank, otherwise we use the first rank. If white queenside is set we use the
   * first four files (queenside), otherwise we use the kingside. If black kingside is set we use
   * the abef files, otherwise we use cdgh. If black queenside is set we use the aceg files,
   * otherwise we use bdfh.
   * 
   * @param castleRights a double map from color and castle type to whether the player can castle
   *        in that direction
   * @return the generated mask corresponding to the castle rights
   */
  public long getMaskCastleRights(Map<Color, Map<Castle, Boolean>> castleRights) {
    byte index = 0;
    if (castleRights.get(Color.WHITE).get(Castle.KINGSIDE)) {
      index += 56;
    }
    if (castleRights.get(Color.WHITE).get(Castle.QUEENSIDE)) {
      index += 4;
    }
    if (castleRights.get(Color.BLACK).get(Castle.KINGSIDE)) {
      index += 2;
    }
    if (castleRights.get(Color.BLACK).get(Castle.QUEENSIDE)) {
      index++;
    }
    return this.getMask(Color.BLACK, Piece.PAWN, index);
  }

  /**
   * Returns the mask for the player to move. We use white's a8 square for storage since white
   * pawns cannot be there.
   * 
   * @return the generated mask corresponding to the player to move
   */
  public long getMaskTurn() {
    return this.getMask(Color.WHITE, Piece.PAWN, (byte) (56));
  }

  /**
   * A double map from player to move and piece to an array of Zobrist masks. The array has 64
   * elements and corresponds to the square for the color and piece. The en passant target, castle
   * rights, and player to move are also stored within the pawn masks on the first and eighth ranks
   * since pawns cannot be positioned there.
   */
  private Map<Color, Map<Piece, Long[]>> masks;
}
