package chess_engine;

public class Move {
	public Move() {
		this.source = 0;
		this.destination = 0;
		this.promoteTo = Board.EMPTY;
		this.enPassantCapture = Board.EP_NO;
		this.castle = Board.CASTLE_NONE;
	}
	
	public Move(long source, long destination, int promoteTo, int enPassantCapture,
			int castle) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = promoteTo;
		this.enPassantCapture = enPassantCapture;
		this.castle = castle;
	}
	
	public long source;
	public long destination;
	public int promoteTo;
	public int enPassantCapture;
	public int castle;
}
