package chess_engine.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import chess_engine.Board;
import chess_engine.IllegalMoveException;
import chess_engine.Move;

public class TestBoard {
	@Test
	public void testFullMoveCounterWhite() throws IllegalMoveException {
		Board board = new Board();
		board.move(new Move("e2", "e4"));
		board.move(new Move("e7", "e5"));
		board.move(new Move("d2", "d4"));
		board.move(new Move("d7", "d5"));
		board.move(new Move("a2", "a3"));
		assertEquals(board.fullMoveCounter, 3);
	}
	
	@Test
	public void testFullMoveCounterBlack() throws IllegalMoveException {
		Board board = new Board();
		board.move(new Move("e2", "e4"));
		board.move(new Move("e7", "e5"));
		board.move(new Move("d2", "d4"));
		board.move(new Move("d7", "d5"));
		board.move(new Move("a2", "a3"));
		board.move(new Move("a7", "a6"));
		assertEquals(board.fullMoveCounter, 4);
	}
	
	@Test
	public void testFullMoveCounterCopy() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("e7", "e5"));
		Board board2 = new Board(board1);
		assertEquals(board2.fullMoveCounter, 2);
	}
	
	@Test
	public void testFullMoveFEN() throws IllegalMoveException {
		Board board = new Board();
		board.setPositionFenstring("4k3/8/8/8/8/8/8/8/4K3 b KQkq - 16 16");
		assertEquals(board.fullMoveCounter, 16);
	}
}
