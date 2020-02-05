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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class Utilities {

	// -------------------- INNER Entry CLASS --------------------

	private static class Entry {

		private Double lastRealizedUtility = null; // reference point for change values
		private Double lastExpectedUtilityChange = null;
		private Double lastRealizedUtilityChange = null;

		Entry() {
		}

		void updateExpectedUtility(final double expectedUtility) {
			if (this.lastRealizedUtility != null) {
				this.lastExpectedUtilityChange = expectedUtility - this.lastRealizedUtility;
			}
		}

		void updateRealizedUtility(final double realizedUtility) {
			if (this.lastRealizedUtility != null) {
				this.lastRealizedUtilityChange = realizedUtility - this.lastRealizedUtility;
			}
			this.lastRealizedUtility = realizedUtility;
		}

		Double getLastRealizedUtility() {
			return this.lastRealizedUtility;
		}

		Double getLastExpectedUtilityChange() {
			return this.lastExpectedUtilityChange;
		}

		Double getLastRealizedUtilityChange() {
			return this.lastRealizedUtilityChange;
		}

	}

	// -------------------- INNER SummaryStatistics CLASS --------------------

	class SummaryStatistics {

		final Map<Id<Person>, Double> personId2expectedUtilityChange;
		final Map<Id<Person>, Double> personId2experiencedUtility;
		final public Double realizedUtilitySum;
		final public Double realizedUtilityChangeSum;

		private SummaryStatistics() {
			final Map<Id<Person>, Double> personId2expectedUtilityChange = new LinkedHashMap<>();
			final Map<Id<Person>, Double> personId2experiencedUtility = new LinkedHashMap<>();
			Double realizedUtilitySum = null;
			Double realizedUtilityChangeSum = null;
			for (Map.Entry<Id<Person>, Entry> mapEntry : personId2entry.entrySet()) {
				final Id<Person> personId = mapEntry.getKey();
				final Entry entry = mapEntry.getValue();
				personId2experiencedUtility.put(personId, entry.lastRealizedUtility);
				if (entry.getLastExpectedUtilityChange() != null) {
					personId2expectedUtilityChange.put(personId, entry.getLastExpectedUtilityChange());
				}
				if (entry.getLastRealizedUtility() != null) {
					if (realizedUtilitySum == null) {
						realizedUtilitySum = entry.getLastRealizedUtility();
					} else {
						realizedUtilitySum += entry.getLastRealizedUtility();
					}
				}
				if (entry.getLastRealizedUtilityChange() != null) {
					if (realizedUtilityChangeSum == null) {
						realizedUtilityChangeSum = entry.getLastRealizedUtilityChange();
					} else {
						realizedUtilityChangeSum += entry.getLastRealizedUtilityChange();
					}
				}
			}
			this.personId2expectedUtilityChange = Collections.unmodifiableMap(personId2expectedUtilityChange);
			this.personId2experiencedUtility = Collections.unmodifiableMap(personId2experiencedUtility);
			this.realizedUtilitySum = realizedUtilitySum;
			this.realizedUtilityChangeSum = realizedUtilityChangeSum;
		}
	}

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, Entry> personId2entry = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	Utilities() {
	}

	// -------------------- INTERNALS --------------------

	private Entry getOrCreateEntry(final Id<Person> personId) {
		Entry entry = this.personId2entry.get(personId);
		if (entry == null) {
			entry = new Entry();
			this.personId2entry.put(personId, entry);
		}
		return entry;
	}

	// -------------------- IMPLEMENTATION --------------------

	void updateExpectedUtility(final Id<Person> personId, final double expectedUtility) {
		this.getOrCreateEntry(personId).updateExpectedUtility(expectedUtility);
	}

	void updateRealizedUtility(final Id<Person> personId, final Double realizedUtility) {
		this.getOrCreateEntry(personId).updateRealizedUtility(realizedUtility);
	}

	SummaryStatistics newSummaryStatistics() {
		return new SummaryStatistics();
	}
}
