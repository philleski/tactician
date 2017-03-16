package tactician;

public enum Piece {
	BISHOP,
	KING,
	KNIGHT,
	PAWN,
	QUEEN,
	ROOK;
	
	public char initial() {
		if(this == Piece.KNIGHT) {
			return 'N';
		}
		return this.name().charAt(0);
	}
	
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
		return null;
	}
}
