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

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ModeChoiceAnalyzer {

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile("/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-22a/"
				+ "output_plans.downsampled.0-2_of_0-25.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Collection<? extends Person> population = scenario.getPopulation().getPersons().values();

		BestPlanSelector<Plan, Person> sel = new BestPlanSelector<>();
		
		double selectedScoreSum = 0.0;
		double bestScoreSum = 0.0;
		for (Person person : population) {
			selectedScoreSum += person.getSelectedPlan().getScore();
			bestScoreSum += sel.selectPlan(person).getScore();			
		}

		System.out.println("selected: " + selectedScoreSum / population.size());
		System.out.println("best:     " + bestScoreSum / population.size());
		
		System.out.println("... DONE");
	}

}
