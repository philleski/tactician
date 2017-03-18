package tactician.Test;

import static org.junit.Assert.*;

import org.junit.Test;

import tactician.Board;
import tactician.LegalMoveGenerator;

/**
 * This class tests the functionality of the {@link LegalMoveGenerator} class.
 * 
 * @author Phil Leszczynski
 */
public class TestLegalMoveGenerator {
	/** Ensures we can recognize when a player is in check. */
	@Test
	public void testIsInCheck() {
		Board board = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBqK w KQkq - 0 1");
		assertTrue(board.isInCheck());
	}
	
	/** Ensures we can recognize when a player is not in check. */
	@Test
	public void testIsNotInCheck() {
		Board board = new Board("rnbqrbnk/pppppppp/8/8/8/8/PPPPPPPP/RNBQRBNK w KQkq - 0 1");
		assertTrue(!board.isInCheck());
	}
}
