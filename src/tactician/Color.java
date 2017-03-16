package tactician;

/**
 * This enum lists the two piece colors in chess.
 * 
 * @author Phil Leszczynski
 */
public enum Color {
	BLACK,
	WHITE;

	/**
	 * Returns the opposite color.
	 * @param color the color to flip
	 * @return the opposite color
	 */
	public static Color flip(Color color) {
		if(color == Color.BLACK) {
			return Color.WHITE;
		}
		else {
			return Color.BLACK;
		}
	}
}
