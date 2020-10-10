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
package org.matsim.contrib.greedo.stationaryreplanningregulation;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 * @see https://www.expertsinuncertainty.net/Portals/60/Multimedia/Presentations/MMRM%20-%20EJ%20Workshop%20August%2014/Keming%20Yu%20-%20Bayesian%20Quantile%20Regression.pdf?ver=2017-08-08-192722-947
 */
public class AdaptiveQuantileEstimator {

	private double stepSize;

	private double proba;

	private double quantile;

	public AdaptiveQuantileEstimator(final double stepSize, final double proba, final double initialQuantileGuess) {
		this.stepSize = stepSize;
		this.proba = proba;
		this.quantile = initialQuantileGuess;
	}
	
	public void setStepSize(final double stepSize) {
		this.stepSize = stepSize;
	}

	public void update(final double realization) {

		final double dLoss_dQuantile;
		if (realization > this.quantile) {
			dLoss_dQuantile = -this.proba;
		} else {
			dLoss_dQuantile = 1.0 - this.proba;
		}

		// Step size one as we are only estimating a constant.
		this.quantile -= this.stepSize * dLoss_dQuantile;
	}
	
	public void setProbability(final double probability) {
		this.proba = probability;
	}
	
	public double getQuantile() {
		return this.quantile;
	}

	public static void main(String[] args) {
		Random rnd = new Random();
		AdaptiveQuantileEstimator aqe = new AdaptiveQuantileEstimator(0.1, 0.1359, 0.0);
		for (int i = 0; i < 100; i++) {
			double realization = rnd.nextGaussian();
			aqe.update(realization);
			System.out.println(realization + "\t" + aqe.quantile);
		}
	}
}
