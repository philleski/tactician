package chess_engine;

public class Bitboard {
	public Bitboard(long data) {
		this.data = data;
	}
	
	public Bitboard(String... squares) {
		this.data = notationHelper.generateMask(squares);
	}
	
	public Bitboard copy() {
		return new Bitboard(this.data);
	}
	
	public String toString() {
		return Long.toHexString(this.data);
	}
	
	public Bitboard flip() {
		long output = 0;
		for(int i = 0; i < 4; i++) {
			long row = this.data & (0x00000000000000ffL << (8 * i));
			output += row << (8 * (7 - 2 * i));
		}
		for(int i = 4; i < 8; i++) {
			long row = this.data & (0x00000000000000ffL << (8 * i));
			output += row >>> (8 * (2 * i - 7));
		}
		return new Bitboard(output);
	}
		
	public static NotationHelper notationHelper = new NotationHelper();
	public long data = 0;
}
