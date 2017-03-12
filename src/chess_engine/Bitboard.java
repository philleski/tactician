package chess_engine;

public class Bitboard {
	public Bitboard(long data) {
		this.data = data;
	}
	
	public Bitboard(String... squares) {
		long result = 0;
		for(String square : squares) {
			result |= NotationHelper.squareToCoord(square);
		}
		this.data = result;
	}
	
	public Bitboard copy() {
		return new Bitboard(this.data);
	}
	
	public void reset() {
		this.data = 0;
	}
	
	public String toString() {
		return Long.toHexString(this.data);
	}
	
	public Bitboard flip() {
		long firstRank = 0x00000000000000ffL;
		long mask = this.data;
		long output = 0;
		for(int i = 0; i < 4; i++) {
			long row = mask & (firstRank << (8 * i));
			output += row << (8 * (7 - 2 * i));
		}
		for(int i = 4; i < 8; i++) {
			long row = mask & (firstRank << (8 * i));
			output += row >>> (8 * (2 * i - 7));
		}
		return new Bitboard(output);
	}
	
	public Bitboard intersection(long mask) {
		return new Bitboard(this.data & mask);
	}
	
	public Bitboard intersection(Bitboard other) {
		return new Bitboard(this.data & other.getData());
	}
	
	public boolean intersects(long mask) {
		return this.intersection(mask).getData() != 0;
	}
	
	public boolean isEmpty() {
		return this.data == 0L;
	}
	
	public Bitboard union(long mask) {
		return new Bitboard(this.data | mask);
	}
	
	public Bitboard union(Bitboard other) {
		return new Bitboard(this.data | other.getData());
	}
	
	public void updateRemove(long mask) {
		this.data &= ~(mask ^ 0);
	}
	
	public void updateRemove(Bitboard other) {
		this.updateRemove(other.getData());
	}
	
	public void updateUnion(long mask) {
		this.data |= mask;
	}
	
	public void updateUnion(Bitboard other) {
		this.updateUnion(other.getData());
	}
	
	public int numBitsSet() {
		// Taken from http://en.wikipedia.org/wiki/Hamming_weight
		long x = this.data;
		int count;
		for(count = 0; x != 0; count++) {
			x &= x - 1;
		}
		return count;
	}
	
	public int numTrailingZeros() {
		return Long.numberOfTrailingZeros(this.data);
	}
	
	public long getData() {
		return this.data;
	}
	
	public void setData(long data) {
		this.data = data;
	}
		
	public static NotationHelper notationHelper = new NotationHelper();
	private long data = 0;
}
