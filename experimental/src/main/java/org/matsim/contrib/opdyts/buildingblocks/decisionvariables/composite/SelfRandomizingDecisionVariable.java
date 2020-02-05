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

import java.util.ArrayList;
import java.util.Collection;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SelfRandomizingDecisionVariable<U extends DecisionVariable> implements DecisionVariable {

	// -------------------- CONSTANTS --------------------

	private final U decisionVariable;

	private final DecisionVariableRandomizer<U> randomizer;

	// -------------------- CONSTRUCTION --------------------

	public SelfRandomizingDecisionVariable(final U decisionVariable, final DecisionVariableRandomizer<U> randomizer) {
		this.decisionVariable = decisionVariable;
		this.randomizer = randomizer;
	}

	// -------------------- IMPLEMENTATION --------------------

	public Collection<SelfRandomizingDecisionVariable<?>> newRandomVariations(final int searchIteration) {
		final Collection<SelfRandomizingDecisionVariable<?>> result = new ArrayList<>();
		for (U randomizedDecisionVariable : this.randomizer.newRandomVariations(this.decisionVariable,
				searchIteration)) {
			result.add(new SelfRandomizingDecisionVariable<U>(randomizedDecisionVariable, this.randomizer));
		}
		return result;
	}

	// -------------------- IMPLEMENTATION OF DecisionVariable --------------------

	@Override
	public void implementInSimulation() {
		this.decisionVariable.implementInSimulation();
	}
	
	// -------------------- OVERRIDING OF Object --------------------
	
	@Override
	public String toString() {
		return this.decisionVariable.toString();
	}
	
}
