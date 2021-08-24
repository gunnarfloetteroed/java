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

import static java.lang.Math.abs;
import static java.lang.Math.pow;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestARStats {

	static double meanVar(double varEps, double a, double _K) {
		return varEps / pow(_K, 2) * (_K + 2 * a * (_K * (1 - a) - (1 - pow(a, _K))) / pow(1 - a, 2));
	}

	public static void main(String[] args) {
		for (int _K : new int[] { 10, 100, 1000 }) {
			for (double a : new double[] { 0.0, 0.5, 0.9, 1 - 1e-6 }) {
				double meanVar = 0;
				for (int r = 0; r < _K; r++) {
					for (int s = 0; s < _K; s++) {
						meanVar += pow(a, abs(r - s));
					}
				}
				meanVar /= (_K * _K);
				System.out.println("a=" + a + "\t_K=" + _K + "\tnum=" + meanVar + "\tanalyt=" + meanVar(1.0, a, _K));
			}
		}
	}

}
