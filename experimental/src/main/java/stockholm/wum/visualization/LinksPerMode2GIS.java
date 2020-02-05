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
package stockholm.wum.visualization;

import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import stockholm.saleem.StockholmTransformationFactory;
import stockholm.wum.analysis.LinkUsageAnalyzer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinksPerMode2GIS {

	private final Scenario scenario;

	private final Map<String, Set<Id<Link>>> detailedMode2LinkIds;

	public LinksPerMode2GIS(final Scenario scenario, final Map<String, Set<Id<Link>>> detailedMode2LinkIds) {
		this.scenario = scenario;
		this.detailedMode2LinkIds = detailedMode2LinkIds;
	}

	public void run(final String path) {

		final FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);

		for (Map.Entry<String, Set<Id<Link>>> entry : this.detailedMode2LinkIds.entrySet()) {
						
//			final List<Link> links = new ArrayList<>(entry.getValue().size());
			Network network = NetworkUtils.createNetwork() ;
			for (Id<Link> linkId : entry.getValue()) {
//				links.add(this.scenario.getNetwork().getLinks().get(linkId));
				network.addLink(this.scenario.getNetwork().getLinks().get(linkId));
			}
			
			final String shapeFileName = FilenameUtils.concat(path, entry.getKey() + "-links.shp");
//			new Links2ESRIShape(links, shapeFileName, builder).write();
			new Links2ESRIShape(network, shapeFileName, builder).write();
			
			// I replaced "links" by "network" to make this compile; I have not tested if it works.  kai, aug'18
		}
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		
		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/2018-08-09_scenario/";
		final String outPath = path + "output/";
		final String configFileName = path + "config.xml";
		final String eventsFileName = outPath + "output_events.xml.gz";

		final Config config = ConfigUtils.loadConfig(configFileName);
		config.network().addParam("inputNetworkFile", "output/output_network.xml.gz");
		config.plans().addParam("inputPlansFile", "output/output_plans.xml.gz");
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final EventsManager eventsManager = new EventsManagerImpl();
		final LinkUsageAnalyzer ptLinkExtractor = new LinkUsageAnalyzer(scenario);
		ptLinkExtractor.setIgnoreCircularLinks(true);
		eventsManager.addHandler(ptLinkExtractor);
		new MatsimEventsReader(eventsManager).readFile(eventsFileName);
		
		// TODO add car links!
		
		final LinksPerMode2GIS linkWriter = new LinksPerMode2GIS(scenario, ptLinkExtractor.getMode2linkIds());
		linkWriter.run(outPath);
		
		System.out.println("... DONE");
	}
}