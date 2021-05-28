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
package floetteroed.utilities;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DynamicDataUtils {

	private DynamicDataUtils() {
	}

	public static <L> double sumOfEntries2(final DynamicData<L> data) {
		double result = 0.0;
		for (L locObj : data.keySet()) {
			for (int bin = 0; bin < data.getBinCnt(); bin++) {
				final double val = data.getBinValue(locObj, bin);
				result += val * val;
			}
		}
		return result;
	}

	public static <L> double sumOfDifferences2(final DynamicData<L> counts1, final DynamicData<L> counts2) {
		if (counts1.getBinCnt() != counts2.getBinCnt()) {
			throw new RuntimeException(
					"arg1 has " + counts1.getBinCnt() + " bins, but arg2 has " + counts2.getBinCnt() + " bins.");
		}
		double result = 0.0;
		for (L locObj : SetUtils.union(counts1.keySet(), counts2.keySet())) {
			for (int bin = 0; bin < counts1.getBinCnt(); bin++) {
				final double diff = counts1.getBinValue(locObj, bin) - counts2.getBinValue(locObj, bin);
				result += diff * diff;
			}
		}
		return result;
	}

	public static <L> DynamicData<L> newDifference(final DynamicData<L> data1, final DynamicData<L> data2,
			final double weight) {
		if (data1.getBinCnt() != data2.getBinCnt()) {
			throw new RuntimeException(
					"arg1 has " + data1.getBinCnt() + " bins, but arg2 has " + data2.getBinCnt() + " bins.");
		}
		final DynamicData<L> result = new DynamicData<L>(data1.getStartTime_s(), data1.getBinSize_s(),
				data1.getBinCnt());
		for (L locObj : SetUtils.union(data1.keySet(), data2.keySet())) {
			for (int bin = 0; bin < data1.getBinCnt(); bin++) {
				result.put(locObj, bin, weight * (data1.getBinValue(locObj, bin) - data2.getBinValue(locObj, bin)));
			}
		}
		return result;
	}

	public static <L> DynamicData<L> newWeightedSum(final DynamicData<L> data1, final double weight1,
			final DynamicData<L> data2, final double weight2) {
		if (data1.getBinCnt() != data2.getBinCnt()) {
			throw new RuntimeException(
					"arg1 has " + data1.getBinCnt() + " bins, but arg2 has " + data2.getBinCnt() + " bins.");
		}
		final DynamicData<L> result = new DynamicData<L>(data1.getStartTime_s(), data1.getBinSize_s(),
				data1.getBinCnt());
		for (L locObj : SetUtils.union(data1.keySet(), data2.keySet())) {
			for (int bin = 0; bin < data1.getBinCnt(); bin++) {
				result.put(locObj, bin,
						weight1 * data1.getBinValue(locObj, bin) + weight2 * data2.getBinValue(locObj, bin));
			}
		}
		return result;
	}

	public static <L> double innerProduct(final DynamicData<L> data1, final DynamicData<L> data2) {
		if (data1.getBinCnt() != data2.getBinCnt()) {
			throw new RuntimeException(
					"arg1 has " + data1.getBinCnt() + " bins, but arg2 has " + data2.getBinCnt() + " bins.");
		}

		double result = 0;
		for (L locObj : SetUtils.union(data1.keySet(), data2.keySet())) {
			for (int bin = 0; bin < data1.getBinCnt(); bin++) {
				result += data1.getBinValue(locObj, bin) * data2.getBinValue(locObj, bin);
			}
		}
		return result;
	}

}
