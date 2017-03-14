package chess_engine;

public class TranspositionTable {
	public enum TranspositionType {
		NODE_PV,
		NODE_CUT,
		NODE_ALL
	}
	
	public class TranspositionEntry {
		public TranspositionEntry() {
		}
		
		public TranspositionEntry(int depth, long positionHash, float fitness, Move bestMove,
				TranspositionType type) {
			this.depth = depth;
			this.positionHash = positionHash;
			this.fitness = fitness;
			this.bestMove = bestMove;
			this.type = type;
		}
		
		public String toString() {
			String result = "Depth=" + this.depth + ", ";
			result += "Fitness=" + this.fitness + ", ";
			result += "BestMove=" + this.bestMove + ", ";
			result += "Type=" + this.type;
			return result;
		}
		
		public int depth = 0;
		public long positionHash = 0;
		public float fitness = 0;
		public TranspositionType type = null;
		public Move bestMove = null;
	}
	
	// Note: The size is in number of entries, not in bytes.
	public TranspositionTable(int size) {
		this.size = size;
		this.data = new long[2 * this.size];
	}
	
	public void put(int depth, long positionHash, float fitness, Move bestMove,
			TranspositionType type) {
		int index = this.index(positionHash);
		long contents = 0;
		contents |= ((long) Float.floatToIntBits(fitness)) << 32;
		if(bestMove != null) {
			contents |= (long) (bestMove.source << 24);
			contents |= (long) (bestMove.destination << 16);
			if(bestMove.promoteTo == Piece.QUEEN) {
				contents |= 0x0000000000001000L;
			} else if(bestMove.promoteTo == Piece.KNIGHT) {
				contents |= 0x0000000000002000L;
			} else if(bestMove.promoteTo == Piece.ROOK) {
				contents |= 0x0000000000004000L;
			} else if(bestMove.promoteTo == Piece.BISHOP) {
				contents |= 0x0000000000008000L;
			}
		}
		if(type == TranspositionType.NODE_PV) {
			contents |= 0x0000000000000100L;
		} else if(type == TranspositionType.NODE_CUT) {
			contents |= 0x0000000000000200L;
		} else if(type == TranspositionType.NODE_ALL) {
			contents |= 0x0000000000000400L;
		}
		contents |= (long) (byte) depth;
		this.data[index] = positionHash;
		this.data[index + 1] = contents;
	}
	
	public TranspositionEntry get(long positionHash) {
		int index = this.index(positionHash);
		long positionHashFound = this.data[index];
		if(positionHash != positionHashFound) {
			return null;
		}
		long contents = this.data[index + 1];
		float fitness = Float.intBitsToFloat((int)(contents >>> 32));
		Move bestMove = null;
		if((contents & 0x0000000011111000) != 0) {
			bestMove = new Move((int) (byte) (contents >>> 24), (int) (byte) (contents >>> 16));
			if((contents & 0x0000000000001000L) != 0) {
				bestMove.promoteTo = Piece.QUEEN;
			} else if((contents & 0x0000000000002000L) != 0) {
				bestMove.promoteTo = Piece.KNIGHT;
			} else if((contents & 0x0000000000004000L) != 0) {
				bestMove.promoteTo = Piece.ROOK;
			} else if((contents & 0x0000000000008000L) != 0) {
				bestMove.promoteTo = Piece.BISHOP;
			}
		}
		TranspositionType type = null;
		if((contents & 0x0000000000000100L) != 0) {
			type = TranspositionType.NODE_PV;
		} else if((contents & 0x0000000000000200L) != 0) {
			type = TranspositionType.NODE_CUT;
		} else if((contents & 0x0000000000000400L) != 0) {
			type = TranspositionType.NODE_ALL;
		}
		int depth = (int) (byte) (contents);
		return new TranspositionEntry(depth, positionHash, fitness, bestMove, type);
	}
	
	private int index(long positionHash) {
		// Unset the sign bit; the modulus is 2 * this.size because we're
		// storing the transposition entry in two longs.
		return ((int)positionHash & 0x7fffffff) % (2 * this.size);
	}
	
	private int size;
	
	// Each transposition entry is composed of two long values together.
	// The first is the position hash.
	// The second has:
	//   bits 0-31: fitness represented as a float
	//   bits 32-39: the best move's source square
	//   bits 40-47: the best move's destination square
	//   bits 48-51: the best move's promoteTo
	//     (none=0000, Q=0001, N=0010, R=0100, B=1000)
	//   bits 52-55: the transposition type (PV=0001, Cut=0010, All=0100)
	//   bits 56-63: the search depth
	private long[] data;
}
