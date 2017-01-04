package chess_engine;

public enum Piece {
	BISHOP,
	KING,
	KNIGHT,
	NOPIECE,
	PAWN,
	QUEEN,
	ROOK;
	
	public static Piece initialToPiece(char initial) {
		initial = Character.toUpperCase(initial);
		if(initial == 'B') {
			return Piece.BISHOP;
		} else if(initial == 'K') {
			return Piece.KING;
		} else if(initial == 'N') {
			return Piece.KNIGHT;
		} else if(initial == 'P') {
			return Piece.PAWN;
		} else if(initial == 'Q') {
			return Piece.QUEEN;
		} else if(initial == 'R') {
			return Piece.ROOK;
		}
		return Piece.NOPIECE;
	}
}
