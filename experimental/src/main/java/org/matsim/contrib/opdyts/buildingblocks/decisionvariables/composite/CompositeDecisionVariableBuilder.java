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
import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CompositeDecisionVariableBuilder {

	// -------------------- MEMBERS --------------------

	private final List<SelfRandomizingDecisionVariable<?>> selfRandomizingDecisionVariables = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public CompositeDecisionVariableBuilder() {
	}

	// -------------------- BUILDING --------------------

	public CompositeDecisionVariableBuilder add(
			final SelfRandomizingDecisionVariable<?> selfRandomizingDecisionVariable) {
		this.selfRandomizingDecisionVariables.add(selfRandomizingDecisionVariable);
		return this;
	}

	public <U extends DecisionVariable> CompositeDecisionVariableBuilder add(final U decisionVariable,
			final DecisionVariableRandomizer<U> randomizer) {
		this.add(new SelfRandomizingDecisionVariable<U>(decisionVariable, randomizer));
		return this;
	}

	public CompositeDecisionVariableBuilder add(final CompositeDecisionVariable compositeDecisionVariable) {
		this.selfRandomizingDecisionVariables.addAll(compositeDecisionVariable.getDecisionVariables());
		return this;
	}

	// -------------------- RESULT ACCESS --------------------
	
	public CompositeDecisionVariable buildDecisionVariable() {
		return new CompositeDecisionVariable(this.selfRandomizingDecisionVariables);
	}

}
