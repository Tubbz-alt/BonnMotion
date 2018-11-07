package edu.bonn.cs.iv.bonnmotion.models.smooth;

public class OneDDoubleWrapper {
	double[] data;

	public OneDDoubleWrapper(int length) {
		data = new double[length];
	}

	public void set(int x, double newDouble) throws WrapperMaximumLengthExceededException {
		if(x >= data.length){
			throw new WrapperMaximumLengthExceededException("Wrapper length exceeded");
		}
		data[x] = newDouble;
	}

	public double get(int x) {
		return data[x];
	}
}
