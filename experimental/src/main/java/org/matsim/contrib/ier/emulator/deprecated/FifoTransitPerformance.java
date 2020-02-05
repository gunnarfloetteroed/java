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
package org.matsim.contrib.ier.emulator.deprecated;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class FifoTransitPerformance implements TransitDriverStartsEventHandler, VehicleDepartsAtFacilityEventHandler,
		VehicleLeavesTrafficEventHandler {

	// -------------------- Composed identifier class --------------------

	private static class LineRouteOtherId<T extends Identifiable<T>> {

		private final Id<TransitLine> lineId;
		private final Id<TransitRoute> routeId;
		private final Id<T> otherId;

		private LineRouteOtherId(final Id<TransitLine> lineId, final Id<TransitRoute> routeId, final Id<T> otherId) {
			this.lineId = lineId;
			this.routeId = routeId;
			this.otherId = otherId;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof LineRouteOtherId) {
				final LineRouteOtherId<?> other = (LineRouteOtherId<?>) obj;
				return (this.lineId.equals(other.lineId) && this.routeId.equals(other.routeId)
						&& this.otherId.equals(other.otherId));
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			// same recipe as in AbstractList
			int hashCode = 1;
			hashCode = 31 * hashCode + this.lineId.hashCode();
			hashCode = 31 * hashCode + this.routeId.hashCode();
			hashCode = 31 * hashCode + this.otherId.hashCode();
			return hashCode;
		}
	}

	// -------------------- REFERENCES --------------------

	private final Vehicles transitVehicles;

	private final TransitSchedule transitSchedule;

	// This one contains the actual result.
	private final Map<LineRouteOtherId<TransitStopFacility>, NextAvailableDepartures> lineRouteStop2nextAvailableDepartures = new LinkedHashMap<>();

	// -------------------- TEMPORARY MEMBERS --------------------

	private final Map<Id<Vehicle>, LineRouteOtherId<Departure>> vehicle2lineRouteDeparture = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public FifoTransitPerformance(final Scenario scenario) {
		this.transitVehicles = scenario.getTransitVehicles();
		this.transitSchedule = scenario.getTransitSchedule();
		this.resetToSchedule();
	}

	// -------------------- INTERNALS --------------------

	public void resetToSchedule() {
		this.lineRouteStop2nextAvailableDepartures.clear();
		for (TransitLine line : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					this.lineRouteStop2nextAvailableDepartures
							.put(new LineRouteOtherId<TransitStopFacility>(line.getId(), route.getId(),
									stop.getStopFacility().getId()), new NextAvailableDepartures(route, stop));
				}
			}
		}
	}

	// -------------------- RESULT ACCESS --------------------

	public synchronized Tuple<Departure, Double> getNextDepartureAndTime_s(final Id<TransitLine> lineId, final TransitRoute route,
			final Id<TransitStopFacility> stopId, final double time_s) {
		final Tuple<Id<Departure>, Double> departureIdAndTime_s = this.lineRouteStop2nextAvailableDepartures
				.get(new LineRouteOtherId<>(lineId, route.getId(), stopId))
				.getNextAvailableDepartureIdAndTime_s(time_s);
		if (departureIdAndTime_s == null) {
			return null;
		} else {
			return new Tuple<>(route.getDepartures().get(departureIdAndTime_s.getFirst()),
					departureIdAndTime_s.getSecond());
		}
	}

	// -------------------- IMPLEMENTATION OF EVENT HANDLERS --------------------

	@Override
	public void reset(final int iteration) {
		this.resetToSchedule();
		this.vehicle2lineRouteDeparture.clear();
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		if (this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			this.vehicle2lineRouteDeparture.put(event.getVehicleId(), new LineRouteOtherId<Departure>(
					event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if (this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			final LineRouteOtherId<Departure> lineRouteDeparture = this.vehicle2lineRouteDeparture
					.get(event.getVehicleId());
			final NextAvailableDepartures nextAvailableDepartures = this.lineRouteStop2nextAvailableDepartures
					.get(new LineRouteOtherId<TransitStopFacility>(lineRouteDeparture.lineId,
							lineRouteDeparture.routeId, event.getFacilityId()));
			nextAvailableDepartures.adjustToRealizedDeparture(event.getTime(), lineRouteDeparture.otherId);
		}
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		if (this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			this.vehicle2lineRouteDeparture.remove(event.getVehicleId());
		}
	}
}
