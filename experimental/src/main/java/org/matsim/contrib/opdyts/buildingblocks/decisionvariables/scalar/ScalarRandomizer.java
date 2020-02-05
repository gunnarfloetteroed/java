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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar;

import java.util.Arrays;
import java.util.Collection;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ScalarRandomizer<U extends ScalarDecisionVariable<U>> implements DecisionVariableRandomizer<U> {

	// -------------------- CONSTANTS --------------------

	private final double initialStepSize;

	private final double searchStageExponent;

	// -------------------- CONSTRUCTION --------------------

	public ScalarRandomizer(final double initialStepSize, final double searchStageExponent) {
		this.initialStepSize = initialStepSize;
		this.searchStageExponent = searchStageExponent;
	}

	public ScalarRandomizer(final double initialStepSize) {
		this(initialStepSize, 0.0);
	}

	// --------------- IMPLEMENTATION OF DecisionVariableRandomizer ---------------

	@Override
	public Collection<U> newRandomVariations(final U decisionVariable, final int searchStage) {
		final double stepSize = this.initialStepSize * Math.pow(searchStage, this.searchStageExponent);
		final U variation1 = decisionVariable.newDeepCopy();
		final U variation2 = decisionVariable.newDeepCopy();
		variation1.setValue(decisionVariable.getValue() - stepSize);
		variation2.setValue(decisionVariable.getValue() + stepSize);
		return Arrays.asList(variation1, variation2);
	}
}
