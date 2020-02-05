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

		// if (time_s > this.simEndTime_s) {
		// Logger.getLogger(this.getClass()).warn("Stuck in " + activity.getType());
		// return time_s; // stuck
		// }

		if (!isFirstElement) {
			// time_s = Math.max(time_s, activity.getStartTime());
			// if (time_s > this.simEndTime_s) {
			// Logger.getLogger(this.getClass()).warn("Stuck in " + activity.getType());
			// return time_s; // stuck
			// }
			this.eventsManager.processEvent(new ActivityStartEvent(time_s, person.getId(), activity.getLinkId(),
					activity.getFacilityId(), activity.getType()));
		}

		if (isLastElement) {
			time_s = Math.max(time_s, this.simEndTime_s);
		} else {
			time_s = ActivityDurationUtils.calculateDepartureTime(activity, time_s,
					this.activityDurationInterpretation);
			// if (time_s <= this.simEndTime_s) {
			this.eventsManager.processEvent(new ActivityEndEvent(time_s, person.getId(), activity.getLinkId(),
					activity.getFacilityId(), activity.getType()));
			// } else {
			// Logger.getLogger(this.getClass()).warn("Stuck in " + activity.getType());
			// }
		}

		return time_s;
	}
}
