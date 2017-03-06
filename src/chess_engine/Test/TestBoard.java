package chess_engine.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import chess_engine.Board;
import chess_engine.IllegalMoveException;

public class TestBoard {
	@Test
	public void testFullMoveCounterWhite() throws IllegalMoveException {
		Board board = new Board();
		board.move("e2", "e4");
		board.move("e7", "e5");
		board.move("d2", "d4");
		board.move("d7", "d5");
		board.move("a2", "a3");
		assertEquals(board.fullMoveCounter, 3);
	}
	
	@Test
	public void testFullMoveCounterBlack() throws IllegalMoveException {
		Board board = new Board();
		board.move("e2", "e4");
		board.move("e7", "e5");
		board.move("d2", "d4");
		board.move("d7", "d5");
		board.move("a2", "a3");
		board.move("a7", "a6");
		assertEquals(board.fullMoveCounter, 4);
	}
	
	@Test
	public void testFullMoveCounterCopy() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move("e2", "e4");
		board1.move("e7", "e5");
		Board board2 = new Board(board1);
		assertEquals(board2.fullMoveCounter, 2);
	}
	
	@Test
	public void testFullMoveFEN() throws IllegalMoveException {
		Board board = new Board("4k3/8/8/8/8/8/8/8/4K3 b KQkq - 16 16");
		assertEquals(board.fullMoveCounter, 16);
	}
}
