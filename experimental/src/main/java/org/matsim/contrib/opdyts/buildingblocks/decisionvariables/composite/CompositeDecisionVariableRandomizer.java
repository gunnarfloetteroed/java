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
import java.util.List;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public abstract class CompositeDecisionVariableRandomizer
		implements DecisionVariableRandomizer<CompositeDecisionVariable> {

	// --------------- IMPLEMENTATION OF DecisionVariableRandomizer ---------------

	@Override
	public final Collection<CompositeDecisionVariable> newRandomVariations(
			final CompositeDecisionVariable compositeDecisionVariable, final int searchIteration) {

		// Obtain a representation of all possible decision variable combinations.
		final List<Collection<SelfRandomizingDecisionVariable<?>>> variationsPerDecisionVariableList = new ArrayList<>();
		for (SelfRandomizingDecisionVariable<?> selfRandomizingDecisionVariable : compositeDecisionVariable
				.getDecisionVariables()) {
			variationsPerDecisionVariableList.add(selfRandomizingDecisionVariable.newRandomVariations(searchIteration));
		}

		// Select, from all combinations, the most relevant ones.
		final Collection<CompositeDecisionVariable> result = new ArrayList<>();
		for (List<SelfRandomizingDecisionVariable<?>> relevantVariations : this.selectDecisionVariableCombinations(
				variationsPerDecisionVariableList, compositeDecisionVariable.getDecisionVariables())) {
			result.add(new CompositeDecisionVariable(relevantVariations));
		}
		return result;
	}

	// -------------------- INTERFACE DEFINITION --------------------

	protected abstract Collection<List<SelfRandomizingDecisionVariable<?>>> selectDecisionVariableCombinations(
			List<Collection<SelfRandomizingDecisionVariable<?>>> variations,
			List<SelfRandomizingDecisionVariable<?>> original);

}
