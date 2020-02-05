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
package org.matsim.contrib.opdyts.experimental;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TrajectoryDataUtils {

	private static double[] scaledCopy(final double[] data, final double factor) {
		double[] result = new double[data.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = data[i] * factor;
		}
		return result;
	}

	public static double[] counts2rates(final double[] counts, final double timeBinSize) {
		return scaledCopy(counts, 1.0 / timeBinSize);
	}

	public static double[] rates2counts(final double[] rates, final double timeBinSize) {
		return scaledCopy(rates, timeBinSize);
	}
	
	public static void assertCompatibility(final double[] data, final TimeDiscretization timeDiscr) {
		if (data.length != timeDiscr.getBinCnt()) {
			throw new RuntimeException("Data array length is " + data.length + ", but time discretization assumes "
					+ timeDiscr.getBinCnt() + " data bins.");
		}
	}
}
