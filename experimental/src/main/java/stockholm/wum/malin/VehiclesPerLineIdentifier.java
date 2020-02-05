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
package stockholm.wum.malin;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class VehiclesPerLineIdentifier {

	private final Scenario scenario;

	public VehiclesPerLineIdentifier(final Scenario scenario) {
		this.scenario = scenario;
	}

	public Set<Id<Vehicle>> getVehicles(final String lineName) {
		
		final TransitLine line = this.scenario.getTransitSchedule().getTransitLines().get(Id.create(lineName, TransitLine.class));
		if (line == null) {
			throw new RuntimeException("Unknown line: " + lineName);
		}
		
		final Set<Id<Vehicle>> result = new LinkedHashSet<>();
		for (TransitRoute route : line.getRoutes().values()) {
			for (Departure departure : route.getDepartures().values()) {
				result.add(departure.getVehicleId());
			}
		}
		return result;
	}
}
