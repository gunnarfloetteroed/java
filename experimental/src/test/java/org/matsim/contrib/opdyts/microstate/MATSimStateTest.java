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
package org.matsim.contrib.opdyts.microstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class MATSimStateTest {

	@Test
	public void test() {

		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		// person1 has two plans, of which one is selected
		Person person1 = population.getFactory().createPerson(Id.createPersonId("person1"));
		Plan plan1ofPerson1 = population.getFactory().createPlan();
		plan1ofPerson1.setType("plan1ofPerson1");
		plan1ofPerson1.setPerson(person1);
		person1.addPlan(plan1ofPerson1);
		person1.setSelectedPlan(plan1ofPerson1);
		Plan plan2ofPerson1 = population.getFactory().createPlan();
		plan2ofPerson1.setType("plan2ofPerson1");
		plan2ofPerson1.setPerson(person1);
		person1.addPlan(plan2ofPerson1);
		population.addPerson(person1);

		// person2 has one plan and that one is selected
		Person person2 = population.getFactory().createPerson(Id.createPersonId("person2"));
		Plan plan1ofPerson2 = population.getFactory().createPlan();
		plan1ofPerson2.setType("plan1ofPerson2");
		plan1ofPerson2.setPerson(person2);
		person2.addPlan(plan1ofPerson2);
		person2.setSelectedPlan(plan1ofPerson2);
		population.addPerson(person2);

		// person3 has no plans
		Person person3 = population.getFactory().createPerson(Id.createPersonId("person3"));
		population.addPerson(person3);
		person3.setSelectedPlan(null);
		
		// extract the current state
		Vector macroState = new Vector(1.0, 2.0, 3.0);
		DecisionVariable decisionVariable = new DecisionVariable() {
			@Override
			public void implementInSimulation() {
			}
		};
		MATSimState microState = (new MATSimStateFactoryImpl()).newState(population, macroState, decisionVariable);

		// now, only the second plan of person1 is left and selected
		person1.removePlan(plan1ofPerson1);
		person1.setSelectedPlan(plan2ofPerson1);

		// person2 takes over the (selected) plan(s) from person3
		person2.getPlans().clear();
		person2.setSelectedPlan(null);

		// person3 takes over the (selected) plan(s) from person2
		person3.getPlans().clear();
		person3.addPlan(plan1ofPerson2);
		person3.getPlans().get(0).setType("plan1ofPerson3");
		person3.getPlans().get(0).setPerson(person3);
		person3.setSelectedPlan(null);

		// reset to the previous state
		microState.implementInSimulation();
		assertEquals(3, population.getPersons().size());

		// person1 has two plans, of which one is selected
		assertEquals(2, population.getPersons().get(Id.createPersonId("person1")).getPlans().size());
		assertEquals("plan1ofPerson1",
				population.getPersons().get(Id.createPersonId("person1")).getPlans().get(0).getType());
		assertEquals("plan2ofPerson1",
				population.getPersons().get(Id.createPersonId("person1")).getPlans().get(1).getType());
		assertEquals("plan1ofPerson1",
				population.getPersons().get(Id.createPersonId("person1")).getSelectedPlan().getType());

		// person2 has one plan, which also is selected
		assertEquals(1, population.getPersons().get(Id.createPersonId("person2")).getPlans().size());
		assertEquals("plan1ofPerson2",
				population.getPersons().get(Id.createPersonId("person2")).getPlans().get(0).getType());
		assertEquals("plan1ofPerson2", population.getPersons().get(Id.createPersonId("person2")).getSelectedPlan().getType());		
		
		// person3 has no plans
		assertEquals(0, population.getPersons().get(Id.createPersonId("person3")).getPlans().size());
		assertNull(population.getPersons().get(Id.createPersonId("person3")).getSelectedPlan());		
	}
}
