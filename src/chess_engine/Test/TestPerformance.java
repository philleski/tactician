package chess_engine.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import chess_engine.Board;
import chess_engine.IllegalMoveException;
import chess_engine.Move;

// Check that the move generations works correctly based on publicly generated
// databases. Optionally print out the runtimes as well.
//
// Perft results were taken from here:
// http://chessprogramming.wikispaces.com/Perft+Results
public class TestPerformance {
	private int perft(Board board, int depth) {
		int nodes = 0;
		if(depth == 0) {
			return 1;
		}
		ArrayList<Move> legalMoves = board.legalMoves();
		if(depth == 1) {
			return legalMoves.size();
		}
		for(Move move : legalMoves) {
			Board copy = new Board(board);
			try {
				copy.move(move);
			}
			catch(IllegalMoveException e) {
			}
			nodes += this.perft(copy, depth - 1);
		}
		return nodes;
	}

	@Test
	public void testInitial() {
		int[] nodeCounts = {1, 20, 400, 8902, 197281};
		for(int depth = 0; depth < nodeCounts.length; depth++) {
			Board board = new Board();
			assertEquals(this.perft(board, depth), nodeCounts[depth]);
		}
	}
	
	@Test
	public void testKiwipete() {
		int[] nodeCounts = {1, 48, 2039, 97862};
		for(int depth = 0; depth < nodeCounts.length; depth++) {
			Board board = new Board();
			board.setPositionFenstring("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
			assertEquals(this.perft(board, depth), nodeCounts[depth]);
		}
	}
	
	@Test
	public void testPosition3() {
		int[] nodeCounts = {1, 14, 191, 2812, 43238, 674624};
		for(int depth = 0; depth < nodeCounts.length; depth++) {
			Board board = new Board();
			board.setPositionFenstring("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -");
			assertEquals(this.perft(board, depth), nodeCounts[depth]);
		}
	}
	
	@Test
	public void testPosition4() {
		int[] nodeCounts = {1, 6, 264, 9467, 422333};
		for(int depth = 0; depth < nodeCounts.length; depth++) {
			Board board = new Board();
			board.setPositionFenstring("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
			assertEquals(this.perft(board, depth), nodeCounts[depth]);
		}
	}
	
	@Test
	public void testPosition4Mirrored() {
		int[] nodeCounts = {1, 6, 264, 9467, 422333};
		for(int depth = 0; depth < nodeCounts.length; depth++) {
			Board board = new Board();
			board.setPositionFenstring("r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1");
			assertEquals(this.perft(board, depth), nodeCounts[depth]);
		}
	}
	
	@Test
	public void testPosition5() {
		int[] nodeCounts = {1, 44, 1486, 62379, 2103487};
		for(int depth = 0; depth < nodeCounts.length; depth++) {
			Board board = new Board();
			board.setPositionFenstring("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
			assertEquals(this.perft(board, depth), nodeCounts[depth]);
		}
	}
	
	@Test
	public void testPosition6() {
		int[] nodeCounts = {1, 46, 2079, 89890};
		for(int depth = 0; depth < nodeCounts.length; depth++) {
			Board board = new Board();
			board.setPositionFenstring("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
			assertEquals(this.perft(board, depth), nodeCounts[depth]);
		}
	}
}
