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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OnlyDepartureArrivalLegEmulator extends AbstractLegEmulator { // implements LegEmulator {

	protected final ActivityFacilities activityFacilities;
	protected final double simEndTime_s;

	private final boolean hasFacilities;

	protected EventsManager eventsManager;

	@Inject
	public OnlyDepartureArrivalLegEmulator(MatsimServices services) {
		this.activityFacilities = services.getScenario().getActivityFacilities();
		this.hasFacilities = (activityFacilities != null) && (activityFacilities.getFacilities() != null);
		this.simEndTime_s = services.getConfig().qsim().getEndTime().seconds();
	}

	@Override
	public void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	@Override
	public void setOverridingTravelTime(TravelTime travelTime) {
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
	}

	@Override
	public double emulateLegAndReturnEndTime_s(int legIndexInPlan, List<PlanElement> planElements, Person person,
			double time_s) {
		final Leg leg = this.getLeg(legIndexInPlan, planElements);
		final Activity previousActivity = this.getPreviousActivity(legIndexInPlan, planElements);
		final Activity nextActivity = this.getFollowingActivity(legIndexInPlan, planElements);

		// Every leg starts with a departure.
		if (time_s <= this.simEndTime_s) {
			this.eventsManager.processEvent(
					new PersonDepartureEvent(time_s, person.getId(), getLinkId(previousActivity), leg.getMode()));
			
			time_s = this.emulateBetweenDepartureAndArrivalAndReturnEndTime_s(leg, person, time_s);

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
		return (time_s + leg.getTravelTime().seconds());
	}
}
