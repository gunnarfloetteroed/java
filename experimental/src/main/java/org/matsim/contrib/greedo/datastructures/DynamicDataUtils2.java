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

import floetteroed.utilities.DynamicData;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DynamicDataUtils2 {

	private DynamicDataUtils2() {
	}

	public static <K> double getMaxNorm(final DynamicData<K> data) {
		double result = 0.0;
		for (K key : data.keySet()) {
			for (int bin = 0; bin < data.getBinCnt(); bin++) {
				result = Math.max(result, Math.abs(data.getBinValue(key, bin)));
			}
		}
		return result;
	}

	public static <K> void makeEntriesAbsolute(final DynamicData<K> data) {
		for (K key : data.keySet()) {
			for (int bin = 0; bin < data.getBinCnt(); bin++) {
				final double value = data.getBinValue(key, bin);
				if (value < 0.0) {
					data.put(key, bin, Math.abs(value));
				}
			}
		}
	}

	public static <K> void makeEntriesSquare(final DynamicData<K> data) {
		for (K key : data.keySet()) {
			for (int bin = 0; bin < data.getBinCnt(); bin++) {
				final double value = data.getBinValue(key, bin);
				if (value != 0.0) {
					data.put(key, bin, value * value);
				}
			}
		}
	}

}
