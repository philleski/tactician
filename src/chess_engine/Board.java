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
		whiteBitboards.put(Piece.PAWN, new Bitboard(
			"a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2"));
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
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 : other.bitboards.entrySet()) {
			Color color = entry1.getKey();
			Map<Piece, Bitboard> bitboardsForColor = new HashMap<Piece, Bitboard>();
			for(Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
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
		// Keep the same object so that we don't have to reinitialize.
		this.positionHasher = other.positionHasher;
		this.positionHash = other.positionHash;
		this.positionHashPawnsKings = other.positionHashPawnsKings;
	}
	
	public String toString() {
		String result = "";
		String rowReversed = "";
		for(long i = 63; i >= 0; i--) {
			long mask = 1L << i;
			
			char representation = ' ';
			for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
				Color color = entry1.getKey();
				for(Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
					Piece piece = entry2.getKey();
					Bitboard bitboard = entry2.getValue();
					if((bitboard.data & mask) != 0) {
						representation = piece.name().charAt(0);
						if(piece == Piece.KNIGHT) {
							representation = 'N';
						}
						if(color == Color.WHITE) {
							representation = (char)(representation - 'A' + 'a');
						}
					}
				}
			}
			rowReversed += representation;
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
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
			Color color = entry1.getKey();
			for(Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
				Bitboard bitboard = entry2.getValue();
				this.playerBitboards.get(color).data |= bitboard.data;
				this.allPieces.data |= bitboard.data;
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

	public void move(Move move) throws IllegalMoveException {
		long sourceMask = 1L << move.source;
		long destinationMask = 1L << move.destination;
		Color turnFlipped = Color.flip(this.turn);
		
		byte rookKingsideSource;
		byte rookKingsideSourceOpp;
		byte rookKingsideDestination;
		byte rookQueensideSource;
		byte rookQueensideSourceOpp;
		byte rookQueensideDestination;
		byte destinationRetreatedOneRow;
		long destinationMaskRetreatedOneRow;
		long maskKingsideRookStartNegative;
		long maskKingsideRookEnd;
		long maskQueensideRookStartNegative;
		long maskQueensideRookEnd;
		long sourceMaskAdvancedTwoRows;
		if(this.turn == Color.WHITE) {
			destinationRetreatedOneRow = (byte)(move.destination - 8);
			destinationMaskRetreatedOneRow = destinationMask >>> 8;
			maskKingsideRookStartNegative = this.maskH1Negative;
			maskKingsideRookEnd = this.maskF1;
			maskQueensideRookStartNegative = this.maskA1Negative;
			maskQueensideRookEnd = this.maskD1;
			rookKingsideSource = 7;
			rookKingsideSourceOpp = 63;
			rookKingsideDestination = 5;
			rookQueensideSource = 0;
			rookQueensideSourceOpp = 56;
			rookQueensideDestination = 3;
			sourceMaskAdvancedTwoRows = sourceMask << 16;
		} else {
			destinationRetreatedOneRow = (byte)(move.destination + 8);
			destinationMaskRetreatedOneRow = destinationMask << 8;
			maskKingsideRookStartNegative = this.maskH8Negative;
			maskKingsideRookEnd = this.maskF8;
			maskQueensideRookStartNegative = this.maskA8Negative;
			maskQueensideRookEnd = this.maskD8;
			rookKingsideSource = 63;
			rookKingsideSourceOpp = 7;
			rookKingsideDestination = 61;
			rookQueensideSource = 56;
			rookQueensideSourceOpp = 0;
			rookQueensideDestination = 59;
			sourceMaskAdvancedTwoRows = sourceMask >>> 16;
		}
		
		// If the opponent's rook is captured, remove their castling rights.
		if((destinationMask & this.bitboards.get(turnFlipped).get(Piece.ROOK).data) != 0) {
			this.positionHash ^= this.positionHasher.getMaskCastleRights(
				this.castleRights);
			if(move.destination == rookQueensideSourceOpp) {
				this.castleRights.get(turnFlipped).put(Castle.QUEENSIDE, false);
			} else if(move.destination == rookKingsideSourceOpp) {
				this.castleRights.get(turnFlipped).put(Castle.KINGSIDE, false);
			}
			this.positionHash ^= this.positionHasher.getMaskCastleRights(
				this.castleRights);
		}
		
		// Remove whatever is in the destination spot.
		boolean found = false;
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
			Color color = entry1.getKey();
			for(Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
				Piece piece = entry2.getKey();
				Bitboard bitboard = entry2.getValue();
				if((bitboard.data & destinationMask) != 0) {
					bitboard.data &= ~(destinationMask ^ 0);
					this.positionHash ^= this.positionHasher.getMask(color, piece, move.destination);
					if(piece == Piece.PAWN || piece == Piece.KING) {
						this.positionHashPawnsKings ^= this.positionHasher.getMask(color, piece, move.destination);
					}
					break;
				}
			}
			if(found) {
				break;
			}
		}
		
		Piece movedPiece = Piece.NOPIECE;
		for(Map.Entry<Piece, Bitboard> entry : this.bitboards.get(this.turn).entrySet()) {
			Piece piece = entry.getKey();
			Bitboard bitboard = entry.getValue();
			if((bitboard.data & sourceMask) != 0) {
				movedPiece = piece;
				bitboard.data &= ~(sourceMask ^ 0);
				bitboard.data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMask(this.turn,
					piece, move.source, move.destination);
				if(piece == Piece.PAWN || piece == Piece.KING) {
					this.positionHashPawnsKings ^= this.positionHasher.getMask(
						this.turn, piece, move.source, move.destination);
				}
				break;
			}
		}
		
		if(movedPiece == Piece.PAWN && destinationMask == this.enPassantTarget) {
			this.bitboards.get(turnFlipped).get(Piece.PAWN).data &=
				~(destinationMaskRetreatedOneRow ^ 0);
			this.positionHash ^= this.positionHasher.getMask(turnFlipped,
				Piece.PAWN, destinationRetreatedOneRow);
			this.positionHashPawnsKings ^= this.positionHasher.getMask(turnFlipped,
				Piece.PAWN, destinationRetreatedOneRow);
		}
		if(movedPiece == Piece.PAWN && sourceMaskAdvancedTwoRows == destinationMask) {
			if(this.enPassantTarget != 0) {
				this.positionHash ^= this.positionHasher.getMaskEnPassantTarget((byte)
					NotationHelper.coordToIndex(this.enPassantTarget));
			}
			this.enPassantTarget = destinationMaskRetreatedOneRow;
			this.positionHash ^= this.positionHasher.getMaskEnPassantTarget(
				destinationRetreatedOneRow);
			this.positionHashPawnsKings ^= this.positionHasher.getMaskEnPassantTarget(
				destinationRetreatedOneRow);
		} else {
			if(this.enPassantTarget != 0) {
				this.positionHash ^= this.positionHasher.getMaskEnPassantTarget((byte)
					NotationHelper.coordToIndex(this.enPassantTarget));
				this.positionHashPawnsKings ^= this.positionHasher.getMaskEnPassantTarget((byte)
					NotationHelper.coordToIndex(this.enPassantTarget));
			}
			this.enPassantTarget = 0;
		}
		if(movedPiece == Piece.KING) {
			this.positionHash ^= this.positionHasher.getMaskCastleRights(
				this.castleRights);
			this.castleRights.get(this.turn).put(Castle.KINGSIDE, false);
			this.castleRights.get(this.turn).put(Castle.QUEENSIDE, false);
			if(move.source - 2 == move.destination) {
				// Castle queenside
				this.bitboards.get(this.turn).get(Piece.ROOK).data &=
					maskQueensideRookStartNegative;
				this.bitboards.get(this.turn).get(Piece.ROOK).data |=
					maskQueensideRookEnd;
				this.positionHash ^= this.positionHasher.getMask(this.turn,
					Piece.ROOK, rookQueensideSource, rookQueensideDestination);
			} else if(move.source + 2 == move.destination) {
				// Castle kingside
				this.bitboards.get(this.turn).get(Piece.ROOK).data &=
					maskKingsideRookStartNegative;
				this.bitboards.get(this.turn).get(Piece.ROOK).data |=
					maskKingsideRookEnd;
				this.positionHash ^= this.positionHasher.getMask(this.turn,
					Piece.ROOK, rookKingsideSource, rookKingsideDestination);
			}
			this.positionHash ^= this.positionHasher.getMaskCastleRights(
				this.castleRights);
		} else if(movedPiece == Piece.PAWN) {
			if(move.promoteTo != Piece.NOPIECE) {
				this.bitboards.get(this.turn).get(Piece.PAWN).data &= ~(destinationMask ^ 0);
				this.bitboards.get(this.turn).get(move.promoteTo).data |= destinationMask;
				// We switched the position hash for all pieces above under the
				// assumption that the destination piece would be the same as
				// the source piece. Since that's not the case with promotion,
				// do the xor again to unset the destination mask for the pawn.
				this.positionHash ^= this.positionHasher.getMask(this.turn, Piece.PAWN,
					move.destination);
				this.positionHash ^= this.positionHasher.getMask(this.turn, move.promoteTo,
					move.destination);
				this.positionHashPawnsKings ^= this.positionHasher.getMask(this.turn, Piece.PAWN,
					move.destination);
			}
		} else if(movedPiece == Piece.ROOK) {
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
		
		if(this.turn == Color.BLACK) {
			this.fullMoveCounter++;
		}
		
		this.turn = Color.flip(this.turn);
		this.positionHash ^= this.positionHasher.getMaskTurn();
		updateSummaryBitboards();
	}
	
	public void move(String algebraic) throws IllegalMoveException {
		Move m = notationHelper.algebraicToMove(this, algebraic);
		this.move(m);
	}
	
	private void setPositionEmpty() {
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
			for(Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
				Bitboard bitboard = entry2.getValue();
				bitboard.data = 0;
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
					this.bitboards.get(color).get(piece).data |= mask;
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
	
	private void setPositionHash() {
		for(byte i = 0; i < 64; i++) {
			long mask = 1L << i;
			for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
				Color color = entry1.getKey();
				for(Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
					Piece piece = entry2.getKey();
					Bitboard bitboard = entry2.getValue();
					if((bitboard.data & mask) != 0) {
						this.positionHash ^= this.positionHasher.getMask(color, piece, i);
						if(piece == Piece.PAWN || piece == Piece.KING) {
							this.positionHashPawnsKings ^=
								this.positionHasher.getMask(color, piece, i);
						}
					}
				}
			}
			if(this.enPassantTarget == mask) {
				this.positionHash ^= this.positionHasher.getMaskEnPassantTarget(i);
				this.positionHashPawnsKings ^= this.positionHasher.getMaskEnPassantTarget(i);
			}
		}
		if(this.turn == Color.BLACK) {
			this.positionHash ^= this.positionHasher.getMaskTurn();
		}
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
			this.castleRights);
	}

	private static LegalMoveGenerator legalMoveGenerator = new LegalMoveGenerator();
	private static NotationHelper notationHelper = new NotationHelper();
	private PositionHasher positionHasher = null;
	
	public Map<Color, Map<Piece, Bitboard>> bitboards = new HashMap<Color, Map<Piece, Bitboard>>();
	public Map<Color, Bitboard> playerBitboards = new HashMap<Color, Bitboard>();
	public Bitboard allPieces;
	
	// Convenience masks for castling
	public long maskA1Negative = ~(notationHelper.generateMask("a1") ^ 0);
	public long maskA8Negative = ~(notationHelper.generateMask("a8") ^ 0);
	public long maskH1Negative = ~(notationHelper.generateMask("h1") ^ 0);
	public long maskH8Negative = ~(notationHelper.generateMask("h8") ^ 0);
	public long maskD1 = notationHelper.generateMask("d1");
	public long maskD8 = notationHelper.generateMask("d8");
	public long maskF1 = notationHelper.generateMask("f1");
	public long maskF8 = notationHelper.generateMask("f8");
		
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
