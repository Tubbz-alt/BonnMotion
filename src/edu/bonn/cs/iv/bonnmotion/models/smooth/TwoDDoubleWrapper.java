package edu.bonn.cs.iv.bonnmotion.models.smooth;

public class TwoDDoubleWrapper {
	private double[][] data;
	private int x;
	private int y;

	public TwoDDoubleWrapper(int length) {
		data = new double[length][length];
		this.x = length;
		this.y = length;
	}

	public TwoDDoubleWrapper(int x, int y) {
		data = new double[x][y];
		this.x = x;
		this.y = y;
	}

	public double get(int x, int y) {
		return data[x][y];
	}

	public void set(int x, int y, double newDouble) throws WrapperMaximumLengthExceededException {
		if(x>=data.length){
			throw new WrapperMaximumLengthExceededException("X index too large");
		} else if(y>=data[x].length){
			throw new WrapperMaximumLengthExceededException("Y index too large"); 
		}
		data[x][y] = newDouble;
	}

	public double[][] getData() {
		return data;
	}

	public void setData(double[][] data) {
		this.data = data;
	}

	public boolean isSquare() {
		return x == y;
	}
}
