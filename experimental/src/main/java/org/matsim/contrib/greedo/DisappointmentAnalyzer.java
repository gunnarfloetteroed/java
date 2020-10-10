/*
 * Copyright 2020 Gunnar Flötteröd
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeCounts;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DisappointmentAnalyzer {

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup conf;

	private final Map<Id<?>, Double> _B;

	// TODO Find more representative statistics.
	//	private Double lastNaiveIndividualAbsE = null;
	//	private Double lastEstimIndividualAbsE = null;

	// -------------------- CONSTRUCTION --------------------

	public DisappointmentAnalyzer(final GreedoConfigGroup conf) {
		this.conf = conf;
		this._B = new LinkedHashMap<>();
	}

	// -------------------- INTERNALS --------------------

	private double predictVariability(final Id<Person> personId, final SpaceTimeCounts<Id<?>> interactions) {
		double result = 0;
		for (Map.Entry<Tuple<Id<?>, Integer>, Double> entry : interactions.entriesView()) {
			final Id<?> loc = entry.getKey().getA();
			final double interaction = entry.getValue();
			result += interaction * this._B.getOrDefault(loc, 0.0);
		}
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void update(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> currentIndicators,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> anticipatedIndicators,
			final Map<Id<Person>, Double> personId2realizedUtilityChange,
			final Map<Id<Person>, Double> personId2anticipatedUtilityChange, final Set<Id<Person>> replannerIds,
			final Map<Id<Person>, SpaceTimeCounts<Id<?>>> personId2interactions, final double stepSize) {

//		this.lastEstimIndividualAbsE = 0.0;
//		this.lastNaiveIndividualAbsE = 0.0;

		final Map<Id<?>, Double> deltaB = new LinkedHashMap<>();

		double etaNumerator = 0.0; // TODO revisit step size logic
		// vector in eta denominator equals deltaDiagonal2 + ... other ^2 terms

		for (Id<Person> personId : personId2realizedUtilityChange.keySet()) {

			final SpaceTimeCounts<Id<?>> interactions = personId2interactions.get(personId);
			final double variability = this.predictVariability(personId, interactions);

			final double anticipatedUtilityChange;
			if (replannerIds.contains(personId)) {
				anticipatedUtilityChange = personId2anticipatedUtilityChange.get(personId);
			} else {
				anticipatedUtilityChange = 0.0;
			}

			final double naiveEn = anticipatedUtilityChange - personId2realizedUtilityChange.get(personId);

			// TODO >>> Used this code before >>>
//			double en;
//			if (this.variabilityConfig.getUseAbsoluteVariability()) {
//				throw new RuntimeException("unsupported");
//			} else {
//				if (this.variabilityConfig.getTruncateImprovement()) {
//					throw new RuntimeException("unsupported");
//				} else {
//					en = this.beta * anticipatedUtilityChange - (this.variabilityConfig.getEstimateStationaryB() ? 0.0
//							: personId2realizedUtilityChange.get(personId)) - variability;
//				}
//			}
			// TODO <<< Used this code before <<<
			final double en = (anticipatedUtilityChange - variability) - personId2realizedUtilityChange.get(personId);

//			this.lastEstimIndividualAbsE += Math.abs(en);
			etaNumerator += en * en;

			for (Map.Entry<Tuple<Id<?>, Integer>, Double> entry : interactions.entriesView()) {
				final Id<?> loc = entry.getKey().getA();
				double interaction = entry.getValue();
				deltaB.put(loc, deltaB.getOrDefault(loc, 0.0) + en * interaction);
			}

		}

		double etaDenominator = 0.0; // TODO stream
		for (double val : deltaB.values()) {
			etaDenominator += val * val;
		}

		if (etaDenominator >= 1e-8) {
			final double eta = stepSize * etaNumerator / etaDenominator;
			for (Map.Entry<Id<?>, Double> entry : deltaB.entrySet()) {
				final Id<?> loc = entry.getKey();
				double newBValue = this._B.getOrDefault(loc, 0.0) + eta * entry.getValue();
				if (this.conf.getNonnegativeB()) {
					newBValue = Math.max(0.0, newBValue);
				}
				this._B.put(loc, newBValue);
			}
		}
	}

//	public Double getLastNaiveAbsE() {
//		return this.lastNaiveIndividualAbsE;
//	}

//	public Double getLastEstimAbsE() {
//		return this.lastEstimIndividualAbsE;
//	}

	// TODO Stick to a map; take out dynamics.
	public DynamicData<Id<?>> getB() { 
		final DynamicData<Id<?>> result = new DynamicData<>(this.conf.newTimeDiscretization());
		for (Map.Entry<Id<?>, Double> entry : this._B.entrySet()) {
			for (int timeBin = 0; timeBin < result.getBinCnt(); timeBin++) {
				result.put(entry.getKey(), timeBin, entry.getValue());
			}
		}
		return result;
	}
}
