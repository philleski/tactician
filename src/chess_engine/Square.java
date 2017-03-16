package chess_engine;

public class Square {
	public Square(String name) {
		int file = Integer.parseInt("" + (name.charAt(0) - 96));
		int rank = Integer.parseInt(name.substring(1, 2));
		this.index = (file - 1) + 8 * (rank - 1);
	}
	
	public Square(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public long getMask() {
		return 1L << this.getIndex();
	}
	
	public char getFile() {
		return (char) (this.index % 8 + 97);
	}
	
	public char getRank() {
		return Integer.toString((this.index / 8) + 1).charAt(0);
	}
	
	public String getName() {
		return "" + this.getFile() + this.getRank();
	}
	
	private int index;
}
