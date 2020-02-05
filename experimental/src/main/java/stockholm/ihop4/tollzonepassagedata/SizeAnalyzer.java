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
import java.util.Arrays;
import java.util.List;

import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SizeAnalyzer {

	private final int[] relevantIdentifiedVehiclesPerMeterLengthClass;
	private final int[] identifiedVehiclesPerMeterLengthClass;

	SizeAnalyzer(final int maxLength_m) {
		this.relevantIdentifiedVehiclesPerMeterLengthClass = new int[maxLength_m];
		this.identifiedVehiclesPerMeterLengthClass = new int[maxLength_m];
	}

	void parse(final String file) {

		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);

		final SizeAnalysisHandler handler = new SizeAnalysisHandler(this.relevantIdentifiedVehiclesPerMeterLengthClass,
				this.identifiedVehiclesPerMeterLengthClass);
		try {
			parser.parse(file, handler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	double[] getRelevanceProbabilityPerMeterLengthClass() {
		final double[] result = new double[this.identifiedVehiclesPerMeterLengthClass.length];
		for (int i = 0; i < this.identifiedVehiclesPerMeterLengthClass.length; i++) {
			final double relevantIdentified = this.relevantIdentifiedVehiclesPerMeterLengthClass[i];
			final double identified = this.identifiedVehiclesPerMeterLengthClass[i];
			if (identified > 0) {
				result[i] = relevantIdentified / identified;
			}
		}
		return result;
	}

	@Override
	public String toString() {
		final double[] relevantProba = this.getRelevanceProbabilityPerMeterLengthClass();
		final StringBuffer result = new StringBuffer("length[m]\tidentified\trelevantIdentified\tPr(relevant)\n");
		for (int i = 0; i < this.identifiedVehiclesPerMeterLengthClass.length; i++) {
			final int relevantIdentified = this.relevantIdentifiedVehiclesPerMeterLengthClass[i];
			final int identified = this.identifiedVehiclesPerMeterLengthClass[i];
			result.append(i + "\t" + identified + "\t" + relevantIdentified + "\t" + relevantProba[i] + "\n");
		}
		return result.toString();
	}

	public static void main(String[] args) throws IOException {

		final List<String> days = Arrays.asList("2016-10-11", "2016-10-12", "2016-10-13", "2016-10-18", "2016-10-19",
				"2016-10-20", "2016-10-25", "2016-10-26", "2016-10-27");

		final int maxLength_m = 20;
		final SizeAnalyzer sizeAnalyzer = new SizeAnalyzer(maxLength_m);

		for (String day : days) {
			for (String postfix : new String[] { "-01", "-02" }) {
				final Path path = Paths.get("/Users/GunnarF/NoBackup/data-workspace/ihop4/2016-10-xx_passagedata",
						"wsp-passages-vtr-" + day + postfix + ".csv");
				System.out.println(path);
				sizeAnalyzer.parse(path.toString());
			}
		}

		System.out.println();
		System.out.println(sizeAnalyzer.toString());
	}

}
