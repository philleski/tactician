package tactician;

/**
 * <p>This class represents a chess move. It contains the source square {@link #source}, the
 * destination square {@link #destination}, and the optional piece to promote to
 * {@link #promoteTo}. Both squares are indexed in the standard way for this engine. An index of 0
 * corresponds to the a1 square, or the bottom left square from white's perspective. The index
 * increments as we move to the right from white's perspective, with 1 being the b1 square, 2 being
 * the c1 square, and so on. Once we reach the h-file the next square is one row up on the a-file.
 * So an index of 8 corresponds to the a2 square, and we keep incrementing to the right. Finally an
 * index of 63 corresponds to the h8 square, the top right square from white's perspective.
 * 
 * <p>There are three special moves in chess to keep in mind: castling, promotion, and en passant.
 * With castling we simply have the source and destination squares reflect the motion of the king.
 * For example if white castles kingside then {@link #source} would be 4 and {@link #destination}
 * would be 6. Promotion is handled by setting {@link #promoteTo} to the promoted piece; for all
 * other moves {@link #promoteTo} is null. En passant is handled by having the source and
 * destination squares reflect the motion of the capturing pawn. This means of course that the
 * captured pawn is on a different square than the destination square.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Castling">Castling</a>
 * @see <a href="https://en.wikipedia.org/wiki/Promotion_(chess)">Promotion</a>
 * @see <a href="https://en.wikipedia.org/wiki/En_passant">En Passant</a>
 * @author Phil Leszczynski
 */
public class Move {
	/**
	 * Initializes a move that is not a pawn promotion.
	 * @param source the index of the source square, 0-63
	 * @param destination the index of the destination square, 0-63
	 */
	public Move(int source, int destination) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = null;
	}
	
	/**
	 * Initializes a generic move.
	 * @param source the index of the source square, 0-63
	 * @param destination the index of the destination square, 0-63
	 * @param promoteTo the type of piece to promote to if the move is a pawn promotion, otherwise
	 *        null
	 */
	public Move(int source, int destination, Piece promoteTo) {
		this.source = source;
		this.destination = destination;
		this.promoteTo = promoteTo;
	}
	
	/**
	 * Initializes a move that is not a pawn promotion.
	 * @param source the source square, e.g. "e5"
	 * @param destination the destination square, e.g. "c5"
	 */
	public Move(String source, String destination) {
		this.source = new Square(source).getIndex();
		this.destination = new Square(destination).getIndex();
		this.promoteTo = null;
	}
	
	/**
	 * Initializes a generic move.
	 * @param source the source square, e.g. "c7"
	 * @param destination the destination square, e.g. "c8"
	 * @param promoteTo the type of piece to promote to if the move is a pawn promotion, otherwise
	 *        null
	 */
	public Move(String source, String destination, Piece promoteTo) {
		this.source = new Square(source).getIndex();
		this.destination = new Square(destination).getIndex();
		this.promoteTo = promoteTo;
	}
	
	/**
	 * Compares this move to another. For them to be equal we need {@link #source},
	 * {@link #destination}, and {@link #promoteTo} to all match.
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Move)) {
			return false;
		}
		Move other = (Move) obj;
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
	
	/**
	 * Outputs the string representation of the move containing the source square, destination
	 * square, and promotion piece if any. For example this may be "a7a5" or "e7e8Q". Note that
	 * algebraic notation, the standard chess notation for representing moves, is impossible
	 * without reference to a board.
	 */
	@Override
	public String toString() {
		String sourceStr = new Square(this.source).getName();
		String destinationStr = new Square(this.destination).getName();
		String promoteToStr = "";
		if(promoteTo == null) {
			promoteToStr = "";
		} else if(promoteTo == Piece.QUEEN) {
			promoteToStr = "q";
		} else if(promoteTo == Piece.ROOK) {
			promoteToStr = "r";
		} else if(promoteTo == Piece.BISHOP) {
			promoteToStr = "b";
		} else if(promoteTo == Piece.KNIGHT) {
			promoteToStr = "n";
		} else {
			promoteToStr = "?";
		}
		return sourceStr + destinationStr + promoteToStr;
	}
	
	/**
	 * The index of the source square from 0-63. See the class definition for how the indexing
	 * works.
	 */
	public int source;
	
	/**
	 * The index of the destination square from 0-63. See the class definition for how the indexing
	 * works.
	 */
	public int destination;
	
	/**
	 * If the move is a pawn promotion, this holds the type of piece that the pawn is promoting to.
	 * Otherwise it is null.
	 */
	public Piece promoteTo;
}
