package chess_engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LegalMoveGenerator {
	public LegalMoveGenerator() {
		this.maskCastleSpace = new HashMap<Color, Map<Castle, Long>>();
		this.maskCastlePawns = new HashMap<Color, Map<Castle, Long>>();
		this.maskCastleKnights = new HashMap<Color, Map<Castle, Long>>();
		this.castleRaysDiagonal = new HashMap<Color, Map<Castle, ArrayList<CastleRay>>>();
		this.castleRaysStraight = new HashMap<Color, Map<Castle, ArrayList<CastleRay>>>();
		this.castleMoves = new HashMap<Color, Map<Castle, Move>>();
		
		for(Color color : Color.values()) {
			this.maskCastleSpace.put(color, new HashMap<Castle, Long>());
			this.maskCastlePawns.put(color, new HashMap<Castle, Long>());
			this.maskCastleKnights.put(color, new HashMap<Castle, Long>());
		}
		
		this.maskCastleSpace.get(Color.WHITE).put(Castle.KINGSIDE,
			new Bitboard("f1", "g1").getData());
		this.maskCastleSpace.get(Color.WHITE).put(Castle.QUEENSIDE,
			new Bitboard("b1", "c1", "d1").getData());
		this.maskCastleSpace.get(Color.BLACK).put(Castle.KINGSIDE,
			new Bitboard(this.maskCastleSpace.get(Color.WHITE)
				.get(Castle.KINGSIDE)).flip().getData());
		this.maskCastleSpace.get(Color.BLACK).put(Castle.QUEENSIDE,
			new Bitboard(this.maskCastleSpace.get(Color.WHITE)
				.get(Castle.QUEENSIDE)).flip().getData());
		
		this.maskCastlePawns.get(Color.WHITE).put(Castle.KINGSIDE,
			new Bitboard("d2", "e2", "f2", "g2").getData());
		this.maskCastlePawns.get(Color.WHITE).put(Castle.QUEENSIDE,
			new Bitboard("b2", "c2", "d2", "e2", "f2").getData());
		this.maskCastlePawns.get(Color.BLACK).put(Castle.KINGSIDE,
			new Bitboard(this.maskCastlePawns.get(Color.WHITE)
				.get(Castle.KINGSIDE)).flip().getData());
		this.maskCastlePawns.get(Color.BLACK).put(Castle.QUEENSIDE,
			new Bitboard(this.maskCastlePawns.get(Color.WHITE)
				.get(Castle.QUEENSIDE)).flip().getData());
			
		this.maskCastleKnights.get(Color.WHITE).put(Castle.KINGSIDE,
			new Bitboard("c2", "d2", "g2", "d3", "e3", "f3", "g3", "h2")
				.getData());
		this.maskCastleKnights.get(Color.WHITE).put(Castle.QUEENSIDE,
			new Bitboard("b2", "c2", "f2", "g2", "c3", "d3", "e3", "f3")
				.getData());
		this.maskCastleKnights.get(Color.BLACK).put(Castle.KINGSIDE,
			new Bitboard(this.maskCastleKnights.get(Color.WHITE)
				.get(Castle.KINGSIDE)).flip().getData());
		this.maskCastleKnights.get(Color.BLACK).put(Castle.QUEENSIDE,
			new Bitboard(this.maskCastleKnights.get(Color.WHITE)
				.get(Castle.QUEENSIDE)).flip().getData());
		
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
		this.castleMoves.get(Color.WHITE).put(Castle.KINGSIDE, new Move((byte)4, (byte)6));
		this.castleMoves.get(Color.WHITE).put(Castle.QUEENSIDE, new Move((byte)4, (byte)2));
		this.castleMoves.get(Color.BLACK).put(Castle.KINGSIDE, new Move((byte)60, (byte)62));
		this.castleMoves.get(Color.BLACK).put(Castle.QUEENSIDE, new Move((byte)60, (byte)58));
		
		this.initAttackSquaresPawn(Color.WHITE);
		this.initAttackSquaresPawn(Color.BLACK);
		this.initAttackSquaresShortRange(this.attackSquaresKing,
			new int[]{-9, -8, -7, -1, 1, 7, 8, 9});
		this.initAttackSquaresShortRange(this.attackSquaresKnight,
			new int[]{-17, -15, -10, -6, 6, 10, 15, 17});
		this.initAttackSquaresLongRange(this.attackSquaresA1H8,
			new int[]{-9, 9});
		this.initAttackSquaresLongRange(this.attackSquaresA8H1,
			new int[]{-7, 7});
		this.initAttackSquaresLongRange(this.attackSquaresHorizontal,
			new int[]{-1, 1});
		this.initAttackSquaresLongRange(this.attackSquaresVertical,
			new int[]{-8, 8});
		for(int i = 0; i < 64; i++) {
			this.attackSquaresBishop[i] = this.attackSquaresA1H8[i] |
				this.attackSquaresA8H1[i];
			this.attackSquaresRook[i] = this.attackSquaresHorizontal[i] |
				this.attackSquaresVertical[i];
			this.attackSquaresQueen[i] = this.attackSquaresBishop[i] |
				this.attackSquaresRook[i];
			this.attackSquaresPawnBlack[i] =
				this.attackSquaresPawnCaptureBlack[i] |
				this.attackSquaresPawnMoveBlack[i];
			this.attackSquaresPawnWhite[i] =
				this.attackSquaresPawnCaptureWhite[i] |
				this.attackSquaresPawnMoveWhite[i];
		}
	}
	
	private void initAttackSquaresPawn(Color color) {
		int start = 0;
		int end = 64;
		if(color == Color.WHITE) {
			start = 8;
		} else {
			end = 56;
		}
		for(int i = start; i < end; i++) {
			ArrayList<Integer> stepSizesMove = new ArrayList<Integer>();
			ArrayList<Integer> stepSizesCapture = new ArrayList<Integer>();
			if(color == Color.WHITE) {
				stepSizesMove.add(8);
				stepSizesCapture.add(7);
				stepSizesCapture.add(9);
				if(i < 16) {
					stepSizesMove.add(16);
				}
			} else {
				stepSizesMove.add(-8);
				stepSizesCapture.add(-7);
				stepSizesCapture.add(-9);
				if(i >= 48) {
					stepSizesMove.add(-16);
				}
			}
			for(int stepSize : stepSizesMove) {
				if(LegalMoveGenerator.inBounds(i, stepSize)) {
					if(color == Color.WHITE) {
						this.attackSquaresPawnMoveWhite[i] |= 1L << (i + stepSize);
					} else {
						this.attackSquaresPawnMoveBlack[i] |= 1L << (i + stepSize);
					}
				}
			}
			for(int stepSize : stepSizesCapture) {
				if(LegalMoveGenerator.inBounds(i, stepSize)) {
					if(color == Color.WHITE) {
						this.attackSquaresPawnCaptureWhite[i] |= 1L << (i + stepSize);
					} else {
						this.attackSquaresPawnCaptureBlack[i] |= 1L << (i + stepSize);
					}
				}
			}
		}
	}
	
	private void initAttackSquaresShortRange(long[] attackSquares, int[] stepSizes) {
		for(int i = 0; i < 64; i++) {
			for(int stepSize : stepSizes) {
				if(LegalMoveGenerator.inBounds(i, stepSize)) {
					attackSquares[i] |= 1L << (i + stepSize);
				}
			}
		}
	}
	
	private void initAttackSquaresLongRange(long[] attackSquares, int[] stepSizes) {
		for(int i = 0; i < 64; i++) {
			for(int stepSize : stepSizes) {
				int position = i;
				while(inBounds(position, stepSize)) {
					position += stepSize;
					attackSquares[i] |= 1L << position;
				}
			}
		}
	}
	
	public void addCastleRayDiagonal(Color color, Castle castle, String squareStart,
			String squareEnd, int stepSize) {
		if(!this.castleRaysDiagonal.containsKey(color)) {
			this.castleRaysDiagonal.put(color, new HashMap<Castle, ArrayList<CastleRay>>());
		}
		if(!this.castleRaysDiagonal.get(color).containsKey(castle)) {
			this.castleRaysDiagonal.get(color).put(castle, new ArrayList<CastleRay>());
		}
		this.castleRaysDiagonal.get(color).get(castle).add(
			new CastleRay(squareStart, squareEnd, stepSize));
	}
	
	public void addCastleRayStraight(Color color, Castle castle, String squareStart,
			String squareEnd, int stepSize) {
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
		
		// If the step size applies to knights, the end file can be different from the start file
		// by up to two.
		int fileDiff = (position % 8) - ((position + stepSize) % 8);
		if(fileDiff < -2) {
			return false;
		}
		if(fileDiff > 2) {
			return false;
		}
		
		return true;
	}
	
	private void appendLegalMovesForPawn(Board board,
			ArrayList<Move> legalMoves, boolean capturesOnly) {
		long movers = board.bitboards.get(board.turn).get(Piece.PAWN).getData();
		long oppPieces = board.playerBitboards.get(Color.flip(board.turn)).getData();
		long[] attackSquaresMoveTable = board.turn == Color.WHITE ?
			this.attackSquaresPawnMoveWhite : this.attackSquaresPawnMoveBlack;
		long[] attackSquaresCaptureTable = board.turn == Color.WHITE ?
			this.attackSquaresPawnCaptureWhite : this.attackSquaresPawnCaptureBlack;
		while(movers != 0) {
			int moverIndex = Long.numberOfTrailingZeros(movers);
			long mover = 1L << moverIndex;
			movers ^= mover;
			boolean isPromotable =
				(board.turn == Color.WHITE && moverIndex >= 48) ||
				(board.turn == Color.BLACK && moverIndex < 16);
			
			// If the pawn is trying to move two squares up and there's
			// something blocking the first square, pretend it's also blocking
			// the second square.
			long moveBlockers = board.allPieces.getData() & ~mover;
			if(board.turn == Color.WHITE) {
				moveBlockers |= (moveBlockers & 0x0000000000FF0000L) << 8;
			} else {
				moveBlockers |= (moveBlockers & 0x0000FF0000000000L) >>> 8;
			}
			
			if(!capturesOnly) {
				long attackSquaresMove = attackSquaresMoveTable[moverIndex];
				attackSquaresMove &= ~moveBlockers;
				while(attackSquaresMove != 0) {
					int attackSquareIndex = Long.numberOfTrailingZeros(attackSquaresMove);
					long attackSquare = 1L << attackSquareIndex;
					attackSquaresMove ^= attackSquare;
					if(!isPromotable) {
						legalMoves.add(new Move(moverIndex, attackSquareIndex));
					} else {
						legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.QUEEN));
						legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.KNIGHT));
						legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.ROOK));
						legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.BISHOP));
					}
				}
			}
			
			long attackSquaresCapture = attackSquaresCaptureTable[moverIndex];
			attackSquaresCapture &= (oppPieces | board.enPassantTarget);
			while(attackSquaresCapture != 0) {
				int attackSquareIndex = Long.numberOfTrailingZeros(attackSquaresCapture);
				long attackSquare = 1L << attackSquareIndex;
				attackSquaresCapture ^= attackSquare;
				if(!isPromotable) {
					legalMoves.add(new Move(moverIndex, attackSquareIndex));
				} else {
					legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.QUEEN));
					legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.KNIGHT));
					legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.ROOK));
					legalMoves.add(new Move(moverIndex, attackSquareIndex, Piece.BISHOP));
				}
			}
		}
	}
	
	private void appendLegalMovesForLongRangePiece(Board board, Piece piece,
			long[] attackSquaresTable, ArrayList<Move> legalMoves, boolean capturesOnly) {
		long movers = board.bitboards.get(board.turn).get(piece).getData();
		long myPieces = board.playerBitboards.get(board.turn).getData();
		while(movers != 0) {
			int moverIndex = Long.numberOfTrailingZeros(movers);
			long mover = 1L << moverIndex;
			movers ^= mover;
			long attackSquares = attackSquaresTable[moverIndex];
			long incidentSquares = attackSquares & board.allPieces.getData();
			long incidentSquaresBefore = incidentSquares & (mover - 1L);
			long incidentSquaresAfter = moverIndex == 63 ? 0L :
				incidentSquares & ~(mover + mover - 1L);
			int leadingZerosBefore = Long.numberOfLeadingZeros(incidentSquaresBefore);
			long incidentMaskBefore = leadingZerosBefore == 64 ? ~0L :
				~((1L << (63 - leadingZerosBefore)) - 1L);
			int trailingZerosAfter = Long.numberOfTrailingZeros(incidentSquaresAfter);
			long incidentMaskAfter =
				(trailingZerosAfter == 0 || trailingZerosAfter >= 63) ? ~0L :
				(1L << (trailingZerosAfter + 1)) - 1L;
			attackSquares &= incidentMaskBefore;
			attackSquares &= incidentMaskAfter;
			while(attackSquares != 0) {
				int attackSquareIndex = Long.numberOfTrailingZeros(attackSquares);
				long attackSquare = 1L << attackSquareIndex;
				attackSquares ^= attackSquare;
				if((attackSquare & myPieces) != 0) {
					continue;
				}
				if(capturesOnly && !board.allPieces.intersects(attackSquare)) {
					continue;
				}
				legalMoves.add(new Move(moverIndex, attackSquareIndex));
			}
		}
	}
	
	private void appendLegalMovesForBishop(Board board,
			ArrayList<Move> legalMoves, boolean capturesOnly) {
		this.appendLegalMovesForLongRangePiece(board, Piece.BISHOP, this.attackSquaresA1H8,
			legalMoves, capturesOnly);
		this.appendLegalMovesForLongRangePiece(board, Piece.BISHOP, this.attackSquaresA8H1,
			legalMoves, capturesOnly);
	}
	
	private void appendLegalMovesForQueen(Board board,
			ArrayList<Move> legalMoves, boolean capturesOnly) {
		this.appendLegalMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresA1H8,
			legalMoves, capturesOnly);
		this.appendLegalMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresA8H1,
			legalMoves, capturesOnly);
		this.appendLegalMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresHorizontal,
			legalMoves, capturesOnly);
		this.appendLegalMovesForLongRangePiece(board, Piece.QUEEN, this.attackSquaresVertical,
			legalMoves, capturesOnly);
	}
	
	private void appendLegalMovesForRook(Board board,
			ArrayList<Move> legalMoves, boolean capturesOnly) {
		this.appendLegalMovesForLongRangePiece(board, Piece.ROOK, this.attackSquaresHorizontal,
			legalMoves, capturesOnly);
		this.appendLegalMovesForLongRangePiece(board, Piece.ROOK, this.attackSquaresVertical,
			legalMoves, capturesOnly);
	}
	
	private void appendLegalMovesForShortRangePiece(Board board, Piece piece,
			long[] attackSquaresTable, ArrayList<Move> legalMoves, boolean capturesOnly) {
		long movers = board.bitboards.get(board.turn).get(piece).getData();
		while(movers != 0) {
			int moverIndex = Long.numberOfTrailingZeros(movers);
			long mover = 1L << moverIndex;
			movers ^= mover;
			long attackSquares = attackSquaresTable[moverIndex];
			attackSquares &= ~board.playerBitboards.get(board.turn).getData();
			while(attackSquares != 0) {
				int attackSquareIndex = Long.numberOfTrailingZeros(attackSquares);
				long attackSquare = 1L << attackSquareIndex;
				attackSquares ^= attackSquare;
				if(capturesOnly && !board.allPieces.intersects(attackSquare)) {
					continue;
				}
				legalMoves.add(new Move(moverIndex, attackSquareIndex));
			}
		}
	}
	
	private void appendLegalMovesForKing(Board board, ArrayList<Move> legalMoves,
			boolean capturesOnly) {
		this.appendLegalMovesForShortRangePiece(board, Piece.KING, this.attackSquaresKing,
			legalMoves, capturesOnly);
	}
	
	private void appendLegalMovesForKnight(Board board, ArrayList<Move> legalMoves,
			boolean capturesOnly) {
		this.appendLegalMovesForShortRangePiece(board, Piece.KNIGHT, this.attackSquaresKnight,
			legalMoves, capturesOnly);
	}
	
	private ArrayList<Move> getLegalMovesForCastling(Board board) {
		ArrayList<Move> result = new ArrayList<Move>();
		for(Castle castle : Castle.values()) {
			if(!board.castleRights.get(board.turn).get(castle)) {
				continue;
			}
			if(board.allPieces.intersects(this.maskCastleSpace.get(board.turn).get(castle))) {
				continue;
			}
			if(this.verifyCastleCheckRule(board, castle)) {
				result.add(this.castleMoves.get(board.turn).get(castle));
			}
		}
		return result;
	}
	
	private boolean verifyCastleCheckRule(Board board, Castle castle) {
		// Make sure the castle isn't happening out of a check or through a check. (If it's into a
		// check the opponent would take the king in the next move, so the AI wouldn't do it
		// anyway.)
		Color turnFlipped = Color.flip(board.turn);
		long oppPiecesDiagonal =
			board.bitboards.get(turnFlipped).get(Piece.BISHOP).union(
			board.bitboards.get(turnFlipped).get(Piece.QUEEN)).getData();
		long oppPiecesStraight =
			board.bitboards.get(turnFlipped).get(Piece.ROOK).union(
			board.bitboards.get(turnFlipped).get(Piece.QUEEN)).getData();
		if(board.bitboards.get(turnFlipped).get(Piece.PAWN).intersects(
				this.maskCastlePawns.get(board.turn).get(castle))) {
			return false;
		}
		if(board.bitboards.get(turnFlipped).get(Piece.KNIGHT).intersects(
				this.maskCastleKnights.get(board.turn).get(castle))) {
			return false;
		}
		for(CastleRay castleRay : this.castleRaysDiagonal.get(board.turn).get(castle)) {
			long otherPieces = board.allPieces.getData() & ~oppPiecesDiagonal;
			if(castleRay.opponentPiecePrecludesCastling(oppPiecesDiagonal, otherPieces)) {
				return false;
			}
			
		}
		for(CastleRay castleRay : this.castleRaysStraight.get(board.turn).get(castle)) {
			long otherPieces = board.allPieces.getData() & ~oppPiecesStraight;
			if(castleRay.opponentPiecePrecludesCastling(oppPiecesStraight, otherPieces)) {
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
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		Bitboard myKings = board.bitboards.get(board.turn).get(Piece.KING);
		
		// Don't return before restoring the board.
		board.turn = Color.flip(board.turn);
		
		this.appendLegalMovesForQueen(board, legalMoves, true);
		this.appendLegalMovesForRook(board, legalMoves, true);
		this.appendLegalMovesForBishop(board, legalMoves, true);
		this.appendLegalMovesForKnight(board, legalMoves, true);
		this.appendLegalMovesForPawn(board, legalMoves, true);
		this.appendLegalMovesForKing(board, legalMoves, true);
		
		for(Move move : legalMoves) {
			if(myKings.intersects(1L << move.destination)) {
				board.turn = Color.flip(board.turn);
				return true;
			}
		}
		
		board.turn = Color.flip(board.turn);
		return false;
	}
	
	public ArrayList<Move> legalMovesFast(Board board, boolean capturesOnly) {
		// Calculate the legal moves without verifying that they don't put the
		// player in check. extraCapture is for en passant, to list the extra
		// square we're capturing (if 1 destroy the piece below the
		// destination)
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		
		this.appendLegalMovesForQueen(board, legalMoves, capturesOnly);
		this.appendLegalMovesForRook(board, legalMoves, capturesOnly);
		this.appendLegalMovesForBishop(board, legalMoves, capturesOnly);
		this.appendLegalMovesForKnight(board, legalMoves, capturesOnly);
		this.appendLegalMovesForPawn(board, legalMoves, capturesOnly);
		this.appendLegalMovesForKing(board, legalMoves, capturesOnly);
		
		if(!capturesOnly) {
			ArrayList<Move> legalMovesResult = new ArrayList<Move>();
			ArrayList<Move> legalMovesNoncapture = new ArrayList<Move>();
			for(Move move : legalMoves) {
				if(board.allPieces.intersects(1L << move.destination)) {
					legalMovesResult.add(move);
				} else {
					legalMovesNoncapture.add(move);
				}
			}
			legalMovesResult.addAll(legalMovesNoncapture);
			legalMovesResult.addAll(this.getLegalMovesForCastling(board));
			return legalMovesResult;
		} else {
			ArrayList<Move> legalMovesCapture = new ArrayList<Move>();
			for(Move move : legalMoves) {
				if(board.allPieces.intersects(1L << move.destination)) {
					legalMovesCapture.add(move);
				}
			}
			return legalMovesCapture;
		}
	}
	
	public ArrayList<Move> legalMoves(Board board) {
		ArrayList<Move> legalMovesFast = this.legalMovesFast(board, false);
		ArrayList<Move> result = new ArrayList<Move>();
		for(Move m : legalMovesFast) {
			Board copy = new Board(board);
			copy.move(m);
			// Go back to the original player to see if they're in check.
			copy.turn = Color.flip(copy.turn);
			if(!copy.isInCheck()) {
				result.add(m);
			}
		}
		return result;
	}
		
	private Map<Color, Map<Castle, Long>> maskCastleSpace;
	private Map<Color, Map<Castle, Long>> maskCastlePawns;
	private Map<Color, Map<Castle, Long>> maskCastleKnights;
	
	private Map<Color, Map<Castle, ArrayList<CastleRay>>> castleRaysDiagonal;
	private Map<Color, Map<Castle, ArrayList<CastleRay>>> castleRaysStraight;

	private Map<Color, Map<Castle, Move>> castleMoves;
	
	private long[] attackSquaresA1H8 = new long[64];
	private long[] attackSquaresA8H1 = new long[64];
	private long[] attackSquaresBishop = new long[64];
	private long[] attackSquaresHorizontal = new long[64];
	private long[] attackSquaresKing = new long[64];
	private long[] attackSquaresKnight = new long[64];
	private long[] attackSquaresPawnBlack = new long[64];
	private long[] attackSquaresPawnCaptureBlack = new long[64];
	private long[] attackSquaresPawnCaptureWhite = new long[64];
	private long[] attackSquaresPawnMoveBlack = new long[64];
	private long[] attackSquaresPawnMoveWhite = new long[64];
	private long[] attackSquaresPawnWhite = new long[64];
	private long[] attackSquaresQueen = new long[64];
	private long[] attackSquaresRook = new long[64];
	private long[] attackSquaresVertical = new long[64];
}
