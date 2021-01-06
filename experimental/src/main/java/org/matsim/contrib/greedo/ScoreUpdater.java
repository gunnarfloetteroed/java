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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.greedo.datastructures.SpaceTimeCounts;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 * Inspired by the greedy heuristic of Merz, P. and Freisleben, B. (2002).
 * "Greedy and local search heuristics for unconstrained binary quadratic
 * programming." Journal of Heuristics 8:197–213.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L the space coordinate type
 *
 */
class ScoreUpdater<L> {

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup greedoConfig;

	private final Network network;

	private final Map<L, Double> _B;

	private final DynamicData<L> changeResiduals;

	private final SpaceTimeCounts<L> unweightedNewXn;
	private final SpaceTimeCounts<L> unweightedOldXn;

	private final SpaceTimeCounts<L> weightedDeltaXn;

	private final SpaceTimeCounts<L> interactionsWhenStaying;
	private final SpaceTimeCounts<L> interactionsWhenMoving;

	private final boolean doesNothing;

	private final double scoreChangeIfZero;
	private final double scoreChangeIfOne;

	private final double tn;
	private final double dn0;
	private final double dn1;

	private boolean residualsUpdated = false;

	// -------------------- CONSTRUCTION --------------------

	ScoreUpdater(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final double individualUtilityChange, final DynamicData<L> changeResiduals,
			final double anticipatedMeanLambda, final Map<L, Double> _B, final GreedoConfigGroup greedoConfig,
			final Network network) {

		this.greedoConfig = greedoConfig;
		this._B = _B;
		this.network = network;

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following because the
		 * same particle may enter the same slot multiple times during one time bin.
		 */
		this.unweightedNewXn = new SpaceTimeCounts<L>(upcomingIndicators, true, false);
		this.unweightedOldXn = new SpaceTimeCounts<L>(currentIndicators, true, false);

		this.weightedDeltaXn = new SpaceTimeCounts<L>(upcomingIndicators, true, true);
		this.weightedDeltaXn.subtract(new SpaceTimeCounts<>(currentIndicators, true, true));
		this.doesNothing = (this.weightedDeltaXn.entriesView().size() == 0);

		/*
		 * Removing the anticipated effect of this individual's replanning (based on
		 * anticipatedMeanLambda) from the change residuals.
		 */
		this.changeResiduals = changeResiduals;
		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.weightedDeltaXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.changeResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual - anticipatedMeanLambda * entry.getValue();
			this.changeResiduals.put(spaceObj, timeBin, newResidual);
		}

		/*
		 * Compute the interaction terms.
		 */

		this.interactionsWhenMoving = new SpaceTimeCounts<L>();
//		if (upcomingIndicators != null) {
//			for (int timeBin = 0; timeBin < greedoConfig.getBinCnt(); timeBin++) {
//				for (SpaceTimeIndicators<L>.Visit visit : upcomingIndicators.getVisits(timeBin)) {
//					if (this.largeEnough(visit.spaceObject, network, greedoConfig.getMinPhysLinkSize_veh())) {
//						this.interactionsWhenMoving.add(visit.spaceObject, timeBin,
//								changeResiduals.getBinValue(visit.spaceObject, timeBin));
//					}
//				}
//			}
//		}
		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedNewXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			if (this.largeEnough(spaceObj)) {
				final int timeBin = entry.getKey().getB();
				this.interactionsWhenMoving.add(spaceObj, timeBin,
						entry.getValue() * changeResiduals.getBinValue(spaceObj, timeBin));
			}
		}

		this.interactionsWhenStaying = new SpaceTimeCounts<L>();
//		if (currentIndicators != null) {
//			for (int timeBin = 0; timeBin < greedoConfig.getBinCnt(); timeBin++) {
//				for (SpaceTimeIndicators<L>.Visit visit : currentIndicators.getVisits(timeBin)) {
//					if (this.largeEnough(visit.spaceObject, network, greedoConfig.getMinPhysLinkSize_veh())) {
//						this.interactionsWhenStaying.add(visit.spaceObject, timeBin,
//								changeResiduals.getBinValue(visit.spaceObject, timeBin));
//					}
//				}
//			}
//		}
		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedOldXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			if (this.largeEnough(spaceObj)) {
				final int timeBin = entry.getKey().getB();
				this.interactionsWhenStaying.add(spaceObj, timeBin,
						entry.getValue() * changeResiduals.getBinValue(spaceObj, timeBin));
			}
		}

		/*
		 * Finally, compute the scores.
		 */

		final double disappointmentIfOne = this.disappointment(1.0);
		final double disappointmentIfZero = this.disappointment(0.0);
		this.dn0 = disappointmentIfZero;
		this.dn1 = disappointmentIfOne;

