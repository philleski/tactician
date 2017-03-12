package chess_engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Board {
	public Board() {
		this.positionHasher = new PositionHasher();
		Map<Piece, Bitboard> whiteBitboards = new HashMap<Piece, Bitboard>();
		Map<Piece, Bitboard> blackBitboards = new HashMap<Piece, Bitboard>();
		whiteBitboards.put(Piece.BISHOP, new Bitboard("c1", "f1"));
		whiteBitboards.put(Piece.KING, new Bitboard("e1"));
		whiteBitboards.put(Piece.KNIGHT, new Bitboard("b1", "g1"));
		whiteBitboards.put(Piece.PAWN, Bitboard.bitboardFromRank(1));
		whiteBitboards.put(Piece.QUEEN, new Bitboard("d1"));
		whiteBitboards.put(Piece.ROOK, new Bitboard("a1", "h1"));
		for(Map.Entry<Piece, Bitboard> entry : whiteBitboards.entrySet()) {
			Piece piece = entry.getKey();
			Bitboard bitboard = entry.getValue();
			blackBitboards.put(piece, bitboard.flip());
		}
		this.bitboards.put(Color.WHITE, whiteBitboards);
		this.bitboards.put(Color.BLACK, blackBitboards);
		updateSummaryBitboards();
		
		this.turn = Color.WHITE;
		// If the last move was a double pawn move, this is the destination
		// coordinate.
		this.enPassantTarget = 0;
		for(Color color : Color.values()) {
			this.castleRights.put(color, new HashMap<Castle, Boolean>());
			for(Castle castle : Castle.values()) {
				this.castleRights.get(color).put(castle, true);
			}
		}
		this.fullMoveCounter = 1;
		
		this.setPositionHash();
	}
	
	public Board(Board other) {
		this.bitboards.clear();
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 :
				other.bitboards.entrySet()) {
			Color color = entry1.getKey();
			Map<Piece, Bitboard> bitboardsForColor =
				new HashMap<Piece, Bitboard>();
			for(Map.Entry<Piece, Bitboard> entry2 :
					entry1.getValue().entrySet()) {
				Piece piece = entry2.getKey();
				Bitboard bitboard = entry2.getValue();
				bitboardsForColor.put(piece, bitboard.copy());
			}
			this.bitboards.put(color, bitboardsForColor);
		}
		updateSummaryBitboards();
		
		this.turn = other.turn;
		this.enPassantTarget = other.enPassantTarget;
		for(Color color : Color.values()) {
			this.castleRights.put(color, new HashMap<Castle, Boolean>());
			for(Castle castle : Castle.values()) {
				this.castleRights.get(color).put(castle,
						other.castleRights.get(color).get(castle));
			}
		}
		this.fullMoveCounter = other.fullMoveCounter;
		this.positionHasher = other.positionHasher;
		this.positionHash = other.positionHash;
		this.positionHashPawnsKings = other.positionHashPawnsKings;
	}
	
	public Board(String fenstring) {
		this();
		this.setPositionFenstring(fenstring);
	}
	
	public String toString() {
		String result = "";
		String rowReversed = "";
		for(long i = 63; i >= 0; i--) {
			long mask = 1L << i;
			
			char initial = ' ';
			for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 :
					this.bitboards.entrySet()) {
				Color color = entry1.getKey();
				for(Map.Entry<Piece, Bitboard> entry2 :
						entry1.getValue().entrySet()) {
					Piece piece = entry2.getKey();
					Bitboard bitboard = entry2.getValue();
					if(bitboard.intersects(mask)) {
						initial = piece.initial();
						if(color == Color.WHITE) {
							initial = (char)(initial - 'A' + 'a');
						}
					}
				}
			}
			rowReversed += initial;
			if(i % 8 == 0) {
				result += new StringBuilder(rowReversed).reverse().toString();
				result += '\n';
				rowReversed = "";
			}
		}
		result += "Legal Moves: ";
		ArrayList<Move> lm = legalMoveGenerator.legalMoves(this);
		for(Move m : lm) {
			result += notationHelper.moveToAlgebraic(this, m) + ", ";
		}
		if(lm.size() > 0) {
			// Remove the last comma.
			result = result.substring(0, result.length() - 2);
		}
		result += "\n";
		if(this.isInCheck()) {
			result += "Check!\n";
		}
		result += "Turn: " + this.turn.toString();
		result += "\n\n";
		return result;
	}
	
	public void updateSummaryBitboards() {
		this.playerBitboards.put(Color.WHITE, new Bitboard());
		this.playerBitboards.put(Color.BLACK, new Bitboard());
		this.allPieces = new Bitboard();
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 :
				this.bitboards.entrySet()) {
			Color color = entry1.getKey();
			for(Map.Entry<Piece, Bitboard> entry2 :
					entry1.getValue().entrySet()) {
				Bitboard bitboard = entry2.getValue();
				this.playerBitboards.get(color).updateUnion(bitboard);
				this.allPieces.updateUnion(bitboard);
			}
		}
	}
	
	public boolean isInCheck() {
		// Determining whether the board is in check involves a lot of legal
		// move generation internals, especially if we want it to be fast. So
		// the logic is moved over to LegalMoveGenerator.
		return legalMoveGenerator.isInCheck(this);
	}
	
	public ArrayList<Move> legalMovesFast(boolean capturesOnly) {
		return legalMoveGenerator.legalMovesFast(this, capturesOnly);
	}
	
	public ArrayList<Move> legalMoves() {
		return legalMoveGenerator.legalMoves(this);
	}
	
	private void moveHandleOpponentRookCapture(Move move) {
		Color turnFlipped = Color.flip(this.turn);
		byte rookKingsideSourceOpponent;
		byte rookQueensideSourceOpponent;
		
		if(this.turn == Color.WHITE) {
			rookKingsideSourceOpponent = 63;
			rookQueensideSourceOpponent = 56;
		} else {
			rookKingsideSourceOpponent = 7;
			rookQueensideSourceOpponent = 0;
		}
		
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
		if(move.destination == rookQueensideSourceOpponent) {
			this.castleRights.get(turnFlipped).put(
				Castle.QUEENSIDE, false);
		} else if(move.destination == rookKingsideSourceOpponent) {
			this.castleRights.get(turnFlipped).put(
				Castle.KINGSIDE, false);
		}
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
	}
	
	private void moveRemoveDestination(Move move) {
		long destinationMask = 1L << move.destination;
		
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 :
				this.bitboards.entrySet()) {
			Color color = entry1.getKey();
			for(Map.Entry<Piece, Bitboard> entry2 :
					entry1.getValue().entrySet()) {
				Piece piece = entry2.getKey();
				Bitboard bitboard = entry2.getValue();
				if(bitboard.intersects(destinationMask)) {
					bitboard.updateRemove(destinationMask);
					this.positionHash ^= this.positionHasher.getMask(
						color, piece, move.destination);
					if(piece == Piece.PAWN || piece == Piece.KING) {
						this.positionHashPawnsKings ^=
							this.positionHasher.getMask(color, piece,
								move.destination);
					}
					return;
				}
			}
		}
	}
	
	private Piece moveUpdateSource(Move move) {
		long sourceMask = 1L << move.source;
		long destinationMask = 1L << move.destination;
		
		Piece movedPiece = Piece.NOPIECE;
		for(Map.Entry<Piece, Bitboard> entry :
				this.bitboards.get(this.turn).entrySet()) {
			Piece piece = entry.getKey();
			Bitboard bitboard = entry.getValue();
			if(bitboard.intersects(sourceMask)) {
				movedPiece = piece;
				bitboard.updateRemove(sourceMask);
				bitboard.updateUnion(destinationMask);
				this.positionHash ^= this.positionHasher.getMask(this.turn,
					piece, move.source, move.destination);
				if(piece == Piece.PAWN || piece == Piece.KING) {
					this.positionHashPawnsKings ^= this.positionHasher.getMask(
						this.turn, piece, move.source, move.destination);
				}
				break;
			}
		}
		return movedPiece;
	}
	
	private void moveEnPassant(Move move) {
		long destinationMask = 1L << move.destination;
		Color turnFlipped = Color.flip(this.turn);
		byte destinationRetreatedOneRow;
		long destinationMaskRetreatedOneRow;
		
		if(this.turn == Color.WHITE) {
			destinationRetreatedOneRow = (byte)(move.destination - 8);
			destinationMaskRetreatedOneRow = destinationMask >>> 8;
		} else {
			destinationRetreatedOneRow = (byte)(move.destination + 8);
			destinationMaskRetreatedOneRow = destinationMask << 8;
		}
		
		this.bitboards.get(turnFlipped).get(Piece.PAWN).updateRemove(
			destinationMaskRetreatedOneRow);
		this.positionHash ^= this.positionHasher.getMask(turnFlipped,
			Piece.PAWN, destinationRetreatedOneRow);
		this.positionHashPawnsKings ^= this.positionHasher.getMask(
			turnFlipped, Piece.PAWN, destinationRetreatedOneRow);
	}
	
	private void moveSetEnPassantTarget(Move move) {
		long destinationMask = 1L << move.destination;
		byte destinationRetreatedOneRow;
		long destinationMaskRetreatedOneRow;
		
		if(this.turn == Color.WHITE) {
			destinationRetreatedOneRow = (byte)(move.destination - 8);
			destinationMaskRetreatedOneRow = destinationMask >>> 8;
		} else {
			destinationRetreatedOneRow = (byte)(move.destination + 8);
			destinationMaskRetreatedOneRow = destinationMask << 8;
		}

		if(this.enPassantTarget != 0) {
			this.positionHash ^= this.positionHasher
				.getMaskEnPassantTarget((byte)NotationHelper.coordToIndex(
					this.enPassantTarget));
		}
		this.enPassantTarget = destinationMaskRetreatedOneRow;
		this.positionHash ^= this.positionHasher.getMaskEnPassantTarget(
			destinationRetreatedOneRow);
		this.positionHashPawnsKings ^= this.positionHasher
			.getMaskEnPassantTarget(destinationRetreatedOneRow);
	}
	
	private void moveUnsetEnPassantTarget(Move move) {
		if(this.enPassantTarget != 0) {
			this.positionHash ^= this.positionHasher
				.getMaskEnPassantTarget((byte)NotationHelper.coordToIndex(
					this.enPassantTarget));
			this.positionHashPawnsKings ^= this.positionHasher
				.getMaskEnPassantTarget((byte)NotationHelper.coordToIndex(
					this.enPassantTarget));
		}
		this.enPassantTarget = 0;
	}
	
	private void moveRemoveCastleRights(Move move) {
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
		this.castleRights.get(this.turn).put(Castle.KINGSIDE, false);
		this.castleRights.get(this.turn).put(Castle.QUEENSIDE, false);
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
	}
	
	private void moveCastleQueenside(Move move) {
		Bitboard rookStart;
		Bitboard rookEnd;
		byte rookSource;
		byte rookDestination;
		
		if(this.turn == Color.WHITE) {
			rookStart = this.bbA1;
			rookEnd = this.bbD1;
			rookSource = 0;
			rookDestination = 3;
		} else {
			rookStart = this.bbA8;
			rookEnd = this.bbD8;
			rookSource = 56;
			rookDestination = 59;
		}
		
		this.bitboards.get(this.turn).get(Piece.ROOK).updateRemove(rookStart);
		this.bitboards.get(this.turn).get(Piece.ROOK).updateUnion(rookEnd);
		this.positionHash ^= this.positionHasher.getMask(this.turn, Piece.ROOK,
			rookSource, rookDestination);
	}
	
	private void moveCastleKingside(Move move) {
		Bitboard rookStart;
		Bitboard rookEnd;
		byte rookSource;
		byte rookDestination;
		
		if(this.turn == Color.WHITE) {
			rookStart = this.bbH1;
			rookEnd = this.bbF1;
			rookSource = 7;
			rookDestination = 5;
		} else {
			rookStart = this.bbH8;
			rookEnd = this.bbF8;
			rookSource = 63;
			rookDestination = 61;
		}
		
		this.bitboards.get(this.turn).get(Piece.ROOK).updateRemove(rookStart);
		this.bitboards.get(this.turn).get(Piece.ROOK).updateUnion(rookEnd);
		this.positionHash ^= this.positionHasher.getMask(this.turn, Piece.ROOK,
			rookSource, rookDestination);
	}
	
	private void movePromote(Move move) {
		long destinationMask = 1L << move.destination;
		
		this.bitboards.get(this.turn).get(Piece.PAWN).updateRemove(
			destinationMask);
		this.bitboards.get(this.turn).get(move.promoteTo).updateUnion(
			destinationMask);
		// We switched the position hash for all pieces above under the
		// assumption that the destination piece would be the same as
		// the source piece. Since that's not the case with promotion,
		// do the xor again to unset the destination mask for the pawn.
		this.positionHash ^= this.positionHasher.getMask(this.turn,
			Piece.PAWN, move.destination);
		this.positionHash ^= this.positionHasher.getMask(this.turn,
			move.promoteTo, move.destination);
		this.positionHashPawnsKings ^= this.positionHasher.getMask(
			this.turn, Piece.PAWN, move.destination);
	}
	
	private void moveUpdateCastlingRightsForRookMove(Move move) {
		byte rookQueensideSource;
		byte rookKingsideSource;
		
		if(this.turn == Color.WHITE) {
			rookQueensideSource = 0;
			rookKingsideSource = 7;
		} else {
			rookQueensideSource = 56;
			rookKingsideSource = 63;
		}
		
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
		if(move.source == rookQueensideSource) {
			this.castleRights.get(this.turn).put(Castle.QUEENSIDE, false);
		} else if(move.source == rookKingsideSource) {
			this.castleRights.get(this.turn).put(Castle.KINGSIDE, false);
		}
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
	}

	public void move(Move move) {
		long sourceMask = 1L << move.source;
		long destinationMask = 1L << move.destination;
		Color turnFlipped = Color.flip(this.turn);
		
		long sourceMaskAdvancedTwoRows;
		if(this.turn == Color.WHITE) {
			sourceMaskAdvancedTwoRows = sourceMask << 16;
		} else {
			sourceMaskAdvancedTwoRows = sourceMask >>> 16;
		}
		
		if(this.bitboards.get(turnFlipped).get(Piece.ROOK).intersects(
				destinationMask)) {
			this.moveHandleOpponentRookCapture(move);
		}
		this.moveRemoveDestination(move);
		Piece movedPiece = this.moveUpdateSource(move);
		if(movedPiece == Piece.PAWN &&
				destinationMask == this.enPassantTarget) {
			this.moveEnPassant(move);
		}
		if(movedPiece == Piece.PAWN &&
				sourceMaskAdvancedTwoRows == destinationMask) {
			this.moveSetEnPassantTarget(move);
		} else {
			this.moveUnsetEnPassantTarget(move);
		}
		if(movedPiece == Piece.KING) {
			this.moveRemoveCastleRights(move);
			if(move.source - 2 == move.destination) {
				this.moveCastleQueenside(move);
			} else if(move.source + 2 == move.destination) {
				this.moveCastleKingside(move);
			}
		} else if(movedPiece == Piece.PAWN &&
				move.promoteTo != Piece.NOPIECE) {
			this.movePromote(move);
		} else if(movedPiece == Piece.ROOK) {
			this.moveUpdateCastlingRightsForRookMove(move);
		}
		
		if(this.turn == Color.BLACK) {
			this.fullMoveCounter++;
		}
		
		this.turn = turnFlipped;
		this.positionHash ^= this.positionHasher.getMaskTurn();
		updateSummaryBitboards();
	}
	
	public void move(String algebraic) {
		Move m = notationHelper.algebraicToMove(this, algebraic);
		this.move(m);
	}
	
	public void move(String source, String destination) {
		Move m = new Move(source, destination);
		this.move(m);
	}
	
	private void setPositionEmpty() {
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 :
				this.bitboards.entrySet()) {
			for(Map.Entry<Piece, Bitboard> entry2 :
					entry1.getValue().entrySet()) {
				Bitboard bitboard = entry2.getValue();
				bitboard.reset();
			}
		}
		this.fullMoveCounter = 1;
		this.updateSummaryBitboards();
		this.setPositionHash();
	}
	
	public void setPositionFenstring(String fenstring) {
		String[] parts = fenstring.split(" ");
		
		String placement = parts[0];
		String[] placementParts = placement.split("/");
		this.setPositionEmpty();
		for(int i = 0; i < 8; i++) {
			// Start with rank 8 and go to rank 1.
			long mask = 1L << 8 * (7 - i);   // a8, a7, ..., a1
			int placementPartLength = placementParts[i].length();
			for(int j = 0; j < placementPartLength; j++) {
				char initial = placementParts[i].charAt(j);
				Color color = Color.WHITE;
				if(Character.isLowerCase(initial)) {
					color = Color.BLACK;
				}
				Piece piece = Piece.initialToPiece(initial);
				if(piece == Piece.NOPIECE) {
					// A numeric amount of blank squares.
					mask <<= (initial - '1');
				} else {
					this.bitboards.get(color).get(piece).updateUnion(mask);
				}
				if(j < placementPartLength - 1) {
					// If we happen to be on h8 it may cause an out-of-bounds
					// error otherwise.
					mask <<= 1;
				}
			}
		}
		updateSummaryBitboards();
		
		String activeColor = parts[1];
		if(activeColor.equals("w")) {
			this.turn = Color.WHITE;
		}
		else {
			this.turn = Color.BLACK;
		}
		
		String castling = parts[2];
		for(Color color : Color.values()) {
			this.castleRights.put(color, new HashMap<Castle, Boolean>());
			for(Castle castle : Castle.values()) {
				this.castleRights.get(color).put(castle, false);
			}
		}
		if(castling.contains("K")) {
			this.castleRights.get(Color.WHITE).put(Castle.KINGSIDE, true);
		}
		if(castling.contains("Q")) {
			this.castleRights.get(Color.WHITE).put(Castle.QUEENSIDE, true);
		}
		if(castling.contains("k")) {
			this.castleRights.get(Color.BLACK).put(Castle.KINGSIDE, true);
		}
		if(castling.contains("q")) {
			this.castleRights.get(Color.BLACK).put(Castle.QUEENSIDE, true);
		}
		
		String enPassantTarget = parts[3];
		if(enPassantTarget.equals("-")) {
			this.enPassantTarget = 0;
		}
		else {
			this.enPassantTarget = NotationHelper.squareToCoord(
					enPassantTarget);
		}
		
		// TODO: implement the halfmove clock
		
		String fullMoveCounter = parts[5];
		this.fullMoveCounter = Integer.parseInt(fullMoveCounter);
		
		this.setPositionHash();
	}
	
	public Piece pieceOnSquare(long mask) {
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 :
				this.bitboards.entrySet()) {
			for(Map.Entry<Piece, Bitboard> entry2 :
					entry1.getValue().entrySet()) {
				Piece piece = entry2.getKey();
				Bitboard bitboard = entry2.getValue();
				if(bitboard.intersects(mask)) {
					return piece;
				}
			}
		}
		return Piece.NOPIECE;
	}
	
	private void setPositionHash() {
		for(byte i = 0; i < 64; i++) {
			long mask = 1L << i;
			for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 :
					this.bitboards.entrySet()) {
				Color color = entry1.getKey();
				for(Map.Entry<Piece, Bitboard> entry2 :
						entry1.getValue().entrySet()) {
					Piece piece = entry2.getKey();
					Bitboard bitboard = entry2.getValue();
					if(bitboard.intersects(mask)) {
						this.positionHash ^= this.positionHasher.getMask(
							color, piece, i);
						if(piece == Piece.PAWN || piece == Piece.KING) {
							this.positionHashPawnsKings ^=
								this.positionHasher.getMask(color, piece, i);
						}
					}
				}
			}
			if(this.enPassantTarget == mask) {
				this.positionHash ^=
					this.positionHasher.getMaskEnPassantTarget(i);
				this.positionHashPawnsKings ^=
					this.positionHasher.getMaskEnPassantTarget(i);
			}
		}
		if(this.turn == Color.BLACK) {
			this.positionHash ^= this.positionHasher.getMaskTurn();
		}
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
	}

	private static LegalMoveGenerator legalMoveGenerator =
		new LegalMoveGenerator();
	private static NotationHelper notationHelper = new NotationHelper();
	private PositionHasher positionHasher = null;
	
	public Map<Color, Map<Piece, Bitboard>> bitboards =
		new HashMap<Color, Map<Piece, Bitboard>>();
	public Map<Color, Bitboard> playerBitboards =
		new HashMap<Color, Bitboard>();
	public Bitboard allPieces;
	
	// Convenience masks for castling
	public Bitboard bbA1 = new Bitboard("a1");
	public Bitboard bbA8 = new Bitboard("a8");
	public Bitboard bbH1 = new Bitboard("h1");
	public Bitboard bbH8 = new Bitboard("h8");
	public Bitboard bbD1 = new Bitboard("d1");
	public Bitboard bbD8 = new Bitboard("d8");
	public Bitboard bbF1 = new Bitboard("f1");
	public Bitboard bbF8 = new Bitboard("f8");
		
	public Color turn = Color.WHITE;
	// If the last move was a double pawn move, this is the destination
	// coordinate.
	public long enPassantTarget = 0;
	public Map<Color, Map<Castle, Boolean>> castleRights =
			new HashMap<Color, Map<Castle, Boolean>>();
	public int fullMoveCounter = 1;
	
	// These are used for the transposition tables.
	public long positionHash = 0;
	public long positionHashPawnsKings = 0;
};
