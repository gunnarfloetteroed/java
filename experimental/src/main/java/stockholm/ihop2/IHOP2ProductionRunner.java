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
package stockholm.ihop2;

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;

import stockholm.saleem.StockholmTransformationFactory;
import stockholm.ihop2.regent.demandreading.ObjectAttributeStatistics;
import stockholm.ihop2.regent.demandreading.PopulationCreator;
import stockholm.ihop2.regent.demandreading.PopulationStatistics;
import stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class IHOP2ProductionRunner {

	public IHOP2ProductionRunner() {
	}

	public static void main(String[] args) throws IOException {

		/*
		 * ALL POSSIBLE PARAMETERS
		 */

		final String path = "/Users/GunnarF/OneDrive - VTI/My Data/ihop2/ihop2-data/";

		final String nodesFile = path + "network-input/Nodes.csv";
		final String segmentsFile = path + "network-input/Segments.csv";
		final String lanesFile = path + "network-input/Lanes.csv";
		final String laneConnectorsFile = path + "network-input/Lane Connectors.csv";
		final String linksFile = path + "network-input/Links.csv";

		final String matsimNetworkFile = path + "network-output/network.xml";
		// final String matsimNetworkFile = "/Users/GunnarF/OneDrive - VTI/My Data/ihop4/Stockholm.xml.gz";

		final String matsimFullNetworkFile = path + "network-output/network-full.xml";
		final String linkAttributesFile = path + "network-output/link-attributes.xml";
		final String matsimLanesFile = path + "network-output/lanes.xml";
		final String matsimTollFile = path + "network-output/toll.xml";

		final String zonesShapeFileName = path + "demand-input/sverige_TZ_EPSG3857.shp";
		final String buildingShapeFileName = path + "demand-input/by_full_EPSG3857_2.shp";
		final String populationFileName = path + "demand-input/trips.xml";

		final double populationSample = 0.25;
		// final String initialPlansFile = "/Users/GunnarF/OneDrive - VTI/My Data/ihop4/25PctAllModes.xml";
		final String initialPlansFile = "/Users/GunnarF/NoBackup/data-workspace/wum/production-scenario/25PctAllModes.xml";

		final String configFileName = path + "matsim-input/matsim-config.xml";
		final double networkUpscaleFactor = 2;
		final String lastIteration = "200";
		final boolean useLanes = false;
		final boolean useRoadPricing = false;
		final boolean doRouteChoice = true; // changes "module 2"'s choice proba
		final boolean doTimeChoice = true; // changes "module 3"'s choice proba

		/*
		 * DECIDE WHAT TO ACTUALLY DO
		 */

		final boolean doNetworkConversion = false;		
		final boolean doPopulationGeneration = false;
		final boolean checkPopulation = false;
		final boolean runMATSim = false;

		/*
		 * TRANSMODELER -> MATSIM NETWORK CONVERSION
		 */

		if (doNetworkConversion) {
			final Transmodeler2MATSimNetwork tm2MATSim = new Transmodeler2MATSimNetwork(nodesFile, linksFile,
					segmentsFile, lanesFile, laneConnectorsFile, matsimNetworkFile, matsimFullNetworkFile,
					linkAttributesFile, matsimLanesFile, matsimTollFile);
			tm2MATSim.run();
		}

		/*
		 * REGENT -> MATSIM POPULATION CONVERSION
		 */
		if (doPopulationGeneration) {

			final PopulationCreator populationCreator = new PopulationCreator(matsimNetworkFile, zonesShapeFileName,
					StockholmTransformationFactory.WGS84_EPSG3857, populationFileName);
			populationCreator.setBuildingsFileName(buildingShapeFileName);
			populationCreator.setPopulationSampleFactor(populationSample);
			populationCreator.run(initialPlansFile);
		}

		if (checkPopulation) {
			ObjectAttributeStatistics personAttrStats = new ObjectAttributeStatistics(populationFileName);
			PopulationStatistics populationStats = new PopulationStatistics(initialPlansFile);

			System.out.println("------------------------------------------------------");
			System.out.println("FROM ATTRIBUTES FILE");
			personAttrStats.printSummaryStatistic();

			System.out.println("------------------------------------------------------");
			System.out.println("FROM POPULATION FILE");
			populationStats.printSummaryStatistic();
		}

		/*
		 * MATSIM ITERATIONS
		 */
		if (runMATSim) {
			final Config config = ConfigUtils.loadConfig(configFileName, new RoadPricingConfigGroup());
			config.getModule("qsim").addParam("flowCapacityFactor",
					Double.toString(networkUpscaleFactor * populationSample));
			config.getModule("qsim").addParam("storageCapacityFactor",
					Double.toString(networkUpscaleFactor * populationSample));
			config.getModule("network").addParam("inputNetworkFile", matsimNetworkFile);
			config.getModule("plans").addParam("inputPlansFile", initialPlansFile);
			config.getModule("controler").addParam("lastIteration", lastIteration);
			config.controler()
					.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

			if (useLanes) {
				config.network().setLaneDefinitionsFile(matsimLanesFile);
				config.qsim().setUseLanes(true);
				config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
				config.controler().setLinkToLinkRoutingEnabled(true);
				config.getModule("qsim").addParam("stuckTime", "1e6");
				config.getModule("qsim").addParam("endTime", "99:00:00");
			} else {
				config.getModule("qsim").addParam("stuckTime", "10");
			}

			if (doRouteChoice) {
				config.getModule("strategy").addParam("ModuleProbability_2", "0.1");
			} else {
				config.getModule("strategy").addParam("ModuleProbability_2", "0.0");
			}

			if (doTimeChoice) {
				config.getModule("strategy").addParam("ModuleProbability_3", "0.1");
			} else {
				config.getModule("strategy").addParam("ModuleProbability_3", "0.0");
			}

			final Controler controler = new Controler(config);

			// LinkToLinkRouting is added automatically when
			// config.controler.linkToLinkRoutingEnabled == true
			// michalm, jan'17
			// if (useLanes) {
			// controler
			// .addOverridingModule(new LinkToLinkRoutingGuiceModule());
			// }
			if (useRoadPricing) {
				controler.setModules(new ControlerDefaultsWithRoadPricingModule());
			}

			controler.run();
		}
	}
}
