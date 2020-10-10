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
 * @param L the space coordinate type
 *
 */
class ScoreUpdaterTII<L> {

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup greedoConfig;

	private final DynamicData<L> presenceResiduals;
	private final DynamicData<L> changeResiduals;

	private final SpaceTimeCounts<L> unweightedNewXn;
	private final SpaceTimeCounts<L> unweightedOldXn;

	private final SpaceTimeCounts<L> unweightedDeltaXn;
	private final SpaceTimeCounts<L> weightedDeltaXn;

	// Interaction i = x_ni * deltaY_i, without abs(.) operation.
	private final SpaceTimeCounts<L> interactionsWhenStaying;
	private final SpaceTimeCounts<L> interactionsWhenMoving;

	private DynamicData<L> _B;

	private final double scoreChangeIfZero;
	private final double scoreChangeIfOne;

	private final double disappointmentIfZero;
	private final double disappointmentIfOne;

	private boolean residualsUpdated = false;

	public Double Tn = null;
	public Double Dn0 = null;

	// -------------------- CONSTRUCTION --------------------

	ScoreUpdaterTII(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final double meanLambda, final double individualUtilityChange, final DynamicData<L> _B,
			final DynamicData<L> presenceResiduals, final DynamicData<L> changeResiduals,
			final GreedoConfigGroup greedoConfig, final Network network) {

		this.greedoConfig = greedoConfig;
		this._B = _B;

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following because the
		 * same particle may enter the same slot multiple times during one time bin.
		 */
		this.unweightedNewXn = new SpaceTimeCounts<L>(upcomingIndicators, true, false);
		this.unweightedOldXn = new SpaceTimeCounts<L>(currentIndicators, true, false);

		this.unweightedDeltaXn = new SpaceTimeCounts<L>(upcomingIndicators, true, false);
		this.unweightedDeltaXn.subtract(new SpaceTimeCounts<L>(currentIndicators, true, false));

		this.weightedDeltaXn = new SpaceTimeCounts<L>(upcomingIndicators, true, true);
		this.weightedDeltaXn.subtract(new SpaceTimeCounts<>(currentIndicators, true, true));

		/*
		 * Update the residuals.
		 */
		this.presenceResiduals = presenceResiduals;
		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedDeltaXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.presenceResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual - meanLambda * entry.getValue();
			this.presenceResiduals.put(spaceObj, timeBin, newResidual);
		}

		this.changeResiduals = changeResiduals;
		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.weightedDeltaXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.changeResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual - meanLambda * entry.getValue();
			this.changeResiduals.put(spaceObj, timeBin, newResidual);
		}

		/*
		 * Interaction terms (new).
		 */
		this.interactionsWhenMoving = new SpaceTimeCounts<L>();
		this.interactionsWhenStaying = new SpaceTimeCounts<L>();
		for (int timeBin = 0; timeBin < _B.getBinCnt(); timeBin++) {
			if (upcomingIndicators != null) {
				for (SpaceTimeIndicators<L>.Visit visit : upcomingIndicators.getVisits(timeBin)) {
					if (this.largeEnough(visit.spaceObject, network, greedoConfig.getMinPhysLinkSize_veh())) {
						this.interactionsWhenMoving.add(visit.spaceObject, timeBin,
								changeResiduals.getBinValue(visit.spaceObject, timeBin));
					}
				}
			}
			if (currentIndicators != null) {
				for (SpaceTimeIndicators<L>.Visit visit : currentIndicators.getVisits(timeBin)) {
					if (this.largeEnough(visit.spaceObject, network, greedoConfig.getMinPhysLinkSize_veh())) {
						this.interactionsWhenStaying.add(visit.spaceObject, timeBin,
								changeResiduals.getBinValue(visit.spaceObject, timeBin));
					}
				}
			}
		}

		/*
		 * Finally, the scores.
		 */

		this.disappointmentIfOne = this.disappointment(1.0);
		this.disappointmentIfZero = this.disappointment(0.0);

		this.Dn0 = this.disappointmentIfZero;
		this.Tn = this.disappointmentIfOne - this.disappointmentIfZero;

		final double scoreIfOne = -(1.0 * individualUtilityChange - this.disappointmentIfOne);
		final double scoreIfMean = -(meanLambda * individualUtilityChange - this.disappointment(meanLambda));
		final double scoreIfZero = -(0.0 * individualUtilityChange - this.disappointmentIfZero);

		this.scoreChangeIfOne = scoreIfOne - scoreIfMean;
		this.scoreChangeIfZero = scoreIfZero - scoreIfMean;
	}

	private boolean largeEnough(final L linkId, final Network net, final double minPhysVehSize_veh) {
		final Link link = net.getLinks().get(linkId); // TODO works only for road traffic!
		final double size_veh = link.getLength() * link.getNumberOfLanes() / 7.5;
		return (size_veh >= minPhysVehSize_veh);
	}

	// -------------------- INTERNALS --------------------

	private double disappointment(final double lambda) {
		double result = 0;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedNewXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			double variability = this.changeResiduals.getBinValue(spaceObj, timeBin);
			if (this.greedoConfig.getSelfInteraction()) {
				variability += lambda * this.weightedDeltaXn.get(spaceObj, timeBin);
			}
			result += lambda * entry.getValue() * this._B.getBinValue(spaceObj, timeBin) * variability;
		}

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedOldXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			double variability = this.changeResiduals.getBinValue(spaceObj, timeBin);
			if (this.greedoConfig.getSelfInteraction()) {
				variability += lambda * this.weightedDeltaXn.get(spaceObj, timeBin);
			}
			result += (1.0 - lambda) * entry.getValue() * this._B.getBinValue(spaceObj, timeBin) * variability;
		}

		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	void updateResiduals(final double newLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.unweightedDeltaXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.presenceResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual + newLambda * entry.getValue();
			this.presenceResiduals.put(spaceObj, timeBin, newResidual);
		}

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.weightedDeltaXn.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.changeResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual + newLambda * entry.getValue();
			this.changeResiduals.put(spaceObj, timeBin, newResidual);
		}
	}

	// -------------------- GETTERS --------------------

	double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}

	double getDisappointmentIfOne() {
		return this.disappointmentIfOne;
	}

	double getDisappointmentIfZero() {
		return this.disappointmentIfZero;
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
				throw new RuntimeException("unsupported feature: self interaction");
			}
		} else {
			result = this.interactionsWhenStaying;
		}
		return result;
	}

}
