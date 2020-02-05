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
package stockholm.ihop2.transmodeler.run;

import static stockholm.ihop2.transmodeler.run.TransmodelerMATSim.EVENTS_ELEMENT;
import static stockholm.ihop2.transmodeler.run.TransmodelerMATSim.LINKATTRIBUTE_FILENAME_ELEMENT;
import static stockholm.ihop2.transmodeler.run.TransmodelerMATSim.PATHS_ELEMENT;
import static stockholm.ihop2.transmodeler.run.TransmodelerMATSim.TRANSMODELERCOMMAND_ELEMENT;
import static stockholm.ihop2.transmodeler.run.TransmodelerMATSim.TRANSMODELERCONFIG;
import static stockholm.ihop2.transmodeler.run.TransmodelerMATSim.TRANSMODELERFOLDER_ELEMENT;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import stockholm.ihop2.transmodeler.tripswriting.TransmodelerTripWriter;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TransmodelerMobsim implements Mobsim {

	private final TimeRepairingEventsManager eventsConsumer;

	private final String eventsFile;
	private final String transmodelerFolder;
	private final String transmodelerCommand;
	private final String linkAttributesFileName;
	private final String pathFileName;
	private final String tripFileName;
	private final Scenario scenario;

	@Inject
	public TransmodelerMobsim(final Scenario scenario,
			final EventsManager events) {
		this.eventsConsumer = new TimeRepairingEventsManager(scenario, events);
		this.eventsFile = scenario.getConfig().getModule(TRANSMODELERCONFIG)
				.getValue(EVENTS_ELEMENT);
		this.transmodelerFolder = scenario.getConfig()
				.getModule(TRANSMODELERCONFIG)
				.getValue(TRANSMODELERFOLDER_ELEMENT);
		this.transmodelerCommand = scenario.getConfig()
				.getModule(TRANSMODELERCONFIG)
				.getValue(TRANSMODELERCOMMAND_ELEMENT);
		this.linkAttributesFileName = scenario.getConfig()
				.getModule(TRANSMODELERCONFIG)
				.getValue(LINKATTRIBUTE_FILENAME_ELEMENT);
		this.pathFileName = scenario.getConfig().getModule(TRANSMODELERCONFIG)
				.getValue(PATHS_ELEMENT);
		this.tripFileName = scenario.getConfig().getModule(TRANSMODELERCONFIG)
				.getValue(TransmodelerMATSim.TRIPS_ELEMENT);
		this.scenario = scenario;
	}

	@Override
	public void run() {

		/*
		 * Read in link attributes.
		 */
		Logger.getLogger(this.getClass().getName()).info(
				"Loading file: " + this.linkAttributesFileName);
		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader linkAttrReader = new ObjectAttributesXmlReader(
				linkAttributes);
		linkAttrReader.readFile(this.linkAttributesFileName);

		/*
		 * Write Transmodeler routes and trips file.
		 */
		Logger.getLogger(this.getClass().getName()).info(
				"Writing files: " + this.pathFileName + ", "
						+ this.tripFileName);
		final TransmodelerTripWriter tripWriter = new TransmodelerTripWriter(
				this.scenario.getPopulation(), linkAttributes);
		try {
			tripWriter.writeTrips(this.pathFileName, this.tripFileName);
		} catch (FileNotFoundException e) {
			Logger.getLogger(this.getClass().getName()).severe(e.getMessage());
			e.printStackTrace();
		}

		/*
		 * Execute Transmodeler.
		 */
		Logger.getLogger(this.getClass().getName()).info(
				"Executing: " + this.transmodelerCommand);
		final Process proc;
		final int exitVal;
		try {
			proc = Runtime.getRuntime().exec(transmodelerCommand, null,
					new File(transmodelerFolder));
			exitVal = proc.waitFor();
			if (exitVal != 0) {
				throw new RuntimeException(
						"Transmodeler terminated with exit code " + exitVal
								+ ".");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		/*
		 * Read Transmodeler events file.
		 */
		this.eventsConsumer.unknownPersonsIDs.clear();
		this.eventsConsumer.unknownLinkIDs.clear();
		this.eventsConsumer.timeCorrectionStats.clear();

		Logger.getLogger(this.getClass().getName()).info(
				"Loading file: " + this.eventsFile);
		final MatsimEventsReader reader = new MatsimEventsReader(
				this.eventsConsumer);
		reader.readFile(this.eventsFile);

		Logger.getLogger(this.getClass().getName()).info(
				"Encountered " + this.eventsConsumer.unknownPersonsIDs.size()
						+ " unknown agents: "
						+ this.eventsConsumer.unknownPersonsIDs);
		Logger.getLogger(this.getClass().getName()).info(
				"Encountered " + this.eventsConsumer.unknownLinkIDs.size()
						+ " unknown links: "
						+ this.eventsConsumer.unknownLinkIDs);
		Logger.getLogger(this.getClass().getName()).info(
				"mean(time correction)   = "
						+ this.eventsConsumer.timeCorrectionStats.getAvg());
		Logger.getLogger(this.getClass().getName()).info(
				"stddev(time correction) = "
						+ this.eventsConsumer.timeCorrectionStats.getStddev());
		Logger.getLogger(this.getClass().getName()).info(
				"min(time correction) = "
						+ this.eventsConsumer.timeCorrectionStats.getMin());
		Logger.getLogger(this.getClass().getName()).info(
				"max(time correction) = "
						+ this.eventsConsumer.timeCorrectionStats.getMax());
	}
}
