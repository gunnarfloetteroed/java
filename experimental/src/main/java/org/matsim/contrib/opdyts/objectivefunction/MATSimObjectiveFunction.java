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

import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.core.controler.AbstractModule;

import floetteroed.opdyts.ObjectiveFunction;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public interface MATSimObjectiveFunction<X extends MATSimState> extends ObjectiveFunction<X> {

	/**
	 * This is an attempt to facilitate usage of MATSim's dependency injection
	 * framework: Instead of requiring all relevant state information to be provided
	 * by the X instance passed to the value(X) function, an objective function may
	 * provide here its own MATSim modules through which it can connect directly to
	 * MATSim.
	 * <p>
	 * The module provided here is added to the MATSim controler each time an opdyts
	 * stage is started.
	 */
	public default AbstractModule newAbstractModule() {
		return AbstractModule.emptyModule();
	}

	@Override
	public double value(X state);

}
