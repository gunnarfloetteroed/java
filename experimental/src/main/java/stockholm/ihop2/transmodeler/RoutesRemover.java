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
package stockholm.ihop2.transmodeler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoutesRemover {

	private RoutesRemover() {
	}

	public static void run(final String fromPopFile, final String toPopFile,
			final String networkFileName) {

		final Config config = ConfigUtils.createConfig();
		config.getModule("network").addParam("inputNetworkFile",
				networkFileName);
		config.getModule("plans").addParam("inputPlansFile", fromPopFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						final Leg leg = (Leg) planElement;
						leg.setRoute(null);
					}
				}
			}
		}

		PopulationWriter popwriter = new PopulationWriter(
				scenario.getPopulation(), scenario.getNetwork());
		popwriter.write(toPopFile);
	}

	public static void main(String[] args) {
		run("./ihop2/matsim-output/ITERS/it.0/0.plans.xml.gz",
				"./ihop2/matsim-input/plans-wout-routes.xml",
				"./ihop2/network-output/network.xml");
	}

}
