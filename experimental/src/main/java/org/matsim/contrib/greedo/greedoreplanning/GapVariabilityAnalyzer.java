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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class GapVariabilityAnalyzer {

	// -------------------- CONSTANTS --------------------

	private int samplesPerIteration;

	// -------------------- MEMBERS --------------------

	private final SummaryStatistics meanGapOverIterationsStats = new SummaryStatistics();

	private final SummaryStatistics gapVarianceOverIterationsStats = new SummaryStatistics();

	private final List<double[]> realizedGaps = new LinkedList<>();

	private final List<SummaryStatistics> gapStats = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	public GapVariabilityAnalyzer(final int samplesPerIteration) {
		this.samplesPerIteration = samplesPerIteration;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateIteration(final double[] realizedGaps) {
		if (realizedGaps.length != this.samplesPerIteration) {
			throw new IllegalArgumentException("wrong sample size");
		}

		this.realizedGaps.add(realizedGaps);

		final SummaryStatistics withinIterationStats = new SummaryStatistics();
		for (double gapVal : realizedGaps) {
			withinIterationStats.addValue(gapVal);
		}
		this.gapStats.add(withinIterationStats);

		this.meanGapOverIterationsStats.addValue(withinIterationStats.getMean());
		this.gapVarianceOverIterationsStats.addValue(withinIterationStats.getVariance());
	}

	public double estimateWithinIterationVariance() {
		return this.gapVarianceOverIterationsStats.getMean() / this.samplesPerIteration;
	}

	public double estimateBetweenIterationVariance() {
		return this.meanGapOverIterationsStats.getVariance() - this.estimateWithinIterationVariance();
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		Random rnd = new Random();
		int _R = 2;
		int _K = 1000;
		double betweenStddev = 1.0;
		double withinStddev = 1.0;
		
		System.out.println("withinIteration\tbetweenIteration");
		GapVariabilityAnalyzer gva = new GapVariabilityAnalyzer(_R);
		for (int k = 0; k < _K; k++) {
			double[] data = new double[_R];
			double mean = betweenStddev * rnd.nextGaussian();
			for (int r = 0; r < _R; r++) {
				data[r] = mean + withinStddev * rnd.nextGaussian();
			}
			gva.updateIteration(data);
			System.out.println(
					(gva.estimateWithinIterationVariance() + "\t" + 
			gva.estimateBetweenIterationVariance()).replace('.', ','));
		}
	}

}
