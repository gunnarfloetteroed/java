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
import static java.util.Collections.unmodifiableMap;
import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.newTotals;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.GreedoConfigGroup;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.DynamicDataXMLFileIO;
import floetteroed.utilities.math.BasicStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SlotAnalyzer {

	private final GreedoConfigGroup greedoConfig;

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2lastAnticipatedVisits = null;

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2lastRealizedVisits = null;
	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2secondLastRealizedVisits = null;

	private final Map<Slot, Double> slot2size = new LinkedHashMap<>();

	public SlotAnalyzer(final GreedoConfigGroup greedoConfig) {
		this.greedoConfig = greedoConfig;
	}

	public void registerAnticipatedSlotUsages(
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2anticipatedVisits) {
		this.personId2lastAnticipatedVisits = personId2anticipatedVisits;
	}

	public Map<Slot, Double> getSlot2sizeView() {
		return unmodifiableMap(this.slot2size);
	}
	
	public void registerRealizedSlotUsages(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2realizedVisits,
			final int iteration, final double innoWeight) {

		this.personId2secondLastRealizedVisits = this.personId2lastRealizedVisits;
		this.personId2lastRealizedVisits = personId2realizedVisits;
		if (this.personId2secondLastRealizedVisits == null) {
			return; // ------------------------------------------------------------------------------
		}

		final DynamicData<Id<?>> realizedSlotUsageChanges = newWeightedSum(
				newTotals(this.greedoConfig.newTimeDiscretization(), this.personId2lastRealizedVisits.values(), false,
						false),
				+1.0, newTotals(this.greedoConfig.newTimeDiscretization(),
						this.personId2secondLastRealizedVisits.values(), false, false),
				-1.0);

		// TODO make clearer what this is
		final Map<Slot, BasicStatistics> slot2avgMaxExpIncr = new LinkedHashMap<>();

		this.personId2lastRealizedVisits.forEach((personId, realizedVisits) -> {
			if (realizedVisits != null) {

				/*
				 * Identify the largest slot usage increase experienced by this person.
				 */
				double maxExperiencedSlotUsageIncrease = 0.0;
				for (int timeBin = 0; timeBin < realizedVisits.getTimeBinCnt(); timeBin++) {
					for (SpaceTimeIndicators<Id<?>>.Visit realizedVisit : realizedVisits.getVisits(timeBin)) {
						maxExperiencedSlotUsageIncrease = Math.max(maxExperiencedSlotUsageIncrease,
								realizedSlotUsageChanges.getBinValue(realizedVisit.spaceObject, timeBin));
					}
				}

				/*
				 * Attach the above information to all slots the considered person anticipated
				 * to visit.
				 */
				final SpaceTimeIndicators<Id<?>> anticipatedVisits = this.personId2lastAnticipatedVisits.get(personId);
				if (anticipatedVisits != null) {
					for (int timeBin = 0; timeBin < anticipatedVisits.getTimeBinCnt(); timeBin++) {
						for (SpaceTimeIndicators<Id<?>>.Visit anticipatedVisit : anticipatedVisits.getVisits(timeBin)) {
							final Slot slot = new Slot(anticipatedVisit.spaceObject, timeBin);
							BasicStatistics slotStats = slot2avgMaxExpIncr.get(slot);
							if (slotStats == null) {
								slotStats = new BasicStatistics();
								slot2avgMaxExpIncr.put(slot, slotStats);
							}
							slotStats.add(maxExperiencedSlotUsageIncrease);
						}
					}
				}
			}
		});

		slot2avgMaxExpIncr.forEach((slot, stats) -> {
			Double size = this.slot2size.get(slot);
			if (size == null) {
				this.slot2size.put(slot, stats.getAvg());
			} else {
				this.slot2size.put(slot, innoWeight * stats.getAvg() + (1.0 - innoWeight) * size);
			}			
		});

		// TODO below only for testing
		final DynamicData<Id<?>> sizeData = new DynamicData<>(this.greedoConfig.newTimeDiscretization());
		this.slot2size.forEach((slot, size) -> {
			sizeData.put(slot.loc, slot.timeBin, size);
		});
		this.write("slotSizes." + iteration + ".xml", sizeData);
	}

	private void write(String file, DynamicData<Id<?>> data) {
		DynamicDataXMLFileIO<Id<?>> writer = new DynamicDataXMLFileIO<>() {
			@Override
			protected String key2attrValue(Id<?> key) {
				return key.toString();
			}

			@Override
			protected Id<?> attrValue2key(String string) {
				throw new UnsupportedOperationException();
			}
		};
		try {
			writer.write(file, data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
