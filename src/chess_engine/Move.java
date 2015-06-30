package chess_engine;

public class Move {
	public Move() {
		this.source = 0;
		this.destination = 0;
		this.promoteTo = Piece.NOPIECE;
	}
	
	public Move(byte source, byte destination) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = Piece.NOPIECE;
	}
	
	public Move(byte source, byte destination, Piece promoteTo) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = promoteTo;
	}
		
	public byte source;
	public byte destination;
	public Piece promoteTo;
}
