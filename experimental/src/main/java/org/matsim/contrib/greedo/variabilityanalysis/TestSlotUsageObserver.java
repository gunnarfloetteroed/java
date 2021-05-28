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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestSlotUsageObserver {

	public static void main(String[] args) {

		// System.out.println("STARTED ...");

		SlotUsageObserver<String, String> observer = new SlotUsageObserver<>(5);

		List<Double> visitor1Errors = new ArrayList<>();
		List<Double> visitor2Errors = new ArrayList<>();

		List<Double> arrival1Errors = new ArrayList<>();
		List<Double> arrival2Errors = new ArrayList<>();
		
		List<Double> departure1Errors = new ArrayList<>();
		List<Double> departure2Errors = new ArrayList<>();

		for (int k = 0; k < 100; k++) {
			Map<String, SpaceTimeIndicators<String>> replannerSlotUsages = new LinkedHashMap<>();

			{
				final SpaceTimeIndicators<String> indicators = new SpaceTimeIndicators<>(1);
				if (k % 2 == 0) {
					indicators.visit("slot_1", 0, 1.0, 1.0);
				} else {
					indicators.visit("slot_2", 0, 1.0, 1.0);
				}
				replannerSlotUsages.put("part_1", indicators);
			}

			{
				final SpaceTimeIndicators<String> indicators = new SpaceTimeIndicators<>(1);
				if (k % 3 == 0) {
					indicators.visit("slot_2", 0, 1.0, 1.0);
				}
				replannerSlotUsages.put("part_2", indicators);
			}

			{
				final SpaceTimeIndicators<String> indicators = new SpaceTimeIndicators<>(1);
				indicators.visit("slot_2", 0, 1.0, 1.0);
				replannerSlotUsages.put("part_3", indicators);
			}

			observer.add(replannerSlotUsages, 1.0 / (1.0 + k));

			System.out.println(observer);

			visitor1Errors.add(observer.slot2stats.get(new Slot<>("slot_1", 0)).getLastVisitorError());
			visitor2Errors.add(observer.slot2stats.get(new Slot<>("slot_2", 0)).getLastVisitorError());

			arrival1Errors.add(observer.slot2stats.get(new Slot<>("slot_1", 0)).getLastArrivalError());
			arrival2Errors.add(observer.slot2stats.get(new Slot<>("slot_2", 0)).getLastArrivalError());

			departure1Errors.add(observer.slot2stats.get(new Slot<>("slot_1", 0)).getLastDepartureError());
			departure2Errors.add(observer.slot2stats.get(new Slot<>("slot_2", 0)).getLastDepartureError());
}

		System.out.println();
		System.out.println("E(visit1)\tE(visit2)\tE(arr1)\tE(arr2)\tE(dpt1)\tE(dpt2)");
		for (int k = 0; k < visitor1Errors.size(); k++) {
			System.out.print(visitor1Errors.get(k) + "\t" + visitor2Errors.get(k) + "\t");
			System.out.print(arrival1Errors.get(k) + "\t" + arrival2Errors.get(k) + "\t");
			System.out.print(departure1Errors.get(k) + "\t" + departure2Errors.get(k) + "\n");
		}

		observer.writeSummaryStatsFile("recurrenceStats.txt");
		
		// System.out.println("... DONE");
	}
}
