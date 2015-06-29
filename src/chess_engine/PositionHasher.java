package chess_engine;

import java.util.Random;

// https://en.wikipedia.org/wiki/Zobrist_hashing
public class PositionHasher {
	public PositionHasher() {
		// We want the Zorbist tables to be "messy" but be exactly the same
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
	
	public long getMaskBlackBishop(int index) {
		return this.blackBishopMask[index];
	}
	
	public long getMaskBlackBishop(int index, int index2) {
		return this.blackBishopMask[index] ^ this.blackBishopMask[index2];
	}
	
	public long getMaskBlackKing(int index) {
		return this.blackKingMask[index];
	}
	
	public long getMaskBlackKing(int index, int index2) {
		return this.blackKingMask[index] ^ this.blackKingMask[index2];
	}
	
	public long getMaskBlackKnight(int index) {
		return this.blackKnightMask[index];
	}
	
	public long getMaskBlackKnight(int index, int index2) {
		return this.blackKnightMask[index] ^ this.blackKnightMask[index2];
	}
	
	public long getMaskBlackPawn(int index) {
		return this.blackPawnMask[index];
	}
	
	public long getMaskBlackPawn(int index, int index2) {
		return this.blackPawnMask[index] ^ this.blackPawnMask[index2];
	}
	
	public long getMaskBlackQueen(int index) {
		return this.blackQueenMask[index];
	}
	
	public long getMaskBlackQueen(int index, int index2) {
		return this.blackQueenMask[index] ^ this.blackQueenMask[index2];
	}
	
	public long getMaskBlackRook(int index) {
		return this.blackRookMask[index];
	}
	
	public long getMaskBlackRook(int index, int index2) {
		return this.blackRookMask[index] ^ this.blackRookMask[index2];
	}
	
	public long getMaskWhiteBishop(int index) {
		return this.whiteBishopMask[index];
	}
	
	public long getMaskWhiteBishop(int index, int index2) {
		return this.whiteBishopMask[index] ^ this.whiteBishopMask[index2];
	}
	
	public long getMaskWhiteKing(int index) {
		return this.whiteKingMask[index];
	}
	
	public long getMaskWhiteKing(int index, int index2) {
		return this.whiteKingMask[index] ^ this.whiteKingMask[index2];
	}
	
	public long getMaskWhiteKnight(int index) {
		return this.whiteKnightMask[index];
	}
	
	public long getMaskWhiteKnight(int index, int index2) {
		return this.whiteKnightMask[index] ^ this.whiteKnightMask[index2];
	}
	
	public long getMaskWhitePawn(int index) {
		return this.whitePawnMask[index];
	}
	
	public long getMaskWhitePawn(int index, int index2) {
		return this.whitePawnMask[index] ^ this.whitePawnMask[index2];
	}
	
	public long getMaskWhiteQueen(int index) {
		return this.whiteQueenMask[index];
	}
	
	public long getMaskWhiteQueen(int index, int index2) {
		return this.whiteQueenMask[index] ^ this.whiteQueenMask[index2];
	}
	
	public long getMaskWhiteRook(int index) {
		return this.whiteRookMask[index];
	}
	
	public long getMaskWhiteRook(int index, int index2) {
		return this.whiteRookMask[index] ^ this.whiteRookMask[index2];
	}
	
	public long getMaskEnPassantTarget(int index) {
		return this.whitePawnMask[index % 8];
	}
	
	public long getMaskCastleRights(boolean whiteKingside,
			boolean whiteQueenside, boolean blackKingside,
			boolean blackQueenside) {
		int index = 0;
		if(whiteKingside) {
			index += 56;
		}
		if(whiteQueenside) {
			index += 4;
		}
		if(blackKingside) {
			index += 2;
		}
		if(blackQueenside) {
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
