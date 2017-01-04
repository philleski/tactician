package chess_engine.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import chess_engine.Board;
import chess_engine.IllegalMoveException;
import chess_engine.Move;

public class TestPositionHasher {
	@Test
	public void testPositionHashCopy() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("a7", "a5"));
		Board board2 = new Board(board1);
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	@Test
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
	
	@Test
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
	
	@Test
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
	
	@Test
	public void testPositionHashRook() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("a2", "a4"));
		board1.move(new Move("a7", "a5"));
		board1.move(new Move("a1", "a2"));
		board1.move(new Move("a8", "a7"));
		board1.move(new Move("a2", "a3"));
		board1.move(new Move("a7", "a6"));
		Board board2 = new Board();
		board2.move(new Move("a2", "a4"));
		board2.move(new Move("a7", "a5"));
		board2.move(new Move("a1", "a3"));
		board2.move(new Move("a8", "a6"));
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	@Test
	public void testPositionHashQueen() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("a7", "a5"));
		board1.move(new Move("d1", "h5"));
		board1.move(new Move("a5", "a4"));
		board1.move(new Move("h5", "f7"));
		Board board2 = new Board();
		board2.move(new Move("e2", "e4"));
		board2.move(new Move("a7", "a5"));
		board2.move(new Move("d1", "f3"));
		board2.move(new Move("a5", "a4"));
		board2.move(new Move("f3", "f7"));
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	@Test
	public void testPositionHashKing() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("a7", "a5"));
		board1.move(new Move("d2", "d4"));
		board1.move(new Move("a5", "a4"));
		board1.move(new Move("e1", "e2"));
		Board board2 = new Board();
		board2.move(new Move("e2", "e4"));
		board2.move(new Move("a7", "a6"));
		board2.move(new Move("d2", "d4"));
		board2.move(new Move("a6", "a5"));
		board2.move(new Move("e1", "d2"));
		board2.move(new Move("a5", "a4"));
		board2.move(new Move("d2", "e2"));
		assertEquals(board1.positionHash, board2.positionHash);
	}
	
	@Test
	public void testPositionHashCastle() throws IllegalMoveException {
		Board board1 = new Board();
		board1.move(new Move("e2", "e4"));
		board1.move(new Move("a7", "a5"));
		board1.move(new Move("f1", "d3"));
		board1.move(new Move("a5", "a4"));
		board1.move(new Move("g1", "f3"));
		board1.move(new Move("a4", "a3"));
		board1.move(new Move("e1", "g1"));
		Board board2 = new Board();
		board2.move(new Move("e2", "e4"));
		board2.move(new Move("a7", "a5"));
		board2.move(new Move("f1", "d3"));
		board2.move(new Move("a5", "a4"));
		board2.move(new Move("g1", "f3"));
		board2.move(new Move("a4", "a3"));
		board2.move(new Move("e1", "e2"));
		board2.move(new Move("b8", "c6"));
		board2.move(new Move("h1", "e1"));
		board2.move(new Move("c6", "b8"));
		board2.move(new Move("e2", "f1"));
		board2.move(new Move("b8", "c6"));
		board2.move(new Move("f1", "g1"));
		board2.move(new Move("c6", "b8"));
		board2.move(new Move("e1", "f1"));
		assertEquals(board1.positionHash, board2.positionHash);
	}
}
