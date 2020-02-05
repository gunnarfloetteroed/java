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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.utilities.FractionalIterable;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PopulationSampler {

	public static void main(String[] args) {

		String from = null;
		String to = null;
		double frac = 0;
		try {
			from = args[0];
			to = args[1];
			frac = Double.parseDouble(args[2]);
		} catch (Exception e) {
			System.out.println("parameters: fromFile toFile samplingFraction");
			System.exit(0);
		}

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(from);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(scenario.getPopulation().getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);
		
		Set<Id<Person>> allToRemove = new LinkedHashSet<>();
		for (Id<Person> personId : new FractionalIterable<>(allPersonIdsShuffled, // scenario.getPopulation().getPersons().keySet(),
				1.0 - frac)) {
			allToRemove.add(personId);
		}

		for (Id<Person> removeId : allToRemove) {
			scenario.getPopulation().getPersons().remove(removeId);
		}

		new PopulationWriter(scenario.getPopulation()).write(to);
	}
}
