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
package utils;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 * @see https://www.expertsinuncertainty.net/Portals/60/Multimedia/Presentations/MMRM%20-%20EJ%20Workshop%20August%2014/Keming%20Yu%20-%20Bayesian%20Quantile%20Regression.pdf?ver=2017-08-08-192722-947
 */
public class AdaptiveQuantileEstimator {

	// -------------------- MEMBERS --------------------

	private double stepSize;

	private double probability;

	private double quantile;

	// -------------------- CONSTRUCTION --------------------

	public AdaptiveQuantileEstimator(final double stepSize, final double probability,
			final double initialQuantileGuess) {
		this.stepSize = stepSize;
		this.probability = probability;
		this.quantile = initialQuantileGuess;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setStepSize(final double stepSize) {
		this.stepSize = stepSize;
	}

	public void setProbability(final double probability) {
		this.probability = probability;
	}

	public void update(final double realization) {
		if (realization > this.quantile) {
			this.quantile += this.stepSize * this.probability;
		} else if (realization < this.quantile) {
			this.quantile -= this.stepSize * (1.0 - this.probability);
		}
	}

	public double getQuantile() {
		return this.quantile;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		Random rnd = new Random();
		AdaptiveQuantileEstimator aqe = new AdaptiveQuantileEstimator(0.1, 0.5, 0); // 1.0 - 0.1359, 0.0);
		for (int i = 0; i < 100; i++) {
			double realization = rnd.nextGaussian();
			aqe.update(realization);
			System.out.println(realization + "\t" + aqe.quantile);
		}
	}
}
