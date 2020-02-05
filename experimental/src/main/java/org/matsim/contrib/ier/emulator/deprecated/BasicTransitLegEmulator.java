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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ier.emulator.OnlyDepartureArrivalLegEmulator;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.pt.routes.ExperimentalTransitRoute;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class BasicTransitLegEmulator extends OnlyDepartureArrivalLegEmulator {

	// -------------------- CONSTRUCTION --------------------

	public BasicTransitLegEmulator(final EventsManager eventsManager, final Scenario scenario) {
		super(eventsManager, scenario.getActivityFacilities(), scenario.getConfig().qsim().getEndTime());
	}

	// -------------------- IMPLEMENTATION OF LegEmulator --------------------

	@Override
	public double emulateBetweenDepartureAndArrivalAndReturnEndTime_s(Leg leg, Person person, double time_s) {

		time_s = Math.max(time_s, leg.getDepartureTime());
//		if (time_s > super.simEndTime_s) {
//			Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
//			return time_s;
//		}
		
		final ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
		this.eventsManager.processEvent(
				new AgentWaitingForPtEvent(time_s, person.getId(), route.getAccessStopId(), route.getEgressStopId()));
		this.eventsManager.processEvent(new PersonEntersVehicleEvent(time_s, person.getId(), null));
		time_s += leg.getTravelTime();
//		if (time_s > super.simEndTime_s) {
//			Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
//			return time_s;
//		}
		this.eventsManager
				.processEvent(new PersonLeavesVehicleEvent(time_s, person.getId(), null));
		return time_s;
	}
}
