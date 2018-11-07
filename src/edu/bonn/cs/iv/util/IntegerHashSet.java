package edu.bonn.cs.iv.util;

import java.util.HashSet;
import java.util.Iterator;
import java.lang.Integer;

public class IntegerHashSet extends HashSet<Integer> {
	private static final long serialVersionUID = -8808140114646027111L;

	public boolean contains(Integer value){
		Iterator<Integer> it = this.iterator();
		while(it.hasNext()){
			Integer entry = it.next();
			if(value.intValue() == entry.intValue()) return true;
		}
		return false;
	}
}
