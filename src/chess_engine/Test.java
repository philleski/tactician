package chess_engine;

import static org.junit.Assert.*;

public class Test {
	@org.junit.Test
	public void testPositionHashCopy() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("a7", "a5"));
		Board board2 = new Board(board1);
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	@org.junit.Test
	public void testPositionHashCapture() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("d7", "d6"));
		board1.move(new Move("e4", "e5"));
		board1.move(new Move("a7", "a6"));
		board1.move(new Move("e5", "d6"));
		Board board2 = new Board();
		board2.move(new Move("e2", "e4"));
		board2.move(new Move("d7", "d5"));
		board2.move(new Move("e4", "d5"));
		board2.move(new Move("a7", "a6"));
		board2.move(new Move("d5", "d6"));
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	@org.junit.Test
	public void testPositionHashKnight() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("a2", "a3"));
		board1.move(new Move("g8", "f6"));
		board1.move(new Move("b1", "c3"));
		Board board2 = new Board();
		board2.move(new Move("b1", "c3"));
		board2.move(new Move("g8", "f6"));
		board2.move(new Move("a2", "a3"));
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	@org.junit.Test
	public void testPositionHashBishop() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("e7", "e5"));
		board1.move(new Move("f1", "c4"));
		board1.move(new Move("f8", "c5"));
		board1.move(new Move("c4", "b5"));
		board1.move(new Move("c5", "b4"));
		Board board2 = new Board();
		board2.move(new Move("e2", "e4"));
		board2.move(new Move("e7", "e5"));
		board2.move(new Move("f1", "b5"));
		board2.move(new Move("f8", "b4"));
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	// FIXME - keep adding position hash tests

}
