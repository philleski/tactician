package chess_engine;

public enum Color {
	BLACK,
	WHITE;

	public static Color flip(Color c) {
		if(c == BLACK) {
			return WHITE;
		}
		else {
			return BLACK;
		}
	}
	
	public static String getName(Color c) {
		if(c == BLACK) {
			return "black";
		}
		else {
			return "white";
		}
	}
}
