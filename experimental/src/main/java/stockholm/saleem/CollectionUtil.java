/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */ 
package stockholm.saleem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * A class to convert a list to array.
 * 
 * @author Mohammad Saleem
 *
 */
public class CollectionUtil<Type> {
	public ArrayList<Type> toArrayList(Iterator<Type> iter){
		ArrayList<Type> arraylist = new ArrayList<Type>();
		while(iter.hasNext()){
			arraylist.add(iter.next());
		}
		return arraylist;
	}
	public double[] toArray(List<Double> alist){//Converting a list to array
		double[] array = new double[alist.size()];
		Iterator<Double> iter = alist.iterator();
		int i=0;
		while(iter.hasNext()){
			array[i]=iter.next();
			i++;
		}
		return array;
	}
}
