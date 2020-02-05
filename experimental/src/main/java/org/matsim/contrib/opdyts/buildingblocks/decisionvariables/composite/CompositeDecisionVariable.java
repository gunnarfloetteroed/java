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

import java.util.List;

import floetteroed.opdyts.DecisionVariable;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CompositeDecisionVariable implements DecisionVariable {

	// -------------------- MEMBERS --------------------

	private final List<SelfRandomizingDecisionVariable<? extends DecisionVariable>> decisionVariables;

	// -------------------- CONSTRUCTION --------------------

	public CompositeDecisionVariable(
			final List<SelfRandomizingDecisionVariable<? extends DecisionVariable>> decisionVariables) {
		this.decisionVariables = decisionVariables;
	}

	// -------------------- GETTERS --------------------

	public List<SelfRandomizingDecisionVariable<? extends DecisionVariable>> getDecisionVariables() {
		return this.decisionVariables;
	}

	// --------------- IMPLEMENTATION OF DecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		for (DecisionVariable decisionVariable : this.decisionVariables) {
			decisionVariable.implementInSimulation();
		}
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("[");
		if (this.decisionVariables.size() > 0) {
			result.append(this.decisionVariables.get(0).toString());
			for (int i = 1; i < this.decisionVariables.size(); i++) {
				result.append(",");
				result.append(this.decisionVariables.get(i));
			}
		}
		result.append("]");
		return result.toString();
	}
}
