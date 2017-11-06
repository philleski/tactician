package tactician;

import java.util.HashMap;
import java.util.Map;

/**
 * This class computes the static evaluation of a board position. This includes factors such as
 * material, king safety, open files for rooks, and pawn structure. It only looks at the current
 * position and does not take into account that for example a rook capture is imminent.
 * 
 * <p>The goal of static evaluation is to estimate how good a board position is for the player to
 * move. It is how we calculate the values of the leaf nodes in the alpha-beta search. For example
 * there is the classic rule of thumb that a queen is worth 9 pawns, a rook is worth 5, and a
 * bishop and knight are each worth 3. This way to count material provides a good estimate; we also
 * use factors such as pawn structure, king safety, and piece activity. As a general rule the
 * fitness evaluations are in centipawns, with a pawn being worth 100. See {@link #fitness(Board)}
 * for more details.
 * 
 * @author Phil Leszczynski
 */
public class Evaluation {
  /**
   * Initializes an evaluation. Creates the transposition tables, sets the material weights for
   * each piece, sets the material weight for the starting position, and sets several helper
   * variables related to king safety.
   */
  public Evaluation() {
    this.fitnessPiece.put(Piece.BISHOP, 333f);
    this.fitnessPiece.put(Piece.KING, 1000000f);
    this.fitnessPiece.put(Piece.KNIGHT, 320f);
    this.fitnessPiece.put(Piece.PAWN, 100f);
    this.fitnessPiece.put(Piece.QUEEN, 880f);
    this.fitnessPiece.put(Piece.ROOK, 510f);

    this.fitnessStartNoKing = 2 * fitnessPiece.get(Piece.ROOK)
        + 2 * fitnessPiece.get(Piece.KNIGHT) + 2 * fitnessPiece.get(Piece.BISHOP)
        + fitnessPiece.get(Piece.QUEEN) + 8 * fitnessPiece.get(Piece.PAWN);
    
    this.pawnShieldQueenside = new HashMap<Color, Bitboard>();
    this.pawnShieldQueensideForward = new HashMap<Color, Bitboard>();
    this.pawnShieldKingside = new HashMap<Color, Bitboard>();
    this.pawnShieldKingsideForward = new HashMap<Color, Bitboard>();

    this.pawnShieldQueenside.put(Color.WHITE, new Bitboard("a2", "b2", "c2"));
    this.pawnShieldQueensideForward.put(Color.WHITE, new Bitboard("a3", "b3", "c3"));
    this.pawnShieldKingside.put(Color.WHITE, new Bitboard("f2", "g2", "h2"));
    this.pawnShieldKingsideForward.put(Color.WHITE, new Bitboard("f3", "g3", "h3"));

    this.pawnShieldQueenside.put(Color.BLACK, this.pawnShieldQueenside.get(Color.WHITE).flip());
    this.pawnShieldQueensideForward.put(Color.BLACK,
        this.pawnShieldQueensideForward.get(Color.WHITE).flip());
    this.pawnShieldKingside.put(Color.BLACK, this.pawnShieldKingside.get(Color.WHITE).flip());
    this.pawnShieldKingsideForward.put(Color.BLACK,
        this.pawnShieldKingsideForward.get(Color.WHITE).flip());
    
    this.pawnKingHashTable = new PawnKingHashTable(pawnKingTableSize);
  }
  
  /**
   * Returns the fraction of the opponent's material that has been removed. At the start of the game
   * this returns 1.0f; if the opponent has just a king left this returns 0.0f.
   * 
   * @param board the board to evaluate the endgame fraction
   * @return the fraction of the way we are into the endgame, judging by the fraction of the
   *         opponent's material that is still on the board
   */
  public float endgameFraction(Board board) {
    float material = 0;
    for (Map.Entry<Piece, Float> entry : this.fitnessPiece.entrySet()) {
      Piece piece = entry.getKey();
      if (piece == Piece.KING) {
        continue;
      }
      int pieceCount = board.bitboards.get(Color.flip(board.turn)).get(piece).numOccupied();
      material += pieceCount * this.fitnessPiece.get(piece);
    }
    return 1 - material / this.fitnessStartNoKing;
  }
  
