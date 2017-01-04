package chess_engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// https://en.wikipedia.org/wiki/Zobrist_hashing
public class PositionHasher {
	public PositionHasher() {
		// We want the Zobrist tables to be "messy" but be exactly the same
		// every time the chess engine runs. This at least helps with
		// debugging.
		Random generator = new Random(0);
		
		for(Color color : Color.values()) {
			Map<Piece, Long[]> colorMask = new HashMap<Piece, Long[]>();
			for(Piece piece : Piece.values()) {
				if(piece == Piece.NOPIECE) {
					continue;
				}
				Long[] pieceMasks = new Long[64];
				for(int i = 0; i < 64; i++) {
					pieceMasks[i] = generator.nextLong();
				}
				colorMask.put(piece, pieceMasks);
			}
			this.masks.put(color, colorMask);
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
		return this.masks.get(color).get(piece)[index];
	}
	
	public long getMask(Color color, Piece piece, byte index, byte index2) {
		return this.getMask(color, piece, index) ^ this.getMask(color, piece, index2);
	}
	
	public long getMaskEnPassantTarget(byte index) {
		return this.getMask(Color.WHITE, Piece.PAWN, (byte)(index % 8));
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
		return this.getMask(Color.WHITE, Piece.PAWN, index);
	}
	
	public long getMaskTurn() {
		return this.getMask(Color.WHITE, Piece.PAWN, (byte)(56));   // The a8 square
	}
	
	private Map<Color, Map<Piece, Long[]>> masks = new HashMap<Color, Map<Piece, Long[]>>();
}
