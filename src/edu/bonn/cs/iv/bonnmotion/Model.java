package edu.bonn.cs.iv.bonnmotion;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface Model {
	public void write(String _name) throws FileNotFoundException, IOException;
	public String getModelName();
}
