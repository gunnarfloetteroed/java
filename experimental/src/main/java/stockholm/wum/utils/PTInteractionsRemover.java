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
package stockholm.wum.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PTInteractionsRemover {

	private static void prn(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				System.out.print(((Activity) pe).getType());
			} else {
				System.out.print(" > " + ((Leg) pe).getMode() + " > ");				
			}
		}
		System.out.println();

	}
	
	public static void run(final Person person, final boolean verbose) {

		if (verbose) {
			prn(person.getSelectedPlan());
		}
		
		final List<Activity> remainingActs = new ArrayList<>();
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				final Activity act = (Activity) pe;
				final String type = act.getType().toLowerCase();
				if (!(type.startsWith("pt") && type.endsWith("interaction"))) {
					remainingActs.add(act);
				}
			}
		}

		final Plan newPlan = PopulationUtils.createPlan();
		for (int i = 0; i < remainingActs.size() - 1; i++) {
			newPlan.addActivity(remainingActs.get(i));
			newPlan.addLeg(PopulationUtils.createLeg("car"));
		}
		newPlan.addActivity(remainingActs.get(remainingActs.size() - 1));

		person.getPlans().clear();
		person.addPlan(newPlan);
		person.setSelectedPlan(newPlan);
		
		if (verbose) {
			prn(person.getSelectedPlan());
			System.out.println();
		}
		
	}

}
