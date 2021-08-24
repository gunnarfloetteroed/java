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

import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class GreedoParameterManager {

	private boolean sqrtGaps = true;

	private final int skipIterations;

	private final int initialMinEvaluatedIterations;

	private final Function<Integer, Integer> trustRegionReducer;

	private final Function<Integer, Integer> replicationIncreaser;

	private int replications;

	private int trustRegion;

	private final List<double[]> allGaps = new LinkedList<>();

	private Double withinIterationVarianceOfMean = null;

	private Double betweenIterationVarianceOfMean = null;

	private int impossibleTrustRegionReductions = 0;

	private int impossibleTrustRegionReductionsInSeries = 0;

	private boolean stationarityDetected = false;
	
	public GreedoParameterManager(final int initialReplications, final int initialTrustRegion, final int skipIterations,
			final int initialMinEvaluatedIterations, final Function<Integer, Integer> trustRegionReducer,
			final Function<Integer, Integer> replicationIncreaser) {
		this.replications = initialReplications;
		this.trustRegion = initialTrustRegion;
		this.skipIterations = skipIterations;
		this.initialMinEvaluatedIterations = initialMinEvaluatedIterations;
		this.trustRegionReducer = trustRegionReducer;
		this.replicationIncreaser = replicationIncreaser;
	}

	public boolean stationarityDetected() {
		return this.stationarityDetected;
	}
	
	public int getTrustRegion() {
		return this.trustRegion;
	}

	public int getReplications() {
		return this.replications;
	}

	public Double getLastStationaryBetweenIterationVarianceOfMean() {
		return this.betweenIterationVarianceOfMean;
	}

	public Double getLastStationaryWithinIterationVarianceOfMean() {
		return this.withinIterationVarianceOfMean;
	}

	public void registerGaps(double[] newGaps) {

		this.allGaps.add(newGaps);

		this.stationarityDetected = false;
		final int minEvaluatedIterations = max(5,
				(int) round(sqrt(2.0 / this.replications) * this.initialMinEvaluatedIterations));

		if (this.allGaps.size() - this.skipIterations >= minEvaluatedIterations) {

			final double[] means = new double[this.allGaps.size() - this.skipIterations];
			final SummaryStatistics withinSampleMeanStats = new SummaryStatistics();
			final SummaryStatistics withinSampleVarStats = new SummaryStatistics();
			for (int k = this.skipIterations; k < this.allGaps.size(); k++) {
				final SummaryStatistics stats = new SummaryStatistics();
				for (double gapVal : this.allGaps.get(k)) {
					stats.addValue(this.sqrtGaps ? sqrt(gapVal) : gapVal);
				}
				means[k - this.skipIterations] = stats.getMean();
				withinSampleMeanStats.addValue(stats.getMean());
				withinSampleVarStats.addValue(stats.getVariance());
			}

			final boolean stationary = new DickeyFullerTest(means).getStationary();
			this.withinIterationVarianceOfMean = withinSampleVarStats.getMean() / this.replications;
			this.betweenIterationVarianceOfMean = withinSampleMeanStats.getVariance();

			if (stationary) {
				
				if (this.withinIterationVarianceOfMean < this.betweenIterationVarianceOfMean) {
					if (this.trustRegion > 1) {
						this.impossibleTrustRegionReductionsInSeries = 0;
						this.trustRegion = this.trustRegionReducer.apply(this.trustRegion);
					} else {
						this.impossibleTrustRegionReductions++;
						this.impossibleTrustRegionReductionsInSeries++;
						this.replications = this.replicationIncreaser.apply(this.replications);
					}
				} else {
					this.impossibleTrustRegionReductionsInSeries = 0;
					this.replications = this.replicationIncreaser.apply(this.replications);
				}

				this.stationarityDetected = true;
				this.allGaps.clear();
			}
		}
	}

	public static void main(String[] args) {

		GreedoParameterManager manager = new GreedoParameterManager(2, 8, 0, 5, tr -> max(tr / 2, 1), repl -> 2 * repl);

		TwoRoutes model = new TwoRoutes(101, 100);

		System.out.println("k\twithinVar\tbetweenVar\tTR\tR\tmeanGap\tonRoute0");

		for (int k = 0; manager.impossibleTrustRegionReductions < 3; k++) {
			model.replan(manager.trustRegion, manager.replications);
			double[] gaps = new double[manager.replications];
			for (int r = 0; r < manager.replications; r++) {
				gaps[r] = model.equilibriumGap(model.drawReplicationAveragedCosts(1));
			}
			manager.registerGaps(gaps);

			final String line = k + "\t" + manager.withinIterationVarianceOfMean + "\t"
					+ manager.betweenIterationVarianceOfMean + "\t" + manager.trustRegion + "\t" + manager.replications
					+ "\t" + Arrays.stream(gaps).average().getAsDouble() + "\t" + model.onRoute0;
			System.out.println(line.replace('.', ',').replace("null", ""));
		}
	}
}
