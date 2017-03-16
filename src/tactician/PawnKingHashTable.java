package tactician;

/**
 * <p>This class is a transposition table similar to {@link TranspositionTable} that memoizes
 * positions already visited. This way if we encounter the same position through a different series
 * of moves through the depth-first search, we can retrieve the results of the calculations from
 * the previous encounter. This table is separate from {@link TranspositionTable} since the pawn
 * structure and king position tend to be relatively static throughout the game, so we can make
 * more detailed calculations related to pawn structure and king safety and retrieve them at a
 * relatively high hit rate.
 * 
 * <p>A key concept is the position hash, a Zobrist hash containing the information about pawns and
 * kings for each player. We ignore the side to move, castling rights, and the en passant target.
 * See {@link PositionHasher} for more details. Within {@link PawnHashTableEntry} we also store the
 * pawn masks and the king indices to guard against the possibility of a hash collision. For both
 * players we count the number of doubled pawns (pawns on the same file), isolated pawns (pawns
 * with no neighbors on adjacent files), and passed pawns (pawns which can promote without
 * encountering enemy pawns on the same or adjacent files).
 * 
 * @author Phil Leszczynski
 */
public class PawnKingHashTable {
	/**
	 * This class contains entries in the pawn/king transposition table. It stores the position
	 * hash as well as the pawn masks and king indices to avoid hash collisions. For each player
	 * it stores the number of doubled, isolated, and passed pawns.
	 * 
	 * @author Phil Leszczynski
	 */
	public class PawnHashTableEntry {
		/** Initializes an empty hash table entry. **/
		public PawnHashTableEntry() {
		}
		
		/**
		 * Initializes a hash table entry with a given position hash, pawn masks, and king indices.
		 * @param positionHash the Zobrist hash describing the pawns and kings
		 * @param pawnMaskWhite the 64-bit pawn bitboard mask for white
		 * @param pawnMaskBlack the 64-bit pawn bitboard mask for black
		 * @param kingIndexWhite the index of the white king, 0-63
		 * @param kingIndexBlack the index of the black king, 0-63
		 */
		public PawnHashTableEntry(long positionHash, long pawnMaskWhite, long pawnMaskBlack,
				int kingIndexWhite, int kingIndexBlack) {
			this.positionHash = positionHash;
			this.pawnMaskWhite = pawnMaskWhite;
			this.pawnMaskBlack = pawnMaskBlack;
			this.kingIndexWhite = kingIndexWhite;
			this.kingIndexBlack = kingIndexBlack;
		}
		
		/** The Zobrist hash describing the pawns and kings */
		public long positionHash = 0;
		
		/** The 64-bit pawn bitboard mask for white */
		public long pawnMaskWhite = 0;
		
		/** The 64-bit pawn bitboard mask for black */
		public long pawnMaskBlack = 0;
		
		/** The index of the white king, 0-63 */
		public int kingIndexWhite = 0;
		
		/** The index of the black king, 0-63 */
		public int kingIndexBlack = 0;
		
		/**
		 * The number of doubled pawns for white, or pawns that are on the same file as other
		 * white pawns.
		 */
		public int numDoubledPawnsWhite = 0;
		
		/**
		 * The number of doubled pawns for black, or pawns that are on the same file as other
		 * black pawns.
		 */
		public int numDoubledPawnsBlack = 0;
		
		/**
		 * The number of isolated pawns for white, or pawns that have no white neighbors on
		 * adjacent files. Note it is possible for pawns to be both doubled and isolated.
		 */
		public int numIsolatedPawnsWhite = 0;
		
		/**
		 * The number of isolated pawns for black, or pawns that have no black neighbors on
		 * adjacent files. Note it is possible for pawns to be both doubled and isolated.
		 */
		public int numIsolatedPawnsBlack = 0;
		
		/**
		 * The number of passed pawns for white, or pawns that can neither be blocked nor captured
		 * by enemy pawns on their way to promotion.
		 */
		public int numPassedPawnsWhite = 0;
		
		/**
		 * The number of passed pawns for black, or pawns that can neither be blocked nor captured
		 * by enemy pawns on their way to promotion.
		 */
		public int numPassedPawnsBlack = 0;
	}
	
	/**
	 * Inserts a record into the pawn/king hash table.
	 * @param positionHash the Zobrist hash describing the pawns and kings
	 * @param pawnMaskWhite the 64-bit pawn bitboard mask for white
	 * @param pawnMaskBlack the 64-bit pawn bitboard mask for black
	 * @param kingIndexWhite the index of the white king, 0-63
	 * @param kingIndexBlack the index of the black king, 0-63
	 */
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
	
	/**
	 * Retrieves a record from the pawn/king hash table.
	 * @param positionHash the Zobrist hash describing the pawns and kings
	 * @return the {@link PawnHashTableEntry} stored in the hash table, or null if none is found
	 */
	public PawnHashTableEntry get(long positionHash) {
		int index = this.index(positionHash);
		PawnHashTableEntry found = this.data[index];
		if(found.positionHash != positionHash) {
			return null;
		}
		return found;
	}
	
	/**
	 * Returns the array index where the position hash is found in {@link #data}.
	 * @param positionHash the Zobrist hash describing the pawns and kings
	 * @return the array index where the position hash is found
	 */
	private int index(long positionHash) {
		// Unset the sign bit.
		return ((int)positionHash & 0x7fffffff) % this.size;
	}
	
	/**
	 * Initializes a pawn/king hash table of a given size. Also precomputes the passed pawn mask
	 * tables to save computation time. The mask tables are given an index 0-63 of the pawn and
	 * return a 64-bit long mask listing the locations of enemy pawns that would prevent the given
	 * pawn from being passed.
	 * @param size the number of hash entries to store in the table
	 */
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
	
	/** The number of entries allocated to the hash table. */
	private int size = 0;
	
	/**
	 * The array holding the potential hash table entries. Note that contrary to
	 * {@link TranspositionTable} we use an array of objects rather than packed bits. This is
	 * because we use far fewer hash keys for the pawn/king table so memory is not as important.
	 */
	private PawnHashTableEntry[] data;
	
	private long[] passedPawnMasksWhite;
	private long[] passedPawnMasksBlack;
}
