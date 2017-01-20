package chess_engine;

public class Move {
	public Move() {
		this.source = 0;
		this.destination = 0;
		this.promoteTo = Piece.NOPIECE;
	}
	
	public Move(int sourceIndex, int attackSquareIndex) {
		this.source = sourceIndex;
		this.destination = attackSquareIndex;
		this.promoteTo = Piece.NOPIECE;
	}
	
	public Move(int source, int destination, Piece promoteTo) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = promoteTo;
	}
	
	public Move(String source, String destination) {
		this.source = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				source));
		this.destination = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				destination));
		this.promoteTo = Piece.NOPIECE;
	}
	
	public Move(String source, String destination, Piece promoteTo) {
		this.source = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				source));
		this.destination = NotationHelper.coordToIndex(NotationHelper.squareToCoord(
				destination));
		this.promoteTo = promoteTo;
	}
	
	public boolean equals(Move other) {
		if(this.source != other.source) {
			return false;
		}
		if(this.destination != other.destination) {
			return false;
		}
		if(this.promoteTo != other.promoteTo) {
			return false;
		}
		return true;
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
		
	public int source;
	public int destination;
	public Piece promoteTo;
}
