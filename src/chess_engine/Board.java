package chess_engine;

import java.util.ArrayList;

public class Board {
	
	private static LegalMoveGenerator legalMoveGenerator = new LegalMoveGenerator();
	private static NotationHelper notationHelper = new NotationHelper();
	
	// All the state information below.
	
	// bits correspond to board squares A1, B1, C1, ...
	public long whiteBishops =	0x0000000000000024L;
	public long whiteKings =	0x0000000000000010L;
	public long whiteKnights =	0x0000000000000042L;
	public long whitePawns =	0x000000000000ff00L;
	public long whiteQueens =	0x0000000000000008L;
	public long whiteRooks =	0x0000000000000081L;
	public long blackBishops =	0x2400000000000000L;
	public long blackKings =	0x1000000000000000L;
	public long blackKnights =	0x4200000000000000L;
	public long blackPawns =	0x00ff000000000000L;
	public long blackQueens =	0x0800000000000000L;
	public long blackRooks =	0x8100000000000000L;
	public int whiteKingIndex = 4;   // 1L << index is the coordinate.
	public int blackKingIndex = 60;
	
	public long whitePieces = whiteBishops | whiteKings | whiteKnights |
			whitePawns | whiteQueens | whiteRooks;
	public long blackPieces = blackBishops | blackKings | blackKnights |
			blackPawns | blackQueens | blackRooks;
	public long allPieces = whitePieces | blackPieces;
	
	public Color turn = Color.WHITE;
	// If the last move was a double pawn move, this is the destination
	// coordinate.
	public long enPassantTarget = 0;
	public boolean whiteCastleRightKingside = true;
	public boolean whiteCastleRightQueenside = true;
	public boolean blackCastleRightKingside = true;
	public boolean blackCastleRightQueenside = true;
	
	public Board() {
	}
	
	public Board(Board other) {
		this.whiteBishops = other.whiteBishops;
		this.whiteKings = other.whiteKings;
		this.whiteKnights = other.whiteKnights;
		this.whitePawns = other.whitePawns;
		this.whiteQueens = other.whiteQueens;
		this.whiteRooks = other.whiteRooks;
		this.blackBishops = other.blackBishops;
		this.blackKings = other.blackKings;
		this.blackKnights = other.blackKnights;
		this.blackPawns = other.blackPawns;
		this.blackQueens = other.blackQueens;
		this.blackRooks = other.blackRooks;
		this.whitePieces = this.whiteBishops | this.whiteKings |
				this.whiteKnights | this.whitePawns | this.whiteQueens |
				this.whiteRooks;
		this.blackPieces = this.blackBishops | this.blackKings |
				this.blackKnights | this.blackPawns | this.blackQueens |
				this.blackRooks;
		this.allPieces = this.whitePieces | this.blackPieces;
		this.turn = other.turn;
		this.enPassantTarget = other.enPassantTarget;
		this.whiteCastleRightKingside = other.whiteCastleRightKingside;
		this.whiteCastleRightQueenside = other.whiteCastleRightQueenside;
		this.blackCastleRightKingside = other.blackCastleRightKingside;
		this.blackCastleRightQueenside = other.blackCastleRightQueenside;
		this.whiteKingIndex = other.whiteKingIndex;
		this.blackKingIndex = other.blackKingIndex;
	}
	
