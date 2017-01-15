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
		
		public TranspositionEntry(int depth, long positionHash,
				float fitness, Move bestMove, TranspositionType type) {
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
		this.data = new TranspositionEntry[this.size];
		for(int i = 0; i < this.size; i++) {
			this.data[i] = new TranspositionEntry();
		}
	}
	
	private int index(long positionHash) {
		// Unset the sign bit.
		return ((int)positionHash & 0x7fffffff) % this.size;
	}
	
	public void put(int depth, long positionHash, float fitness, Move bestMove,
			TranspositionType type) {
		// Assume we won't store more than MAX_INT entries.
		this.data[this.index(positionHash)] =
				new TranspositionEntry(depth, positionHash, fitness, bestMove,
						type);
	}
	
	public TranspositionEntry get(long positionHash) {
		TranspositionEntry entry = this.data[this.index(positionHash)];
		if(entry.positionHash == positionHash) {
			return entry;
		}
		return null;
	}
	
	private int size;
	private TranspositionEntry[] data;
}
