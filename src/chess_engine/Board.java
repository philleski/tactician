package chess_engine;

import java.util.ArrayList;

public class Board {
	private static enum Castle {
		KINGSIDE_BLACK,
		KINGSIDE_WHITE,
		QUEENSIDE_BLACK,
		QUEENSIDE_WHITE
	}
	
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
		ArrayList<Move> lm = this.legalMoves();
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
	
	private void appendLegalMovesForPieceDiagonal(long coordIndex,
			long myPieces, long oppPieces, ArrayList<Move> legalMoves) {
		long mask = 1L << coordIndex;
		// NW
		long nw = coordIndex;
		while(nw % 8 != 0 && nw + 7 < 64) {
			nw += 7;
			if(((1L << nw) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << nw));
				break;
			}
			if(((1L << nw) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << nw));
		}
		// NE
		long ne = coordIndex;
		while(ne % 8 != 7 && ne + 9 < 64) {
			ne += 9;
			if(((1L << ne) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << ne));
				break;
			}
			if(((1L << ne) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << ne));
		}
		// SW
		long sw = coordIndex;
		while(sw % 8 != 0 && sw - 9 >= 0) {
			sw -= 9;
			if(((1L << sw) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << sw));
				break;
			}
			if(((1L << sw) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << sw));
		}
		// SE
		long se = coordIndex;
		while(se % 8 != 7 && se - 7 >= 0) {
			se -= 7;
			if(((1L << se) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << se));
				break;
			}
			if(((1L << se) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << se));
		}
	}
	
	private void appendLegalMovesForPieceStraight(long coordIndex,
			long myPieces, long oppPieces, ArrayList<Move> legalMoves) {
		long mask = 1L << coordIndex;
		// N
		long n = coordIndex;
		while(n + 8 < 64) {
			n += 8;
			if(((1L << n) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << n));
				break;
			}
			if(((1L << n) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << n));
		}
		// W
		long w = coordIndex;
		while(w % 8 != 0) {
			w -= 1;
			if(((1L << w) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << w));
				break;
			}
			if(((1L << w) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << w));
		}
		// E
		long e = coordIndex;
		while(e % 8 != 7) {
			e += 1;
			if(((1L << e) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << e));
				break;
			}
			if(((1L << e) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << e));
		}
		// S
		long s = coordIndex;
		while(s - 8 >= 0) {
			s -= 8;
			if(((1L << s) & oppPieces) != 0) {
				legalMoves.add(new Move(mask, 1L << s));
				break;
			}
			if(((1L << s) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new Move(mask, 1L << s));
		}
	}
	
	private void appendLegalMovesForKnight(long coordIndex, long myPieces,
			long oppPieces, ArrayList<Move> legalMoves) {
		long mask = 1L << coordIndex;
		// NNW
		if(coordIndex % 8 != 0 && coordIndex < 48) {
			long next = coordIndex + 15;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// NNE
		if(coordIndex % 8 != 7 && coordIndex < 48) {
			long next = coordIndex + 17;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// NWW
		if(coordIndex % 8 > 1 && coordIndex < 56) {
			long next = coordIndex + 6;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// NEE
		if(coordIndex % 8 < 6 && coordIndex < 56) {
			long next = coordIndex + 10;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// SWW
		if(coordIndex % 8 > 1 && coordIndex >= 8) {
			long next = coordIndex - 10;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// SEE
		if(coordIndex % 8 < 6 && coordIndex >= 8) {
			long next = coordIndex - 6;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// SSW
		if(coordIndex % 8 != 0 && coordIndex >= 16) {
			long next = coordIndex - 17;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// SSE
		if(coordIndex % 8 != 7 && coordIndex >= 16) {
			long next = coordIndex - 15;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
	}
	
	private void appendLegalMovesForKing(long coordIndex, long myPieces,
			long oppPieces, ArrayList<Move> legalMoves) {
		long mask = 1L << coordIndex;
		// NW
		if(coordIndex % 8 != 0 && coordIndex < 56) {
			long next = coordIndex + 7;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// N
		if(coordIndex < 56) {
			long next = coordIndex + 8;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// NE
		if(coordIndex % 8 != 7 && coordIndex < 56) {
			long next = coordIndex + 9;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// W
		if(coordIndex % 8 != 0) {
			long next = coordIndex - 1;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// E
		if(coordIndex % 8 != 7) {
			long next = coordIndex + 1;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// SW
		if(coordIndex % 8 != 0 && coordIndex >= 8) {
			long next = coordIndex - 9;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// S
		if(coordIndex >= 8) {
			long next = coordIndex - 8;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
		// SE
		if(coordIndex % 8 != 7 && coordIndex >= 8) {
			long next = coordIndex - 7;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new Move(mask, 1L << next));
			}
		}
	}
	
	private boolean verifyCastleHelper(long maskStart, long maskEnd,
			long oppPieceType1, long oppPieceType2, int step) {
		// Search for enemy bishops/queens or rooks/queens. If there's another
		// piece in the way, stop the search. oppPieceType1, oppPieceType2 can
		// be this.blackBishops, this.blackQueens or this.blackRooks,
		// this.blackQueens, for example.
		long mask = maskStart;
		while(true) {
			if((this.allPieces & mask) != 0) {
				if((oppPieceType1 & mask) != 0 ||
						(oppPieceType2 & mask) != 0) {
					return false;
				}
				else {
					// There's a piece in the way so stop the search.
					return true;
				}
			}
			// We couldn't do a conventional while-loop because if we increment
			// the mask above the bounds of a 64-bit long it could get us in
			// trouble.
			if(mask == maskEnd) {
				break;
			}
			if(step > 0) {
				mask <<= step;
			}
			else {
				mask >>>= (-step);
			}
		}
		return true;
	}
	
	private boolean verifyCastleCheckRule(Castle type) {
		// Make sure the castle isn't happening out of a check or through a
		// check. (If it's into a check the opponent would take the king in the
		// next move, so the AI wouldn't do it anyway.) I'm using
		// mask_helper.py to compute these magic-number masks (especially the
		// pawn/knight masks).
		if(type == Castle.KINGSIDE_WHITE) {
			// d2, e2, f2, g2
			if((this.blackPawns & 0x0000000000007800L) != 0) {
				return false;
			}
			// c2, d3, f3, g2, d2, e3, g3, h2
			if((this.blackKnights & 0x000000000078cc00L) != 0) {
				return false;
			}
			// Left from e1. Magic numbers are d1, a1.
			if(!this.verifyCastleHelper(
					0x0000000000000008L, 0x0000000000000001L,
					this.blackRooks, this.blackQueens, -1)) {
				return false;
			}
			// Left diagonal from e1. Magic numbers are d2, a5.
			if(!this.verifyCastleHelper(
					0x0000000000000800L, 0x0000000100000000L,
					this.blackBishops, this.blackQueens, 7)) {
				return false;
			}
			// Up from e1. Magic numbers are e2, e8.
			if(!this.verifyCastleHelper(
					0x0000000000001000L, 0x1000000000000000L,
					this.blackRooks, this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from e1. Magic numbers are f2, h4.
			if(!this.verifyCastleHelper(
					0x0000000000002000L, 0x0000000080000000L,
					this.blackBishops, this.blackQueens, 9)) {
				return false;
			}
			// Left diagonal from f1. Magic numbers are e2, a6.
			if(!this.verifyCastleHelper(
					0x0000000000001000L, 0x0000010000000000L,
					this.blackBishops, this.blackQueens, 7)) {
				return false;
			}
			// Up from f1. Magic numbers are f2, f8.
			if(!this.verifyCastleHelper(
					0x0000000000000020L, 0x2000000000000000L,
					this.blackRooks, this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from f1. Magic numbers are g2, h3.
			if(!this.verifyCastleHelper(
					0x0000000000004000L, 0x0000000000800000L,
					this.blackBishops, this.blackQueens, 9)) {
				return false;
			}
			return true;
		}
		else if(type == Castle.QUEENSIDE_WHITE) {
			// c2, d2, e2, f2
			if((this.blackPawns & 0x0000000000003c00L) != 0) {
				return false;
			}
			// b2, c3, e3, f2, c2, d3, f3, g2
			if((this.blackKnights & 0x00000000003c6600L) != 0) {
				return false;
			}
			// Right from e1. Magic numbers are f1, h1.
			if(!this.verifyCastleHelper(
					0x0000000000000020L, 0x0000000000000080L,
					this.blackRooks, this.blackQueens, 1)) {
				return false;
			}
			// Left diagonal from e1. Magic numbers are d2, a5.
			if(!this.verifyCastleHelper(
					0x0000000000000800L, 0x0000000100000000L,
					this.blackBishops, this.blackQueens, 7)) {
				return false;
			}
			// Up from e1. Magic numbers are e2, e8.
			if(!this.verifyCastleHelper(
					0x0000000000001000L, 0x1000000000000000L,
					this.blackRooks, this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from e1. Magic numbers are f2, h4.
			if(!this.verifyCastleHelper(
					0x0000000000002000L, 0x0000000080000000L,
					this.blackBishops, this.blackQueens, 9)) {
				return false;
			}
			// Left diagonal from d1. Magic numbers are c2, a4.
			if(!this.verifyCastleHelper(
					0x0000000000000400L, 0x0000000001000000L,
					this.blackBishops, this.blackQueens, 7)) {
				return false;
			}
			// Up from d1. Magic numbers are d2, d8.
			if(!this.verifyCastleHelper(
					0x0000000000000800L, 0x0800000000000000L,
					this.blackRooks, this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from d1. Magic numbers are e2, h5.
			if(!this.verifyCastleHelper(
					0x0000000000001000L, 0x0000008000000000L,
					this.blackBishops, this.blackQueens, 9)) {
				return false;
			}
			return true;
		}
		else if(type == Castle.KINGSIDE_BLACK) {
			// d7, e7, f7, g7
			if((this.whitePawns & 0x0078000000000000L) != 0) {
				return false;
			}
			// c7, d6, f6, g7, d7, e6, g6, h7
			if((this.whiteKnights & 0x00cc780000000000L) != 0) {
				return false;
			}
			// Left from e8. Magic numbers are d8, a8.
			if(!this.verifyCastleHelper(
					0x0800000000000000L, 0x0100000000000000L,
					this.whiteRooks, this.whiteQueens, -1)) {
				return false;
			}
			// Left diagonal from e8. Magic numbers are d7, a4.
			if(!this.verifyCastleHelper(
					0x0008000000000000L, 0x0000000001000000L,
					this.whiteBishops, this.whiteQueens, -9)) {
				return false;
			}
			// Down from e8. Magic numbers are e7, e1.
			if(!this.verifyCastleHelper(
					0x0010000000000000L, 0x0000000000000010L,
					this.whiteRooks, this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from e8. Magic numbers are f7, h5.
			if(!this.verifyCastleHelper(
					0x0020000000000000L, 0x0000008000000000L,
					this.whiteBishops, this.whiteQueens, -7)) {
				return false;
			}
			// Left diagonal from f8. Magic numbers are e7, a3.
			if(!this.verifyCastleHelper(
					0x0010000000000000L, 0x0000000000010000L,
					this.whiteBishops, this.whiteQueens, -9)) {
				return false;
			}
			// Down from f8. Magic numbers are f7, f1.
			if(!this.verifyCastleHelper(
					0x0020000000000000L, 0x0000000000000020L,
					this.whiteRooks, this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from f8. Magic numbers are g7, h6.
			if(!this.verifyCastleHelper(
					0x0040000000000000L, 0x0000800000000000L,
					this.whiteBishops, this.whiteQueens, -7)) {
				return false;
			}
			return true;
		}
		else if(type == Castle.QUEENSIDE_BLACK) {
			// c7, d7, e7, f7
			if((this.whitePawns & 0x003c000000000000L) != 0) {
				return false;
			}
			// b7, c6, e6, f7, c7, d6, f6, g7
			if((this.whiteKnights & 0x00663c0000000000L) != 0) {
				return false;
			}
			// Right from e8. Magic numbers are f8, h8.
			if(!this.verifyCastleHelper(
					0x2000000000000000L, 0x8000000000000000L,
					this.whiteRooks, this.whiteQueens, 1)) {
				return false;
			}
			// Left diagonal from e8. Magic numbers are d7, a4.
			if(!this.verifyCastleHelper(
					0x0008000000000000L, 0x0000000001000000L,
					this.whiteBishops, this.whiteQueens, -9)) {
				return false;
			}
			// Down from e8. Magic numbers are e7, e1.
			if(!this.verifyCastleHelper(
					0x0010000000000000L, 0x0000000000000010L,
					this.whiteRooks, this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from e8. Magic numbers are f7, h5.
			if(!this.verifyCastleHelper(
					0x0020000000000000L, 0x0000008000000000L,
					this.whiteBishops, this.whiteQueens, -7)) {
				return false;
			}
			// Left diagonal from d8. Magic numbers are c7, a5.
			if(!this.verifyCastleHelper(
					0x0004000000000000L, 0x0000000100000000L,
					this.whiteBishops, this.whiteQueens, -9)) {
				return false;
			}
			// Down from d8. Magic numbers are d7, d1.
			if(!this.verifyCastleHelper(
					0x0008000000000000L, 0x0000000000000008L,
					this.whiteRooks, this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from d8. Magic numbers are e7, h4.
			if(!this.verifyCastleHelper(
					0x0010000000000000L, 0x0000000080000000L,
					this.whiteBishops, this.whiteQueens, -7)) {
				return false;
			}
			return true;
		}
		return true;
	}
	
	public boolean isInCheck() {
		// Not the full list of legal moves (for example not pawns moving
		// forward or castling), but a superset of the ones that could capture
		// the player's king. Also to speed things up not all of them are
		// actually legal, but the ones that aren't wouldn't be able to capture
		// the player's king anyway.
		ArrayList<Move> oppLegalMoves = new ArrayList<Move>();
		long myKings = this.getMyKings();
		long myPieces = this.getMyPieces();
		long oppPieces = this.getOppPieces();
		for(long i = 0; i < 64; i++) {
			long mask = 1L << i;
			if((oppPieces & mask) == 0) {
				continue;
			}
			if((this.getOppPawns() & mask) != 0) {
				if(this.turn == Color.WHITE) {
					// Look at the black player's pawns.
					if(i % 8 != 0) {
						oppLegalMoves.add(new Move(mask, mask >>> 9));
					}
					if(i % 8 != 7) {
						oppLegalMoves.add(new Move(mask, mask >>> 7));
					}
				}
				else {
					// Look at the white player's pawns.
					if(i % 8 != 0) {
						oppLegalMoves.add(new Move(mask, mask << 7));
					}
					if(i % 8 != 7) {
						oppLegalMoves.add(new Move(mask, mask << 9));
					}
				}
			}
			else if((this.getOppBishops() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, oppPieces, myPieces,
						oppLegalMoves);
			}
			else if((this.getOppRooks() & mask) != 0) {
				this.appendLegalMovesForPieceStraight(i, oppPieces, myPieces,
						oppLegalMoves);
			}
			else if((this.getOppQueens() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, oppPieces, myPieces,
						oppLegalMoves);
				this.appendLegalMovesForPieceStraight(i, oppPieces, myPieces,
						oppLegalMoves);
			}
			else if((this.getOppKnights() & mask) != 0) {
				this.appendLegalMovesForKnight(i, oppPieces, myPieces,
						oppLegalMoves);
			}
			else if((this.getOppKings() & mask) != 0) {
				this.appendLegalMovesForKing(i, oppPieces, myPieces,
						oppLegalMoves);
			}
		}
		for(Move m : oppLegalMoves) {
			if((m.destination & myKings) != 0) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Move> legalMovesFast() {
		// Calculate the legal moves without verifying that they don't put the
		// player in check. extraCapture is for en passant, to list the extra
		// square we're capturing (if 1 destroy the piece below the
		// destination)
		ArrayList<Move> result = new ArrayList<Move>();
		
		long myPieces = this.getMyPieces();
		long oppPieces = this.getOppPieces();
		
		for(long i = 0; i < 64; i++) {
			long mask = 1L << i;
			// Pawns
			if((myPieces & mask) == 0) {
				continue;
			}
			if((this.getMyPawns() & mask) != 0) {
				if(this.turn == Color.WHITE) {
					// One space forward
					if(((mask << 8) & this.allPieces) == 0) {
						if(mask >>> 48 == 0) {
							result.add(new Move(mask, mask << 8));
						}
						else {
							result.add(new Move(mask, mask << 8,
									Piece.BISHOP));
							result.add(new Move(mask, mask << 8,
									Piece.KNIGHT));
							result.add(new Move(mask, mask << 8,
									Piece.QUEEN));
							result.add(new Move(mask, mask << 8,
									Piece.ROOK));
						}
					}
					// Two spaces forward
					if(i < 16 && ((mask << 8) & this.allPieces) == 0 &&
						((mask << 16) & this.allPieces) == 0) {
						result.add(new Move(mask, mask << 16));
					}
					// Capture left
					if(i % 8 != 0 && ((mask << 7) & oppPieces) != 0) {
						if(mask >>> 48 == 0) {
							result.add(new Move(mask, mask << 7));
						}
						else {
							result.add(new Move(mask, mask << 7,
									Piece.BISHOP));
							result.add(new Move(mask, mask << 7,
									Piece.KNIGHT));
							result.add(new Move(mask, mask << 7,
									Piece.QUEEN));
							result.add(new Move(mask, mask << 7,
									Piece.ROOK));
						}
					}
					// Capture right
					if(i % 8 != 7 && ((mask << 9) & oppPieces) != 0) {
						if(mask >>> 48 == 0) {
							result.add(new Move(mask, mask << 9));
						}
						else {
							result.add(new Move(mask, mask << 9,
									Piece.BISHOP));
							result.add(new Move(mask, mask << 9,
									Piece.KNIGHT));
							result.add(new Move(mask, mask << 9,
									Piece.QUEEN));
							result.add(new Move(mask, mask << 9,
									Piece.ROOK));
						}
					}
					// En passant
					if(this.enPassantTarget != 0) {
						if(i % 8 != 0 && mask << 7 == this.enPassantTarget) {
							result.add(new Move(mask, mask << 7));
						}
						if(i % 8 != 7 && mask << 9 == this.enPassantTarget) {
							result.add(new Move(mask, mask << 9));
						}
					}
				}
				else {
					// One space forward
					if(((mask >>> 8) & this.allPieces) == 0) {
						if(mask >>> 16 != 0) {
							result.add(new Move(mask, mask >>> 8));
						}
						else {
							result.add(new Move(mask, mask >>> 8,
									Piece.BISHOP));
							result.add(new Move(mask, mask >>> 8,
									Piece.KNIGHT));
							result.add(new Move(mask, mask >>> 8,
									Piece.QUEEN));
							result.add(new Move(mask, mask >>> 8,
									Piece.ROOK));
						}
					}
					// Two spaces forward
					if(i >= 48 && ((mask >>> 8) & this.allPieces) == 0 &&
						((mask >>> 16) & this.allPieces) == 0) {
						result.add(new Move(mask, mask >>> 16));
					}
					// Capture left
					if(i % 8 != 0 && ((mask >>> 9) & oppPieces) != 0) {
						if(mask >>> 16 != 0) {
							result.add(new Move(mask, mask >>> 9));
						}
						else {
							result.add(new Move(mask, mask >>> 9,
									Piece.BISHOP));
							result.add(new Move(mask, mask >>> 9,
									Piece.KNIGHT));
							result.add(new Move(mask, mask >>> 9,
									Piece.QUEEN));
							result.add(new Move(mask, mask >>> 9,
									Piece.ROOK));
						}
					}
					// Capture right
					if(i % 8 != 7 && ((mask >>> 7) & oppPieces) != 0) {
						if(mask >>> 16 != 0) {
							result.add(new Move(mask, mask >>> 7));
						}
						else {
							result.add(new Move(mask, mask >>> 7,
									Piece.BISHOP));
							result.add(new Move(mask, mask >>> 7,
									Piece.KNIGHT));
							result.add(new Move(mask, mask >>> 7,
									Piece.QUEEN));
							result.add(new Move(mask, mask >>> 7,
									Piece.ROOK));
						}
					}
					// En passant
					if(this.enPassantTarget != 0) {
						if(i % 8 != 0 && mask >>> 9 == this.enPassantTarget) {
							result.add(new Move(mask, mask >>> 9));
						}
						if(i % 8 != 7 && mask >>> 7 == this.enPassantTarget) {
							result.add(new Move(mask, mask >>> 7));
						}
					}
				}
			}
			else if((this.getMyBishops() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, myPieces, oppPieces,
						result);
			}
			else if((this.getMyRooks() & mask) != 0) {
				this.appendLegalMovesForPieceStraight(i, myPieces, oppPieces,
						result);
			}
			else if((this.getMyQueens() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, myPieces, oppPieces,
						result);
				this.appendLegalMovesForPieceStraight(i, myPieces, oppPieces,
						result);
			}
			else if((this.getMyKnights() & mask) != 0) {
				this.appendLegalMovesForKnight(i, myPieces, oppPieces, result);
			}
			else if((this.getMyKings() & mask) != 0) {
				this.appendLegalMovesForKing(i, myPieces, oppPieces, result);
			}
		}
		
		// Castling
		if(this.turn == Color.WHITE) {
			if(this.whiteCastleRightKingside) {
				if((this.allPieces & 0x0000000000000060L) == 0) {
					if(this.verifyCastleCheckRule(Castle.KINGSIDE_WHITE)) {
						result.add(new Move(0x0000000000000010L,
								0x0000000000000040L));
					}
				}
			}
			if(this.whiteCastleRightQueenside) {
				if((this.allPieces & 0x000000000000000eL) == 0) {
					if(this.verifyCastleCheckRule(Castle.QUEENSIDE_WHITE)) {
						result.add(new Move(0x0000000000000010L,
								0x0000000000000004L));
					}
				}
			}
		}
		else {
			if(this.blackCastleRightKingside) {
				if((this.allPieces & 0x6000000000000000L) == 0) {
					if(this.verifyCastleCheckRule(Castle.KINGSIDE_BLACK)) {
						result.add(new Move(0x1000000000000000L,
								0x4000000000000000L));
					}
				}
			}
			if(this.blackCastleRightQueenside) {
				if((this.allPieces & 0x0e00000000000000L) == 0) {
					if(this.verifyCastleCheckRule(Castle.QUEENSIDE_BLACK)) {
						result.add(new Move(0x1000000000000000L,
								0x0400000000000000L));
					}
				}
			}
		}
		
		return result;
	}
	
	public ArrayList<Move> legalMoves() {
		ArrayList<Move> legalMovesFast = this.legalMovesFast();
		ArrayList<Move> result = new ArrayList<Move>();
		for(Move m : legalMovesFast) {
			Board copy = new Board(this);
			try {
				copy.move(m);
			}
			catch(IllegalMoveException e) {
				System.out.println("Illegal move: " + e);
				continue;
			}
			// Go back to the original player to see if they're in check.
			copy.turn = Color.getOpposite(copy.turn);
			if(!copy.isInCheck()) {
				result.add(m);
			}
		}
		return result;
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
		if(!enPassantTarget.equals("-")) {
			this.enPassantTarget = NotationHelper.squareToCoord(
					enPassantTarget);
		}
		
		// TODO: Implement the halfmove clock and possibly fullmove number.
	}
	
	private long getMyBishops() {
		if(this.turn == Color.WHITE) {
			return this.whiteBishops;
		}
		else {
			return this.blackBishops;
		}
	}
	private long getMyKings() {
		if(this.turn == Color.WHITE) {
			return this.whiteKings;
		}
		else {
			return this.blackKings;
		}
	}
	private long getMyKnights() {
		if(this.turn == Color.WHITE) {
			return this.whiteKnights;
		}
		else {
			return this.blackKnights;
		}
	}
	private long getMyPawns() {
		if(this.turn == Color.WHITE) {
			return this.whitePawns;
		}
		else {
			return this.blackPawns;
		}
	}
	private long getMyQueens() {
		if(this.turn == Color.WHITE) {
			return this.whiteQueens;
		}
		else {
			return this.blackQueens;
		}
	}
	private long getMyRooks() {
		if(this.turn == Color.WHITE) {
			return this.whiteRooks;
		}
		else {
			return this.blackRooks;
		}
	}
	private long getMyPieces() {
		if(this.turn == Color.WHITE) {
			return this.whitePieces;
		}
		else {
			return this.blackPieces;
		}
	}
	private long getOppBishops() {
		if(this.turn == Color.WHITE) {
			return this.blackBishops;
		}
		else {
			return this.whiteBishops;
		}
	}
	private long getOppKings() {
		if(this.turn == Color.WHITE) {
			return this.blackKings;
		}
		else {
			return this.whiteKings;
		}
	}
	private long getOppKnights() {
		if(this.turn == Color.WHITE) {
			return this.blackKnights;
		}
		else {
			return this.whiteKnights;
		}
	}
	private long getOppPawns() {
		if(this.turn == Color.WHITE) {
			return this.blackPawns;
		}
		else {
			return this.whitePawns;
		}
	}
	private long getOppQueens() {
		if(this.turn == Color.WHITE) {
			return this.blackQueens;
		}
		else {
			return this.whiteQueens;
		}
	}
	private long getOppRooks() {
		if(this.turn == Color.WHITE) {
			return this.blackRooks;
		}
		else {
			return this.whiteRooks;
		}
	}
	private long getOppPieces() {
		if(this.turn == Color.WHITE) {
			return this.blackPieces;
		}
		else {
			return this.whitePieces;
		}
	}
};
