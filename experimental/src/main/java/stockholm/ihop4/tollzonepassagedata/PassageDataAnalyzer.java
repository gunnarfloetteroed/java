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
package stockholm.ihop4.tollzonepassagedata;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Time;
import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class PassageDataAnalyzer {

	private final DynamicData<String> data;

	private final double[] weightPerVehicleLengthClass;

	PassageDataAnalyzer(final TimeDiscretization timeDiscr, final double[] weightPerVehicleLengthClass) {
		this.data = new DynamicData<>(timeDiscr);
		this.weightPerVehicleLengthClass = weightPerVehicleLengthClass;
	}

	void parse(final String file) {

		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);

		final PassageDataHandler handler = new PassageDataHandler(this.data, this.weightPerVehicleLengthClass);
		try {
			parser.parse(file, handler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	DynamicData<String> getData() {
		return this.data;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		List<String> locations = new ArrayList<>(this.data.keySet());
		result.append("\t");
		for (String loc : locations) {
			result.append(loc + "\t");
		}
		result.append("\n");
		for (int bin = 0; bin < this.data.getBinCnt(); bin++) {
			result.append("[" + Time.strFromSec(this.data.binStart_s(bin)) + ", "
					+ Time.strFromSec(this.data.binStart_s(bin + 1)) + ")\t");
			for (String loc : locations) {
				result.append((this.data.getBinValue(loc, bin)) + "\t");
			}
			result.append("\n");
		}
		return result.toString();
	}

	public static void main(String[] args) throws IOException {

		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600 / 2, 24 * 2);

		final List<String> days = Arrays.asList("2016-10-11", "2016-10-12", "2016-10-13", "2016-10-18", "2016-10-19",
				"2016-10-20", "2016-10-25", "2016-10-26", "2016-10-27");

		PassageDataAnalyzer dataAnalyzer = new PassageDataAnalyzer(timeDiscr,
				new double[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});

		for (String day : days) {
			for (String postfix : new String[] { "-01", "-02" }) {
				final Path path = Paths.get("/Users/GunnarF/NoBackup/data-workspace/ihop4/2016-10-xx_passagedata",
						"wsp-passages-vtr-" + day + postfix + ".csv");
				System.out.println(path);

				dataAnalyzer.parse(path.toString());
			}
		}

		System.out.println(dataAnalyzer.toString());
	}
}
