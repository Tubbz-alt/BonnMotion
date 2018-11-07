/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2005 University of Bonn                                **
 **                                                                           **
 ** This program is free software; you can redistribute it and/or modify      **
 ** it under the terms of the GNU General Public License as published by      **
 ** the Free Software Foundation; either version 2 of the License, or         **
 ** (at your option) any later version.                                       **
 **                                                                           **
 ** This program is distributed in the hope that it will be useful,           **
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of            **
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             **
 ** GNU General Public License for more details.                              **
 **                                                                           **
 ** You should have received a copy of the GNU General Public License         **
 ** along with this program; if not, write to the Free Software               **
 ** Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA **
 *******************************************************************************/

package edu.bonn.cs.iv.util;

/** Diese Klasse dient der effizienten Verwaltung von Objekten, die einen int-Schlüssel besitzen. Sie werden sortiert in einem Array abgespeichert, so daß über die Schlüssel in logarithmischer Zeit auf die Objekte zugegriffen werden kann. Das Hinzufügen und das Löschen eines Elements sind wegen des Verschiebens von Array-Elementen von linearer Komplexität. */

public class SortedList {
    protected Sortable[] list;
    protected int count;
    
	public SortedList() {
		list = new Sortable[8];
	}

    protected static int findPosHelper(Sortable[] list, int begin, int end, int key) {
        while (begin <= end) {
            int mid = (begin + end) / 2;
            int n = list[mid].getKey();
            if (key == n)
                return mid;
            else
                if (key < n)
                    end = mid - 1;
                else
                    begin = mid + 1;
        }
        return end;
    }

	/** Liefert die Position eines Elements.
 * 	@param key Schlüssel des Elements.
 * 	@return Position des Elements; falls nicht vorhanden, Position, _hinter_ der das Element stehen müßte. */
    protected int findPos(int key) {
        return findPosHelper(list, 0, count - 1, key);
    }
	
	/** Hinzufügen eines Elements.
 * 	@param e Hinzuzufügendes Element.
 * 	@return Position, an der das Element eingefügt wurde, bzw. -1, falls Schlüssel schon vorhanden. */
    public int add(Sortable e) {
        Sortable[] src = list;
		Sortable[] dst;
        if (count == list.length)
            dst = new Sortable[list.length * 2];
        else
            dst = list;
        int p = findPos(e.getKey());
        if ((p >= 0) && (p < list.length) && (list[p].getKey() == e.getKey()))
            return -1;
        p++;
		if (src != dst)
	        System.arraycopy(src, 0, dst, 0, p);
        System.arraycopy(src, p, dst, p + 1, count - p);
        dst[p] = e;
        list = dst;
        count++;
		return p;
    }

	/** Liefert eine Kopie der Liste. Die gespeicherten Objekte werden nicht gecloned; die Referenzen auf diese bleiben die gleichen.
 * 	@return Kopie der Liste. */
	public Object clone() {
		SortedList s = new SortedList();
		s.list = new Sortable[list.length];
		System.arraycopy(list, 0, s.list, 0, count);
		s.count = count;
		return s;
	}
    
	/** Löscht ein Element.
 * 	@param key Schlüssel des zu löschenden Elements.
 * 	@return Gelöschtes Element. */
    public Sortable delete(int key) {
        int p = indexOf(key);
        if (p == -1)
            return null;
		return deleteElementAt(p);
    }
	
	/** Löscht eine Liste von Elementen.
 * 	@param l Liste mit Elementen, die gelöscht werden sollen. */
	public void delete(SortedList l) {
		for (int i = 0; i < l.size(); i++) {
			Sortable e = l.elementAt(i);
			int idx = indexOf(e.getKey());
			if (idx >= 0)
				deleteElementAt(idx);
		}
	}

	public void deleteAllElements() {
		count = 0;
	}

	/** Löscht das Element an einer bestimmten Position.
 * 	@param p Position des zu löschenden Elements.
 * 	@return Gelöschtes Element (oder null, falls Position nicht belegt). */
	public Sortable deleteElementAt(int p) {
		if (p >= count)
			return null;
		Sortable rVal = list[p];
        Sortable[] src = list;
		Sortable[] dst;
        if ((count < list.length / 4) && (count > 8))
            dst = new Sortable[list.length / 2];
        else
            dst = list;
		if (src != dst)
			System.arraycopy(src, 0, dst, 0, p);
        System.arraycopy(src, p + 1, dst, p, count - p - 1);
		list = dst;
        count--;
		return rVal;
	}
	
	/** Liefert Element einer bestimmten Position.
 * 	@param p Position des gesuchten Elements.
 * 	@return Gesuchtes Element. */
	public Sortable elementAt(int p) {
		if ((p >= 0) && (p < count))
			return list[p];
		else
			return null;
	}
    
	/** Liefert ein gespeichertes Elements.
 * 	@param key Schlüssel des gesuchten Elements.
 * 	@return Das gesuchte Element, bzw. null, falls nicht vorhanden. */
	public Sortable get(int key) {
		int p = indexOf(key);
        if (p == -1)
            return null;
		else
			return list[p];
	}

	/** Liefert die Position eines bestimmten Elements.
 * 	@param p Schlüssel des Elements.
 * 	@return Position des Elements, bzw. -1, falls Element nicht vorhanden. */
	public int indexOf(int key) {
        int p = findPos(key);
        if ((p == -1) || (list[p].getKey() != key))
            return -1;
		else
			return p;
	}
	
	/** Fügt eine Liste von Elementen hinzu.
 * 	@param l Liste mit Elementen, die hinzugefügt werden sollen. */	
	public void merge(SortedList l) {
		int size = list.length;
		while (size < count + l.count)
			size *= 2;
		Sortable[] dst = new Sortable[size];
		int i = 0, j = 0, p = 0;
		while ((i < count) && (j < l.count))
			if (list[i].getKey() < l.list[j].getKey())
				dst[p++] = list[i++];
			else
				dst[p++] = l.list[j++];
		if (i < count)
			System.arraycopy(list, i, dst, p, count - i);
		else
			System.arraycopy(l.list, j, dst, p, l.count - j);
		list = dst;
		count += l.count;
	}

	/** Liefert die Anzahl der gespeicherten Elemente.
 * 	@return Anzahl der gespeicherten Elemente. */
	public int size() {
		return count;
	}

/*	public int matchingKeys(SortedList l) {
        int begin = 0;
        int end = l.count - 1;
        int match = 0;
        for (int i = 0; i < count; i++) {
            int key = list[i].getKey();
            int p = findPosHelper(l.list, begin, end, key);
            if (p >= 0) {
                begin = p;
                if (l.list[p].getKey() == key)
                    match++;
            }
        }
		return match;
	} */

	public String toString() {
		String s = "";
		for (int i = 0; i < count; i++)
			s += "[" + list[i].getKey() + "]";
		return s;
	}
}
