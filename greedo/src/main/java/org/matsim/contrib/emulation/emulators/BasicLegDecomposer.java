/*
 * Greedo -- Equilibrium approximation for general-purpose multi-agent simulations.
 *
 * Copyright 2022 Gunnar Flötteröd
 * 
 *
 * This file is part of Greedo.
 *
 * Greedo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greedo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Greedo.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@gmail.com
 *
 */
package org.matsim.contrib.emulation.emulators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class BasicLegDecomposer extends LegDecomposer {

	@Override
	public void processCompositeLeg(Plan plan, Leg leg) {
		// Not much to do, we decompose the (already atomic) leg in itself.
		super.atomicPlanElements.add(leg);
	}

}
