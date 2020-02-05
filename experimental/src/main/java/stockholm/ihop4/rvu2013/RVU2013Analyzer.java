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
package stockholm.ihop4.rvu2013;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.Time;
import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import stockholm.ihop2.regent.demandreading.PopulationCreator;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RVU2013Analyzer extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	public static final String startRegionLabel = "D_A_SOMR";
	public static final String endRegionLabel = "D_B_SOMR";
	public static final Set<String> stockholmRegionValues = new LinkedHashSet<>(
			Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));

	public static final String purposeLabel = "H_ARE";
	public static final Set<String> workPurposeValues = new LinkedHashSet<>(Arrays.asList("2", "80"));
	public static final Set<String> otherPurposeValues = new LinkedHashSet<>(Arrays.asList("6", "7", "8", "9", "10",
			"11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
	public static final String work = PopulationCreator.WORK;
	public static final String other = PopulationCreator.OTHER;

	public static final String personLabel = "UENR";
	public static final String tripLabel = "H_NUMMER";
	public static final String segmentLabel = "D_NUMMER";

	public static final String startTimeLabel = "D_A_KL";
	public static final String endTimeLabel = "D_B_KL";
	public static final String travelTimeLabel = "D_RESTID";

	public static final String startLocationLabel = "D_A_S";
	public static final String endLocationLabel = "D_B_S";

	public static final String mainModeLabel = "H_FORD";
	public static final Set<String> carDriverValues = new LinkedHashSet<>(
			Arrays.asList("501", "502", "503", "504", "505", "506", "507", "508", "509"));

	// -------------------- MEMBERS --------------------

	private final Map<String, Traveler> id2traveler = new LinkedHashMap<>();

	private final TourSequenceTimeStructures timeStructures;

	// -------------------- CONSTRUCTION --------------------

	public RVU2013Analyzer(final String fileName) {

		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		parser.setOmitEmptyColumns(false);

		this.timeStructures = new TourSequenceTimeStructures();

		final BasicStatistics onlyWorkStartStats = new BasicStatistics();
		final BasicStatistics onlyOtherStartStats = new BasicStatistics();
		final BasicStatistics workInBothStartStats = new BasicStatistics();
		final BasicStatistics otherInBothStartStats = new BasicStatistics();

		final BasicStatistics onlyWorkDurStats = new BasicStatistics();
		final BasicStatistics onlyOtherDurStats = new BasicStatistics();
		final BasicStatistics workInBothDurStats = new BasicStatistics();
		final BasicStatistics otherInBothDurStats = new BasicStatistics();
		final BasicStatistics intermediateHomeDurStats = new BasicStatistics();

		for (Traveler person : this.id2traveler.values()) {

			final List<Tour> tours = person.tours(person.startLocation());
			final boolean relevant;

			if (tours.size() == 1) {

				final String firstPurpose = tours.get(0).uniquePurpose();
				final Tuple<Integer, Integer> startAndDur = tours.get(0).mainActivityStartAndDuration_s();
				if (startAndDur == null) {
					relevant = false;
				} else if (work.equals(firstPurpose)) {
					relevant = true;
					this.timeStructures.add(firstPurpose, startAndDur.getFirst(), startAndDur.getSecond());
					onlyWorkStartStats.add(startAndDur.getFirst());
					onlyWorkDurStats.add(startAndDur.getSecond());
				} else if (other.equals(firstPurpose)) {
					relevant = true;
					this.timeStructures.add(firstPurpose, startAndDur.getFirst(), startAndDur.getSecond());
					onlyOtherStartStats.add(startAndDur.getFirst());
					onlyOtherDurStats.add(startAndDur.getSecond());
				} else {
					relevant = false;
				}

			} else if (tours.size() == 2) {

				final Tuple<Integer, Integer> startAndDur1 = tours.get(0).mainActivityStartAndDuration_s();
				final Tuple<Integer, Integer> startAndDur2 = tours.get(1).mainActivityStartAndDuration_s();
				if (!work.equals(tours.get(0).uniquePurpose()) || !other.equals(tours.get(1).uniquePurpose())
						|| (startAndDur1 == null) || (startAndDur2 == null)) {
					relevant = false;
				} else {
					relevant = true;
					final int intermedHomeDur_s = tours.get(1).startTime_s() - tours.get(0).endTime_s();
					this.timeStructures.add(work, startAndDur1.getFirst(), startAndDur1.getSecond(), other,
							startAndDur2.getFirst(), startAndDur2.getSecond(), intermedHomeDur_s);
					workInBothStartStats.add(startAndDur1.getFirst());
					workInBothDurStats.add(startAndDur1.getSecond());
					otherInBothStartStats.add(startAndDur2.getFirst());
					otherInBothDurStats.add(startAndDur2.getSecond());
					intermediateHomeDurStats.add(intermedHomeDur_s);
				}

			} else {
				relevant = false;
			}

			if (relevant) {
				System.out.println(person);
			}
		}

		System.out.println();

		System.out.print("start work in 1 tour: ");
		System.out.println(toString(onlyWorkStartStats));

		System.out.print("dur work in 1 tour: ");
		System.out.println(toString(onlyWorkDurStats));

		System.out.print("start other in 1 tour: ");
		System.out.println(toString(onlyOtherStartStats));

		System.out.print("dur other in 1 tour: ");
		System.out.println(toString(onlyOtherDurStats));

		System.out.print("start work in 2 tour: ");
		System.out.println(toString(workInBothStartStats));

		System.out.print("dur work in 2 tour: ");
		System.out.println(toString(workInBothDurStats));

		System.out.print("start other in 2 tour: ");
		System.out.println(toString(otherInBothStartStats));

		System.out.print("dur other in 2 tour: ");
		System.out.println(toString(otherInBothDurStats));

		System.out.print("dur intermed. home: ");
		System.out.println(toString(intermediateHomeDurStats));
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void startCurrentDataRow() {

		final String mainMode = this.getStringValue(mainModeLabel);
		final String startRegion = this.getStringValue(startRegionLabel);
		final String endRegion = this.getStringValue(endRegionLabel);

		if (carDriverValues.contains(mainMode) && stockholmRegionValues.contains(startRegion)
				&& stockholmRegionValues.contains(endRegion)) {

			final String purpose;
			{
				final String purposeValue = this.getStringValue(purposeLabel);
				if (workPurposeValues.contains(purposeValue)) {
					purpose = work;
				} else if (otherPurposeValues.contains(purposeValue)) {
					purpose = other;
				} else {
					purpose = null;
				}
			}

			if (purpose != null) {

				final String personId = this.getStringValue(personLabel);
				Traveler person = this.id2traveler.get(personId);
				if (person == null) {
					person = new Traveler(personId);
					this.id2traveler.put(personId, person);
				}

				final String startTime = this.getStringValue(startTimeLabel);
				final String endTime = this.getStringValue(endTimeLabel);
				if ((startTime.length() >= 3) && (endTime.length() >= 3)) {

					final String tripId = this.getStringValue(tripLabel);
					final String segmentId = this.getStringValue(segmentLabel);
					final String duration = this.getStringValue(travelTimeLabel);
					final String startLocation = this.getStringValue(startLocationLabel);
					final String endLocation = this.getStringValue(endLocationLabel);

					final TripSegment segment = new TripSegment(tripId, segmentId, startTime, endTime, duration,
							purpose, startLocation, endLocation);
					person.add(segment);
				}
			}
		}
	}

	public TourSequenceTimeStructures getTimeStructures() {
		return this.timeStructures;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED ...");

		final String fileName = "/Users/GunnarF/OneDrive - VTI/My Data/ihop4/rvu2013/MDRE_1113_original.csv";
		final RVU2013Analyzer analyzer = new RVU2013Analyzer(fileName);
		System.out.println(analyzer.getTimeStructures());

		System.out.println("... DONE");
	}

	static String toString(BasicStatistics stat) {
		StringBuffer result = new StringBuffer();
		result.append("mean = " + Time.strFromSec((int) stat.getAvg()));
		result.append(", stddev = " + Time.strFromSec((int) stat.getStddev()));
		result.append(", min = " + Time.strFromSec((int) stat.getMin()));
		result.append(", max = " + Time.strFromSec((int) stat.getMax()));
		return result.toString();
	}

}
