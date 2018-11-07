/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2004 University of Bonn                                **
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

import java.util.HashMap;
import edu.bonn.cs.iv.bonnmotion.Position;
import java.util.Map;
import java.util.Iterator;


/** Diese Klasse implementiert eine HashMap, die beim get die equals Funktion der Klasse Position verwendet. */

public class PositionHashMap extends HashMap {
	private static final long serialVersionUID = 6541722900508992094L;

	public Object get(Position key){
		Iterator it = this.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry)it.next();
			Position k = ((Position)entry.getKey());
			//System.out.println("k " + k.x + " " + k.y);
			//System.out.println("key " + key.x + " " + key.y);
			if(key.equals(k)){
				return entry.getValue();
			}
		}
		return super.get(key);
	}
	
	public void changeto(Position key, Object value){
		Iterator it = this.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry)it.next();
			Position k = ((Position)entry.getKey());
			if(key.equals(k)){
		//		System.out.println("entferne key " + k.x + " " + k.y);
				super.remove(k);
				break;
			}
		}
		//System.out.println("fuege ein key " + key.x + " " + key.y + " value " + ((Double)value).doubleValue());
		//System.out.println("fuege ein key " + key.x + " " + key.y);
		super.put(key, value);
	}
}
