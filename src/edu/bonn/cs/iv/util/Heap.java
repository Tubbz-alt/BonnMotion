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

/** Diese Klasse implementiert einen Heap, der Objekte zusammen mit einer Priorit�t abspeichert und ein Element mit niedrigster Priorit�t l�schen kann. Sowohl abspeichern als auch L�schen eines Elements sind von logarithmischer Komplexit�t. */

public class Heap {
	/** Diese Klasse dient dazu, die Objekte zusammen mit einer Priorit�t in dem Heap zu speichern. */
	class Record {
		Object data;
		double prio;
		
		public Record(Object data, double prio) {
			this.data = data;
			this.prio = prio;
		}
	}

	protected Record[] list = new Record[8];
	protected int count = 0;
	protected boolean minTop;
	
	public Heap(boolean minTop) {
		this.minTop = minTop;
	}
	
	public Heap() {
		this(true);
	}
	
	/** �ndert die Gr��e des internen Arrays.
 * 	@param nsize Neue Array-Gr��e. */
	protected void resize(int nsize) {
		Record[] nl = new Record[nsize];
		System.arraycopy(list, 0, nl, 0, count);
		list = nl;
	}

	/** Vertauschen von Elementen des internen Arrays.
 * 	@param i Erste Array-Position.
 * 	@param j Zweite Array-Position. */
	protected void swap(int i, int j) {
		Record r = list[i];
		list[i] = list[j];
		list[j] = r;
	}

	/** Position des Vaters eines Array-Elements.
 * 	@param p Position des Array-Elements. */
	protected static int father(int p) {
		return ((p + 1) / 2) - 1;
	}
	
	/** Position des linken Nachfolgers eines Array-Elements.
 * 	@param p Position des Array-Elements. */
	protected static int left(int p) {
		return (2 * (p + 1)) - 1;
	}
	
	/** Position des rechten Nachfolgers eines Array-Elements.
 * 	@param p Position des Array-Elements. */
	protected static int right(int p) {
		return 2 * (p + 1);
	}

	protected boolean smaller(double a, double b) {
		if (minTop)
			return a < b;
		else
			return a > b;
	}

	/** Stellt die Heap-Eigenschaft wieder her.
 * 	@param p Position, an der die Heap-Eigenschaft verletzt sein k�nnte. */
	protected void reheapify(int p) {
		while (true) {
			int l = left(p);
			int r = right(p);
			int s = -1;
			if (l < count)
				if (r < count)
					if (smaller(list[l].prio, list[r].prio))
						s = l;
					else
						s = r;
				else
					s = l;
			else
				s = -1;
			if (! ((s == -1) || smaller(list[p].prio, list[s].prio))) {
				swap(p, s);
				p = s;
			}
			else
				break;
		}
	}

	/** Hinzuf�gen eines Elements.
 * 	@param data Hinzuzuf�gendes Element.
 * 	@param prio Priorit�t des Elements. */
	public void add(Object data, double prio) {
		if (count == list.length)
			resize(list.length * 2);
		int p = count++;
		list[p] = new Record(data, prio);
		while ((p > 0) && (smaller(list[p].prio, list[father(p)].prio))) {
			swap(p, father(p));
			p = father(p);
		}
	}
	
	/** Liefert eine Kopie des Heaps. Die gespeicherten Objekte werden nicht gecloned; die Referenzen auf diese bleiben die gleichen.
 * 	@return Kopie des Heaps. */
	public Object clone() {
		Heap h = new Heap();
		h.list = new Record[list.length];
		System.arraycopy(list, 0, h.list, 0, count);
		h.count = count;
		return h;
	}

	/** Liefert und l�scht ein Element mit kleinster Priorit�t.
 * 	@return Gel�schtes Element. */
	public Object deleteMin() {
		return removeElementAt(0);
	}
	
	/** Liefert das Element an einer bestimmten Position der Liste.
 * 	@param pos Position. */
	public Object elementAt(int pos) {
		return list[pos].data;
	}

	public double level(int pos) {
		return list[pos].prio;
	}

	/** Liefert die kleinste Priorit�t aller gespeicherten Elemente. */
	public double minLevel() {
		return list[0].prio;
	}
	
	/** Entfernt das Element and einer bestimmten Position der Liste.
 * 	@param pos Position. */
	public Object removeElementAt(int pos) {
		Object rVal = list[pos].data;
		list[pos] = list[--count];
		reheapify(pos);
		return rVal;
	}

	/** Liefert die Anzahl der gespeicherten Elemente.
 * 	@return Anzahl der gespeicherten Elemente. */
	public int size() {
		return count;
	}
}
