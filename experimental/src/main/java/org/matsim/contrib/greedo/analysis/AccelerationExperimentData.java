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
package org.matsim.contrib.greedo.analysis;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jfree.data.xy.YIntervalSeries;

import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationExperimentData {

	// -------------------- CONSTANTS --------------------

	private final File parentFolder;

	// -------------------- MEMBERS --------------------
	
	private final Double[][] betas;
	private final Double[][] realizedLambdas;
	private final Double[][] realizedUtilities;
	private final Double[][] expectedUtilityChanges;
	// private final Double[][] performanceCorrelations;
	private final Double[][] ageCorrelations;

	private final Double[][] agePercentile10;
	private final Double[][] agePercentile20;
	private final Double[][] agePercentile30;
	private final Double[][] agePercentile40;
	private final Double[][] agePercentile50;
	private final Double[][] agePercentile60;
	private final Double[][] agePercentile70;
	private final Double[][] agePercentile80;
	private final Double[][] agePercentile90;

	// -------------------- CONSTRUCTION --------------------

	public AccelerationExperimentData(final String parentFolderName, final int firstScenarioIndex,
			final int scenarioCnt, final int iterationCnt) {

		this.parentFolder = new File(parentFolderName);
		if (!this.parentFolder.exists()) {
			throw new RuntimeException(parentFolderName + " does not exist.");
		}
		if (!this.parentFolder.isDirectory()) {
			throw new RuntimeException(parentFolderName + " is not a directory.");
		}

		this.betas = new Double[scenarioCnt][iterationCnt];
		this.realizedLambdas = new Double[scenarioCnt][iterationCnt];
		this.realizedUtilities = new Double[scenarioCnt][iterationCnt];
		this.expectedUtilityChanges = new Double[scenarioCnt][iterationCnt];
		// this.performanceCorrelations = new Double[scenarioCnt][iterationCnt];
		this.ageCorrelations = new Double[scenarioCnt][iterationCnt];
		this.agePercentile10 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile20 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile30 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile40 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile50 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile60 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile70 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile80 = new Double[scenarioCnt][iterationCnt];
		this.agePercentile90 = new Double[scenarioCnt][iterationCnt];

		for (int scenarioIndex = firstScenarioIndex; scenarioIndex < firstScenarioIndex
				+ scenarioCnt; scenarioIndex++) {
			final String scenarioFolderName = "run" + scenarioIndex;
			System.out.println("processing scenario folder: " + scenarioFolderName);

			final File logFile = FileUtils.getFile(this.parentFolder, scenarioFolderName, "output", "acceleration.log");
			TabularFileParser parser = new TabularFileParser();
			AccelerationLogHandler handler = new AccelerationLogHandler(this.betas[scenarioIndex],
					this.realizedLambdas[scenarioIndex], this.realizedUtilities[scenarioIndex],
					this.expectedUtilityChanges[scenarioIndex],
					// this.performanceCorrelations[scenarioIndex],
					this.ageCorrelations[scenarioIndex], this.agePercentile10[scenarioIndex],
					this.agePercentile20[scenarioIndex], this.agePercentile30[scenarioIndex],
					this.agePercentile40[scenarioIndex], this.agePercentile50[scenarioIndex],
					this.agePercentile60[scenarioIndex], this.agePercentile70[scenarioIndex],
					this.agePercentile80[scenarioIndex], this.agePercentile90[scenarioIndex]);
			parser.setDelimiterRegex("\\t");
			parser.setOmitEmptyColumns(false);
			try {
				parser.parse(logFile.getAbsolutePath(), handler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}
	}

	// -------------------- INTERNALS --------------------

	private double[] minAvgMaxOrNull(final Double[][] data, final int iteration) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double sum = 0;
		int cnt = 0;
		for (int run = 0; run < data.length; run++) {
			final Double val = data[run][iteration];
			if (val != null) {
				min = Math.min(min, val);
				max = Math.max(max, val);
				sum += val;
				cnt++;
			}
		}
		if (cnt == data.length) {
			return new double[] { min, sum / cnt, max };
		} else {
			return null;
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	YIntervalSeries newSeries(final String key, final Double[][] data) {
		final YIntervalSeries result = new YIntervalSeries(key);
		for (int iteration = 0; iteration < data[0].length; iteration++) {
			final double[] minAvgMax = this.minAvgMaxOrNull(data, iteration);
			if (minAvgMax != null) {
				result.add(iteration, minAvgMax[1], minAvgMax[0], minAvgMax[2]);
			}
		}
		return result;
	}

	YIntervalSeries newBetaSeries(final String key) {
		return this.newSeries(key, this.betas);
	}

	YIntervalSeries newRealizedLambdaSeries(final String key) {
		return this.newSeries(key, this.realizedLambdas);
	}

	YIntervalSeries newRealizedUtilitiesSeries(final String key) {
		return this.newSeries(key, this.realizedUtilities);
	}

	YIntervalSeries newExpectedUtilityChangesSeries(final String key) {
		return this.newSeries(key, this.expectedUtilityChanges);
	}

	// YIntervalSeries newPerformanceCorrelationSeries(final String key) {
	// return this.newSeries(key, this.performanceCorrelations);
	// }

	YIntervalSeries newAgeCorrelationSeries(final String key) {
		return this.newSeries(key, this.ageCorrelations);
	}

	YIntervalSeries newAgePercentile10Series(final String key) {
		return this.newSeries(key, this.agePercentile10);
	}

	YIntervalSeries newAgePercentile20Series(final String key) {
		return this.newSeries(key, this.agePercentile20);
	}

	YIntervalSeries newAgePercentile30Series(final String key) {
		return this.newSeries(key, this.agePercentile30);
	}

	YIntervalSeries newAgePercentile40Series(final String key) {
		return this.newSeries(key, this.agePercentile40);
	}

	YIntervalSeries newAgePercentile50Series(final String key) {
		return this.newSeries(key, this.agePercentile50);
	}

	YIntervalSeries newAgePercentile60Series(final String key) {
		return this.newSeries(key, this.agePercentile60);
	}

	YIntervalSeries newAgePercentile70Series(final String key) {
		return this.newSeries(key, this.agePercentile70);
	}

	YIntervalSeries newAgePercentile80Series(final String key) {
		return this.newSeries(key, this.agePercentile80);
	}

	YIntervalSeries newAgePercentile90Series(final String key) {
		return this.newSeries(key, this.agePercentile90);
	}

	double[] newFinalPoints(final Double[][] data) {
		final double[] result = new double[data.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = data[i][data[i].length - 1];
		}
		return result;
	}

	double[] newExpectedUtilityChangesFinalPoints() {
		return this.newFinalPoints(this.expectedUtilityChanges);
	}

}
