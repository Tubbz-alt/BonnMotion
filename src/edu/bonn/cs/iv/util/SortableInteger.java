package edu.bonn.cs.iv.util;

/** Dient zur Abspeicherung von int-Werten in SortedLists. */

public class SortableInteger implements Sortable {
    public final int key;
    
    public SortableInteger(int key) {
		this.key = key;
	}

    public int getKey() {
		return key;
	}
}
