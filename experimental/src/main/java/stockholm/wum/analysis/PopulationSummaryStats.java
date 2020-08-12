/*
 * Copyright 2020 Gunnar Flötteröd
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PopulationSummaryStats {

	final Map<String, Double> mode2metersTraveled;
	final Map<Id<Person>, Double> id2score;

	public PopulationSummaryStats(final String popFile) {
		final Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(popFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		this.mode2metersTraveled = new LinkedHashMap<>();
		this.id2score = new LinkedHashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			this.id2score.put(person.getId(), person.getSelectedPlan().getScore());
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Leg) {
					final Leg leg = (Leg) pE;
					this.mode2metersTraveled.put(leg.getMode(),
							this.mode2metersTraveled.getOrDefault(leg.getMode(), 0.0) + leg.getRoute().getDistance());
				}
			}
		}
	}

	// -------------------- --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/OneDrive - VTI/My Data/wum/WUM-FINAL/";

		final PopulationSummaryStats baseCaseStats = new PopulationSummaryStats(
				path + "/malin-comparison/baseCase_malinBoatUsers.xml");
		final PopulationSummaryStats policyCaseStats = new PopulationSummaryStats(
				path + "/malin-comparison/policyCase_malinBoatUsers.xml");

		final Set<Id<Person>> allPersonIds = new TreeSet<>(baseCaseStats.id2score.keySet());
		allPersonIds.addAll(policyCaseStats.id2score.keySet()); // should not change anything
		System.out.println();
		System.out.println("ID\tBASECASE\tPOLICYCASE");
		for (Id<Person> personId : allPersonIds) {
			System.out.print(personId);
			System.out.print("\t" + baseCaseStats.id2score.get(personId));
			System.out.println("\t" + policyCaseStats.id2score.get(personId));
		}
		System.out.println();
		
		final Set<String> allModes = new TreeSet<>(baseCaseStats.mode2metersTraveled.keySet());
		allModes.addAll(policyCaseStats.mode2metersTraveled.keySet()); // may add smth new
		System.out.println();
		System.out.println("MODE\tBASECASE\tPOLICYCASE");
		for (String mode : allModes) {
			System.out.print(mode);
			System.out.print("\t" + Math.round(baseCaseStats.mode2metersTraveled.getOrDefault(mode, 0.0)));
			System.out.println(
					"\t" + Math.round(policyCaseStats.mode2metersTraveled.getOrDefault(mode, 0.0)));
		}
		System.out.println();

		System.out.println("... DONE");

	}

}
