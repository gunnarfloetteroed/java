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
package stockholm.ihop2.regent.demandreading;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationStatistics extends AbstractDemandStatistic {

	public PopulationStatistics(final String populationFile) {
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile(populationFile);
		this.parse(scenario.getPopulation());
	}

	public PopulationStatistics(final Population population) {
		this.parse(population);
	}

	private void parse(final Population population) {
		for (Person person : population.getPersons().values()) {
			this.allPersonIds.add(person.getId().toString());
			for (Map.Entry<String, Object> entry : person.getAttributes().getAsMap().entrySet()) {
				this.addAttribute(entry.getKey(), entry.getValue());
			}
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					this.addTripMode(((Leg) planElement).getMode());
				}
			}
		}
	}

	public static void main(String[] args) {
		PopulationStatistics stats = new PopulationStatistics(
				"/Users/GunnarF/NoBackup/data-workspace/ihop2/ihop2-data/demand-output/1pctAllModes.xml");
		stats.printSummaryStatistic();
	}

}
