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
package stockholm.ihop4;

import java.nio.file.Paths;

import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.LinkEntryCountDeviationObjectiveFunction;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.LinkEntryCounter;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.CountTrajectorySummarizer;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.TrajectoryPlotter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;

import floetteroed.utilities.TimeDiscretization;
import stockholm.ihop4.IhopConfigGroup.CountResidualMagnitudeType;
import stockholm.ihop4.tollzonepassagedata.TollZoneMeasurementReader;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SimulatedTollZoneFlowAnalyzer {

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		System.out.println("remove this line to continue");
		System.exit(0);
		
		// CONFIGURE

		final boolean useAllDayData = true;
		final boolean useTollOnlyData = false;

		// final String path =
		// "/Users/GunnarF/NoBackup/data-workspace/ihop4/2019-02-06b";
		// final String path =
		// "/Users/GunnarF/NoBackup/data-workspace/ihop4/2019-02-12a";
		final String path = "/Users/GunnarF/NoBackup/data-workspace/ihop4/2019-02-15b";

		final String eventFileName = path + "/output_events.xml.gz";
		final String outputDirectory = path;

		final Config config = ConfigUtils.createConfig();
		final IhopConfigGroup ihopConfig = ConfigUtils.addOrGetModule(config, IhopConfigGroup.class);
		ihopConfig.setTollZoneCountsFolder("/Users/GunnarF/NoBackup/data-workspace/ihop4/2016-10-xx_passagedata");
		ihopConfig.setCountResidualMagnitude(CountResidualMagnitudeType.square);
		ihopConfig.setSimulatedPopulationShare(0.01);

		// LOAD REAL DATA

		final TollZoneMeasurementReader measReader = new TollZoneMeasurementReader(ihopConfig.getTollZoneCountsFolder(),
				config, 20, new TimeDiscretization(0, 1800, 48), new TimeDiscretization(6 * 3600 + 30 * 60, 1800, 24),
				1);
		measReader.run();

		// WIRE REAL DATA INTO PLOTTER

		final TrajectoryPlotter trajectoryPlotter = new TrajectoryPlotter(outputDirectory, 1); // log interval plays no
																								// role
		if (useAllDayData) {
			for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent : measReader
					.getAllDayMeasurements().getObjectiveFunctions()) {
				trajectoryPlotter.addDataSource(objectiveFunctionComponent);
			}
		}
		if (useTollOnlyData) {
			for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent : measReader
					.getOnlyTollTimeMeasurements().getObjectiveFunctions()) {
				trajectoryPlotter.addDataSource(objectiveFunctionComponent);
			}
		}
		trajectoryPlotter.addSummarizer(new CountTrajectorySummarizer(new TimeDiscretization(0, 1800, 48)));

		// PARSE EVENT FILE

		final EventsManager eventsManager = EventsUtils.createEventsManager();

		for (LinkEntryCounter handler : measReader.getAllDayMeasurements().getHandlers()) {
			eventsManager.addHandler(handler);
		}
		for (LinkEntryCounter handler : measReader.getOnlyTollTimeMeasurements().getHandlers()) {
			eventsManager.addHandler(handler);
		}

		EventsUtils.readEvents(eventsManager, eventFileName);

		for (LinkEntryCounter handler : measReader.getAllDayMeasurements().getHandlers()) {
			handler.consolidateData();
		}
		for (LinkEntryCounter handler : measReader.getOnlyTollTimeMeasurements().getHandlers()) {
			handler.consolidateData();
		}

		// AND WRITE OUT

		trajectoryPlotter.writeToFile(Paths.get(outputDirectory, "trajectories_only-all-day.txt"));

		System.out.println("... DONE");
	}

}
