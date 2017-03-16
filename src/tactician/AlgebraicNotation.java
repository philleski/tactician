package tactician;

public class AlgebraicNotation {
	private static Move findCastleKingsideMove(Board board) {
		for(Move move : board.legalMoves()) {
			if(move.source + 2 == move.destination) {
				return move;
			}
		}
		return null;
	}
	
	private static Move findCastleQueensideMove(Board board) {
		for(Move move : board.legalMoves()) {
			if(move.source - 2 == move.destination) {
				return move;
			}
		}
		return null;
	}
	
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
	
	private static Move findNonPawnMove(Board board, Piece mover, Square destination,
			char matchFile, char matchRank) {
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
			if(matchFile != '_' && matchFile != destinationSquare.getFile()) {
				continue;
			}
			if(matchRank != '_' && matchRank != destinationSquare.getRank()) {
				continue;
			}
			return move;
		}
		return null;
	}
	
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
	
	private static String getAmbiguity(Board board, Move moveToMatch, Piece moverToMatch) {
		Square sourceToMatch = new Square(moveToMatch.source);
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
		
		String ambiguity = getAmbiguity(board, move, mover);
		
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
