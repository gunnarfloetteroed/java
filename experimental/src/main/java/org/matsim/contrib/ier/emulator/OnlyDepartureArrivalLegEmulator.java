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
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OnlyDepartureArrivalLegEmulator implements LegEmulator {

	protected final EventsManager eventsManager;
	protected final ActivityFacilities activityFacilities;
	protected final double simEndTime_s;

	private final boolean hasFacilities;

	public OnlyDepartureArrivalLegEmulator(final EventsManager eventsManager,
			final ActivityFacilities activityFacilities, final double simEndTime_s) {
		this.eventsManager = eventsManager;
		this.activityFacilities = activityFacilities;
		this.hasFacilities = (activityFacilities != null) && (activityFacilities.getFacilities() != null);
		this.simEndTime_s = simEndTime_s;
	}

	private Id<Link> getLinkId(final Activity activity) {
		// TODO Revisit this. The current precedence order works with WUM model. Gunnar
		// 2019-09-03
		if ((activity.getFacilityId() != null) && this.hasFacilities) {
			final ActivityFacility facility = this.activityFacilities.getFacilities().get(activity.getFacilityId());
			if ((facility != null) && (facility.getLinkId() != null)) {
				return facility.getLinkId();
			}
		}
		if (activity.getLinkId() != null) {
			return activity.getLinkId();
		}
		throw new RuntimeException("Could not identify a link id for activity " + activity.toString());
		// if (activity.getFacilityId() != null) {
		// return
		// activityFacilities.getFacilities().get(activity.getFacilityId()).getLinkId();
		// } else {
		// return activity.getLinkId();
		// }
	}

	@Override
	public final double emulateLegAndReturnEndTime_s(final Leg leg, final Person person,
			final Activity previousActivity, final Activity nextActivity, double time_s) {

		// time_s = Math.max(time_s, leg.getDepartureTime());
		// if (time_s > this.simEndTime_s) {
		// Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode());
		// return time_s;
		// }

		// Every leg starts with a departure.
		if (time_s <= this.simEndTime_s) {
			this.eventsManager.processEvent(
					new PersonDepartureEvent(time_s, person.getId(), getLinkId(previousActivity), leg.getMode()));
			time_s = this.emulateBetweenDepartureAndArrivalAndReturnEndTime_s(leg, person, time_s);
			// if (time_s > this.simEndTime_s) {
			// Logger.getLogger(this.getClass()).warn("Stuck in " + leg.getMode() + " at
			// time " + time_s + " in class "
			// + this.getClass().getSimpleName());
			// return time_s;
			// }

			// Every leg ends with an arrival.
			if (time_s <= this.simEndTime_s) {
				this.eventsManager.processEvent(
						new PersonArrivalEvent(time_s, person.getId(), getLinkId(nextActivity), leg.getMode()));
			}
		}

		return time_s;
	}

	// Hook for stuff that happens between departure and arrival.
	public double emulateBetweenDepartureAndArrivalAndReturnEndTime_s(final Leg leg, final Person person,
			double time_s) {
		return (time_s + leg.getTravelTime());
	}
}
