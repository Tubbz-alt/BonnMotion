package edu.bonn.cs.iv.bonnmotion.models.smooth;

public class ThreeDDoubleArrayWrapper {
	private TwoDDoubleWrapper[] data;
	private int x;
	private int y;
	private int z;

	public ThreeDDoubleArrayWrapper(int length) {
		data = new TwoDDoubleWrapper[length];
		for (int i = 0; i < length; i++)
			data[i] = new TwoDDoubleWrapper(length);
		this.x = length;
		this.y = length;
		this.z = length;
	}

	public ThreeDDoubleArrayWrapper(int x, int y, int z) {
		data = new TwoDDoubleWrapper[x];
		for (int i = 0; i < x; i++)
			data[i] = new TwoDDoubleWrapper(y, z);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double get(int x, int y, int z) {
		return data[x].get(y, z);
	}

	public void set(int x, int y, int z, double newDouble) throws WrapperMaximumLengthExceededException {
		if(x>=data.length){
			throw new WrapperMaximumLengthExceededException("Top-level wrapper x index too large");
		}
		data[x].set(y, z, newDouble);
	}

	public boolean isSquare() {
		return (x == y) && (y == z);
	}
}
