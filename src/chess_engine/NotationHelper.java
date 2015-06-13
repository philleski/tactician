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
	
	public Move algebraicToMove(Board board, String algebraic) throws IllegalMoveException {
		if(algebraic.length() < 2) {
			throw new IllegalMoveException("Illegal move: too short.");
		}
		for(Move m : board.legalMoves()) {
			String sourceSquare = coordToSquare(m.source);
			if(algebraic.equals("O-O")) {
				if(m.castle == Board.CASTLE_WHITE_KINGSIDE || m.castle == Board.CASTLE_BLACK_KINGSIDE) {
					return m;
				}
			}
			else if(algebraic.equals("O-O-O")) {
				if(m.castle == Board.CASTLE_WHITE_QUEENSIDE || m.castle == Board.CASTLE_BLACK_QUEENSIDE) {
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
				if(squareToCoord(algebraicDest) != m.destination) {
					continue;
				}
				if(algebraic.charAt(0) >= 'a' && algebraic.charAt(0) <= 'h') {
					if((m.source & board.whitePawns) != 0 || (m.source & board.blackPawns) != 0) {
						if(algebraic.charAt(0) == sourceSquare.charAt(0)) {
							if(m.promoteTo == Board.EMPTY) {
								return m;
							}
							else {
								char promoteChar = algebraic.charAt(algebraic.length() - 1);
								if(promoteChar == 'B' && m.promoteTo == Board.BISHOP) {
									return m;
								}
								else if(promoteChar == 'N' && m.promoteTo == Board.KNIGHT) {
									return m;
								}
								else if(promoteChar == 'Q' && m.promoteTo == Board.QUEEN) {
									return m;
								}
								else if(promoteChar == 'R' && m.promoteTo == Board.ROOK) {
									return m;
								}
							}
						}
					}
				}
				else if(algebraic.charAt(0) == 'K') {
					if((m.source & board.whiteKings) != 0 || (m.source & board.blackKings) != 0) {
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
						if((m.source & board.whiteBishops) != 0 || (m.source & board.blackBishops) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'N') {
						if((m.source & board.whiteKnights) != 0 || (m.source & board.blackKnights) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'Q') {
						if((m.source & board.whiteQueens) != 0 || (m.source & board.blackQueens) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'R') {
						if((m.source & board.whiteRooks) != 0 || (m.source & board.blackRooks) != 0) {
							return m;
						}
					}
				}
			}
		}
		return new Move();
	}
	
	private String algebraicAmbiguityForPiece(ArrayList<Move> legalMoves, long pieceFamily, long source, long dest) {
		ArrayList<String> piecesToDest = new ArrayList<String>();
		String sourceSquare = coordToSquare(source);
		for(Move m : legalMoves) {
			if(dest != m.destination) {
				continue;
			}
			if((m.source & pieceFamily) != 0) {
				piecesToDest.add(coordToSquare(m.source));
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
	
	public String moveToLongAlgebraic(Board board, Move move) {
		String result = coordToSquare(move.source) + coordToSquare(move.destination);
		if(move.promoteTo == Board.BISHOP) {
			result += "b";
		}
		else if(move.promoteTo == Board.KNIGHT) {
			result += "n";
		}
		else if(move.promoteTo == Board.QUEEN) {
			result += "q";
		}
		else if(move.promoteTo == Board.ROOK) {
			result += "r";
		}
		return result;
	}
		
	public String moveToAlgebraic(Board board, Move move) {
		String sourceSquare = coordToSquare(move.source);
		String destSquare = coordToSquare(move.destination);
		
		ArrayList<Move> legalMoves = board.legalMoves();
		
		String bishopAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteBishops | board.blackBishops, move.source, move.destination);
		String knightAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteKnights | board.blackKnights, move.source, move.destination);
		String queenAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteQueens | board.blackQueens, move.source, move.destination);
		String rookAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.whiteRooks | board.blackRooks, move.source, move.destination);

		boolean capturing = false;
		if(move.enPassantCapture == Board.EP_YES || (move.destination & board.allPieces) != 0) {
			capturing = true;
		}
		String temp;
		if((move.source & board.whiteBishops) != 0 || (move.source & board.blackBishops) != 0) {
			if(capturing) {
				return "B" + bishopAmbiguity + "x" + destSquare;
			}
			else {
				return "B" + bishopAmbiguity + destSquare;
			}
		}
		else if((move.source & board.whiteKings) != 0 || (move.source & board.blackKings) != 0) {
			if(move.castle == Board.CASTLE_WHITE_KINGSIDE || move.castle == Board.CASTLE_BLACK_KINGSIDE) {
				return "O-O";
			}
			else if(move.castle == Board.CASTLE_WHITE_QUEENSIDE || move.castle == Board.CASTLE_BLACK_QUEENSIDE) {
				return "O-O-O";
			}
			else if(capturing) {
				return "Kx" + destSquare;
			}
			else {
				return "K" + destSquare;
			}
		}
		else if((move.source & board.whiteKnights) != 0 || (move.source & board.blackKnights) != 0) {
			if(capturing) {
				return "N" + knightAmbiguity + "x" + destSquare;
			}
			else {
				return "N" + knightAmbiguity + destSquare;
			}
		}
		else if((move.source & board.whitePawns) != 0 || (move.source & board.blackPawns) != 0) {
			if(capturing) {
				temp = sourceSquare.substring(0, 1) + "x" + destSquare;
			}
			else {
				temp = destSquare;
			}
			if(move.promoteTo == Board.EMPTY) {
				return temp;
			}
			else if(move.promoteTo == Board.BISHOP) {
				return temp + "=B";
			}
			else if(move.promoteTo == Board.KNIGHT) {
				return temp + "=N";
			}
			else if(move.promoteTo == Board.QUEEN) {
				return temp + "=Q";
			}
			else if(move.promoteTo == Board.ROOK) {
				return temp + "=R";
			}
		}
		else if((move.source & board.whiteQueens) != 0 || (move.source & board.blackQueens) != 0) {
			if(capturing) {
				return "Q" + queenAmbiguity + "x" + destSquare;
			}
			else {
				return "Q" + queenAmbiguity + destSquare;
			}
		}
		else if((move.source & board.whiteRooks) != 0 || (move.source & board.blackRooks) != 0) {
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
