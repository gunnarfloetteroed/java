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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class LinkTravelStatistic
		implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private class LinkEntry {
		private Set<Id<Person>> personIds = new LinkedHashSet<>();

		private void registerEntry(Id<Person> personId) {
			this.personIds.add(personId);
		}
	}

	private final Predicate<Double> timeChecker;
	private final Predicate<Id<Link>> linkChecker;
	private final Predicate<Id<Person>> personChecker;
	private final Predicate<Id<Vehicle>> vehicleChecker;

	private final Network network;
	private final boolean writeCapacity;

	private final Map<Id<Vehicle>, Set<Id<Person>>> vehicleId2personIds = new LinkedHashMap<>();

	private final Map<Id<Link>, LinkEntry> linkId2entry = new LinkedHashMap<>();

	LinkTravelStatistic(final Network network, final Predicate<Double> timeChecker,
			final Predicate<Id<Link>> linkChecker, final Predicate<Id<Person>> personChecker,
			final Predicate<Id<Vehicle>> vehicleChecker, final boolean writeCapacity) {
		this.network = network;
		this.timeChecker = timeChecker;
		this.linkChecker = linkChecker;
		this.personChecker = personChecker;
		this.vehicleChecker = vehicleChecker;
		this.writeCapacity = writeCapacity;
	}

	private LinkEntry getOrCreate(final Id<Link> linkId) {
		LinkEntry entry = this.linkId2entry.get(linkId);
		if (entry == null) {
			entry = new LinkEntry();
			this.linkId2entry.put(linkId, entry);
		}
		return entry;
	}

	// EVENT HANDLING

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

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (this.timeChecker.test(event.getTime()) && this.linkChecker.test(event.getLinkId())) {
			final Set<Id<Person>> personIds = this.vehicleId2personIds.get(event.getVehicleId());
			if (personIds != null) {
				personIds.stream().forEach(personId -> this.getOrCreate(event.getLinkId()).registerEntry(personId));
			}
		}
	}

	// FILE WRITING

	void writeLinkData(final String fileName) throws FileNotFoundException {

		final PrintWriter writer = new PrintWriter(fileName);
		writer.println("linkId,flowCapacity[veh/h],entryCnt,agentList");

		for (Map.Entry<Id<Link>, LinkEntry> entry : this.linkId2entry.entrySet()) {
			writer.print(entry.getKey());
			final Link link = this.network.getLinks().get(entry.getKey());
			if (this.writeCapacity) {
				writer.print("," + MathHelpers.round(link.getCapacity()));
			} else {
				writer.print(",*");
			}

			writer.print("," + entry.getValue().personIds.size());
			for (Id<Person> personId : entry.getValue().personIds) {
				writer.print("," + personId);
			}
			writer.println();
		}

		writer.flush();
		writer.close();
	}
}