  /**
   * Returns an evaluation of the given player's king safety in centipawns. As a general rule the
   * king should be near one of the home corners early in the game because it is safer there. But if
   * the opponent has little material remaining the king should be active and close to the center of
   * the board. Additionally we have a bonus for an intact pawn shield in front of a castled king
   * early on in the game as this hinders the opponent's attack.
   * 
   * @param board the board to evaluate the king safety
   * @param color the player whose king safety to evaluate
   * @param endgameFraction the endgame fraction as calculated in {@link #endgameFraction(Board)}.
   *        Performance is important here and we want to save resources on the calculation when
   *        possible.
   * @return the given player's king safety score in centipawns
   */
  public float fitnessKingSafety(Board board, Color color, float endgameFraction) {
    int distanceFromHomeRank = 0;
    int kingIndex = board.bitboards.get(color).get(Piece.KING).numEmptyStartingSquares();
    if (color == Color.WHITE) {
      distanceFromHomeRank = (int) (kingIndex / 8);
    } else {
      distanceFromHomeRank = 7 - (int) (kingIndex / 8);
    }
    float rankFitness =
        -this.fitnessKingRankFactor * distanceFromHomeRank * (0.6f - endgameFraction);
    float fileFitness = this.fitnessKingFile[kingIndex % 8] * (0.6f - endgameFraction);

    float openFilePenalty = 0;
    float pawnShieldPenalty = 0;
    if (endgameFraction < 0.7) {
      int protectorsHome = 3;
      int protectorsOneStep = 0;
      if (color == Color.WHITE) {
        if (kingIndex % 8 <= 2) {
          protectorsHome = board.bitboards.get(color).get(Piece.PAWN)
              .intersection(pawnShieldQueenside.get(color)).numOccupied();
          protectorsOneStep = board.bitboards.get(color).get(Piece.PAWN)
              .intersection(pawnShieldQueensideForward.get(color)).numOccupied();
        } else if (kingIndex % 8 >= 5) {
          protectorsHome = board.bitboards.get(color).get(Piece.PAWN)
              .intersection(pawnShieldKingside.get(color)).numOccupied();
          protectorsOneStep = board.bitboards.get(color).get(Piece.PAWN)
              .intersection(pawnShieldKingsideForward.get(color)).numOccupied();
        }
      }
      if (protectorsHome + protectorsOneStep == 2) {
        pawnShieldPenalty = 25 * protectorsHome + 50 * protectorsOneStep;
      } else if (protectorsHome + protectorsOneStep == 1) {
        pawnShieldPenalty = 50 * protectorsHome + 75 * protectorsOneStep;
      } else if (protectorsHome + protectorsOneStep == 0) {
        pawnShieldPenalty = 150;
      }
      pawnShieldPenalty *= (1 - endgameFraction);

      if (kingIndex % 8 <= 2 || kingIndex % 8 >= 5) {
        // Don't have an open file penalty before castling, as we may get opportunities to
        // capture pawns in the center.
        Bitboard file = Bitboard.bitboardFromFile(kingIndex % 8);
        if (!board.bitboards.get(color).get(Piece.PAWN).intersects(file)) {
          openFilePenalty = 150 * (1 - endgameFraction);
        }
      }
    }

    return rankFitness + fileFitness - pawnShieldPenalty - openFilePenalty;
  }
  
  /**
   * Returns an evaluation of the given player's rook placement bonus on open files. As a rule of
   * thumb a rook is more powerful on an open file containing no pawns as it controls a lot of
   * squares. It is also powerful on a semi-open file containing only opposing pawns, as it applies
   * pressure preventing the pawn(s) from advancing.
   * 
   * @param board the board to evaluate rook placement on open files
   * @param color the player whose rook placement to evaluate
   * @param endgameFraction the endgame fraction as calculated in {@link #endgameFraction(Board)}.
   *        Performance is important here and we want to save resources on the calculation when
   *        possible.
   * @return the given player's rook open file bonus in centipawns
   */
  public float fitnessRookFiles(Board board, Color color, float endgameFraction) {
    // Assign a bonus for a rook being on an open file (one with no pawns)
    // or a semi-open file (one with only enemy pawns).
    float result = 0;
    long rooks = board.bitboards.get(color).get(Piece.ROOK).getData();
    long myPawns = board.bitboards.get(color).get(Piece.PAWN).getData();
    long oppPawns = board.bitboards.get(Color.flip(color)).get(Piece.PAWN).getData();
    while (rooks != 0) {
      int rookIndex = Long.numberOfTrailingZeros(rooks);
      long rook = 1L << rookIndex;
      rooks ^= rook;
      Bitboard rookFile = Bitboard.bitboardFromFile(rookIndex % 8);
      if (!rookFile.intersects(myPawns)) {
        if (!rookFile.intersects(oppPawns)) {
          result += this.fitnessRookOpenFile;
        } else {
          result += this.fitnessRookSemiOpenFile;
        }
      }
    }
    return result;
  }
  
