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
package org.matsim.contrib.opdyts.objectivefunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.core.controler.AbstractModule;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class MATSimObjectiveFunctionSum<X extends MATSimState> implements MATSimObjectiveFunction<X> {

	private List<MATSimObjectiveFunction<X>> objectiveFunctions = new ArrayList<>();
	private List<Double> weights = new ArrayList<>();

	public void add(final MATSimObjectiveFunction<X> objectiveFunction, final Double weight) {
		this.objectiveFunctions.add(objectiveFunction);
		this.weights.add(weight);
	}

	@Override
	public AbstractModule newAbstractModule() {
		AbstractModule result = AbstractModule.emptyModule();
		for (MATSimObjectiveFunction<X> objectiveFunction : this.objectiveFunctions) {
			result = AbstractModule.override(Arrays.asList(result), objectiveFunction.newAbstractModule());
		}
		return result;
	};

	@Override
	public double value(final X state) {
		double result = 0.0;
		for (int i = 0; i < this.objectiveFunctions.size(); i++) {
			result += this.objectiveFunctions.get(i).value(state) * this.weights.get(i);
		}
		return result;
	}
}
