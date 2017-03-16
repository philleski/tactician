package tactician.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import chess_engine.Board;

public class TestLegalMoveGenerator {
	@Test
	public void testIsInCheck() {
		Board board = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBqK w KQkq - 0 1");
		assertTrue(board.isInCheck());
	}
	
	@Test
	public void testIsNotInCheck() {
		Board board = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
		assertTrue(!board.isInCheck());
	}
}
