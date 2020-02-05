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
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;

/**
 * Keeps track of when every single private vehicle enters which link.
 * 
 * @author Gunnar Flötteröd
 *
 */
class PrivateTrafficLinkUsageListener
		implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	private final Map<Id<Vehicle>, Id<Person>> privateVehicleId2DriverId = new LinkedHashMap<>();

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> driverId2indicators;

	private final Map<Id<Link>, Double> linkWeights;

	private Map<Id<Person>, Double> personWeights;

	// -------------------- CONSTRUCTION --------------------

	PrivateTrafficLinkUsageListener(final TimeDiscretization timeDiscretization,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> driverId2indicators,
			final Map<Id<Link>, Double> linkWeights, final Map<Id<Person>, Double> personWeights) {
		this.timeDiscretization = timeDiscretization;
		this.driverId2indicators = driverId2indicators;
		this.linkWeights = linkWeights;
		this.personWeights = personWeights;
	}

	// -------------------- SETTERS -------------------

	public void updatePersonWeights(final Map<Id<Person>, Double> personWeights) {
		this.personWeights = personWeights;
	}

	// -------------------- RESULT ACCESS --------------------

	Map<Id<Person>, SpaceTimeIndicators<Id<?>>> getDriverId2IndicatorsView() {
		return Collections.unmodifiableMap(this.driverId2indicators);
	}

	// -------------------- INTERNALS --------------------

	private void registerLinkEntry(final Id<Link> linkId, final Id<Vehicle> vehicleId, final double time_s) {
		final Id<Person> driverId = this.privateVehicleId2DriverId.get(vehicleId);
		if ((driverId != null) && (time_s >= this.timeDiscretization.getStartTime_s())
				&& (time_s < this.timeDiscretization.getEndTime_s())) {
			final Double personWeight = this.personWeights.get(driverId);
			if (personWeight != null) {
				final Double linkWeight = this.linkWeights.get(linkId);
				if (linkWeight != null) {
					SpaceTimeIndicators<Id<?>> indicators = this.driverId2indicators.get(driverId);
					if (indicators == null) {
						indicators = new SpaceTimeIndicators<Id<?>>(this.timeDiscretization.getBinCnt());
						this.driverId2indicators.put(driverId, indicators);
					}
					indicators.visit(linkId, this.timeDiscretization.getBin(time_s), personWeight, linkWeight);
				}
			}
		}
	}

	// --------------- IMPLEMENTATION OF EventHandler INTERFACES ---------------

	@Override
	public void reset(int iteration) {
		this.privateVehicleId2DriverId.clear();
		this.driverId2indicators.clear();
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		final Id<Person> driverId = event.getPersonId();
		if (driverId != null) {
			this.privateVehicleId2DriverId.put(event.getVehicleId(), driverId);
			this.registerLinkEntry(event.getLinkId(), event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.registerLinkEntry(event.getLinkId(), event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		this.privateVehicleId2DriverId.remove(event.getVehicleId());
	}
}
