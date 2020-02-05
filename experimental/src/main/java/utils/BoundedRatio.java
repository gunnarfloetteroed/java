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
package utils;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class BoundedRatio {

	private final double absEps;

	private final double relEps;

	public BoundedRatio(final double absEps, final double relEps) {
		this.absEps = absEps;
		this.relEps = relEps;
	}

	private static final double max(final double a, final double b, final double c) {
		return Math.max(a, Math.max(b, c));
	}

	public double ratio(double num, double den) {

		double sgn = Math.signum(num) * Math.signum(den);

		num = Math.abs(num);
		den = Math.abs(den);

		num = max(num, this.absEps, this.relEps * den);
		den = max(den, this.absEps, this.relEps * num);

		return (sgn * num / den);
	}

	public static void main(String[] args) {

		final Random rnd = new Random();
		final BoundedRatio br = new BoundedRatio(0.001, 0.01);

		for (int i = 0; i < 1000; i++) {
			final double num = rnd.nextGaussian();
			final double den = rnd.nextGaussian();

			final double unboundedRatio = num / den;
			final double boundedRatio = br.ratio(num, den);

			if (unboundedRatio != boundedRatio) {
				System.out.println(num + "\t/\t " + den + "\t=\t" + unboundedRatio + "\t->\t" + boundedRatio);
			}

		}

	}

}
