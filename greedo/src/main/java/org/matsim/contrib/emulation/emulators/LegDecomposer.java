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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public abstract class LegDecomposer {

	// -------------------- MEMBERS --------------------

	final protected List<PlanElement> atomicPlanElements = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public LegDecomposer() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<PlanElement> getAtomicPlanElementsView() {
		return Collections.unmodifiableList(this.atomicPlanElements);
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public final void setCompositeLeg(final Leg leg, final Plan plan) {
		this.atomicPlanElements.clear();
		this.processCompositeLeg(plan, leg);
	}

	/**
	 * Expand here the given "composite" leg into an alternating sequence of
	 * "atomics" legs and activities. The sequence must leg/act-alternating and
	 * start and end with a leg. It must be added to the atomicPlanElements list.
	 */
	protected abstract void processCompositeLeg(final Plan plan, final Leg leg);

}
