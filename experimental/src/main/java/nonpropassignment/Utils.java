/*
 * Copyright 2020 Gunnar Flötteröd
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
package nonpropassignment;

import java.util.List;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Utils {

	static double sum(final double[] a) {
		double sum = 0;
		for (double addend : a) {
			sum += addend;
		}
		return sum;
	}

	static void mult(final double[] a, final double fact) {
		for (int i = 0; i < a.length; i++) {
			a[i] *= fact;
		}
	}

	static double innerProduct(final double[] a, final double b[]) {
		double result = a[0] * b[0];
		for (int i = 1; i < a.length; i++) {
			result += a[i] * b[i];
		}
		return result;
	}

	static double innerProductWithIndicatorIndices(final double[] a, final int indicatorIndices[]) {
		double result = 0;
		for (int indicatorIndex : indicatorIndices) {
			result += a[indicatorIndex];
		}
		return result;
	}

	static void add(final double[] to, final double[] addend, final double weight) {
		for (int i = 0; i < to.length; i++) {
			to[i] += weight * addend[i];
		}
	}

	static void addIndicatorIndices(final double[] to, final int[] indicatorIndices, final double weight) {
		for (int indicatorIndex : indicatorIndices) {
			to[indicatorIndex] += weight;
		}
	}

	static double[] toArray(final List<Double> list) {
		double[] array = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}
}
