package tactician.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import tactician.Board;
import tactician.Brain;
import tactician.Move;

/**
 * This class tests the functionality of the {@link Brain} class.
 * 
 * @author Phil Leszczynski
 */
public class TestBrain {
  /**
   * Tests basic move calculation and ensures that the engine can checkmate the opponent in one move
   * if the opportunity exists.
   */
  @Test
  public void testMateInOneWinning() {
    Board board = new Board();
    board.move("f2", "f3");
    board.move("e7", "e5");
    board.move("g2", "g4");
    Brain brain = new Brain();
    Move move = brain.getMove(board);
    assertEquals(move.toString(), "d8h4");
  }
  
  /**
   * Makes sure that checkmate is pursued immediately rather than having a mate with the rook in a
   * more favorable position, for example.
   */
  @Test
  public void testMateQuickly() {
    Board board = new Board("4K3/7r/5p2/5k2/8/8/1r6/8 b KQkq - 0 1");
    Brain brain = new Brain();
    Move move = brain.getMove(board);
    assertEquals(move.toString(), "b2b8");
  }
  
  /**
   * Makes sure we don't overflow the bounds of the transposition table. This position resulted in
   * a bug where the transposition table index overflowed its data structure.
   */
  @Test
  public void testTranspositionTableBounds() {
    Board board = new Board("Bn5r/p3k2p/b5p1/4p3/3q4/2nP3Q/PPP2PPP/R3KR2 w Q - 5 19");
    Brain brain = new Brain();
    Move move = brain.getMove(board);
    assertNotNull(move);
  }

  /**
   * Tests to make sure the engine can still make a move even if it will be checkmated next move.
   * This is to prevent a common bug where no move is played because essentially the initial score
   * is negative infinity and each move's score is also negative infinity and no best move is found.
   */
  @Test
  public void testMateInOneLosing() {
    Board board = new Board("8/8/8/8/8/7k/q7/7K w KQkq - 0 1");
    Brain brain = new Brain();
    Move move = brain.getMove(board);
    assertNotNull(move);
  }

  /**
   * Tests slightly higher reasoning where the engine forgoes an immediate advantage for a better
   * advantage later. In this case a weak engine or player would capture the c6 pawn for immediate
   * material advantage, but actually lose material after the knight is recaptured. Instead the
   * engine should see three plies in advance that moving the knight to c7 forks the king and rook,
   * and that the high-valued rook is captured next move.
   */
  @Test
  public void testFork() {
    Board board = new Board();
    board.move("b1", "c3");
    board.move("d7", "d5");
    board.move("c3", "b5");
    board.move("d8", "h4");
    board.move("h2", "h3");
    board.move("c7", "c6");
    Brain brain = new Brain();
    Move move = brain.getMove(board);
    assertEquals(move.toString(), "b5c7");
  }
}
