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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TourSequenceTimeStructures {

	// -------------------- INNER CLASS --------------------

	public static class TimeStructure {

		private final int[] startTimes_s;
		private final int[] durations_s;
		private final int[] intermediateHomeDurations;

		private TimeStructure(final int start_s, final int duration_s) {
			this.startTimes_s = new int[] { start_s };
			this.durations_s = new int[] { duration_s };
			this.intermediateHomeDurations = new int[] {};
		}

		private TimeStructure(final int start1_s, final int duration1_s, final int start2_s, final int duration2_s,
				final int intermediateHomeDuration_s) {
			this.startTimes_s = new int[] { start1_s, start2_s };
			this.durations_s = new int[] { duration1_s, duration2_s };
			this.intermediateHomeDurations = new int[] { intermediateHomeDuration_s };
		}

		public double start_s(int tourIndex) {
			return this.startTimes_s[tourIndex];
		}

		public double duration_s(int tourIndex) {
			return this.durations_s[tourIndex];
		}

		public double intermedHomeDur_s(int precedingTourIndex) {
			return this.intermediateHomeDurations[precedingTourIndex];
		}
	}

	// -------------------- MEMBERS --------------------

	private Map<List<String>, List<TimeStructure>> purposes2timeStructures = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TourSequenceTimeStructures() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<TimeStructure> getTimeStructures(final String... purposes) {
		final List<String> purposeList = Arrays.asList(purposes);
		List<TimeStructure> timeStructures = this.purposes2timeStructures.get(purposeList);
		if (timeStructures == null) {
			timeStructures = new ArrayList<>();
			this.purposes2timeStructures.put(purposeList, timeStructures);
		}
		return timeStructures;
	}

	public void add(final String purpose1, final int start1_s, final int dur1_s) {
		this.getTimeStructures(purpose1).add(new TimeStructure(start1_s, dur1_s));
	}

	public void add(final String purpose1, final int start1_s, final int dur1_s, final String purpose2,
			final int start2_s, final int dur2_s, final int intermediateHomeDur_s) {
		this.getTimeStructures(purpose1, purpose2)
				.add(new TimeStructure(start1_s, dur1_s, start2_s, dur2_s, intermediateHomeDur_s));
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<List<String>, List<TimeStructure>> entry : this.purposes2timeStructures.entrySet()) {
			result.append(entry.getKey() + " : " + entry.getValue().size() + " entries\n");
		}
		return result.toString();
	}
}
