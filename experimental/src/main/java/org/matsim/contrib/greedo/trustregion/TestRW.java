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
package org.matsim.contrib.greedo.trustregion;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestRW {

	public static void main(String[] args) {

		Random rnd = new Random();

		double x = 1.0;
		double y = 0.0;

		Double prev = null;
		for (int k = 0; k < 1000; k++) {
			final double e = rnd.nextGaussian() + 10;
			if (prev != null) {
				final double delta = e - prev;
				x *= Math.exp(-delta);
				y += delta;
				System.out.println((x + "\t" + y).replace('.', ','));
			}
			prev = e;
		}

	}

}
