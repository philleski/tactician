package tactician.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import chess_engine.Board;
import chess_engine.Brain;
import chess_engine.Color;
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
		Board board = new Board("4k3/8/8/8/8/8/8/8/4K3 b KQkq - 1 0 1");
		Brain brain = new Brain();
		float endgameFraction = brain.endgameFraction(board);
		assertTrue(0.99 < endgameFraction && endgameFraction < 1.01);
	}
	
	@Test
	public void testFitnessKingSafetyOpening() {
		// In the opening the king is safest close to its home corner.
		Brain brain = new Brain();
		Board homeCorner = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
		float homeCornerSafety = brain.fitnessKingSafety(homeCorner, Color.WHITE, 0);
		Board homeEdge = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		float homeEdgeSafety = brain.fitnessKingSafety(homeEdge, Color.WHITE, 0);
		Board sideEdge = new Board("rnbqrbnk/pppppppp/8/8/7K/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1");
		float sideEdgeSafety = brain.fitnessKingSafety(sideEdge, Color.WHITE, 0);
		Board center = new Board("rnbqrbnk/pppppppp/8/8/4K3/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1");
		float centerSafety = brain.fitnessKingSafety(center, Color.WHITE, 0);
		assertTrue(homeCornerSafety > homeEdgeSafety);
		assertTrue(homeEdgeSafety > sideEdgeSafety);
		assertTrue(sideEdgeSafety > centerSafety);
	}
	
	@Test
	public void testFitnessKingSafetyEndgame() {
		// In the endgame the king is a fighting piece most effective in the center.
		Brain brain = new Brain();
		Board homeCorner = new Board("k7/p7/8/8/8/8/P7/K7 w KQkq - 0 1");
		float homeCornerSafety = brain.fitnessKingSafety(homeCorner, Color.WHITE, 1);
		Board homeEdge = new Board("k7/p7/8/8/8/8/P7/3K4 w KQkq - 0 1");
		float homeEdgeSafety = brain.fitnessKingSafety(homeEdge, Color.WHITE, 1);
		Board sideEdge = new Board("k7/p7/8/8/K7/8/P7/8 w KQkq - 0 1");
		float sideEdgeSafety = brain.fitnessKingSafety(sideEdge, Color.WHITE, 1);
		Board center = new Board("k7/p7/8/8/3K4/8/P7/8 w KQkq - 0 1");
		float centerSafety = brain.fitnessKingSafety(center, Color.WHITE, 1);
		assertTrue(homeCornerSafety < homeEdgeSafety);
		assertTrue(homeEdgeSafety < sideEdgeSafety);
		assertTrue(sideEdgeSafety < centerSafety);
	}
	
	@Test
	public void testFitnessPawnShield() {
		Brain brain = new Brain();
		Board shielded = new Board("6k1/pppppppp/8/8/8/8/5PPP/6K1 w KQkq - 0 1");
		float shieldedSafety = brain.fitnessKingSafety(shielded, Color.WHITE, 0.3f);
		Board forward = new Board("6k1/pppppppp/8/8/6P1/8/5P1P/6K1 w KQkq - 0 1");
		float forwardSafety = brain.fitnessKingSafety(forward, Color.WHITE, 0.3f);
		Board exposed = new Board("6k1/pppppppp/8/8/8/8/5P1P/6K1 w KQkq - 0 1");
		float exposedSafety = brain.fitnessKingSafety(exposed, Color.WHITE, 0.3f);
		assertTrue(shieldedSafety > forwardSafety);
		assertTrue(forwardSafety > exposedSafety);
	}
	
	@Test
	public void testFitnessDoubledPawn() {
		Brain brain = new Brain();
		Board doubled = new Board("7k/8/8/8/8/P7/PP6/7K w KQkq - 0 1");
		float doubledFitness = brain.fitness(doubled);
		Board control = new Board("7k/8/8/8/8/P7/1PP5/7K w KQkq - 0 1");
		float controlFitness = brain.fitness(control);
		assertTrue(doubledFitness < controlFitness);
	}
	
	@Test
	public void testFitnessIsolatedPawn() {
		Brain brain = new Brain();
		Board isolated = new Board("7k/8/8/8/8/8/P1P5/7K w KQkq - 0 1");
		float isolatedFitness = brain.fitness(isolated);
		Board control = new Board("7k/8/8/8/8/8/PP6/7K w KQkq - 0 1");
		float controlFitness = brain.fitness(control);
		assertTrue(isolatedFitness < controlFitness);
	}
	
	@Test
	public void testFitnessPassedPawn() {
		Brain brain = new Brain();
		Board passed = new Board("7k/8/8/8/2p5/8/P1P5/7K w KQkq - 0 1");
		float passedFitness = brain.fitness(passed);
		Board control = new Board("7k/8/8/8/1p6/8/P1P5/7K w KQkq - 0 1");
		float cleanFitness = brain.fitness(control);
		assertTrue(passedFitness > cleanFitness);
	}
	
	@Test
	public void testFitnessCastleRights() {
		Brain brain = new Brain();
		Board both = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
		float bothFitness = brain.fitness(both);
		Board kingside = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w Kkq - 0 1");
		float kingsideFitness = brain.fitness(kingside);
		Board queenside = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w Qkq - 0 1");
		float queensideFitness = brain.fitness(queenside);
		Board neither = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w kq - 0 1");
		float neitherFitness = brain.fitness(neither);
		assertTrue(bothFitness > kingsideFitness);
		assertTrue(bothFitness > queensideFitness);
		assertTrue(kingsideFitness > neitherFitness);
		assertTrue(queensideFitness > neitherFitness);
	}
	
	@Test
	public void testFitnessCastleRightsPotential() {
		Brain brain = new Brain();
		Board brokenShield = new Board("rnbqrbnk/pppppppp/8/8/6P1/8/PPPPPP1P/RNBQRBNK w KQkq - 0 1");
		float brokenShieldFitness = brain.fitness(brokenShield);
		Board control = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
		float controlFitness = brain.fitness(control);
		assertTrue(brokenShieldFitness < controlFitness);
	}
	
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
	
	@Test
	public void testMateInOneLosing() {
		// This is to prevent a bug where all moves lead to checkmate and no move is "good enough",
		// so a null move is played.
		Board board = new Board("8/8/8/8/8/7k/q7/7K w KQkq - 0 1");
		Brain brain = new Brain();
		Move move = brain.getMove(board);
		assertNotNull(move);
	}
	
	@Test
	public void testFork() {
		Board board = new Board();
		board.move("b1", "c3");
		board.move("d7", "d5");
		board.move("c3", "b5");
		board.move("d8", "h4");
		// Move the c-pawn so the knight actually recognizes the fork instead of just trying to win
		// a pawn.
		board.move("h2", "h3");
		board.move("c7", "c6");
		Brain brain = new Brain();
		Move move = brain.getMove(board);
		assertEquals(move.toString(), "b5c7");
	}
	
	@Test
	public void testFitness() {
		Board board = new Board();
		board.move("e2", "e4");
		board.move("d7", "d5");
		board.move("e4", "d5");
		Brain brain = new Brain();
		assertTrue(brain.fitness(board) < 0);
		board.move("h7", "h6");
		assertTrue(brain.fitness(board) > 0);
	}
}
