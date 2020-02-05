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
package org.matsim.contrib.greedo;

import java.util.Map;

import org.matsim.contrib.greedo.datastructures.SpaceTimeCounts;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 * The "score" this class refers to is the anticipated change of the search
 * acceleration objective function resulting from setting a single agent's
 * (possibly space-weighted) 0/1 re-planning indicator.
 * 
 * Implements the score used in the greedy heuristic of Merz, P. and Freisleben,
 * B. (2002). "Greedy and local search heuristics for unconstrained binary
 * quadratic programming." Journal of Heuristics 8:197–213.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
class ScoreUpdater<L> {

	// -------------------- MEMBERS --------------------

	private final DynamicData<L> interactionResiduals;

	private final SpaceTimeCounts<L> individualWeightedChanges;

	private /* final */ double scoreChangeIfZero;

	private /* final */ double scoreChangeIfOne;

	private boolean residualsUpdated = false;

	// -------------------- CONSTRUCTION --------------------

	ScoreUpdater(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final double meanLambda, final double beta, final DynamicData<L> interactionResiduals,
			final double individualUtilityChange, final boolean correctAgentSize) {

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following because the
		 * same vehicle may enter the same link multiple times during one time bin.
		 */
		this.individualWeightedChanges = new SpaceTimeCounts<L>(upcomingIndicators, true, true);
		this.individualWeightedChanges.subtract(new SpaceTimeCounts<>(currentIndicators, true, true));

		/*
		 * Update the interaction residuals.
		 */
		this.interactionResiduals = interactionResiduals;
		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			double oldResidual = this.interactionResiduals.getBinValue(spaceObj, timeBin);
			double newResidual = oldResidual - meanLambda * weightedIndividualChange;
			this.interactionResiduals.put(spaceObj, timeBin, newResidual);
		}

		/*
		 * Compute score (approximated objective function in the solution heuristic):
		 * 
		 * interaction(lambda) - beta * lambda * individualUtilityChange.
		 */
		double sumOfWeightedIndividualChanges2 = 0.0;
		double sumOfWeightedIndividualChangesTimesInteractionResiduals = 0.0;
		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			sumOfWeightedIndividualChanges2 += weightedIndividualChange * weightedIndividualChange;
			sumOfWeightedIndividualChangesTimesInteractionResiduals += weightedIndividualChange
					* this.interactionResiduals.getBinValue(spaceObj, timeBin);
		}

		final double scoreIfOne = this.expectedInteraction(1.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals) - beta * 1.0 * individualUtilityChange;
		final double scoreIfMean = this.expectedInteraction(meanLambda, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals) - beta * meanLambda * individualUtilityChange;
		final double scoreIfZero = this.expectedInteraction(0.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals) - beta * 0.0 * individualUtilityChange;

		this.scoreChangeIfOne = scoreIfOne - scoreIfMean;
		this.scoreChangeIfZero = scoreIfZero - scoreIfMean;

		if (correctAgentSize) {
			final double agentSizeCorrection = (-1.0) * this.individualWeightedChanges.sumOfSquareEntries();
			this.scoreChangeIfOne += (1.0 - meanLambda) * agentSizeCorrection;
			this.scoreChangeIfZero += (0.0 - meanLambda) * agentSizeCorrection;
		}
	}

	// -------------------- INTERNALS --------------------

	private double expectedInteraction(final double lambda, final double sumOfWeightedIndividualChanges2,
			final double sumOfWeightedIndividualChangesTimesInteractionResiduals) {
		return lambda * lambda * sumOfWeightedIndividualChanges2
				+ 2.0 * lambda * sumOfWeightedIndividualChangesTimesInteractionResiduals;
	}

	// -------------------- IMPLEMENTATION --------------------

	void updateResiduals(final double newLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.interactionResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual + newLambda * entry.getValue();
			this.interactionResiduals.put(spaceObj, timeBin, newResidual);
		}
	}

	// -------------------- GETTERS --------------------

	double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}
}
