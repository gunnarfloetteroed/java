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
package stockholm.ihop4.tollzonepassagedata;

import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.LinkEntryCountDeviationObjectiveFunction;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.LinkEntryCounter;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.CountTrajectorySummarizer;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.TrajectoryDataSummarizer;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.TrajectoryPlotter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

import floetteroed.utilities.TimeDiscretization;
import stockholm.ihop4.IhopConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TollZoneMeasurementAnalyzer {

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		final Config config = ConfigUtils.loadConfig(args[0]);
		final TrajectoryPlotter trajectoryPlotter = new TrajectoryPlotter(config, 1);

		// final TollZoneMeasurementReader measReader = new
		// TollZoneMeasurementReader(config);
		final TollZoneMeasurementReader measReader = new TollZoneMeasurementReader(
				ConfigUtils.addOrGetModule(config, IhopConfigGroup.class).getTollZoneCountsFolder(), config, 20,
				new TimeDiscretization(0, 1800, 48), new TimeDiscretization(6 * 3600 + 30 * 60, 1800, 24), 1);

		measReader.run();
		for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent : measReader.getAllDayMeasurements()
				.getObjectiveFunctions()) {
			trajectoryPlotter.addDataSource(objectiveFunctionComponent);
		}
		for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent : measReader
				.getOnlyTollTimeMeasurements().getObjectiveFunctions()) {
			trajectoryPlotter.addDataSource(objectiveFunctionComponent);
		}

		{
			Set<String> ingoingLinks = new LinkedHashSet<>(
					TollZonePassageDataSpecification.ingoingChargingPoint2link.values());
			TrajectoryDataSummarizer ingoingSummarizer = new CountTrajectorySummarizer(
					new TimeDiscretization(0, 1800, 48));
			ingoingSummarizer.setDataSourcePredicate(dataSource -> ingoingLinks.contains(dataSource.getIdentifier()));
			trajectoryPlotter.addSummarizer(ingoingSummarizer);
		}

		{
			Set<String> outgoingLinks = new LinkedHashSet<>(
					TollZonePassageDataSpecification.outgoingChargingPoint2link.values());
			TrajectoryDataSummarizer outgoingSummarizer = new CountTrajectorySummarizer(
					new TimeDiscretization(0, 1800, 48));
			outgoingSummarizer.setDataSourcePredicate(dataSource -> outgoingLinks.contains(dataSource.getIdentifier()));
			trajectoryPlotter.addSummarizer(outgoingSummarizer);
		}

		// >>> EVENTS PARSING >>>

		final String path = "/Users/GunnarF/NoBackup/data-workspace/ihop4/2018-12-06/it.2000/";
		final String eventsFileName = path + "2000.events.xml.gz";

		final EventsManager events = EventsUtils.createEventsManager(config);

		for (EventHandler handler : measReader.getAllDayMeasurements().getHandlers()) {
			events.addHandler(handler);
		}
		for (EventHandler handler : measReader.getOnlyTollTimeMeasurements().getHandlers()) {
			events.addHandler(handler);
		}

		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);

		for (LinkEntryCounter handler : measReader.getAllDayMeasurements().getHandlers()) {
			handler.notifyAfterMobsim(new AfterMobsimEvent(null, 0));
		}
		for (LinkEntryCounter handler : measReader.getOnlyTollTimeMeasurements().getHandlers()) {
			handler.notifyAfterMobsim(new AfterMobsimEvent(null, 0));
		}

		// <<< EVENTS PARSING <<<

		trajectoryPlotter.writeToFile(Paths.get("analysis.plot"));

		System.out.println("... DONE");
	}
}
