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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import floetteroed.utilities.statisticslogging.Statistic;

/**
 *
 * @author Gunnar Flötteröd
 * 
 */
public class Ages {

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup greedoConfig;

	private Map<Id<Person>, Integer> personId2age = null;

	private List<Integer> sortedAges = null;

	private double averageAge;

	private Map<Id<Person>, Double> personId2weight = null;

	private double averageWeight;

	// -------------------- CONSTRUCTION --------------------

	Ages(final Set<Id<Person>> populationIds, final GreedoConfigGroup greedoConfig) {
		this.personId2age = Collections
				.unmodifiableMap(populationIds.stream().collect(Collectors.toMap(id -> id, id -> 0)));
		this.greedoConfig = greedoConfig;
		this.internalUpdate();
	}

	// -------------------- INTERNALS --------------------

	private void internalUpdate() {
		this.sortedAges = new ArrayList<>(this.personId2age.values());
		Collections.sort(this.sortedAges);
		this.averageAge = this.sortedAges.stream().mapToDouble(age -> age).average().getAsDouble();
		this.personId2weight = Collections.unmodifiableMap(this.personId2age.entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> this.greedoConfig.getAgeWeight(entry.getValue()))));
		this.averageWeight = this.personId2weight.values().stream().mapToDouble(weight -> weight).average()
				.getAsDouble();
	}

	// -------------------- IMPLEMENTATION --------------------

	void update(final Set<Id<Person>> replanners) {
		this.personId2age = Collections
				.unmodifiableMap(this.personId2age.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(),
						entry -> replanners.contains(entry.getKey()) ? 0 : entry.getValue() + 1)));
		this.internalUpdate();
	}

	Map<Id<Person>, Integer> getAgesView() {
		return this.personId2age;
	}

	Map<Id<Person>, Double> getWeightsView() {
		return this.personId2weight;
	}

	// -------------------- STATISTICS --------------------

	Statistic<LogDataWrapper> newAvgAgeStatistic() {
		return new Statistic<LogDataWrapper>() {
			@Override
			public String label() {
				return "AvgAge";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(averageAge);
			}
		};
	}

	Statistic<LogDataWrapper> newAvgAgeWeightStatistic() {
		return new Statistic<LogDataWrapper>() {
			@Override
			public String label() {
				return "AvgAgeWeight";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(averageWeight);
			}
		};
	}

	Statistic<LogDataWrapper> newAgePercentile(final int percent) {

		return new Statistic<LogDataWrapper>() {
			@Override
			public String label() {
				return "agePercentile" + percent;
			}

			@Override
			public String value(final LogDataWrapper logData) {
				if (sortedAges == null) {
					return Statistic.toString(null);
				} else {
					final int index = max(0, min(sortedAges.size() - 1, (int) ((percent / 100.0) * sortedAges.size())));
					return Statistic.toString(sortedAges.get(index));
				}
			}
		};
	}
}
