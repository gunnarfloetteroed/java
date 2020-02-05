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
package org.matsim.contrib.opdyts.buildingblocks.calibration.counting;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Counter {

	private final TimeDiscretization timeDiscr;

	private int[] data = null;

	private int tooSmallCnt = 0;

	private int tooLargeCnt = 0;

	public Counter(final TimeDiscretization timeDiscr) {
		this.timeDiscr = timeDiscr;
		this.resetData();
	}

	public void resetData() {
		this.data = new int[timeDiscr.getBinCnt()];
		this.tooSmallCnt = 0;
		this.tooLargeCnt = 0;
	}

	public void inc(double time_s) {
		final int bin = this.timeDiscr.getBin(time_s);
		if (bin < 0) {
			this.tooSmallCnt++;
		} else if (bin >= this.timeDiscr.getBinCnt()) {
			this.tooLargeCnt++;
		} else {
			this.data[bin]++;
		}
	}

	public int[] getData() {
		return this.data;
	}

	public int getTooSmallCnt() {
		return this.tooSmallCnt;
	}

	public int getTooLargeCnt() {
		return this.tooLargeCnt;
	}
}
