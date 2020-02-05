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

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.TrajectoryPlotDataSource;
import org.matsim.contrib.opdyts.buildingblocks.utils.DiscretizationChanger;
import org.matsim.contrib.opdyts.buildingblocks.utils.DiscretizationChanger.DataType;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TotalDeviationObjectiveFunctionEXPERIMENTAL implements MATSimObjectiveFunction<MATSimState> {

	private final TimeDiscretization timeDiscr;

	private final List<TrajectoryPlotDataSource> dataSources = new ArrayList<>();

	public TotalDeviationObjectiveFunctionEXPERIMENTAL(final TimeDiscretization timeDiscr) {
		this.timeDiscr = timeDiscr;
	}

	public void addDataSource(TrajectoryPlotDataSource dataSource) {
		this.dataSources.add(dataSource);
	}

	@Override
	public double value(MATSimState state) {

		final double[] realData = new double[this.timeDiscr.getBinCnt()];
		final double[] simData = new double[this.timeDiscr.getBinCnt()];

		for (TrajectoryPlotDataSource dataSource : this.dataSources) {

			final double[] newRealData;
			{
				final DiscretizationChanger realDataDiscrChanger = new DiscretizationChanger(
						dataSource.getTimeDiscretization(), dataSource.getRealData(), DataType.TOTALS);
				realDataDiscrChanger.run(this.timeDiscr);
				newRealData = realDataDiscrChanger.getToTotalsCopy();
			}

			final double[] newSimData;
			{
				final DiscretizationChanger simDataDiscrChanger = new DiscretizationChanger(
						dataSource.getTimeDiscretization(), dataSource.getSimulatedData(), DataType.TOTALS);
				simDataDiscrChanger.run(this.timeDiscr);
				newSimData = simDataDiscrChanger.getToTotalsCopy();
			}

			for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
				realData[bin] += newRealData[bin];
				simData[bin] += newSimData[bin];
			}
		}

		double numerator = 0;
		double denominator = 0;
		for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
			numerator += Math.pow(simData[bin] - realData[bin], 2.0);
			denominator += Math.pow(realData[bin], 2.0);
		}
		return numerator / denominator;
	}
}
