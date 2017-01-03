package chess_engine;

import java.util.Map;
import java.util.Random;

// https://en.wikipedia.org/wiki/Zobrist_hashing
public class PositionHasher {
	public PositionHasher() {
		// We want the Zobrist tables to be "messy" but be exactly the same
		// every time the chess engine runs. This at least helps with
		// debugging.
		Random generator = new Random(0);
		
		for(int i = 0; i < 64; i++) {
			this.blackBishopMask[i] = generator.nextLong();
			this.blackKingMask[i] = generator.nextLong();
			this.blackKnightMask[i] = generator.nextLong();
			this.blackPawnMask[i] = generator.nextLong();
			this.blackQueenMask[i] = generator.nextLong();
			this.blackRookMask[i] = generator.nextLong();
			
			this.whiteBishopMask[i] = generator.nextLong();
			this.whiteKingMask[i] = generator.nextLong();
			this.whiteKnightMask[i] = generator.nextLong();
			this.whitePawnMask[i] = generator.nextLong();
			this.whiteQueenMask[i] = generator.nextLong();
			this.whiteRookMask[i] = generator.nextLong();
		}
	}
	
	// We take advantage of the fact that pawns can't be in the edge ranks to
	// stuff extra metadata into only 12 long variables:
	// 1. The en passant target file is in the first rank of the white pawns.
	// 2. The board color is the a8 square of the white pawns.
	// 3. The castling rights are in the first and last rows of the black
	//    pawns. [whiteKingside, whiteQueenside, blackKingside, blackQueenside]
	//    has a total of 16 bits. These bits map to a1-h1 and then a8-h8 in the
	//    black pawn mask.
	
	public long getMask(Color color, Piece piece, byte index) {
		if(color == Color.WHITE) {
			if(piece == Piece.BISHOP) {
				return getMaskWhiteBishop(index);
			} else if(piece == Piece.KING) {
				return getMaskWhiteKing(index);
			} else if(piece == Piece.KNIGHT) {
				return getMaskWhiteKnight(index);
			} else if(piece == Piece.PAWN) {
				return getMaskWhitePawn(index);
			} else if(piece == Piece.QUEEN) {
				return getMaskWhiteQueen(index);
			} else if(piece == Piece.ROOK) {
				return getMaskWhiteRook(index);
			}
		} else {
			if(piece == Piece.BISHOP) {
				return getMaskBlackBishop(index);
			} else if(piece == Piece.KING) {
				return getMaskBlackKing(index);
			} else if(piece == Piece.KNIGHT) {
				return getMaskBlackKnight(index);
			} else if(piece == Piece.PAWN) {
				return getMaskBlackPawn(index);
			} else if(piece == Piece.QUEEN) {
				return getMaskBlackQueen(index);
			} else if(piece == Piece.ROOK) {
				return getMaskBlackRook(index);
			}
		}
		return 0;
	}
	
	public long getMask(Color color, Piece piece, byte index, byte index2) {
		if(color == Color.WHITE) {
			if(piece == Piece.BISHOP) {
				return getMaskWhiteBishop(index, index2);
			} else if(piece == Piece.KING) {
				return getMaskWhiteKing(index, index2);
			} else if(piece == Piece.KNIGHT) {
				return getMaskWhiteKnight(index, index2);
			} else if(piece == Piece.PAWN) {
				return getMaskWhitePawn(index, index2);
			} else if(piece == Piece.QUEEN) {
				return getMaskWhiteQueen(index, index2);
			} else if(piece == Piece.ROOK) {
				return getMaskWhiteRook(index, index2);
			}
		} else {
			if(piece == Piece.BISHOP) {
				return getMaskBlackBishop(index, index2);
			} else if(piece == Piece.KING) {
				return getMaskBlackKing(index, index2);
			} else if(piece == Piece.KNIGHT) {
				return getMaskBlackKnight(index, index2);
			} else if(piece == Piece.PAWN) {
				return getMaskBlackPawn(index, index2);
			} else if(piece == Piece.QUEEN) {
				return getMaskBlackQueen(index, index2);
			} else if(piece == Piece.ROOK) {
				return getMaskBlackRook(index, index2);
			}
		}
		return 0;
	}
	
	public long getMaskBlackBishop(byte index) {
		return this.blackBishopMask[index];
	}
	
