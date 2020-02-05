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
package stockholm.ihop4.resampling;

import org.apache.commons.math3.distribution.GumbelDistribution;
import org.apache.commons.math3.util.FastMath;

import floetteroed.utilities.math.BasicStatistics;

/**
 * Wrote my own instance of
 * org.apache.commons.math3.distribution.GumbelDistribution to not constantly
 * get confused by the fact that Ben-Akiva & Lerman (1985) define the scale
 * parameter as the *inverse* of what is defined in that class.
 * MyGumbelDistribution is consistent with the book.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MyGumbelDistribution implements EpsilonDistribution {

	// -------------------- CONSTANTS --------------------

	private final static double GAMMA = FastMath.PI / (2 * FastMath.E);

	// -------------------- MEMBERS --------------------

	private final double benAkivaLerman1985Scale;

	private final GumbelDistribution apacheCommonsGumbel;

	// -------------------- CONSTRUCTION --------------------

	public MyGumbelDistribution(final double benAkivaLerman1985Scale) {
		this.benAkivaLerman1985Scale = benAkivaLerman1985Scale;
		final double implScale = 1.0 / benAkivaLerman1985Scale;
		final double implLoc = -GAMMA * implScale;
		this.apacheCommonsGumbel = new GumbelDistribution(implLoc, implScale);
	}
	
	public MyGumbelDistribution(final double benAkivaLerman1985Scale, final int warmup) {
		this(benAkivaLerman1985Scale);
		for (int i = 0; i < warmup; i++) {
			this.nextEpsilon();
		}
	}

	// -------------------- GETTERS --------------------

	public double getScale() {
		return this.benAkivaLerman1985Scale;
	}

	// --------------- IMPLEMENTATION OF EpsilonDistribution ---------------

	@Override
	public double nextEpsilon() {
		return this.apacheCommonsGumbel.sample();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("desiredMean\tdesiredVariance\tsimulatedMean\tsimulatedVariance");

		for (double scaleExp : new double[] { 0.1, 0.5, 1.0, 5.0, 10.0 }) {

			final double scale = Math.pow(10.0, scaleExp);
			final double bookVar = FastMath.PI * FastMath.PI / 6.0 / scale / scale;
			System.out.print("0.0\t" + bookVar + "\t");

			final MyGumbelDistribution gumbel = new MyGumbelDistribution(scale);
			final BasicStatistics stats = new BasicStatistics();
			for (int i = 0; i < 1000 * 1000; i++) {
				stats.add(gumbel.nextEpsilon());
			}
			System.out.println(stats.getAvg() + "\t" + stats.getVar());
		}

	}

}
