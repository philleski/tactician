package tactician.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import tactician.Board;

/**
 * This class tests the functionality of the {@link Board} class.
 * 
 * @author Phil Leszczynski
 */
public class TestBoard {
	/**
	 * Tests the full move counter when white is the last to move. The full move counter should
	 * start at 1 and increment every time black moves.
	 */
	@Test
	public void testFullMoveCounterWhite() {
		Board board = new Board();
		board.move("e2", "e4");
		board.move("e7", "e5");
		board.move("d2", "d4");
		board.move("d7", "d5");
		board.move("a2", "a3");
		assertEquals(board.fullMoveCounter, 3);
	}
	
	/**
	 * Tests the full move counter when black is the last to move. The full move counter should
	 * start at 1 and increment every time black moves.
	 */
	@Test
	public void testFullMoveCounterBlack() {
		Board board = new Board();
		board.move("e2", "e4");
		board.move("e7", "e5");
		board.move("d2", "d4");
		board.move("d7", "d5");
		board.move("a2", "a3");
		board.move("a7", "a6");
		assertEquals(board.fullMoveCounter, 4);
	}
	
	/** Tests that the full move counter is copied over when a board is cloned. */
	@Test
	public void testFullMoveCounterCopy() {
		Board board1 = new Board();
		board1.move("e2", "e4");
		board1.move("e7", "e5");
		Board board2 = new Board(board1);
		assertEquals(board2.fullMoveCounter, 2);
	}
	
	/**
	 * Tests that when a board is initialized from a FEN string, the full move counter is copied
	 * over correctly.
	 */
	@Test
	public void testFullMoveFEN() {
		Board board = new Board("4k3/8/8/8/8/8/8/8/4K3 b KQkq - 16 16");
		assertEquals(board.fullMoveCounter, 16);
	}
}