	public long getMaskBlackBishop(byte index, byte index2) {
		return this.blackBishopMask[index] ^ this.blackBishopMask[index2];
	}
	
	public long getMaskBlackKing(byte index) {
		return this.blackKingMask[index];
	}
	
	public long getMaskBlackKing(byte index, byte index2) {
		return this.blackKingMask[index] ^ this.blackKingMask[index2];
	}
	
	public long getMaskBlackKnight(byte index) {
		return this.blackKnightMask[index];
	}
	
	public long getMaskBlackKnight(byte index, byte index2) {
		return this.blackKnightMask[index] ^ this.blackKnightMask[index2];
	}
	
	public long getMaskBlackPawn(byte index) {
		return this.blackPawnMask[index];
	}
	
	public long getMaskBlackPawn(byte index, byte index2) {
		return this.blackPawnMask[index] ^ this.blackPawnMask[index2];
	}
	
	public long getMaskBlackQueen(byte index) {
		return this.blackQueenMask[index];
	}
	
	public long getMaskBlackQueen(byte index, byte index2) {
		return this.blackQueenMask[index] ^ this.blackQueenMask[index2];
	}
	
	public long getMaskBlackRook(byte index) {
		return this.blackRookMask[index];
	}
	
	public long getMaskBlackRook(byte index, byte index2) {
		return this.blackRookMask[index] ^ this.blackRookMask[index2];
	}
	
	public long getMaskWhiteBishop(byte index) {
		return this.whiteBishopMask[index];
	}
	
	public long getMaskWhiteBishop(byte index, byte index2) {
		return this.whiteBishopMask[index] ^ this.whiteBishopMask[index2];
	}
	
	public long getMaskWhiteKing(byte index) {
		return this.whiteKingMask[index];
	}
	
	public long getMaskWhiteKing(byte index, byte index2) {
		return this.whiteKingMask[index] ^ this.whiteKingMask[index2];
	}
	
	public long getMaskWhiteKnight(byte index) {
		return this.whiteKnightMask[index];
	}
	
	public long getMaskWhiteKnight(byte index, byte index2) {
		return this.whiteKnightMask[index] ^ this.whiteKnightMask[index2];
	}
	
	public long getMaskWhitePawn(byte index) {
		return this.whitePawnMask[index];
	}
	
	public long getMaskWhitePawn(byte index, byte index2) {
		return this.whitePawnMask[index] ^ this.whitePawnMask[index2];
	}
	
	public long getMaskWhiteQueen(byte index) {
		return this.whiteQueenMask[index];
	}
	
	public long getMaskWhiteQueen(byte index, byte index2) {
		return this.whiteQueenMask[index] ^ this.whiteQueenMask[index2];
	}
	
	public long getMaskWhiteRook(byte index) {
		return this.whiteRookMask[index];
	}
	
	public long getMaskWhiteRook(byte index, byte index2) {
		return this.whiteRookMask[index] ^ this.whiteRookMask[index2];
	}
	
	public long getMaskEnPassantTarget(byte index) {
		return this.whitePawnMask[index % 8];
	}
	
	public long getMaskCastleRights(Map<Color, Boolean> castleRightKingside,
			Map<Color, Boolean> castleRightQueenside) {
		byte index = 0;
		if(castleRightKingside.get(Color.WHITE)) {
			index += 56;
		}
		if(castleRightQueenside.get(Color.WHITE)) {
			index += 4;
		}
		if(castleRightKingside.get(Color.BLACK)) {
			index += 2;
		}
		if(castleRightQueenside.get(Color.BLACK)) {
			index++;
		}
		return this.blackPawnMask[index];
	}
	
	public long getMaskTurn() {
		return this.whitePawnMask[56];   // The a8 square
	}
	
	private long[] blackBishopMask = new long[64];
	private long[] blackKingMask = new long[64];
	private long[] blackKnightMask = new long[64];
	private long[] blackPawnMask = new long[64];
	private long[] blackQueenMask = new long[64];
	private long[] blackRookMask = new long[64];

	private long[] whiteBishopMask = new long[64];
	private long[] whiteKingMask = new long[64];
	private long[] whiteKnightMask = new long[64];
	private long[] whitePawnMask = new long[64];
	private long[] whiteQueenMask = new long[64];
	private long[] whiteRookMask = new long[64];
}
