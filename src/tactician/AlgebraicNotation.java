package tactician;

/**
 * This class is responsible for converting between a {@link Move} object and chess algebraic
 * notation. {@link #algebraicToMove(Board, String)} converts a string in algebraic notation to a
 * {@link Move} and {@link #moveToAlgebraic(Board, Move)} does the opposite.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Algebraic_notation_(chess)">Algebraic Notation</a>
 * @author Phil Leszczynski
 */
public class AlgebraicNotation {
	/**
	 * Returns a move where the player castles kingside if it is legal, or null otherwise.
	 * @param board the board containing the position
	 * @return a move that castles kingside if one is legal, null otherwise
	 */
	private static Move findCastleKingsideMove(Board board) {
		for(Move move : board.legalMoves()) {
			if(move.source + 2 == move.destination) {
				return move;
			}
		}
		return null;
	}
	
	/**
	 * Returns a move where the player castles queenside if it is legal, or null otherwise.
	 * @param board the board containing the position
	 * @return a move that castles queenside if one is legal, null otherwise
	 */
	private static Move findCastleQueensideMove(Board board) {
		for(Move move : board.legalMoves()) {
			if(move.source - 2 == move.destination) {
				return move;
			}
		}
		return null;
	}
	
	/**
	 * Returns a pawn move given a board, a starting file for the pawn, a destination square, and a
	 * piece to promote to (null if the pawn does not promote). If there is no such legal move
	 * available on the board, returns null.
	 * @param board the board containing the position
	 * @param file the starting file for the pawn, e.g. 'c'
	 * @param destination the square where the pawn moves to
	 * @param promoteTo the type of piece if the pawn promotes, null otherwise
	 * @return a pawn move on the board that meets the criteria, null if no such move is found
	 */
	private static Move findPawnMove(Board board, char file, Square destination, Piece promoteTo) {
		for(Move move : board.legalMoves()) {
			Square sourceSquare = new Square(move.source);
			Square destinationSquare = new Square(move.destination);
			Piece sourcePiece = board.pieceOnSquare(sourceSquare);
			char sourceFile = sourceSquare.getName().charAt(0);
			if(sourcePiece != Piece.PAWN) {
				continue;
			}
			if(sourceFile != file) {
				continue;
			}
			if(destination != destinationSquare) {
				continue;
			}
			if(move.promoteTo != promoteTo) {
				continue;
			}
			return move;
		}
		return null;
	}
	
	/**
	 * Returns a non-pawn move given a board, the type of piece to move, a destination square, a
	 * start file for the piece if specified, and a start rank for the piece if specified. The
	 * start file and start rank may be needed to resolve ambiguity since for example it is
	 * possible for the white player's b1 and e2 knights to go to c3. If the start file is set to
	 * 'e', that means the e2-knight is intended as the mover. If either the start file or start
	 * rank is not needed, it should be passed in as '_'.
	 * @param board the board containing the position
	 * @param mover the type of piece to move
	 * @param destination the square where the piece moves to
	 * @param startFile the name of the file if more than one piece of the given type on different
	 *        files can reach the destination square, '_' otherwise
	 * @param startRank the name of the rank if more than one piece of the given type on different
	 *        ranks can reach the destination square (and startFile is not sufficient to tell the
	 *        difference), '_' otherwise
	 * @return a non-pawn move on the board that meets the criteria, null if no such move is found
	 */
	private static Move findNonPawnMove(Board board, Piece mover, Square destination,
			char startFile, char startRank) {
		for(Move move : board.legalMoves()) {
			Square sourceSquare = new Square(move.source);
			Square destinationSquare = new Square(move.destination);
			Piece sourcePiece = board.pieceOnSquare(sourceSquare);
			if(sourcePiece != mover) {
				continue;
			}
			if(destination != destinationSquare) {
				continue;
			}
			if(startFile != '_' && startFile != destinationSquare.getFile()) {
				continue;
			}
			if(startRank != '_' && startRank != destinationSquare.getRank()) {
				continue;
			}
			return move;
		}
		return null;
	}
	
	/**
	 * Given a board and a move string in algebraic notation, returns a {@link Move} object
	 * corresponding to that move. It first deals with the special cases for castling moves, "O-O"
	 * and "O-O-O". Otherwise if the first character is a lowercase letter it is a pawn move and it
	 * delegates to {@link #findPawnMove(Board, char, Square, Piece)}. Otherwise it is a non-pawn
	 * move and we look for ambiguity resolving characters such as the 'e' in "Nec3". Finally we
	 * delegate to {@link #findNonPawnMove(Board, Piece, Square, char, char)}.
	 * @param board the board containing the position
	 * @param algebraic the string describing the move in algebraic notation
	 * @return a {@link Move} object if such a legal move is found on the board, null otherwise
	 */
	public static Move algebraicToMove(Board board, String algebraic) {
		if(algebraic.length() < 2) {
			System.err.println("Illegal move: too short: " + algebraic);
			return null;
		}
		
		if(algebraic.equals("O-O")) {
			return findCastleKingsideMove(board);
		}
		if(algebraic.equals("O-O-O")) {
			return findCastleQueensideMove(board);
		}
		
		String destinationName = "";
		Piece promoteTo = null;
		if(algebraic.charAt(algebraic.length() - 2) == '=') {
			// The move is a pawn promotion and ends with, for example "=Q". So the destination
			// string is the last two digits before the equals sign.
			destinationName = algebraic.substring(algebraic.length() - 4, algebraic.length() - 2);
			promoteTo = Piece.initialToPiece(algebraic.charAt(algebraic.length() - 1));
		}
		else {
			destinationName = algebraic.substring(algebraic.length() - 2, algebraic.length());
		}
		Square destination = new Square(destinationName);
		
		char algebraicPrefix = algebraic.charAt(0);
		if(algebraicPrefix >= 'a' && algebraicPrefix <= 'h') {
			return findPawnMove(board, algebraicPrefix, destination, promoteTo);
		}
		
		Piece mover = Piece.initialToPiece(algebraicPrefix);
		String algebraicTrimmed = algebraic.replace("x", "");
		int algebraicTrimmedLength = algebraicTrimmed.length();
		char matchFile = '_';
		char matchRank = '_';
		if(algebraicTrimmedLength == 4) {
			// The second character is used to resolve ambiguity when two of the same type of piece
			// can go to the same destination square. For example if two knights can go to c3, the
			// move can be disambiguated as Nbc3 or Nec3. Similarly the first character can be a
			// rank such as R1e7.
			char resolver = algebraicTrimmed.charAt(1);
			if(resolver >= 'a' && resolver <= 'h') {
				matchFile = resolver;
			} else {
				matchRank = resolver;
			}
		} else if(algebraicTrimmedLength == 5) {
			// This is a rare case when both the file and rank are needed to resolve ambiguity. For
			// example if there are three white queens on a1, a3, and c1, and the player wishes to
			// move the a1-queen to c3, the move would be written as Qa1c3.
			matchFile = algebraicTrimmed.charAt(1);
			matchRank = algebraicTrimmed.charAt(2);
		}
		return findNonPawnMove(board, mover, destination, matchFile, matchRank);
	}
	
