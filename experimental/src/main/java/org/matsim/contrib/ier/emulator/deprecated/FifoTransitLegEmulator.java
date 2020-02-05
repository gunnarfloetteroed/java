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

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ier.emulator.OnlyDepartureArrivalLegEmulator;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import floetteroed.utilities.Time;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class FifoTransitLegEmulator extends OnlyDepartureArrivalLegEmulator {

	// -------------------- MEMBERS --------------------

	private final FifoTransitPerformance fifoTransitPerformance;

	private final Map<Id<TransitLine>, TransitLine> transitLines;

	private final Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities;

	// -------------------- CONSTRUCTION --------------------

	public FifoTransitLegEmulator(final EventsManager eventsManager,
			final FifoTransitPerformance fifoTransitPerformance, final Scenario scenario) {
		super(eventsManager, scenario.getActivityFacilities(), scenario.getConfig().qsim().getEndTime());
		this.fifoTransitPerformance = fifoTransitPerformance;
		this.transitLines = scenario.getTransitSchedule().getTransitLines();
		this.stopFacilities = scenario.getTransitSchedule().getFacilities();
	}

	// -------------------- IMPLEMENTATION OF LegEmulator --------------------

	@Override
	public double emulateBetweenDepartureAndArrivalAndReturnEndTime_s(Leg leg, Person person, double time_s) {

		time_s = Math.max(time_s, leg.getDepartureTime());
//		if (time_s > super.simEndTime_s) {
//			Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
//			return time_s; // stuck
//		}

		final ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
		final Id<TransitStopFacility> accessStopId = route.getAccessStopId();
		final Id<TransitStopFacility> egressStopId = route.getEgressStopId();
		final TransitLine line = this.transitLines.get(route.getLineId());
		final TransitRoute transitRoute = line.getRoutes().get(route.getRouteId());

		this.eventsManager.processEvent(new AgentWaitingForPtEvent(time_s, person.getId(), accessStopId, egressStopId));

		final Tuple<Departure, Double> nextDepartureAndTime_s = this.fifoTransitPerformance
				.getNextDepartureAndTime_s(line.getId(), transitRoute, accessStopId, time_s);
		if (nextDepartureAndTime_s == null) {
			Logger.getLogger(this.getClass()).warn(
					"found no next departure at time " + time_s + "s == " + Time.strFromSec((int) Math.round(time_s)));
			return Double.POSITIVE_INFINITY; // stuck
		} else {

			time_s = Math.max(time_s, nextDepartureAndTime_s.getSecond());
//			if (time_s > super.simEndTime_s) {
//				Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
//				return time_s; // stuck
//			}
			this.eventsManager.processEvent(new PersonEntersVehicleEvent(time_s, person.getId(), null));

			time_s += (transitRoute.getStop(this.stopFacilities.get(egressStopId)).getArrivalOffset()
					- transitRoute.getStop(this.stopFacilities.get(accessStopId)).getDepartureOffset());
//			if (time_s > super.simEndTime_s) {
//				Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
//				return time_s; // stuck
//			}
			this.eventsManager.processEvent(new PersonLeavesVehicleEvent(time_s, person.getId(), null));

			return time_s;
		}
	}
}
