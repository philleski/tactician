package chess_engine;

/**
 * <p>This class represents which squares on the chessboard are "occupied" for a given
 * representation. For example you could have a bitboard for white bishops; the bitboard would
 * represent that white has bishops on the e5 and g7 squares, and nowhere else. You could also have
 * a bitboard for squares attacked by a knight on f6; in that case the bitboard would contain 8
 * occupied squares.
 * 
 * <p>The data is stored in {@link #data} as a 64-bit long. The low bit 0x000000000000001L says
 * whether the a1 square (bottom-left from white's perspective) is occupied. The next bit
 * represents b1, the square to the right from white's perspective. It continues for c1-h1, a2-h2,
 * and so on. Finally the highest bit 0x8000000000000000L represents the h8 square.
 * 
 * @author Phil Leszczynski
 */
public class Bitboard {
	/** Initializes a bitboard with no squares occupied. */
	public Bitboard() {
		this.data = 0L;
	}
	
	/**
	 * Initializes a bitboard represented by given data.
	 * @param data a 64-bit long representing data as described in the class definition.
	 */
	public Bitboard(long data) {
		this.data = data;
	}
	
	/**
	 * Initializes a bitboard represented by given occupied squares.
	 * @param squares a list of squares to be occupied, e.g. "d4", "e7", "h2"
	 */
	public Bitboard(String... squares) {
		long result = 0;
		for(String square : squares) {
			result |= NotationHelper.squareToCoord(square);
		}
		this.data = result;
	}
	
	/**
	 * Returns a bitboard for a given rank (horizontal row) of occupied squares on the chessboard.
	 * @param rank an integer from 0-7, 0 being the rank closest to the white player, 7 being the
	 *        rank closest to the black player
	 * @return a bitboard with squares in the rank occupied
	 */
	public static Bitboard bitboardFromRank(int rank) {
		long zerothRankMask = 0x00000000000000ffL;
		long rankMask = zerothRankMask << (8 * rank);
		return new Bitboard(rankMask);
	}
	
	/**
	 * Returns a bitboard for a given file (vertical row) of occupied squares on the chessboard.
	 * @param file an integer from 0-7, 0 being the left file from white's perspective, 7 being the
	 *        right file from white's perspective
	 * @return a bitboard with squares in the file occupied
	 */
	public static Bitboard bitboardFromFile(int file) {
		long zerothFileMask = 0x0101010101010101L;
		long fileMask = zerothFileMask << file;
		return new Bitboard(fileMask);
	}
	
	/**
	 * Returns a bitboard with the same occupied squares.
	 * @return a bitboard with the same occupied squares
	 */
	public Bitboard copy() {
		return new Bitboard(this.data);
	}
	
	/**
	 * Clears the bitboard so that it no longer has any occupied squares.
	 */
	public void clear() {
		this.data = 0;
	}
	
	/**
	 * Returns the hexadecimal representation of the bitboard's data.
	 */
	@Override
	public String toString() {
		return Long.toHexString(this.data);
	}
	
	/**
	 * Reflects the bitboard along the horizontal axis dividing the two players (the line between
	 * the fourth and fifth ranks, 1-indexed). For example if the original bitboard has occupied
	 * squares c2 and d5, the resulting bitboard will have occupied squares c7 and d4.
	 * @return the horizontally flipped bitboard
	 */
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
	
	/**
	 * Returns a modified bitboard which has squares occupied by the original AND by the mask.
	 * See the class definition for how the mask's data is represented.
	 * @param mask a 64-bit long representing another bitboard with which to intersect the original
	 * @return a bitboard containing all the original occupied squares that are also occupied by
	 *         the mask
	 */
	public Bitboard intersection(long mask) {
		return new Bitboard(this.data & mask);
	}
	
	/**
	 * Returns a modified bitboard which has squares occupied by the original AND by the other
	 * bitboard.
	 * @param other a bitboard with which to intersect the original
	 * @return a bitboard containing the occupied squares of both this and the other bitboard
	 */
	public Bitboard intersection(Bitboard other) {
		return new Bitboard(this.data & other.getData());
	}
	
	/**
	 * Tests whether the bitboard has any squares in common with the mask. See the class definition
	 * for how the mask's data is represented.
	 * @param mask a 64-bit long representing a bitboard with which to find common occupied squares
	 * @return true if there are any occupied squares in common, false otherwise
	 */
	public boolean intersects(long mask) {
		return this.intersection(mask).getData() != 0;
	}
	
