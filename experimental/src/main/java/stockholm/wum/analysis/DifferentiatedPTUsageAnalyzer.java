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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DifferentiatedPTUsageAnalyzer {

	private final Scenario scenario;

	private final Map<String, Set<Id<Person>>> mode2userIds = new LinkedHashMap<>();

	public DifferentiatedPTUsageAnalyzer(final Scenario scenario) {
		this.scenario = scenario;
	}

	public Map<String, Set<Id<Person>>> getMode2userIds() {
		return this.mode2userIds;
	}
	
	public void run() {

		// Collect direct transit route references.

		final Map<Id<TransitRoute>, TransitRoute> id2transitRoute = new LinkedHashMap<>();
		for (TransitLine line : this.scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				id2transitRoute.put(route.getId(), route);
			}
		}

		// Identify users of differentiated transit modes.

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					final Leg leg = (Leg) planElement;
					if (leg.getRoute() instanceof ExperimentalTransitRoute) {
						final TransitRoute route = id2transitRoute
								.get(((ExperimentalTransitRoute) leg.getRoute()).getRouteId());
						Set<Id<Person>> users = this.mode2userIds.get(route.getTransportMode());
						if (users == null) {
							users = new LinkedHashSet<>();
							this.mode2userIds.put(route.getTransportMode(), users);
						}
						users.add(person.getId());
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/production-scenario/";
		final String configFileName = path + "config.xml";

		Config config = ConfigUtils.loadConfig(configFileName);
		config.network().addParam("inputNetworkFile", "2018-08-29_output/output_network.xml.gz");
		config.plans().addParam("inputPlansFile", "2018-08-29_output/output_plans.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		final DifferentiatedPTUsageAnalyzer ptUsageAnalyzer = new DifferentiatedPTUsageAnalyzer(scenario);
		ptUsageAnalyzer.run();

		for (Map.Entry<String, Set<Id<Person>>> entry : ptUsageAnalyzer.mode2userIds.entrySet()) {
			System.out.println("Mode " + entry.getKey() + " has " + entry.getValue().size() + " users.");
		}
	}
}
