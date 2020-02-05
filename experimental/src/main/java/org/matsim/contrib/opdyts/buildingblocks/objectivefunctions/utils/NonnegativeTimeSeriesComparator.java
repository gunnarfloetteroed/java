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
package org.matsim.contrib.opdyts.buildingblocks.objectivefunctions.utils;

/**
 * @author Gunnar Flötteröd
 *
 */
public class NonnegativeTimeSeriesComparator {

	private Double earthMoverDistance = null;

	private Double absoluteDifference = null;

	public NonnegativeTimeSeriesComparator() {
	}

	public void compute(final double[] x, final double[] y) {

		if (x.length != y.length) {
			throw new RuntimeException("Different lengths: x.length = " + x.length + ", y.length = " + y.length);
		}

		double xSum = 0;
		double ySum = 0;
		for (int i = 0; i < x.length; i++) {
			if (x[i] < 0) {
				throw new RuntimeException("x[" + i + "] = " + x[i] + " < 0.");
			}
			xSum += x[i];
			if (y[i] < 0) {
				throw new RuntimeException("y[" + i + "] = " + y[i] + " < 0.");
			}
			ySum += y[i];
		}
		this.absoluteDifference = Math.abs(xSum - ySum);

		this.earthMoverDistance = 0.0;
		if ((xSum >= 1e-8) && (ySum >= 1e-8)) {
			final double xScale = Math.min(xSum, ySum) / xSum;
			final double yScale = Math.min(xSum, ySum) / ySum;

			double lastDistance = 0.0;
			for (int i = 0; i < x.length; i++) {
				lastDistance = (xScale * x[i] + lastDistance) - yScale * y[i];
				this.earthMoverDistance += Math.abs(lastDistance);
			}
		}
	}

	public Double getEarthMoverDistance() {
		return this.earthMoverDistance;
	}

	public Double getAbsoluteDifference() {
		return this.absoluteDifference;
	}
}
