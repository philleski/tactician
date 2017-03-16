package chess_engine;

import java.util.ArrayList;

public class AlgebraicNotation {
	public static Move algebraicToMove(Board board, String algebraic) {
		if(algebraic.length() < 2) {
			System.err.println("Illegal move: too short: " + algebraic);
			return null;
		}
		for(Move m : board.legalMoves()) {
			long sourceMask = 1L << m.source;
			String sourceSquare = new Square(m.source).getName();
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
				long algebraicDestIndex = new Square(algebraicDest).getIndex();
				if(algebraicDestIndex != m.destination) {
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
		return null;
	}
	
	private static String algebraicAmbiguityForPiece(ArrayList<Move> legalMoves, long pieceFamily,
			int source, int dest) {
		ArrayList<String> piecesToDest = new ArrayList<String>();
		String sourceSquare = new Square(source).getName();
		for(Move m : legalMoves) {
			if(dest != m.destination) {
				continue;
			}
			long sourceMask = 1L << m.source;
			if((sourceMask & pieceFamily) != 0) {
				String sourceName = new Square(source).getName();
				piecesToDest.add(sourceName);
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
		
	public static String moveToAlgebraic(Board board, Move move) {
		String sourceSquare = new Square(move.source).getName();
		String destSquare = new Square(move.destination).getName();
		
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
				
		String bishopAmbiguity = algebraicAmbiguityForPiece(legalMoves,
			bitboardBishop.getData(), move.source, move.destination);
		String knightAmbiguity = algebraicAmbiguityForPiece(legalMoves,
			bitboardKnight.getData(), move.source, move.destination);
		String queenAmbiguity = algebraicAmbiguityForPiece(legalMoves,
			bitboardQueen.getData(), move.source, move.destination);
		String rookAmbiguity = algebraicAmbiguityForPiece(legalMoves,
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
