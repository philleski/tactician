package chess_engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LegalMoveGenerator {
	public LegalMoveGenerator() {
		for(Color color : Color.values()) {
			this.maskCastleSpace.put(color, new HashMap<Castle, Long>());
			this.maskCastlePawns.put(color, new HashMap<Castle, Long>());
			this.maskCastleKnights.put(color, new HashMap<Castle, Long>());
		}
		
		this.maskCastleSpace.get(Color.WHITE).put(Castle.KINGSIDE,
			notationHelper.generateMask("f1", "g1"));
		this.maskCastleSpace.get(Color.WHITE).put(Castle.QUEENSIDE,
			notationHelper.generateMask("b1", "c1", "d1"));
		this.maskCastleSpace.get(Color.BLACK).put(Castle.KINGSIDE,
			Bitboard.flip(this.maskCastleSpace.get(Color.WHITE).get(Castle.KINGSIDE)));
		this.maskCastleSpace.get(Color.BLACK).put(Castle.QUEENSIDE,
			Bitboard.flip(this.maskCastleSpace.get(Color.WHITE).get(Castle.QUEENSIDE)));
		
		this.maskCastlePawns.get(Color.WHITE).put(Castle.KINGSIDE,
			notationHelper.generateMask("d2", "e2", "f2", "g2"));
		this.maskCastlePawns.get(Color.WHITE).put(Castle.QUEENSIDE,
			notationHelper.generateMask("b2", "c2", "d2", "e2", "f2"));
		this.maskCastlePawns.get(Color.BLACK).put(Castle.KINGSIDE,
			Bitboard.flip(this.maskCastlePawns.get(Color.WHITE).get(Castle.KINGSIDE)));
		this.maskCastlePawns.get(Color.BLACK).put(Castle.QUEENSIDE,
			Bitboard.flip(this.maskCastlePawns.get(Color.WHITE).get(Castle.QUEENSIDE)));
			
		this.maskCastleKnights.get(Color.WHITE).put(Castle.KINGSIDE,
			notationHelper.generateMask("c2", "d2", "g2", "d3", "e3", "f3",
				"g3", "h2"));
		this.maskCastleKnights.get(Color.WHITE).put(Castle.QUEENSIDE,
			notationHelper.generateMask("b2", "c2", "f2", "g2", "c3", "d3",
				"e3", "f3"));
		this.maskCastleKnights.get(Color.BLACK).put(Castle.KINGSIDE,
			Bitboard.flip(this.maskCastleKnights.get(Color.WHITE).get(Castle.KINGSIDE)));
		this.maskCastleKnights.get(Color.BLACK).put(Castle.QUEENSIDE,
			Bitboard.flip(this.maskCastleKnights.get(Color.WHITE).get(Castle.QUEENSIDE)));
		
		this.addCastleRayStraight(Color.WHITE, Castle.KINGSIDE, "d1", "a1", -1);
		this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "d2", "a5", 7);
		this.addCastleRayStraight(Color.WHITE, Castle.KINGSIDE, "e2", "e8", 8);
		this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "f2", "h4", 9);
		this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "e2", "a6", 7);
		this.addCastleRayStraight(Color.WHITE, Castle.KINGSIDE, "f2", "f8", 8);
		this.addCastleRayDiagonal(Color.WHITE, Castle.KINGSIDE, "g2", "h3", 9);
		
		this.addCastleRayStraight(Color.WHITE, Castle.QUEENSIDE, "f1", "h1", 1);
		this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "d2", "a5", 7);
		this.addCastleRayStraight(Color.WHITE, Castle.QUEENSIDE, "e2", "e8", 8);
		this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "f2", "h4", 9);
		this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "c2", "a4", 7);
		this.addCastleRayStraight(Color.WHITE, Castle.QUEENSIDE, "d2", "d8", 8);
		this.addCastleRayDiagonal(Color.WHITE, Castle.QUEENSIDE, "e2", "h5", 9);
		
		this.castleRaysDiagonal.put(Color.BLACK, new HashMap<Castle, ArrayList<CastleRay>>());
		this.castleRaysStraight.put(Color.BLACK, new HashMap<Castle, ArrayList<CastleRay>>());
		for(Castle castle : Castle.values()) {
			ArrayList<CastleRay> castleRaysDiagonalBlack = new ArrayList<CastleRay>();
			for(CastleRay castleRay : this.castleRaysDiagonal.get(Color.WHITE).get(castle)) {
				castleRaysDiagonalBlack.add(castleRay.flip());
			}
			this.castleRaysDiagonal.get(Color.BLACK).put(castle, castleRaysDiagonalBlack);
			
			ArrayList<CastleRay> castleRaysStraightBlack = new ArrayList<CastleRay>();
			for(CastleRay castleRay : this.castleRaysStraight.get(Color.WHITE).get(castle)) {
				castleRaysStraightBlack.add(castleRay.flip());
			}
			this.castleRaysStraight.get(Color.BLACK).put(castle, castleRaysStraightBlack);
		}
		
		this.castleMoves.put(Color.WHITE, new HashMap<Castle, Move>());
		this.castleMoves.put(Color.BLACK, new HashMap<Castle, Move>());
		this.castleMoves.get(Color.WHITE).put(Castle.KINGSIDE,
				new Move((byte)4, (byte)6));
		this.castleMoves.get(Color.WHITE).put(Castle.QUEENSIDE,
				new Move((byte)4, (byte)2));
		this.castleMoves.get(Color.BLACK).put(Castle.KINGSIDE,
				new Move((byte)60, (byte)62));
		this.castleMoves.get(Color.BLACK).put(Castle.QUEENSIDE,
				new Move((byte)60, (byte)58));
	}
	
	public void addCastleRayDiagonal(Color color, Castle castle,
			String squareStart, String squareEnd, int stepSize) {
		if(!this.castleRaysDiagonal.containsKey(color)) {
			this.castleRaysDiagonal.put(color, new HashMap<Castle, ArrayList<CastleRay>>());
		}
		if(!this.castleRaysDiagonal.get(color).containsKey(castle)) {
			this.castleRaysDiagonal.get(color).put(castle, new ArrayList<CastleRay>());
		}
		this.castleRaysDiagonal.get(color).get(castle).add(
				new CastleRay(squareStart, squareEnd, stepSize));
	}
	
	public void addCastleRayStraight(Color color, Castle castle,
			String squareStart, String squareEnd, int stepSize) {
		if(!this.castleRaysStraight.containsKey(color)) {
			this.castleRaysStraight.put(color, new HashMap<Castle, ArrayList<CastleRay>>());
		}
		if(!this.castleRaysStraight.get(color).containsKey(castle)) {
			this.castleRaysStraight.get(color).put(castle, new ArrayList<CastleRay>());
		}
		this.castleRaysStraight.get(color).get(castle).add(
				new CastleRay(squareStart, squareEnd, stepSize));
	}
		
	private static boolean inBounds(int position, int stepSize) {
		if(position + stepSize < 0) {
			return false;
		}
		if(position + stepSize >= 64) {
			return false;
		}
		
		// If the step size applies to knights, the end file can be different
		// from the start file by up to two.
		int fileDiff = (position % 8) - ((position + stepSize) % 8);
		if(fileDiff < -2) {
			return false;
		}
		if(fileDiff > 2) {
			return false;
		}
		
		return true;
	}
	
	private void appendLegalMovesForPieceLongRange(byte start,
			long myPieces, long oppPieces, int[] stepSizes,
			ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		for(int i = 0; i < stepSizes.length; i++) {
			int position = start;
			while(inBounds(position, stepSizes[i])) {
				position += stepSizes[i];
				if(((1L << position) & oppPieces) != 0) {
					legalMovesCapture.add(new Move(start, (byte)position));
					break;
				}
				if(((1L << position) & myPieces) != 0) {
					break;
				}
				legalMovesNoncapture.add(new Move(start, (byte)position));
			}
		}
	}
	
	private void appendLegalMovesForPieceShortRange(byte start,
			long myPieces, long oppPieces, int[] stepSizes,
			ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		for(int i = 0; i < stepSizes.length; i++) {
			if(!inBounds(start, stepSizes[i])) {
				continue;
			}
			int position = start + stepSizes[i];
			if(((1L << position) & oppPieces) != 0) {
				legalMovesCapture.add(new Move(start, (byte)position));
				continue;
			}
			if(((1L << position) & myPieces) != 0) {
				continue;
			}
			legalMovesNoncapture.add(new Move(start, (byte)position));
		}
	}
	
	private void appendLegalMovesForPieceLongRangeDiagonal(byte start,
			long myPieces, long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		int[] stepSizes = {-9, -7, 7, 9};
		this.appendLegalMovesForPieceLongRange(start, myPieces, oppPieces,
			stepSizes, legalMovesCapture, legalMovesNoncapture);
	}
	
	private void appendLegalMovesForPieceLongRangeStraight(byte start,
			long myPieces, long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		int[] stepSizes = {-8, -1, 1, 8};
		this.appendLegalMovesForPieceLongRange(start, myPieces, oppPieces,
			stepSizes, legalMovesCapture, legalMovesNoncapture);
	}
	
	private void appendLegalMovesForKnight(byte start, long myPieces,
			long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		int[] stepSizes = {-17, -15, -10, -6, 6, 10, 15, 17};
		this.appendLegalMovesForPieceShortRange(start, myPieces, oppPieces,
			stepSizes, legalMovesCapture, legalMovesNoncapture);
	}
	
	private void appendLegalMovesForKing(byte start, long myPieces,
			long oppPieces, ArrayList<Move> legalMovesCapture,
			ArrayList<Move> legalMovesNoncapture) {
		int[] stepSizes = {-9, -8, -7, -1, 1, 7, 8, 9};
		this.appendLegalMovesForPieceShortRange(start, myPieces, oppPieces,
			stepSizes, legalMovesCapture, legalMovesNoncapture);
	}
	
	private boolean verifyCastleCheckRule(Board board, Castle castle) {
		// Make sure the castle isn't happening out of a check or through a
		// check. (If it's into a check the opponent would take the king in the
		// next move, so the AI wouldn't do it anyway.)
		Color turnFlipped = Color.flip(board.turn);
		long oppPiecesDiagonal = board.bitboards.get(turnFlipped).get(Piece.BISHOP).data &
				board.bitboards.get(turnFlipped).get(Piece.QUEEN).data;
		long oppPiecesStraight = board.bitboards.get(turnFlipped).get(Piece.ROOK).data &
				board.bitboards.get(turnFlipped).get(Piece.QUEEN).data;
		if((board.bitboards.get(turnFlipped).get(Piece.PAWN).data &
				this.maskCastlePawns.get(board.turn).get(castle)) != 0) {
			return false;
		}
		if((board.bitboards.get(turnFlipped).get(Piece.KNIGHT).data &
				this.maskCastleKnights.get(board.turn).get(castle)) != 0) {
			return false;
		}
		for(CastleRay castleRay : this.castleRaysDiagonal.get(board.turn).get(castle)) {
			if(castleRay.opponentPiecePrecludesCastling(board.turn,
					castle, oppPiecesDiagonal,
					board.playerBitboards.get(board.turn).data)) {
				return false;
			}
			
		}
		for(CastleRay castleRay : this.castleRaysStraight.get(board.turn).get(castle)) {
			if(castleRay.opponentPiecePrecludesCastling(board.turn,
					castle, oppPiecesStraight,
					board.playerBitboards.get(board.turn).data)) {
				return false;
			}
			
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
		long myKings = board.bitboards.get(board.turn).get(Piece.KING).data;
		Color turnFlipped = Color.flip(board.turn);
		long myPieces = board.playerBitboards.get(board.turn).data;
		long oppPieces = board.playerBitboards.get(turnFlipped).data;
		for(byte i = 0; i < 64; i++) {
			long mask = 1L << i;
			if((oppPieces & (1L << i)) == 0) {
				continue;
			}
			if((board.bitboards.get(turnFlipped).get(Piece.PAWN).data & mask) != 0) {
				if(board.turn == Color.WHITE) {
					// Look at the black player's pawns.
					if(i % 8 != 0) {
						oppLegalMovesCapture.add(new Move(i, (byte)(i - 9)));
					}
					if(i % 8 != 7) {
						oppLegalMovesCapture.add(new Move(i, (byte)(i - 7)));
					}
				}
				else {
					// Look at the white player's pawns.
					if(i % 8 != 0) {
						oppLegalMovesCapture.add(new Move(i, (byte)(i + 7)));
					}
					if(i % 8 != 7) {
						oppLegalMovesCapture.add(new Move(i, (byte)(i + 9)));
					}
				}
			}
			else if((board.bitboards.get(turnFlipped).get(Piece.BISHOP).data & mask) != 0) {
				this.appendLegalMovesForPieceLongRangeDiagonal(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((board.bitboards.get(turnFlipped).get(Piece.ROOK).data & mask) != 0) {
				this.appendLegalMovesForPieceLongRangeStraight(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((board.bitboards.get(turnFlipped).get(Piece.QUEEN).data & mask) != 0) {
				this.appendLegalMovesForPieceLongRangeDiagonal(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
				this.appendLegalMovesForPieceLongRangeStraight(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((board.bitboards.get(turnFlipped).get(Piece.KNIGHT).data & mask) != 0) {
				this.appendLegalMovesForKnight(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
			else if((board.bitboards.get(turnFlipped).get(Piece.KING).data & mask) != 0) {
				this.appendLegalMovesForKing(i, oppPieces, myPieces,
						oppLegalMovesCapture, oppLegalMovesNoncapture);
			}
		}
		for(Move m : oppLegalMovesCapture) {
			if(((1L << m.destination) & myKings) != 0) {
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
		Color turnFlipped = Color.flip(board.turn);
		long myPieces = board.playerBitboards.get(board.turn).data;
		long oppPieces = board.playerBitboards.get(turnFlipped).data;
		for(byte i = 0; i < 64; i++) {
			long mask = 1L << i;
			if((myPieces & mask) == 0) {
				continue;
			}
			if((board.bitboards.get(board.turn).get(Piece.PAWN).data & mask) != 0) {
				boolean isOnFileLeft = (i % 8 == 0);
				boolean isOnFileRight = (i % 8 == 7);
				boolean isOnHomeRank;
				boolean isPromotable;
				byte indexAdvancedOneRow;
				byte indexAdvancedOneRowLeft;
				byte indexAdvancedOneRowRight;
				byte indexAdvancedTwoRows;
				long maskAdvancedOneRow;
				long maskAdvancedOneRowLeft;
				long maskAdvancedOneRowRight;
				long maskAdvancedTwoRows;
				if(board.turn == Color.WHITE) {
					isOnHomeRank = (mask >>> 16 == 0);
					isPromotable = (mask >>> 48 != 0);
					indexAdvancedOneRow = (byte)(i + 8);
					indexAdvancedOneRowLeft = (byte)(i + 7);
					indexAdvancedOneRowRight = (byte)(i + 9);
					indexAdvancedTwoRows = (byte)(i + 16);
					maskAdvancedOneRow = mask << 8;
					maskAdvancedOneRowLeft = mask << 7;
					maskAdvancedOneRowRight = mask << 9;
					maskAdvancedTwoRows = mask << 16;
				} else {
					isOnHomeRank = (mask >>> 48 != 0);
					isPromotable = (mask >>> 16 == 0);
					indexAdvancedOneRow = (byte)(i - 8);
					indexAdvancedOneRowLeft = (byte)(i - 9);
					indexAdvancedOneRowRight = (byte)(i - 7);
					indexAdvancedTwoRows = (byte)(i - 16);
					maskAdvancedOneRow = mask >>> 8;
					maskAdvancedOneRowLeft = mask >>> 9;
					maskAdvancedOneRowRight = mask >>> 7;
					maskAdvancedTwoRows = mask >>> 16;
				}
				// One space forward
				if((maskAdvancedOneRow & board.allPieces.data) == 0) {
					if(!isPromotable) {
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRow));
					} else {
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRow, Piece.QUEEN));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRow, Piece.KNIGHT));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRow, Piece.ROOK));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRow, Piece.BISHOP));
					}
				}
				// Two spaces forward
				if(isOnHomeRank && (maskAdvancedOneRow & board.allPieces.data) == 0 &&
						(maskAdvancedTwoRows & board.allPieces.data) == 0) {
					legalMovesNoncapture.add(new Move(i, indexAdvancedTwoRows));
				}
				// Capture left
				if(!isOnFileLeft && (maskAdvancedOneRowLeft & oppPieces) != 0) {
					if(!isPromotable) {
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowLeft));
					} else {
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowLeft, Piece.QUEEN));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowLeft, Piece.KNIGHT));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowLeft, Piece.ROOK));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowLeft, Piece.BISHOP));
					}
				}
				// Capture right
				if(!isOnFileRight && (maskAdvancedOneRowRight & oppPieces) != 0) {
					if(!isPromotable) {
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowRight));
					} else {
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowRight, Piece.QUEEN));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowRight, Piece.KNIGHT));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowRight, Piece.ROOK));
						legalMovesNoncapture.add(
								new Move(i, indexAdvancedOneRowRight, Piece.BISHOP));
					}
				}
				// En passant
				if(board.enPassantTarget != 0) {
					if(!isOnFileLeft && maskAdvancedOneRowLeft == board.enPassantTarget) {
						legalMovesCapture.add(new Move(i, indexAdvancedOneRowLeft));
					}
					if(!isOnFileRight && maskAdvancedOneRowRight == board.enPassantTarget) {
						legalMovesCapture.add(new Move(i, indexAdvancedOneRowRight));
					}
				}
			}
			else if((board.bitboards.get(board.turn).get(Piece.BISHOP).data & mask) != 0) {
				this.appendLegalMovesForPieceLongRangeDiagonal(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((board.bitboards.get(board.turn).get(Piece.ROOK).data & mask) != 0) {
				this.appendLegalMovesForPieceLongRangeStraight(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((board.bitboards.get(board.turn).get(Piece.QUEEN).data & mask) != 0) {
				this.appendLegalMovesForPieceLongRangeDiagonal(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
				this.appendLegalMovesForPieceLongRangeStraight(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((board.bitboards.get(board.turn).get(Piece.KNIGHT).data & mask) != 0) {
				this.appendLegalMovesForKnight(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
			else if((board.bitboards.get(board.turn).get(Piece.KING).data & mask) != 0) {
				this.appendLegalMovesForKing(i, myPieces, oppPieces,
						legalMovesCapture, legalMovesNoncapture);
			}
		}
		
		// Castling
		if(!capturesOnly) {
			for(Castle castle : Castle.values()) {
				if(!board.castleRights.get(board.turn).get(castle)) {
					continue;
				}
				if((board.allPieces.data &
						this.maskCastleSpace.get(board.turn).get(castle)) != 0) {
					continue;
				}
				if(this.verifyCastleCheckRule(board, castle)) {
					legalMovesNoncapture.add(this.castleMoves.get(board.turn).get(castle));
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
			copy.turn = Color.flip(copy.turn);
			if(!copy.isInCheck()) {
				result.add(m);
			}
		}
		return result;
	}
	
	private static NotationHelper notationHelper = new NotationHelper();
	
	private Map<Color, Map<Castle, Long>> maskCastleSpace =
			new HashMap<Color, Map<Castle, Long>>();
	private Map<Color, Map<Castle, Long>> maskCastlePawns =
			new HashMap<Color, Map<Castle, Long>>();
	private Map<Color, Map<Castle, Long>> maskCastleKnights =
			new HashMap<Color, Map<Castle, Long>>();
	
	private Map<Color, Map<Castle, ArrayList<CastleRay>>> castleRaysDiagonal =
			new HashMap<Color, Map<Castle, ArrayList<CastleRay>>>();
	private Map<Color, Map<Castle, ArrayList<CastleRay>>> castleRaysStraight =
			new HashMap<Color, Map<Castle, ArrayList<CastleRay>>>();

	private Map<Color, Map<Castle, Move>> castleMoves =
			new HashMap<Color, Map<Castle, Move>>();
}
