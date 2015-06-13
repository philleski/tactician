package chess_engine;

public class Move {
	public Move() {
		this.source = 0;
		this.destination = 0;
		this.promoteTo = Piece.NOPIECE;
	}
	
	public Move(long source, long destination) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = Piece.NOPIECE;
	}
	
	public Move(long source, long destination, Piece promoteTo) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = promoteTo;
	}
		
	public long source;
	public long destination;
	public Piece promoteTo;
}
