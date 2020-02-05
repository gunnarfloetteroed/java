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
package org.matsim.contrib.opdyts.buildingblocks.calibration.plotting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.matsim.contrib.opdyts.buildingblocks.utils.DiscretizationChanger;
import org.matsim.contrib.opdyts.buildingblocks.utils.DiscretizationChanger.DataType;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CountTrajectorySummarizer implements TrajectoryDataSummarizer {

	// -------------------- CONSTANTS --------------------

	public static final String COUNT_DATA_TYPE = "counts";

	private final TimeDiscretization timeDiscr;

	// -------------------- MEMBERS --------------------

	private final List<TrajectoryPlotDataSource> dataSources = new ArrayList<>();

	private Predicate<TrajectoryPlotDataSource> dataSourcePredicate = null;

	private double[] simData = null;

	private double[] realData = null;

	// -------------------- CONSTRUCTION --------------------

	public CountTrajectorySummarizer(final TimeDiscretization timeDiscr) {
		this.timeDiscr = timeDiscr;
		this.setDataSourcePredicate(
				(TrajectoryPlotDataSource dataSource) -> COUNT_DATA_TYPE.equals(dataSource.getDataType()));
	}

	// --------------- IMPLEMENTATION OF TrajectoryDataSummarizer ---------------

	public void clear() {
		this.dataSources.clear();
		this.simData = null;
		this.realData = null;
	}

	public void setDataSourcePredicate(final Predicate<TrajectoryPlotDataSource> candidatePredicate) {
		this.dataSourcePredicate = candidatePredicate;
	}

	public void offerCandidate(final TrajectoryPlotDataSource dataSource) {
		// if (COUNT_DATA_TYPE.equals(dataSource.getDataType())) {
		if (this.dataSourcePredicate.test(dataSource)) {
			this.dataSources.add(dataSource);
		}
	}

	public void build() {

		this.realData = new double[this.timeDiscr.getBinCnt()];
		this.simData = new double[this.timeDiscr.getBinCnt()];

		for (TrajectoryPlotDataSource dataSource : this.dataSources) {

			final double[] newRealData;
			{
				final DiscretizationChanger realDataDiscrChanger = new DiscretizationChanger(
						dataSource.getTimeDiscretization(), dataSource.getRealData(), DataType.TOTALS);
				realDataDiscrChanger.run(this.getTimeDiscretization());
				newRealData = realDataDiscrChanger.getToTotalsCopy();
			}

			final double[] newSimData;
			{
				final DiscretizationChanger simDataDiscrChanger = new DiscretizationChanger(
						dataSource.getTimeDiscretization(), dataSource.getSimulatedData(), DataType.TOTALS);
				simDataDiscrChanger.run(this.getTimeDiscretization());
				newSimData = simDataDiscrChanger.getToTotalsCopy();
			}

			for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
				this.realData[bin] += newRealData[bin];
				this.simData[bin] += newSimData[bin];
			}
		}
	}

	@Override
	public String getIdentifier() {
		return "Trajectory data summary of data type " + this.getDataType();
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return this.timeDiscr;
	}

	@Override
	public double[] getSimulatedData() {
		return this.simData;
	}

	@Override
	public double[] getRealData() {
		return this.realData;
	}

	@Override
	public String getDataType() {
		return COUNT_DATA_TYPE;
	}

}
