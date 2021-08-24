/*
 * Copyright 2021 Gunnar Flötteröd
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
package org.matsim.contrib.greedo.greedoreplanning;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TwoRoutes {

	private final Random rnd = new Random();

	private final int popSize;

	private final double physCostStddev;

	public int onRoute0;

	public TwoRoutes(final int popSize, final double physCostStddev) {
		this.popSize = popSize;
		this.physCostStddev = physCostStddev;
		this.onRoute0 = this.popSize;
	}

	private int onRoute1() {
		return (this.popSize - this.onRoute0);
	}

	public double[] drawReplicationAveragedCosts(final int replications) {
		final double cost0 = this.onRoute0 + this.physCostStddev / sqrt(replications) * this.rnd.nextGaussian();
		final double cost1 = this.onRoute1() + this.physCostStddev / sqrt(replications) * this.rnd.nextGaussian();
		return new double[] { cost0, cost1 };
	}

	public double equilibriumGap(final double[] costs) {
		if (costs[0] < costs[1]) {
			return this.onRoute1() * (costs[1] - costs[0]);
		} else {
			return this.onRoute0 * (costs[0] - costs[1]);
		}
	}

	public void replan(int trustRegion, int replications) {
		final double[] costs = this.drawReplicationAveragedCosts(replications);
		if (costs[0] < costs[1]) {
			final int switchers = min(trustRegion, this.onRoute1());
			this.onRoute0 += switchers;
		} else if (costs[1] < costs[0]) {
			final int switchers = min(trustRegion, this.onRoute0);
			this.onRoute0 -= switchers;
		}
	}

	public static void main(String[] args) {
		TwoRoutes tr = new TwoRoutes(101, 100);
		for (int k = 0; k < 1000; k++) {
			System.out.println(tr.onRoute0 + "\t" + tr.equilibriumGap(tr.drawReplicationAveragedCosts(1)));
			tr.replan(1, 10000);
		}
	}
}
