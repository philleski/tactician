package tactician.Test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tactician.Board;
import tactician.PositionHasher;

/**
 * This class tests the position hash functionality tracked by {@link Board} and implemented in
 * {@link PositionHasher}. This is important to test well since errors lead to silent bugs that
 * accumulate over time. The engine would be able to play as effectively but would not be as fast
 * since the hit rate on the transposition table would be dramatically reduced. Basically the goal
 * of these tests is to ensure that if a position was arrived at in two different ways then they
 * both have the same position hash.
 * 
 * @author Phil Leszczynski
 */
public class TestPositionHasher {
  /** Tests that when a board is cloned the position hash is copied. */
  @Test
  public void testPositionHashCopy() {
    Board board1 = new Board();
    board1.move("e2", "e4");
    board1.move("a7", "a5");
    Board board2 = new Board(board1);
    assertEquals(board1.positionHash, board2.positionHash);
  }

  /**
   * Tests that the same position hash arises even when a capture is made on two different squares.
   */
  @Test
  public void testPositionHashCapture() {
    Board board1 = new Board();
    board1.move("e2", "e4");
    board1.move("d7", "d6");
    board1.move("e4", "e5");
    board1.move("a7", "a6");
    board1.move("e5", "d6");
    Board board2 = new Board();
    board2.move("e2", "e4");
    board2.move("d7", "d5");
    board2.move("e4", "d5");
    board2.move("a7", "a6");
    board2.move("d5", "d6");
    assertEquals(board1.positionHash, board2.positionHash);
  }

  /** Tests that the same position hash arises with a different move order of the knight. */
  @Test
  public void testPositionHashKnight() {
    Board board1 = new Board();
    board1.move("a2", "a3");
    board1.move("g8", "f6");
    board1.move("b1", "c3");
    Board board2 = new Board();
    board2.move("b1", "c3");
    board2.move("g8", "f6");
    board2.move("a2", "a3");
    assertEquals(board1.positionHash, board2.positionHash);
  }

  /** Tests that the same position hash arises with a different move order of the bishop. */
  @Test
  public void testPositionHashBishop() {
    Board board1 = new Board();
    board1.move("e2", "e4");
    board1.move("e7", "e5");
    board1.move("f1", "c4");
    board1.move("f8", "c5");
    board1.move("c4", "b5");
    board1.move("c5", "b4");
    Board board2 = new Board();
    board2.move("e2", "e4");
    board2.move("e7", "e5");
    board2.move("f1", "b5");
    board2.move("f8", "b4");
    assertEquals(board1.positionHash, board2.positionHash);
  }

  /** Tests that the same position hash arises with a different move order of the rook. */
  @Test
  public void testPositionHashRook() {
    Board board1 = new Board();
    board1.move("a2", "a4");
    board1.move("a7", "a5");
    board1.move("a1", "a2");
    board1.move("a8", "a7");
    board1.move("a2", "a3");
    board1.move("a7", "a6");
    Board board2 = new Board();
    board2.move("a2", "a4");
    board2.move("a7", "a5");
    board2.move("a1", "a3");
    board2.move("a8", "a6");
    assertEquals(board1.positionHash, board2.positionHash);
  }

  /** Tests that the same position hash arises with a different move order of the queen. */
  @Test
  public void testPositionHashQueen() {
    Board board1 = new Board();
    board1.move("e2", "e4");
    board1.move("a7", "a5");
    board1.move("d1", "h5");
    board1.move("a5", "a4");
    board1.move("h5", "f7");
    Board board2 = new Board();
    board2.move("e2", "e4");
    board2.move("a7", "a5");
    board2.move("d1", "f3");
    board2.move("a5", "a4");
    board2.move("f3", "f7");
    assertEquals(board1.positionHash, board2.positionHash);
  }

  /** Tests that the same position hash arises with a different move order of the king. */
  @Test
  public void testPositionHashKing() {
    Board board1 = new Board();
    board1.move("e2", "e4");
    board1.move("a7", "a5");
    board1.move("d2", "d4");
    board1.move("a5", "a4");
    board1.move("e1", "e2");
    Board board2 = new Board();
    board2.move("e2", "e4");
    board2.move("a7", "a6");
    board2.move("d2", "d4");
    board2.move("a6", "a5");
    board2.move("e1", "d2");
    board2.move("a5", "a4");
    board2.move("d2", "e2");
    assertEquals(board1.positionHash, board2.positionHash);
  }

  /**
   * Tests that the same position hash arises whether the player actually castles or artificially
   * castles. Artificial castling is when the king and rook maneuver to the castled position without
   * actually performing the castling move.
   */
  @Test
  public void testPositionHashCastle() {
    Board board1 = new Board();
    board1.move("e2", "e4");
    board1.move("a7", "a5");
    board1.move("f1", "d3");
    board1.move("a5", "a4");
    board1.move("g1", "f3");
    board1.move("a4", "a3");
    board1.move("e1", "g1");
    Board board2 = new Board();
    board2.move("e2", "e4");
    board2.move("a7", "a5");
    board2.move("f1", "d3");
    board2.move("a5", "a4");
    board2.move("g1", "f3");
    board2.move("a4", "a3");
    board2.move("e1", "e2");
    board2.move("b8", "c6");
    board2.move("h1", "e1");
    board2.move("c6", "b8");
    board2.move("e2", "f1");
    board2.move("b8", "c6");
    board2.move("f1", "g1");
    board2.move("c6", "b8");
    board2.move("e1", "f1");
    assertEquals(board1.positionHash, board2.positionHash);
  }
}
