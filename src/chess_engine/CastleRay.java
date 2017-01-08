// Checks whether castling is allowed for a given side and direction.

package chess_engine;

import java.util.ArrayList;

public class CastleRay {
	public CastleRay(long maskStart, long maskEnd, int stepSize) {
		this.maskStart = maskStart;
		this.maskEnd = maskEnd;
		this.stepSize = stepSize;
		
		this.masks = new ArrayList<Long>();
		this.maskTotal = 0;
		long mask = this.maskStart;
		this.masks.add(mask);
		this.maskTotal |= mask;
		while(true) {
			// We couldn't do a conventional while-loop because if we increment
			// the mask above the bounds of a 64-bit long it could get us in
			// trouble.
			if(mask == this.maskEnd) {
				this.masks.add(mask);
				this.maskTotal |= mask;
				break;
			}
			if(this.stepSize > 0) {
				mask <<= this.stepSize;
			}
			else {
				mask >>>= (-this.stepSize);
			}
			this.masks.add(mask);
			this.maskTotal |= mask;
		}
	}
	
	public CastleRay(String squareStart, String squareEnd, int stepSize) {
		this(notationHelper.generateMask(squareStart),
				notationHelper.generateMask(squareEnd), stepSize);
	}
	
	public CastleRay flip() {
		long maskStart = Bitboard.flip(this.maskStart);
		long maskEnd = Bitboard.flip(this.maskEnd);
		int stepSize = 0;
		if(Math.abs(this.stepSize) == 1) {
			stepSize = this.stepSize;
		} else if(this.stepSize > 0) {
			stepSize = this.stepSize - 16;
		} else {
			stepSize = this.stepSize + 16;
		}
		return new CastleRay(maskStart, maskEnd, stepSize);
	}
	
	public boolean opponentPiecePrecludesCastling(Color color, Castle castle,
			long oppPieceMask, long myPieces) {
		if((this.maskTotal & oppPieceMask) == 0) {
			// The opposing piece is not in the ray so it can't prevent us from
			// castling.
			return false;
		}
		if((this.maskTotal & myPieces) == 0) {
			// The opposing piece is in the ray but none of our pieces are, so
			// it prevents us from castling.
			return true;
		}
		// At this point both the opposing piece and one of our pieces is in
		// the ray. We can castle if and only if our piece is closer to
		// maskStart.
		for(long mask : this.masks) {
			if((mask & oppPieceMask) != 0) {
				return true;
			} else if((mask & myPieces) != 0) {
				return false;
			}
		}
		// We should never get to this point.
		return false;
	}
	
	private long maskStart;
	private long maskEnd;
	private int stepSize;
	private ArrayList<Long> masks;
	private long maskTotal;
	private static NotationHelper notationHelper = new NotationHelper();
}
