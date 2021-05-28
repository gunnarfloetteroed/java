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

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.signum;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class StochasticTrustRegionAdjustment {

	private final double oneSidedRangeBeforeTrafo;

	private final double oneSidedRangeAfterTrafo;

	private final double stationaryFactor;

	private double trustRegion;

	private Double secondLastGap;

	private Double lastDeltaGBeforeTrafo;

	private Double lastDeltaGAfterTrafo;

	public StochasticTrustRegionAdjustment(final double oneSidedRangeBeforeTrafo, final double oneSidedRangeAfterTrafo,
			final double stationaryFactor, final double initialTrustRegion) {
		this.oneSidedRangeBeforeTrafo = oneSidedRangeBeforeTrafo;
		this.oneSidedRangeAfterTrafo = oneSidedRangeAfterTrafo;
		this.stationaryFactor = stationaryFactor;
		this.trustRegion = initialTrustRegion;
	}

	private void update(final double lastGap) {
		if (this.secondLastGap != null) {
			this.lastDeltaGBeforeTrafo = lastGap - this.secondLastGap;
			// System.out.println(this.lastDeltaGBeforeTrafo);
			this.lastDeltaGAfterTrafo = this.lastDeltaGBeforeTrafo
					* (this.oneSidedRangeAfterTrafo / this.oneSidedRangeBeforeTrafo);
			this.lastDeltaGAfterTrafo = signum(this.lastDeltaGAfterTrafo)
					* min(this.oneSidedRangeAfterTrafo, abs(this.lastDeltaGAfterTrafo));			
			// this.trustRegion *= Math.pow(1.0 - this.lastDeltaGAfterTrafo, this.stationaryFactor);
			if (this.lastDeltaGAfterTrafo >= 0) {
				this.trustRegion *= 1.0 - 1.0 * this.lastDeltaGAfterTrafo;
			} else {
				final double m = 4.0 * ((1.0 - this.stationaryFactor) / this.oneSidedRangeAfterTrafo) - 1.0;
				this.trustRegion *= 1.0 + m * this.lastDeltaGAfterTrafo;
				
			}
		}
		this.secondLastGap = lastGap;
	}

	public static void main(String[] args) {
		Random rnd = new Random();
		StochasticTrustRegionAdjustment adj = new StochasticTrustRegionAdjustment(2.0, 0.5, 1.0, 1.0);
		System.out.println("gap\tTR");
		for (int k = 0; k < 1000; k++) {
			final double meanGap = 6.0 + 6.0 * Math.exp(-0.01 * k);
			// final double stochGap = meanGap + 0.5 * rnd.nextGaussian();
			final double stochGap = meanGap + (rnd.nextDouble() - 0.5);
			adj.update(stochGap);
			System.out.println((stochGap + "\t" + adj.trustRegion).replace('.', ','));
		}
	}
}
