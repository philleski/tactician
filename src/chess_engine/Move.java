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
	
	public Move(String source, String destination) {
		this.source = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				source));
		this.destination = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				destination));
	}
	
	public Move(String source, String destination, Piece promoteTo) {
		this.source = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				source));
		this.destination = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				destination));
		this.promoteTo = promoteTo;
	}
	
	public String toString() {
		String sourceStr = NotationHelper.indexToSquare(this.source);
		String destinationStr = NotationHelper.indexToSquare(this.destination);
		String promoteToStr = "";
		if(promoteTo == Piece.NOPIECE) {
			promoteToStr = "";
		} else if(promoteTo == Piece.QUEEN) {
			promoteToStr = "Q";
		} else if(promoteTo == Piece.ROOK) {
			promoteToStr = "R";
		} else if(promoteTo == Piece.BISHOP) {
			promoteToStr = "B";
		} else if(promoteTo == Piece.KNIGHT) {
			promoteToStr = "N";
		} else {
			promoteToStr = "?";
		}
		return sourceStr + destinationStr + promoteToStr;
	}
		
	public byte source;
	public byte destination;
	public Piece promoteTo;
}
