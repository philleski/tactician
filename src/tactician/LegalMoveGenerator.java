package tactician;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class generates legal and pseudo-legal moves for a given board position. A legal move is
 * simply a move under the standard rules of chess, generated by {@link #legalMoves(Board)}. A
 * pseudo-legal move is broader in the sense that it can put the moving player in check. This is
 * useful for some purposes because it's computationally expensive to determine whether a player is
 * in check. Pseudo-legal moves are generated by {@link #legalMovesFast(Board, boolean)}.
 * 
 * <p>A key concept is the use of attack squares, which are masks describing where a piece can move
 * from a given origin. See {@link Bitboard} for the 64-bit mask implementation and {@link Move}
 * for how the 0-63 integer indexing works. For example suppose we have a knight on the c1 square
 * and we want to generate possible moves for it. We pre-generate, through
 * {@link #LegalMoveGenerator()}, the variable {@link #attackSquaresKnight}[2] which lists the
 * possible squares where the knight can move: a2, b3, d3, and e2. The attack square mask would
 * then be 0x00000000000a11 (the union of the four 64-bit longs representing each of the four
 * squares). Then when generating pseudo-legal moves we loop through each set bit in the attack
 * square to obtain the potential moves c1a2, c1b3, c1d3, and c1e2. A move is pseudo-legal if and
 * only if the destination square does not contain a friendly piece. For long-range pieces such as
 * the bishop, rook, and queen it is a bit more complicated since if there is a piece blocking its
 * path it cannot continue to generate pseudo-legal moves further along in the path. We still use
 * attacking squares for long-range pieces but need to be more careful when looping through
 * potential moves.
 * 
 * <p>Another set of pre-generated variables relates to castling. The rules of chess state that the
 * king cannot castle out of a check, through a check, nor into a check. When generating
 * pseudo-legal moves we are only concerned with the first two. {@link #maskCastleSpace} stores the
 * occupied squares between the rook and the king; we need to ensure there are no pieces there for
 * castling to be allowed. We also have {@link #maskCastlePawns} and {@link #maskCastleKnights} to
 * store the locations of enemy pawns or knights that would prevent the king from castling. For
 * example if there is a black knight on h2 then white cannot castle kingside, as the king would
 * move through a check on f1. {@link #castleRaysDiagonal} and {@link #castleRaysStraight} deal
 * with the possibility of long-range pieces preventing a player from castling, see
 * {@link CastleRay} for more details. It turns out that a mask for the enemy king is not
 * necessary: the only squares where the enemy king can prevent check either lead to pseudo-legal
 * moves or are squares where the enemy king could never have gotten to in the first place. (For
 * example if white considers castling his or her king is on e1, so black's king could never have
 * gotten to d2, e2, or f2.)
 * 
 * @author Phil Leszczynski
 */
public class LegalMoveGenerator {
  /** Initializes the legal move generator. */
  public LegalMoveGenerator() {
    this.maskCastleSpace = new HashMap<Color, Map<Castle, Long>>();
    this.maskCastlePawns = new HashMap<Color, Map<Castle, Long>>();
    this.maskCastleKnights = new HashMap<Color, Map<Castle, Long>>();
    this.castleRaysDiagonal = new HashMap<Color, Map<Castle, ArrayList<CastleRay>>>();
    this.castleRaysStraight = new HashMap<Color, Map<Castle, ArrayList<CastleRay>>>();
    this.castleMoves = new HashMap<Color, Map<Castle, Move>>();

    for (Color color : Color.values()) {
      this.maskCastleSpace.put(color, new HashMap<Castle, Long>());
      this.maskCastlePawns.put(color, new HashMap<Castle, Long>());
      this.maskCastleKnights.put(color, new HashMap<Castle, Long>());
    }

    this.maskCastleSpace.get(Color.WHITE).put(Castle.KINGSIDE, new Bitboard("f1", "g1").getData());
    this.maskCastleSpace.get(Color.WHITE).put(Castle.QUEENSIDE,
        new Bitboard("b1", "c1", "d1").getData());
    this.maskCastleSpace.get(Color.BLACK).put(Castle.KINGSIDE,
        new Bitboard(this.maskCastleSpace.get(Color.WHITE).get(Castle.KINGSIDE)).flip().getData());
    this.maskCastleSpace.get(Color.BLACK).put(Castle.QUEENSIDE,
        new Bitboard(this.maskCastleSpace.get(Color.WHITE).get(Castle.QUEENSIDE)).flip().getData());

    this.maskCastlePawns.get(Color.WHITE).put(Castle.KINGSIDE,
        new Bitboard("d2", "e2", "f2", "g2").getData());
    this.maskCastlePawns.get(Color.WHITE).put(Castle.QUEENSIDE,
        new Bitboard("b2", "c2", "d2", "e2", "f2").getData());
    this.maskCastlePawns.get(Color.BLACK).put(Castle.KINGSIDE,
        new Bitboard(this.maskCastlePawns.get(Color.WHITE).get(Castle.KINGSIDE)).flip().getData());
    this.maskCastlePawns.get(Color.BLACK).put(Castle.QUEENSIDE,
        new Bitboard(this.maskCastlePawns.get(Color.WHITE).get(Castle.QUEENSIDE)).flip().getData());

    this.maskCastleKnights.get(Color.WHITE).put(Castle.KINGSIDE,
        new Bitboard("c2", "d2", "g2", "d3", "e3", "f3", "g3", "h2").getData());
    this.maskCastleKnights.get(Color.WHITE).put(Castle.QUEENSIDE,
        new Bitboard("b2", "c2", "f2", "g2", "c3", "d3", "e3", "f3").getData());
    this.maskCastleKnights.get(Color.BLACK).put(Castle.KINGSIDE,
        new Bitboard(this.maskCastleKnights.get(Color.WHITE).get(Castle.KINGSIDE)).flip()
            .getData());
    this.maskCastleKnights.get(Color.BLACK).put(Castle.QUEENSIDE,
        new Bitboard(this.maskCastleKnights.get(Color.WHITE).get(Castle.QUEENSIDE)).flip()
            .getData());

    this.addCastleRayStraight(Color.WHITE, Castle.KINGSIDE, "d1", "a1", -1);
    this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "d2", "a5", 7);
    this.addCastleRayStraight(Color.WHITE, Castle.KINGSIDE, "e2", "e8", 8);
    this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "f2", "h4", 9);
    this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "e2", "a6", 7);
    this.addCastleRayStraight(Color.WHITE, Castle.KINGSIDE, "f2", "f8", 8);
    this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "g2", "h3", 9);
    this.addCastleRayStraight(Color.WHITE, Castle.KINGSIDE, "g2", "g8", 8);
    this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "h2", "h2", 9);
    this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "f2", "a7", 7);

    this.addCastleRayStraight(Color.WHITE, Castle.QUEENSIDE, "f1", "h1", 1);
    this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "d2", "a5", 7);
    this.addCastleRayStraight(Color.WHITE, Castle.QUEENSIDE, "e2", "e8", 8);
    this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "f2", "h4", 9);
    this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "c2", "a4", 7);
    this.addCastleRayStraight(Color.WHITE, Castle.QUEENSIDE, "d2", "d8", 8);
    this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "e2", "h5", 9);
    this.addCastleRayStraight(Color.WHITE, Castle.QUEENSIDE, "c2", "c8", 8);
    this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "b2", "a3", 7);
    this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "d2", "h6", 9);

    this.castleRaysDiagonal.put(Color.BLACK, new HashMap<Castle, ArrayList<CastleRay>>());
    this.castleRaysStraight.put(Color.BLACK, new HashMap<Castle, ArrayList<CastleRay>>());
    for (Castle castle : Castle.values()) {
      ArrayList<CastleRay> castleRaysDiagonalBlack = new ArrayList<CastleRay>();
      for (CastleRay castleRay : this.castleRaysDiagonal.get(Color.WHITE).get(castle)) {
        castleRaysDiagonalBlack.add(castleRay.flip());
      }
      this.castleRaysDiagonal.get(Color.BLACK).put(castle, castleRaysDiagonalBlack);

      ArrayList<CastleRay> castleRaysStraightBlack = new ArrayList<CastleRay>();
      for (CastleRay castleRay : this.castleRaysStraight.get(Color.WHITE).get(castle)) {
        castleRaysStraightBlack.add(castleRay.flip());
      }
      this.castleRaysStraight.get(Color.BLACK).put(castle, castleRaysStraightBlack);
    }

    this.castleMoves.put(Color.WHITE, new HashMap<Castle, Move>());
    this.castleMoves.put(Color.BLACK, new HashMap<Castle, Move>());
    this.castleMoves.get(Color.WHITE).put(Castle.KINGSIDE, new Move((byte) 4, (byte) 6));
    this.castleMoves.get(Color.WHITE).put(Castle.QUEENSIDE, new Move((byte) 4, (byte) 2));
    this.castleMoves.get(Color.BLACK).put(Castle.KINGSIDE, new Move((byte) 60, (byte) 62));
    this.castleMoves.get(Color.BLACK).put(Castle.QUEENSIDE, new Move((byte) 60, (byte) 58));

    this.initAttackSquaresPawn(Color.WHITE);
    this.initAttackSquaresPawn(Color.BLACK);
    this.initAttackSquaresShortRange(this.attackSquaresKing,
        new int[] {-9, -8, -7, -1, 1, 7, 8, 9});
    this.initAttackSquaresShortRange(this.attackSquaresKnight,
        new int[] {-17, -15, -10, -6, 6, 10, 15, 17});
    this.initAttackSquaresLongRange(this.attackSquaresA1H8, new int[] {-9, 9});
    this.initAttackSquaresLongRange(this.attackSquaresA8H1, new int[] {-7, 7});
    this.initAttackSquaresLongRange(this.attackSquaresHorizontal, new int[] {-1, 1});
    this.initAttackSquaresLongRange(this.attackSquaresVertical, new int[] {-8, 8});
    for (int i = 0; i < 64; i++) {
      this.attackSquaresBishop[i] = this.attackSquaresA1H8[i] | this.attackSquaresA8H1[i];
      this.attackSquaresRook[i] = this.attackSquaresHorizontal[i] | this.attackSquaresVertical[i];
      this.attackSquaresQueen[i] = this.attackSquaresBishop[i] | this.attackSquaresRook[i];
      this.attackSquaresPawnBlack[i] =
          this.attackSquaresPawnCaptureBlack[i] | this.attackSquaresPawnMoveBlack[i];
      this.attackSquaresPawnWhite[i] =
          this.attackSquaresPawnCaptureWhite[i] | this.attackSquaresPawnMoveWhite[i];
    }
  }

  /**
   * Generates the attack squares for pawns of a given color. The length-64 arrays
   * {@link #attackSquaresPawnMoveWhite} and {@link #attackSquaresPawnMoveBlack} describe where a
   * pawn can move from the indexed square. Similarly the length-64 arrays
   * {@link #attackSquaresPawnCaptureWhite} and {@link #attackSquaresPawnCaptureBlack} describe
   * where a pawn can capture from the indexed square.
   * 
   * @param color the color for which to generate the attack squares
   */
  private void initAttackSquaresPawn(Color color) {
    int start = 0;
    int end = 64;
    if (color == Color.WHITE) {
      start = 8;
    } else {
      end = 56;
    }
    for (int i = start; i < end; i++) {
      ArrayList<Integer> stepSizesMove = new ArrayList<Integer>();
      ArrayList<Integer> stepSizesCapture = new ArrayList<Integer>();
      if (color == Color.WHITE) {
        stepSizesMove.add(8);
        stepSizesCapture.add(7);
        stepSizesCapture.add(9);
        if (i < 16) {
          stepSizesMove.add(16);
        }
      } else {
        stepSizesMove.add(-8);
        stepSizesCapture.add(-7);
        stepSizesCapture.add(-9);
        if (i >= 48) {
          stepSizesMove.add(-16);
        }
      }
      for (int stepSize : stepSizesMove) {
        if (LegalMoveGenerator.inBounds(i, stepSize)) {
          if (color == Color.WHITE) {
            this.attackSquaresPawnMoveWhite[i] |= 1L << (i + stepSize);
          } else {
            this.attackSquaresPawnMoveBlack[i] |= 1L << (i + stepSize);
          }
        }
      }
      for (int stepSize : stepSizesCapture) {
        if (LegalMoveGenerator.inBounds(i, stepSize)) {
          if (color == Color.WHITE) {
            this.attackSquaresPawnCaptureWhite[i] |= 1L << (i + stepSize);
          } else {
            this.attackSquaresPawnCaptureBlack[i] |= 1L << (i + stepSize);
          }
        }
      }
    }
  }

  /**
   * Generates the attack squares for short range pieces (kings and knights, pawns are handled in
   * {@link #initAttackSquaresPawn(Color)}.
   * 
   * @param attackSquares an uninitialized length-64 array modified to contain the attack squares
   *        for a given piece
   * @param stepSizes an array of offsets the piece can move, e.g. a knight that moves one square
   *        down and two to the left has a step size of -8 - 2 = -10
   */
  private void initAttackSquaresShortRange(long[] attackSquares, int[] stepSizes) {
    for (int i = 0; i < 64; i++) {
      for (int stepSize : stepSizes) {
        if (LegalMoveGenerator.inBounds(i, stepSize)) {
          attackSquares[i] |= 1L << (i + stepSize);
        }
      }
    }
  }

  /**
   * Generates the attack squares for long range pieces (bishops, rooks, and queens).
   * 
   * @param attackSquares an uninitialized length-64 array modified to contain the attack squares
   *        for a given piece
   * @param stepSizes an array of offsets the piece can move one step, e.g. a bishop that moves one
   *        square up and one to the left has a step size of 8 - 1 = 7
   */
  private void initAttackSquaresLongRange(long[] attackSquares, int[] stepSizes) {
    for (int i = 0; i < 64; i++) {
      for (int stepSize : stepSizes) {
        int position = i;
        while (inBounds(position, stepSize)) {
          position += stepSize;
          attackSquares[i] |= 1L << position;
        }
      }
    }
  }

  /**
   * Generates the diagonal {@link CastleRay} for a given color, castle side, start square, end
   * square, and step size. Consider for this example the castle ray containing f2, g3, and h4. If
   * the opponent has a bishop or queen on any of these squares, and there is no other piece on
   * these squares closer to e1, then white cannot castle kingside.
   * 
   * @param color the color of the side that may want to castle
   * @param castle the direction the player may want to castle
   * @param squareStart the start square, "f2" in our example above
   * @param squareEnd the end square, "h4" in our example above
   * @param stepSize the step size among successive squares, 8 + 1 = 9 in our example above
   */
  private void addCastleRayDiagonal(Color color, Castle castle, String squareStart,
      String squareEnd, int stepSize) {
    if (!this.castleRaysDiagonal.containsKey(color)) {
      this.castleRaysDiagonal.put(color, new HashMap<Castle, ArrayList<CastleRay>>());
    }
    if (!this.castleRaysDiagonal.get(color).containsKey(castle)) {
      this.castleRaysDiagonal.get(color).put(castle, new ArrayList<CastleRay>());
    }
    this.castleRaysDiagonal.get(color).get(castle)
        .add(new CastleRay(squareStart, squareEnd, stepSize));
  }

  /**
   * Generates the horizontal or vertical {@link CastleRay} for a given color, castle side, start
   * square, end square, and step size. Consider for this example the castle ray containing d2-d8.
   * If the opponent has a rook or queen on any of these squares, and there is no other piece on
   * these squares closer to d1, then white cannot castle queenside.
   * 
   * @param color the color of the side that may want to castle
   * @param castle the direction the player may want to castle
   * @param squareStart the start square, "d2" in our example above
   * @param squareEnd the end square, "d8" in our example above
   * @param stepSize the step size among successive squares, 8 in our example above
   */
  private void addCastleRayStraight(Color color, Castle castle, String squareStart,
      String squareEnd, int stepSize) {
    if (!this.castleRaysStraight.containsKey(color)) {
      this.castleRaysStraight.put(color, new HashMap<Castle, ArrayList<CastleRay>>());
    }
    if (!this.castleRaysStraight.get(color).containsKey(castle)) {
      this.castleRaysStraight.get(color).put(castle, new ArrayList<CastleRay>());
    }
    this.castleRaysStraight.get(color).get(castle)
        .add(new CastleRay(squareStart, squareEnd, stepSize));
  }

  /**
   * Returns whether a given position and step size keep us within the bounds of the board. For
   * example if a bishop is on position 0 (the a1 square) it cannot go down and to the left with a
   * step size of -9. If a knight is on position 8 (the a2 square) it cannot go up one square and
   * two squares to the left with a step size of 8 - 2 = 6. That would move the knight to the g2
   * square, which is not a legal move for a knight.
   * 
   * @param position the standard board position as an integer with 0 representing the a1 square, 1
   *        representing the b1 square, 8 representing the a2 square, and so on.
   * @param stepSize the amount of squares to the right to shift the position, for example a step
   *        size of 9 moves the piece one step up and one step to the right.
   * @return true if the step size applied to the position is in bounds of the board, false
   *         otherwise
   */
  private static boolean inBounds(int position, int stepSize) {
    if (position + stepSize < 0) {
      return false;
    }
    if (position + stepSize >= 64) {
      return false;
    }
    int fileDiff = (position % 8) - ((position + stepSize) % 8);
    if (fileDiff < -2) {
      return false;
    }
    if (fileDiff > 2) {
      return false;
    }
    return true;
  }

  /**
   * Appends the pseudo-legal pawn moves for a given board. Optionally includes only the moves that
   * make a capture, including en passant.
   * 
   * @param board the board containing the position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForPawn(Board board, ArrayList<Move> moves, boolean capturesOnly) {
    long movers = board.bitboards.get(board.turn).get(Piece.PAWN).getData();
    long oppPieces = board.playerBitboards.get(Color.flip(board.turn)).getData();
    long[] attackSquaresMoveTable = board.turn == Color.WHITE ? this.attackSquaresPawnMoveWhite
        : this.attackSquaresPawnMoveBlack;
    long[] attackSquaresCaptureTable = board.turn == Color.WHITE
        ? this.attackSquaresPawnCaptureWhite : this.attackSquaresPawnCaptureBlack;
    while (movers != 0) {
      int moverIndex = Long.numberOfTrailingZeros(movers);
      long mover = 1L << moverIndex;
      movers ^= mover;
      boolean isPromotable = (board.turn == Color.WHITE && moverIndex >= 48)
          || (board.turn == Color.BLACK && moverIndex < 16);

      // If the pawn is trying to move two squares up and there's something blocking the
      // first square, it's also blocking the second square.
      long moveBlockers = board.allPieces.getData() & ~mover;
      if (board.turn == Color.WHITE) {
        moveBlockers |= (moveBlockers & 0x0000000000FF0000L) << 8;
      } else {
        moveBlockers |= (moveBlockers & 0x0000FF0000000000L) >>> 8;
      }

      if (!capturesOnly) {
        long attackSquaresMove = attackSquaresMoveTable[moverIndex];
        attackSquaresMove &= ~moveBlockers;
        while (attackSquaresMove != 0) {
          int attackSquareIndex = Long.numberOfTrailingZeros(attackSquaresMove);
          long attackSquare = 1L << attackSquareIndex;
          attackSquaresMove ^= attackSquare;
          if (!isPromotable) {
            moves.add(new Move(moverIndex, attackSquareIndex));
          } else {
            moves.add(new Move(moverIndex, attackSquareIndex, Piece.QUEEN));
            moves.add(new Move(moverIndex, attackSquareIndex, Piece.KNIGHT));
            moves.add(new Move(moverIndex, attackSquareIndex, Piece.ROOK));
            moves.add(new Move(moverIndex, attackSquareIndex, Piece.BISHOP));
          }
        }
      }

      long attackSquaresCapture = attackSquaresCaptureTable[moverIndex];
      attackSquaresCapture &= (oppPieces | board.enPassantTarget);
      while (attackSquaresCapture != 0) {
        int attackSquareIndex = Long.numberOfTrailingZeros(attackSquaresCapture);
        long attackSquare = 1L << attackSquareIndex;
        attackSquaresCapture ^= attackSquare;
        if (!isPromotable) {
          moves.add(new Move(moverIndex, attackSquareIndex));
        } else {
          moves.add(new Move(moverIndex, attackSquareIndex, Piece.QUEEN));
          moves.add(new Move(moverIndex, attackSquareIndex, Piece.KNIGHT));
          moves.add(new Move(moverIndex, attackSquareIndex, Piece.ROOK));
          moves.add(new Move(moverIndex, attackSquareIndex, Piece.BISHOP));
        }
      }
    }
  }

  /**
   * Appends the pseudo-legal moves for a long range piece for a given board. The long range pieces
   * are the bishop, rook, and queen.
   * 
   * @param board the board containing the position
   * @param piece the type of piece to generate moves for
   * @param attackSquaresTable the table containing the attack squares for the given piece, or where
   *        the piece can move to from a given board position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForLongRangePiece(Board board, Piece piece, long[] attackSquaresTable,
      ArrayList<Move> moves, boolean capturesOnly) {
    long movers = board.bitboards.get(board.turn).get(piece).getData();
    long myPieces = board.playerBitboards.get(board.turn).getData();
    while (movers != 0) {
      int moverIndex = Long.numberOfTrailingZeros(movers);
      long mover = 1L << moverIndex;
      movers ^= mover;
      long attackSquares = attackSquaresTable[moverIndex];
      long incidentSquares = attackSquares & board.allPieces.getData();
      long incidentSquaresBefore = incidentSquares & (mover - 1L);
      long incidentSquaresAfter = moverIndex == 63 ? 0L : incidentSquares & ~(mover + mover - 1L);
      int leadingZerosBefore = Long.numberOfLeadingZeros(incidentSquaresBefore);
      long incidentMaskBefore =
          leadingZerosBefore == 64 ? ~0L : ~((1L << (63 - leadingZerosBefore)) - 1L);
      int trailingZerosAfter = Long.numberOfTrailingZeros(incidentSquaresAfter);
      long incidentMaskAfter = (trailingZerosAfter == 0 || trailingZerosAfter >= 63) ? ~0L
          : (1L << (trailingZerosAfter + 1)) - 1L;
      attackSquares &= incidentMaskBefore;
      attackSquares &= incidentMaskAfter;
      while (attackSquares != 0) {
        int attackSquareIndex = Long.numberOfTrailingZeros(attackSquares);
        long attackSquare = 1L << attackSquareIndex;
        attackSquares ^= attackSquare;
        if ((attackSquare & myPieces) != 0) {
          continue;
        }
        if (capturesOnly && !board.allPieces.intersects(attackSquare)) {
          continue;
        }
        moves.add(new Move(moverIndex, attackSquareIndex));
      }
    }
  }

  /**
   * Appends the pseudo-legal moves for bishops for a given board.
   * 
   * @param board the board containing the position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForBishop(Board board, ArrayList<Move> moves, boolean capturesOnly) {
    this.appendMovesForLongRangePiece(board, Piece.BISHOP, this.attackSquaresA1H8, moves,
        capturesOnly);
    this.appendMovesForLongRangePiece(board, Piece.BISHOP, this.attackSquaresA8H1, moves,
        capturesOnly);
  }

  /**
   * Appends the pseudo-legal moves for queens for a given board.
   * 
   * @param board the board containing the position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForQueen(Board board, ArrayList<Move> moves, boolean capturesOnly) {
    this.appendMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresA1H8, moves,
        capturesOnly);
    this.appendMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresA8H1, moves,
        capturesOnly);
    this.appendMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresHorizontal, moves,
        capturesOnly);
    this.appendMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresVertical, moves,
        capturesOnly);
  }

  /**
   * Appends the pseudo-legal moves for rooks for a given board.
   * 
   * @param board the board containing the position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForRook(Board board, ArrayList<Move> moves, boolean capturesOnly) {
    this.appendMovesForLongRangePiece(board, Piece.ROOK, this.attackSquaresHorizontal, moves,
        capturesOnly);
    this.appendMovesForLongRangePiece(board, Piece.ROOK, this.attackSquaresVertical, moves,
        capturesOnly);
  }

  /**
   * Appends the pseudo-legal moves for a short range piece for a given board. The short range
   * pieces are the king and knight. Pawns are handled separately in
   * {@link #appendMovesForPawn(Board, ArrayList, boolean)}.
   * 
   * @param board the board containing the position
   * @param piece the type of piece to generate moves for
   * @param attackSquaresTable the table containing the attack squares for the given piece, or where
   *        the piece can move to from a given board position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForShortRangePiece(Board board, Piece piece, long[] attackSquaresTable,
      ArrayList<Move> moves, boolean capturesOnly) {
    long movers = board.bitboards.get(board.turn).get(piece).getData();
    while (movers != 0) {
      int moverIndex = Long.numberOfTrailingZeros(movers);
      long mover = 1L << moverIndex;
      movers ^= mover;
      long attackSquares = attackSquaresTable[moverIndex];
      attackSquares &= ~board.playerBitboards.get(board.turn).getData();
      while (attackSquares != 0) {
        int attackSquareIndex = Long.numberOfTrailingZeros(attackSquares);
        long attackSquare = 1L << attackSquareIndex;
        attackSquares ^= attackSquare;
        if (capturesOnly && !board.allPieces.intersects(attackSquare)) {
          continue;
        }
        moves.add(new Move(moverIndex, attackSquareIndex));
      }
    }
  }

  /**
   * Appends the pseudo-legal moves for kings for a given board.
   * 
   * @param board the board containing the position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForKing(Board board, ArrayList<Move> moves, boolean capturesOnly) {
    this.appendMovesForShortRangePiece(board, Piece.KING, this.attackSquaresKing, moves,
        capturesOnly);
  }

  /**
   * Appends the pseudo-legal moves for knights for a given board.
   * 
   * @param board the board containing the position
   * @param moves an ArrayList of pseudo-legal moves to which we will append
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   */
  private void appendMovesForKnight(Board board, ArrayList<Move> moves, boolean capturesOnly) {
    this.appendMovesForShortRangePiece(board, Piece.KNIGHT, this.attackSquaresKnight, moves,
        capturesOnly);
  }

  /**
   * Appends the pseudo-legal moves for castling for a given board. We verify that the player has
   * the rights to castle in the given direction, that the king does not castle out of a check,
   * that the king does not castle into a check, and that the king does not castle into a check.
   * 
   * @param board the board containing the position
   * @return an ArrayList of castling moves for the given board
   */
  private ArrayList<Move> getMovesForCastling(Board board) {
    ArrayList<Move> result = new ArrayList<Move>();
    for (Castle castle : Castle.values()) {
      if (!board.castleRights.get(board.turn).get(castle)) {
        continue;
      }
      if (board.allPieces.intersects(this.maskCastleSpace.get(board.turn).get(castle))) {
        continue;
      }
      if (this.verifyCastleCheckRule(board, castle)) {
        result.add(this.castleMoves.get(board.turn).get(castle));
      }
    }
    return result;
  }

  /**
   * Verifies that, given a board and a castle direction, the king does not castle out of a check,
   * nor through a check, nor into a check.
   * 
   * @param board the board containing the position
   * @param castle the direction to verify the castle check rule
   * @return true if the king does not castle out/through/into a check, false otherwise
   */
  private boolean verifyCastleCheckRule(Board board, Castle castle) {
    Color turnFlipped = Color.flip(board.turn);
    long oppPiecesDiagonal = board.bitboards.get(turnFlipped).get(Piece.BISHOP)
        .union(board.bitboards.get(turnFlipped).get(Piece.QUEEN)).getData();
    long oppPiecesStraight = board.bitboards.get(turnFlipped).get(Piece.ROOK)
        .union(board.bitboards.get(turnFlipped).get(Piece.QUEEN)).getData();
    if (board.bitboards.get(turnFlipped).get(Piece.PAWN)
        .intersects(this.maskCastlePawns.get(board.turn).get(castle))) {
      return false;
    }
    if (board.bitboards.get(turnFlipped).get(Piece.KNIGHT)
        .intersects(this.maskCastleKnights.get(board.turn).get(castle))) {
      return false;
    }
    for (CastleRay castleRay : this.castleRaysDiagonal.get(board.turn).get(castle)) {
      long otherPieces = board.allPieces.getData() & ~oppPiecesDiagonal;
      if (castleRay.opponentPiecePrecludesCastling(oppPiecesDiagonal, otherPieces)) {
        return false;
      }

    }
    for (CastleRay castleRay : this.castleRaysStraight.get(board.turn).get(castle)) {
      long otherPieces = board.allPieces.getData() & ~oppPiecesStraight;
      if (castleRay.opponentPiecePrecludesCastling(oppPiecesStraight, otherPieces)) {
        return false;
      }

    }
    return true;
  }

  /**
   * Given a board determines whether the player to move is in check.
   * 
   * @param board the board containing the position for which to test for check
   * @return true if the player to move is in check, false otherwise
   */
  public boolean isInCheck(Board board) {
    ArrayList<Move> legalMovesFast = new ArrayList<Move>();
    Bitboard myKings = board.bitboards.get(board.turn).get(Piece.KING);

    // Don't return before restoring the turn.
    board.turn = Color.flip(board.turn);

    this.appendMovesForQueen(board, legalMovesFast, true);
    this.appendMovesForRook(board, legalMovesFast, true);
    this.appendMovesForBishop(board, legalMovesFast, true);
    this.appendMovesForKnight(board, legalMovesFast, true);
    this.appendMovesForPawn(board, legalMovesFast, true);
    this.appendMovesForKing(board, legalMovesFast, true);

    for (Move move : legalMovesFast) {
      if (myKings.intersects(1L << move.destination)) {
        board.turn = Color.flip(board.turn);
        return true;
      }
    }

    board.turn = Color.flip(board.turn);
    return false;
  }

  /**
   * Generates pseudo-legal moves for a given board. Optionally includes only the moves that
   * capture. A pseudo-legal move is one where the player may put himself or herself into check.
   * This is useful because testing for check is computationally expensive, and pseudo-legal moves
   * are useful for many purposes.
   * 
   * @param board the board for which to generate pseudo-legal moves
   * @param capturesOnly if true will include only captures, otherwise will contain all pseudo-legal
   *        moves
   * @return an ArrayList of pseudo-legal moves
   */
  public ArrayList<Move> legalMovesFast(Board board, boolean capturesOnly) {
    ArrayList<Move> legalMovesFast = new ArrayList<Move>();

    this.appendMovesForPawn(board, legalMovesFast, capturesOnly);
    this.appendMovesForKnight(board, legalMovesFast, capturesOnly);
    this.appendMovesForBishop(board, legalMovesFast, capturesOnly);
    this.appendMovesForRook(board, legalMovesFast, capturesOnly);
    this.appendMovesForQueen(board, legalMovesFast, capturesOnly);
    this.appendMovesForKing(board, legalMovesFast, capturesOnly);

    if (!capturesOnly) {
      legalMovesFast.addAll(this.getMovesForCastling(board));
    }

    return legalMovesFast;
  }

  /**
   * Generates legal moves for a given board.
   * 
   * @param board the board for which to generate legal moves
   * @return an ArrayList of legal moves
   */
  public ArrayList<Move> legalMoves(Board board) {
    ArrayList<Move> legalMovesFast = this.legalMovesFast(board, false);
    ArrayList<Move> result = new ArrayList<Move>();
    for (Move move : legalMovesFast) {
      Board copy = new Board(board);
      copy.move(move);
      // Go back to the original player to see if they're in check.
      copy.turn = Color.flip(copy.turn);
      if (!copy.isInCheck()) {
        result.add(move);
      }
    }
    return result;
  }
  
  private Map<Color, Map<Castle, Long>> maskCastleSpace;
  private Map<Color, Map<Castle, Long>> maskCastlePawns;
  private Map<Color, Map<Castle, Long>> maskCastleKnights;

  private Map<Color, Map<Castle, ArrayList<CastleRay>>> castleRaysDiagonal;
  private Map<Color, Map<Castle, ArrayList<CastleRay>>> castleRaysStraight;

  private Map<Color, Map<Castle, Move>> castleMoves;

  private long[] attackSquaresA1H8 = new long[64];
  private long[] attackSquaresA8H1 = new long[64];
  private long[] attackSquaresBishop = new long[64];
  private long[] attackSquaresHorizontal = new long[64];
  private long[] attackSquaresKing = new long[64];
  private long[] attackSquaresKnight = new long[64];
  private long[] attackSquaresPawnBlack = new long[64];
  private long[] attackSquaresPawnCaptureBlack = new long[64];
  private long[] attackSquaresPawnCaptureWhite = new long[64];
  private long[] attackSquaresPawnMoveBlack = new long[64];
  private long[] attackSquaresPawnMoveWhite = new long[64];
  private long[] attackSquaresPawnWhite = new long[64];
  private long[] attackSquaresQueen = new long[64];
  private long[] attackSquaresRook = new long[64];
  private long[] attackSquaresVertical = new long[64];
}
