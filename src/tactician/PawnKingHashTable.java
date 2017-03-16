package tactician;

public class PawnKingHashTable {
	public class PawnHashTableEntry {
		public PawnHashTableEntry() {
		}
		
		public PawnHashTableEntry(long positionHash, long pawnMaskWhite, long pawnMaskBlack,
				int kingIndexWhite, int kingIndexBlack) {
			this.positionHash = positionHash;
			this.pawnMaskWhite = pawnMaskWhite;
			this.pawnMaskBlack = pawnMaskBlack;
			this.kingIndexWhite = kingIndexWhite;
			this.kingIndexBlack = kingIndexBlack;
		}
		
		public long positionHash = 0;
		public long pawnMaskWhite = 0;
		public long pawnMaskBlack = 0;
		public int kingIndexWhite = 0;
		public int kingIndexBlack = 0;
		
		// Computed values
		public int numDoubledPawnsWhite = 0;
		public int numDoubledPawnsBlack = 0;
		public int numIsolatedPawnsWhite = 0;
		public int numIsolatedPawnsBlack = 0;
		public int numPassedPawnsWhite = 0;
		public int numPassedPawnsBlack = 0;
	}
	
	public void put(long positionHash, long pawnMaskWhite, long pawnMaskBlack, int kingIndexWhite,
			int kingIndexBlack) {
		int index = this.index(positionHash);
		this.data[index] = new PawnHashTableEntry(positionHash, pawnMaskWhite, pawnMaskBlack,
			kingIndexWhite, kingIndexBlack);
		
		for(int fileIndex = 0; fileIndex < 8; fileIndex++) {
			Bitboard fileBitboard = Bitboard.bitboardFromFile(fileIndex);
			int filePawnsWhite = fileBitboard.intersection(pawnMaskWhite).numOccupied();
			int filePawnsBlack = fileBitboard.intersection(pawnMaskBlack).numOccupied();
			if(filePawnsWhite > 1) {
				this.data[index].numDoubledPawnsWhite += filePawnsWhite;
			}
			if(filePawnsBlack > 1) {
				this.data[index].numDoubledPawnsBlack += filePawnsBlack;
			}
			
			Bitboard fileBitboardLeft = new Bitboard();
			Bitboard fileBitboardRight = new Bitboard();
			if(fileIndex > 0) {
				fileBitboardLeft = new Bitboard(fileIndex - 1);
			}
			if(fileIndex < 7) {
				fileBitboardRight = new Bitboard(fileIndex + 1);
			}
			if(filePawnsWhite >= 1) {
				int filePawnsWhiteLeft = fileBitboardLeft
					.intersection(pawnMaskWhite).numOccupied();
				int filePawnsWhiteRight = fileBitboardRight
					.intersection(pawnMaskWhite).numOccupied();
				if(filePawnsWhiteLeft == 0 && filePawnsWhiteRight == 0) {
					this.data[index].numIsolatedPawnsWhite += filePawnsWhite;
				}
			}
			if(filePawnsBlack >= 1) {
				int filePawnsBlackLeft = fileBitboardLeft
					.intersection(pawnMaskBlack).numOccupied();
				int filePawnsBlackRight = fileBitboardRight
					.intersection(pawnMaskBlack).numOccupied();
				if(filePawnsBlackLeft == 0 && filePawnsBlackRight == 0) {
					this.data[index].numIsolatedPawnsBlack += filePawnsBlack;
				}
			}
		}
		
		long whitePawns = pawnMaskWhite;
		while(whitePawns != 0) {
			int pawnIndex = Long.numberOfTrailingZeros(whitePawns);
			long pawn = 1L << pawnIndex;
			whitePawns ^= pawn;
			if((this.passedPawnMasksWhite[pawnIndex] & pawnMaskBlack) == 0) {
				this.data[index].numPassedPawnsWhite++;
			}
		}
		long blackPawns = pawnMaskBlack;
		while(blackPawns != 0) {
			int pawnIndex = Long.numberOfTrailingZeros(blackPawns);
			long pawn = 1L << pawnIndex;
			blackPawns ^= pawn;
			if((this.passedPawnMasksBlack[pawnIndex] & pawnMaskWhite) == 0) {
				this.data[index].numPassedPawnsBlack++;
			}
		}
	}
	
	public PawnHashTableEntry get(long positionHash) {
		int index = this.index(positionHash);
		PawnHashTableEntry found = this.data[index];
		if(found.positionHash != positionHash) {
			return null;
		}
		return found;
	}
	
	private int index(long positionHash) {
		// Unset the sign bit.
		return ((int)positionHash & 0x7fffffff) % this.size;
	}
	
	public PawnKingHashTable(int size) {
		this.size = size;
		this.data = new PawnHashTableEntry[size];
		for(int i = 0; i < size; i++) {
			this.data[i] = new PawnHashTableEntry();
		}
		
		this.passedPawnMasksWhite = new long[64];
		this.passedPawnMasksBlack = new long[64];
		for(int i = 8; i < 56; i++) {
			this.passedPawnMasksWhite[i] = 0;
			for(int j = i; j < 56; j+=8) {
				this.passedPawnMasksWhite[i] |= (1 << j);
				if(i % 8 != 0) {
					this.passedPawnMasksWhite[i] |= (1 << (j - 1));
				}
				if(i % 8 != 7) {
					this.passedPawnMasksWhite[i] |= (1 << (j + 1));
				}
			}
			this.passedPawnMasksBlack[i] = 0;
			for(int j = i; j >= 8; j-=8) {
				this.passedPawnMasksBlack[i] |= (1 << j);
				if(i % 8 != 0) {
					this.passedPawnMasksBlack[i] |= (1 << (j - 1));
				}
				if(i % 8 != 7) {
					this.passedPawnMasksBlack[i] |= (1 << (j + 1));
				}
			}
		}
	}
	
	private int size = 0;
	
	// Memory isn't quite as important for the pawn hash table versus the
	// transposition table, so we can get away with an array of objects.
	private PawnHashTableEntry[] data;
	
	private long[] passedPawnMasksWhite;
	private long[] passedPawnMasksBlack;
}