	/**
	 * Given a board and a move on the board, gets the ambiguity string containing the file and/or
	 * rank if other pieces of the same type can move to the same destination square. For example
	 * if there are white knights on b1 and e2, and we wish to move the e2-knight, the ambiguity
	 * string would be "e". If there are black rooks on g3 and g7, and we wish to move the g7-rook,
	 * the ambiguity string would be "7". If there are white queens on a1, a3, and c1, and we wish
	 * to move the a1-queen to c3, the ambiguity string would be "a1" since neither "a" nor "1" is
	 * sufficient to describe which queen should move. Finally if there are black bishops on a8 and
	 * b8, and we wish to move the b8-bishop to c7, then the ambiguity string would be "" since
	 * only one bishop is able to move to c7.
	 * @param board the board containing the position
	 * @param moveToMatch the move on the board we wish to match, i.e. find other pieces of the
	 *        same type that can move to the same destination square
	 * @see <a href="https://en.wikipedia.org/wiki/Algebraic_notation_(chess)">
	 *      Algebraic Notation</a>
	 * @return the ambiguity string for the board and move, e.g. "e", "7", "a1", or ""
	 */
	private static String getAmbiguity(Board board, Move moveToMatch) {
		Square sourceToMatch = new Square(moveToMatch.source);
		Piece moverToMatch = board.pieceOnSquare(sourceToMatch);
		char fileToMatch = sourceToMatch.getFile();
		char rankToMatch = sourceToMatch.getRank();
		boolean matchingFileFound = false;
		boolean matchingRankFound = false;
		for(Move move : board.legalMoves()) {
			Piece mover = board.pieceOnSquare(new Square(move.source));
			if(move == moveToMatch) {
				// Only search for moves different than the move requested.
				continue;
			}
			if(mover != moverToMatch) {
				continue;
			}
			Square source = new Square(move.source);
			char file = source.getFile();
			char rank = source.getRank();
			if(file == fileToMatch) {
				matchingFileFound = true;
			}
			if(rank == rankToMatch) {
				matchingRankFound = true;
			}
		}
		String result = "";
		if(matchingFileFound) {
			result += fileToMatch;
		}
		if(matchingRankFound) {
			result += rankToMatch;
		}
		return result;
	}
	
	/**
	 * Given a board and a move on the board, returns the string representing the move in algebraic
	 * notation. It first deals with the special case castling moves "O-O" and "O-O-O". It then
	 * gets the prefix which is either the pawn file or the type of piece. It then gets the
	 * ambiguity string through {@link #getAmbiguity(Board, Move)}. It then inserts the character
	 * 'x' if the move is a capture. Next it puts in the destination square. Finally it adds in the
	 * promotion string if there is one, for example "=Q".
	 * @param board the board containing the position
	 * @param move the move to convert to algebraic notation
	 * @return a string representing the move in algebraic notation
	 */
	public static String moveToAlgebraic(Board board, Move move) {
		Square sourceSquare = new Square(move.source);
		Square destinationSquare = new Square(move.destination);
		Piece mover = board.pieceOnSquare(sourceSquare);
		
		if(mover == Piece.KING && move.source + 2 == move.destination) {
			return "O-O";
		}
		if(mover == Piece.KING && move.source - 2 == move.destination) {
			return "O-O-O";
		}
		
		String algebraicPrefix;
		if(mover == Piece.PAWN) {
			algebraicPrefix = "" + sourceSquare.getFile();
		} else {
			algebraicPrefix = "" + mover.initial();
		}
		
		String ambiguity = getAmbiguity(board, move);
		
		String captureStr = "";
		if(board.allPieces.intersects(destinationSquare)) {
			captureStr = "x";
		}
		else if(mover == Piece.PAWN && board.enPassantTarget == (1L << move.destination)) {
			captureStr = "x";
		}
		
		String destinationSquareName = destinationSquare.getName();
		
		String promoteToStr = "";
		if(move.promoteTo != null) {
			promoteToStr = "=" + move.promoteTo.initial();
		}
		
		return algebraicPrefix + ambiguity + captureStr + destinationSquareName + promoteToStr;
	}
}
