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
package org.matsim.contrib.opdyts.buildingblocks.convergencecriteria;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AR1ConvergenceCriterionTest {

	@Test
	public void test() {
		// fail("Not yet implemented");
	}

	// --------------- TODO Non-unit tests (from original class) ---------------

	static Random rnd = new Random();

	static double systematicData(int k) {
		final double max = 30.0;
		return Math.min(k, max);
	}

	static double nextNoiseData(double oldNoiseData) {
		final double noiseSigma = 5.0;
		final double noiseInertia = 0.5;
		return noiseInertia * oldNoiseData + (1.0 - noiseInertia) * noiseSigma * rnd.nextGaussian();
	}

	public static void main(String[] args) {

		double e = 0;
		for (int k = 0; k < 1000; k++) {
			e = nextNoiseData(e);
		}

		AR1ConvergenceCriterion crit = new AR1ConvergenceCriterion(0.2);
		List<Double> data = new LinkedList<>();
		List<Double> noise = new LinkedList<>();
		List<Double> trend = new LinkedList<>();
		final int maxK = 1000 * 1000;
		for (int k = 0; !crit.getConverged() && k < maxK; k++) {
			final double mean = systematicData(k);
			e = nextNoiseData(e);
			data.add(mean + e);
			noise.add(e);
			trend.add(mean);
			crit.process(data);
		}

		System.out.println("data\tnoise\ttrend\tlower\tmean\tupper");
		for (int k = 0; k < data.size(); k++) {
			System.out.print(data.get(k) + "\t");
			System.out.print(noise.get(k) + "\t");
			System.out.print(trend.get(k) + "\t");
			if (crit.getConverged() && k >= crit.getConvergedSinceIteration()) {
				System.out.print(crit.getConvergedMean() - 2.0 * crit.getConvergedMeanStddev() + "\t");
				System.out.print(crit.getConvergedMean() + "\t");
				System.out.print(crit.getConvergedMean() + 2.0 * crit.getConvergedMeanStddev());
			}
			System.out.println();
		}
	}
}
