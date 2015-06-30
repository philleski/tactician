package chess_engine;

public class TranspositionTable {
	// TODO - Refactor these into their own files.
	// https://chessprogramming.wikispaces.com/Node+Types#PV
	public enum TranspositionType {
		NODE_EXACT,
		NODE_ALPHA,
		NODE_BETA
	}
	
	public class TranspositionEntry {
		public TranspositionEntry() {
		}
		
		public TranspositionEntry(int depth, long positionHash,
				float fitness, TranspositionType type) {
			this.depth = depth;
			this.positionHash = positionHash;
			this.fitness = fitness;
			this.type = type;
		}
		
		public int depth = 0;
		public long positionHash = 0;
		public float fitness = 0;
		public TranspositionType type = null;
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
	
	public void put(int depth, long positionHash, float fitness, TranspositionType type) {
		// Assume we won't store more than MAX_INT entries.
		this.data[this.index(positionHash)] =
				new TranspositionEntry(depth, positionHash, fitness, type);
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
