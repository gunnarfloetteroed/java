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
package stockholm.wum.analysis;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkUsageAnalyzer implements LinkEnterEventHandler {

	private final Scenario scenario;

	private boolean ignoreCircularLinks = true;

	private Map<String, Set<Id<Link>>> mode2linkIds = new LinkedHashMap<>();

	public LinkUsageAnalyzer(final Scenario scenario) {
		this.scenario = scenario;
		final Set<Id<Link>> carLinkIds = new LinkedHashSet<>();
		this.mode2linkIds.put("car", carLinkIds);
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				carLinkIds.add(link.getId());
			}
		}
	}

	public void setIgnoreCircularLinks(final boolean ignoreCircularLinks) {
		this.ignoreCircularLinks = ignoreCircularLinks;
	}

	public Map<String, Set<Id<Link>>> getMode2linkIds() {
		return this.mode2linkIds;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {

		final Vehicle transitVehicle = this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId());

		if (transitVehicle != null) {

			Link transitLink = this.scenario.getNetwork().getLinks().get(event.getLinkId());
			Id<Node> fromNodeId = transitLink.getFromNode().getId();
			Id<Node> toNodeId = transitLink.getToNode().getId();

			if (!this.ignoreCircularLinks || !fromNodeId.equals(toNodeId)) {

				final String mode = transitVehicle.getType().getId().toString();

				Set<Id<Link>> linkIds = this.mode2linkIds.get(mode);
				if (linkIds == null) {
					linkIds = new LinkedHashSet<>();
					this.mode2linkIds.put(mode, linkIds);
				}
				linkIds.add(event.getLinkId());
			}
		}
	}

	public static void main(String[] args) {
		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/production-scenario/";
		final String configFileName = path + "config.xml";
		final String eventsFileName = path + "output/output_events.xml.gz";

		final Config config = ConfigUtils.loadConfig(configFileName);
		config.network().addParam("inputNetworkFile", "output/output_network.xml.gz");
		config.plans().addParam("inputPlansFile", "output/output_plans.xml.gz");
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final EventsManager eventsManager = new EventsManagerImpl();
		final LinkUsageAnalyzer linkUsageAnalyzer = new LinkUsageAnalyzer(scenario);
		linkUsageAnalyzer.setIgnoreCircularLinks(true);
		eventsManager.addHandler(linkUsageAnalyzer);
		new MatsimEventsReader(eventsManager).readFile(eventsFileName);

		for (Map.Entry<String, Set<Id<Link>>> entry : linkUsageAnalyzer.getMode2linkIds().entrySet()) {
			System.out.println("Mode " + entry.getKey() + " uses " + entry.getValue().size() + " links.");
		}
	}
}
