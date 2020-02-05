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
package org.matsim.contrib.opdyts.buildingblocks.objectivefunctions;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WeightedSumObjectiveFunction<X extends SimulatorState> implements ObjectiveFunction<X> {

	private List<Tuple<ObjectiveFunction<X>, Double>> objFctAndWeight = new ArrayList<>();

	public WeightedSumObjectiveFunction() {
	}

	public void add(final ObjectiveFunction<X> objectiveFunction, final Double weight) {
		this.objFctAndWeight.add(new Tuple<>(objectiveFunction, weight));
	}

	@Override
	public double value(final X state) {
		double result = 0.0;
		for (Tuple<ObjectiveFunction<X>, Double> objFctAndWeight : this.objFctAndWeight) {
			result += objFctAndWeight.getFirst().value(state) * objFctAndWeight.getSecond();
		}
		return result;
	}

}
