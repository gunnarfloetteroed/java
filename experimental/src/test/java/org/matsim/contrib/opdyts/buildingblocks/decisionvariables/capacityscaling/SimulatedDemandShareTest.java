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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.capacityscaling;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariable;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariableBuilder;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.OneAtATimeRandomizer;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.ScalarRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SimulatedDemandShareTest {

	@Test
	public void testIndividual() {
		SimulatedDemandShare decVar = new SimulatedDemandShare(null, 0.1, null);
		ScalarRandomizer<SimulatedDemandShare> randomizer = new ScalarRandomizer<>(0.01);
		boolean found0_09 = false;
		boolean found0_11 = false;
		for (SimulatedDemandShare variation : randomizer.newRandomVariations(decVar, 0)) {
			found0_09 |= (variation.getValue() >= 0.09 - 1e-8 && variation.getValue() <= 0.09 + 1e-8);
			found0_11 |= (variation.getValue() >= 0.11 - 1e-8 && variation.getValue() <= 0.11 + 1e-8);
		}
		Assert.assertTrue(found0_09);
		Assert.assertTrue(found0_11);
	}

	@Test
	public void testComposite() {		
		SimulatedDemandShare decVar = new SimulatedDemandShare(null, 0.1, null);
		ScalarRandomizer<SimulatedDemandShare> randomizer = new ScalarRandomizer<>(0.01);

		CompositeDecisionVariableBuilder builder = new CompositeDecisionVariableBuilder();
		builder.add(decVar,  randomizer);		
		CompositeDecisionVariable compositeDecVar = builder.buildDecisionVariable();
		OneAtATimeRandomizer compositeRandomizer = new OneAtATimeRandomizer();
		
		System.out.println(compositeRandomizer.newRandomVariations(compositeDecVar, 0));
	}

}
