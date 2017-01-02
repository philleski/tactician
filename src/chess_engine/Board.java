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
		
		this.whiteKingIndex = 4;   // 1L << index is the coordinate.
		this.blackKingIndex = 60;
		
		this.turn = Color.WHITE;
		// If the last move was a double pawn move, this is the destination
		// coordinate.
		this.enPassantTarget = 0;
		this.whiteCastleRightKingside = true;
		this.whiteCastleRightQueenside = true;
		this.blackCastleRightKingside = true;
		this.blackCastleRightQueenside = true;
		
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
		this.whiteCastleRightKingside = other.whiteCastleRightKingside;
		this.whiteCastleRightQueenside = other.whiteCastleRightQueenside;
		this.blackCastleRightKingside = other.blackCastleRightKingside;
		this.blackCastleRightQueenside = other.blackCastleRightQueenside;
		this.whiteKingIndex = other.whiteKingIndex;
		this.blackKingIndex = other.blackKingIndex;
		// Keep the same object so that we don't have to reinitialize.
		this.positionHasher = other.positionHasher;
		this.positionHash = other.positionHash;
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
						if(piece == Piece.KNIGHT) {
							representation = 'N';
						}
						if(color == Color.WHITE) {
							representation = (char) (representation - 'A' + 'a');
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
		this.whitePieces = new Bitboard();
		this.blackPieces = new Bitboard();
		this.allPieces = new Bitboard();
		for(Map.Entry<Color, Map<Piece, Bitboard>> entry1 : this.bitboards.entrySet()) {
			Color color = entry1.getKey();
			for(Map.Entry<Piece, Bitboard> entry2 : entry1.getValue().entrySet()) {
				Bitboard bitboard = entry2.getValue();
				if(color == Color.WHITE) {
					this.whitePieces.data |= bitboard.data;
					this.allPieces.data |= bitboard.data;
				} else {
					this.blackPieces.data |= bitboard.data;
					this.allPieces.data |= bitboard.data;
				}
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

	public void move(Move move)
			throws IllegalMoveException {
		long sourceMask = 1L << move.source;
		long destinationMask = 1L << move.destination;
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
					break;
				}
			}
			if(found) {
				break;
			}
		}
		
		if(this.turn == Color.WHITE) {
			if((this.bitboards.get(Color.WHITE).get(Piece.PAWN).data & sourceMask) != 0 &&
					destinationMask == this.enPassantTarget) {
				this.bitboards.get(Color.BLACK).get(Piece.PAWN).data &= ~((destinationMask >>> 8) ^ 0);
				this.positionHash ^= this.positionHasher.getMaskBlackPawn(
						(byte)(move.destination - 8));
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.PAWN).data & sourceMask) != 0 &&
					sourceMask << 16 == destinationMask) {
				if(this.enPassantTarget != 0) {
					this.positionHash ^=
							this.positionHasher.getMaskEnPassantTarget((byte)
									NotationHelper.coordToIndex(
											this.enPassantTarget));
				}
				this.enPassantTarget = destinationMask >> 8;
				this.positionHash ^=
						this.positionHasher.getMaskEnPassantTarget(
								(byte)(move.destination - 8));
			} else {
				if(this.enPassantTarget != 0) {
					this.positionHash ^=
							this.positionHasher.getMaskEnPassantTarget((byte)
									NotationHelper.coordToIndex(
											this.enPassantTarget));
				}
				this.enPassantTarget = 0;
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.BISHOP).data & sourceMask) != 0) {
				this.bitboards.get(Color.WHITE).get(Piece.BISHOP).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.WHITE).get(Piece.BISHOP).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskWhiteBishop(
						move.source, move.destination);
			} else if((this.bitboards.get(Color.WHITE).get(Piece.KING).data & sourceMask) != 0) {
				this.bitboards.get(Color.WHITE).get(Piece.KING).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.WHITE).get(Piece.KING).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
				this.whiteCastleRightKingside = false;
				this.whiteCastleRightQueenside = false;
				if(move.source - 2 == move.destination) {
					// Castle queenside
					this.bitboards.get(Color.WHITE).get(Piece.ROOK).data &= this.maskA1Negative;
					this.bitboards.get(Color.WHITE).get(Piece.ROOK).data |= this.maskD1;
					this.whiteCastleRightKingside = false;
					this.whiteCastleRightQueenside = false;
					this.positionHash ^= this.positionHasher.getMaskWhiteRook(
							(byte)0, (byte)3);
				}
				else if(move.source + 2 == move.destination) {
					// Castle kingside
					this.bitboards.get(Color.WHITE).get(Piece.ROOK).data &= this.maskH1Negative;
					this.bitboards.get(Color.WHITE).get(Piece.ROOK).data |= this.maskF1;
					this.whiteCastleRightKingside = false;
					this.whiteCastleRightQueenside = false;
					this.positionHash ^= this.positionHasher.getMaskWhiteRook(
							(byte)5, (byte)7);
				}
				this.whiteKingIndex = move.destination;
				this.positionHash ^= this.positionHasher.getMaskWhiteKing(
						move.source, move.destination);
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
			} else if((this.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data & sourceMask) != 0) {
				this.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskWhiteKnight(
						move.source, move.destination);
			} else if((this.bitboards.get(Color.WHITE).get(Piece.PAWN).data & sourceMask) != 0) {
				this.bitboards.get(Color.WHITE).get(Piece.PAWN).data &= ~(sourceMask ^ 0);
				this.positionHash ^= this.positionHasher.getMaskWhitePawn(
						move.source);
				if(move.destination < 56) {
					this.bitboards.get(Color.WHITE).get(Piece.PAWN).data |= destinationMask;
					this.positionHash ^= this.positionHasher.getMaskWhitePawn(
							move.destination);
				}
				else if(move.promoteTo == Piece.BISHOP) {
					this.bitboards.get(Color.WHITE).get(Piece.BISHOP).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskWhiteBishop(
									move.destination);
				}
				else if(move.promoteTo == Piece.KNIGHT) {
					this.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskWhiteKnight(
									move.destination);
				}
				else if(move.promoteTo == Piece.QUEEN) {
					this.bitboards.get(Color.WHITE).get(Piece.QUEEN).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskWhiteQueen(
									move.destination);
				}
				else if(move.promoteTo == Piece.ROOK) {
					this.bitboards.get(Color.WHITE).get(Piece.ROOK).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskWhiteRook(
									move.destination);
				}
				else {
					throw new IllegalMoveException(
							"Don't know what to promote to.");
				}
			} else if((this.bitboards.get(Color.WHITE).get(Piece.QUEEN).data & sourceMask) != 0) {
				this.bitboards.get(Color.WHITE).get(Piece.QUEEN).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.WHITE).get(Piece.QUEEN).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskWhiteQueen(
						move.source, move.destination);
			} else if((this.bitboards.get(Color.WHITE).get(Piece.ROOK).data & sourceMask) != 0) {
				this.bitboards.get(Color.WHITE).get(Piece.ROOK).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.WHITE).get(Piece.ROOK).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskWhiteRook(
						move.source, move.destination);
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
				if(move.source == 0) {
					this.whiteCastleRightQueenside = false;
				}
				else if(move.source == 7) {
					this.whiteCastleRightKingside = false;
				}
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
			}
		}
		else {
			if((this.bitboards.get(Color.BLACK).get(Piece.PAWN).data & sourceMask) != 0 &&
					destinationMask == this.enPassantTarget) {
				this.bitboards.get(Color.WHITE).get(Piece.PAWN).data &= ~((destinationMask << 8) ^ 0);
				this.positionHash ^= this.positionHasher.getMaskWhitePawn(
						(byte)(move.destination + 8));
			}
			if((this.bitboards.get(Color.BLACK).get(Piece.PAWN).data & sourceMask) != 0 &&
					sourceMask >>> 16 == destinationMask) {
				if(this.enPassantTarget != 0) {
					this.positionHash ^=
							this.positionHasher.getMaskEnPassantTarget((byte)
									NotationHelper.coordToIndex(
											this.enPassantTarget));
				}
				this.enPassantTarget = destinationMask << 8;
				this.positionHash ^=
						this.positionHasher.getMaskEnPassantTarget(
								(byte)(move.destination + 8));
			} else {
				if(this.enPassantTarget != 0) {
					this.positionHash ^=
							this.positionHasher.getMaskEnPassantTarget((byte)
									NotationHelper.coordToIndex(
											this.enPassantTarget));
				}
				this.enPassantTarget = 0;
			}
			if((this.bitboards.get(Color.BLACK).get(Piece.BISHOP).data & sourceMask) != 0) {
				this.bitboards.get(Color.BLACK).get(Piece.BISHOP).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.BLACK).get(Piece.BISHOP).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskBlackBishop(
						move.source, move.destination);
			} else if((this.bitboards.get(Color.BLACK).get(Piece.KING).data & sourceMask) != 0) {
				this.bitboards.get(Color.BLACK).get(Piece.KING).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.BLACK).get(Piece.KING).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
				this.blackCastleRightKingside = false;
				this.blackCastleRightQueenside = false;
				if(move.source - 2 == move.destination) {
					// Castle queenside
					this.bitboards.get(Color.BLACK).get(Piece.ROOK).data &= this.maskA8Negative;
					this.bitboards.get(Color.BLACK).get(Piece.ROOK).data |= this.maskD8;
					this.blackCastleRightKingside = false;
					this.blackCastleRightQueenside = false;
					this.positionHash ^= this.positionHasher.getMaskBlackRook(
							(byte)56, (byte)59);
				}
				else if(move.source + 2 == move.destination) {
					// Castle kingside
					this.bitboards.get(Color.BLACK).get(Piece.ROOK).data &= this.maskH8Negative;
					this.bitboards.get(Color.BLACK).get(Piece.ROOK).data |= this.maskF8;
					this.blackCastleRightKingside = false;
					this.blackCastleRightQueenside = false;
					this.positionHash ^= this.positionHasher.getMaskBlackRook(
							(byte)61, (byte)63);
				}
				this.blackKingIndex = move.destination;
				this.positionHash ^= this.positionHasher.getMaskBlackKing(
						move.source, move.destination);
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
			} else if((this.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data & sourceMask) != 0) {
				this.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskBlackKnight(
						move.source, move.destination);
			} else if((this.bitboards.get(Color.BLACK).get(Piece.PAWN).data & sourceMask) != 0) {
				this.bitboards.get(Color.BLACK).get(Piece.PAWN).data &= ~(sourceMask ^ 0);
				this.positionHash ^= this.positionHasher.getMaskBlackPawn(
						move.source);
				if(move.destination > 7) {
					this.bitboards.get(Color.BLACK).get(Piece.PAWN).data |= destinationMask;
					this.positionHash ^= this.positionHasher.getMaskBlackPawn(
							move.destination);
				}
				else if(move.promoteTo == Piece.BISHOP) {
					this.bitboards.get(Color.BLACK).get(Piece.BISHOP).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskBlackBishop(
									move.destination);
				}
				else if(move.promoteTo == Piece.KNIGHT) {
					this.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskBlackKnight(
									move.destination);
				}
				else if(move.promoteTo == Piece.QUEEN) {
					this.bitboards.get(Color.BLACK).get(Piece.QUEEN).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskBlackQueen(
									move.destination);
				}
				else if(move.promoteTo == Piece.ROOK) {
					this.bitboards.get(Color.BLACK).get(Piece.ROOK).data |= destinationMask;
					this.positionHash ^=
							this.positionHasher.getMaskBlackRook(
									move.destination);
				}
				else {
					throw new IllegalMoveException(
							"Don't know what to promote to.");
				}
			} else if((this.bitboards.get(Color.BLACK).get(Piece.QUEEN).data & sourceMask) != 0) {
				this.bitboards.get(Color.BLACK).get(Piece.QUEEN).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.BLACK).get(Piece.QUEEN).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskBlackQueen(
						move.source, move.destination);
			} else if((this.bitboards.get(Color.BLACK).get(Piece.ROOK).data & sourceMask) != 0) {
				this.bitboards.get(Color.BLACK).get(Piece.ROOK).data &= ~(sourceMask ^ 0);
				this.bitboards.get(Color.BLACK).get(Piece.ROOK).data |= destinationMask;
				this.positionHash ^= this.positionHasher.getMaskBlackRook(
						move.source, move.destination);
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
				if(move.source == 56) {
					this.blackCastleRightQueenside = false;
				}
				else if(move.source == 63) {
					this.blackCastleRightKingside = false;
				}
				this.positionHash ^= this.positionHasher.getMaskCastleRights(
						this.whiteCastleRightKingside,
						this.whiteCastleRightQueenside,
						this.blackCastleRightKingside,
						this.blackCastleRightQueenside);
			}
		}
		this.turn = Color.getOpposite(this.turn);
		this.positionHash ^= this.positionHasher.getMaskTurn();
		updateSummaryBitboards();
	}
	
	public void move(String algebraic) throws IllegalMoveException {
		Move m = notationHelper.algebraicToMove(this, algebraic);
		this.move(m);
	}
	
	private void setPositionEmpty() {
		this.bitboards.get(Color.WHITE).get(Piece.BISHOP).data = 0;
		this.bitboards.get(Color.WHITE).get(Piece.KING).data = 0;
		this.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data = 0;
		this.bitboards.get(Color.WHITE).get(Piece.PAWN).data = 0;
		this.bitboards.get(Color.WHITE).get(Piece.QUEEN).data = 0;
		this.bitboards.get(Color.WHITE).get(Piece.ROOK).data = 0;
		
		this.bitboards.get(Color.BLACK).get(Piece.BISHOP).data = 0;
		this.bitboards.get(Color.BLACK).get(Piece.KING).data = 0;
		this.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data = 0;
		this.bitboards.get(Color.BLACK).get(Piece.PAWN).data = 0;
		this.bitboards.get(Color.BLACK).get(Piece.QUEEN).data = 0;
		this.bitboards.get(Color.BLACK).get(Piece.ROOK).data = 0;
		
		this.whitePieces = new Bitboard();
		this.blackPieces = new Bitboard();
		this.allPieces = new Bitboard();
		
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
				char piece = placementParts[i].charAt(j);
				
				if(piece == 'b') {
					this.bitboards.get(Color.BLACK).get(Piece.BISHOP).data |= mask;
				}
				else if(piece == 'k') {
					this.bitboards.get(Color.BLACK).get(Piece.KING).data |= mask;
					this.blackKingIndex = NotationHelper.coordToIndex(mask);
				}
				else if(piece == 'n') {
					this.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data |= mask;
				}
				else if(piece == 'p') {
					this.bitboards.get(Color.BLACK).get(Piece.PAWN).data |= mask;
				}
				else if(piece == 'q') {
					this.bitboards.get(Color.BLACK).get(Piece.QUEEN).data |= mask;
				}
				else if(piece == 'r') {
					this.bitboards.get(Color.BLACK).get(Piece.ROOK).data |= mask;
				}
				else if(piece == 'B') {
					this.bitboards.get(Color.WHITE).get(Piece.BISHOP).data |= mask;
				}
				else if(piece == 'K') {
					this.bitboards.get(Color.WHITE).get(Piece.KING).data |= mask;
					this.whiteKingIndex = NotationHelper.coordToIndex(mask);
				}
				else if(piece == 'N') {
					this.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data |= mask;
				}
				else if(piece == 'P') {
					this.bitboards.get(Color.WHITE).get(Piece.PAWN).data |= mask;
				}
				else if(piece == 'Q') {
					this.bitboards.get(Color.WHITE).get(Piece.QUEEN).data |= mask;
				}
				else if(piece == 'R') {
					this.bitboards.get(Color.WHITE).get(Piece.ROOK).data |= mask;
				}
				else {
					// A numeric amount of blank squares.
					mask <<= (piece - '1');
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
		this.whiteCastleRightKingside = false;
		this.whiteCastleRightQueenside = false;
		this.blackCastleRightKingside = false;
		this.blackCastleRightQueenside = false;
		if(castling.contains("K")) {
			this.whiteCastleRightKingside = true;
		}
		if(castling.contains("Q")) {
			this.whiteCastleRightQueenside = true;
		}
		if(castling.contains("k")) {
			this.blackCastleRightKingside = true;
		}
		if(castling.contains("q")) {
			this.blackCastleRightQueenside = true;
		}
		
		String enPassantTarget = parts[3];
		if(enPassantTarget.equals("-")) {
			this.enPassantTarget = 0;
		}
		else {
			this.enPassantTarget = NotationHelper.squareToCoord(
					enPassantTarget);
		}
		
		this.setPositionHash();
		
		// TODO: Implement the halfmove clock and possibly fullmove number.
	}
	
	private void setPositionHash() {
		for(byte i = 0; i < 64; i++) {
			long mask = 1L << i;
			if((this.bitboards.get(Color.BLACK).get(Piece.BISHOP).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskBlackBishop(i);
			}
			if((this.bitboards.get(Color.BLACK).get(Piece.KING).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskBlackKing(i);
			}
			if((this.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskBlackKnight(i);
			}
			if((this.bitboards.get(Color.BLACK).get(Piece.PAWN).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskBlackPawn(i);
			}
			if((this.bitboards.get(Color.BLACK).get(Piece.QUEEN).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskBlackQueen(i);
			}
			if((this.bitboards.get(Color.BLACK).get(Piece.ROOK).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskBlackRook(i);
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.BISHOP).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskWhiteBishop(i);
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.KING).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskWhiteKing(i);
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskWhiteKnight(i);
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.PAWN).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskWhitePawn(i);
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.QUEEN).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskWhiteQueen(i);
			}
			if((this.bitboards.get(Color.WHITE).get(Piece.ROOK).data & mask) != 0) {
				this.positionHash ^= this.positionHasher.getMaskWhiteRook(i);
			}
			if(this.enPassantTarget == mask) {
				this.positionHash ^=
						this.positionHasher.getMaskEnPassantTarget(i);
			}
		}
		if(this.turn == Color.BLACK) {
			this.positionHash ^= this.positionHasher.getMaskTurn();
		}
		this.positionHash ^= this.positionHasher.getMaskCastleRights(
				this.whiteCastleRightKingside, this.whiteCastleRightQueenside,
				this.blackCastleRightKingside, this.blackCastleRightQueenside);
	}

	private static LegalMoveGenerator legalMoveGenerator = new LegalMoveGenerator();
	private static NotationHelper notationHelper = new NotationHelper();
	private PositionHasher positionHasher = null;
	
	public Map<Color, Map<Piece, Bitboard>> bitboards = new HashMap<Color, Map<Piece, Bitboard>>();
	public Bitboard whitePieces;
	public Bitboard blackPieces;
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
	
	public int whiteKingIndex = 4;   // 1L << index is the coordinate.
	public int blackKingIndex = 60;
	
	public Color turn = Color.WHITE;
	// If the last move was a double pawn move, this is the destination
	// coordinate.
	public long enPassantTarget = 0;
	public boolean whiteCastleRightKingside = true;
	public boolean whiteCastleRightQueenside = true;
	public boolean blackCastleRightKingside = true;
	public boolean blackCastleRightQueenside = true;
	
	// This is used for the transposition tables.
	public long positionHash = 0;
};
