package edu.bonn.cs.iv.bonnmotion.models.smooth;

public class OneDIntWrapper {
	int[] data;

	public OneDIntWrapper(int length) {
		data = new int[length];
	}

	public void set(int x, int newInt) throws WrapperMaximumLengthExceededException {
		if(x >= data.length){
			throw new WrapperMaximumLengthExceededException("Wrapper length exceeded");
		}
		data[x] = newInt;
	}

	public int get(int x) {
		return data[x];
	}
}
