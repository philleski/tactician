package chess_engine;

public class TranspositionTable {
	// TODO - Refactor these into their own files.
	// https://chessprogramming.wikispaces.com/Node+Types#PV
	public enum TranspositionType {
		NODE_PV,
		NODE_CUT,
		NODE_ALL
	}
	
	public class TranspositionEntry {
		public TranspositionEntry() {
		}
		
		public TranspositionEntry(long positionHash, float fitness,
				TranspositionType type) {
			this.positionHash = positionHash;
			this.fitness = fitness;
			this.type = type;
		}
		
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
	
	public void put(long positionHash, float fitness, TranspositionType type) {
		// Assume we won't store more than MAX_INT entries.
		this.data[this.index(positionHash)] =
				new TranspositionEntry(positionHash, fitness, type);
	}
	
	private int hits = 0;
	private int collisions = 0;
	private int nones = 0;
	
	public TranspositionEntry get(long positionHash) {
		TranspositionEntry entry = this.data[this.index(positionHash)];
		if(entry.positionHash == positionHash) {
			hits++;
			// System.out.println(hits + " " + collisions + " " + nones);
			return entry;
		}
		else if(entry.positionHash != 0) {
			collisions++;
			// System.out.println(hits + " " + collisions + " " + nones);
		} else {
			nones++;
			// System.out.println(hits + " " + collisions + " " + nones);
		}
		return null;
	}
	
	private int size;
	private TranspositionEntry[] data;
}
