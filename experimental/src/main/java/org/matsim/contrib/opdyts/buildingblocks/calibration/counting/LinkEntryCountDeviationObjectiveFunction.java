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
package org.matsim.contrib.opdyts.buildingblocks.calibration.counting;

import java.util.function.Function;
import java.util.function.Supplier;

import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.CountTrajectorySummarizer;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.TrajectoryPlotDataSource;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkEntryCountDeviationObjectiveFunction
		implements MATSimObjectiveFunction<MATSimState>, TrajectoryPlotDataSource {

	// -------------------- MEMBERS --------------------

	private final double[] realData;

	private final LinkEntryCounter simulationCounter;

	private final Function<Double, Double> residualMagnitude;

	private final Supplier<Double> simulatedPopulationShare;

	private int startBin; // inclusive
	private int endBin; // exclusive

	// -------------------- CONSTRUCTION --------------------

	public LinkEntryCountDeviationObjectiveFunction(final double[] realData, final LinkEntryCounter simulationCounter,
			final Function<Double, Double> residualEvaluator, final Supplier<Double> simulatedPopulationShare) {
		this.realData = realData;
		this.simulationCounter = simulationCounter;
		this.residualMagnitude = residualEvaluator;
		this.simulatedPopulationShare = simulatedPopulationShare;

		this.setStartEndBin(0, realData.length);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setStartEndBin(final int startBin_inclusive, final int endBin_exclusive) {
		this.startBin = startBin_inclusive;
		this.endBin = endBin_exclusive;
	}

	public int getStartBin() {
		return this.startBin;
	}
	
	public int getEndBin() {
		return this.endBin;
	}
	
	public CountMeasurementSpecification getSpecification() {
		return this.simulationCounter.getSpecification();
	}

	public void consolidateData() {
		this.simulationCounter.consolidateData();
	}
	
	// -------------------- IMPLEMENTATION OF ObjectiveFunction --------------------

	@Override
	public double value(final MATSimState state) {
		final int[] simData = this.simulationCounter.getDataOfLastExtractedIteration();
		double result = 0;
		// for (int i = 0; i < this.realData.length; i++) {
		for (int i = this.startBin; i < this.endBin; i++) {
			final double residual = this.realData[i] - simData[i] / this.simulatedPopulationShare.get();
			result += this.residualMagnitude.apply(residual);
		}
		return result;
	}

	// --------------- IMPLEMENTATION OF TrajectoryPlotDataSource ---------------

	@Override
	public String getIdentifier() {
		return this.simulationCounter.getSpecification().getLinks().iterator().next().toString();
		// final Set<Id<Link>> linkIds =
		// this.simulationCounter.getSpecification().getLinks();
		// return "Traffic counts on link" + (linkIds.size() > 1 ? "s " : " ") +
		// linkIds;
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return this.simulationCounter.getSpecification().getTimeDiscretization();
	}

	@Override
	public double[] getSimulatedData() {
		final int[] source = this.simulationCounter.getDataOfLastExtractedIteration();
		if (source == null) {
			return null;
		} else {
			final double[] result = new double[source.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = source[i] / this.simulatedPopulationShare.get();
			}
			return result;
		}
	}

	@Override
	public double[] getRealData() {
		return this.realData;
	}

	@Override
	public String getDataType() {
		return CountTrajectorySummarizer.COUNT_DATA_TYPE;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("LINKS: " + this.getSpecification().getLinks() + "\n");
		result.append("TIMES: " + this.getSpecification().getTimeDiscretization() + "\n");
		result.append("real: ");
		for (double val : this.realData) {
			result.append("\t" + val);
		}
		result.append("\n");
		result.append("simu: ");
		for (int val : this.simulationCounter.getDataOfLastExtractedIteration()) {
			result.append("\t" + val / this.simulatedPopulationShare.get());
		}
		result.append("\n");
		return result.toString();
	}
}
