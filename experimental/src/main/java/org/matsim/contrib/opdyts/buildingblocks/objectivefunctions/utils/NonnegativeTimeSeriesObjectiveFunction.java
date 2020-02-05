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
package org.matsim.contrib.opdyts.buildingblocks.objectivefunctions.utils;

import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public abstract class NonnegativeTimeSeriesObjectiveFunction<X extends MATSimState> implements MATSimObjectiveFunction<X> {

	// ---------------------- CONSTANTS ----------------------

	private final NonnegativeTimeSeriesComparator comp = new NonnegativeTimeSeriesComparator();

	private final double[] realData;

	// ---------------------- MEMBERS ----------------------

	private double removalRelativeToOneBinShiftWeight = 1.0;

	// ---------------------- CONSTRUCTION ----------------------

	public NonnegativeTimeSeriesObjectiveFunction(final double[] realData) {
		this.realData = realData;
	}

	// ---------------------- SETTERS ----------------------

	public void setRemovalRelativeToOneBinShiftWeight(final double weight) {
		this.removalRelativeToOneBinShiftWeight = weight;
	}

	// ----------------- IMPLEMENTATION OF ObjectiveFunction -----------------

	@Override
	public double value(final X state) {
		this.comp.compute(this.simData(state), this.realData);
		return this.comp.getEarthMoverDistance()
				+ this.removalRelativeToOneBinShiftWeight * this.comp.getAbsoluteDifference();
	}

	// ----------------- INTERFACE DEFINITION -----------------

	public abstract double[] simData(final X state);

}
