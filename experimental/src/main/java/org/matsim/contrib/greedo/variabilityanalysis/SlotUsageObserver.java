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
package org.matsim.contrib.greedo.variabilityanalysis;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SlotUsageObserver<P, L> {

	private final int memory;

	Map<Slot<L>, SlotStatistics<P>> slot2stats = new LinkedHashMap<>();

	public SlotUsageObserver(final int memory) {
		this.memory = memory;
	}

	private SlotStatistics<P> getOrCreateSlotStatistics(final Slot<L> slot) {
		SlotStatistics<P> stats = this.slot2stats.get(slot);
		if (stats == null) {
			stats = new SlotStatistics<>(this.memory);
			this.slot2stats.put(slot, stats);
		}
		return stats;
	}

	public void add(final Map<P, SpaceTimeIndicators<L>> allSlotUsages, final double innovationWeight) {
		allSlotUsages.entrySet().forEach(entry -> {
			final P particle = entry.getKey();
			final SpaceTimeIndicators<L> indicators = entry.getValue();
			for (int timeBin = 0; timeBin < indicators.getTimeBinCnt(); timeBin++) {
				final int timeBinCopy = timeBin;
				indicators.getVisits(timeBin).forEach(v -> {
					final Slot<L> slot = new Slot<>(v.spaceObject, timeBinCopy);
					this.getOrCreateSlotStatistics(slot).addNewVisitor(particle);
				});
			}
		});
		this.slot2stats.values().forEach(stats -> stats.finalizeNewVisitors(innovationWeight));
	}

	public double[] shareOfSlotsExceedingRecurrenceProba(final double threshold) {
		final double[] result = new double[this.memory];
		this.slot2stats.values().forEach(stats -> {
			for (int l = 0; l < this.memory; l++) {
				if (stats.arrivalCoeffs[l] >= threshold) {
					result[l]++;
				}
			}
		});
		for (int l = 0; l < this.memory; l++) {
			result[l] /= this.slot2stats.size();
		}
		return result;
	}

	public void writeSummaryStatsFile(final String fileName) {
		try {
			final PrintWriter writer = new PrintWriter(fileName);

			writer.print("threshold");
			for (int l = 0; l < this.memory; l++) {
				writer.print("\tlag=" + l);
			}
			writer.println();

			for (double threshold = 0; threshold <= 1.01; threshold += 0.05) {
				writer.print(threshold);
				final double[] shares = this.shareOfSlotsExceedingRecurrenceProba(threshold);
				for (int l = 0; l < this.memory; l++) {
					writer.print("\t" + shares[l]);
				}
				writer.println();
			}

			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		this.slot2stats.entrySet().forEach(entry -> {
			result.append(entry.getKey());
			result.append("\t");
			result.append(entry.getValue());
			result.append("\n");
		});
		return result.toString();
	}
}
