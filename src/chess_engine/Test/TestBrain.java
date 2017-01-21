package chess_engine.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import chess_engine.Board;
import chess_engine.Brain;
import chess_engine.Color;
import chess_engine.IllegalMoveException;
import chess_engine.Move;

public class TestBrain {
	@Test
	public void testEndgameFractionStart() {
		Board board = new Board();
		Brain brain = new Brain();
		float endgameFraction = brain.endgameFraction(board);
		assertTrue(-0.01 < endgameFraction && endgameFraction < 0.01);
	}
	
	@Test
	public void testEndgameFractionEnd() {
		Board board = new Board();
		board.setPositionFenstring("4k3/8/8/8/8/8/8/8/4K3 b KQkq - 1");
		Brain brain = new Brain();
		float endgameFraction = brain.endgameFraction(board);
		assertTrue(0.99 < endgameFraction && endgameFraction < 1.01);
	}
	
	@Test
	public void testFitnessKingSafetyOpening() {
		// In the opening the king is safest close to its home corner.
		Brain brain = new Brain();
		Board homeCorner = new Board();
		homeCorner.setPositionFenstring(
			"rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
		float homeCornerSafety = brain.fitnessKingSafety(
			homeCorner, Color.WHITE, 0);
		Board homeEdge = new Board();
		homeEdge.setPositionFenstring(
			"rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		float homeEdgeSafety = brain.fitnessKingSafety(
			homeEdge, Color.WHITE, 0);
		Board sideEdge = new Board();
		sideEdge.setPositionFenstring(
			"rnbqrbnk/pppppppp/8/8/7K/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1");
		float sideEdgeSafety = brain.fitnessKingSafety(
			sideEdge, Color.WHITE, 0);
		Board center = new Board();
		center.setPositionFenstring(
			"rnbqrbnk/pppppppp/8/8/4K3/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1");
		float centerSafety = brain.fitnessKingSafety(
			center, Color.WHITE, 0);
		assertTrue(homeCornerSafety > homeEdgeSafety);
		assertTrue(homeEdgeSafety > sideEdgeSafety);
		assertTrue(sideEdgeSafety > centerSafety);
	}
	
	@Test
	public void testFitnessKingSafetyEndgame() {
		// In the endgame the king is a fighting piece most effective in the center.
		Brain brain = new Brain();
		Board homeCorner = new Board();
		homeCorner.setPositionFenstring(
			"k7/p7/8/8/8/8/P7/K7 w KQkq - 0 1");
		float homeCornerSafety = brain.fitnessKingSafety(
			homeCorner, Color.WHITE, 1);
		Board homeEdge = new Board();
		homeEdge.setPositionFenstring(
			"k7/p7/8/8/8/8/P7/3K4 w KQkq - 0 1");
		float homeEdgeSafety = brain.fitnessKingSafety(
			homeEdge, Color.WHITE, 1);
		Board sideEdge = new Board();
		sideEdge.setPositionFenstring(
			"k7/p7/8/8/K7/8/P7/8 w KQkq - 0 1");
		float sideEdgeSafety = brain.fitnessKingSafety(
			sideEdge, Color.WHITE, 1);
		Board center = new Board();
		center.setPositionFenstring(
			"k7/p7/8/8/3K4/8/P7/8 w KQkq - 0 1");
		float centerSafety = brain.fitnessKingSafety(
			center, Color.WHITE, 1);
		assertTrue(homeCornerSafety < homeEdgeSafety);
		assertTrue(homeEdgeSafety < sideEdgeSafety);
		assertTrue(sideEdgeSafety < centerSafety);
	}
	
	@Test
	public void testFitnessPawnShield() {
		Brain brain = new Brain();
		Board shielded = new Board();
		shielded.setPositionFenstring(
			"6k1/pppppppp/8/8/8/8/5PPP/6K1 w KQkq - 0 1");
		float shieldedSafety = brain.fitnessKingSafety(
			shielded, Color.WHITE, 0.3f);
		Board forward = new Board();
		forward.setPositionFenstring(
			"6k1/pppppppp/8/8/6P1/8/5P1P/6K1 w KQkq - 0 1");
		float forwardSafety = brain.fitnessKingSafety(
			forward, Color.WHITE, 0.3f);
		Board exposed = new Board();
		exposed.setPositionFenstring(
			"6k1/pppppppp/8/8/8/8/5P1P/6K1 w KQkq - 0 1");
		float exposedSafety = brain.fitnessKingSafety(
			exposed, Color.WHITE, 0.3f);
		assertTrue(shieldedSafety > forwardSafety);
		assertTrue(forwardSafety > exposedSafety);
	}
	
	@Test
	public void testBrainMateInOne() throws IllegalMoveException {
		Board board = new Board();
		board.move(new Move("f2", "f3"));
		board.move(new Move("e7", "e5"));
		board.move(new Move("g2", "g4"));
		Brain brain = new Brain();
		Move move = brain.getMove(board);
		assertEquals(move.toString(), "d8h4");
	}
	
	@Test
	public void testFork() throws IllegalMoveException {
		Board board = new Board();
		board.move(new Move("b1", "c3"));
		board.move(new Move("d7", "d5"));
		board.move(new Move("c3", "b5"));
		board.move(new Move("d8", "h4"));
		// Move the c-pawn so the knight actually recognizes the fork instead
		// of just trying to win a pawn.
		board.move(new Move("h2", "h3"));
		board.move(new Move("c7", "c6"));
		Brain brain = new Brain();
		Move move = brain.getMove(board);
		assertEquals(move.toString(), "b5c7");
	}
	
	@Test
	public void testFitness() throws IllegalMoveException {
		Board board = new Board();
		board.move(new Move("e2", "e4"));
		board.move(new Move("d7", "d5"));
		board.move(new Move("e4", "d5"));
		Brain brain = new Brain();
		assertTrue(brain.fitness(board) < 0);
		board.move(new Move("h7", "h6"));
		assertTrue(brain.fitness(board) > 0);
	}
}
