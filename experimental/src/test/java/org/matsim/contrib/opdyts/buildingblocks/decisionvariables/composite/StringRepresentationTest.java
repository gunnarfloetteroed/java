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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite;

import org.junit.Test;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.ClosingTime;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.OpeningTime;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.TypicalDuration;

import floetteroed.opdyts.DecisionVariable;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class StringRepresentationTest {

	@Test // TODO Eventually check against expected format.
	public void test() {
		System.out.println(
				new SelfRandomizingDecisionVariable<DecisionVariable>(new ClosingTime(null, "w", 8 * 3600), null));
	}

	@Test // TODO Eventually check against expected format.
	public void test2() {
		SelfRandomizingDecisionVariable<?> var = new SelfRandomizingDecisionVariable<DecisionVariable>(
				new ClosingTime(null, "w", 8 * 3600), null);
		var = new SelfRandomizingDecisionVariable<DecisionVariable>(var, null);
		var = new SelfRandomizingDecisionVariable<DecisionVariable>(var, null);
		var = new SelfRandomizingDecisionVariable<DecisionVariable>(var, null);
		System.out.println(var);
	}

	@Test // TODO Eventually check against expected format.
	public void test3() {
		CompositeDecisionVariableBuilder builder = new CompositeDecisionVariableBuilder();
		builder.add(new OpeningTime(null, "w", 8 * 3600), null);
		builder.add(new TypicalDuration(null, "w", 12 * 3600), null);
		builder.add(new ClosingTime(null, "w", 20 * 3600), null);
		CompositeDecisionVariable comp = builder.buildDecisionVariable();
		System.out.println(comp);
	}
}
