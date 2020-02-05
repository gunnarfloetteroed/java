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
public interface ModeAnalyzer {

	public static void main(String[] args) {
		System.out.println("STARTED ..:");
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile("/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-14b/2000.plans.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Set<String> legModes = new LinkedHashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					legModes.add(leg.getMode());
					System.out.println(leg.getRoute().getRouteDescription());
				}
			}
		}
		System.out.println(legModes);
		
		System.out.println("... DONE");
	}
	
}

