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

import floetteroed.utilities.Time;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class TripSegment implements Comparable<TripSegment> {

	final Integer tripId;
	final Integer segmentId;
	final int startTime_s;
	final int endTime_s;
	final String travelDur;
	final String purpose;
	final String startLocation;
	final String endLocation;

	TripSegment(final String tripId, final String segmentId, final String startTime, final String endTime,
			final String travelDur, final String purpose, final String startLocation, final String endLocation) {
		this.tripId = Integer.parseInt(tripId);
		this.segmentId = Integer.parseInt(segmentId);
		this.travelDur = travelDur;
		this.purpose = purpose;
		this.startLocation = startLocation;
		this.endLocation = endLocation;
		this.startTime_s = str2sec(startTime);
		this.endTime_s = str2sec(endTime);
	}

	private int str2sec(final String str) {
		final int min = Integer.parseInt(str.substring(str.length() - 2, str.length()));
		final int hrs = Integer.parseInt(str.substring(0, str.length() - 2));
		return min * 60 + hrs * 3600;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("    Segment " + this.segmentId + ": purpose=" + this.purpose + " from "
				+ Time.strFromSec(this.startTime_s) + " until " + Time.strFromSec(this.endTime_s) + ", from "
				+ this.startLocation + " to " + this.endLocation + "\n");
		return result.toString();
	}

	@Override
	public int compareTo(TripSegment o) {
		return this.segmentId.compareTo(o.segmentId);
	}

}