	/**
	 * Tests whether the bitboard has any squares in common with the other bitboard.
	 * @param other a bitboard with which to find common occupied squares
	 * @return true if there are any occupied squares in common, false otherwise
	 */
	public boolean intersects(Bitboard other) {
		return this.intersects(other.getData());
	}
	
	/**
	 * Tests whether the bitboard has any occupied squares.
	 * @return true if there are no occupied squares, false if there is at least one
	 */
	public boolean isEmpty() {
		return this.data == 0L;
	}
	
	/**
	 * Returns a modified bitboard which has squares occupied by the original OR by the mask.
	 * See the class definition for how the mask's data is represented.
	 * @param mask a 64-bit long representing another bitboard with which to union the original
	 * @return a bitboard containing all the original occupied squares plus those occupied by the
	 *         mask
	 */
	public Bitboard union(long mask) {
		return new Bitboard(this.data | mask);
	}
	
	/**
	 * Returns a modified bitboard which has squares occupied by the original OR by the other
	 * bitboard.
	 * @param other a bitboard with which to union the original
	 * @return a bitboard containing the occupied squares of either this or the other bitboard
	 */
	public Bitboard union(Bitboard other) {
		return new Bitboard(this.data | other.getData());
	}
	
	/**
	 * Updates the bitboard to remove all the squares occupied by the mask. For example if this
	 * has occupied squares e7 and h2, and the mask has occupied squares h2 and c5, then this will
	 * end up having only e7 occupied.
	 * @param mask a 64-bit long representing the occupied squares to remove
	 */
	public void updateRemove(long mask) {
		this.data &= ~(mask ^ 0);
	}
	
	/**
	 * Updates the bitboard to remove all the squares occupied by the other bitboard. For example
	 * if this has occupied squares e7 and h2, and other has occupied squares h2 and c5, then this
	 * will end up having only e7 occupied.
	 * @param other a bitboard containing the occupied squares to remove
	 */
	public void updateRemove(Bitboard other) {
		this.updateRemove(other.getData());
	}
	
	/**
	 * Updates the bitboard to include all the squares occupied by the mask. For example if this
	 * has occupied squares e7 and h2, and the mask has occupied squares h2 and c5, then this will
	 * end up having e7, h2, and c5 occupied.
	 * @param mask a 64-bit long representing the occupied squares to add in
	 */
	public void updateUnion(long mask) {
		this.data |= mask;
	}
	
	/**
	 * Updates the bitboard to include all the squares occupied by the other bitboard. For example
	 * if this has occupied squares e7 and h2, and the mask has occupied squares h2 and c5, then
	 * this will end up having e7, h2, and c5 occupied.
	 * @param other a bitboard containing the occupied squares to add in
	 */
	public void updateUnion(Bitboard other) {
		this.updateUnion(other.getData());
	}
	
	/**
	 * Counts the number of occupied squares.
	 * @return the number of occupied squares
	 */
	public int numOccupied() {
		// Taken from http://en.wikipedia.org/wiki/Hamming_weight
		long x = this.data;
		int count;
		for(count = 0; x != 0; count++) {
			x &= x - 1;
		}
		return count;
	}
	
	/**
	 * Returns the number of empty squares starting from a1. For example if this has occupied
	 * squares c2 and h5, then since a1-h1, a2, and b2 are empty, the result would be 10. If a1 is
	 * occupied the result is 0.
	 * @return the number of starting empty squares
	 */
	public int numEmptyStartingSquares() {
		return Long.numberOfTrailingZeros(this.data);
	}
	
	/**
	 * Returns the 64-bit long representation of the bitboard. See the class definition for a
	 * description of the long representation.
	 * @return the bitboard representation as a 64-bit long
	 */
	public long getData() {
		return this.data;
	}
	
	/**
	 * Sets the occupied squares based on a 64-bit long representation. See the class definition
	 * for a description of the long representation.
	 * @param data the 64-bit long containing the occupied squares
	 */
	public void setData(long data) {
		this.data = data;
	}
	
	/**
	 * The internal 64-bit representation of the occupied squares. See the class definition for a
	 * description of the long representation.
	 */
	private long data = 0;
}
