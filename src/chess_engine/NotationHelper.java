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
	
	public Move algebraicToMove(Board board, String algebraic) {
		if(algebraic.length() < 2) {
			System.err.println("Illegal move: too short: " + algebraic);
			return null;
		}
		for(Move m : board.legalMoves()) {
			long sourceMask = 1L << m.source;
			String sourceSquare = coordToSquare(sourceMask);
			if(algebraic.equals("O-O") && board.pieceOnSquare(sourceMask) == Piece.KING) {
				if(m.source + 2 == m.destination) {
					return m;
				}
			}
			else if(algebraic.equals("O-O-O") && board.pieceOnSquare(sourceMask) == Piece.KING) {
				if(m.source - 2 == m.destination) {
					return m;
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
				if(algebraicDest.charAt(0) < 'a' || algebraicDest.charAt(0) > 'h') {
					System.err.println("Illegal move: destination " +
						"file not from a-h: " + algebraicDest);
					return null;
				}
				if(algebraicDest.charAt(1) < '1' || algebraicDest.charAt(1) > '8') {
					System.err.println("Illegal move: destination " +
						"rank not from 1-8: " + algebraicDest);
					return null;
				}
				if(squareToCoord(algebraicDest) != m.destination) {
					continue;
				}
				if(algebraic.charAt(0) >= 'a' && algebraic.charAt(0) <= 'h') {
					if(board.pieceOnSquare(sourceMask) == Piece.PAWN) {
						if(algebraic.charAt(0) == sourceSquare.charAt(0)) {
							if(m.promoteTo == null) {
								return m;
							}
							char promoteChar = algebraic.charAt(algebraic.length() - 1);
							if(promoteChar == 'B' && m.promoteTo == Piece.BISHOP) {
								return m;
							}
							else if(promoteChar == 'N' && m.promoteTo == Piece.KNIGHT) {
								return m;
							}
							else if(promoteChar == 'Q' && m.promoteTo == Piece.QUEEN) {
								return m;
							}
							else if(promoteChar == 'R' && m.promoteTo == Piece.ROOK) {
								return m;
							}
						}
					}
				}
				else if(algebraic.charAt(0) == 'K' &&
						board.pieceOnSquare(sourceMask) == Piece.KING) {
					return m;
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
					if(algebraic.charAt(0) == 'B' &&
							board.pieceOnSquare(sourceMask) == Piece.BISHOP) {
						return m;
					}
					else if(algebraic.charAt(0) == 'N' &&
							board.pieceOnSquare(sourceMask) == Piece.KNIGHT) {
						return m;
					}
					else if(algebraic.charAt(0) == 'Q' &&
							board.pieceOnSquare(sourceMask) == Piece.QUEEN) {
						return m;
					}
					else if(algebraic.charAt(0) == 'R' &&
							board.pieceOnSquare(sourceMask) == Piece.ROOK) {
						return m;
					}
				}
			}
		}
		return new Move();
	}
	
	private String algebraicAmbiguityForPiece(ArrayList<Move> legalMoves, long pieceFamily,
			int source, int dest) {
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
		String result = coordToSquare(1L << move.source) + coordToSquare(1L << move.destination);
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
		
		Bitboard bitboardBishop =
			board.bitboards.get(Color.WHITE).get(Piece.BISHOP).intersection(
			board.bitboards.get(Color.BLACK).get(Piece.BISHOP));
		Bitboard bitboardKnight =
			board.bitboards.get(Color.WHITE).get(Piece.KNIGHT).intersection(
			board.bitboards.get(Color.BLACK).get(Piece.KNIGHT));
		Bitboard bitboardQueen =
			board.bitboards.get(Color.WHITE).get(Piece.QUEEN).intersection(
			board.bitboards.get(Color.BLACK).get(Piece.QUEEN));
		Bitboard bitboardRook =
			board.bitboards.get(Color.WHITE).get(Piece.ROOK).intersection(
			board.bitboards.get(Color.BLACK).get(Piece.ROOK));
				
		String bishopAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
			bitboardBishop.getData(), move.source, move.destination);
		String knightAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
			bitboardKnight.getData(), move.source, move.destination);
		String queenAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
			bitboardQueen.getData(), move.source, move.destination);
		String rookAmbiguity = this.algebraicAmbiguityForPiece(legalMoves,
			bitboardRook.getData(), move.source, move.destination);
				
		boolean capturing = false;
		if(board.allPieces.intersects(destinationMask)) {
			capturing = true;
		}
		else if(destinationMask == board.enPassantTarget) {
			if(board.pieceOnSquare(sourceMask) == Piece.PAWN) {
				capturing = true;
			}
		}
				
		String temp;
		if(board.pieceOnSquare(sourceMask) == Piece.BISHOP) {
			if(capturing) {
				return "B" + bishopAmbiguity + "x" + destSquare;
			}
			else {
				return "B" + bishopAmbiguity + destSquare;
			}
		}
		else if(board.pieceOnSquare(sourceMask) == Piece.KING) {
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
		else if(board.pieceOnSquare(sourceMask) == Piece.KNIGHT) {
			if(capturing) {
				return "N" + knightAmbiguity + "x" + destSquare;
			}
			else {
				return "N" + knightAmbiguity + destSquare;
			}
		}
		else if(board.pieceOnSquare(sourceMask) == Piece.PAWN) {
			if(capturing) {
				temp = sourceSquare.substring(0, 1) + "x" + destSquare;
			}
			else {
				temp = destSquare;
			}
			if(move.promoteTo == null) {
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
		else if(board.pieceOnSquare(sourceMask) == Piece.QUEEN) {
			if(capturing) {
				return "Q" + queenAmbiguity + "x" + destSquare;
			}
			else {
				return "Q" + queenAmbiguity + destSquare;
			}
		}
		else if(board.pieceOnSquare(sourceMask) == Piece.ROOK) {
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
