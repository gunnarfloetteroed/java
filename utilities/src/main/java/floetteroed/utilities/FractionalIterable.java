/*
 * Copyright 2018 Gunnar Flötteröd
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
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package floetteroed.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FractionalIterable<T> implements Iterable<T> {

	private final Iterable<T> iterable;

	private final double fraction;

	public FractionalIterable(final Iterable<T> iterable, final double fraction) {
		this.iterable = iterable;
		this.fraction = fraction;
	}

	@Override
	public Iterator<T> iterator() {
		return new FractionalIterator<>(this.iterable.iterator(), fraction);
	}

	public static void main(String[] args) {
		final List<Integer> all = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			all.add(i);
		}
		for (double f = 0; f <= 1.0; f += 0.1) {
			double cnt = 0;
			for (Integer element : new FractionalIterable<>(all, f)) {
				cnt++;
			}
			System.out
					.println(" should be: " + f + "; is " + (cnt / all.size()));
		}
	}
}
