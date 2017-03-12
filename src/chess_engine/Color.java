package chess_engine;

public enum Color {
	BLACK,
	WHITE;

	public static Color flip(Color c) {
		if(c == Color.BLACK) {
			return Color.WHITE;
		}
		else {
			return Color.BLACK;
		}
	}
}
