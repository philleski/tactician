package tactician.Test;

import static org.junit.Assert.*;
import org.junit.Test;
import tactician.Board;
import tactician.Color;
import tactician.Evaluation;

/**
 * This class tests the functionality of the {@link Evaluation} class.
 * 
 * @author Phil Leszczynski
 */
public class TestEvaluation {
  /**
   * Tests the endgame fraction at the start of the game. The endgame fraction should be 0 at the
   * start and 1 if only both kings are left on the board.
   */
  @Test
  public void testEndgameFractionStart() {
    Board board = new Board();
    Evaluation evaluation = new Evaluation();
    float endgameFraction = evaluation.endgameFraction(board);
    assertTrue(-0.01 < endgameFraction && endgameFraction < 0.01);
  }

  /**
   * Tests the endgame fraction at the end of the game. The endgame fraction should be 0 at the
   * start and 1 if only both kings are left on the board.
   */
  @Test
  public void testEndgameFractionEnd() {
    Board board = new Board("4k3/8/8/8/8/8/8/8/4K3 b KQkq - 1 0 1");
    Evaluation evaluation = new Evaluation();
    float endgameFraction = evaluation.endgameFraction(board);
    assertTrue(0.99 < endgameFraction && endgameFraction < 1.01);
  }

  /**
   * Tests the king safety near the beginning of the game. In the opening the king is safest near
   * one of its home corner and less safe near the center of the board. Ensures that a home corner
   * is safest, followed by the center of the home row, followed by the side of the board, followed
   * by the center of the board.
   */
  @Test
  public void testFitnessKingSafetyOpening() {
    Evaluation evaluation = new Evaluation();
    Board homeCorner = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
    float homeCornerSafety = evaluation.fitnessKingSafety(homeCorner, Color.WHITE, 0);
    Board homeEdge = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    float homeEdgeSafety = evaluation.fitnessKingSafety(homeEdge, Color.WHITE, 0);
    Board sideEdge = new Board("rnbqrbnk/pppppppp/8/8/7K/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1");
    float sideEdgeSafety = evaluation.fitnessKingSafety(sideEdge, Color.WHITE, 0);
    Board center = new Board("rnbqrbnk/pppppppp/8/8/4K3/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1");
    float centerSafety = evaluation.fitnessKingSafety(center, Color.WHITE, 0);
    assertTrue(homeCornerSafety > homeEdgeSafety);
    assertTrue(homeEdgeSafety > sideEdgeSafety);
    assertTrue(sideEdgeSafety > centerSafety);
  }

  /**
   * Tests the king safety near the end of the game. In the endgame the king is most effective close
   * to the center of the board and less effective near its home corners. Ensures that the center
   * scores highest, followed by the side of the board, followed by the home row, followed by a home
   * corner.
   */
  @Test
  public void testFitnessKingSafetyEndgame() {
    Evaluation evaluation = new Evaluation();
    Board homeCorner = new Board("k7/p7/8/8/8/8/P7/K7 w KQkq - 0 1");
    float homeCornerSafety = evaluation.fitnessKingSafety(homeCorner, Color.WHITE, 1);
    Board homeEdge = new Board("k7/p7/8/8/8/8/P7/3K4 w KQkq - 0 1");
    float homeEdgeSafety = evaluation.fitnessKingSafety(homeEdge, Color.WHITE, 1);
    Board sideEdge = new Board("k7/p7/8/8/K7/8/P7/8 w KQkq - 0 1");
    float sideEdgeSafety = evaluation.fitnessKingSafety(sideEdge, Color.WHITE, 1);
    Board center = new Board("k7/p7/8/8/3K4/8/P7/8 w KQkq - 0 1");
    float centerSafety = evaluation.fitnessKingSafety(center, Color.WHITE, 1);
    assertTrue(homeCornerSafety < homeEdgeSafety);
    assertTrue(homeEdgeSafety < sideEdgeSafety);
    assertTrue(sideEdgeSafety < centerSafety);
  }

  /**
   * Tests the integrity of a pawn shield in front of a king. In general at the beginning and middle
   * of the game the king should be protected by a wall of pawns making it difficult for the
   * opponent to build up an attack. Ensures that a perfectly formed wall is best, followed by a
   * mostly intact wall, followed by a completely exposed king.
   */
  @Test
  public void testFitnessPawnShield() {
    Evaluation evaluation = new Evaluation();
    Board shielded = new Board("6k1/pppppppp/8/8/8/8/5PPP/6K1 w KQkq - 0 1");
    float shieldedSafety = evaluation.fitnessKingSafety(shielded, Color.WHITE, 0.3f);
    Board forward = new Board("6k1/pppppppp/8/8/6P1/8/5P1P/6K1 w KQkq - 0 1");
    float forwardSafety = evaluation.fitnessKingSafety(forward, Color.WHITE, 0.3f);
    Board exposed = new Board("6k1/pppppppp/8/8/8/8/5P1P/6K1 w KQkq - 0 1");
    float exposedSafety = evaluation.fitnessKingSafety(exposed, Color.WHITE, 0.3f);
    assertTrue(shieldedSafety > forwardSafety);
    assertTrue(forwardSafety > exposedSafety);
  }

