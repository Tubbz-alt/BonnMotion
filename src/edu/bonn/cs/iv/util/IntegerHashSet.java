package edu.bonn.cs.iv.util;

import java.util.HashSet;
import java.util.Iterator;
import java.lang.Integer;


/** Diese Klasse implementiert ein HashSet, die beim contains die als Schlssel Integer verwendet. */

public class IntegerHashSet extends HashSet {
	private static final long serialVersionUID = -8808140114646027111L;

	public boolean contains(Integer value){
		Iterator it = this.iterator();
		while(it.hasNext()){
			Integer entry = (Integer)it.next();
			if(value.intValue() == entry.intValue()){
				System.out.println("schon drin " + value);
				return true;
			}
		}
		return false;
	}
}
