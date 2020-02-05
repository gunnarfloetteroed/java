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
package org.matsim.contrib.ier.emulator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;

/**
 * TODO: Include toll.
 *
 * @author Gunnar Flötteröd
 *
 */
public class CarLegEmulator extends OnlyDepartureArrivalLegEmulator {

	private final Network network;
	private final TravelTime travelTime;

	public CarLegEmulator(final EventsManager eventsManager, final Network network, final TravelTime travelTime,
			ActivityFacilities activityFacilities, final double simEndTime_s) {
		super(eventsManager, activityFacilities, simEndTime_s);
		this.network = network;
		this.travelTime = travelTime;
	}

	@Override
	public double emulateBetweenDepartureAndArrivalAndReturnEndTime_s(final Leg leg, final Person person,
			double time_s) {

		// time_s = Math.max(time_s, leg.getDepartureTime());
		// if (time_s > super.simEndTime_s) {
		// Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
		// return time_s;
		// }

		// if (!(leg.getRoute() instanceof NetworkRoute)) {
		// throw new RuntimeException(
		// "Expecting a " + NetworkRoute.class.getSimpleName() + " when emulating a car
		// leg.");
		// }
		final NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
		if (!networkRoute.getStartLinkId().equals(networkRoute.getEndLinkId())) {

			final Id<Vehicle> vehicleId = Id.createVehicleId(person.getId());

			// First link of a network route.
			Link link = this.network.getLinks().get(networkRoute.getStartLinkId());
			this.eventsManager.processEvent(
					new VehicleEntersTrafficEvent(time_s, person.getId(), link.getId(), vehicleId, leg.getMode(), 0.0));
			time_s += this.travelTime.getLinkTravelTime(link, time_s, person, null);
			// if (time_s > super.simEndTime_s) {
			// Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
			// return time_s;
			// }
			this.eventsManager.processEvent(new LinkLeaveEvent(time_s, vehicleId, link.getId()));

			// Intermediate links of a network route.
			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				link = this.network.getLinks().get(linkId);
				this.eventsManager.processEvent(new LinkEnterEvent(time_s, vehicleId, link.getId()));
				time_s += this.travelTime.getLinkTravelTime(link, time_s, person, null);
				// if (time_s > super.simEndTime_s) {
				// Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
				// return time_s;
				// }
				this.eventsManager.processEvent(new LinkLeaveEvent(time_s, vehicleId, link.getId()));
			}

			// Last link of a network route.
			this.eventsManager.processEvent(new LinkEnterEvent(time_s, vehicleId, networkRoute.getEndLinkId()));
			this.eventsManager.processEvent(new VehicleLeavesTrafficEvent(time_s, person.getId(),
					networkRoute.getEndLinkId(), vehicleId, leg.getMode(), 0.0));
		}

		return time_s;
	}
}
