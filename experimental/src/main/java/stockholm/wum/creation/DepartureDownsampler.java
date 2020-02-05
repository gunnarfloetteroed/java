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
package stockholm.wum.creation;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 *
 * Down-samples transit departures. This is not meant to be realistic but only
 * to allow for technical experimentation in configurations where a full
 * scenario exceeds the available memory.
 *
 * @author Gunnar Flötteröd
 *
 */
public class DepartureDownsampler {

	private final Scenario scenario;

	private final double shareToKeep;

	private int totalDepartures = 0;
	private int removedDepartures = 0;

	public DepartureDownsampler(final Scenario scenario, final double shareToKeep) {
		this.scenario = scenario;
		this.shareToKeep = shareToKeep;
	}

	public int getTotalDepartures() {
		return this.totalDepartures;
	}

	public int getRemovedDepartures() {
		return this.removedDepartures;
	}

	public void run() {
		for (TransitLine line : this.scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				final Set<Departure> removeDepartures = new LinkedHashSet<>();
				for (Departure departure : route.getDepartures().values()) {
					this.totalDepartures++;
					if (MatsimRandom.getRandom().nextDouble() > this.shareToKeep) {
						removeDepartures.add(departure);
						this.removedDepartures++;
					}
				}
				for (Departure departure : removeDepartures) {
					route.removeDeparture(departure);
				}
			}
		}
	}

}
