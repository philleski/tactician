package chess_engine;

import java.util.ArrayList;

public class LegalMoveGenerator {
	private static enum Castle {
		KINGSIDE_BLACK,
		KINGSIDE_WHITE,
		QUEENSIDE_BLACK,
		QUEENSIDE_WHITE
	}
	
	private void appendLegalMovesForPieceDiagonal(long coordIndex,
			long myPieces, long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		long mask = 1L << coordIndex;
		// NW
		long nw = coordIndex;
		while(nw % 8 != 0 && nw + 7 < 64) {
			nw += 7;
			if(((1L << nw) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << nw));
				break;
			}
			if(((1L << nw) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << nw));
		}
		// NE
		long ne = coordIndex;
		while(ne % 8 != 7 && ne + 9 < 64) {
			ne += 9;
			if(((1L << ne) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << ne));
				break;
			}
			if(((1L << ne) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << ne));
		}
		// SW
		long sw = coordIndex;
		while(sw % 8 != 0 && sw - 9 >= 0) {
			sw -= 9;
			if(((1L << sw) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << sw));
				break;
			}
			if(((1L << sw) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << sw));
		}
		// SE
		long se = coordIndex;
		while(se % 8 != 7 && se - 7 >= 0) {
			se -= 7;
			if(((1L << se) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << se));
				break;
			}
			if(((1L << se) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << se));
		}
	}
	
	private void appendLegalMovesForPieceStraight(long coordIndex,
			long myPieces, long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		long mask = 1L << coordIndex;
		// N
		long n = coordIndex;
		while(n + 8 < 64) {
			n += 8;
			if(((1L << n) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << n));
				break;
			}
			if(((1L << n) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << n));
		}
		// W
		long w = coordIndex;
		while(w % 8 != 0) {
			w -= 1;
			if(((1L << w) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << w));
				break;
			}
			if(((1L << w) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << w));
		}
		// E
		long e = coordIndex;
		while(e % 8 != 7) {
			e += 1;
			if(((1L << e) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << e));
				break;
			}
			if(((1L << e) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << e));
		}
		// S
		long s = coordIndex;
		while(s - 8 >= 0) {
			s -= 8;
			if(((1L << s) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(mask, 1L << s));
				break;
			}
			if(((1L << s) & myPieces) != 0) {
				break;
			}
			legalMovesNoncapture.add(new Move(mask, 1L << s));
		}
	}
	
	private void appendLegalMovesForKnight(long coordIndex, long myPieces,
			long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		long mask = 1L << coordIndex;
		// NNW
		if(coordIndex % 8 != 0 && coordIndex < 48) {
			long next = coordIndex + 15;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// NNE
		if(coordIndex % 8 != 7 && coordIndex < 48) {
			long next = coordIndex + 17;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// NWW
		if(coordIndex % 8 > 1 && coordIndex < 56) {
			long next = coordIndex + 6;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// NEE
		if(coordIndex % 8 < 6 && coordIndex < 56) {
			long next = coordIndex + 10;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// SWW
		if(coordIndex % 8 > 1 && coordIndex >= 8) {
			long next = coordIndex - 10;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// SEE
		if(coordIndex % 8 < 6 && coordIndex >= 8) {
			long next = coordIndex - 6;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// SSW
		if(coordIndex % 8 != 0 && coordIndex >= 16) {
			long next = coordIndex - 17;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// SSE
		if(coordIndex % 8 != 7 && coordIndex >= 16) {
			long next = coordIndex - 15;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
	}
	
	private void appendLegalMovesForKing(long coordIndex, long myPieces,
			long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		long mask = 1L << coordIndex;
		// NW
		if(coordIndex % 8 != 0 && coordIndex < 56) {
			long next = coordIndex + 7;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// N
		if(coordIndex < 56) {
			long next = coordIndex + 8;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// NE
		if(coordIndex % 8 != 7 && coordIndex < 56) {
			long next = coordIndex + 9;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// W
		if(coordIndex % 8 != 0) {
			long next = coordIndex - 1;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// E
		if(coordIndex % 8 != 7) {
			long next = coordIndex + 1;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// SW
		if(coordIndex % 8 != 0 && coordIndex >= 8) {
			long next = coordIndex - 9;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// S
		if(coordIndex >= 8) {
			long next = coordIndex - 8;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
		// SE
		if(coordIndex % 8 != 7 && coordIndex >= 8) {
			long next = coordIndex - 7;
			if(((1L << next) & myPieces) == 0) {
				if(((1L << next) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(mask, 1L << next));
				}
				else {
					legalMovesNoncapture.add(new Move(mask, 1L << next));
				}
			}
		}
	}
	
	private boolean verifyCastleHelper(Board board, long maskStart, long maskEnd,
			long oppPieceType1, long oppPieceType2, int step) {
		// Search for enemy bishops/queens or rooks/queens. If there's another
		// piece in the way, stop the search. oppPieceType1, oppPieceType2 can
		// be this.blackBishops, this.blackQueens or this.blackRooks,
		// this.blackQueens, for example.
		long mask = maskStart;
		while(true) {
			if((board.allPieces & mask) != 0) {
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
	
	private boolean verifyCastleCheckRule(Board board, Castle type) {
		// Make sure the castle isn't happening out of a check or through a
		// check. (If it's into a check the opponent would take the king in the
		// next move, so the AI wouldn't do it anyway.) I'm using
		// mask_helper.py to compute these magic-number masks (especially the
		// pawn/knight masks).
		if(type == Castle.KINGSIDE_WHITE) {
			// d2, e2, f2, g2
			if((board.blackPawns & 0x0000000000007800L) != 0) {
				return false;
			}
			// c2, d3, f3, g2, d2, e3, g3, h2
			if((board.blackKnights & 0x000000000078cc00L) != 0) {
				return false;
			}
			// Left from e1. Magic numbers are d1, a1.
			if(!this.verifyCastleHelper(board,
					0x0000000000000008L, 0x0000000000000001L,
					board.blackRooks, board.blackQueens, -1)) {
				return false;
			}
			// Left diagonal from e1. Magic numbers are d2, a5.
			if(!this.verifyCastleHelper(board,
					0x0000000000000800L, 0x0000000100000000L,
					board.blackBishops, board.blackQueens, 7)) {
				return false;
			}
			// Up from e1. Magic numbers are e2, e8.
			if(!this.verifyCastleHelper(board,
					0x0000000000001000L, 0x1000000000000000L,
					board.blackRooks, board.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from e1. Magic numbers are f2, h4.
			if(!this.verifyCastleHelper(board,
					0x0000000000002000L, 0x0000000080000000L,
					board.blackBishops, board.blackQueens, 9)) {
				return false;
			}
			// Left diagonal from f1. Magic numbers are e2, a6.
			if(!this.verifyCastleHelper(board,
					0x0000000000001000L, 0x0000010000000000L,
					board.blackBishops, board.blackQueens, 7)) {
				return false;
			}
			// Up from f1. Magic numbers are f2, f8.
			if(!this.verifyCastleHelper(board,
					0x0000000000000020L, 0x2000000000000000L,
					board.blackRooks, board.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from f1. Magic numbers are g2, h3.
			if(!this.verifyCastleHelper(board,
					0x0000000000004000L, 0x0000000000800000L,
					board.blackBishops, board.blackQueens, 9)) {
				return false;
			}
			return true;
		}
		else if(type == Castle.QUEENSIDE_WHITE) {
			// c2, d2, e2, f2
			if((board.blackPawns & 0x0000000000003c00L) != 0) {
				return false;
			}
			// b2, c3, e3, f2, c2, d3, f3, g2
			if((board.blackKnights & 0x00000000003c6600L) != 0) {
				return false;
			}
			// Right from e1. Magic numbers are f1, h1.
			if(!this.verifyCastleHelper(board,
					0x0000000000000020L, 0x0000000000000080L,
					board.blackRooks, board.blackQueens, 1)) {
				return false;
			}
			// Left diagonal from e1. Magic numbers are d2, a5.
			if(!this.verifyCastleHelper(board,
					0x0000000000000800L, 0x0000000100000000L,
					board.blackBishops, board.blackQueens, 7)) {
				return false;
			}
			// Up from e1. Magic numbers are e2, e8.
			if(!this.verifyCastleHelper(board,
					0x0000000000001000L, 0x1000000000000000L,
					board.blackRooks, board.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from e1. Magic numbers are f2, h4.
			if(!this.verifyCastleHelper(board,
					0x0000000000002000L, 0x0000000080000000L,
					board.blackBishops, board.blackQueens, 9)) {
				return false;
			}
			// Left diagonal from d1. Magic numbers are c2, a4.
			if(!this.verifyCastleHelper(board,
					0x0000000000000400L, 0x0000000001000000L,
					board.blackBishops, board.blackQueens, 7)) {
				return false;
			}
			// Up from d1. Magic numbers are d2, d8.
			if(!this.verifyCastleHelper(board,
					0x0000000000000800L, 0x0800000000000000L,
					board.blackRooks, board.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from d1. Magic numbers are e2, h5.
			if(!this.verifyCastleHelper(board,
					0x0000000000001000L, 0x0000008000000000L,
					board.blackBishops, board.blackQueens, 9)) {
				return false;
			}
			return true;
		}
		else if(type == Castle.KINGSIDE_BLACK) {
			// d7, e7, f7, g7
			if((board.whitePawns & 0x0078000000000000L) != 0) {
				return false;
			}
			// c7, d6, f6, g7, d7, e6, g6, h7
			if((board.whiteKnights & 0x00cc780000000000L) != 0) {
				return false;
			}
			// Left from e8. Magic numbers are d8, a8.
			if(!this.verifyCastleHelper(board,
					0x0800000000000000L, 0x0100000000000000L,
					board.whiteRooks, board.whiteQueens, -1)) {
				return false;
			}
			// Left diagonal from e8. Magic numbers are d7, a4.
			if(!this.verifyCastleHelper(board,
					0x0008000000000000L, 0x0000000001000000L,
					board.whiteBishops, board.whiteQueens, -9)) {
				return false;
			}
			// Down from e8. Magic numbers are e7, e1.
			if(!this.verifyCastleHelper(board,
					0x0010000000000000L, 0x0000000000000010L,
					board.whiteRooks, board.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from e8. Magic numbers are f7, h5.
			if(!this.verifyCastleHelper(board,
					0x0020000000000000L, 0x0000008000000000L,
					board.whiteBishops, board.whiteQueens, -7)) {
				return false;
			}
			// Left diagonal from f8. Magic numbers are e7, a3.
			if(!this.verifyCastleHelper(board,
					0x0010000000000000L, 0x0000000000010000L,
					board.whiteBishops, board.whiteQueens, -9)) {
				return false;
			}
			// Down from f8. Magic numbers are f7, f1.
			if(!this.verifyCastleHelper(board,
					0x0020000000000000L, 0x0000000000000020L,
					board.whiteRooks, board.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from f8. Magic numbers are g7, h6.
			if(!this.verifyCastleHelper(board,
					0x0040000000000000L, 0x0000800000000000L,
					board.whiteBishops, board.whiteQueens, -7)) {
				return false;
			}
			return true;
		}
		else if(type == Castle.QUEENSIDE_BLACK) {
			// c7, d7, e7, f7
			if((board.whitePawns & 0x003c000000000000L) != 0) {
				return false;
			}
			// b7, c6, e6, f7, c7, d6, f6, g7
			if((board.whiteKnights & 0x00663c0000000000L) != 0) {
				return false;
			}
			// Right from e8. Magic numbers are f8, h8.
			if(!this.verifyCastleHelper(board,
					0x2000000000000000L, 0x8000000000000000L,
					board.whiteRooks, board.whiteQueens, 1)) {
				return false;
			}
			// Left diagonal from e8. Magic numbers are d7, a4.
			if(!this.verifyCastleHelper(board,
					0x0008000000000000L, 0x0000000001000000L,
					board.whiteBishops, board.whiteQueens, -9)) {
				return false;
			}
			// Down from e8. Magic numbers are e7, e1.
			if(!this.verifyCastleHelper(board,
					0x0010000000000000L, 0x0000000000000010L,
					board.whiteRooks, board.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from e8. Magic numbers are f7, h5.
			if(!this.verifyCastleHelper(board,
					0x0020000000000000L, 0x0000008000000000L,
					board.whiteBishops, board.whiteQueens, -7)) {
				return false;
			}
			// Left diagonal from d8. Magic numbers are c7, a5.
			if(!this.verifyCastleHelper(board,
					0x0004000000000000L, 0x0000000100000000L,
					board.whiteBishops, board.whiteQueens, -9)) {
				return false;
			}
			// Down from d8. Magic numbers are d7, d1.
			if(!this.verifyCastleHelper(board,
					0x0008000000000000L, 0x0000000000000008L,
					board.whiteRooks, board.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from d8. Magic numbers are e7, h4.
			if(!this.verifyCastleHelper(board,
					0x0010000000000000L, 0x0000000080000000L,
					board.whiteBishops, board.whiteQueens, -7)) {
				return false;
			}
			return true;
		}
		return true;
	}
	

	public boolean isInCheck(Board board) {
		// Not the full list of legal moves (for example not pawns moving
		// forward or castling), but a superset of the ones that could capture
		// the player's king. Also to speed things up not all of them are
		// actually legal, but the ones that aren't wouldn't be able to capture
		// the player's king anyway.
		ArrayList<Move> oppLegalMovesCapture = new ArrayList<Move>();
		ArrayList<Move> oppLegalMovesNoncapture = new ArrayList<Move>();
		long myKings = this.getMyKings(board);
		long myPieces = this.getMyPieces(board);
		long oppPieces = this.getOppPieces(board);
		for(long i = 0; i < 64; i++) {
			long mask = 1L << i;
			if((oppPieces & mask) == 0) {
				continue;
			}
			if((this.getOppPawns(board) & mask) != 0) {
				if(board.turn == Color.WHITE) {
					// Look at the black player's pawns.
					if(i % 8 != 0) {
						oppLegalMovesCapture.add(new Move(mask, mask >>> 9));
					}
					if(i % 8 != 7) {
						oppLegalMovesCapture.add(new Move(mask, mask >>> 7));
					}
				}
				else {
					// Look at the white player's pawns.
					if(i % 8 != 0) {
						oppLegalMovesCapture.add(new Move(mask, mask << 7));
					}
					if(i % 8 != 7) {
						oppLegalMovesCapture.add(new Move(mask, mask << 9));
					}
				}
			}
			else if((this.getOppBishops(board) & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((this.getOppRooks(board) & mask) != 0) {
				this.appendLegalMovesForPieceStraight(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((this.getOppQueens(board) & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
				this.appendLegalMovesForPieceStraight(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((this.getOppKnights(board) & mask) != 0) {
				this.appendLegalMovesForKnight(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((this.getOppKings(board) & mask) != 0) {
				this.appendLegalMovesForKing(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
		}
		for(Move m : oppLegalMovesCapture) {
			if((m.destination & myKings) != 0) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Move> legalMovesFast(Board board, boolean capturesOnly) {
		// Calculate the legal moves without verifying that they don't put the
		// player in check. extraCapture is for en passant, to list the extra
		// square we're capturing (if 1 destroy the piece below the
		// destination)
		ArrayList<Move> legalMovesCapture = new ArrayList<Move>();
		ArrayList<Move> legalMovesNoncapture = new ArrayList<Move>();
		
		long myPieces = this.getMyPieces(board);
		long oppPieces = this.getOppPieces(board);
		
		for(long i = 0; i < 64; i++) {
			long mask = 1L << i;
			// Pawns
			if((myPieces & mask) == 0) {
				continue;
			}
			if((this.getMyPawns(board) & mask) != 0) {
				if(board.turn == Color.WHITE) {
					// One space forward
					if(((mask << 8) & board.allPieces) == 0) {
						if(mask >>> 48 == 0) {
							legalMovesNoncapture.add(new Move(mask, mask << 8));
						}
						else {
							legalMovesNoncapture.add(new Move(mask, mask << 8,
									Piece.BISHOP));
							legalMovesNoncapture.add(new Move(mask, mask << 8,
									Piece.KNIGHT));
							legalMovesNoncapture.add(new Move(mask, mask << 8,
									Piece.QUEEN));
							legalMovesNoncapture.add(new Move(mask, mask << 8,
									Piece.ROOK));
						}
					}
					// Two spaces forward
					if(i < 16 && ((mask << 8) & board.allPieces) == 0 &&
						((mask << 16) & board.allPieces) == 0) {
						legalMovesNoncapture.add(new Move(mask, mask << 16));
					}
					// Capture left
					if(i % 8 != 0 && ((mask << 7) & oppPieces) != 0) {
						if(mask >>> 48 == 0) {
							legalMovesCapture.add(new Move(mask, mask << 7));
						}
						else {
							legalMovesCapture.add(new Move(mask, mask << 7,
									Piece.BISHOP));
							legalMovesCapture.add(new Move(mask, mask << 7,
									Piece.KNIGHT));
							legalMovesCapture.add(new Move(mask, mask << 7,
									Piece.QUEEN));
							legalMovesCapture.add(new Move(mask, mask << 7,
									Piece.ROOK));
						}
					}
					// Capture right
					if(i % 8 != 7 && ((mask << 9) & oppPieces) != 0) {
						if(mask >>> 48 == 0) {
							legalMovesCapture.add(new Move(mask, mask << 9));
						}
						else {
							legalMovesCapture.add(new Move(mask, mask << 9,
									Piece.BISHOP));
							legalMovesCapture.add(new Move(mask, mask << 9,
									Piece.KNIGHT));
							legalMovesCapture.add(new Move(mask, mask << 9,
									Piece.QUEEN));
							legalMovesCapture.add(new Move(mask, mask << 9,
									Piece.ROOK));
						}
					}
					// En passant
					if(board.enPassantTarget != 0) {
						if(i % 8 != 0 && mask << 7 == board.enPassantTarget) {
							legalMovesCapture.add(new Move(mask, mask << 7));
						}
						if(i % 8 != 7 && mask << 9 == board.enPassantTarget) {
							legalMovesCapture.add(new Move(mask, mask << 9));
						}
					}
				}
				else {
					// One space forward
					if(((mask >>> 8) & board.allPieces) == 0) {
						if(mask >>> 16 != 0) {
							legalMovesNoncapture.add(new Move(mask, mask >>> 8));
						}
						else {
							legalMovesNoncapture.add(new Move(mask, mask >>> 8,
									Piece.BISHOP));
							legalMovesNoncapture.add(new Move(mask, mask >>> 8,
									Piece.KNIGHT));
							legalMovesNoncapture.add(new Move(mask, mask >>> 8,
									Piece.QUEEN));
							legalMovesNoncapture.add(new Move(mask, mask >>> 8,
									Piece.ROOK));
						}
					}
					// Two spaces forward
					if(i >= 48 && ((mask >>> 8) & board.allPieces) == 0 &&
						((mask >>> 16) & board.allPieces) == 0) {
						legalMovesNoncapture.add(new Move(mask, mask >>> 16));
					}
					// Capture left
					if(i % 8 != 0 && ((mask >>> 9) & oppPieces) != 0) {
						if(mask >>> 16 != 0) {
							legalMovesCapture.add(new Move(mask, mask >>> 9));
						}
						else {
							legalMovesCapture.add(new Move(mask, mask >>> 9,
									Piece.BISHOP));
							legalMovesCapture.add(new Move(mask, mask >>> 9,
									Piece.KNIGHT));
							legalMovesCapture.add(new Move(mask, mask >>> 9,
									Piece.QUEEN));
							legalMovesCapture.add(new Move(mask, mask >>> 9,
									Piece.ROOK));
						}
					}
					// Capture right
					if(i % 8 != 7 && ((mask >>> 7) & oppPieces) != 0) {
						if(mask >>> 16 != 0) {
							legalMovesCapture.add(new Move(mask, mask >>> 7));
						}
						else {
							legalMovesCapture.add(new Move(mask, mask >>> 7,
									Piece.BISHOP));
							legalMovesCapture.add(new Move(mask, mask >>> 7,
									Piece.KNIGHT));
							legalMovesCapture.add(new Move(mask, mask >>> 7,
									Piece.QUEEN));
							legalMovesCapture.add(new Move(mask, mask >>> 7,
									Piece.ROOK));
						}
					}
					// En passant
					if(board.enPassantTarget != 0) {
						if(i % 8 != 0 && mask >>> 9 == board.enPassantTarget) {
							legalMovesCapture.add(new Move(mask, mask >>> 9));
						}
						if(i % 8 != 7 && mask >>> 7 == board.enPassantTarget) {
							legalMovesCapture.add(new Move(mask, mask >>> 7));
						}
					}
				}
			}
			else if((this.getMyBishops(board) & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((this.getMyRooks(board) & mask) != 0) {
				this.appendLegalMovesForPieceStraight(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((this.getMyQueens(board) & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
				this.appendLegalMovesForPieceStraight(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((this.getMyKnights(board) & mask) != 0) {
				this.appendLegalMovesForKnight(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((this.getMyKings(board) & mask) != 0) {
				this.appendLegalMovesForKing(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
		}
		
		// Castling
		if(!capturesOnly) {
			if(board.turn == Color.WHITE) {
				if(board.whiteCastleRightKingside) {
					if((board.allPieces & 0x0000000000000060L) == 0) {
						if(this.verifyCastleCheckRule(board, Castle.KINGSIDE_WHITE)) {
							legalMovesNoncapture.add(new Move(0x0000000000000010L,
									0x0000000000000040L));
						}
					}
				}
				if(board.whiteCastleRightQueenside) {
					if((board.allPieces & 0x000000000000000eL) == 0) {
						if(this.verifyCastleCheckRule(board, Castle.QUEENSIDE_WHITE)) {
							legalMovesNoncapture.add(new Move(0x0000000000000010L,
									0x0000000000000004L));
						}
					}
				}
			}
			else {
				if(board.blackCastleRightKingside) {
					if((board.allPieces & 0x6000000000000000L) == 0) {
						if(this.verifyCastleCheckRule(board, Castle.KINGSIDE_BLACK)) {
							legalMovesNoncapture.add(new Move(0x1000000000000000L,
									0x4000000000000000L));
						}
					}
				}
				if(board.blackCastleRightQueenside) {
					if((board.allPieces & 0x0e00000000000000L) == 0) {
						if(this.verifyCastleCheckRule(board, Castle.QUEENSIDE_BLACK)) {
							legalMovesNoncapture.add(new Move(0x1000000000000000L,
									0x0400000000000000L));
						}
					}
				}
			}
		}
		
		if(capturesOnly) {
			return legalMovesCapture;
		}
		else {
			legalMovesCapture.addAll(legalMovesNoncapture);
			return legalMovesCapture;
		}
	}
	
	public ArrayList<Move> legalMoves(Board board) {
		ArrayList<Move> legalMovesFast = this.legalMovesFast(board, false);
		ArrayList<Move> result = new ArrayList<Move>();
		for(Move m : legalMovesFast) {
			Board copy = new Board(board);
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
	
	private long getMyBishops(Board board) {
		if(board.turn == Color.WHITE) {
			return board.whiteBishops;
		}
		else {
			return board.blackBishops;
		}
	}
	
	private long getMyKings(Board board) {
		if(board.turn == Color.WHITE) {
			return board.whiteKings;
		}
		else {
			return board.blackKings;
		}
	}
	
	private long getMyKnights(Board board) {
		if(board.turn == Color.WHITE) {
			return board.whiteKnights;
		}
		else {
			return board.blackKnights;
		}
	}
	
	private long getMyPawns(Board board) {
		if(board.turn == Color.WHITE) {
			return board.whitePawns;
		}
		else {
			return board.blackPawns;
		}
	}
	
	private long getMyQueens(Board board) {
		if(board.turn == Color.WHITE) {
			return board.whiteQueens;
		}
		else {
			return board.blackQueens;
		}
	}
	
	private long getMyRooks(Board board) {
		if(board.turn == Color.WHITE) {
			return board.whiteRooks;
		}
		else {
			return board.blackRooks;
		}
	}
	
	private long getMyPieces(Board board) {
		if(board.turn == Color.WHITE) {
			return board.whitePieces;
		}
		else {
			return board.blackPieces;
		}
	}
	
	private long getOppBishops(Board board) {
		if(board.turn == Color.WHITE) {
			return board.blackBishops;
		}
		else {
			return board.whiteBishops;
		}
	}
	
	private long getOppKings(Board board) {
		if(board.turn == Color.WHITE) {
			return board.blackKings;
		}
		else {
			return board.whiteKings;
		}
	}
	
	private long getOppKnights(Board board) {
		if(board.turn == Color.WHITE) {
			return board.blackKnights;
		}
		else {
			return board.whiteKnights;
		}
	}
	
	private long getOppPawns(Board board) {
		if(board.turn == Color.WHITE) {
			return board.blackPawns;
		}
		else {
			return board.whitePawns;
		}
	}
	
	private long getOppQueens(Board board) {
		if(board.turn == Color.WHITE) {
			return board.blackQueens;
		}
		else {
			return board.whiteQueens;
		}
	}
	
	private long getOppRooks(Board board) {
		if(board.turn == Color.WHITE) {
			return board.blackRooks;
		}
		else {
			return board.whiteRooks;
		}
	}
	
	private long getOppPieces(Board board) {
		if(board.turn == Color.WHITE) {
			return board.blackPieces;
		}
		else {
			return board.whitePieces;
		}
	}
}

