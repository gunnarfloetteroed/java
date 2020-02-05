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

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
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
public class ExtractFerryUsers {

	public static void main(String[] args) {

		final String path = "/Users/GunnarF/NoBackup/data-workspace/wum/runs/2018-01-06b/";
		final String fromFile = "output/ITERS/it.300/300.plans.xml.gz";
		final String toFile = "300.plans.ferry-users.xml.gz";

		final Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(path + fromFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Set<Id<Person>> noFerryUsers = new LinkedHashSet<>(scenario.getPopulation().getPersons().keySet());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if ((planElement instanceof Leg)) {
					final Leg leg = (Leg) planElement;
					if ("ferryPassenger".equals(leg.getMode())) {
						noFerryUsers.remove(person.getId());
					}
				}
			}
		}
		
		for (Id<Person> noFerryUser : noFerryUsers) {
			scenario.getPopulation().getPersons().remove(noFerryUser);
		}
		final PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(path + toFile);
		
		
		
	}

}
