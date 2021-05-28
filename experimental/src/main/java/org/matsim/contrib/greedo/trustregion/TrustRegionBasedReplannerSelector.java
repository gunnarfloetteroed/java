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
package org.matsim.contrib.greedo.trustregion;

import static floetteroed.utilities.DynamicDataUtils.newWeightedSum;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Collections.shuffle;
import static java.util.Collections.sort;
import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.newTotals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.GreedoConfigGroup;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.utilities.DynamicData;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TrustRegionBasedReplannerSelector {

	// -------------------- INNER CLASSES --------------------

	private class Particle {
		final Id<Person> id;
		final double deltaUn0;
		final Set<Slot> affectedSlots;

		Particle(Id<Person> personId, double deltaUn0, Set<Slot> affectedSlots) {
			this.id = personId;
			this.deltaUn0 = deltaUn0;
			this.affectedSlots = affectedSlots;
		}
	}

	private class ReplannerSelection {

		private final Map<Id<Person>, Double> replannerId2deltaUn0 = new LinkedHashMap<>();

		private final Map<Slot, Double> slot2anticipatedUsage = new LinkedHashMap<>();

		private ReplannerSelection(final double trustRegion, final Random rnd, final Map<Slot, Double> slot2size) {

			final List<Particle> particleList = new ArrayList<>(particles);
			shuffle(particleList, rnd); // to break symmetries
			sort(particleList, new Comparator<Particle>() {
				@Override
				public int compare(Particle p1, Particle p2) {
					return Double.compare(p2.deltaUn0, p1.deltaUn0);
				}
			});

			final Map<Slot, Double> slot2space = new LinkedHashMap<>(allKnownSlotsCnt());
			for (Slot slot : allKnownSlots()) {
				slot2space.put(slot, trustRegion / max(1.0, slot2size.getOrDefault(slot, 1.0)));
			}

			for (Particle particle : particleList) {

				// below accounting for particle size
//				final double availableSpace = this.minSpace(particle.affectedSlots, slot2space);
//				if (availableSpace > 0.0) {
//					final double requiredSpace = personId2size.getOrDefault(particle.id, defaultParticleSize);
//					final double allocatedSpace = min(requiredSpace, availableSpace);
//
//					for (Slot slot : particle.affectedSlots) {
//						slot2space.put(slot, max(0.0, slot2space.get(slot) - allocatedSpace));
//					}
//					if (rnd.nextDouble() < (allocatedSpace / requiredSpace)) {
//						this.replannerId2deltaUn0.put(particle.id, particle.deltaUn0);
//					}
//				}

				// working original (before accounting for particle sizes)
				final double availableSpace = minSpace(particle.affectedSlots, slot2space);
				if (availableSpace > 0.0) {
					final double requiredSpace = 1.0;
					final double allocatedSpace = min(requiredSpace, availableSpace);

					for (Slot slot : particle.affectedSlots) {
						slot2space.put(slot, max(0.0, slot2space.get(slot) - allocatedSpace));
					}
					if (rnd.nextDouble() < allocatedSpace / requiredSpace) {
						this.replannerId2deltaUn0.put(particle.id, particle.deltaUn0);
					}
				}
			}

			slot2space.forEach((slot, space) -> this.slot2anticipatedUsage.put(slot, max(0.0, trustRegion - space)));
		}

		private Map<Id<Person>, Double> getResult() {
			return Collections.unmodifiableMap(this.replannerId2deltaUn0);
		}

		private int allKnownSlotsCnt() {
			return (linkId2capacity.size() * conf.getBinCnt());
		}

		private Iterable<Slot> allKnownSlots() {
			List<Slot> result = new ArrayList<>(this.allKnownSlotsCnt());
			linkId2capacity.keySet().stream().forEach(linkId -> {
				for (int timeBin = 0; timeBin < conf.getBinCnt(); timeBin++) {
					result.add(new Slot(linkId, timeBin));
				}
			});
			return result;
		}

		private double minSpace(final Iterable<Slot> slots, final Map<Slot, Double> slot2space) {
			double result = Double.POSITIVE_INFINITY;
			for (Slot slot : slots) {
				result = min(result, slot2space.get(slot));
			}
			return result;
		}
	}

	// -------------------- CONSTANTS --------------------

	private final Random rnd = MatsimRandom.getRandom();

	private final GreedoConfigGroup conf;

	private final Map<Id<Link>, Integer> linkId2capacity;

	private final Set<Id<Person>> allPersonIds;

	private final String logFile = "TR.txt";

	private final double defaultParticleSize = 1.0; // TODO hardwired

	// -------------------- MEMBERS --------------------

	private double trustRegion;

	private final List<Particle> particles = new ArrayList<>();

	private final Map<Id<Person>, Double> personId2size;
	private final Map<Id<Person>, Double> personId2sizeUpdateCnt;

	private final Map<Id<Person>, Integer> personId2numberOfReplans = new LinkedHashMap<>();

	private Map<Id<Person>, Double> lastReplannerId2expectedUtilityChange = null;

	private Map<Slot, Double> lastSlot2lastAnticipatedUsage = null;

	private Double rho = null;

	// -------------------- CONSTRUCTION --------------------

	public TrustRegionBasedReplannerSelector(Scenario scenario, GreedoConfigGroup conf, double initialTrustRegion) {
		this.conf = conf;
		this.trustRegion = initialTrustRegion;

		this.allPersonIds = new LinkedHashSet<>(scenario.getPopulation().getPersons().keySet());
		this.personId2size = new LinkedHashMap<>(scenario.getPopulation().getPersons().size());
		this.personId2sizeUpdateCnt = new LinkedHashMap<>(scenario.getPopulation().getPersons().size());

		this.linkId2capacity = new LinkedHashMap<>();
		scenario.getNetwork().getLinks().values().stream().forEach(link -> {
			final Integer cap = (int) (link.getCapacity() / scenario.getNetwork().getCapacityPeriod());
			if (cap < 1) {
				Logger.getLogger(this.getClass())
						.warn("link " + link.getId() + " has cap = " + cap + ", increasing to 1");
			}
			this.linkId2capacity.put(link.getId(), max(1, cap));
		});

		try {
			FileUtils.writeStringToFile(new File(this.logFile), "<DeltaUn0>\tTR\trho\t<lambda>\n", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	// -------------------- INTERNALS --------------------

	// TODO Counts every slot at most once.
	private Set<Slot> createSlots(final SpaceTimeIndicators<Id<?>> indicators) {
		final Set<Slot> result = new LinkedHashSet<>(indicators.size());
		for (int timeBin = 0; timeBin < indicators.getTimeBinCnt(); timeBin++) {
			final int timeBinCopy = timeBin;
			indicators.getVisits(timeBin).forEach(v -> {
				result.add(new Slot(v.spaceObject, timeBinCopy));
			});
		}
		return result;
	}

	private void increaseNumberOfReplans(final Id<Person> replannerId) {
		this.personId2numberOfReplans.put(replannerId, 1 + this.personId2numberOfReplans.getOrDefault(replannerId, 0));
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(final Id<Person> personId, final double deltaUn0,
			final SpaceTimeIndicators<Id<?>> anticipatedIndicators,
			final SpaceTimeIndicators<Id<?>> currentIndicators) {
		final Set<Slot> anticipatedSlots = this.createSlots(anticipatedIndicators);
		this.particles.add(new Particle(personId, deltaUn0, anticipatedSlots));
		// final Set<Slot> currentSlots = this.createSlots(currentIndicators);
		// this.particles.add(new Particle(personId, deltaUn0, union(anticipatedSlots,
		// currentSlots)));
	}

	public Set<Id<Person>> selectReplanners(final double innoWeight, final Map<Slot, Double> slot2size) {

		final ReplannerSelection replannerSelection = new ReplannerSelection(this.trustRegion, this.rnd, slot2size);
		this.lastReplannerId2expectedUtilityChange = replannerSelection.getResult();
		this.lastSlot2lastAnticipatedUsage = replannerSelection.slot2anticipatedUsage;

		this.lastReplannerId2expectedUtilityChange.keySet().forEach(id -> this.increaseNumberOfReplans(id));

		try {
			final double totalCnt = this.particles.size();
			final double replannerCnt = this.lastReplannerId2expectedUtilityChange.size();
			final double deltaUn0sum = this.particles.stream().mapToDouble(p -> p.deltaUn0).sum();
			FileUtils.writeStringToFile(new File(this.logFile), ((deltaUn0sum / totalCnt) + "\t" + this.trustRegion
					+ "\t" + this.rho + "\t" + (replannerCnt / totalCnt) + "\n").replace('.', ','), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.particles.clear();

		return new LinkedHashSet<>(this.lastReplannerId2expectedUtilityChange.keySet());
		// return new LinkedHashSet<>();
	}

	public void updatePersonSizes(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2lastSlotUsages,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2secondLastSlotUsages) {

		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> replannerId2lastSlotUsages = new LinkedHashMap<>();
		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> replannerId2secondLastSlotUsages = new LinkedHashMap<>();
		for (Id<Person> replannerId : this.lastReplannerId2expectedUtilityChange.keySet()) {
			replannerId2lastSlotUsages.put(replannerId, personId2lastSlotUsages.getOrDefault(replannerId,
					new SpaceTimeIndicators<>(this.conf.getBinCnt())));
			replannerId2secondLastSlotUsages.put(replannerId, personId2secondLastSlotUsages.getOrDefault(replannerId,
					new SpaceTimeIndicators<>(this.conf.getBinCnt())));
		}

		// Sum up realized slot usages over entire population
		// TODO The summation must be consistent with the variability evaluation!
		final DynamicData<Id<?>> slotUsageChanges = newWeightedSum(
				newTotals(this.conf.newTimeDiscretization(), replannerId2lastSlotUsages.values(), false, false), +1.0,
				newTotals(this.conf.newTimeDiscretization(), replannerId2secondLastSlotUsages.values(), false, false),
				-1.0);

		for (Id<Person> replannerId : this.lastReplannerId2expectedUtilityChange.keySet()) {

			final SpaceTimeIndicators<Id<?>> indicators = replannerId2lastSlotUsages.get(replannerId);

			// System.out.println("REPLANNER: " + replannerId + ", last size = " +
			// indicators.size());

			double size = 1.0; // TODO hardwired
			for (int timeBin = 0; timeBin < indicators.getTimeBinCnt(); timeBin++) {
				for (SpaceTimeIndicators<Id<?>>.Visit visit : indicators.getVisits(timeBin)) {
					size = max(size, slotUsageChanges.getBinValue(visit.spaceObject, timeBin) / this.trustRegion);
				}
			}
			if (this.personId2size.containsKey(replannerId)) {
				final double innoWeight = 1.0
						/ sqrt(max(1.0, this.personId2numberOfReplans.getOrDefault(replannerId, 0)));
				this.personId2size.put(replannerId,
						innoWeight * size + (1.0 - innoWeight) * this.personId2size.get(replannerId));
			} else {
				this.personId2size.put(replannerId, size);
			}
		}
	}
}
