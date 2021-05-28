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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class ExperimentalCnSetter {

	private final String logFile = "accLog.txt";

	private final GreedoConfigGroup conf;

	private final Map<Id<Person>, Double> personId2MeanDeltaUn0 = new LinkedHashMap<>();

	private final Map<Id<Person>, Double> personId2PrevDeltaUn0 = new LinkedHashMap<>();

	private final Map<Id<Person>, Double> personId2MeanDeteriorationIfNotReplanning = new LinkedHashMap<>();

	private Double eta = null;
	private Double lastTargetLambda = null;

	ExperimentalCnSetter(final GreedoConfigGroup conf) {
		this.conf = conf;
		try {
			FileUtils.writeStringToFile(new File(this.logFile),
					"<deltaUn0>\t<meanDeltaUn0>\treplanningRate\ttargetRate\tmeanDet\teta\n", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Map.Entry<Id<Person>, Double>> personsSortedDescendingByReplanningCriterion(
			final Map<Id<Person>, Double> personId2DeltaUn0) {
		final List<Map.Entry<Id<Person>, Double>> entryList = new ArrayList<>(personId2DeltaUn0.size());
		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			entryList.add(new Map.Entry<Id<Person>, Double>() {
				final Id<Person> personId = entry.getKey();
				final double value = entry.getValue() - personId2MeanDeltaUn0.getOrDefault(personId, 0.0)
						+ 0.5 * personId2MeanDeteriorationIfNotReplanning.getOrDefault(personId, 0.0);

				@Override
				public Id<Person> getKey() {
					return this.personId;
				}

				@Override
				public Double getValue() {
					return this.value;
				}

				@Override
				public Double setValue(Double value) {
					throw new UnsupportedOperationException();
				}
			});
		}
		Collections.sort(entryList, new Comparator<Map.Entry<?, Double>>() {
			@Override
			public int compare(final Entry<?, Double> o1, final Entry<?, Double> o2) {
				return o2.getValue().compareTo(o1.getValue()); // largest values first
			}
		});
		return Collections.unmodifiableList(entryList);
	}

	private int thresholdIndex(List<Entry<Id<Person>, Double>> compareToEtaValues, final double targetLambda) {
		return Math.min((int) (targetLambda * compareToEtaValues.size()), compareToEtaValues.size() - 1);
	}

	private double thresholdEta(final Map<Id<Person>, Double> personId2DeltaUn0, final double targetLambda) {
		final List<Entry<Id<Person>, Double>> compareToEtaValues = this
				.personsSortedDescendingByReplanningCriterion(personId2DeltaUn0);
		return compareToEtaValues.get(thresholdIndex(compareToEtaValues, targetLambda)).getValue();
	}

	void update(final Set<Id<Person>> replanners, final Map<Id<Person>, Double> personId2DeltaUn0,
			final double innoWeight, final double targetLambda) {

		this.lastTargetLambda = targetLambda;
		final double realizedLambda = ((double) replanners.size()) / personId2DeltaUn0.size();

		final double intermedEta = this.thresholdEta(personId2DeltaUn0, targetLambda);
		if (this.eta == null) {
			this.eta = intermedEta;
		} else {
			this.eta = innoWeight * intermedEta + (1.0 - innoWeight) * this.eta;
		}

		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			final Id<Person> personId = entry.getKey();
			final double deltaUn0 = entry.getValue();

			final Double oldMeanDeltaUn0 = this.personId2MeanDeltaUn0.get(personId);
			if (oldMeanDeltaUn0 == null) {
				this.personId2MeanDeltaUn0.put(personId, entry.getValue());
			} else {
				this.personId2MeanDeltaUn0.put(personId, innoWeight * deltaUn0 + (1.0 - innoWeight) * oldMeanDeltaUn0);
			}

			if (!replanners.contains(personId)) {
				final Double prevDeltaUn0 = this.personId2PrevDeltaUn0.get(personId);
				if (prevDeltaUn0 != null) {
					final double deterioration = deltaUn0 - prevDeltaUn0;
					final Double oldMeanDeterioration = this.personId2MeanDeteriorationIfNotReplanning.get(personId);
					if (oldMeanDeterioration == null) {
						this.personId2MeanDeteriorationIfNotReplanning.put(personId, deterioration);
					} else {
						this.personId2MeanDeteriorationIfNotReplanning.put(personId,
								innoWeight * deterioration + (1.0 - innoWeight) * oldMeanDeltaUn0);
					}
				}
			}

			this.personId2PrevDeltaUn0.put(personId, deltaUn0);
		}

		try {
			FileUtils.writeStringToFile(new File(this.logFile),
					personId2DeltaUn0.values().stream().mapToDouble(x -> x).average().getAsDouble() + "\t"
							+ this.personId2MeanDeltaUn0.values().stream().mapToDouble(x -> x).average().getAsDouble()
							+ "\t" + realizedLambda + "\t" + targetLambda + "\t"
							+ ((this.personId2MeanDeteriorationIfNotReplanning.size() == 0) ? ""
									: this.personId2MeanDeteriorationIfNotReplanning.values().stream()
											.mapToDouble(x -> x).average().getAsDouble())
							+ "\t" + this.eta + "\n",
					true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	Set<Id<Person>> selectReplanners(final Map<Id<Person>, Double> personId2DeltaUn0) {
		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		if ((!this.conf.getSbaytify() && (this.eta == null))
				|| (this.conf.getSbaytify() && (this.lastTargetLambda == null))) {
			replanners.addAll(personId2DeltaUn0.keySet());
		} else {
			if (this.conf.getSbaytify()) {
				final List<Map.Entry<Id<Person>, Double>> descSortedEntries = this
						.personsSortedDescendingByReplanningCriterion(personId2DeltaUn0);
				final int threshIndex = this.thresholdIndex(descSortedEntries, this.lastTargetLambda);
				for (int i = 0; i <= threshIndex; i++) {
					replanners.add(descSortedEntries.get(i).getKey());
				}
			} else {
				for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
					final Id<Person> personId = entry.getKey();
					if (entry.getValue() >= this.eta + this.personId2MeanDeltaUn0.getOrDefault(personId, 0.0)
							- 0.5 * this.personId2MeanDeteriorationIfNotReplanning.getOrDefault(personId, 0.0)) {
						replanners.add(personId);
					}
				}
			}
		}
		return replanners;
	}

}
