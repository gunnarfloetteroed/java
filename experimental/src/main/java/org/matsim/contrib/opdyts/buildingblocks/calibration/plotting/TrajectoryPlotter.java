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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import floetteroed.utilities.Time;
import floetteroed.utilities.TimeDiscretization;

/**
 * This is an IterationEndsListener because the completion of the simulated data
 * extraction is implemented in an AfterMobsimListener. If this was also an
 * AfterMobsimListener then it appears not guaranteed that the data is completed
 * before the writing is triggered.
 *
 * @author Gunnar Flötteröd
 *
 */
public class TrajectoryPlotter implements IterationEndsListener {

	// -------------------- CONSTANTS --------------------

	private final String outputDirectory;

	private final int logInterval;

	// -------------------- MEMBERS --------------------

	private final List<TrajectoryPlotDataSource> dataSources = new ArrayList<>();

	private final List<TrajectoryDataSummarizer> summarizers = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public TrajectoryPlotter(final String outputDirectory, final int logInterval) {
		this.outputDirectory = outputDirectory;
		this.logInterval = logInterval;
	}

	public TrajectoryPlotter(final Config config, final int logInterval) {
		this(config.controler().getOutputDirectory(), logInterval);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addDataSource(final TrajectoryPlotDataSource dataSource) {
		this.dataSources.add(dataSource);
	}

	public void addSummarizer(final TrajectoryDataSummarizer summarizer) {
		this.summarizers.add(summarizer);
	}

	// --------------- IMPLEMENTATION OF AfterMobsimListener ---------------

	private void printlnData(final String label, final double[] data, final PrintWriter writer) {
		writer.print(label);
		if (data != null) {
			for (double val : data) {
				writer.print("\t");
				writer.print(val);
			}
		}
		writer.println();
	}

	private void printBlock(final TrajectoryPlotDataSource dataSource, final PrintWriter writer) {
		// 1st row: The description
		writer.println(dataSource.getIdentifier());
		// 2nd row: The time line
		final TimeDiscretization timeDiscr = dataSource.getTimeDiscretization();
		writer.print("time");
		for (int bin = 0; bin < timeDiscr.getBinCnt(); bin++) {
			writer.print("\t[");
			writer.print(Time.strFromSec(timeDiscr.getBinStartTime_s(bin)));
			writer.print(",");
			writer.print(Time.strFromSec(timeDiscr.getBinEndTime_s(bin)));
			writer.print(")");
		}
		writer.println();
		// 3rd row: The simulated data
		this.printlnData("simulated", dataSource.getSimulatedData(), writer);
		// 4th row: The real data
		this.printlnData("real", dataSource.getRealData(), writer);
		// 5th row: empty.
		writer.println();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() % this.logInterval == 0) {
			for (TrajectoryDataSummarizer summarizer : this.summarizers) {
				summarizer.clear();
			}
			final Path path = Paths.get(this.outputDirectory + "/trajectories." + event.getIteration() + ".data");
			this.writeToFile(path);
		}
	}

	public void writeToFile(final Path path) {
		try {
			final PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path));
			for (TrajectoryPlotDataSource dataSource : this.dataSources) {
				this.printBlock(dataSource, writer);
				for (TrajectoryDataSummarizer summarizer : this.summarizers) {
					summarizer.offerCandidate(dataSource);
				}
			}
			for (TrajectoryDataSummarizer summarizer : this.summarizers) {
				summarizer.build();
				this.printBlock(summarizer, writer);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			Logger.getLogger(this.getClass()).error(e);
		}
	}
}