  /**
   * Returns an evaluation of the given player's bonus for retaining castling rights, as well as
   * pawn shield integrity on the respective flanks. The right to castle is important early in the
   * game as it puts the king in safety and helps activate the rook. We also take the pawn shield on
   * the wings into account, since castling would not be as useful if the king is vulnerable to
   * attack.
   * 
   * @param board the board to evaluate castling rights
   * @param color the player whose castling rights to evaluate
   * @param endgameFraction the endgame fraction as calculated in {@link #endgameFraction(Board)}.
   *        Performance is important here and we want to save resources on the calculation when
   *        possible.
   * @return the given player's castle rights bonus in centipawns
   */
  public float fitnessCastleRights(Board board, Color color, float endgameFraction) {
    if (endgameFraction > 0.5) {
      return 0;
    }

    float result = 0;
    boolean castleRightQueenside = board.castleRights.get(color).get(Castle.QUEENSIDE);
    boolean castleRightKingside = board.castleRights.get(color).get(Castle.KINGSIDE);
    if (castleRightQueenside) {
      result += this.fitnessCastleRightQueenside;
    }
    if (castleRightKingside) {
      result += this.fitnessCastleRightKingside;
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
   * Returns the static evaluation fitness score for a given board. In general we evaluate various
   * bonuses and penalties both for the side to move and the opponent. The result is the score from
   * the player to move's perspective, i.e. the player's fitness minus the opponent's fitness.
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
    for (Map.Entry<Piece, Float> entry : this.fitnessPiece.entrySet()) {
      Piece piece = entry.getKey();
      if (piece == Piece.PAWN) {
        continue;
      }
      int myPieceCount = board.bitboards.get(board.turn).get(piece).numOccupied();
      int oppPieceCount = board.bitboards.get(turnFlipped).get(piece).numOccupied();
      fitness += (myPieceCount - oppPieceCount) * this.fitnessPiece.get(piece);
      if (piece == Piece.BISHOP) {
        if (myPieceCount >= 2) {
          fitness += this.fitnessBishopPairBonus;
        }
        if (oppPieceCount >= 2) {
          fitness -= this.fitnessBishopPairBonus;
        }
      }
    }

    float endgameFraction = this.endgameFraction(board);

    Bitboard pawnBitboardRelativeToMe = board.bitboards.get(board.turn).get(Piece.PAWN);
    Bitboard pawnBitboardRelativeToOpp = board.bitboards.get(turnFlipped).get(Piece.PAWN);
    if (board.turn == Color.WHITE) {
      pawnBitboardRelativeToOpp = pawnBitboardRelativeToOpp.flip();
    } else {
      pawnBitboardRelativeToMe = pawnBitboardRelativeToMe.flip();
    }

    PawnKingHashTable.PawnHashTableEntry entry =
        this.pawnKingHashTable.get(board.positionHashPawnsKings);
    if (entry == null) {
      this.pawnKingHashTable.put(board.positionHashPawnsKings,
          board.bitboards.get(Color.WHITE).get(Piece.PAWN).getData(),
          board.bitboards.get(Color.BLACK).get(Piece.PAWN).getData(),
          board.bitboards.get(Color.WHITE).get(Piece.KING).numEmptyStartingSquares(),
          board.bitboards.get(Color.BLACK).get(Piece.KING).numEmptyStartingSquares());
      entry = this.pawnKingHashTable.get(board.positionHashPawnsKings);
    }

    float doubledPawnPenalty = 15 * (entry.numDoubledPawnsWhite - entry.numDoubledPawnsBlack);
    float isolatedPawnPenalty = 15 * (entry.numIsolatedPawnsWhite - entry.numIsolatedPawnsBlack);
    float passedPawnBonus = 30 * (entry.numPassedPawnsWhite - entry.numPassedPawnsBlack);
    if (board.turn == Color.BLACK) {
      doubledPawnPenalty *= -1;
      isolatedPawnPenalty *= -1;
      passedPawnBonus *= -1;
    }
    fitness -= doubledPawnPenalty;
    fitness -= isolatedPawnPenalty;
    fitness += passedPawnBonus;

    // Since pawns can't be on the edge ranks.
    for (int rank = 1; rank < 7; rank++) {
      Bitboard rankBitboard = Bitboard.bitboardFromRank(rank);
      for (int centrality = 0; centrality < 4; centrality++) {
        Bitboard centralityBitboard =
            Bitboard.bitboardFromFile(centrality).union(Bitboard.bitboardFromFile(8 - centrality));
        float pawnFactor =
            (1 - endgameFraction) * this.fitnessPawnTableOpening[rank][centrality];
        pawnFactor += endgameFraction * this.fitnessPawnTableEndgame[rank][centrality];

        int myPawnsOnRank = pawnBitboardRelativeToMe.intersection(rankBitboard)
            .intersection(centralityBitboard).numOccupied();
        int oppPawnsOnRank = pawnBitboardRelativeToOpp.intersection(rankBitboard)
            .intersection(centralityBitboard).numOccupied();

        fitness += pawnFactor * (myPawnsOnRank - oppPawnsOnRank);
      }
    }

    fitness += this.fitnessKingSafety(board, board.turn, endgameFraction)
        - this.fitnessKingSafety(board, turnFlipped, endgameFraction);
    fitness += this.fitnessRookFiles(board, board.turn, endgameFraction)
        - this.fitnessRookFiles(board, turnFlipped, endgameFraction);
    fitness += this.fitnessCastleRights(board, board.turn, endgameFraction)
        - this.fitnessCastleRights(board, turnFlipped, endgameFraction);
    
    // The engine should try to checkmate as quickly as possible or delay being checkmated as long
    // as possible. Do this by penalizing the winning side for a checkmate by the number of moves
    // in the game.
    if(board.bitboards.get(board.turn).get(Piece.KING).isEmpty()) {
      fitness += board.fullMoveCounter * FITNESS_MOVE;
    }
    if(board.bitboards.get(Color.flip(board.turn)).get(Piece.KING).isEmpty()) {
      fitness -= board.fullMoveCounter * FITNESS_MOVE;
    }

    return fitness;
  }

  public static float FITNESS_LARGE = 1000000000;
  // To checkmate as quickly as possible, put in a penalty for waiting.
  public static float FITNESS_MOVE = 10000;
  
  private Map<Piece, Float> fitnessPiece = new HashMap<Piece, Float>();
  private float fitnessStartNoKing = 0;

  private float fitnessRookOpenFile = 50;
  private float fitnessRookSemiOpenFile = 25;

  private float fitnessCastleRightQueenside = 15;
  private float fitnessCastleRightKingside = 30;

  private float fitnessBishopPairBonus = 50;

  // It goes as [rank][centrality]. rank goes from 0 to 7 and is from the
  // perspective of that player. centrality goes from 0 (files a, h) to 3
  // (files d, e).
  private float[][] fitnessPawnTableOpening =
      {{0, 0, 0, 0}, {90, 95, 105, 110}, {90, 95, 105, 115}, {90, 95, 110, 120},
          {97, 103, 117, 127}, {106, 112, 125, 140}, {117, 122, 134, 159}, {0, 0, 0, 0}};
  private float[][] fitnessPawnTableEndgame =
      {{0, 0, 0, 0}, {120, 105, 95, 90}, {120, 105, 95, 90}, {125, 110, 100, 95},
          {133, 117, 107, 100}, {145, 129, 116, 105}, {161, 146, 127, 110}, {0, 0, 0, 0}};
  private float fitnessKingRankFactor = 75;
  private float[] fitnessKingFile = {0, 0, -90, -180, -180, -90, 0, 0};
  
  private PawnKingHashTable pawnKingHashTable = null;
  private static int pawnKingTableSize = 64 * 1024;
  
  private Map<Color, Bitboard> pawnShieldQueenside;
  private Map<Color, Bitboard> pawnShieldQueensideForward;
  private Map<Color, Bitboard> pawnShieldKingside;
  private Map<Color, Bitboard> pawnShieldKingsideForward;
}
