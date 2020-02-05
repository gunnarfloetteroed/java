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
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SlotUsageListener implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
		PersonEntersVehicleEventHandler, VehicleLeavesTrafficEventHandler {

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2indicators;

	private final PrivateTrafficLinkUsageListener privateTrafficLinkUsageListener;

	private final TransitVehicleUsageListener transitVehicleUsageListener;

	private Integer lastResetIteration = null;

	private boolean reactToReset = true;

	// -------------------- CONSTRUCTION --------------------

	public SlotUsageListener(final TimeDiscretization timeDiscretization, final Map<Id<Person>, Double> personWeights,
			final Map<Id<Link>, Double> linkWeights, final Map<Id<Vehicle>, Double> transitVehicleWeights) {
		this.personId2indicators = new ConcurrentHashMap<>(); // Shared by different listeners.
		this.privateTrafficLinkUsageListener = new PrivateTrafficLinkUsageListener(timeDiscretization,
				this.personId2indicators, linkWeights, personWeights);
		this.transitVehicleUsageListener = new TransitVehicleUsageListener(timeDiscretization, this.personId2indicators,
				transitVehicleWeights, personWeights);
	}

	// -------------------- SETTERS --------------------

	public void setReactToReset(final boolean reactToReset) {
		this.reactToReset = reactToReset;
	}

	public void updatePersonWeights(final Map<Id<Person>, Double> personWeights) {
		this.privateTrafficLinkUsageListener.updatePersonWeights(personWeights);
		this.transitVehicleUsageListener.updatePersonWeights(personWeights);
	}

	// -------------------- CONTENT ACCESS --------------------

	public Integer getLastResetIteration() {
		return this.lastResetIteration;
	}

	public Map<Id<Person>, SpaceTimeIndicators<Id<?>>> getIndicatorView() {
		return Collections.unmodifiableMap(this.personId2indicators);
	}

	// -------------------- IMPLEMENTATION OF *EventHandler --------------------

	@Override
	public synchronized void reset(final int iteration) {
		this.lastResetIteration = iteration;
		if (this.reactToReset) {
			this.privateTrafficLinkUsageListener.reset(iteration);
			this.transitVehicleUsageListener.reset(iteration);
		}
	}

	@Override
	public synchronized void handleEvent(final VehicleEntersTrafficEvent event) {
		this.privateTrafficLinkUsageListener.handleEvent(event);
	}

	@Override
	public synchronized void handleEvent(final LinkEnterEvent event) {
		this.privateTrafficLinkUsageListener.handleEvent(event);
	}

	@Override
	public synchronized void handleEvent(final PersonEntersVehicleEvent event) {
		this.transitVehicleUsageListener.handleEvent(event);
	}

	@Override
	public synchronized void handleEvent(final VehicleLeavesTrafficEvent event) {
		this.privateTrafficLinkUsageListener.handleEvent(event);
	}
}
