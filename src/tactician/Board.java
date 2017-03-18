package tactician;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the full state of a chessboard. For example it contains bitboards for all
 * the pieces, castling rights, and where en passant is possible. It also has the
 * {@link #move(Move)} method which makes a given move on the board. It also tracks the position
 * hashes which are useful for memoizing duplicates through transposition tables. See
 * {@link PositionHasher} and {@link PawnKingHashTable} for more details.
 * 
 * <p>When the board changes state we have several summary fields that MUST be updated. This
 * includes the summary bitboards {@link #playerBitboards} and {@link #allPieces} which track the
 * occupied pieces for white/black and for both players, respectively. Also when a move is made the
 * position hashes {@link #positionHash} and {@link #positionHashPawnsKings} must be updated for
 * the transposition table memoization to work. We need to update {@link #castleRights},
 * {@link #enPassantTarget}, and {@link #fullMoveCounter} as well as necessary.
 * 
 * @author Phil Leszczynski
 */
public class Board {
  /**
   * Initializes a board at the chess starting position. There are 16 pieces on the board on their
   * usual squares, the summary bitboards are updated accordingly, the full move counter is set to
   * 1, the en passant target is unset, the castling rights are cleared, and the position hashes
   * are initialized.
   */
  public Board() {
    this.positionHasher = new PositionHasher();
    Map<Piece, Bitboard> whiteBitboards = new HashMap<Piece, Bitboard>();
    Map<Piece, Bitboard> blackBitboards = new HashMap<Piece, Bitboard>();
    whiteBitboards.put(Piece.BISHOP, new Bitboard("c1", "f1"));
    whiteBitboards.put(Piece.KING, new Bitboard("e1"));
    whiteBitboards.put(Piece.KNIGHT, new Bitboard("b1", "g1"));
    whiteBitboards.put(Piece.PAWN, Bitboard.bitboardFromRank(1));
    whiteBitboards.put(Piece.QUEEN, new Bitboard("d1"));
    whiteBitboards.put(Piece.ROOK, new Bitboard("a1", "h1"));
    for (Map.Entry<Piece, Bitboard> entry : whiteBitboards.entrySet()) {
      Piece piece = entry.getKey();
      Bitboard bitboard = entry.getValue();
      blackBitboards.put(piece, bitboard.flip());
    }
    this.bitboards = new HashMap<Color, Map<Piece, Bitboard>>();
    this.bitboards.put(Color.WHITE, whiteBitboards);
    this.bitboards.put(Color.BLACK, blackBitboards);
    this.playerBitboards = new HashMap<Color, Bitboard>();
    updateSummaryBitboards();

    this.turn = Color.WHITE;
    // If the last move was a double pawn move, this is the destination
    // coordinate.
    this.enPassantTarget = 0;
    this.castleRights = new HashMap<Color, Map<Castle, Boolean>>();
    for (Color color : Color.values()) {
      this.castleRights.put(color, new HashMap<Castle, Boolean>());
      for (Castle castle : Castle.values()) {
        this.castleRights.get(color).put(castle, true);
      }
    }
    this.fullMoveCounter = 1;

    this.setPositionHash();
  }

  /**
   * Initializes a board to be a duplicate of another board. Sets the piece bitboards to match the
   * other board, updates the summary bitboards, updates the full move counter, updates the en
   * passant target, copies the castling rights, and copies the position hashes.
   * 
   * @param other the board whose state to copy
   */
  public Board(Board other) {
    this.bitboards = new HashMap<Color, Map<Piece, Bitboard>>();
    for (Map.Entry<Color, Map<Piece, Bitboard>> entry1 : other.bitboards.entrySet()) {
      Color color = entry1.getKey();
      Map<Piece, Bitboard> bitboardsForColor = new HashMap<Piece, Bitboard>();
      for (Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
        Piece piece = entry2.getKey();
        Bitboard bitboard = entry2.getValue();
        bitboardsForColor.put(piece, bitboard.copy());
      }
      this.bitboards.put(color, bitboardsForColor);
    }
    this.playerBitboards = new HashMap<Color, Bitboard>();
    updateSummaryBitboards();

    this.turn = other.turn;
    this.enPassantTarget = other.enPassantTarget;
    this.castleRights = new HashMap<Color, Map<Castle, Boolean>>();
    for (Color color : Color.values()) {
      this.castleRights.put(color, new HashMap<Castle, Boolean>());
      for (Castle castle : Castle.values()) {
        this.castleRights.get(color).put(castle, other.castleRights.get(color).get(castle));
      }
    }
    this.fullMoveCounter = other.fullMoveCounter;
    this.positionHasher = other.positionHasher;
    this.positionHash = other.positionHash;
    this.positionHashPawnsKings = other.positionHashPawnsKings;
  }

  /**
   * Initializes a board to a state given by a FEN string. FEN is a standard for representing
   * chessboard state; for more details
   * 
   * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">
   *      Forsyth-Edwards Notation</a>
   * @param fenstring the string in FEN notation representing the board state
   */
  public Board(String fenstring) {
    this();
    this.setPositionFenstring(fenstring);
  }

  /** Pretty prints the most important parts of the board state. */
  @Override
  public String toString() {
    String result = "";
    String rowReversed = "";
    for (long i = 63; i >= 0; i--) {
      long mask = 1L << i;
      char initial = ' ';
      for (Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
        Color color = entry1.getKey();
        for (Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
          Piece piece = entry2.getKey();
          Bitboard bitboard = entry2.getValue();
          if (bitboard.intersects(mask)) {
            initial = piece.initial();
            if (color == Color.BLACK) {
              initial = (char) (initial - 'A' + 'a');
            }
          }
        }
      }
      rowReversed += initial;
      if (i % 8 == 0) {
        result += new StringBuilder(rowReversed).reverse().toString();
        result += '\n';
        rowReversed = "";
      }
    }
    result += "Legal Moves: ";
    ArrayList<Move> legalMoves = legalMoveGenerator.legalMoves(this);
    for (Move move : legalMoves) {
      result += AlgebraicNotation.moveToAlgebraic(this, move) + ", ";
    }
    if (legalMoves.size() > 0) {
      // Remove the last comma.
      result = result.substring(0, result.length() - 2);
    }
    result += "\n";
    if (this.isInCheck()) {
      result += "Check!\n";
    }
    result += "Turn: " + this.turn.toString();
    result += "\n\n";
    return result;
  }

  /**
   * Returns whether or not the player in {@link #turn} is in check.
   * 
   * @return true if the player is in check, false otherwise
   */
  public boolean isInCheck() {
    return legalMoveGenerator.isInCheck(this);
  }

  /**
   * Returns the pseudo-legal moves for the player in {@link #turn}. See
   * {@link LegalMoveGenerator#legalMovesFast} for a precise definition.
   * 
   * @param capturesOnly if true return only the pseudo-legal moves that capture a piece, if false
   *        return all pseudo-legal moves. En passant counts as a capture.
   * @return an ArrayList of pseudo-legal moves for the current position
   */
  public ArrayList<Move> legalMovesFast(boolean capturesOnly) {
    return legalMoveGenerator.legalMovesFast(this, capturesOnly);
  }

  /**
   * Returns the legal moves for the player in {@link #turn}. This includes all legal moves per the
   * rules of chess, not just pseudo-legal moves.
   * 
   * @return an AraryList of legal moves for the current position
   */
  public ArrayList<Move> legalMoves() {
    return legalMoveGenerator.legalMoves(this);
  }

  /**
   * When making a move, handles the special case where the opponent's rook is captured. In this
   * case the castling rights corresponding to that rook must be removed. This handles the unusual
   * scenario where for example white's h1 rook is captured and then white eventually swings the a1
   * rook over to h1 and attempts to castle kingside. Such a move is not allowed by the rules of
   * chess, so we must remove kingside castling rights in that case. Note that summary tables are
   * not updated here, they should be updated in {@link #move(Move)}.
   * 
   * @param move the move containing a rook capture
   */
  private void moveHandleOpponentRookCapture(Move move) {
    Color turnFlipped = Color.flip(this.turn);
    byte rookKingsideSourceOpponent;
    byte rookQueensideSourceOpponent;

    if (this.turn == Color.WHITE) {
      rookKingsideSourceOpponent = 63;
      rookQueensideSourceOpponent = 56;
    } else {
      rookKingsideSourceOpponent = 7;
      rookQueensideSourceOpponent = 0;
    }

    this.positionHash ^= this.positionHasher.getMaskCastleRights(this.castleRights);
    if (move.destination == rookQueensideSourceOpponent) {
      this.castleRights.get(turnFlipped).put(Castle.QUEENSIDE, false);
    } else if (move.destination == rookKingsideSourceOpponent) {
      this.castleRights.get(turnFlipped).put(Castle.KINGSIDE, false);
    }
    this.positionHash ^= this.positionHasher.getMaskCastleRights(this.castleRights);
  }

  /**
   * When making a move, removes the piece on the destination square from the opposing player's
   * piece bitboard. Note that summary tables are not updated here, they should be updated in
   * {@link #move(Move)}.
   * 
   * @param move the move containing a capture
   */
  private void moveRemoveDestination(Move move) {
    long destinationMask = 1L << move.destination;
    Color turnFlipped = Color.flip(this.turn);

    Map<Piece, Bitboard> opponentBitboards = this.bitboards.get(turnFlipped);
    for (Map.Entry<Piece, Bitboard> entry2 : opponentBitboards.entrySet()) {
      Piece piece = entry2.getKey();
      Bitboard bitboard = entry2.getValue();
      if (bitboard.intersects(destinationMask)) {
        bitboard.updateRemove(destinationMask);
        this.positionHash ^= this.positionHasher.getMask(turnFlipped, piece, move.destination);
        if (piece == Piece.PAWN || piece == Piece.KING) {
          this.positionHashPawnsKings ^=
              this.positionHasher.getMask(turnFlipped, piece, move.destination);
        }
        return;
      }
    }
  }

  /**
   * When making a move, transfers the piece within the player's piece bitboard from the source
   * square to the destination square. Note that summary tables are not updated here, they should be
   * updated in {@link #move(Move)}.
   * 
   * @param move the move for which to transfer piece position
   * @return the type of piece that moved
   */
  private Piece moveUpdateTransferPiece(Move move) {
    long sourceMask = 1L << move.source;
    long destinationMask = 1L << move.destination;

    Piece movedPiece = null;
    for (Map.Entry<Piece, Bitboard> entry : this.bitboards.get(this.turn).entrySet()) {
      Piece piece = entry.getKey();
      Bitboard bitboard = entry.getValue();
      if (bitboard.intersects(sourceMask)) {
        movedPiece = piece;
        bitboard.updateRemove(sourceMask);
        bitboard.updateUnion(destinationMask);
        this.positionHash ^=
            this.positionHasher.getMask(this.turn, piece, move.source, move.destination);
        if (piece == Piece.PAWN || piece == Piece.KING) {
          this.positionHashPawnsKings ^=
              this.positionHasher.getMask(this.turn, piece, move.source, move.destination);
        }
        break;
      }
    }
    return movedPiece;
  }

  /**
   * When making a move, handles the case where an en passant capture occurred. Assume that the
   * source piece has already been transferred with {@link #moveUpdateTransferPiece(Move)}. Note
   * that {@link #moveRemoveDestination(Move)} only removes the opposing piece on the destination
   * square, whereas with en passant the pawn behind the destination square is captured. So in this
   * case we still have to remove the opposing pawn. Note that summary tables are not updated here,
   * they should be updated in {@link #move(Move)}.
   * 
   * @see <a href="https://en.wikipedia.org/wiki/En_passant">En Passant</a>
   * @param move the move containing the en passant capture
   */
  private void moveEnPassant(Move move) {
    long destinationMask = 1L << move.destination;
    Color turnFlipped = Color.flip(this.turn);
    byte destinationRetreatedOneRow;
    long destinationMaskRetreatedOneRow;

    if (this.turn == Color.WHITE) {
      destinationRetreatedOneRow = (byte) (move.destination - 8);
      destinationMaskRetreatedOneRow = destinationMask >>> 8;
    } else {
      destinationRetreatedOneRow = (byte) (move.destination + 8);
      destinationMaskRetreatedOneRow = destinationMask << 8;
    }

    this.bitboards.get(turnFlipped).get(Piece.PAWN).updateRemove(destinationMaskRetreatedOneRow);
    this.positionHash ^=
        this.positionHasher.getMask(turnFlipped, Piece.PAWN, destinationRetreatedOneRow);
    this.positionHashPawnsKings ^=
        this.positionHasher.getMask(turnFlipped, Piece.PAWN, destinationRetreatedOneRow);
  }

  /**
   * When making a move, updates the en passant target {@link #enPassantTarget} if the pawn has
   * moved forward two spaces. See the definition of {@link #enPassantTarget} for more details. Note
   * that summary tables are not updated here, they should be updated in {@link #move(Move)}.
   * 
   * @param move a move where the pawn moved forward two spaces
   */
  private void moveSetEnPassantTarget(Move move) {
    long destinationMask = 1L << move.destination;
    int destinationRetreatedOneRow;
    long destinationMaskRetreatedOneRow;

    if (this.turn == Color.WHITE) {
      destinationRetreatedOneRow = (byte) (move.destination - 8);
      destinationMaskRetreatedOneRow = destinationMask >>> 8;
    } else {
      destinationRetreatedOneRow = (byte) (move.destination + 8);
      destinationMaskRetreatedOneRow = destinationMask << 8;
    }

    if (this.enPassantTarget != 0) {
      this.positionHash ^= this.positionHasher.getMaskEnPassantTarget(this.enPassantTarget);
    }
    this.enPassantTarget = destinationMaskRetreatedOneRow;
    this.positionHash ^= this.positionHasher.getMaskEnPassantTarget(destinationMaskRetreatedOneRow);
    this.positionHashPawnsKings ^=
        this.positionHasher.getMaskEnPassantTarget(destinationRetreatedOneRow);
  }

  /**
   * When making a move, removes the en passant target {@link #enPassantTarget} if the move is
   * anything other than a pawn moving forward two spaces. See the definition of
   * {@link #enPassantTarget} for more details. Note that summary tables are not updated here, they
   * should be updated in {@link #move(Move)}.
   * 
   * @param move a move that is anything other than a pawn moving forward two spaces
   */
  private void moveUnsetEnPassantTarget(Move move) {
    if (this.enPassantTarget != 0) {
      this.positionHash ^= this.positionHasher.getMaskEnPassantTarget(this.enPassantTarget);
      this.positionHashPawnsKings ^=
          this.positionHasher.getMaskEnPassantTarget(this.enPassantTarget);
    }
    this.enPassantTarget = 0;
  }

  /**
   * When making a move, if it is a king move, removes both kingside and queenside castling rights
   * from that player. The rules of chess state that if a king has moved or castled, the player can
   * no longer castle for the rest of the game. Note that summary tables are not updated here, they
   * should be updated in {@link #move(Move)}.
   * 
   * @param move a king move, including castling
   */
  private void moveKingRemoveCastleRights(Move move) {
    this.positionHash ^= this.positionHasher.getMaskCastleRights(this.castleRights);
    this.castleRights.get(this.turn).put(Castle.KINGSIDE, false);
    this.castleRights.get(this.turn).put(Castle.QUEENSIDE, false);
    this.positionHash ^= this.positionHasher.getMaskCastleRights(this.castleRights);
  }

  /**
   * When making a move, if the player castles queenside, update the position of the rook. Update
   * the position hash accordingly. Note that summary tables are not updated here, they should be
   * updated in {@link #move(Move)}.
   * 
   * @param move a move where the player castles queenside
   */
  private void moveCastleQueenside(Move move) {
    Bitboard rookStart;
    Bitboard rookEnd;
    byte rookSource;
    byte rookDestination;

    if (this.turn == Color.WHITE) {
      rookStart = this.bbA1;
      rookEnd = this.bbD1;
      rookSource = 0;
      rookDestination = 3;
    } else {
      rookStart = this.bbA8;
      rookEnd = this.bbD8;
      rookSource = 56;
      rookDestination = 59;
    }

    this.bitboards.get(this.turn).get(Piece.ROOK).updateRemove(rookStart);
    this.bitboards.get(this.turn).get(Piece.ROOK).updateUnion(rookEnd);
    this.positionHash ^=
        this.positionHasher.getMask(this.turn, Piece.ROOK, rookSource, rookDestination);
  }

  /**
   * When making a move, if the player castles kingside, update the position of the rook. Update the
   * position hash accordingly. Note that summary tables are not updated here, they should be
   * updated in {@link #move(Move)}.
   * 
   * @param move a move where the player castles kingside
   */
  private void moveCastleKingside(Move move) {
    Bitboard rookStart;
    Bitboard rookEnd;
    byte rookSource;
    byte rookDestination;

    if (this.turn == Color.WHITE) {
      rookStart = this.bbH1;
      rookEnd = this.bbF1;
      rookSource = 7;
      rookDestination = 5;
    } else {
      rookStart = this.bbH8;
      rookEnd = this.bbF8;
      rookSource = 63;
      rookDestination = 61;
    }

    this.bitboards.get(this.turn).get(Piece.ROOK).updateRemove(rookStart);
    this.bitboards.get(this.turn).get(Piece.ROOK).updateUnion(rookEnd);
    this.positionHash ^=
        this.positionHasher.getMask(this.turn, Piece.ROOK, rookSource, rookDestination);
  }

  /**
   * When making a move, handle the case where a pawn is promoted. Note that
   * {@link Board#moveUpdateTransferPiece(Move)} updates the pawn bitboard to move the pawn onto the
   * destination square on the promotion rank. We have to correct for this by removing the pawn from
   * that square on its bitboard, in addition to updating the bitboard of the promoted piece. Note
   * that summary tables are not updated here, they should be updated in {@link #move(Move)}.
   * 
   * @param move a move that promotes a pawn to a queen, knight, rook, or bishop.
   */
  private void movePromote(Move move) {
    long destinationMask = 1L << move.destination;

    this.bitboards.get(this.turn).get(Piece.PAWN).updateRemove(destinationMask);
    this.bitboards.get(this.turn).get(move.promoteTo).updateUnion(destinationMask);
    this.positionHash ^= this.positionHasher.getMask(this.turn, Piece.PAWN, move.destination);
    this.positionHash ^= this.positionHasher.getMask(this.turn, move.promoteTo, move.destination);
    this.positionHashPawnsKings ^=
        this.positionHasher.getMask(this.turn, Piece.PAWN, move.destination);
  }

  /**
   * When making a move, if a rook moves, remove castling rights if needed. The rules of chess
   * stipulate that if a rook moves from its initial square then castling on that side is not
   * allowed for the rest of the game, but castling on the other side may be allowed. Note that
   * summary tables are not updated here, they should be updated in {@link #move(Move)}.
   * 
   * @param move a rook move
   */
  private void moveUpdateCastlingRightsForRookMove(Move move) {
    byte rookQueensideSource;
    byte rookKingsideSource;

    if (this.turn == Color.WHITE) {
      rookQueensideSource = 0;
      rookKingsideSource = 7;
    } else {
      rookQueensideSource = 56;
      rookKingsideSource = 63;
    }

    this.positionHash ^= this.positionHasher.getMaskCastleRights(this.castleRights);
    if (move.source == rookQueensideSource) {
      this.castleRights.get(this.turn).put(Castle.QUEENSIDE, false);
    } else if (move.source == rookKingsideSource) {
      this.castleRights.get(this.turn).put(Castle.KINGSIDE, false);
    }
    this.positionHash ^= this.positionHasher.getMaskCastleRights(this.castleRights);
  }

  /**
   * Make a move and fully update the state of the board. For example this updates the bitboard of
   * the moving piece, removes a captured piece if any from its bitboard, updates castling rights,
   * and updates the en passant target square. It also switches the player to move, increments the
   * fullmove counter if needed, and updates summary bitboards.
   * 
   * @param move the move to make on the board
   */
  public void move(Move move) {
    long sourceMask = 1L << move.source;
    long destinationMask = 1L << move.destination;
    Color turnFlipped = Color.flip(this.turn);

    long sourceMaskAdvancedTwoRows;
    if (this.turn == Color.WHITE) {
      sourceMaskAdvancedTwoRows = sourceMask << 16;
    } else {
      sourceMaskAdvancedTwoRows = sourceMask >>> 16;
    }

    if (this.bitboards.get(turnFlipped).get(Piece.ROOK).intersects(destinationMask)) {
      this.moveHandleOpponentRookCapture(move);
    }
    if (this.playerBitboards.get(turnFlipped).intersects(destinationMask)) {
      this.moveRemoveDestination(move);
    }
    Piece movedPiece = this.moveUpdateTransferPiece(move);
    if (movedPiece == Piece.PAWN && destinationMask == this.enPassantTarget) {
      this.moveEnPassant(move);
    }
    if (movedPiece == Piece.PAWN && sourceMaskAdvancedTwoRows == destinationMask) {
      this.moveSetEnPassantTarget(move);
    } else {
      this.moveUnsetEnPassantTarget(move);
    }
    if (movedPiece == Piece.KING) {
      this.moveKingRemoveCastleRights(move);
      if (move.source - 2 == move.destination) {
        this.moveCastleQueenside(move);
      } else if (move.source + 2 == move.destination) {
        this.moveCastleKingside(move);
      }
    } else if (movedPiece == Piece.PAWN && move.promoteTo != null) {
      this.movePromote(move);
    } else if (movedPiece == Piece.ROOK) {
      this.moveUpdateCastlingRightsForRookMove(move);
    }

    if (this.turn == Color.BLACK) {
      this.fullMoveCounter++;
    }

    this.turn = turnFlipped;
    this.positionHash ^= this.positionHasher.getMaskTurn();
    updateSummaryBitboards();
  }

  /**
   * Make a move and fully update the state of the board. See {@link #move(Move)} for a summary of
   * the pre and post conditions. Note we're assuming this move is not a promotion. If such a move
   * is needed one can use {@link #move(Move)}.
   * 
   * @param source the square from which to move, for example "c3"
   * @param destination the square where to move, for example "c6"
   */
  public void move(String source, String destination) {
    Move m = new Move(source, destination);
    this.move(m);
  }

  /**
   * Clears the board so that it contains no pieces. Updates the summary bitboards and position
   * hashes accordingly, and sets the full move counter to 1.
   */
  private void clear() {
    for (Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
      for (Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
        Bitboard bitboard = entry2.getValue();
        bitboard.clear();
      }
    }
    this.fullMoveCounter = 1;
    this.updateSummaryBitboards();
    this.setPositionHash();
  }

  /**
   * Sets the board to a state given by a FEN string. FEN is a standard for representing chessboard
   * state. Note the halfmove clock is not yet implemented, as this engine does not yet detect
   * threefold move repetition. For more details about FEN
   * 
   * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation"> Forsyth-Edwards
   *      Notation</a>
   * @param fenstring the string in FEN notation representing the board state
   */
  public void setPositionFenstring(String fenstring) {
    String[] parts = fenstring.split(" ");

    String placement = parts[0];
    String[] placementParts = placement.split("/");
    this.clear();
    for (int i = 0; i < 8; i++) {
      // Start with rank 8 and go to rank 1.
      long mask = 1L << 8 * (7 - i); // a8, a7, ..., a1
      int placementPartLength = placementParts[i].length();
      for (int j = 0; j < placementPartLength; j++) {
        char initial = placementParts[i].charAt(j);
        Color color = Color.WHITE;
        if (Character.isLowerCase(initial)) {
          color = Color.BLACK;
        }
        Piece piece = Piece.initialToPiece(initial);
        if (piece == null) {
          // A numeric amount of blank squares.
          mask <<= (initial - '1');
        } else {
          this.bitboards.get(color).get(piece).updateUnion(mask);
        }
        if (j < placementPartLength - 1) {
          // If we happen to be on h8 it may cause an out-of-bounds
          // error otherwise.
          mask <<= 1;
        }
      }
    }
    updateSummaryBitboards();

    String activeColor = parts[1];
    if (activeColor.equals("w")) {
      this.turn = Color.WHITE;
    } else {
      this.turn = Color.BLACK;
    }

    String castling = parts[2];
    for (Color color : Color.values()) {
      this.castleRights.put(color, new HashMap<Castle, Boolean>());
      for (Castle castle : Castle.values()) {
        this.castleRights.get(color).put(castle, false);
      }
    }
    if (castling.contains("K")) {
      this.castleRights.get(Color.WHITE).put(Castle.KINGSIDE, true);
    }
    if (castling.contains("Q")) {
      this.castleRights.get(Color.WHITE).put(Castle.QUEENSIDE, true);
    }
    if (castling.contains("k")) {
      this.castleRights.get(Color.BLACK).put(Castle.KINGSIDE, true);
    }
    if (castling.contains("q")) {
      this.castleRights.get(Color.BLACK).put(Castle.QUEENSIDE, true);
    }

    String enPassantTarget = parts[3];
    if (enPassantTarget.equals("-")) {
      this.enPassantTarget = 0;
    } else {
      this.enPassantTarget = new Square(enPassantTarget).getMask();
    }

    // Note we don't yet implement the halfmove clock.

    String fullMoveCounter = parts[5];
    this.fullMoveCounter = Integer.parseInt(fullMoveCounter);

    this.setPositionHash();
  }

  /**
   * Returns the type of piece on a given square. Returns null if no piece is found there.
   * 
   * @param square the square where the piece is located
   * @return the type of piece residing on the given square, or null if no piece is found.
   */
  public Piece pieceOnSquare(Square square) {
    for (Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
      for (Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
        Piece piece = entry2.getKey();
        Bitboard bitboard = entry2.getValue();
        if (bitboard.intersects(square)) {
          return piece;
        }
      }
    }
    return null;
  }

  /**
   * Sets the {@link #positionHash} and {@link #positionHashPawnsKings} variables for the current
   * board position.
   */
  private void setPositionHash() {
    this.positionHash = 0;
    this.positionHashPawnsKings = 0;
    for (byte i = 0; i < 64; i++) {
      long mask = 1L << i;
      for (Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
        Color color = entry1.getKey();
        for (Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
          Piece piece = entry2.getKey();
          Bitboard bitboard = entry2.getValue();
          if (bitboard.intersects(mask)) {
            this.positionHash ^= this.positionHasher.getMask(color, piece, i);
            if (piece == Piece.PAWN || piece == Piece.KING) {
              this.positionHashPawnsKings ^= this.positionHasher.getMask(color, piece, i);
            }
          }
        }
      }
      if (this.enPassantTarget == mask) {
        this.positionHash ^= this.positionHasher.getMaskEnPassantTarget(i);
        this.positionHashPawnsKings ^= this.positionHasher.getMaskEnPassantTarget(i);
      }
    }
    if (this.turn == Color.BLACK) {
      this.positionHash ^= this.positionHasher.getMaskTurn();
    }
    this.positionHash ^= this.positionHasher.getMaskCastleRights(this.castleRights);
  }

  /**
   * Updates the summary bitboards {@link #playerBitboards} and {@link #allPieces} for the current
   * board position.
   */
  private void updateSummaryBitboards() {
    this.playerBitboards.put(Color.WHITE, new Bitboard());
    this.playerBitboards.put(Color.BLACK, new Bitboard());
    this.allPieces = new Bitboard();
    for (Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
      Color color = entry1.getKey();
      for (Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
        Bitboard bitboard = entry2.getValue();
        this.playerBitboards.get(color).updateUnion(bitboard);
        this.allPieces.updateUnion(bitboard);
      }
    }
  }

  /**
   * A double map containing the bitboards for each color and piece. For example this includes the
   * bitboard containing white bishops within the current board position.
   */
  public Map<Color, Map<Piece, Bitboard>> bitboards;

  /**
   * A map of bitboards containing all the pieces for each color. For example this includes the
   * bitboard containing all the pieces on the board for the white player.
   */
  public Map<Color, Bitboard> playerBitboards;

  /**
   * A summary bitboard containing all the pieces on the board.
   */
  public Bitboard allPieces;

  /**
   * The color of the player who will next make a move. For example this is white at the start of
   * the game.
   */
  public Color turn;

  /**
   * A mask representing the en passant target square. See {@link Bitboard} for a description of
   * the 64-bit long occupied square implementation. If the last move was not a pawn moving forward
   * two spaces, of if it's the start of the game, the en passant target is set to 0. Otherwise if
   * the last move was a pawn moving forward two spaces, the en passant target is set to the square
   * in between the source and destination squares. This signifies that if the other player wishes
   * to make an en passant capture on the next move, his/her pawn will end up on the en passant
   * target square.
   * 
   * @see <a href="https://en.wikipedia.org/wiki/En_passant">En Passant</a>
   */
  public long enPassantTarget;

  /**
   * A double map containing castling rights for each color and castle type. If the corresponding
   * Boolean value is set to true that means the player can castle in that direction, otherwise it
   * is prohibited. Note that this is the long-term definition of right-to-castle, meaning neither
   * the king nor the chosen rook has moved up to this point in the game. Other conditions must
   * still be met for castling to be a legal move: for example there may be no pieces between the
   * king and the rook, and the player cannot castle out of / through / into check. For a full
   * description of castling requirements:
   * 
   * @see <a href="https://en.wikipedia.org/wiki/Castling">Castling</a>
   */
  public Map<Color, Map<Castle, Boolean>> castleRights;

  /**
   * An integer representing the number of full moves that elapsed through the game, according to
   * the FEN standard. This starts at 1 and is incremented whenever black makes a move.
   * 
   * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation"> Forsyth-Edwards
   *      Notation</a>
   */
  public int fullMoveCounter;

  /**
   * A Zobrist hash of the current position, used for memoization through the transposition table
   * {@link TranspositionTable}. It hashes the bitboards of all the pieces, the side to move,
   * castling rights, and the en passant target square. This is useful when we want to evaluate the
   * same position arising through a different series of moves; we can save computational resources
   * by looking at what we concluded in the previous position.
   * 
   * @see <a href="https://en.wikipedia.org/wiki/Zobrist_hashing">Zobrist Hashing</a>
   */
  public long positionHash;

  /**
   * A Zobrist hash of the current position containing only the kings and pawns. It hashes only the
   * bitboards of the pawns and kings. It ignores the side to move, castling rights, and the en
   * passant target. We use a special position hash since the pawn structure and king position are
   * relatively static throughout the middlegame. Therefore we can perform more expensive
   * calculations related to pawn structure and king safety, as we will get a much higher hit rate
   * through memoization.
   */
  public long positionHashPawnsKings;

  private static LegalMoveGenerator legalMoveGenerator = new LegalMoveGenerator();
  private PositionHasher positionHasher = null;

  // Convenience bitboards for castling.
  private Bitboard bbA1 = new Bitboard("a1");
  private Bitboard bbA8 = new Bitboard("a8");
  private Bitboard bbH1 = new Bitboard("h1");
  private Bitboard bbH8 = new Bitboard("h8");
  private Bitboard bbD1 = new Bitboard("d1");
  private Bitboard bbD8 = new Bitboard("d8");
  private Bitboard bbF1 = new Bitboard("f1");
  private Bitboard bbF8 = new Bitboard("f8");
}
