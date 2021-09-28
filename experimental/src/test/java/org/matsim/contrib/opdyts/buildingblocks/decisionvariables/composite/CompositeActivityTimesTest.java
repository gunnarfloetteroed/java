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
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.ScalarRandomizer;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CompositeActivityTimesTest {

	@Test // TODO Test against expected value.
	public void test() {

		System.out.println(Math.pow(0, -1));
		
		CompositeDecisionVariableBuilder builder = new CompositeDecisionVariableBuilder();
		builder.add(new OpeningTime(null, "w", 3600), new ScalarRandomizer<>(1800, 0));
		builder.add(new ClosingTime(null, "w", 7200), new ScalarRandomizer<>(1800, 0));
		CompositeDecisionVariable original = builder.buildDecisionVariable();

		DecisionVariableRandomizer<CompositeDecisionVariable> randomizer = new OneAtATimeRandomizer();

		System.out.println("original: " + original);
		for (CompositeDecisionVariable variation : randomizer.newRandomVariations(original, 0)) {
			System.out.println("variation: " + variation);
		}

	}

}
