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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class StochasticTrustRegionController {

	private final double gamma;

	private final Random rnd;

	private double radius;

	private double improvementCnt = 0.0;
	private double deteriorationCnt = 0.0;

	public StochasticTrustRegionController(double gamma, double initialRadius, Random rnd) {
		this.gamma = gamma;
		this.rnd = rnd;
		this.radius = initialRadius;
	}

	public void update(double increase, double innoWeight) {

		this.improvementCnt = (1.0 - innoWeight) * this.improvementCnt + ((increase > 0.0) ? innoWeight : 0.0);
		this.deteriorationCnt = (1.0 - innoWeight) * this.deteriorationCnt + ((increase > 0.0) ? 0.0 : innoWeight);
		final double totalCnt = this.improvementCnt + this.deteriorationCnt;
		final double improvementProba = max(0.0, min(1.0, this.improvementCnt / totalCnt));

		final double changeProba = (1.0 / improvementProba) * (this.gamma / (1.0 + this.gamma));
		
		if (this.rnd.nextDouble() < changeProba) {
			if (increase > 0) {
				this.radius *= (0.9 / this.gamma);
			} else {
				this.radius *= this.gamma;
			}
		}
	}
	
	public double getRadius() {
		return this.radius;
	}

	public static void main(String[] args) {

		Random rnd = new Random();
		StochasticTrustRegionController trc = new StochasticTrustRegionController(0.8, 5, rnd);
		
		for (int it = 0; it < 1000; it++) {
			double val = rnd.nextGaussian() + (it < 500 ? 0.5 : 0.0);
			trc.update(val, Math.pow(1.0 / (1.0 + it), 0.5));
			System.out.println(trc.getRadius());
		}
		
		
		
	}
}
