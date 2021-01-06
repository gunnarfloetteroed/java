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

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import utils.AdaptiveQuantileEstimator;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class StationaryReplanningRegulator {

	// -------------------- MEMBERS --------------------

	private double stepSize;

	private double meanReplanningRate;

	private final Map<Id<Person>, AdaptiveQuantileEstimator> personId2cn = new LinkedHashMap<>();

	private final Map<Id<Person>, Double> personId2expDn0 = new LinkedHashMap<>();

	private Double avgSqrtOfExpDn0;

	// -------------------- CONSTRUCTION --------------------

	public StationaryReplanningRegulator(final double stepSize, final double meanReplanningRate) {
		this.stepSize = stepSize;
		this.meanReplanningRate = meanReplanningRate;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setStepSize(final double stepSize) {
		this.stepSize = stepSize;
		for (AdaptiveQuantileEstimator cn : this.personId2cn.values()) {
			cn.setStepSize(stepSize);
		}
	}

	public void setMeanReplanningRate(final double meanReplanningRate) {
		this.meanReplanningRate = meanReplanningRate;
	}

	public Map<Id<Person>, Double> getCnView() {
		final Map<Id<Person>, Double> result = new LinkedHashMap<>();
		for (Map.Entry<Id<Person>, AdaptiveQuantileEstimator> entry : this.personId2cn.entrySet()) {
			result.put(entry.getKey(), entry.getValue().getQuantile());
		}
		return unmodifiableMap(result);
	}

	public void update(final Map<Id<Person>, Double> personId2deltaUn0, final Map<Id<Person>, Double> personId2Dn0,
			final Map<Id<Person>, Double> personId2Tn) {

		// Update expected disappointment of not replanning.

		this.avgSqrtOfExpDn0 = 0.0;
		for (Map.Entry<Id<Person>, Double> entry : personId2Dn0.entrySet()) {
			final Id<Person> personId = entry.getKey();
			final double dn0 = entry.getValue();
			final double newExpDn0 = max(0.0,
					(1.0 - this.stepSize) * this.personId2expDn0.getOrDefault(personId, 0.0) + this.stepSize * dn0);
			this.personId2expDn0.put(personId, newExpDn0);
			this.avgSqrtOfExpDn0 += sqrt(newExpDn0);

		}
		this.avgSqrtOfExpDn0 /= this.personId2expDn0.size();
		this.avgSqrtOfExpDn0 = Math.max(1e-8, this.avgSqrtOfExpDn0);

		/*
		 * Update cn replanning thresholds.
		 * 
		 * The version below is *wrong*, it does not set cn such that the target
		 * replanning rate is achieved but sets it (approximately) to the 
		 * targetReplanningRate-percentile of the as the DeltaUn0 distribution.
		 * 
		 * cn.setProbability(1.0 - targetReplanningRate) would be "correct".
		 */

		for (Id<Person> personId : personId2Dn0.keySet()) {
			final double targetReplanningRate = Math.min(1.0,
					this.meanReplanningRate * Math.sqrt(this.personId2expDn0.get(personId)) / this.avgSqrtOfExpDn0);
			AdaptiveQuantileEstimator cn = this.personId2cn.get(personId);
			if (cn == null) {
				cn = new AdaptiveQuantileEstimator(this.stepSize, targetReplanningRate, 0.0);
				this.personId2cn.put(personId, cn);
			} else {
				cn.setProbability(targetReplanningRate);
			}
			cn.update(personId2deltaUn0.get(personId) - personId2Tn.get(personId));
		}
	}
}
