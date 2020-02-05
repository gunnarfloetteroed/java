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
package stockholm.wum.malin;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Keeps track of when every single passenger enters which transit vehicle.
 * 
 * @author Gunnar Flötteröd
 *
 */
class PersonVehicleTracker implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	// -------------------- MEMBERS --------------------

	private final Predicate<Id<Person>> personChecker;

	private final Predicate<Id<Vehicle>> vehicleChecker;

	private final Map<Id<Vehicle>, Set<Id<Person>>> vehicleId2personIds = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	PersonVehicleTracker(final Predicate<Id<Person>> personChecker, final Predicate<Id<Vehicle>> vehicleChecker) {
		this.personChecker = personChecker;
		this.vehicleChecker = vehicleChecker;
	}

	// -------------------- IMPLEMENTATION OF *EventHandler --------------------

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.personChecker.test(event.getPersonId()) && this.vehicleChecker.test(event.getVehicleId())) {
			Set<Id<Person>> personIds = this.vehicleId2personIds.get(event.getVehicleId());
			if (personIds == null) {
				personIds = new LinkedHashSet<>();
				this.vehicleId2personIds.put(event.getVehicleId(), personIds);
			}
			personIds.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.personChecker.test(event.getPersonId()) && this.vehicleChecker.test(event.getVehicleId())) {
			Set<Id<Person>> personIds = this.vehicleId2personIds.get(event.getVehicleId());
			personIds.remove(event.getPersonId());
			if (personIds.isEmpty()) {
				this.vehicleId2personIds.remove(event.getVehicleId());
			}

		}
	}
}
