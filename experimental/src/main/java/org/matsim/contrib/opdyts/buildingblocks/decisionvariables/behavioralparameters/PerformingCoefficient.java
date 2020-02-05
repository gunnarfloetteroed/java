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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.behavioralparameters;

import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.AbstractScalarDecisionVariable;
import org.matsim.core.config.Config;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PerformingCoefficient extends AbstractScalarDecisionVariable<PerformingCoefficient> {

	// -------------------- CONSTANTS --------------------

	private final Config config;

	// -------------------- MEMBERS --------------------

	// private double value;

	// -------------------- CONSTRUCTION --------------------

	public PerformingCoefficient(final Config config, final double value) {
		super(value);
		this.config = config;
		// this.value = value;
	}

	// --------------- IMPLEMENTATION OF ScalarDecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		// this.config.planCalcScore().setPerforming_utils_hr(this.value);
		this.config.planCalcScore().setPerforming_utils_hr(this.getValue());
	}

	// @Override
	// public void setValue(final double value) {
	// this.value = value;
	// }

	// @Override
	// public double getValue() {
	// return this.value;
	// }

	@Override
	public PerformingCoefficient newDeepCopy() {
		// return new PerformingCoefficient(this.config, this.value);
		return new PerformingCoefficient(this.config, this.getValue());
	}

	@Override
	public String toString() {
		// return this.getClass().getSimpleName() + "(" + this.value + ")";
		return this.getClass().getSimpleName() + "(" + this.getValue() + ")";
	}
}
