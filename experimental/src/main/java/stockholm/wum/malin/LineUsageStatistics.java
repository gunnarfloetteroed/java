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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Time;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LineUsageStatistics implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		VehicleArrivesAtFacilityEventHandler {

	private class Entry {
		private final Id<Person> traveler;
		private final String whatHappened;
		private final String stopName;
		private final double time_s;

		Entry(final Id<Person> traveler, final String whatHappened, Id<TransitStopFacility> stop, final double time_s) {
			this.traveler = traveler;
			this.whatHappened = whatHappened;
			this.stopName = scenario.getTransitSchedule().getFacilities().get(stop).getName();
			this.time_s = time_s;
		}

		@Override
		public String toString() {
			return "Traveler " + this.traveler + " " + this.whatHappened + " at stop " + this.stopName + " at time "
					+ Time.strFromSec((int) time_s);
		}

	}

	private final Scenario scenario;

	private final Map<Id<Vehicle>, Id<TransitStopFacility>> vehicleId2stopId = new LinkedHashMap<>();

	private List<Entry> entries = new LinkedList<>();

	private final Predicate<Id<Vehicle>> vehicleSelector;

	private final Predicate<Double> timeSelector;

	private final Predicate<Id<Person>> personSelector;

	private final Predicate<Id<TransitStopFacility>> stopSelector;

	private final Set<Id<Person>> travelers = new LinkedHashSet<>();

	public LineUsageStatistics(final Predicate<Id<Vehicle>> vehicleSelector, final Predicate<Double> timeSelector,
			final Predicate<Id<Person>> personSelector, final Predicate<Id<TransitStopFacility>> stopSelector,
			final Scenario scenario) {
		this.vehicleSelector = vehicleSelector;
		this.timeSelector = timeSelector;
		this.personSelector = personSelector;
		this.stopSelector = stopSelector;
		this.scenario = scenario;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehicleSelector.test(event.getVehicleId()) && this.timeSelector.test(event.getTime())
				&& this.personSelector.test(event.getPersonId())
				&& this.stopSelector.test(this.vehicleId2stopId.get(event.getVehicleId()))) {
			event.getPersonId();
			event.getVehicleId();
			this.travelers.add(event.getPersonId());
			this.entries.add(new Entry(event.getPersonId(), "enters vehicle",
					this.vehicleId2stopId.get(event.getVehicleId()), event.getTime()));
		}
	}

	public Set<Id<Person>> getTravelers() {
		return this.travelers;
	}

	@Override
	public void handleEvent(final VehicleArrivesAtFacilityEvent event) {
		if (this.vehicleSelector.test(event.getVehicleId()) && this.timeSelector.test(event.getTime())) {
			this.vehicleId2stopId.put(event.getVehicleId(), event.getFacilityId());
		}

	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.vehicleSelector.test(event.getVehicleId()) && this.timeSelector.test(event.getTime())
				&& this.personSelector.test(event.getPersonId())
				&& this.stopSelector.test(this.vehicleId2stopId.get(event.getVehicleId()))) {
			event.getPersonId();
			event.getVehicleId();
			this.travelers.add(event.getPersonId());
			this.entries.add(new Entry(event.getPersonId(), "leaves vehicle",
					this.vehicleId2stopId.get(event.getVehicleId()), event.getTime()));
		}
	}

	public String getEntryExitLog() {
		final StringBuffer result = new StringBuffer();
		for (Entry entry : this.entries) {
			result.append(entry.toString());
			result.append("\n");
		}
		return result.toString();
	}
}
