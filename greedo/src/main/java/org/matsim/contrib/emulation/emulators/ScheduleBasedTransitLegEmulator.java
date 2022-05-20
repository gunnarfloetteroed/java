/*
 * Greedo -- Equilibrium approximation for general-purpose multi-agent simulations.
 *
 * Copyright 2022 Gunnar Flötteröd
 * 
 *
 * This file is part of Greedo.
 *
 * Greedo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greedo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Greedo.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@gmail.com
 *
 */
package org.matsim.contrib.emulation.emulators;

import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ScheduleBasedTransitLegEmulator extends OnlyDepartureArrivalLegEmulator {

	private final TransitSchedule transitSchedule;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public ScheduleBasedTransitLegEmulator(MatsimServices services) {
		super(services);
		this.transitSchedule = services.getScenario().getTransitSchedule();
	}

	// -------------------- INTERNALS --------------------

	private double guessArrivalOffset_s(final OptionalTime arrivalOffset_s, final OptionalTime departureOffset_s) {
		if (arrivalOffset_s.isDefined()) {
			return arrivalOffset_s.seconds();
		} else if (departureOffset_s.isDefined()) {
			return departureOffset_s.seconds();
		} else {
			throw new RuntimeException(
					"arrival offset = " + arrivalOffset_s + ", departure offset = " + departureOffset_s);
		}
	}

	private double guessDepartureOffset_s(final OptionalTime arrivalOffset_s, final OptionalTime departureOffset_s) {
		if (departureOffset_s.isDefined()) {
			return departureOffset_s.seconds();
		} else if (arrivalOffset_s.isDefined()) {
			return arrivalOffset_s.seconds();
		} else {
			throw new RuntimeException(
					"arrival offset = " + arrivalOffset_s + ", departure offset = " + departureOffset_s);
		}
	}

	private boolean routeConnectsStops(final TransitRoute route, final TransitRouteStop fromStop,
			final TransitRouteStop toStop) {

		Integer earliestFromStopIndex = null;
		for (int i = 0; (i < route.getStops().size()) && (earliestFromStopIndex == null); i++) {
			if (route.getStops().get(i).getStopFacility().getId().equals(fromStop.getStopFacility().getId())) {
				earliestFromStopIndex = i;
			}
		}
		if (earliestFromStopIndex == null) {
			return false;
		}

		Integer latestToStopIndex = null;
		for (int j = route.getStops().size() - 1; (j >= 0) && (latestToStopIndex == null); j--) {
			if (route.getStops().get(j).getStopFacility().getId().equals(toStop.getStopFacility().getId())) {
				latestToStopIndex = j;
			}
		}
		if (latestToStopIndex == null) {
			return false;
		}

		return (earliestFromStopIndex <= latestToStopIndex);
	}

	private Departure getNextDeparture(final double time_s, final TransitLine line, final TransitRouteStop fromStop,
			final TransitRouteStop toStop) {
		final double earliestAllowedDepartureTime_s = time_s
				- this.guessDepartureOffset_s(fromStop.getArrivalOffset(), fromStop.getDepartureOffset());
		Departure result = null;
		for (TransitRoute route : line.getRoutes().values()) {
			if (this.routeConnectsStops(route, fromStop, toStop)) {
				for (Departure candidate : route.getDepartures().values()) {
					if ((earliestAllowedDepartureTime_s <= candidate.getDepartureTime())
							&& ((result == null) || (candidate.getDepartureTime() < result.getDepartureTime()))) {
						result = candidate;
					}
				}
			}
		}
		return result;
	}

	// -------------------- IMPLEMENTATION OF LegEmulator --------------------

	@Override
	public double emulateBetweenDepartureAndArrivalAndReturnEndTime_s(final Leg leg, final Person person,
			double time_s) {

		if (time_s <= this.simEndTime_s) {

			final TransitPassengerRoute legRoute = (TransitPassengerRoute) leg.getRoute();
			this.eventsManager.processEvent(new AgentWaitingForPtEvent(time_s, person.getId(),
					legRoute.getAccessStopId(), legRoute.getEgressStopId()));

			final TransitLine line = this.transitSchedule.getTransitLines().get(legRoute.getLineId());
			final TransitRoute transitRoute = line.getRoutes().get(legRoute.getRouteId());
			final TransitRouteStop fromStop = transitRoute
					.getStop(this.transitSchedule.getFacilities().get(legRoute.getAccessStopId()));
			final TransitRouteStop toStop = transitRoute
					.getStop(this.transitSchedule.getFacilities().get(legRoute.getEgressStopId()));
			final Departure departure = this.getNextDeparture(time_s, line, fromStop, toStop);
			if (departure == null) {
				return this.simEndTime_s + 1; // Fairly arbitrary.. Gunnar 2019-09-10.
			}

			time_s = departure.getDepartureTime()
					+ this.guessDepartureOffset_s(fromStop.getArrivalOffset(), fromStop.getDepartureOffset());
			if (time_s <= this.simEndTime_s) {
				this.eventsManager.processEvent(new PersonEntersVehicleEvent(time_s, person.getId(), null));
				time_s = departure.getDepartureTime()
						+ this.guessArrivalOffset_s(toStop.getArrivalOffset(), toStop.getDepartureOffset());
				if (time_s <= this.simEndTime_s) {
					this.eventsManager.processEvent(new PersonLeavesVehicleEvent(time_s, person.getId(), null));
				}
			}
		}

		return time_s;
	}

}
