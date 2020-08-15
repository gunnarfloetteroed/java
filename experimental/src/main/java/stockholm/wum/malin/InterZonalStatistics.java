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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.math.MathHelpers;
import stockholm.ihop2.regent.demandreading.ZonalSystem;
import stockholm.ihop2.regent.demandreading.Zone;
import stockholm.ihop4.sampersutilities.MultiLegPTTripSummarizer;
import stockholm.ihop4.sampersutilities.SampersDifferentiatedPTScoringFunction;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class InterZonalStatistics implements LegHandler, ActivityHandler {

	// -------------------- INNER CLASS --------------------

	static class Entry {

		private int observations = 0;

		private double travelTimeSum_s = 0;

		private double travelDistanceSum_m = 0;

		void register(final double travelTime_s, final double travelDistance_m) {
			this.observations++;
			this.travelTimeSum_s += travelTime_s;
			this.travelDistanceSum_m += travelDistance_m;
		}

		Integer getMeanTravelTime_s() {
			if (this.observations == 0) {
				return null;
			} else {
				return MathHelpers.round(this.travelTimeSum_s / this.observations);
			}
		}

		Integer getMeanDistance_m() {
			if (this.observations == 0) {
				return null;
			} else {
				return MathHelpers.round(this.travelDistanceSum_m / this.observations);
			}
		}

		int getObservations() {
			return this.observations;
		}

	}

	// -------------------- CONSTANTS --------------------

	private final ZonalSystem zonalSystem;

	private final Scenario scenario;

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, MultiLegPTTripSummarizer> ptTripSummarizers = new LinkedHashMap<>();

	private final Set<Zone> origins = new LinkedHashSet<>();

	private final Set<Zone> destinations = new LinkedHashSet<>();

	private final Map<Tuple<Zone, Zone>, Entry> od2Entry = new LinkedHashMap<>();

	private long valid = 0;

	private long invalid = 0;

	// -------------------- CONSTRUCTION --------------------

	public InterZonalStatistics(final ZonalSystem zonalSystem, final Scenario scenario) {
		this.zonalSystem = zonalSystem;
		this.scenario = scenario;
	}

	// -------------------- INTERNALS --------------------

	private void assertExistingZone(final String zoneId) {
		if (this.zonalSystem.getZone(zoneId) == null) {
			throw new RuntimeException("Unknown zone: " + zoneId);
		}
	}

	private Entry getOrCreateEntry(final Zone origin, final Zone destination) {
		final Tuple<Zone, Zone> od = new Tuple<>(origin, destination);
		Entry entry = this.od2Entry.get(od);
		if (entry == null) {
			entry = new Entry();
			this.od2Entry.put(od, entry);
		}
		return entry;
	}

	private MultiLegPTTripSummarizer getOrCreateTripSummarizer(final Id<Person> personId) {
		MultiLegPTTripSummarizer ptSummarizer = this.ptTripSummarizers.get(personId);
		if (ptSummarizer == null) {
			ptSummarizer = new MultiLegPTTripSummarizer(SampersDifferentiatedPTScoringFunction.PT_SUBMODES, null,
					(leg) -> {
						final Route route = leg.getRoute();
						final Zone fromZone = this.zonalSystem.getZone(
								this.scenario.getNetwork().getLinks().get(route.getStartLinkId()).getFromNode());
						final Zone toZone = this.zonalSystem
								.getZone(this.scenario.getNetwork().getLinks().get(route.getEndLinkId()).getFromNode());
						if ((fromZone != null) && (toZone != null)) {
							this.getOrCreateEntry(fromZone, toZone).register(route.getTravelTime().seconds(),
									route.getDistance());
							this.valid++;
						} else {
							this.invalid++;
						}
					});
			this.ptTripSummarizers.put(personId, ptSummarizer);
		}
		return ptSummarizer;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addOrigin(final String originId) {
		this.assertExistingZone(originId);
		this.origins.add(this.zonalSystem.getZone(originId));
	}

	public void addDestination(final String destinationId) {
		this.assertExistingZone(destinationId);
		this.destinations.add(this.zonalSystem.getZone(destinationId));
	}

	@Override
	public void handleActivity(final PersonExperiencedActivity act) {
		this.getOrCreateTripSummarizer(act.getAgentId()).handleActivity(act.getActivity());
	}

	@Override
	public void handleLeg(final PersonExperiencedLeg leg) {
		this.getOrCreateTripSummarizer(leg.getAgentId()).handleLeg(leg.getLeg());
	}

	public long getValidCnt() {
		return this.valid;
	}

	public long getInvalidCnt() {
		return this.invalid;
	}

	public void toFolder(final File folder) throws FileNotFoundException {

		final String dataHeader = "flow[persons],travelTime[s],travelDistance[m]";

		final Map<Zone, PrintWriter> origin2writer = new LinkedHashMap<>();
		for (Zone origin : this.origins) {
			final PrintWriter writer = new PrintWriter(FileUtils.getFile(folder, "origin_" + origin.getId() + ".csv"));
			origin2writer.put(origin, writer);
			writer.println("destination," + dataHeader);
		}

		final Map<Zone, PrintWriter> destination2writer = new LinkedHashMap<>();
		for (Zone destination : this.destinations) {
			final PrintWriter writer = new PrintWriter(
					FileUtils.getFile(folder, "destination_" + destination.getId() + ".csv"));
			destination2writer.put(destination, writer);
			writer.println("origin," + dataHeader);
		}

		for (Map.Entry<Tuple<Zone, Zone>, Entry> entry : this.od2Entry.entrySet()) {

			final int flow = entry.getValue().getObservations();
			final int travelTime_s = entry.getValue().getMeanTravelTime_s();
			final int travelDistance_m = entry.getValue().getMeanDistance_m();
			final String data = flow + "," + travelTime_s + "," + travelDistance_m;

			final Zone origin = entry.getKey().getFirst();
			final Zone destination = entry.getKey().getSecond();
			if (origin2writer.containsKey(origin)) {
				origin2writer.get(origin).println(destination.getId() + "," + data);
			}
			if (destination2writer.containsKey(destination)) {
				destination2writer.get(destination).println(origin.getId() + "," + data);
			}
		}

		for (PrintWriter writer : origin2writer.values()) {
			writer.flush();
			writer.close();
		}
		for (PrintWriter writer : destination2writer.values()) {
			writer.flush();
			writer.close();
		}
	}
}
