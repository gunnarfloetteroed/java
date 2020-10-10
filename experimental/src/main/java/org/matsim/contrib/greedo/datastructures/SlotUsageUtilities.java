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
package org.matsim.contrib.greedo.datastructures;

import java.util.Collection;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SlotUsageUtilities {

	private SlotUsageUtilities() {
	}

	public static <L> void addIndicatorsToTotalsTreatingNullAsZero(final DynamicData<L> totals,
			final SpaceTimeIndicators<L> indicators, final double factor, final boolean useParticleWeight,
			final boolean useSlotWeight) {
		if (indicators != null) {
			for (int bin = 0; bin < indicators.getTimeBinCnt(); bin++) {
				for (SpaceTimeIndicators<L>.Visit visit : indicators.getVisits(bin)) {
					totals.add(visit.spaceObject, bin, factor * visit.weight(useParticleWeight, useSlotWeight));
				}
			}
		}
	}

	public static <L> DynamicData<L> newTotals(final TimeDiscretization timeDiscr,
			final Collection<SpaceTimeIndicators<L>> allIndicators, final boolean useParticleWeight,
			final boolean useSlotWeight) {
		final DynamicData<L> totals = new DynamicData<L>(timeDiscr);
		for (SpaceTimeIndicators<L> indicators : allIndicators) {
			addIndicatorsToTotalsTreatingNullAsZero(totals, indicators, 1.0, useParticleWeight, useSlotWeight);
		}
		return totals;
	}

}
