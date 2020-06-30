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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
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

		final String fromFile = "/Users/GunnarF/OneDrive - VTI/My Data/wum/WUM-FINAL/2019-11-06_basecase/output/output_plans.xml.gz";
		final String toFile = "/Users/GunnarF/OneDrive - VTI/My Data/wum/WUM-FINAL/basecase_ferry-users.xml.gz";
		final String onlyIdsFile = "/Users/GunnarF/OneDrive - VTI/My Data/wum/WUM-FINAL/basecase_ferry-users.ids.txt";

		
		
		final Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(fromFile);
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
		writer.write(toFile);
		
		try {
			final PrintWriter idWriter = new PrintWriter(onlyIdsFile);
			for (Id<Person> id : scenario.getPopulation().getPersons().keySet()) {
				idWriter.println(id.toString());
			}
			idWriter.flush();
			idWriter.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}

}
