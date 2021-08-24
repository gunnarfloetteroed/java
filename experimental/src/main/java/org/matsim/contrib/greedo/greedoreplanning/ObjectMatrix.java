/*
 * Copyright 2021 Gunnar Flötteröd
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
package org.matsim.contrib.greedo.greedoreplanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ObjectMatrix<E> {

	private final List<List<E>> data;

	public ObjectMatrix(final int rows, final int cols) {
		this.data = new ArrayList<>(rows);
		for (int row = 0; row < rows; row++) {
			this.data.add(new ArrayList<>(Collections.nCopies(cols, null)));
		}
	}

	public ObjectMatrix(final int rowsAndCols) {
		this(rowsAndCols, rowsAndCols);
	}

	public void set(final int row, final int col, final E value) {
		this.data.get(row).set(col, value);
	}

	public E get(final int row, final int col) {
		return this.data.get(row).get(col);
	}

	public E getOrDefault(final int row, final int col, final E defaultVal) {
		final E currentVal = this.get(row, col);
		if (currentVal == null) {
			return defaultVal;
		} else {
			return currentVal;
		}
	}

	public static void main(String[] args) {
		ObjectMatrix<Double> m = new ObjectMatrix<Double>(2, 4);
		System.out.println(m.get(1, 3));
		System.out.println(m.getOrDefault(1, 3, 0.0));
		m.set(1, 3, 1.0);
		System.out.println(m.get(1, 3));
		System.out.println(m.getOrDefault(1, 3, 0.0));

	}
}
