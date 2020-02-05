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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.SetUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Tour {

	List<TripSegment> segments = new ArrayList<>();

	String uniquePurpose() {
		if (this.segments.size() == 0) {
			return null;
		} else {
			final String currentPurpose = this.segments.get(0).purpose;
			for (int i = 1; i < this.segments.size(); i++) {
				if (!currentPurpose.equals(this.segments.get(i).purpose)) {
					return null;
				}
			}
			return currentPurpose;
		}
	}

	Tuple<Integer, Integer> mainActivityStartAndDuration_s() {

		final Map<String, Integer> location2firstArrival_s = new LinkedHashMap<>();
		final Map<String, Integer> location2lastDeparture_s = new LinkedHashMap<>();

		for (TripSegment segment : this.segments) {
			if (!location2firstArrival_s.containsKey(segment.endLocation)) {
				location2firstArrival_s.put(segment.endLocation, segment.endTime_s);
			}
			location2lastDeparture_s.put(segment.startLocation, segment.startTime_s);
		}

		int start_s = Integer.MIN_VALUE;
		int dur_s = Integer.MIN_VALUE;
		for (String loc : SetUtils.intersect(location2firstArrival_s.keySet(), location2lastDeparture_s.keySet())) {
			final int candDur_s = location2lastDeparture_s.get(loc) - location2firstArrival_s.get(loc);
			if (candDur_s > dur_s) {
				dur_s = candDur_s;
				start_s = location2firstArrival_s.get(loc);
			}
		}

		if (dur_s >= 0 && start_s >= 0) {
			return new Tuple<>(start_s, dur_s);
		} else {
			return null;
		}

	}

	public Integer startTime_s() {
		if (this.segments.size() == 0) {
			return null;
		} else {
			return this.segments.get(0).startTime_s;
		}
	}

	public Integer endTime_s() {
		if (this.segments.size() == 0) {
			return null;
		} else {
			return this.segments.get(this.segments.size() - 1).endTime_s;
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (TripSegment segment : this.segments) {
			result.append(segment.toString());
		}
		return result.toString();
	}
}