	public String toString() {
		String result = "";
		String rowReversed = "";
		for(long i = 63; i >= 0; i--) {
			long mask = 1L << i;
			// The pawn representation is a little nonstandard because it's
			// hard to tell the difference between 'p' and 'P'.
			if((this.blackBishops & mask) != 0) {
				rowReversed += 'B';
			}
			else if((this.blackKings & mask) != 0) {
				rowReversed += 'K';
			}
			else if((this.blackKnights & mask) != 0) {
				rowReversed += 'N';
			}
			else if((this.blackPawns & mask) != 0) {
				rowReversed += 'p';
			}
			else if((this.blackQueens & mask) != 0) {
				rowReversed += 'Q';
			}
			else if((this.blackRooks & mask) != 0) {
				rowReversed += 'R';
			}
			else if((this.whiteBishops & mask) != 0) {
				rowReversed += 'b';
			}
			else if((this.whiteKings & mask) != 0) {
				rowReversed += 'k';
			}
			else if((this.whiteKnights & mask) != 0) {
				rowReversed += 'n';
			}
			else if((this.whitePawns & mask) != 0) {
				rowReversed += 'o';
			}
			else if((this.whiteQueens & mask) != 0) {
				rowReversed += 'q';
			}
			else if((this.whiteRooks & mask) != 0) {
				rowReversed += 'r';
			}
			else {
				rowReversed += ' ';
			}
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
		// Remove whatever is in the destination spot.
		if((this.whiteBishops & move.destination) != 0) {
			this.whiteBishops &= ~(move.destination ^ 0);
		}
		else if((this.whiteKings & move.destination) != 0) {
			this.whiteKings &= ~(move.destination ^ 0);
		}
		else if((this.whiteKnights & move.destination) != 0) {
			this.whiteKnights &= ~(move.destination ^ 0);
		}
		else if((this.whitePawns & move.destination) != 0) {
			this.whitePawns &= ~(move.destination ^ 0);
		}
		else if((this.whiteQueens & move.destination) != 0) {
			this.whiteQueens &= ~(move.destination ^ 0);
		}
		else if((this.whiteRooks & move.destination) != 0) {
			this.whiteRooks &= ~(move.destination ^ 0);
		}
		else if((this.blackBishops & move.destination) != 0) {
			this.blackBishops &= ~(move.destination ^ 0);
		}
		else if((this.blackKings & move.destination) != 0) {
			this.blackKings &= ~(move.destination ^ 0);
		}
		else if((this.blackKnights & move.destination) != 0) {
			this.blackKnights &= ~(move.destination ^ 0);
		}
		else if((this.blackPawns & move.destination) != 0) {
			this.blackPawns &= ~(move.destination ^ 0);
		}
		else if((this.blackQueens & move.destination) != 0) {
			this.blackQueens &= ~(move.destination ^ 0);
		}
		else if((this.blackRooks & move.destination) != 0) {
			this.blackRooks &= ~(move.destination ^ 0);
		}
		
		if(this.turn == Color.WHITE) {
			if((this.whitePawns & move.source) != 0 &&
					move.destination == this.enPassantTarget) {
				this.blackPawns &= ~((move.destination >>> 8) ^ 0);
			}
			if((this.whitePawns & move.source) != 0 &&
					move.source << 16 == move.destination) {
				this.enPassantTarget = move.destination;
			} else {
				this.enPassantTarget = 0;
			}
			if((this.whiteBishops & move.source) != 0) {
				this.whiteBishops &= ~(move.source ^ 0);
				this.whiteBishops |= move.destination;
			} else if((this.whiteKings & move.source) != 0) {
				this.whiteKings &= ~(move.source ^ 0);
				this.whiteKings |= move.destination;
				this.whiteCastleRightKingside = false;
				this.whiteCastleRightQueenside = false;
				if(move.source > 1L && move.source >>> 2 == move.destination) {
					// Castle queenside
					this.whiteRooks &= ~(0x0000000000000001L ^ 0);
					this.whiteRooks |= 0x0000000000000008L;
					this.whiteCastleRightKingside = false;
					this.whiteCastleRightQueenside = false;
				}
				else if(move.destination > 1L &&
						move.destination >>> 2 == move.source) {
					// Castle kingside
					this.whiteRooks &= ~(0x0000000000000080L ^ 0);
					this.whiteRooks |= 0x0000000000000020L;
					this.whiteCastleRightKingside = false;
					this.whiteCastleRightQueenside = false;
				}
				this.whiteKingIndex = 
						notationHelper.coordToIndex(move.destination);
			} else if((this.whiteKnights & move.source) != 0) {
				this.whiteKnights &= ~(move.source ^ 0);
				this.whiteKnights |= move.destination;
			} else if((this.whitePawns & move.source) != 0) {
				this.whitePawns &= ~(move.source ^ 0);
				if(move.destination >>> 56L == 0) {
					this.whitePawns |= move.destination;
				}
				else if(move.promoteTo == Piece.BISHOP) {
					this.whiteBishops |= move.destination;
				}
				else if(move.promoteTo == Piece.KNIGHT) {
					this.whiteKnights |= move.destination;
				}
				else if(move.promoteTo == Piece.QUEEN) {
					this.whiteQueens |= move.destination;
				}
				else if(move.promoteTo == Piece.ROOK) {
					this.whiteRooks |= move.destination;
				}
				else {
					throw new IllegalMoveException(
							"Don't know what to promote to.");
				}
			} else if((this.whiteQueens & move.source) != 0) {
				this.whiteQueens &= ~(move.source ^ 0);
				this.whiteQueens |= move.destination;
			} else if((this.whiteRooks & move.source) != 0) {
				this.whiteRooks &= ~(move.source ^ 0);
				this.whiteRooks |= move.destination;
				if(move.source == 0x0000000000000001L) {
					this.whiteCastleRightQueenside = false;
				}
				if(move.source == 0x0000000000000080L) {
					this.whiteCastleRightKingside = false;
				}
			}
		}
		else {
			if((this.blackPawns & move.source) != 0 &&
					move.destination == this.enPassantTarget) {
				this.whitePawns &= ~((move.destination << 8) ^ 0);
			}
			if((this.blackPawns & move.source) != 0 &&
					move.source >>> 16 == move.destination) {
				this.enPassantTarget = move.destination;
			} else {
				this.enPassantTarget = 0;
			}
			if((this.blackBishops & move.source) != 0) {
				this.blackBishops &= ~(move.source ^ 0);
				this.blackBishops |= move.destination;
			} else if((this.blackKings & move.source) != 0) {
				this.blackKings &= ~(move.source ^ 0);
				this.blackKings |= move.destination;
				this.blackCastleRightKingside = false;
				this.blackCastleRightQueenside = false;
				if(move.source > 1L && move.source >>> 2 == move.destination) {
					// Castle queenside
					this.blackRooks &= ~(0x0100000000000000L ^ 0);
					this.blackRooks |= 0x0800000000000000L;
					this.blackCastleRightKingside = false;
					this.blackCastleRightQueenside = false;
				}
				else if(move.destination > 1L &&
						move.destination >>> 2 == move.source) {
					// Castle kingside
					this.blackRooks &= ~(0x8000000000000000L ^ 0);
					this.blackRooks |= 0x2000000000000000L;
					this.blackCastleRightKingside = false;
					this.blackCastleRightQueenside = false;
				}
				this.blackKingIndex = 
						notationHelper.coordToIndex(move.destination);
			} else if((this.blackKnights & move.source) != 0) {
				this.blackKnights &= ~(move.source ^ 0);
				this.blackKnights |= move.destination;
			} else if((this.blackPawns & move.source) != 0) {
				this.blackPawns &= ~(move.source ^ 0);
				if(move.destination >>> 8L != 0) {
					this.blackPawns |= move.destination;
				}
				else if(move.promoteTo == Piece.BISHOP) {
					this.blackBishops |= move.destination;
				}
				else if(move.promoteTo == Piece.KNIGHT) {
					this.blackKnights |= move.destination;
				}
				else if(move.promoteTo == Piece.QUEEN) {
					this.blackQueens |= move.destination;
				}
				else if(move.promoteTo == Piece.ROOK) {
					this.blackRooks |= move.destination;
				}
				else {
					throw new IllegalMoveException(
							"Don't know what to promote to.");
				}
			} else if((this.blackQueens & move.source) != 0) {
				this.blackQueens &= ~(move.source ^ 0);
				this.blackQueens |= move.destination;
			} else if((this.blackRooks & move.source) != 0) {
				this.blackRooks &= ~(move.source ^ 0);
				this.blackRooks |= move.destination;
				if(move.source == 0x0100000000000000L) {
					this.blackCastleRightQueenside = false;
				}
				if(move.source == 0x8000000000000000L) {
					this.blackCastleRightKingside = false;
				}
			}
		}
		this.turn = Color.getOpposite(this.turn);
		this.whitePieces = this.whiteBishops | this.whiteKings |
				this.whiteKnights | this.whitePawns | this.whiteQueens |
				this.whiteRooks;
		this.blackPieces = this.blackBishops | this.blackKings |
				this.blackKnights | this.blackPawns | this.blackQueens |
				this.blackRooks;
		this.allPieces = this.whitePieces | this.blackPieces;
	}
	
	public void move(String algebraic) throws IllegalMoveException {
		Move m = notationHelper.algebraicToMove(this, algebraic);
		this.move(m);
	}
	
	private void setPositionEmpty() {
		this.whiteBishops = 0;
		this.whiteKings = 0;
		this.whiteKnights = 0;
		this.whitePawns = 0;
		this.whiteQueens = 0;
		this.whiteRooks = 0;
		
		this.blackBishops = 0;
		this.blackKings = 0;
		this.blackKnights = 0;
		this.blackPawns = 0;
		this.blackQueens = 0;
		this.blackRooks = 0;
		
		this.whitePieces = 0;
		this.blackPieces = 0;
		this.allPieces = 0;
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
					this.blackBishops |= mask;
				}
				else if(piece == 'k') {
					this.blackKings |= mask;
					this.blackKingIndex = notationHelper.coordToIndex(mask);
				}
				else if(piece == 'n') {
					this.blackKnights |= mask;
				}
				else if(piece == 'p') {
					this.blackPawns |= mask;
				}
				else if(piece == 'q') {
					this.blackQueens |= mask;
				}
				else if(piece == 'r') {
					this.blackRooks |= mask;
				}
				else if(piece == 'B') {
					this.whiteBishops |= mask;
				}
				else if(piece == 'K') {
					this.whiteKings |= mask;
					this.whiteKingIndex = notationHelper.coordToIndex(mask);
				}
				else if(piece == 'N') {
					this.whiteKnights |= mask;
				}
				else if(piece == 'P') {
					this.whitePawns |= mask;
				}
				else if(piece == 'Q') {
					this.whiteQueens |= mask;
				}
				else if(piece == 'R') {
					this.whiteRooks |= mask;
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
		this.whitePieces = this.whiteBishops | this.whiteKings |
				this.whiteKnights | this.whitePawns | this.whiteQueens |
				this.whiteRooks;
		this.blackPieces = this.blackBishops | this.blackKings |
				this.blackKnights | this.blackPawns | this.blackQueens |
				this.blackRooks;
		this.allPieces = this.whitePieces | this.blackPieces;
		
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
		
		// TODO: Implement the halfmove clock and possibly fullmove number.
	}

};
