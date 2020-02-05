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

import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Trip implements Comparable<Trip> {

	final Integer tripId;
	final SortedSet<TripSegment> segments;

	Trip(final Integer tripId) {
		this.tripId = tripId;
		this.segments = new TreeSet<>();
	}

	void add(TripSegment segment) {
		this.segments.add(segment);
	}

	String startLocation() {
		if (this.segments == null) {
			return null;
		} else {
			return this.segments.first().startLocation;
		}
	}
	
	String endLocation() {
		if (this.segments == null) {
			return null;			
		} else {
			return this.segments.last().endLocation;
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("  Trip " + tripId + "\n");
		for (TripSegment segment : this.segments) {
			result.append(segment);
		}
		return result.toString();
	}

	@Override
	public int compareTo(Trip o) {
		return this.tripId.compareTo(o.tripId);
	}

}
