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
package org.matsim.contrib.opdyts.buildingblocks.utils;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.math.MathHelpers;

/**
 * TODO This should be moved to some utilities package. Not specific to
 * calibration.
 *
 * @author Gunnar Flötteröd
 *
 */
public class DiscretizationChanger {

	// -------------------- CONSTANTS --------------------

	public static enum DataType {
		TOTALS, // bin values are totals
		RATES // bin values are rates, i.e. normalized to bin length
	};

	private final TimeDiscretization fromDiscr;

	// Internally, everything is in totals.
	private final double[] fromTotals;

	private final DataType dataType;

	// -------------------- MEMBERS --------------------

	private TimeDiscretization toDiscr = null;

	private double[] toTotals = null;

	// -------------------- CONSTRUCTION --------------------

	public DiscretizationChanger(final TimeDiscretization fromDiscr, final double[] fromData, final DataType dataType) {

		if (fromDiscr.getBinCnt() != fromData.length) {
			throw new RuntimeException("Data array length " + fromData.length + " differns from discretization bin cnt "
					+ fromDiscr.getBinCnt() + ".");
		}

		this.fromDiscr = fromDiscr;
		this.dataType = dataType;
		if (DataType.TOTALS.equals(dataType)) {
			this.fromTotals = this.copy(fromData);
		} else if (DataType.RATES.equals(dataType)) {
			this.fromTotals = this.scaledCopy(fromData, fromDiscr.getBinSize_s());
		} else {
			throw new RuntimeException("unknown data type: " + this.dataType);
		}
	}

	// -------------------- INTERNALS --------------------

	private double[] copy(final double[] data) {
		final double[] result = new double[data.length];
		System.arraycopy(data, 0, result, 0, data.length);
		return result;
	}

	private double[] scaledCopy(final double[] data, final double factor) {
		final double[] result = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			result[i] = data[i] * factor;
		}
		return result;
	}

	// -------------------- GETTERS --------------------

	public TimeDiscretization getFromDiscretization() {
		return this.fromDiscr;
	}

	public double[] getFromTotalsCopy() {
		return this.copy(this.fromTotals);
	}

	public double[] getFromRatesCopy() {
		return this.scaledCopy(this.fromTotals, 1.0 / this.fromDiscr.getBinSize_s());
	}

	public DataType getDataType() {
		return this.dataType;
	}

	public TimeDiscretization getToDiscretization() {
		return this.toDiscr;
	}

	public double[] getToTotalsCopy() {
		return this.copy(this.toTotals);
	}

	public double[] getToRatesCopy() {
		return this.scaledCopy(this.toTotals, 1.0 / this.toDiscr.getBinSize_s());
	}

	// -------------------- IMPLEMENATION --------------------

	/* package (testing) */ static double overlap(final double start1, final double end1, final double start2,
			final double end2) {
		return MathHelpers.overlap(start1, end1, start2, end2);
	}

	public void run(final TimeDiscretization toDiscr) {

		this.toDiscr = toDiscr;
		this.toTotals = new double[toDiscr.getBinCnt()];

		final double earliestTime_s = Math.max(this.fromDiscr.getStartTime_s(), this.toDiscr.getStartTime_s());
		final double latestTime_s = Math.min(this.fromDiscr.getEndTime_s(), this.toDiscr.getEndTime_s());
		if (earliestTime_s >= latestTime_s) {
			return; // ------------------------------
		}

		double currentTime_s = earliestTime_s;
		while (currentTime_s + 1e-9 < latestTime_s) { // a bit of slack to avoid infinite loops
			final int fromBin = this.fromDiscr.getBin(currentTime_s);
			final int toBin = this.toDiscr.getBin(currentTime_s);
			final double fromBinEndTime_s = this.fromDiscr.getBinEndTime_s(fromBin);
			final double toBinEndTime_s = this.toDiscr.getBinEndTime_s(toBin);

			final double overlap_s = overlap(this.fromDiscr.getBinStartTime_s(fromBin), fromBinEndTime_s,
					this.toDiscr.getBinStartTime_s(toBin), toBinEndTime_s);
			this.toTotals[toBin] += this.fromTotals[fromBin] * (overlap_s / this.fromDiscr.getBinSize_s());

			currentTime_s = Math.min(fromBinEndTime_s, toBinEndTime_s);
		}
	}
}
