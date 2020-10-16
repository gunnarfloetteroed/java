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

import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeCounts;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

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

	private SummaryStatistics summaryStatistics;

	// -------------------- CONSTRUCTION --------------------

	public DisappointmentAnalyzer(final GreedoConfigGroup conf) {
		this.conf = conf;
		this._B = new LinkedHashMap<>();
		this.summaryStatistics = new SummaryStatistics();
	}

	// -------------------- INTERNALS --------------------

	private double disappointment(final Id<Person> personId, final SpaceTimeCounts<Id<?>> interactions) {
		double result = 0;
		for (Map.Entry<Tuple<Id<?>, Integer>, Double> entry : interactions.entriesView()) {
			final Id<?> location = entry.getKey().getA();
			final double interaction = entry.getValue();
			result += interaction * this._B.getOrDefault(location, 0.0);
		}
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void update(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> currentIndicators,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> anticipatedIndicators,
			final Map<Id<Person>, Double> realizedUtilityChanges,
			final Map<Id<Person>, Double> anticipatedUtilityChanges, final Set<Id<Person>> replannerIds,
			final Map<Id<Person>, SpaceTimeCounts<Id<?>>> interactions, final double stepSize) {

		double sumOfNullE = 0.0;
		double sumOfNaiveE = 0.0;
		double sumOfEstimE = 0.0;

		double sumOfNullE2 = 0.0;
		double sumOfNaiveE2 = 0.0;
		double sumOfEstimE2 = 0.0;

		final Map<Id<?>, Double> deltaB = new LinkedHashMap<>();

		for (Map.Entry<Id<Person>, Double> personEntry : realizedUtilityChanges.entrySet()) {
			final Id<Person> personId = personEntry.getKey();
			final double realizedUtilityChange = personEntry.getValue();

			final double anticipatedUtilityChange;
			if (replannerIds.contains(personId)) {
				anticipatedUtilityChange = anticipatedUtilityChanges.get(personId);
			} else {
				anticipatedUtilityChange = 0.0;
			}

			sumOfNullE += realizedUtilityChange;
			sumOfNullE2 += realizedUtilityChange * realizedUtilityChange;

			final double naiveEn = realizedUtilityChange - anticipatedUtilityChange;
			sumOfNaiveE += naiveEn;
			sumOfNaiveE2 += naiveEn * naiveEn;

			final double estimEn = realizedUtilityChange
					- (anticipatedUtilityChange - this.disappointment(personId, interactions.get(personId)));
			sumOfEstimE += estimEn;
			sumOfEstimE2 += estimEn * estimEn;

			for (Map.Entry<Tuple<Id<?>, Integer>, Double> interactionEntry : interactions.get(personId).entriesView()) {
				final Id<?> location = interactionEntry.getKey().getA();
				double interaction = interactionEntry.getValue();
				deltaB.put(location, deltaB.getOrDefault(location, 0.0) - estimEn * interaction);
			}
		}

		double deltaBSum2 = 0.0;
		for (double val : deltaB.values()) {
			deltaBSum2 += val * val;
		}

		if ((!this.conf.getZeroB()) && (deltaBSum2 >= 1e-8)) {
			final double eta = stepSize * sumOfEstimE2 / deltaBSum2;
			for (Map.Entry<Id<?>, Double> entry : deltaB.entrySet()) {
				final Id<?> location = entry.getKey();
				double newBValue = this._B.getOrDefault(location, 0.0) + eta * entry.getValue();
				if (this.conf.getNonnegativeB()) {
					newBValue = Math.max(0.0, newBValue);
				}
				this._B.put(location, newBValue);
			}
		}

		this.summaryStatistics = new SummaryStatistics(sumOfNullE, sumOfNaiveE, sumOfEstimE, sumOfNullE2, sumOfNaiveE2,
				sumOfEstimE2);
	}

	public Map<Id<?>, Double> getBView() {
		return unmodifiableMap(this._B);
	}

	public SummaryStatistics getSummaryStatistics() {
		return this.summaryStatistics;
	}

	public static class SummaryStatistics {

		public final Double sumOfNullE;
		public final Double sumOfNaiveE;
		public final Double sumOfEstimE;

		public final Double sumOfNullE2;
		public final Double sumOfNaiveE2;
		public final Double sumOfEstimE2;

		private SummaryStatistics() {
			this(null, null, null, null, null, null);
		}
		
		private SummaryStatistics(final Double sumOfNullE, final Double sumOfNaiveE, final Double sumOfEstimE,
				final Double sumOfNullE2, final Double sumOfNaiveE2, final Double sumOfEstimE2) {
			this.sumOfNullE = sumOfNullE;
			this.sumOfNaiveE = sumOfNaiveE;
			this.sumOfEstimE = sumOfEstimE;
			this.sumOfNullE2 = sumOfNullE2;
			this.sumOfNaiveE2 = sumOfNaiveE2;
			this.sumOfEstimE2 = sumOfEstimE2;
		}
	}
}
