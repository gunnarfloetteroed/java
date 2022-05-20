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

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.mobsim.qsim.agents.ActivityDurationUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class BasicActivityEmulator implements ActivityEmulator {

	private final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;
	private final EventsManager eventsManager;
	private final double simEndTime_s;

	public BasicActivityEmulator(final EventsManager eventsManager, final double simEndTime_s,
			final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation) {
		this.eventsManager = eventsManager;
		this.simEndTime_s = simEndTime_s;
		this.activityDurationInterpretation = activityDurationInterpretation;
	}

	public double emulateActivityAndReturnEndTime_s(final Activity activity, final Person person, double time_s,
			final boolean isFirstElement, final boolean isLastElement) {
		if (!isFirstElement) {
			this.eventsManager.processEvent(new ActivityStartEvent(time_s, person.getId(), activity.getLinkId(),
					activity.getFacilityId(), activity.getType(), activity.getCoord()));
		}
		if (isLastElement) {
			time_s = Math.max(time_s, this.simEndTime_s);
		} else {
			time_s = ActivityDurationUtils.calculateDepartureTime(activity, time_s,
					this.activityDurationInterpretation);
			this.eventsManager.processEvent(new ActivityEndEvent(time_s, person.getId(), activity.getLinkId(),
					activity.getFacilityId(), activity.getType()));
		}
		return time_s;
	}
}
