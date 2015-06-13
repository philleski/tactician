package chess_engine;

import java.util.ArrayList;

public class NotationHelper {
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
	
	public long[] algebraicToMove(Board board, String algebraic) throws IllegalMoveException {
		if(algebraic.length() < 2) {
			throw new IllegalMoveException("Illegal move: too short.");
		}
		for(long[] m : board.legalMoves()) {
			long source = m[0];
			long dest = m[1];
			int promoteTo = (int)m[2];
			int castle = (int)m[4]; 
			String sourceSquare = coordToSquare(source);
			if(algebraic.equals("O-O")) {
				if(castle == Board.CASTLE_WHITE_KINGSIDE || castle == Board.CASTLE_BLACK_KINGSIDE) {
					return m;
				}
			}
			else if(algebraic.equals("O-O-O")) {
				if(castle == Board.CASTLE_WHITE_QUEENSIDE || castle == Board.CASTLE_BLACK_QUEENSIDE) {
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
					if((source & board.whitePawns) != 0 || (source & board.blackPawns) != 0) {
						if(algebraic.charAt(0) == sourceSquare.charAt(0)) {
							if(promoteTo == Board.EMPTY) {
								return m;
							}
							else {
								char promoteChar = algebraic.charAt(algebraic.length() - 1);
								if(promoteChar == 'B' && promoteTo == Board.BISHOP) {
									return m;
								}
								else if(promoteChar == 'N' && promoteTo == Board.KNIGHT) {
									return m;
								}
								else if(promoteChar == 'Q' && promoteTo == Board.QUEEN) {
									return m;
								}
								else if(promoteChar == 'R' && promoteTo == Board.ROOK) {
									return m;
								}
							}
						}
					}
				}
				else if(algebraic.charAt(0) == 'K') {
					if((source & board.whiteKings) != 0 || (source & board.blackKings) != 0) {
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
						if((source & board.whiteBishops) != 0 || (source & board.blackBishops) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'N') {
						if((source & board.whiteKnights) != 0 || (source & board.blackKnights) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'Q') {
						if((source & board.whiteQueens) != 0 || (source & board.blackQueens) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'R') {
						if((source & board.whiteRooks) != 0 || (source & board.blackRooks) != 0) {
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
	
	public String moveToLongAlgebraic(Board board, long[] move) {
		long source = move[0];
		long dest = move[1];
		int promoteTo = (int)move[2];
		String result = coordToSquare(source) + coordToSquare(dest);
		if(promoteTo == Board.BISHOP) {
			result += "b";
		}
		else if(promoteTo == Board.KNIGHT) {
			result += "n";
		}
		else if(promoteTo == Board.QUEEN) {
			result += "q";
		}
		else if(promoteTo == Board.ROOK) {
			result += "r";
		}
		return result;
	}
		
	public String moveToAlgebraic(Board board, long[] move) {
		long source = move[0];
		long dest = move[1];
		int promoteTo = (int)move[2];
		int enPassantCapture = (int)move[3];
		int castle = (int)move[4];

		String sourceSquare = coordToSquare(source);
		String destSquare = coordToSquare(dest);
		
		ArrayList<long[]> legalMoves = board.legalMoves();
		
		String bishopAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteBishops | board.blackBishops, source, dest);
		String knightAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteKnights | board.blackKnights, source, dest);
		String queenAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteQueens | board.blackQueens, source, dest);
		String rookAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteRooks | board.blackRooks, source, dest);

		boolean capturing = false;
		if(enPassantCapture == Board.EP_YES || (dest & board.allPieces) != 0) {
			capturing = true;
		}
		String temp;
		if((source & board.whiteBishops) != 0 || (source & board.blackBishops) != 0) {
			if(capturing) {
				return "B" + bishopAmbiguity + "x" + destSquare;
			}
			else {
				return "B" + bishopAmbiguity + destSquare;
			}
		}
		else if((source & board.whiteKings) != 0 || (source & board.blackKings) != 0) {
			if(castle == Board.CASTLE_WHITE_KINGSIDE || castle == Board.CASTLE_BLACK_KINGSIDE) {
				return "O-O";
			}
			else if(castle == Board.CASTLE_WHITE_QUEENSIDE || castle == Board.CASTLE_BLACK_QUEENSIDE) {
				return "O-O-O";
			}
			else if(capturing) {
				return "Kx" + destSquare;
			}
			else {
				return "K" + destSquare;
			}
		}
		else if((source & board.whiteKnights) != 0 || (source & board.blackKnights) != 0) {
			if(capturing) {
				return "N" + knightAmbiguity + "x" + destSquare;
			}
			else {
				return "N" + knightAmbiguity + destSquare;
			}
		}
		else if((source & board.whitePawns) != 0 || (source & board.blackPawns) != 0) {
			if(capturing) {
				temp = sourceSquare.substring(0, 1) + "x" + destSquare;
			}
			else {
				temp = destSquare;
			}
			if(promoteTo == Board.EMPTY) {
				return temp;
			}
			else if(promoteTo == Board.BISHOP) {
				return temp + "=B";
			}
			else if(promoteTo == Board.KNIGHT) {
				return temp + "=N";
			}
			else if(promoteTo == Board.QUEEN) {
				return temp + "=Q";
			}
			else if(promoteTo == Board.ROOK) {
				return temp + "=R";
			}
		}
		else if((source & board.whiteQueens) != 0 || (source & board.blackQueens) != 0) {
			if(capturing) {
				return "Q" + queenAmbiguity + "x" + destSquare;
			}
			else {
				return "Q" + queenAmbiguity + destSquare;
			}
		}
		else if((source & board.whiteRooks) != 0 || (source & board.blackRooks) != 0) {
			if(capturing) {
				return "R" + rookAmbiguity + "x" + destSquare;
			}
			else {
				return "R" + rookAmbiguity + destSquare;
			}
		}
		return "";
	}
}
