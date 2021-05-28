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

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.Collections.shuffle;
import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.SetUtils;
import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SlotPacker {

	// private final Function<Integer, Integer> indicatorTrafo = (x -> x);

	private class Slot {
		final Id<?> loc;
		final int timeBin;

		Slot(final Id<?> loc, final int timeBin) {
			this.loc = loc;
			this.timeBin = timeBin;
		}

		@Override
		public boolean equals(final Object other) {
			if (other instanceof Slot) {
				final Slot otherSlot = (Slot) other;
				return (this.loc.equals(otherSlot.loc) && (this.timeBin == otherSlot.timeBin));
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return 31 * (31 * this.loc.hashCode() + Integer.hashCode(this.timeBin));
		}

		@Override
		public String toString() {
			return this.loc + "(" + this.timeBin + ")";
		}
	}

	private class Particle {
		final Id<Person> id;
		final double deltaUn0;
		final Set<Slot> posSlots;
		final Set<Slot> negSlots;
		final Set<Slot> newSlots;
		final Set<Slot> support;

		Particle(Id<Person> personId, double deltaUn0, Set<Slot> posSlots, Set<Slot> negSlots, Set<Slot> newSlots,
				Set<Slot> support) {
			this.id = personId;
			this.deltaUn0 = deltaUn0;
			this.posSlots = posSlots;
			this.negSlots = negSlots;
			this.newSlots = newSlots;
			this.support = support;
		}
	}

	private final List<Particle> particles = new ArrayList<>();

	private final Map<Slot, Double> slot2capacity;
	private final Map<Slot, Double> slot2remaining;
	private final Map<Slot, Integer> slot2X;

	SlotPacker(final Network network, final TimeDiscretization timeDiscr) {
		this.slot2capacity = new LinkedHashMap<>();
		this.slot2X = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			for (int timeBin = 0; timeBin < timeDiscr.getBinCnt(); timeBin++) {
				final Slot slot = new Slot(link.getId(), timeBin);
				this.slot2capacity.put(slot, link.getCapacity() / network.getCapacityPeriod());
				this.slot2X.put(slot, 0);
			}
		}
		this.slot2remaining = new LinkedHashMap<>(this.slot2capacity);
	}

	private Set<Slot> createSlots(final SpaceTimeIndicators<Id<?>> indicators) {
		final Set<Slot> result = new LinkedHashSet<>(indicators.size());
		for (int timeBin = 0; timeBin < indicators.getTimeBinCnt(); timeBin++) {
			final int timeBinCopy = timeBin;
			indicators.getVisits(timeBin).forEach(v -> {
				result.add(new Slot(v.spaceObject, timeBinCopy));
			});
		}
//		FIXME No slot double-counting.
//		if (result.size() != indicators.size()) {
//			Logger.getLogger(this.getClass())
//					.warn("Number of indicators = " + indicators.size() + ", number of slots = " + result.size());
//		}
		return result;
	}

	public void add(final Id<Person> personId, final double deltaUn0,
			final SpaceTimeIndicators<Id<?>> anticipatedIndicators,
			final SpaceTimeIndicators<Id<?>> currentIndicators) {

		final Set<Slot> currentSlots = createSlots(currentIndicators);
		final Set<Slot> anticipatedSlots = createSlots(anticipatedIndicators);

		final Set<Slot> addedSlots = new LinkedHashSet<>(anticipatedSlots);
		addedSlots.removeAll(currentSlots);

		final Set<Slot> abandonedSlots = new LinkedHashSet<>(currentSlots);
		abandonedSlots.removeAll(anticipatedSlots);

		// TODO could even consider building this based on the non-adjusted
		// anticipations
		for (Slot slot : currentSlots) {
			this.slot2remaining.put(slot, this.slot2remaining.get(slot) - 1.0);
			this.slot2X.put(slot, this.slot2X.get(slot) + 1);
		}

		// this.particles.add(new Particle(personId, deltaUn0, new
		// ArrayList<>(union(newlyVisitedSlots, noLongerVisitedSlots))));
		this.particles.add(new Particle(personId, deltaUn0, addedSlots, abandonedSlots, anticipatedSlots,
				SetUtils.union(currentSlots, anticipatedSlots)));
	}

	private boolean particleFitsInTrustRegion(final Particle particle, final Map<Slot, Integer> xNew,
			final Map<Slot, Integer> xRef, final int trustRegion) {
		for (Slot slot : particle.posSlots) {
			// int deltaX = this.indicatorTrafo.apply(1);
			if (abs((xNew.get(slot) + 1) - xRef.get(slot)) > trustRegion) {
				return false;
			}
		}
		for (Slot slot : particle.negSlots) {
			// int deltaX = this.indicatorTrafo.apply(-1);
			if (abs((xNew.get(slot) - 1) - xRef.get(slot)) > trustRegion) {
				return false;
			}
		}
		return true;
	}

	private double xNormAfterParticleSwitch(final Particle particle, final Map<Slot, Integer> x) {
		double result = 0.0;
		for (Map.Entry<Slot, Integer> entry : x.entrySet()) {
			final Slot slot = entry.getKey();
			final int newVal = entry.getValue() + (particle.posSlots.contains(slot) ? 1 : 0)
					- (particle.negSlots.contains(slot) ? 1 : 0);
			result = max(result, abs(newVal));
		}
		return result;
	}

	public Set<Id<Person>> pack(int trustRegion) {

		shuffle(this.particles); // to break symmetries
		sort(this.particles, new Comparator<Particle>() {
			@Override
			public int compare(Particle p1, Particle p2) {
				return Double.compare(p2.deltaUn0, p1.deltaUn0);
//				return Double.compare(p2.deltaUn0 / Math.max(1.0, p2.newSlots.size()),
//						p1.deltaUn0 / Math.max(1.0, p1.newSlots.size()));
			}
		});

//		final double eta;
//		{
//			double num = 0.0;
//			double den = 0.0;
//			for (Slot slot : this.slot2capacity.keySet()) {
//				double x = this.slot2total.get(slot);
//				double c = this.slot2capacity.get(slot);
//				num += x * c;
//				den += c * c;
//			}
//			eta = num / den;
//		}
//		Logger.getLogger(this.getClass()).info("eta = " + eta);

//		double currentNorm = 0.0;
//		for (Slot slot : this.slot2capacity.keySet()) {
//			double x = this.slot2X.get(slot);
//			// double c = this.slot2capacity.get(slot);
//			currentNorm = Math.max(currentNorm, abs(x));
//		}
//		Logger.getLogger(this.getClass()).info("currentMaxNorm = " + currentNorm);

//		double utopiaNorm = 0.0;
//		for (Map.Entry<Slot, Double> e : this.slot2total.entrySet()) {
//			final Slot slot = e.getKey();
//			final Double total = e.getValue();
//			final Double capacity = slot2capacity.get(slot);
//			utopiaNorm = Math.max(utopiaNorm, total / capacity);
//		}
//		double utopiaNorm2 = 0.0;
//		for (Map.Entry<Slot, Double> e : this.slot2total.entrySet()) {
//			final Slot slot = e.getKey();
//			final Double total = e.getValue();
//			final Double capacity = this.slot2capacity.get(slot);
//			utopiaNorm2 += Math.pow(total / capacity, 2.0);
//		}

		final Set<Slot> availableSlots = new LinkedHashSet<>(this.slot2capacity.keySet());
//		final Map<Slot, Integer> slot2newX = new LinkedHashMap<>(this.slot2X);
//		final Map<Slot, Double> slot2space = new LinkedHashMap<>();
//		this.slot2remaining.forEach((s, r) -> {
//			slot2space.put(s, Math.min(r, initialSlotCapacity));
//		});
//		final Map<Slot, Integer> slot2space = new LinkedHashMap<>();
//		for (Particle particle : this.particles) {
//			particle.slots.forEach(s -> slot2space.put(s, initialSlotCapacity));
//		}
//		final Map<Slot, Double> slot2space = new LinkedHashMap<>(this.slot2remaining);

		final Set<Id<Person>> result = new LinkedHashSet<>();
		for (Particle particle : this.particles) {
//			double candNorm2 = currentNorm;
//			for (Slot slot : particle.posSlots) {
//				final double x = this.slot2total.get(slot);
//				final double c = this.slot2capacity.get(slot);
//				candNorm2 += pow(x + 1.0, 2.0) - pow(x, 2.0);
//			}
//			for (Slot slot : particle.negSlots) {
//				final double x = this.slot2total.get(slot);
//				final double c = this.slot2capacity.get(slot);
//				candNorm2 += pow(x - 1.0, 2.0) - pow(x, 2.0);
//			}
//			for (Slot slot : particle.posSlots) {
//				candNorm2 += (pow(slot2newTotal.get(slot) + 1, 2.0) - pow(slot2newTotal.get(slot), 2.0))
//						/ pow(this.slot2capacity.get(slot), 2.0);
//			}
//			for (Slot slot : particle.negSlots) {
//				candNorm2 += (pow(slot2newTotal.get(slot) - 1, 2.0) - pow(slot2newTotal.get(slot), 2.0))
//						/ pow(this.slot2capacity.get(slot), 2.0);
//			}

//			final double candNorm = this.xNormAfterParticleSwitch(particle, slot2newX);
//			if ((candNorm <= currentNorm)
//					&& this.particleFitsInTrustRegion(particle, slot2newX, this.slot2X, trustRegion)) {
//				// particle.posSlots.forEach(s -> slot2space.put(s, slot2space.get(s) - 1.0));
//				// particle.negSlots.forEach(s -> slot2space.put(s, slot2space.get(s) + 1.0));
//				particle.posSlots.forEach(s -> slot2newX.put(s, slot2newX.get(s) + 1));
//				particle.negSlots.forEach(s -> slot2newX.put(s, slot2newX.get(s) - 1));
//				result.add(particle.id);
//				currentNorm = candNorm;
//			}

//			if (availableSlots.containsAll(particle.newSlots)) {
//				availableSlots.removeAll(particle.newSlots);
//				result.add(particle.id);
//			}

			if (availableSlots.containsAll(particle.support)) {
				availableSlots.removeAll(particle.support);
				result.add(particle.id);
			}
		
		}

		return result;
	}

//	public static void main(String[] args) {
//		SlotPacker<String> gp = new SlotPacker<>();
//		SpaceTimeIndicators<String> ind1 = new SpaceTimeIndicators<>(2);
//		ind1.visit("1", 0, 1, 1);
//		SpaceTimeIndicators<String> ind2 = new SpaceTimeIndicators<>(2);
//		ind2.visit("1", 0, 1, 1);
//		SpaceTimeIndicators<String> ind3 = new SpaceTimeIndicators<>(2);
//		ind3.visit("1", 1, 1, 1);
//		gp.add(Id.createPersonId(1), 1.0, ind1);
//		gp.add(Id.createPersonId(2), 2.0, ind2);
//		gp.add(Id.createPersonId(3), 3.0, ind3);
//		System.out.println(gp.pack(1));
//	}
}
