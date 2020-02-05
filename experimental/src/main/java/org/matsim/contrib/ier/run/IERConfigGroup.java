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
package org.matsim.contrib.ier.run;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IERConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "ier";

	public IERConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- writePerformanceOutput --------------------

	private boolean writePerformanceOutput = false;

	@StringGetter("writePerformanceOutput")
	public boolean getWritePerformanceOutput() {
		return this.writePerformanceOutput;
	}

	@StringSetter("writePerformanceOutput")
	public void setWritePerformanceOutput(boolean writePerformanceOutput) {
		this.writePerformanceOutput = writePerformanceOutput;
	}

	// -------------------- (experimental switch) parallel --------------------
	//
	// private boolean parallel = true;
	//
	// @StringGetter("parallel")
	// public boolean getParallel() {
	// return this.parallel;
	// }
	//
	// @StringSetter("parallel")
	// public void setParallel(boolean parallel) {
	// this.parallel = parallel;
	// }

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
