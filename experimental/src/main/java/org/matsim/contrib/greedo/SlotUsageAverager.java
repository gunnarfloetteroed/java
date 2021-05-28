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
package org.matsim.contrib.greedo;

import static floetteroed.utilities.DynamicDataUtils.innerProduct;
import static floetteroed.utilities.DynamicDataUtils.newWeightedSum;
import static floetteroed.utilities.DynamicDataUtils.sumOfDifferences2;
import static floetteroed.utilities.SetUtils.union;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.addIndicatorsToTotalsTreatingNullAsZero;
import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.newTotals;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.FractionalIterable;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SlotUsageAverager {

	private final String logFile = "stepSizeLog.txt";

	private final boolean useSlotWeight = true;

	private final double lambdaStartup = 0.25;
	private final double lambdaMin = 0.01;
	private final double beta = 0.9;

	private final GreedoConfigGroup config;

	// Let "z" be the state of this process. It is defined as the vector of total
	// anticipated slot usages, accounting for replanner selection.

	// for approximating the value function
	private DynamicData<Id<?>> previousZ = null;
	private Double previousDeltaX2 = null;
	private Double previousDeltaUn0 = null;
	private DynamicData<Id<?>> valueCenterZ = null;
	private Double w = null;

	// for book-keeping
	private DynamicData<Id<?>> meanZ = null;
	private DynamicData<Id<?>> meanX = null;

	SlotUsageAverager(final GreedoConfigGroup config) {
		this.config = config;
		try {
			FileUtils.writeStringToFile(new File(this.logFile),
					"meanX1\tmeanX2\tmeanZ1\tmeanZ2\tmeanV1\tmeanV2\tlambda\tdeltaX2\tdeltaUn0\tw\n", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	Set<Id<Person>> selectReplanners(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> realizedSlotUsages,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> hypotheticalSlotUsages, final double deltaUn0,
			final double innoWeight) {

		final DynamicData<Id<?>> currentX = newTotals(this.config.newTimeDiscretization(), realizedSlotUsages.values(),
				false, this.useSlotWeight);
		if (this.meanX == null) {
			this.meanX = currentX;
		} else {
			this.meanX = newWeightedSum(this.meanX, 1.0 - innoWeight, currentX, innoWeight);
		}

		final DynamicData<Id<?>> deltaX = newWeightedSum(newTotals(this.config.newTimeDiscretization(),
				hypotheticalSlotUsages.values(), false, this.useSlotWeight), 1.0, currentX, -1.0);
		final double deltaX2 = deltaX.sumOfEntries2();

		final double lambda;
		if (this.valueCenterZ == null) {
			lambda = this.lambdaStartup;
		} else {
			final double lambdaNum = innerProduct(deltaX, newWeightedSum(this.valueCenterZ, 1.0, currentX, -1.0));
			final double lambdaDen = max(1e-8, deltaX2);
			final double lambdaUnconstr = lambdaNum / lambdaDen;
			lambda = max(this.lambdaMin, min(1.0, lambdaUnconstr));
		}

		final Set<Id<Person>> allPersonIds = union(realizedSlotUsages.keySet(), hypotheticalSlotUsages.keySet());
		final Set<Id<Person>> replannerIds = new LinkedHashSet<>();
		for (Id<Person> replannerId : new FractionalIterable<>(allPersonIds, lambda)) {
			replannerIds.add(replannerId);
		}

		final DynamicData<Id<?>> currentZ = new DynamicData<>(this.config.newTimeDiscretization());
		for (Id<Person> personId : allPersonIds) {
			addIndicatorsToTotalsTreatingNullAsZero(currentZ,
					replannerIds.contains(personId) ? hypotheticalSlotUsages.get(personId)
							: realizedSlotUsages.get(personId),
					1.0, false, this.useSlotWeight);
		}
		if (this.meanZ == null) {
			this.meanZ = currentZ;
		} else {
			this.meanZ = newWeightedSum(this.meanZ, 1.0 - innoWeight, currentZ, innoWeight);
		}

		if (this.previousZ == null) {
			this.valueCenterZ = currentZ;
			this.w = 1.0;
		} else {
			final double wNew = (
			// this.previousDeltaX2
			this.previousDeltaUn0 + this.beta * this.w * sumOfDifferences2(currentZ, this.valueCenterZ))
					/ max(1e-8, sumOfDifferences2(this.previousZ, this.valueCenterZ));
			final double etaUnconstr = 1.0 - sqrt(wNew / this.w);

			final double eta = innoWeight * min(1.0, max(0.0, etaUnconstr));
			this.w = (1.0 - innoWeight) * this.w + innoWeight * wNew;
			this.valueCenterZ = newWeightedSum(this.valueCenterZ, 1.0 - eta, this.previousZ, eta);
		}
		this.previousZ = currentZ;
		this.previousDeltaX2 = deltaX2;
		this.previousDeltaUn0 = deltaUn0;

		final ThreePointProjector tpp = new ThreePointProjector();
		final double d12 = sqrt(sumOfDifferences2(this.meanX, this.meanZ));
		final double d23 = sqrt(sumOfDifferences2(this.meanZ, this.valueCenterZ));
		final double d31 = sqrt(sumOfDifferences2(this.valueCenterZ, this.meanX));
		tpp.update(d12, d23, d31);
		try {
			writeStringToFile(new File(this.logFile),
					(tpp.z1[0] + "\t" + tpp.z1[1] + "\t" + tpp.z2[0] + "\t" + tpp.z2[1] + "\t" + tpp.z3[0] + "\t"
							+ tpp.z3[1] + "\t" + lambda + "\t" + deltaX2 + "\t" + deltaUn0 + "\t" + this.w + "\n")
									.replace('.', ','),
					true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return replannerIds;
	}
}
