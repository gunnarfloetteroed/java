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
package org.matsim.contrib.emulation;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class EmulationConfigGroup extends ReflectiveConfigGroup {

	// TODO rename to "emulation"
	public static final String GROUP_NAME = "ier";

	public EmulationConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- iterationsPerCycle --------------------

	/**
	 * Number of replanning + scoring iterations.
	 */
	private int iterationsPerCycle = 10;

	@StringGetter("iterationsPerCycle")
	public int getIterationsPerCycle() {
		return this.iterationsPerCycle;
	}

	@StringSetter("iterationsPerCycle")
	public void setIterationsPerCycle(int iterationsPerCycle) {
		this.iterationsPerCycle = iterationsPerCycle;
	}

	// -------------------- batchSize --------------------

	/**
	 * This is the number of agents that are simulated in a chunk on each thread. A
	 * value that is too small will slow down the emulation because of the overhead
	 * of creating the scoring functions. A value that is too high will lead to the
	 * situation where some threads may have a lot of "heavy" agents that take a lot
	 * of run time and some may have only "light" ones. This would also effectively
	 * increase runtime.
	 */
	private int batchSize = 200;

	@StringGetter("batchSize")
	public int getBatchSize() {
		return this.batchSize;
	}

	@StringSetter("batchSize")
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

}
