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
package org.matsim.contrib.greedo.datastructures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;

/**
 * Stores real-valued (weighted counting) data in a map with keys consisting of
 * (space, time) tuples. "Space" is represented by the generic class L (e.g. a
 * network link).
 * 
 * This minimal class exists only to speed up numerical operations in
 * {@link ScoreUpdater} that require iterating over all map entries. For a less
 * memory-intensive implementation, see {@link SpaceTimeIndicators}.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
public class SpaceTimeCounts<L> {

	// -------------------- MEMBERS --------------------

	private final Map<Tuple<L, Integer>, Double> data = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public SpaceTimeCounts(final SpaceTimeIndicators<L> parent, final boolean useParticleWeight,
			final boolean useSlotWeight) {
		if (parent != null) {
			for (int timeBin = 0; timeBin < parent.getTimeBinCnt(); timeBin++) {
				for (SpaceTimeIndicators<L>.Visit visit : parent.getVisits(timeBin)) {
					this.add(newKey(visit.spaceObject, timeBin), visit.weight(useParticleWeight, useSlotWeight));
				}
			}
		}
	}

	// -------------------- INTERNALS --------------------

	private static <L> Tuple<L, Integer> newKey(final L spaceObj, final Integer timeBin) {
		return new Tuple<>(spaceObj, timeBin);
	}

	private double get(final Tuple<L, Integer> key) {
		if (this.data.containsKey(key)) {
			return this.data.get(key);
		} else {
			return 0.0;
		}
	}

	private void set(final Tuple<L, Integer> key, final double value) {
		if (value == 0.0) {
			this.data.remove(key);
		} else {
			this.data.put(key, value);
		}
	}

	private void add(final Tuple<L, Integer> key, final double addend) {
		this.set(key, this.get(key) + addend);
	}

	// -------------------- IMPLEMENTATION --------------------

	public Set<Map.Entry<Tuple<L, Integer>, Double>> entriesView() {
		return Collections.unmodifiableSet(this.data.entrySet());
	}

	public void subtract(final SpaceTimeCounts<L> other) {
		for (Map.Entry<Tuple<L, Integer>, Double> otherEntry : other.data.entrySet()) {
			this.set(otherEntry.getKey(), this.get(otherEntry.getKey()) - otherEntry.getValue());
		}
	}

	// NEW

	public double sumOfSquareEntries() {
		double result = 0.0;
		for (double val : this.data.values()) {
			result += val * val;
		}
		return result;
	}
}
