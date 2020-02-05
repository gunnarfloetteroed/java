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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;

import stockholm.ihop2.regent.demandreading.ZonalSystem;
import stockholm.ihop2.regent.demandreading.Zone;

import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class PersonTravelStatistics implements ActivityHandler, LegHandler {

	class PersonEntry {

		private Map<String, String> actType2zoneId = new LinkedHashMap<>();
		double totalTravelTime_s = 0;
		double totalTravelDistance_m = 0;

		PersonEntry() {
		}

		void update(final Activity act) {
			final Link link = network.getLinks().get(act.getLinkId());
			Zone zone = zonalSystem.getZone(link.getFromNode());
			if (zone == null) {
				zone = zonalSystem.getZone(link.getToNode());
			}
			if (zone != null) {
				this.actType2zoneId.put(act.getType(), zone.getId());
			}
		}

		void update(final Leg leg) {
			this.totalTravelTime_s += leg.getTravelTime();
			this.totalTravelDistance_m += leg.getRoute().getDistance();
		}
	}

	private final Predicate<PersonExperiencedActivity> experiencedActivitySelector;

	private final Predicate<PersonExperiencedLeg> experiencedLegSelector;

	private final Network network;

	private final ZonalSystem zonalSystem;

	final Map<Id<Person>, PersonEntry> personId2entry = new LinkedHashMap<>();

	PersonTravelStatistics(final Predicate<PersonExperiencedActivity> experiencedActivitySelector,
			final Predicate<PersonExperiencedLeg> experiencedLegSelector, final Network network,
			final ZonalSystem zonalSystem) {
		this.experiencedActivitySelector = experiencedActivitySelector;
		this.experiencedLegSelector = experiencedLegSelector;
		this.network = network;
		this.zonalSystem = zonalSystem;
	}

	private PersonEntry getOrCreate(final Id<Person> personId) {
		PersonEntry entry = this.personId2entry.get(personId);
		if (entry == null) {
			entry = new PersonEntry();
			this.personId2entry.put(personId, entry);
		}
		return entry;
	}

	@Override
	public void handleActivity(PersonExperiencedActivity experiencedActivity) {
		if (this.experiencedActivitySelector.test(experiencedActivity)) {
			this.getOrCreate(experiencedActivity.getAgentId()).update(experiencedActivity.getActivity());
		}
	}

	@Override
	public void handleLeg(PersonExperiencedLeg experiencedLeg) {
		if (this.experiencedLegSelector.test(experiencedLeg)) {
			this.getOrCreate(experiencedLeg.getAgentId()).update(experiencedLeg.getLeg());
		}
	}

	void writePersonData(final String fileName) throws FileNotFoundException {

		final Set<String> allActs = new LinkedHashSet<>();
		for (PersonEntry entry : this.personId2entry.values()) {
			allActs.addAll(entry.actType2zoneId.keySet());
		}

		final PrintWriter writer = new PrintWriter(fileName);
		writer.print("personId,totalTravelTime[s],totalTravelDistance[m]");
		for (String act : allActs) {
			writer.print("," + act);
		}
		writer.println();

		for (Map.Entry<Id<Person>, PersonEntry> entry : this.personId2entry.entrySet()) {
			writer.print(entry.getKey());
			writer.print("," + entry.getValue().totalTravelTime_s);
			writer.print("," + entry.getValue().totalTravelDistance_m);
			for (String act : allActs) {
				final String zoneId = entry.getValue().actType2zoneId.get(act);
				writer.print("," + (zoneId != null ? zoneId : ""));
			}
			writer.println();
		}

		writer.flush();
		writer.close();
	}
}