//		this.dn0 = disappointmentIfZero;
//		this.tn = disappointmentIfOne - disappointmentIfZero;
//
//		final double scoreIfOne = -(1.0 * individualUtilityChange - disappointmentIfOne);
//		final double scoreIfMean = -(anticipatedMeanLambda * individualUtilityChange
//				- this.disappointment(anticipatedMeanLambda));
//		final double scoreIfZero = -(0.0 * individualUtilityChange - disappointmentIfZero);

		final double scoreIfOne;
		final double scoreIfMean;
		final double scoreIfZero;
		if (this.greedoConfig.getUseDinT()) {
			this.tn = disappointmentIfOne - disappointmentIfZero;
			scoreIfOne = -(1.0 * individualUtilityChange - disappointmentIfOne);
			scoreIfMean = -(anticipatedMeanLambda * individualUtilityChange
					- this.disappointment(anticipatedMeanLambda));
			scoreIfZero = -(0.0 * individualUtilityChange - disappointmentIfZero);
		} else {
			this.tn = 0.0;
			scoreIfOne = -(1.0 * individualUtilityChange - 0.0);
			scoreIfMean = -(anticipatedMeanLambda * individualUtilityChange - 0.0);
			scoreIfZero = -(0.0 * individualUtilityChange - 0.0);
		}

		this.scoreChangeIfOne = scoreIfOne - scoreIfMean;
		this.scoreChangeIfZero = scoreIfZero - scoreIfMean;
	}

	// -------------------- INTERNALS --------------------

	// FIXME As it is now, this only makes sense for road traffic.
	private boolean largeEnough(final L linkId) {
		final Link link = this.network.getLinks().get(linkId);
		final double size_veh = link.getLength() * link.getNumberOfLanes() / 7.5;
		return (size_veh >= this.greedoConfig.getMinPhysLinkSize_veh());
	}

	private double disappointment(final double lambda) {
		double result = 0;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedNewXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			if (this.largeEnough(spaceObj)) {
				final int timeBin = entry.getKey().getB();
				double change = this.changeResiduals.getBinValue(spaceObj, timeBin);
				if (this.greedoConfig.getSelfInteraction()) {
					change += lambda * this.weightedDeltaXn.get(spaceObj, timeBin);
				}
				result += lambda * entry.getValue() * this._B.getOrDefault(spaceObj, 0.0) * change;
			}
		}

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedOldXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			if (this.largeEnough(spaceObj)) {
				final int timeBin = entry.getKey().getB();
				double change = this.changeResiduals.getBinValue(spaceObj, timeBin);
				if (this.greedoConfig.getSelfInteraction()) {
					change += lambda * this.weightedDeltaXn.get(spaceObj, timeBin);
				}
				result += (1.0 - lambda) * entry.getValue() * this._B.getOrDefault(spaceObj, 0.0) * change;
			}
		}

		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	void updateResiduals(final double realizedLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.weightedDeltaXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.changeResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual + realizedLambda * entry.getValue();
			this.changeResiduals.put(spaceObj, timeBin, newResidual);
		}
	}

	// -------------------- GETTERS --------------------

	boolean doesNothing() {
		return this.doesNothing;
	}

	double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}

	double getTn() {
		return this.tn;
	}

	double getDn0() {
		return this.dn0;
	}

	double getDn1() {
		return this.dn1;
	}

	SpaceTimeCounts<L> getRealizedInteractions(final boolean isReplanner) {
		final SpaceTimeCounts<L> result;
		if (isReplanner) {
			result = this.interactionsWhenMoving;
			if (this.greedoConfig.getSelfInteraction()) {
				for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedNewXn.entriesView()) {
					final L spaceObj = entry.getKey().getA();
					final int timeBin = entry.getKey().getB();
					result.add(spaceObj, timeBin, this.weightedDeltaXn.get(spaceObj, timeBin));
				}
			}
		} else {
			result = this.interactionsWhenStaying;
		}
		return result;
	}
}
