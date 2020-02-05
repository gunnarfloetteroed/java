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
package org.matsim.contrib.greedo.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.math.BasicStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PairwiseTTest {

	private final List<double[]> dataList = new ArrayList<>();

	private final List<String> nameList = new ArrayList<>();

	public PairwiseTTest() {
	}

	public void addData(final double[] data, final String name) {
		this.dataList.add(data);
		this.nameList.add(name);
	}

	public Map<Tuple<String, String>, BasicStatistics> computeRelativeDifferenceStatistics() {
		final Map<Tuple<String, String>, BasicStatistics> result = new LinkedHashMap<>();
		for (int i = 0; i < this.dataList.size(); i++) {
			final double[] data1 = this.dataList.get(i);
			for (int j = 0; j < this.dataList.size(); j++) {
				final double[] data2 = this.dataList.get(j);
				if (data1.length != data2.length) {
					throw new RuntimeException("Data sets have different number of points.");
				}
				final BasicStatistics stats = new BasicStatistics();
				for (int pnt = 0; pnt < data1.length; pnt++) {
					stats.add((data1[pnt] - data2[pnt]) / Math.max(data1[pnt], data2[pnt]));
				}

				result.put(new Tuple<>(this.nameList.get(i), this.nameList.get(j)), stats);
			}
		}
		return result;
	}

	public String toString(Map<Tuple<String, String>, BasicStatistics> differenceStats) {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Tuple<String, String>, BasicStatistics> entry : differenceStats.entrySet()) {
			result.append(entry.getKey().getFirst());
			result.append(" - ");
			result.append(entry.getKey().getSecond());
			result.append(" :\t meanRelDiff = ");
			result.append(entry.getValue().getAvg());
			result.append(", tStat = ");
			result.append(entry.getValue().getAvg() / entry.getValue().getStddev());
			result.append("\n\n");
		}
		return result.toString();
	}

	@Override
	public String toString() {
		return this.toString(this.computeRelativeDifferenceStatistics());
	}

}
