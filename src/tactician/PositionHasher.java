package tactician;

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
		
		this.masks = new HashMap<Color, Map<Piece, Long[]>>();
		for(Color color : Color.values()) {
			Map<Piece, Long[]> colorMask = new HashMap<Piece, Long[]>();
			for(Piece piece : Piece.values()) {
				if(piece == null) {
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
	
	public long getMask(Color color, Piece piece, int index) {
		return this.masks.get(color).get(piece)[index];
	}
	
	public long getMask(Color color, Piece piece, int index, int index2) {
		return this.getMask(color, piece, index) ^ this.getMask(
			color, piece, index2);
	}
	
	public long getMaskEnPassantTarget(long enPassantTarget) {
		int index = Long.numberOfTrailingZeros(enPassantTarget);
		return this.getMask(Color.WHITE, Piece.PAWN, index % 8);
	}
	
	public long getMaskCastleRights(
			Map<Color, Map<Castle, Boolean>> castleRights) {
		byte index = 0;
		if(castleRights.get(Color.WHITE).get(Castle.KINGSIDE)) {
			index += 56;
		}
		if(castleRights.get(Color.WHITE).get(Castle.QUEENSIDE)) {
			index += 4;
		}
		if(castleRights.get(Color.BLACK).get(Castle.KINGSIDE)) {
			index += 2;
		}
		if(castleRights.get(Color.BLACK).get(Castle.QUEENSIDE)) {
			index++;
		}
		return this.getMask(Color.WHITE, Piece.PAWN, index);
	}
	
	public long getMaskTurn() {
		// The a8 square is reserved for turn hashing.
		return this.getMask(Color.WHITE, Piece.PAWN, (byte)(56));
	}
	
	private Map<Color, Map<Piece, Long[]>> masks;
}
