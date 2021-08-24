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

import static floetteroed.utilities.SetUtils.difference;
import static floetteroed.utilities.SetUtils.intersect;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.nCopies;
import static java.util.Collections.shuffle;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.trustregion.Slot;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.utilities.Tuple;
import utils.ParetoSet;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PopulationSampleManager {

	// -------------------- INNER CLASSES --------------------

	public static class Particle {
		public final Id<Person> id;
		public final double deltaUn0;
		public final List<Slot> anticipatedSlots;
		public final List<Slot> currentSlots;

		final double anticipatedDeltaXNorm2;

		public Particle(Id<Person> personId, double deltaUn0, List<Slot> anticipatedSlots, List<Slot> currentSlots) {
			this.id = personId;
			this.deltaUn0 = deltaUn0;
			this.anticipatedSlots = unmodifiableList(anticipatedSlots);
			this.currentSlots = unmodifiableList(currentSlots);
			this.anticipatedDeltaXNorm2 = this.anticipatedDeltaXInnerProduct(this);
		}

		double anticipatedDeltaXInnerProduct(final Particle other) {
			final Set<Slot> myArrivals = difference(this.anticipatedSlots, this.currentSlots);
			final Set<Slot> myDepartures = difference(this.currentSlots, this.anticipatedSlots);
			final Set<Slot> otherArrivals = difference(other.anticipatedSlots, other.currentSlots);
			final Set<Slot> otherDepartures = difference(other.currentSlots, other.anticipatedSlots);
			return intersect(myArrivals, otherArrivals).size() + intersect(myDepartures, otherDepartures).size()
					- intersect(myArrivals, otherDepartures).size() - intersect(myDepartures, otherArrivals).size();
		}
	}

	// -------------------- CONSTANTS --------------------

	private final Random random = MatsimRandom.getRandom();

	private final Set<Id<Link>> allLinkIds;

	private final GreedoConfigGroup greedoConfig;

	private final int _R;

	private final int _S = 10;

	private final int _T = 10;

	// -------------------- MEMBERS --------------------

	// For each key, there is a non-null value.
	private final Map<Id<Person>, List<Particle>> personId2particles = new LinkedHashMap<>();

	private int replications;

	private double trustRegion;

	// -------------------- CONSTRUCTION --------------------

	PopulationSampleManager(final Scenario scenario, final GreedoConfigGroup greedoConfig) {
		this.allLinkIds = unmodifiableSet(new LinkedHashSet<>(scenario.getNetwork().getLinks().keySet()));
		this.greedoConfig = greedoConfig;
		this._R = greedoConfig.getIterationReplications();
		this.trustRegion = greedoConfig.getTrustRegion();
		this.replications = 0;
	}

	// -------------------- INTERNALS --------------------

	public static List<Slot> createSlots(final SpaceTimeIndicators<Id<?>> indicators) {
		if (indicators != null) {
			final List<Slot> result = new ArrayList<>(indicators.size());
			for (int timeBin = 0; timeBin < indicators.getTimeBinCnt(); timeBin++) {
				final int timeBinCopy = timeBin;
				indicators.getVisits(timeBin).forEach(v -> {
					result.add(new Slot(v.spaceObject, timeBinCopy));
				});
			}
			return result;
		} else {
			return new ArrayList<>(0);
		}
	}

	private Set<Slot> allAnticipatedSlots(final List<Particle> particles) {
		final Set<Slot> result = new LinkedHashSet<>(
				particles.stream().mapToInt(p -> p.anticipatedSlots.size()).sum() / 2);
		for (Particle particle : particles) {
			result.addAll(particle.anticipatedSlots);
		}
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	void addVisits(final Id<Person> personId, final SpaceTimeIndicators<Id<?>> anticipatedVisits,
			final SpaceTimeIndicators<Id<?>> currentVisits, final double deltaUn0) {
		final List<Slot> anticipatedSlots = this.createSlots(anticipatedVisits);
		final List<Slot> currentSlots = this.createSlots(currentVisits);
		final Particle particle = new Particle(personId, deltaUn0, anticipatedSlots, currentSlots);
		this.personId2particles
				.computeIfAbsent(personId, id -> new ArrayList<>(this.greedoConfig.getIterationReplications()))
				.add(particle);
	}

	void registerCompletedReplication() {
		this.replications++;
	}

	Set<Id<Person>> selectReplanners() {
		// return this.selectReplanners_2021_05_10();
		// return this.selectReplanners_2021_05_11();
		// return this.selectReplanners_2021_05_19();
		// return this.selectReplanners_2021_05_24();
		// return this.selectReplanners_2021_05_26();
		return this.selectReplanners_2021_05_26b();
	}

	private Map<Slot, Double> slot2weightedUsage(final List<Particle> particles,
			final List<Double> normalizedReplicationWeights) {
		final Map<Slot, Double> result = new LinkedHashMap<>();
		for (int r = 0; r < this._R; r++) {
			final double w_r = normalizedReplicationWeights.get(r);
			for (Slot slot : particles.get(r).anticipatedSlots) {
				result.put(slot, result.getOrDefault(slot, 0.0) + w_r);
			}
		}
		return result;
	}

	private Set<Id<Person>> selectReplannersFromWeightedReplications(final List<Double> normalizedReplicationWeights) {

		final List<Tuple<Id<Person>, Double>> personIdIdAndWeightedMeanDeltaUn0 = new ArrayList<>(
				this.personId2particles.size());
		for (Map.Entry<Id<Person>, List<Particle>> e : this.personId2particles.entrySet()) {
			final Id<Person> personId = e.getKey();
			final List<Particle> particles = e.getValue();
			double weightedMeanDeltaUn0 = 0.0;
			for (int r = 0; r < this._R; r++) {
				weightedMeanDeltaUn0 += normalizedReplicationWeights.get(r) * particles.get(r).deltaUn0;
			}
			personIdIdAndWeightedMeanDeltaUn0.add(new Tuple<>(personId, weightedMeanDeltaUn0));
		}
		shuffle(personIdIdAndWeightedMeanDeltaUn0, this.random); // to break symmetries
		sort(personIdIdAndWeightedMeanDeltaUn0, new Comparator<Tuple<Id<Person>, Double>>() {
			@Override
			public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
				return Double.compare(tuple2.getB(), tuple1.getB());
			}
		});

		final Map<Slot, Double> slot2allVisits = new LinkedHashMap<>(
				this.allLinkIds.size() * this.greedoConfig.getBinCnt());
		for (Id<Link> linkId : this.allLinkIds) {
			for (int timeBin = 0; timeBin < this.greedoConfig.getBinCnt(); timeBin++) {
				slot2allVisits.put(new Slot(linkId, timeBin), 0.0);
			}
		}

		final Set<Id<Person>> replannerIds = new LinkedHashSet<>();

		for (Tuple<Id<Person>, Double> candiateTuple : personIdIdAndWeightedMeanDeltaUn0) {
			final Id<Person> personId = candiateTuple.getA();
			final Map<Slot, Double> slot2usage = this.slot2weightedUsage(this.personId2particles.get(personId),
					normalizedReplicationWeights);

			double availableRelativeSpace = 1.0;
			for (Map.Entry<Slot, Double> e : slot2usage.entrySet()) {
				final Slot slot = e.getKey();
				final double requiredSpace = e.getValue();
				if (requiredSpace > 0.0) {
					final double availableSpace = this.greedoConfig.getTrustRegion() - slot2allVisits.get(slot);
					availableRelativeSpace = min(availableRelativeSpace, availableSpace / requiredSpace);
				}
			}

			if (availableRelativeSpace > 0.0) {
				for (Map.Entry<Slot, Double> e : slot2usage.entrySet()) {
					final Slot slot = e.getKey();
					final double requiredSpace = e.getValue();
					slot2allVisits.put(slot, slot2allVisits.get(slot) + availableRelativeSpace * requiredSpace);
				}
				if (this.random.nextDouble() < availableRelativeSpace) {
					replannerIds.add(personId);
				}
			}
		}

		return unmodifiableSet(replannerIds);
	}

	Set<Id<Person>> selectReplanners_2021_05_26b() {

		if (this.replications < this._R) {
			return unmodifiableSet(new LinkedHashSet<>());
		}

		final List<Set<Id<Person>>> replannerIdsPerReplication = new ArrayList<>(this._R);

		final ParetoSet<Integer> paretoSet = new ParetoSet<>();
		double[] deltaU0s = new double[this._R];
		double[] maxSlotUsages = new double[this._R];
		for (int r1 = 0; r1 < this._R; r1++) { // defines the considered re-planners

			final List<Double> tmpWeights = new ArrayList<>(nCopies(this._R, 0.0));
			tmpWeights.set(r1, 1.0);
			replannerIdsPerReplication.add(this.selectReplannersFromWeightedReplications(tmpWeights));

			double deltaU0Sum = 0.0;
			double maxSlotUsageSum = 0.0;
			for (int r2 = 0; r2 < this._R; r2++) { // evaluate against all replications
				if ((r2 != r1) || (this._R == 1)) {
					final Map<Slot, Integer> slot2usage = new LinkedHashMap<>();
					for (Id<Person> replannerId : replannerIdsPerReplication.get(r1)) {
						final Particle particle = this.personId2particles.get(replannerId).get(r2);
						deltaU0Sum += particle.deltaUn0;
						for (Slot slot : particle.anticipatedSlots) {
							slot2usage.put(slot, slot2usage.getOrDefault(slot, 0) + 1);
						}
					}
					final double maxSlotUsage = slot2usage.values().stream().mapToInt(cnt -> cnt).max().getAsInt();
					maxSlotUsageSum += maxSlotUsage;
				}
			}

			deltaU0s[r1] = deltaU0Sum;
			maxSlotUsages[r1] = maxSlotUsageSum;
			Logger.getLogger(this.getClass()).info("STATS R" + r1 + ": " + deltaU0s[r1] + ", " + maxSlotUsages[r1]);
			paretoSet.addPoint(-deltaU0s[r1], maxSlotUsages[r1], r1);
		}

		final int[] paretoPointsPerReplication = new int[this._R];
		for (ParetoSet.Point<Integer> point : paretoSet.paretoSet) {
			paretoPointsPerReplication[point.ref]++;
		}
		Logger.getLogger(this.getClass()).info("PARETO COUNTS:");
		for (int cnt : paretoPointsPerReplication) {
			Logger.getLogger(this.getClass()).info(" " + cnt);
		}
		System.out.println();

		final int selectedReplication = new ArrayList<>(paretoSet.paretoSet)
				.get(this.random.nextInt(paretoSet.paretoSet.size())).ref;
		Logger.getLogger(this.getClass()).info(" -> selected replication: " + selectedReplication);

//		Integer bestReplication = null;
//		double largestRatio = Double.NEGATIVE_INFINITY;
//		for (int r = 0; r < this._R; r++) {
//			double candRatio = deltaU0s[r] / maxSlotUsages[r];
//			if (candRatio > largestRatio) {
//				bestReplication = r;
//				largestRatio = candRatio;
//			}
//		}
//		return unmodifiableSet(replannerIdsPerReplication.get(bestReplication));
//		return unmodifiableSet(replannerIdsPerReplication.get(this._R - 1));

		this.personId2particles.clear();
		this.replications = 0;
		return unmodifiableSet(replannerIdsPerReplication.get(selectedReplication));
	}

	Set<Id<Person>> selectReplanners_2021_05_26() {

		if (this.replications < this._R) {
			return unmodifiableSet(new LinkedHashSet<>());
		}

		final List<Map<Id<Person>, Double>> personId2ReplanProbaList = new ArrayList<>(this._R);
		for (int r = 0; r < this._R; r++) {
			final Map<Id<Person>, Double> personId2replanProba = new LinkedHashMap<>();
			final List<Double> tmpWeights = new ArrayList<>(nCopies(this._R, 0.0));
			tmpWeights.set(r, 1.0);
			for (int s = 0; s < this._S; s++) {
				for (Id<Person> tmpReplannerId : this.selectReplannersFromWeightedReplications(tmpWeights)) {
					personId2replanProba.put(tmpReplannerId,
							personId2replanProba.getOrDefault(tmpReplannerId, 0.0) + 1.0 / this._S);
				}
			}
			personId2ReplanProbaList.add(personId2replanProba);
		}

		Set<Id<Person>> replannerIds = null;

		final List<Double> weights = new ArrayList<>(nCopies(this._R, 1.0 / this._R));
//		final List<Double> weights = new ArrayList<>(nCopies(this._R, 0.0));
//		weights.set(0, 1.0);

		for (int t = 0; t < _T; t++) {
			replannerIds = this.selectReplannersFromWeightedReplications(weights);

			final StringBuffer line = new StringBuffer("REPLICATION WEIGHTS: ");
			for (int r = 0; r < this._R; r++) {
				line.append(weights.get(r));
				line.append("\t");
			}
			line.append("->\t#REPLANNERS: ");
			line.append(replannerIds.size());
			System.out.println(line.toString());

			if (t < this._T - 1) {
				for (int r = 0; r < this._R; r++) {

					final Map<Id<Person>, Double> personId2replanProba = personId2ReplanProbaList.get(r);

					double llSum = 0.0;
					for (Id<Person> personId : this.personId2particles.keySet()) {
						double replanProba = min(1.0 - 1e-3,
								max(1e-3, personId2replanProba.getOrDefault(personId, 0.0)));
						if (replannerIds.contains(personId)) {
							llSum += Math.log(replanProba);
						} else {
							llSum += Math.log(1.0 - replanProba);
						}
					}
					weights.set(r, llSum);
				}

				final double maxLLSum = weights.stream().mapToDouble(x -> x).max().getAsDouble();
				for (int r = 0; r < this._R; r++) {
					weights.set(r, Math.exp(weights.get(r) - maxLLSum));
				}

				final double weightSum = weights.stream().mapToDouble(x -> x).sum();
				for (int r = 0; r < this._R; r++) {
					weights.set(r, weights.get(r) / weightSum);
				}
			}
		}

		this.personId2particles.clear();
		this.replications = 0;
		return unmodifiableSet(replannerIds);
	}

	Set<Id<Person>> selectReplanners_2021_05_24() {

		if (this.replications < this.greedoConfig.getIterationReplications()) {
			return unmodifiableSet(new LinkedHashSet<>());
		}

		final List<Tuple<Id<Person>, Double>> replanningCandidateIdsIdAndDeltaUn0Sum = new ArrayList<>(
				this.personId2particles.size());
		for (Map.Entry<Id<Person>, List<Particle>> e : this.personId2particles.entrySet()) {
			final Id<Person> personId = e.getKey();
			final double deltaUn0Sum = e.getValue().stream().mapToDouble(p -> p.deltaUn0).sum();
			replanningCandidateIdsIdAndDeltaUn0Sum.add(new Tuple<>(personId, deltaUn0Sum));
		}
		shuffle(replanningCandidateIdsIdAndDeltaUn0Sum, this.random); // to break symmetries
		sort(replanningCandidateIdsIdAndDeltaUn0Sum, new Comparator<Tuple<Id<Person>, Double>>() {
			@Override
			public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
				return Double.compare(tuple2.getB(), tuple1.getB());
			}
		});

		final Map<Slot, Double> slot2allVisits = new LinkedHashMap<>(
				this.allLinkIds.size() * this.greedoConfig.getBinCnt());
		for (Id<Link> linkId : this.allLinkIds) {
			for (int timeBin = 0; timeBin < this.greedoConfig.getBinCnt(); timeBin++) {
				slot2allVisits.put(new Slot(linkId, timeBin), 0.0);
			}
		}

		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		for (Tuple<Id<Person>, Double> replanningCandidateIdAndDeltaUn0Sum : replanningCandidateIdsIdAndDeltaUn0Sum) {
			final Id<Person> personId = replanningCandidateIdAndDeltaUn0Sum.getA();
			final Set<Slot> allAnticipatedSlots = this.allAnticipatedSlots(this.personId2particles.get(personId));

			double availableSpace = min(1.0, this.greedoConfig.getTrustRegion());
			for (Slot slot : allAnticipatedSlots) {
				availableSpace = min(availableSpace, this.greedoConfig.getTrustRegion() - slot2allVisits.get(slot));
			}

			if (availableSpace > 0.0) {
				for (Slot slot : allAnticipatedSlots) {
					slot2allVisits.put(slot, slot2allVisits.get(slot) + availableSpace);

				}
				if (this.random.nextDouble() < availableSpace) {
					replanners.add(personId);
				}
			}
		}

		this.personId2particles.clear();
		this.replications = 0;
		return unmodifiableSet(replanners);
	}

	Set<Id<Person>> selectReplanners_2021_05_19() {

		if (this.replications < this.greedoConfig.getIterationReplications()) {
			return unmodifiableSet(new LinkedHashSet<>());
		}

		final List<Id<Person>> replanningCandidateIds; // horrible implementation below
		{
			final List<Tuple<Id<Person>, Double>> personIdAndEstimDeltaUn0 = new ArrayList<>(
					this.personId2particles.size());
			for (Map.Entry<Id<Person>, List<Particle>> e : this.personId2particles.entrySet()) {
				final Id<Person> personId = e.getKey();
				final List<Particle> particles = e.getValue();
				final Particle lastParticle = particles.get(particles.size() - 1);

				double estimDeltaUn0Sum = lastParticle.deltaUn0;
				for (int r = 0; r < particles.size() - 2; r++) {
					final Particle particle = particles.get(r);
					if (particle.anticipatedDeltaXNorm2 > 0) {
						estimDeltaUn0Sum += particle.deltaUn0 / particle.anticipatedDeltaXNorm2
								* particle.anticipatedDeltaXInnerProduct(lastParticle);
					}
				}
				personIdAndEstimDeltaUn0.add(new Tuple<>(personId, estimDeltaUn0Sum / particles.size()));
			}
			shuffle(personIdAndEstimDeltaUn0, this.random); // to break symmetries
			sort(personIdAndEstimDeltaUn0, new Comparator<Tuple<Id<Person>, Double>>() {
				@Override
				public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
					return Double.compare(tuple2.getB(), tuple1.getB());
				}
			});
			replanningCandidateIds = new ArrayList<>(personIdAndEstimDeltaUn0.size());
			personIdAndEstimDeltaUn0.forEach(tuple -> replanningCandidateIds.add(tuple.getA()));
		}

		final Map<Slot, List<Double>> slot2allVisits = new LinkedHashMap<>(
				this.allLinkIds.size() * this.greedoConfig.getBinCnt());
		for (Id<Link> linkId : this.allLinkIds) {
			for (int timeBin = 0; timeBin < this.greedoConfig.getBinCnt(); timeBin++) {
				final List<Double> visits = new ArrayList<>(nCopies(this.greedoConfig.getIterationReplications(), 0.0));
				slot2allVisits.put(new Slot(linkId, timeBin), visits);
			}
		}

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		for (Id<Person> personId : replanningCandidateIds) {

			final Map<Slot, List<Integer>> slot2candVisits = new LinkedHashMap<>();

			final int replication = this.greedoConfig.getIterationReplications() - 1;

			double maxSlotUsage = 0.0;
			for (Slot slot : this.personId2particles.get(personId).get(replication).anticipatedSlots) {
				List<Integer> candVisits = slot2candVisits.get(slot);
				if (candVisits == null) {
					candVisits = new ArrayList<>(nCopies(this.greedoConfig.getIterationReplications(), 0));
					slot2candVisits.put(slot, candVisits);
				}
				candVisits.set(replication, candVisits.get(replication) + 1); // self
				maxSlotUsage = max(maxSlotUsage, slot2allVisits.get(slot).get(replication)); // only others
			}

			final double availableSpace = min(1.0, this.trustRegion - maxSlotUsage);
			if (availableSpace > 0.0) {
				for (Slot slot : this.personId2particles.get(personId).get(replication).anticipatedSlots) {
					slot2allVisits.get(slot).set(replication,
							slot2allVisits.get(slot).get(replication) + availableSpace);
				}
				if (this.random.nextDouble() < availableSpace) {
					replanners.add(personId);
				}
			}
		}

		this.personId2particles.clear();
		this.replications = 0;
		return unmodifiableSet(replanners);
	}

	Set<Id<Person>> selectReplanners_2021_05_11() {

		if (this.replications < this.greedoConfig.getIterationReplications()) {
			return unmodifiableSet(new LinkedHashSet<>());
		}

		final List<Id<Person>> replanningCandidateIds; // horrible implementation below
		{
			final List<Tuple<Id<Person>, Double>> personIdAndAvgDeltaUn0 = new ArrayList<>(
					this.personId2particles.size());
			for (Map.Entry<Id<Person>, List<Particle>> e : this.personId2particles.entrySet()) {
				final Id<Person> personId = e.getKey();
				final double avgDeltaUn0 = e.getValue().stream().mapToDouble(p -> p.deltaUn0).average().getAsDouble();
				personIdAndAvgDeltaUn0.add(new Tuple<>(personId, avgDeltaUn0));
			}
			shuffle(personIdAndAvgDeltaUn0, this.random); // to break symmetries
			sort(personIdAndAvgDeltaUn0, new Comparator<Tuple<Id<Person>, Double>>() {
				@Override
				public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
					return Double.compare(tuple2.getB(), tuple1.getB());
				}
			});
			replanningCandidateIds = new ArrayList<>(personIdAndAvgDeltaUn0.size());
			personIdAndAvgDeltaUn0.forEach(tuple -> replanningCandidateIds.add(tuple.getA()));
		}

		final Map<Slot, List<Double>> slot2allVisits = new LinkedHashMap<>(
				this.allLinkIds.size() * this.greedoConfig.getBinCnt());
		for (Id<Link> linkId : this.allLinkIds) {
			for (int timeBin = 0; timeBin < this.greedoConfig.getBinCnt(); timeBin++) {
				final List<Double> visits = new ArrayList<>(nCopies(this.greedoConfig.getIterationReplications(), 0.0));
				slot2allVisits.put(new Slot(linkId, timeBin), visits);
			}
		}

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		for (Id<Person> personId : replanningCandidateIds) {

			final Map<Slot, List<Integer>> slot2candVisits = new LinkedHashMap<>();
			double sumOfMaxSlotUsages = 0.0;
			for (int replication = 0; replication < this.greedoConfig.getIterationReplications(); replication++) {
				double maxSlotUsage = 0.0;
				for (Slot slot : this.personId2particles.get(personId).get(replication).anticipatedSlots) {
					List<Integer> candVisits = slot2candVisits.get(slot);
					if (candVisits == null) {
						candVisits = new ArrayList<>(nCopies(this.greedoConfig.getIterationReplications(), 0));
						slot2candVisits.put(slot, candVisits);
					}
					candVisits.set(replication, candVisits.get(replication) + 1); // self
					maxSlotUsage = max(maxSlotUsage, slot2allVisits.get(slot).get(replication)); // only others
				}
				sumOfMaxSlotUsages += maxSlotUsage;
			}

			final double availableSpace = min(1.0,
					this.trustRegion - sumOfMaxSlotUsages / this.greedoConfig.getIterationReplications());
			if (availableSpace > 0.0) {
				for (int replication = 0; replication < this.greedoConfig.getIterationReplications(); replication++) {
					for (Slot slot : this.personId2particles.get(personId).get(replication).anticipatedSlots) {
						slot2allVisits.get(slot).set(replication,
								slot2allVisits.get(slot).get(replication) + availableSpace);
					}
				}
				if (this.random.nextDouble() < availableSpace) {
					replanners.add(personId);
				}
			}
		}

		this.personId2particles.clear();
		this.replications = 0;
		return unmodifiableSet(replanners);
	}

	Set<Id<Person>> selectReplanners_2021_05_10() {

		if (this.replications < this.greedoConfig.getIterationReplications()) {
			return unmodifiableSet(new LinkedHashSet<>());
		}

		final Map<Id<Person>, Double> personId2replanProbaSum = new LinkedHashMap<>(this.personId2particles.size());

		for (int replication = 0; replication < this.greedoConfig.getIterationReplications(); replication++) {

			final Map<Slot, Double> slot2remainingSpace = new LinkedHashMap<>(
					this.allLinkIds.size() * this.greedoConfig.getBinCnt());
			for (Id<Link> linkId : this.allLinkIds) {
				for (int timeBin = 0; timeBin < this.greedoConfig.getBinCnt(); timeBin++) {
					slot2remainingSpace.put(new Slot(linkId, timeBin), this.trustRegion);
				}
			}

			final List<Id<Person>> replanningCandidateIds; // horrible implementation below
			{
				final List<Tuple<Id<Person>, Double>> personIdAndDeltaUn0 = new ArrayList<>(
						this.personId2particles.size());
				for (Map.Entry<Id<Person>, List<Particle>> e : this.personId2particles.entrySet()) {
					final Id<Person> personId = e.getKey();
					final Particle particle = e.getValue().get(replication);
					personIdAndDeltaUn0.add(new Tuple<>(personId, particle.deltaUn0));
				}
				shuffle(personIdAndDeltaUn0, this.random); // to break symmetries
				sort(personIdAndDeltaUn0, new Comparator<Tuple<Id<Person>, Double>>() {
					@Override
					public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
						return Double.compare(tuple2.getB(), tuple1.getB());
					}
				});
				replanningCandidateIds = new ArrayList<>(personIdAndDeltaUn0.size());
				personIdAndDeltaUn0.forEach(tuple -> replanningCandidateIds.add(tuple.getA()));
			}

			for (Id<Person> personId : replanningCandidateIds) {

				final Map<Slot, Integer> slot2anticipatedVisits = new LinkedHashMap<>();
				for (Particle particle : this.personId2particles.get(personId)) {
					for (Slot slot : particle.anticipatedSlots) {
						slot2anticipatedVisits.put(slot, 1 + slot2anticipatedVisits.getOrDefault(slot, 0));
					}
				}

				double minRelativeAvailableSpace = 1.0;
				for (Map.Entry<Slot, Integer> entry : slot2anticipatedVisits.entrySet()) {
					final Slot slot = entry.getKey();
					final double neededSpace = entry.getValue(); // Is one or larger.
					minRelativeAvailableSpace = min(minRelativeAvailableSpace,
							slot2remainingSpace.get(slot) / neededSpace);
				}

				if (minRelativeAvailableSpace > 0.0) {
					for (Map.Entry<Slot, Integer> entry : slot2anticipatedVisits.entrySet()) {
						final Slot slot = entry.getKey();
						final double neededSpace = entry.getValue();
						slot2remainingSpace.put(slot,
								max(0.0, slot2remainingSpace.get(slot) - minRelativeAvailableSpace * neededSpace));
					}
					personId2replanProbaSum.put(personId,
							minRelativeAvailableSpace + personId2replanProbaSum.getOrDefault(personId, 0.0));
				}
			}
		}

		// TODO set size could be anticipated based on sum of replanning probabilities
		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		for (Id<Person> personId : this.personId2particles.keySet()) {
			if (this.random.nextDouble() < (personId2replanProbaSum.getOrDefault(personId, 0.0)
					/ this.greedoConfig.getIterationReplications())) {
				replanners.add(personId);
			}
		}

		this.personId2particles.clear();
		this.replications = 0;
		return unmodifiableSet(replanners);
	}
}
