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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Traveler {

	final String personId;
	final SortedMap<Integer, Trip> travels;

	Traveler(final String personId) {
		this.personId = personId;
		this.travels = new TreeMap<>();
	}

	void add(TripSegment segment) {
		Trip trip = this.travels.get(segment.tripId);
		if (trip == null) {
			trip = new Trip(segment.tripId);
			this.travels.put(trip.tripId, trip);
		}
		trip.add(segment);
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();

		result.append("Person " + this.personId + "\n");

		List<Tour> tours = this.tours(this.startLocation());
		if (tours != null) {
			for (Tour tour : tours) {
				result.append("  Tour (unique purpose=" + tour.uniquePurpose() + ")\n");
				result.append(tour);
			}
		}
		// for (Trip travel : this.travels.values()) {
		// result.append(travel);
		// }

		return result.toString();
	}

	// -------------------- ANALYSIS BELOW --------------------

	String startLocation() {
		if (this.travels.size() == 0) {
			return null;
		} else {
			return this.travels.get(this.travels.firstKey()).startLocation();
		}
	}

	String endLocation() {
		if (this.travels.size() == 0) {
			return null;
		} else {
			return this.travels.get(this.travels.lastKey()).endLocation();
		}
	}

	List<TripSegment> segments() {
		final List<TripSegment> result = new ArrayList<>();
		for (Trip trip : this.travels.values()) {
			result.addAll(trip.segments);
		}
		return result;
	}

	List<Tour> tours(final String homeLocation) {

		final List<Tour> tours = new ArrayList<>();
		if (homeLocation == null) {
			return tours;
		}

		Tour currentTour = null;
		for (TripSegment segment : this.segments()) {

			if (homeLocation.equals(segment.startLocation)) {

				if (currentTour != null) { // probably too conservative
					return new ArrayList<>(0);
				}
				currentTour = new Tour();
				currentTour.segments.add(segment);

			} else if (homeLocation.equals(segment.endLocation)) {

				if (currentTour == null) { // probably too conservative
					return new ArrayList<>(0);
				}
				currentTour.segments.add(segment);
				tours.add(currentTour);
				currentTour = null;

			} else {

				if (currentTour != null) {
					currentTour.segments.add(segment);
				}

			}
		}

		return tours;
	}
}
