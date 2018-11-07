package edu.bonn.cs.iv.bonnmotion.models.smooth;

public class TwoDIntWrapper {
	private int[][] data;
	private int x;
	private int y;

	public TwoDIntWrapper(int length) {
		data = new int[length][length];
		this.x = length;
		this.y = length;
	}

	public TwoDIntWrapper(int[][] data) {
		this.data = data;
		this.x = data.length;
		this.y = data[0].length;
	}

	public TwoDIntWrapper(int x, int y) {
		data = new int[x][y];
		this.x = x;
		this.y = y;
	}

	public int get(int x, int y) {
		return data[x][y];
	}

	public void set(int x, int y, int newInt) throws WrapperMaximumLengthExceededException {
		if(x>=data.length){
			throw new WrapperMaximumLengthExceededException("X index too large");
		} else if(y>=data[x].length){
			throw new WrapperMaximumLengthExceededException("Y index too large"); 
		}
		data[x][y] = newInt;
	}

	public int[][] getData() {
		return data;
	}

	public void setData(int[][] data) {
		this.data = data;
	}

	public boolean isSquare() {
		return x == y;
	}
}
