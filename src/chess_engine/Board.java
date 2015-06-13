package chess_engine;

import java.util.*;

import chess_engine.IllegalMoveException;

public class Board {
	public static int WHITE = 0;
	public static int BLACK = 1;
	
	public static int EMPTY = 2;
	public static int BISHOP = 3;
	public static int KNIGHT = 4;
	public static int KING = 5;
	public static int PAWN = 6;
	public static int QUEEN = 7;
	public static int ROOK = 8;
	
	public static int EP_YES = 9;
	public static int EP_NO = 10;
	
	public static int CASTLE_NONE = 11;
	public static int CASTLE_WHITE_QUEENSIDE = 12;
	public static int CASTLE_WHITE_KINGSIDE = 13;
	public static int CASTLE_BLACK_QUEENSIDE = 14;
	public static int CASTLE_BLACK_KINGSIDE = 15;
	
	public static int OVER_NOT = 16;
	public static int OVER_STALEMATE = 17;
	public static int OVER_CHECKMATE = 18;
	
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
		this.whiteKingMoved = other.whiteKingMoved;
		this.whiteRookAMoved = other.whiteRookAMoved;
		this.whiteRookHMoved = other.whiteRookHMoved;
		this.blackKingMoved = other.blackKingMoved;
		this.blackRookAMoved = other.blackRookAMoved;
		this.blackRookHMoved = other.blackRookHMoved;
	}
	
	public static String coordToSquare(long coord) {
		String file;
		String rank;
		int offset = 0;
		while((coord & 1L) == 0) {
			coord = coord >>> 1;
			offset++;
		}
		file = "" + ((char)(offset % 8 + 97));
		rank = Integer.toString((offset / 8) + 1);
		return file + rank;
	}
	
	public static long squareToCoord(String square) {
		int file = Integer.parseInt("" + (square.charAt(0) - 96));
		int rank = Integer.parseInt(square.substring(1, 2));
		int offset = (file - 1) + 8 * (rank - 1);
		return 1L << (long) offset;
	}
	
	public long[] algebraicToMove(String algebraic) throws IllegalMoveException {
		if(algebraic.length() < 2) {
			throw new IllegalMoveException("Illegal move: too short.");
		}
		for(long[] m : this.legalMoves()) {
			long source = m[0];
			long dest = m[1];
			int promoteTo = (int)m[2];
			int castle = (int)m[4]; 
			String sourceSquare = coordToSquare(source);
			if(algebraic.equals("O-O")) {
				if(castle == CASTLE_WHITE_KINGSIDE || castle == CASTLE_BLACK_KINGSIDE) {
					return m;
				}
			}
			else if(algebraic.equals("O-O-O")) {
				if(castle == CASTLE_WHITE_QUEENSIDE || castle == CASTLE_BLACK_QUEENSIDE) {
					return m;
				}
			}
			else {
				String algebraicDest = "";
				if(algebraic.charAt(algebraic.length() - 2) == '=') {
					algebraicDest = algebraic.substring(algebraic.length() - 4, algebraic.length() - 2);
				}
				else {
					algebraicDest = algebraic.substring(algebraic.length() - 2, algebraic.length());
				}
				if(algebraicDest.charAt(0) < 'a' || algebraicDest.charAt(0) > 'h') {
					throw new IllegalMoveException("Illegal move: destination file not from a-h: " + algebraicDest);
				}
				if(algebraicDest.charAt(1) < '1' || algebraicDest.charAt(1) > '8') {
					throw new IllegalMoveException("Illegal move: destination rank not from 1-8: " + algebraicDest);
				}
				if(squareToCoord(algebraicDest) != dest) {
					continue;
				}
				if(algebraic.charAt(0) >= 'a' && algebraic.charAt(0) <= 'h') {
					if((source & this.whitePawns) != 0 || (source & this.blackPawns) != 0) {
						if(algebraic.charAt(0) == sourceSquare.charAt(0)) {
							if(promoteTo == EMPTY) {
								return m;
							}
							else {
								char promoteChar = algebraic.charAt(algebraic.length() - 1);
								if(promoteChar == 'B' && promoteTo == BISHOP) {
									return m;
								}
								else if(promoteChar == 'N' && promoteTo == KNIGHT) {
									return m;
								}
								else if(promoteChar == 'Q' && promoteTo == QUEEN) {
									return m;
								}
								else if(promoteChar == 'R' && promoteTo == ROOK) {
									return m;
								}
							}
						}
					}
				}
				else if(algebraic.charAt(0) == 'K') {
					if((source & this.whiteKings) != 0 || (source & this.blackKings) != 0) {
						return m;
					}
				}
				else {
					String algebraicTrimmed = algebraic.replace("x", "");
					int algebraicTrimmedLength = algebraicTrimmed.length();
					if(algebraicTrimmedLength == 3) {
						// No algebraic ambiguity.
					} else if(algebraicTrimmedLength == 4) {
						if(algebraicTrimmed.charAt(1) >= 'a' && algebraicTrimmed.charAt(1) <= 'h') {
							// File algebraic ambiguity.
							if(sourceSquare.charAt(0) != algebraicTrimmed.charAt(1)) {
								continue;
							}
						}
						else {
							// Rank algebraic ambiguity.
							if(sourceSquare.charAt(1) != algebraicTrimmed.charAt(1)) {
								continue;
							}
						}
					} else if(algebraicTrimmedLength == 5) {
						// Double algebraic ambiguity.
						if(sourceSquare.charAt(0) != algebraicTrimmed.charAt(1)) {
							continue;
						}
						if(sourceSquare.charAt(1) != algebraicTrimmed.charAt(2)) {
							continue;
						}
					}
					if(algebraic.charAt(0) == 'B') {
						if((source & this.whiteBishops) != 0 || (source & this.blackBishops) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'N') {
						if((source & this.whiteKnights) != 0 || (source & this.blackKnights) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'Q') {
						if((source & this.whiteQueens) != 0 || (source & this.blackQueens) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'R') {
						if((source & this.whiteRooks) != 0 || (source & this.blackRooks) != 0) {
							return m;
						}
					}
				}
			}
		}
		return new long[]{};
	}
	
	private String algebraicAmbiguityForPiece(ArrayList<long[]> legalMoves, long pieceFamily, long source, long dest) {
		ArrayList<String> piecesToDest = new ArrayList<String>();
		String sourceSquare = coordToSquare(source);
		for(long[] m : legalMoves) {
			long mSource = m[0];
			long mDest = m[1];
			if(dest != mDest) {
				continue;
			}
			if((mSource & pieceFamily) != 0) {
				piecesToDest.add(coordToSquare(mSource));
			}
		}
		int sharedFiles = 0;
		int sharedRanks = 0;
		for(String otherSourceSquare : piecesToDest) {
			if(otherSourceSquare.charAt(0) == sourceSquare.charAt(0)) {
				sharedFiles++;
			}
			if(otherSourceSquare.charAt(1) == sourceSquare.charAt(1)) {
				sharedRanks++;
			}
		}
		if(piecesToDest.size() > 1) {
			if(sharedFiles == 1) {
				return "" + sourceSquare.charAt(0);
			}
			else if(sharedRanks == 1) {
				return "" + sourceSquare.charAt(1);
			}
			else {
				return sourceSquare;
			}
		}
		return "";
	}
	
	public String moveToLongAlgebraic(long[] move) {
		long source = move[0];
		long dest = move[1];
		int promoteTo = (int)move[2];
		String result = coordToSquare(source) + coordToSquare(dest);
		if(promoteTo == BISHOP) {
			result += "b";
		}
		else if(promoteTo == KNIGHT) {
			result += "n";
		}
		else if(promoteTo == QUEEN) {
			result += "q";
		}
		else if(promoteTo == ROOK) {
			result += "r";
		}
		return result;
	}
		
	public String moveToAlgebraic(long[] move) {
		long source = move[0];
		long dest = move[1];
		int promoteTo = (int)move[2];
		int enPassantCapture = (int)move[3];
		int castle = (int)move[4];

		String sourceSquare = coordToSquare(source);
		String destSquare = coordToSquare(dest);
		
		ArrayList<long[]> legalMoves = this.legalMoves();
		
		String bishopAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				this.whiteBishops | this.blackBishops, source, dest);
		String knightAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				this.whiteKnights | this.blackKnights, source, dest);
		String queenAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				this.whiteQueens | this.blackQueens, source, dest);
		String rookAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				this.whiteRooks | this.blackRooks, source, dest);

		boolean capturing = false;
		if(enPassantCapture == EP_YES || (dest & this.allPieces) != 0) {
			capturing = true;
		}
		String temp;
		if((source & this.whiteBishops) != 0 || (source & this.blackBishops) != 0) {
			if(capturing) {
				return "B" + bishopAmbiguity + "x" + destSquare;
			}
			else {
				return "B" + bishopAmbiguity + destSquare;
			}
		}
		else if((source & this.whiteKings) != 0 || (source & this.blackKings) != 0) {
			if(castle == CASTLE_WHITE_KINGSIDE || castle == CASTLE_BLACK_KINGSIDE) {
				return "O-O";
			}
			else if(castle == CASTLE_WHITE_QUEENSIDE || castle == CASTLE_BLACK_QUEENSIDE) {
				return "O-O-O";
			}
			else if(capturing) {
				return "Kx" + destSquare;
			}
			else {
				return "K" + destSquare;
			}
		}
		else if((source & this.whiteKnights) != 0 || (source & this.blackKnights) != 0) {
			if(capturing) {
				return "N" + knightAmbiguity + "x" + destSquare;
			}
			else {
				return "N" + knightAmbiguity + destSquare;
			}
		}
		else if((source & this.whitePawns) != 0 || (source & this.blackPawns) != 0) {
			if(capturing) {
				temp = sourceSquare.substring(0, 1) + "x" + destSquare;
			}
			else {
				temp = destSquare;
			}
			if(promoteTo == EMPTY) {
				return temp;
			}
			else if(promoteTo == BISHOP) {
				return temp + "=B";
			}
			else if(promoteTo == KNIGHT) {
				return temp + "=N";
			}
			else if(promoteTo == QUEEN) {
				return temp + "=Q";
			}
			else if(promoteTo == ROOK) {
				return temp + "=R";
			}
		}
		else if((source & this.whiteQueens) != 0 || (source & this.blackQueens) != 0) {
			if(capturing) {
				return "Q" + queenAmbiguity + "x" + destSquare;
			}
			else {
				return "Q" + queenAmbiguity + destSquare;
			}
		}
		else if((source & this.whiteRooks) != 0 || (source & this.blackRooks) != 0) {
			if(capturing) {
				return "R" + rookAmbiguity + "x" + destSquare;
			}
			else {
				return "R" + rookAmbiguity + destSquare;
			}
		}
		return "";
	}
	
	public String repr() {
		String result = "";
		String rowReversed = "";
		for(long i = 63; i >= 0; i--) {
			long mask = 1L << i;
			// The pawn representation is a little nonstandard because it's hard to tell the difference between
			// 'p' and 'P'.
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
		ArrayList<long[]> lm = this.legalMoves();
		for(long[] m : lm) {
			result += this.moveToAlgebraic(m) + ", ";
		}
		if(lm.size() > 0) {
			// Remove the last comma.
			result = result.substring(0, result.length() - 2);
		}
		result += "\n";
		if(this.isInCheck()) {
			result += "Check!\n";
		}
		if(this.turn == WHITE) {
			result += "Turn: white";
		}
		else {
			result += "Turn: black";
		}
		result += "\n\n";
		return result;
	}
	
	private void appendLegalMovesForPieceDiagonal(long coordIndex, long myPieces, long oppPieces,
			ArrayList<long[]> legalMoves) {
		long mask = 1L << coordIndex;
		// NW
		long nw = coordIndex;
		while(nw % 8 != 0 && nw + 7 < 64) {
			nw += 7;
			if(((1L << nw) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << nw, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << nw) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << nw, EMPTY, EP_NO, CASTLE_NONE});
		}
		// NE
		long ne = coordIndex;
		while(ne % 8 != 7 && ne + 9 < 64) {
			ne += 9;
			if(((1L << ne) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << ne, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << ne) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << ne, EMPTY, EP_NO, CASTLE_NONE});
		}
		// SW
		long sw = coordIndex;
		while(sw % 8 != 0 && sw - 9 >= 0) {
			sw -= 9;
			if(((1L << sw) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << sw, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << sw) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << sw, EMPTY, EP_NO, CASTLE_NONE});
		}
		// SE
		long se = coordIndex;
		while(se % 8 != 7 && se - 7 >= 0) {
			se -= 7;
			if(((1L << se) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << se, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << se) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << se, EMPTY, EP_NO, CASTLE_NONE});
		}
	}
	
	private void appendLegalMovesForPieceStraight(long coordIndex, long myPieces, long oppPieces,
			ArrayList<long[]> legalMoves) {
		long mask = 1L << coordIndex;
		// N
		long n = coordIndex;
		while(n + 8 < 64) {
			n += 8;
			if(((1L << n) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << n, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << n) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << n, EMPTY, EP_NO, CASTLE_NONE});
		}
		// W
		long w = coordIndex;
		while(w % 8 != 0) {
			w -= 1;
			if(((1L << w) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << w, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << w) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << w, EMPTY, EP_NO, CASTLE_NONE});
		}
		// E
		long e = coordIndex;
		while(e % 8 != 7) {
			e += 1;
			if(((1L << e) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << e, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << e) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << e, EMPTY, EP_NO, CASTLE_NONE});
		}
		// S
		long s = coordIndex;
		while(s - 8 >= 0) {
			s -= 8;
			if(((1L << s) & oppPieces) != 0) {
				legalMoves.add(new long[]{mask, 1L << s, EMPTY, EP_NO, CASTLE_NONE});
				break;
			}
			if(((1L << s) & myPieces) != 0) {
				break;
			}
			legalMoves.add(new long[]{mask, 1L << s, EMPTY, EP_NO, CASTLE_NONE});
		}
	}
	
	private void appendLegalMovesForKnight(long coordIndex, long myPieces, long oppPieces,
			ArrayList<long[]> legalMoves) {
		long mask = 1L << coordIndex;
		// NNW
		if(coordIndex % 8 != 0 && coordIndex < 48) {
			long next = coordIndex + 15;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// NNE
		if(coordIndex % 8 != 7 && coordIndex < 48) {
			long next = coordIndex + 17;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// NWW
		if(coordIndex % 8 > 1 && coordIndex < 56) {
			long next = coordIndex + 6;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// NEE
		if(coordIndex % 8 < 6 && coordIndex < 56) {
			long next = coordIndex + 10;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// SWW
		if(coordIndex % 8 > 1 && coordIndex >= 8) {
			long next = coordIndex - 10;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// SEE
		if(coordIndex % 8 < 6 && coordIndex >= 8) {
			long next = coordIndex - 6;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// SSW
		if(coordIndex % 8 != 0 && coordIndex >= 16) {
			long next = coordIndex - 17;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// SSE
		if(coordIndex % 8 != 7 && coordIndex >= 16) {
			long next = coordIndex - 15;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
	}
	
	private void appendLegalMovesForKing(long coordIndex, long myPieces, long oppPieces,
			ArrayList<long[]> legalMoves) {
		long mask = 1L << coordIndex;
		// NW
		if(coordIndex % 8 != 0 && coordIndex < 56) {
			long next = coordIndex + 7;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// N
		if(coordIndex < 56) {
			long next = coordIndex + 8;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// NE
		if(coordIndex % 8 != 7 && coordIndex < 56) {
			long next = coordIndex + 9;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// W
		if(coordIndex % 8 != 0) {
			long next = coordIndex - 1;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// E
		if(coordIndex % 8 != 7) {
			long next = coordIndex + 1;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// SW
		if(coordIndex % 8 != 0 && coordIndex >= 8) {
			long next = coordIndex - 9;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// S
		if(coordIndex >= 8) {
			long next = coordIndex - 8;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
		// SE
		if(coordIndex % 8 != 7 && coordIndex >= 8) {
			long next = coordIndex - 7;
			if(((1L << next) & myPieces) == 0) {
				legalMoves.add(new long[]{mask, 1L << next, EMPTY, EP_NO, CASTLE_NONE});
			}
		}
	}
	
	private boolean verifyCastleHelper(long maskStart, long maskEnd, long oppPieceType1,
			long oppPieceType2, int step) {
		// Search for enemy bishops/queens or rooks/queens. If there's another piece in the way, stop the
		// search. oppPieceType1, oppPieceType2 can be this.blackBishops, this.blackQueens or this.blackRooks,
		// this.blackQueens, for example.
		long mask = maskStart;
		while(true) {
			if((this.allPieces & mask) != 0) {
				if((oppPieceType1 & mask) != 0 || (oppPieceType2 & mask) != 0) {
					return false;
				}
				else {
					// There's a piece in the way so stop the search.
					return true;
				}
			}
			// We couldn't do a conventional while-loop because if we increment the mask above the bounds of a
			// 64-bit long it could get us in trouble.
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
	
	private boolean verifyCastleCheckRule(int castleType) {
		// Make sure the castle isn't happening out of a check or through a check. (If it's into a check the
		// opponent would take the king in the next move, so the AI wouldn't do it anyway.)
		// I'm using mask_helper.py to compute these magic-number masks (especially the pawn/knight masks).
		if(castleType == CASTLE_WHITE_KINGSIDE) {
			// d2, e2, f2, g2
			if((this.blackPawns & 0x0000000000007800L) != 0) {
				return false;
			}
			// c2, d3, f3, g2, d2, e3, g3, h2
			if((this.blackKnights & 0x000000000078cc00L) != 0) {
				return false;
			}
			// Left from e1. Magic numbers are d1, a1.
			if(!this.verifyCastleHelper(0x0000000000000008L, 0x0000000000000001L, this.blackRooks,
					this.blackQueens, -1)) {
				return false;
			}
			// Left diagonal from e1. Magic numbers are d2, a5.
			if(!this.verifyCastleHelper(0x0000000000000800L, 0x0000000100000000L, this.blackBishops,
					this.blackQueens, 7)) {
				return false;
			}
			// Up from e1. Magic numbers are e2, e8.
			if(!this.verifyCastleHelper(0x0000000000001000L, 0x1000000000000000L, this.blackRooks,
					this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from e1. Magic numbers are f2, h4.
			if(!this.verifyCastleHelper(0x0000000000002000L, 0x0000000080000000L, this.blackBishops,
					this.blackQueens, 9)) {
				return false;
			}
			// Left diagonal from f1. Magic numbers are e2, a6.
			if(!this.verifyCastleHelper(0x0000000000001000L, 0x0000010000000000L, this.blackBishops,
					this.blackQueens, 7)) {
				return false;
			}
			// Up from f1. Magic numbers are f2, f8.
			if(!this.verifyCastleHelper(0x0000000000000020L, 0x2000000000000000L, this.blackRooks,
					this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from f1. Magic numbers are g2, h3.
			if(!this.verifyCastleHelper(0x0000000000004000L, 0x0000000000800000L, this.blackBishops,
					this.blackQueens, 9)) {
				return false;
			}
			return true;
		}
		else if(castleType == CASTLE_WHITE_QUEENSIDE) {
			// c2, d2, e2, f2
			if((this.blackPawns & 0x0000000000003c00L) != 0) {
				return false;
			}
			// b2, c3, e3, f2, c2, d3, f3, g2
			if((this.blackKnights & 0x00000000003c6600L) != 0) {
				return false;
			}
			// Right from e1. Magic numbers are f1, h1.
			if(!this.verifyCastleHelper(0x0000000000000020L, 0x0000000000000080L, this.blackRooks,
					this.blackQueens, 1)) {
				return false;
			}
			// Left diagonal from e1. Magic numbers are d2, a5.
			if(!this.verifyCastleHelper(0x0000000000000800L, 0x0000000100000000L, this.blackBishops,
					this.blackQueens, 7)) {
				return false;
			}
			// Up from e1. Magic numbers are e2, e8.
			if(!this.verifyCastleHelper(0x0000000000001000L, 0x1000000000000000L, this.blackRooks,
					this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from e1. Magic numbers are f2, h4.
			if(!this.verifyCastleHelper(0x0000000000002000L, 0x0000000080000000L, this.blackBishops,
					this.blackQueens, 9)) {
				return false;
			}
			// Left diagonal from d1. Magic numbers are c2, a4.
			if(!this.verifyCastleHelper(0x0000000000000400L, 0x0000000001000000L, this.blackBishops,
					this.blackQueens, 7)) {
				return false;
			}
			// Up from d1. Magic numbers are d2, d8.
			if(!this.verifyCastleHelper(0x0000000000000800L, 0x0800000000000000L, this.blackRooks,
					this.blackQueens, 8)) {
				return false;
			}
			// Right diagonal from d1. Magic numbers are e2, h5.
			if(!this.verifyCastleHelper(0x0000000000001000L, 0x0000008000000000L, this.blackBishops,
					this.blackQueens, 9)) {
				return false;
			}
			return true;
		}
		else if(castleType == CASTLE_BLACK_KINGSIDE) {
			// d7, e7, f7, g7
			if((this.whitePawns & 0x0078000000000000L) != 0) {
				return false;
			}
			// c7, d6, f6, g7, d7, e6, g6, h7
			if((this.whiteKnights & 0x00cc780000000000L) != 0) {
				return false;
			}
			// Left from e8. Magic numbers are d8, a8.
			if(!this.verifyCastleHelper(0x0800000000000000L, 0x0100000000000000L, this.whiteRooks,
					this.whiteQueens, -1)) {
				return false;
			}
			// Left diagonal from e8. Magic numbers are d7, a4.
			if(!this.verifyCastleHelper(0x0008000000000000L, 0x0000000001000000L, this.whiteBishops,
					this.whiteQueens, -9)) {
				return false;
			}
			// Down from e8. Magic numbers are e7, e1.
			if(!this.verifyCastleHelper(0x0010000000000000L, 0x0000000000000010L, this.whiteRooks,
					this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from e8. Magic numbers are f7, h5.
			if(!this.verifyCastleHelper(0x0020000000000000L, 0x0000008000000000L, this.whiteBishops,
					this.whiteQueens, -7)) {
				return false;
			}
			// Left diagonal from f8. Magic numbers are e7, a3.
			if(!this.verifyCastleHelper(0x0010000000000000L, 0x0000000000010000L, this.whiteBishops,
					this.whiteQueens, -9)) {
				return false;
			}
			// Down from f8. Magic numbers are f7, f1.
			if(!this.verifyCastleHelper(0x0020000000000000L, 0x0000000000000020L, this.whiteRooks,
					this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from f8. Magic numbers are g7, h6.
			if(!this.verifyCastleHelper(0x0040000000000000L, 0x0000800000000000L, this.whiteBishops,
					this.whiteQueens, -7)) {
				return false;
			}
			return true;
		}
		else if(castleType == CASTLE_BLACK_QUEENSIDE) {
			// c7, d7, e7, f7
			if((this.whitePawns & 0x003c000000000000L) != 0) {
				return false;
			}
			// b7, c6, e6, f7, c7, d6, f6, g7
			if((this.whiteKnights & 0x00663c0000000000L) != 0) {
				return false;
			}
			// Right from e8. Magic numbers are f8, h8.
			if(!this.verifyCastleHelper(0x2000000000000000L, 0x8000000000000000L, this.whiteRooks,
					this.whiteQueens, 1)) {
				return false;
			}
			// Left diagonal from e8. Magic numbers are d7, a4.
			if(!this.verifyCastleHelper(0x0008000000000000L, 0x0000000001000000L, this.whiteBishops,
					this.whiteQueens, -9)) {
				return false;
			}
			// Down from e8. Magic numbers are e7, e1.
			if(!this.verifyCastleHelper(0x0010000000000000L, 0x0000000000000010L, this.whiteRooks,
					this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from e8. Magic numbers are f7, h5.
			if(!this.verifyCastleHelper(0x0020000000000000L, 0x0000008000000000L, this.whiteBishops,
					this.whiteQueens, -7)) {
				return false;
			}
			// Left diagonal from d8. Magic numbers are c7, a5.
			if(!this.verifyCastleHelper(0x0004000000000000L, 0x0000000100000000L, this.whiteBishops,
					this.whiteQueens, -9)) {
				return false;
			}
			// Down from d8. Magic numbers are d7, d1.
			if(!this.verifyCastleHelper(0x0008000000000000L, 0x0000000000000008L, this.whiteRooks,
					this.whiteQueens, -8)) {
				return false;
			}
			// Right diagonal from d8. Magic numbers are e7, h4.
			if(!this.verifyCastleHelper(0x0010000000000000L, 0x0000000080000000L, this.whiteBishops,
					this.whiteQueens, -7)) {
				return false;
			}
			return true;
		}
		return true;
	}
	
	public boolean isInCheck() {
		// Not the full list of legal moves (for example not pawns moving forward or castling), but a
		// superset of the ones that could capture the player's king. Also to speed things up not all of
		// them are actually legal, but the ones that aren't wouldn't be able to capture the player's
		// king anyway.
		ArrayList<long[]> oppLegalMoves = new ArrayList<long[]>();
		long myKings = this.getMyKings();
		long myPieces = this.getMyPieces();
		long oppPieces = this.getOppPieces();
		for(long i = 0; i < 64; i++) {
			long mask = 1L << i;
			if((oppPieces & mask) == 0) {
				continue;
			}
			if((this.getOppPawns() & mask) != 0) {
				if(this.turn == WHITE) {
					// Look at the black player's pawns.
					if(i % 8 != 0) {
						oppLegalMoves.add(new long[]{mask, mask >>> 9, EMPTY, EP_NO, CASTLE_NONE});
					}
					if(i % 8 != 7) {
						oppLegalMoves.add(new long[]{mask, mask >>> 7, EMPTY, EP_NO, CASTLE_NONE});
					}
				}
				else {
					// Look at the white player's pawns.
					if(i % 8 != 0) {
						oppLegalMoves.add(new long[]{mask, mask << 7, EMPTY, EP_NO, CASTLE_NONE});
					}
					if(i % 8 != 7) {
						oppLegalMoves.add(new long[]{mask, mask << 9, EMPTY, EP_NO, CASTLE_NONE});
					}
				}
			}
			else if((this.getOppBishops() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, oppPieces, myPieces, oppLegalMoves);
			}
			else if((this.getOppRooks() & mask) != 0) {
				this.appendLegalMovesForPieceStraight(i, oppPieces, myPieces, oppLegalMoves);
			}
			else if((this.getOppQueens() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, oppPieces, myPieces, oppLegalMoves);
				this.appendLegalMovesForPieceStraight(i, oppPieces, myPieces, oppLegalMoves);
			}
			else if((this.getOppKnights() & mask) != 0) {
				this.appendLegalMovesForKnight(i, oppPieces, myPieces, oppLegalMoves);
			}
			else if((this.getOppKings() & mask) != 0) {
				this.appendLegalMovesForKing(i, oppPieces, myPieces, oppLegalMoves);
			}
		}
		for(long[] m : oppLegalMoves) {
			long dest = m[1];
			if((dest & myKings) != 0) {
				return true;
			}
		}
		return false;
	}
	
	public int isOver() {
		if(this.legalMoves().size() > 0) {
			return OVER_NOT;
		}
		if(this.isInCheck()) {
			return OVER_CHECKMATE;
		}
		return OVER_STALEMATE;
	}
	
	public ArrayList<long[]> legalMovesFast() {
		// Calculate the legal moves without verifying that they don't put the player in check.
		// Each element is [source, destination, promoteTo, extraCapture]
		// extraCapture is for en passant, to list the extra square we're capturing (if 1 destroy the piece
		// below the destination)
		ArrayList<long[]> result = new ArrayList<long[]>();
		
		long myPieces = this.getMyPieces();
		long oppPieces = this.getOppPieces();
		
		for(long i = 0; i < 64; i++) {
			long mask = 1L << i;
			// Pawns
			if((myPieces & mask) == 0) {
				continue;
			}
			if((this.getMyPawns() & mask) != 0) {
				if(this.turn == WHITE) {
					// One space forward
					if(((mask << 8) & this.allPieces) == 0) {
						if(mask >>> 48 == 0) {
							result.add(new long[]{mask, mask << 8, EMPTY, EP_NO, CASTLE_NONE});
						}
						else {
							result.add(new long[]{mask, mask << 8, BISHOP, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 8, KNIGHT, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 8, QUEEN, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 8, ROOK, EP_NO, CASTLE_NONE});
						}
					}
					// Two spaces forward
					if(i < 16 && ((mask << 8) & this.allPieces) == 0 &&
						((mask << 16) & this.allPieces) == 0) {
						result.add(new long[]{mask, mask << 16, EMPTY, EP_NO, CASTLE_NONE});
					}
					// Capture left
					if(i % 8 != 0 && ((mask << 7) & oppPieces) != 0) {
						if(mask >>> 48 == 0) {
							result.add(new long[]{mask, mask << 7, EMPTY, EP_NO, CASTLE_NONE});
						}
						else {
							result.add(new long[]{mask, mask << 7, BISHOP, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 7, KNIGHT, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 7, QUEEN, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 7, ROOK, EP_NO, CASTLE_NONE});
						}
					}
					// Capture right
					if(i % 8 != 7 && ((mask << 9) & oppPieces) != 0) {
						if(mask >>> 48 == 0) {
							result.add(new long[]{mask, mask << 9, EMPTY, EP_NO, CASTLE_NONE});
						}
						else {
							result.add(new long[]{mask, mask << 9, BISHOP, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 9, KNIGHT, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 9, QUEEN, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask << 9, ROOK, EP_NO, CASTLE_NONE});
						}
					}
					// En passant
					if(this.enPassantTarget != 0) {
						if(i % 8 != 0 && mask >>> 1 == this.enPassantTarget) {
							result.add(new long[]{mask, mask << 7, EMPTY, EP_YES, CASTLE_NONE});
						}
						if(i % 8 != 7 && mask << 1 == this.enPassantTarget) {
							result.add(new long[]{mask, mask << 9, EMPTY, EP_YES, CASTLE_NONE});
						}
					}
				}
				else {
					// One space forward
					if(((mask >>> 8) & this.allPieces) == 0) {
						if(mask >>> 16 != 0) {
							result.add(new long[]{mask, mask >>> 8, EMPTY, EP_NO, CASTLE_NONE});
						}
						else {
							result.add(new long[]{mask, mask >>> 8, BISHOP, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 8, KNIGHT, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 8, QUEEN, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 8, ROOK, EP_NO, CASTLE_NONE});
						}
					}
					// Two spaces forward
					if(i >= 48 && ((mask >>> 8) & this.allPieces) == 0 &&
						((mask >>> 16) & this.allPieces) == 0) {
						result.add(new long[]{mask, mask >>> 16, EMPTY, EP_NO, CASTLE_NONE});
					}
					// Capture left
					if(i % 8 != 0 && ((mask >>> 9) & oppPieces) != 0) {
						if(mask >>> 16 != 0) {
							result.add(new long[]{mask, mask >>> 9, EMPTY, EP_NO, CASTLE_NONE});
						}
						else {
							result.add(new long[]{mask, mask >>> 9, BISHOP, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 9, KNIGHT, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 9, QUEEN, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 9, ROOK, EP_NO, CASTLE_NONE});
						}
					}
					// Capture right
					if(i % 8 != 7 && ((mask >>> 7) & oppPieces) != 0) {
						if(mask >>> 16 != 0) {
							result.add(new long[]{mask, mask >>> 7, EMPTY, EP_NO, CASTLE_NONE});
						}
						else {
							result.add(new long[]{mask, mask >>> 7, BISHOP, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 7, KNIGHT, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 7, QUEEN, EP_NO, CASTLE_NONE});
							result.add(new long[]{mask, mask >>> 7, ROOK, EP_NO, CASTLE_NONE});
						}
					}
					// En passant
					if(this.enPassantTarget != 0) {
						if(i % 8 != 0 && mask >>> 1 == this.enPassantTarget) {
							result.add(new long[]{mask, mask >>> 9, EMPTY, EP_YES, CASTLE_NONE});
						}
						if(i % 8 != 7 && mask << 1 == this.enPassantTarget) {
							result.add(new long[]{mask, mask >>> 7, EMPTY, EP_YES, CASTLE_NONE});
						}
					}
				}
			}
			else if((this.getMyBishops() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, myPieces, oppPieces, result);
			}
			else if((this.getMyRooks() & mask) != 0) {
				this.appendLegalMovesForPieceStraight(i, myPieces, oppPieces, result);
			}
			else if((this.getMyQueens() & mask) != 0) {
				this.appendLegalMovesForPieceDiagonal(i, myPieces, oppPieces, result);
				this.appendLegalMovesForPieceStraight(i, myPieces, oppPieces, result);
			}
			else if((this.getMyKnights() & mask) != 0) {
				this.appendLegalMovesForKnight(i, myPieces, oppPieces, result);
			}
			else if((this.getMyKings() & mask) != 0) {
				this.appendLegalMovesForKing(i, myPieces, oppPieces, result);
			}
		}
		
		// Castling
		if(this.turn == WHITE) {
			if(!this.whiteKingMoved && (this.whiteKings & 0x0000000000000010L) != 0) {
				if(!this.whiteRookHMoved && (this.whiteRooks & 0x0000000000000080L) != 0) {
					if((this.allPieces & 0x0000000000000060L) == 0) {
						if(this.verifyCastleCheckRule(CASTLE_WHITE_KINGSIDE)) {
							result.add(new long[]{0x0000000000000010L, 0x0000000000000040L, EMPTY, EP_NO,
								CASTLE_WHITE_KINGSIDE});
						}
					}
				}
				if(!this.whiteRookAMoved && (this.whiteRooks & 0x0000000000000001L) != 0) {
					if((this.allPieces & 0x000000000000000eL) == 0) {
						if(this.verifyCastleCheckRule(CASTLE_WHITE_QUEENSIDE)) {
							result.add(new long[]{0x0000000000000010L, 0x0000000000000004L, EMPTY, EP_NO,
								CASTLE_WHITE_QUEENSIDE});
						}
					}
				}
			}
		}
		else {
			if(!this.blackKingMoved && (this.blackKings & 0x1000000000000000L) != 0) {
				if(!this.blackRookHMoved && (this.blackRooks & 0x8000000000000000L) != 0) {
					if((this.allPieces & 0x6000000000000000L) == 0) {
						if(this.verifyCastleCheckRule(CASTLE_BLACK_KINGSIDE)) {
							result.add(new long[]{0x1000000000000000L, 0x4000000000000000L, EMPTY, EP_NO,
								CASTLE_BLACK_KINGSIDE});
						}
					}
				}
				if(!this.blackRookAMoved && (this.blackRooks & 0x0100000000000000L) != 0) {
					if((this.allPieces & 0x0e00000000000000L) == 0) {
						if(this.verifyCastleCheckRule(CASTLE_BLACK_QUEENSIDE)) {
							result.add(new long[]{0x1000000000000000L, 0x0400000000000000L, EMPTY, EP_NO,
								CASTLE_BLACK_QUEENSIDE});
						}
					}
				}
			}
		}
		
		return result;
	}
	
	public ArrayList<long[]> legalMoves() {
		ArrayList<long[]> legalMovesFast = this.legalMovesFast();
		ArrayList<long[]> result = new ArrayList<long[]>();
		for(long[] m : legalMovesFast) {
			Board copy = new Board(this);
			try {
				copy.move(m);
			}
			catch(IllegalMoveException e) {
				System.out.println("Illegal move: " + e);
				continue;
			}
			// Go back to the original player to see if they're in check.
			if(copy.turn == WHITE) {
				copy.turn = BLACK;
			}
			else {
				copy.turn = WHITE;
			}
			if(!copy.isInCheck()) {
				result.add(m);
			}
		}
		return result;
	}
	
	public void move(long[] legalMove) throws IllegalMoveException {
		long source = legalMove[0];
		long dest = legalMove[1];
		int promoteTo = (int)legalMove[2];
		int enPassantCapture = (int)legalMove[3];
		int castle = (int)legalMove[4];
		this.move(source, dest, promoteTo, enPassantCapture, castle);
	}
	
	public void move(long source, long dest, int promoteTo, int enPassantCapture, int castle)
			throws IllegalMoveException {
		// Remove whatever is in the destination spot.
		if((this.whiteBishops & dest) != 0) {
			this.whiteBishops &= ~(dest ^ 0);
		}
		else if((this.whiteKings & dest) != 0) {
			this.whiteKings &= ~(dest ^ 0);
		}
		else if((this.whiteKnights & dest) != 0) {
			this.whiteKnights &= ~(dest ^ 0);
		}
		else if((this.whitePawns & dest) != 0) {
			this.whitePawns &= ~(dest ^ 0);
		}
		else if((this.whiteQueens & dest) != 0) {
			this.whiteQueens &= ~(dest ^ 0);
		}
		else if((this.whiteRooks & dest) != 0) {
			this.whiteRooks &= ~(dest ^ 0);
		}
		else if((this.blackBishops & dest) != 0) {
			this.blackBishops &= ~(dest ^ 0);
		}
		else if((this.blackKings & dest) != 0) {
			this.blackKings &= ~(dest ^ 0);
		}
		else if((this.blackKnights & dest) != 0) {
			this.blackKnights &= ~(dest ^ 0);
		}
		else if((this.blackPawns & dest) != 0) {
			this.blackPawns &= ~(dest ^ 0);
		}
		else if((this.blackQueens & dest) != 0) {
			this.blackQueens &= ~(dest ^ 0);
		}
		else if((this.blackRooks & dest) != 0) {
			this.blackRooks &= ~(dest ^ 0);
		}
		
		if(enPassantCapture == EP_YES) {
			if(this.turn == WHITE) {
				this.blackPawns &= ~((dest >>> 8) ^ 0);
			}
			else {
				this.whitePawns &= ~((dest << 8) ^ 0);
			}
		}
		
		if(this.turn == WHITE) {
			if((this.whitePawns & source) != 0 && source << 16 == dest) {
				this.enPassantTarget = dest;
			} else {
				this.enPassantTarget = 0;
			}
			if((this.whiteBishops & source) != 0) {
				this.whiteBishops &= ~(source ^ 0);
				this.whiteBishops |= dest;
			} else if((this.whiteKings & source) != 0) {
				this.whiteKings &= ~(source ^ 0);
				this.whiteKings |= dest;
				this.whiteKingMoved = true;
			} else if((this.whiteKnights & source) != 0) {
				this.whiteKnights &= ~(source ^ 0);
				this.whiteKnights |= dest;
			} else if((this.whitePawns & source) != 0) {
				this.whitePawns &= ~(source ^ 0);
				if(dest >>> 56L == 0) {
					this.whitePawns |= dest;
				}
				else if(promoteTo == BISHOP) {
					this.whiteBishops |= dest;
				}
				else if(promoteTo == KNIGHT) {
					this.whiteKnights |= dest;
				}
				else if(promoteTo == QUEEN) {
					this.whiteQueens |= dest;
				}
				else if(promoteTo == ROOK) {
					this.whiteRooks |= dest;
				}
				else {
					throw new IllegalMoveException("Don't know what to promote to.");
				}
			} else if((this.whiteQueens & source) != 0) {
				this.whiteQueens &= ~(source ^ 0);
				this.whiteQueens |= dest;
			} else if((this.whiteRooks & source) != 0) {
				this.whiteRooks &= ~(source ^ 0);
				this.whiteRooks |= dest;
				if(source == 0x0000000000000001L) {
					this.whiteRookAMoved = true;
				}
				if(source == 0x0000000000000080L) {
					this.whiteRookHMoved = true;
				}
			}
			if(castle == CASTLE_WHITE_QUEENSIDE) {
				this.whiteRooks &= ~(0x0000000000000001L ^ 0);
				this.whiteRooks |= 0x0000000000000008L;
				this.whiteKingMoved = true;
				this.whiteRookAMoved = true;
			}
			if(castle == CASTLE_WHITE_KINGSIDE) {
				this.whiteRooks &= ~(0x0000000000000080L ^ 0);
				this.whiteRooks |= 0x0000000000000020L;
				this.whiteKingMoved = true;
				this.whiteRookHMoved = true;
			}
			this.turn = BLACK;
		}
		else {
			if((this.blackPawns & source) != 0 && source >>> 16 == dest) {
				this.enPassantTarget = dest;
			} else {
				this.enPassantTarget = 0;
			}
			if((this.blackBishops & source) != 0) {
				this.blackBishops &= ~(source ^ 0);
				this.blackBishops |= dest;
			} else if((this.blackKings & source) != 0) {
				this.blackKings &= ~(source ^ 0);
				this.blackKings |= dest;
				this.blackKingMoved = true;
			} else if((this.blackKnights & source) != 0) {
				this.blackKnights &= ~(source ^ 0);
				this.blackKnights |= dest;
			} else if((this.blackPawns & source) != 0) {
				this.blackPawns &= ~(source ^ 0);
				if(dest >>> 8L != 0) {
					this.blackPawns |= dest;
				}
				else if(promoteTo == BISHOP) {
					this.blackBishops |= dest;
				}
				else if(promoteTo == KNIGHT) {
					this.blackKnights |= dest;
				}
				else if(promoteTo == QUEEN) {
					this.blackQueens |= dest;
				}
				else if(promoteTo == ROOK) {
					this.blackRooks |= dest;
				}
				else {
					throw new IllegalMoveException("Don't know what to promote to.");
				}
			} else if((this.blackQueens & source) != 0) {
				this.blackQueens &= ~(source ^ 0);
				this.blackQueens |= dest;
			} else if((this.blackRooks & source) != 0) {
				this.blackRooks &= ~(source ^ 0);
				this.blackRooks |= dest;
				if(source == 0x0100000000000000L) {
					this.whiteRookAMoved = true;
				}
				if(source == 0x8000000000000000L) {
					this.whiteRookHMoved = true;
				}
			}
			if(castle == CASTLE_BLACK_QUEENSIDE) {
				this.blackRooks &= ~(0x0100000000000000L ^ 0);
				this.blackRooks |= 0x0800000000000000L;
				this.blackKingMoved = true;
				this.blackRookAMoved = true;
			}
			if(castle == CASTLE_BLACK_KINGSIDE) {
				this.blackRooks &= ~(0x8000000000000000L ^ 0);
				this.blackRooks |= 0x2000000000000000L;
				this.blackKingMoved = true;
				this.blackRookHMoved = true;
			}
			this.turn = WHITE;
		}
		this.whitePieces = this.whiteBishops | this.whiteKings | this.whiteKnights | this.whitePawns |
				this.whiteQueens | this.whiteRooks;
		this.blackPieces = this.blackBishops | this.blackKings | this.blackKnights | this.blackPawns |
				this.blackQueens | this.blackRooks;
		this.allPieces = this.whitePieces | this.blackPieces;
	}
	
	public void move(String algebraic) throws IllegalMoveException {
		long[] m = this.algebraicToMove(algebraic);
		if(m.length == 0) {
			throw new IllegalMoveException("Illegal move: Could not convert algebraic move.");
		}
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
					// If we happen to be on h8 it may cause an out-of-bounds error otherwise.
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
			this.turn = WHITE;
		}
		else {
			this.turn = BLACK;
		}
		
		String castling = parts[2];
		this.whiteKingMoved = true;
		this.whiteRookAMoved = true;
		this.whiteRookHMoved = true;
		this.blackKingMoved = true;
		this.blackRookAMoved = true;
		this.blackRookHMoved = true;
		if(castling.contains("K")) {
			this.whiteKingMoved = false;
			this.whiteRookHMoved = false;
		}
		if(castling.contains("Q")) {
			this.whiteKingMoved = false;
			this.whiteRookAMoved = false;
		}
		if(castling.contains("k")) {
			this.blackKingMoved = false;
			this.blackRookHMoved = false;
		}
		if(castling.contains("q")) {
			this.blackKingMoved = false;
			this.blackRookAMoved = false;
		}
		
		String enPassantTarget = parts[3];
		if(!enPassantTarget.equals("-")) {
			this.enPassantTarget = squareToCoord(enPassantTarget);
		}
		
		// TODO: Implement the halfmove clock and possibly fullmove number.
	}
	
	private long getMyBishops() {
		if(this.turn == WHITE) {
			return this.whiteBishops;
		}
		else {
			return this.blackBishops;
		}
	}
	private long getMyKings() {
		if(this.turn == WHITE) {
			return this.whiteKings;
		}
		else {
			return this.blackKings;
		}
	}
	private long getMyKnights() {
		if(this.turn == WHITE) {
			return this.whiteKnights;
		}
		else {
			return this.blackKnights;
		}
	}
	private long getMyPawns() {
		if(this.turn == WHITE) {
			return this.whitePawns;
		}
		else {
			return this.blackPawns;
		}
	}
	private long getMyQueens() {
		if(this.turn == WHITE) {
			return this.whiteQueens;
		}
		else {
			return this.blackQueens;
		}
	}
	private long getMyRooks() {
		if(this.turn == WHITE) {
			return this.whiteRooks;
		}
		else {
			return this.blackRooks;
		}
	}
	private long getMyPieces() {
		if(this.turn == WHITE) {
			return this.whitePieces;
		}
		else {
			return this.blackPieces;
		}
	}
	private long getOppBishops() {
		if(this.turn == WHITE) {
			return this.blackBishops;
		}
		else {
			return this.whiteBishops;
		}
	}
	private long getOppKings() {
		if(this.turn == WHITE) {
			return this.blackKings;
		}
		else {
			return this.whiteKings;
		}
	}
	private long getOppKnights() {
		if(this.turn == WHITE) {
			return this.blackKnights;
		}
		else {
			return this.whiteKnights;
		}
	}
	private long getOppPawns() {
		if(this.turn == WHITE) {
			return this.blackPawns;
		}
		else {
			return this.whitePawns;
		}
	}
	private long getOppQueens() {
		if(this.turn == WHITE) {
			return this.blackQueens;
		}
		else {
			return this.whiteQueens;
		}
	}
	private long getOppRooks() {
		if(this.turn == WHITE) {
			return this.blackRooks;
		}
		else {
			return this.whiteRooks;
		}
	}
	private long getOppPieces() {
		if(this.turn == WHITE) {
			return this.blackPieces;
		}
		else {
			return this.whitePieces;
		}
	}
	
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
	
	public long whitePieces = whiteBishops | whiteKings | whiteKnights | whitePawns | whiteQueens | whiteRooks;
	public long blackPieces = blackBishops | blackKings | blackKnights | blackPawns | blackQueens | blackRooks;
	public long allPieces = whitePieces | blackPieces;
	
	public int turn = WHITE;
	// If the last move was a double pawn move, this is the destination coordinate.
	public long enPassantTarget = 0;
	public boolean whiteKingMoved = false;
	public boolean whiteRookAMoved = false;
	public boolean whiteRookHMoved = false;
	public boolean blackKingMoved = false;
	public boolean blackRookAMoved = false;
	public boolean blackRookHMoved = false;
};
