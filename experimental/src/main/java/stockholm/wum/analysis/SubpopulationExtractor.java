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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.StringUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SubpopulationExtractor {

	public static void main(String[] args) {

		String from = null;
		String to = null;
		String idList = null;
		try {
			from = args[0];
			to = args[1];
			idList = args[2];
		} catch (Exception e) {
			System.out.println("parameters: fromFile toFile commaSeparatedListOfPersonIds");
			System.exit(0);
		}

		final Set<Id<Person>> allRelevantPersonIds = new LinkedHashSet<>();
		for (String part : StringUtils.explode(idList, ',')) {
			allRelevantPersonIds.add(Id.createPersonId(part.trim().intern()));
		}
		System.out.println("Identified " + allRelevantPersonIds.size() + " person ids: " + allRelevantPersonIds);

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(from);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Set<Id<Person>> allToRemove = new LinkedHashSet<>();
		for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
			if (!allRelevantPersonIds.contains(personId)) {
				allToRemove.add(personId);
			}
		}

		for (Id<Person> removeId : allToRemove) {
			scenario.getPopulation().getPersons().remove(removeId);
		}
		System.out.println("Identified " + scenario.getPopulation().getPersons().keySet().size() + " persons: "
				+ scenario.getPopulation().getPersons().keySet());

		for (Person person : scenario.getPopulation().getPersons().values()) {
			PersonUtils.removeUnselectedPlans(person);
		}
		
		new PopulationWriter(scenario.getPopulation()).write(to);
	}
}