  /**
   * Tests the penalty for doubled pawns. Doubled pawns occur when there is more than one friendly
   * pawn on the same file. They are less effective because they are typically more vulnerable and
   * they cannot both threaten to promote. Ensures that a 3-pawn position with doubled pawns scores
   * lower than a 3-pawn position with pawns on separate files.
   */
  @Test
  public void testFitnessDoubledPawn() {
    Evaluation evaluation = new Evaluation();
    Board doubled = new Board("7k/8/8/8/8/P7/PP6/7K w KQkq - 0 1");
    float doubledFitness = evaluation.fitness(doubled);
    Board control = new Board("7k/8/8/8/8/P7/1PP5/7K w KQkq - 0 1");
    float controlFitness = evaluation.fitness(control);
    assertTrue(doubledFitness < controlFitness);
  }

  /**
   * Tests the penalty for isolated pawns. Isolated pawns occur when a pawn has no friendly
   * neighbors on adjacent files. They are typically more vulnerable since they lack the benefit of
   * a pawn chain for protection. Ensures that a 2-pawn position with isolated pawns scores lower
   * than a 2-pawn position with pawns on neighboring files.
   */
  @Test
  public void testFitnessIsolatedPawn() {
    Evaluation evaluation = new Evaluation();
    Board isolated = new Board("7k/8/8/8/8/8/P1P5/7K w KQkq - 0 1");
    float isolatedFitness = evaluation.fitness(isolated);
    Board control = new Board("7k/8/8/8/8/8/PP6/7K w KQkq - 0 1");
    float controlFitness = evaluation.fitness(control);
    assertTrue(isolatedFitness < controlFitness);
  }

  /**
   * Tests the bonus for passed pawns. Passed pawns are those that cannot be stopped from promoting
   * by enemy pawns. In other words, none of the squares in a the column in front of the pawn, nor
   * the squares adjacent to that column, are occupid by enemy pawns. Passed pawns are valuable
   * because they divert the opponent's pieces to blocking them, putting the opponent into a
   * defensive state. They also simply have a higher chance of promoting. Ensures that a 2vs1 pawn
   * position with a passed pawn scores better than a 2vs1 position where neither pawn is passed.
   */
  @Test
  public void testFitnessPassedPawn() {
    Evaluation evaluation = new Evaluation();
    Board passed = new Board("7k/8/8/8/2p5/8/P1P5/7K w KQkq - 0 1");
    float passedFitness = evaluation.fitness(passed);
    Board control = new Board("7k/8/8/8/1p6/8/P1P5/7K w KQkq - 0 1");
    float cleanFitness = evaluation.fitness(control);
    assertTrue(passedFitness > cleanFitness);
  }

  /**
   * Tests the bonus for castling rights. Castling rights are beneficial since they give the player
   * the option of moving the king to safety and activating a rook. Ensures that having both
   * castling rights is best, followed by having one, followed by having none.
   */
  @Test
  public void testFitnessCastleRights() {
    Evaluation evaluation = new Evaluation();
    Board both = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
    float bothFitness = evaluation.fitness(both);
    Board kingside = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w Kkq - 0 1");
    float kingsideFitness = evaluation.fitness(kingside);
    Board queenside = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w Qkq - 0 1");
    float queensideFitness = evaluation.fitness(queenside);
    Board neither = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w kq - 0 1");
    float neitherFitness = evaluation.fitness(neither);
    assertTrue(bothFitness > kingsideFitness);
    assertTrue(bothFitness > queensideFitness);
    assertTrue(kingsideFitness > neitherFitness);
    assertTrue(queensideFitness > neitherFitness);
  }

  /**
   * Tests the potential to castle based on the pawn shield. For example if the pawn shield is
   * weakened on the kingside, then the option to castle there is not as valuable. Ensures that
   * having an intact pawn shield at a particular castling location scores higher than a broken pawn
   * shield.
   */
  @Test
  public void testFitnessCastleRightsPotential() {
    Evaluation evaluation = new Evaluation();
    Board brokenShield = new Board("rnbqrbnk/pppppppp/8/8/6P1/8/PPPPPP1P/RNBQRBNK w KQkq - 0 1");
    float brokenShieldFitness = evaluation.fitness(brokenShield);
    Board control = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
    float controlFitness = evaluation.fitness(control);
    assertTrue(brokenShieldFitness < controlFitness);
  }
  
  /**
   * Tests basic material advantage. Ensures that, absent compensating factors, the player with a
   * material advantage has a score above 0 and the player with a material disadvantage has a score
   * below 0.
   */
  @Test
  public void testFitness() {
    Board board = new Board();
    board.move("e2", "e4");
    board.move("d7", "d5");
    board.move("e4", "d5");
    Evaluation evaluation = new Evaluation();
    assertTrue(evaluation.fitness(board) < 0);
    board.move("h7", "h6");
    assertTrue(evaluation.fitness(board) > 0);
  }
}
