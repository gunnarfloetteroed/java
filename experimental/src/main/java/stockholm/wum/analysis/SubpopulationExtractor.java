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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SubpopulationExtractor {

	public SubpopulationExtractor() {
	}

	private Set<Id<Person>> readIds(final String idFile) throws IOException {
		final Set<Id<Person>> result = new LinkedHashSet<>();
		final BufferedReader reader = new BufferedReader(new FileReader(idFile));
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() > 0) {
				result.add(Id.createPersonId(line));
			}
		}
		reader.close();
		return result;
	}

	public void run(final String from, final String to, final String ids) {

		final Set<Id<Person>> personIds;
		try {
			personIds = this.readIds(ids);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		final Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(from);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		scenario.getPopulation().getPersons().keySet().retainAll(personIds);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			PersonUtils.removeUnselectedPlans(person);
		}

		new PopulationWriter(scenario.getPopulation()).write(to);
	}

	public static void main(String[] args) {
		final String path = "/Users/GunnarF/OneDrive - VTI/My Data/wum/WUM-FINAL/";
		
		System.out.println("STARTED ...");

		// new SubpopulationExtractor().run(path + "2019-11-06_basecase/output/output_plans.xml.gz", path + "baseCase_malinBoatUsers.xml.gz", path + "malin-boat-users.ids.txt");
		new SubpopulationExtractor().run(path + "2019-11-07_policycase/output/output_plans.xml.gz", path + "policyCase_malinBoatUsers.xml.gz", path + "malin-boat-users.ids.txt");
		
		System.out.println("... DONE");
	}

}
