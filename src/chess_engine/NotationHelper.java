package chess_engine;

import java.util.ArrayList;

public class NotationHelper {
	// https://en.wikipedia.org/wiki/Find_first_set
	public static byte coordToIndex(long coord) {
		byte leadingZeros = 0;
		if((coord & 0xffffffff00000000L) == 0) {
			leadingZeros += 32;
			coord <<= 32;
		}
		if((coord & 0xffff000000000000L) == 0) {
			leadingZeros += 16;
			coord <<= 16;
		}
		if((coord & 0xff00000000000000L) == 0) {
			leadingZeros += 8;
			coord <<= 8;
		}
		if((coord & 0xf000000000000000L) == 0) {
			leadingZeros += 4;
			coord <<= 4;
		}
		if((coord & 0xc000000000000000L) == 0) {
			leadingZeros += 2;
			coord <<= 2;
		}
		if((coord & 0x8000000000000000L) == 0) {
			leadingZeros++;
			coord <<= 1;
		}
		return (byte)(63 - leadingZeros);
	}
	
	public static String indexToSquare(int index) {
		String file;
		String rank;
		file = "" + ((char)(index % 8 + 97));
		rank = Integer.toString((index / 8) + 1);
		return file + rank;
	}
	
	public static String coordToSquare(long coord) {
		String file;
		String rank;
		int offset = 0;
		if(coord == 0) {
			return "a1";
		}
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
	
	public Move algebraicToMove(Board board, String algebraic)
			throws IllegalMoveException {
		if(algebraic.length() < 2) {
			throw new IllegalMoveException("Illegal move: too short.");
		}
		for(Move m : board.legalMoves()) {
			long sourceMask = 1L << m.source;
			String sourceSquare = coordToSquare(sourceMask);
			if(algebraic.equals("O-O")) {
				if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.KING).data) != 0 ||
						(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.KING).data) != 0) {
					if(m.source + 2 == m.destination) {
						return m;
					}
				}
			}
			else if(algebraic.equals("O-O-O")) {
				if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.KING).data) != 0 ||
						(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.KING).data) != 0) {
					if(m.source - 2 == m.destination) {
						return m;
					}
				}
			}
			else {
				String algebraicDest = "";
				if(algebraic.charAt(algebraic.length() - 2) == '=') {
					algebraicDest = algebraic.substring(algebraic.length() - 4,
							algebraic.length() - 2);
				}
				else {
					algebraicDest = algebraic.substring(algebraic.length() - 2,
							algebraic.length());
				}
				if(algebraicDest.charAt(0) < 'a' ||
						algebraicDest.charAt(0) > 'h') {
					throw new IllegalMoveException("Illegal move: destination "
							+ "file not from a-h: " + algebraicDest);
				}
				if(algebraicDest.charAt(1) < '1' ||
						algebraicDest.charAt(1) > '8') {
					throw new IllegalMoveException("Illegal move: destination "
							+ "rank not from 1-8: " + algebraicDest);
				}
				if(squareToCoord(algebraicDest) != m.destination) {
					continue;
				}
				if(algebraic.charAt(0) >= 'a' && algebraic.charAt(0) <= 'h') {
					if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.PAWN).data) != 0 ||
							(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.PAWN).data) != 0) {
						if(algebraic.charAt(0) == sourceSquare.charAt(0)) {
							if(m.promoteTo == Piece.NOPIECE) {
								return m;
							}
							char promoteChar = algebraic.charAt(
									algebraic.length() - 1);
							if(promoteChar == 'B' &&
									m.promoteTo == Piece.BISHOP) {
								return m;
							}
							else if(promoteChar == 'N' &&
									m.promoteTo == Piece.KNIGHT) {
								return m;
							}
							else if(promoteChar == 'Q' &&
									m.promoteTo == Piece.QUEEN) {
								return m;
							}
							else if(promoteChar == 'R' &&
									m.promoteTo == Piece.ROOK) {
								return m;
							}
						}
					}
				}
				else if(algebraic.charAt(0) == 'K') {
					if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.KING).data) != 0 ||
							(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.KING).data) != 0) {
						return m;
					}
				}
				else {
					String algebraicTrimmed = algebraic.replace("x", "");
					int algebraicTrimmedLength = algebraicTrimmed.length();
					if(algebraicTrimmedLength == 3) {
						// No algebraic ambiguity.
					} else if(algebraicTrimmedLength == 4) {
						if(algebraicTrimmed.charAt(1) >= 'a' &&
								algebraicTrimmed.charAt(1) <= 'h') {
							// File algebraic ambiguity.
							if(sourceSquare.charAt(0) !=
									algebraicTrimmed.charAt(1)) {
								continue;
							}
						}
						else {
							// Rank algebraic ambiguity.
							if(sourceSquare.charAt(1) !=
									algebraicTrimmed.charAt(1)) {
								continue;
							}
						}
					} else if(algebraicTrimmedLength == 5) {
						// Double algebraic ambiguity.
						if(sourceSquare.charAt(0) !=
								algebraicTrimmed.charAt(1)) {
							continue;
						}
						if(sourceSquare.charAt(1) !=
								algebraicTrimmed.charAt(2)) {
							continue;
						}
					}
					if(algebraic.charAt(0) == 'B') {
						if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.BISHOP).data) != 0 ||
								(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.BISHOP).data) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'N') {
						if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data) != 0 ||
								(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'Q') {
						if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.QUEEN).data) != 0 ||
								(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.QUEEN).data) != 0) {
							return m;
						}
					}
					else if(algebraic.charAt(0) == 'R') {
						if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.ROOK).data) != 0 ||
								(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.ROOK).data) != 0) {
							return m;
						}
					}
				}
			}
		}
		return new Move();
	}
	
	private String algebraicAmbiguityForPiece(ArrayList<Move> legalMoves,
			long pieceFamily, int source, int dest) {
		ArrayList<String> piecesToDest = new ArrayList<String>();
		String sourceSquare = indexToSquare(source);
		for(Move m : legalMoves) {
			if(dest != m.destination) {
				continue;
			}
			long sourceMask = 1L << m.source;
			if((sourceMask & pieceFamily) != 0) {
				piecesToDest.add(coordToSquare(sourceMask));
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
		String result = coordToSquare(1L << move.source) +
				coordToSquare(1L << move.destination);
		if(move.promoteTo == Piece.BISHOP) {
			result += "b";
		}
		else if(move.promoteTo == Piece.KNIGHT) {
			result += "n";
		}
		else if(move.promoteTo == Piece.QUEEN) {
			result += "q";
		}
		else if(move.promoteTo == Piece.ROOK) {
			result += "r";
		}
		return result;
	}
		
	public String moveToAlgebraic(Board board, Move move) {
		String sourceSquare = indexToSquare(move.source);
		String destSquare = indexToSquare(move.destination);
		
		long sourceMask = 1L << move.source;
		long destinationMask = 1L << move.destination;
				
		ArrayList<Move> legalMoves = board.legalMoves();
				
		String bishopAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.bitboards.get(Color.WHITE).get(Piece.BISHOP).data | board.bitboards.get(Color.BLACK).get(Piece.BISHOP).data, move.source,
				move.destination);
		String knightAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data | board.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data, move.source,
				move.destination);
		String queenAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.bitboards.get(Color.WHITE).get(Piece.QUEEN).data | board.bitboards.get(Color.BLACK).get(Piece.QUEEN).data, move.source,
				move.destination);
		String rookAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
				board.bitboards.get(Color.WHITE).get(Piece.ROOK).data | board.bitboards.get(Color.BLACK).get(Piece.ROOK).data, move.source,
				move.destination);
				
		boolean capturing = false;
		if((destinationMask & board.allPieces.data) != 0) {
			capturing = true;
		}
		else if(destinationMask == board.enPassantTarget) {
			if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.PAWN).data) != 0 ||
					(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.PAWN).data) != 0) {
				capturing = true;
			}
		}
				
		String temp;
		if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.BISHOP).data) != 0 ||
				(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.BISHOP).data) != 0) {
			if(capturing) {
				return "B" + bishopAmbiguity + "x" + destSquare;
			}
			else {
				return "B" + bishopAmbiguity + destSquare;
			}
		}
		else if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.KING).data) != 0 ||
				(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.KING).data) != 0) {
			if(move.source + 2 == move.destination) {
				return "O-O";
			}
			else if(move.source - 2 == move.destination) {
				return "O-O-O";
			}
			else if(capturing) {
				return "Kx" + destSquare;
			}
			else {
				return "K" + destSquare;
			}
		}
		else if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.KNIGHT).data) != 0 ||
				(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.KNIGHT).data) != 0) {
			if(capturing) {
				return "N" + knightAmbiguity + "x" + destSquare;
			}
			else {
				return "N" + knightAmbiguity + destSquare;
			}
		}
		else if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.PAWN).data) != 0 ||
				(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.PAWN).data) != 0) {
			if(capturing) {
				temp = sourceSquare.substring(0, 1) + "x" + destSquare;
			}
			else {
				temp = destSquare;
			}
			if(move.promoteTo == Piece.NOPIECE) {
				return temp;
			}
			else if(move.promoteTo == Piece.BISHOP) {
				return temp + "=B";
			}
			else if(move.promoteTo == Piece.KNIGHT) {
				return temp + "=N";
			}
			else if(move.promoteTo == Piece.QUEEN) {
				return temp + "=Q";
			}
			else if(move.promoteTo == Piece.ROOK) {
				return temp + "=R";
			}
		}
		else if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.QUEEN).data) != 0 ||
				(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.QUEEN).data) != 0) {
			if(capturing) {
				return "Q" + queenAmbiguity + "x" + destSquare;
			}
			else {
				return "Q" + queenAmbiguity + destSquare;
			}
		}
		else if((sourceMask & board.bitboards.get(Color.WHITE).get(Piece.ROOK).data) != 0 ||
				(sourceMask & board.bitboards.get(Color.BLACK).get(Piece.ROOK).data) != 0) {
			if(capturing) {
				return "R" + rookAmbiguity + "x" + destSquare;
			}
			else {
				return "R" + rookAmbiguity + destSquare;
			}
		}
		return "";
	}
	
	public long generateMask(String... squares) {
		long result = 0;
		for(String square : squares) {
			result |= squareToCoord(square);
		}
		return result;
	}
}
