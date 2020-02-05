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

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public abstract class AbstractScalarDecisionVariable<U extends AbstractScalarDecisionVariable<U>>
		implements ScalarDecisionVariable<U> {

	// -------------------- MEMBERS --------------------

	private double minValue = Double.NEGATIVE_INFINITY;

	private double maxValue = Double.POSITIVE_INFINITY;

	private double value; // guaranteed to be initialized in constructor

	// -------------------- CONSTRUCTION --------------------

	public AbstractScalarDecisionVariable(final double value) {
		this.value = value;
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void setMinValue_s(final double minValue) {
		this.minValue = minValue;
	}

	public double getMinValue_s() {
		return this.minValue;
	}

	public void setMaxValue_s(final double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMaxValue_s() {
		return this.maxValue;
	}

	// ---------- PARTIAL IMPLEMENTATION OF ScalarDecisionVariable ----------

	@Override
	public final void setValue(double value) {
		value = Math.max(value, this.minValue);
		value = Math.min(value, this.maxValue);
		this.value = value;
	}

	@Override
	public final double getValue() {
		return this.value;
	}

}
