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
package org.matsim.contrib.greedo.listeners;

import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;

/**
 * Keeps track of when every single passenger enters which transit vehicle.
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransitVehicleUsageListener implements PersonEntersVehicleEventHandler {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	// Maps a person on all vehicle-time-slots used by that person.
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> passengerId2indicators;

	private Map<Id<Person>, Double> personWeights = null;

	private final Map<Id<Vehicle>, Double> vehicleWeights;

	// -------------------- CONSTRUCTION --------------------

	public TransitVehicleUsageListener(final TimeDiscretization timeDiscretization,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> passengerId2EntryIndicators,
			final Map<Id<Vehicle>, Double> transitVehicleWeights, final Map<Id<Person>, Double> personWeights) {
		this.timeDiscretization = timeDiscretization;
		this.passengerId2indicators = passengerId2EntryIndicators;
		this.vehicleWeights = transitVehicleWeights;
		this.personWeights = personWeights;
	}

	// -------------------- SETTERS -------------------

	public void updatePersonWeights(final Map<Id<Person>, Double> personWeights) {
		this.personWeights = personWeights;
	}

	// -------------------- IMPLEMENTATION --------------------

	Map<Id<Person>, SpaceTimeIndicators<Id<?>>> getIndicatorView() {
		return Collections.unmodifiableMap(this.passengerId2indicators);
	}

	// --------------- IMPLEMENTATION OF EventHandler INTERFACES ---------------

	@Override
	public void reset(int iteration) {
		this.passengerId2indicators.clear();
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		final double time_s = event.getTime();
		if ((event.getVehicleId() != null) && (event.getPersonId() != null)
				&& (time_s >= this.timeDiscretization.getStartTime_s())
				&& (time_s < this.timeDiscretization.getEndTime_s())) {
			final Double personWeight = this.personWeights.get(event.getPersonId());
			if (personWeight != null) {
				final Double vehicleWeight = this.vehicleWeights.get(event.getVehicleId());
				if (vehicleWeight != null) {
					SpaceTimeIndicators<Id<?>> indicators = this.passengerId2indicators.get(event.getPersonId());
					if (indicators == null) {
						indicators = new SpaceTimeIndicators<Id<?>>(this.timeDiscretization.getBinCnt());
						this.passengerId2indicators.put(event.getPersonId(), indicators);
					}
					indicators.visit(event.getVehicleId(), this.timeDiscretization.getBin(time_s), personWeight,
							vehicleWeight);
				}
			}
		}
	}
}
