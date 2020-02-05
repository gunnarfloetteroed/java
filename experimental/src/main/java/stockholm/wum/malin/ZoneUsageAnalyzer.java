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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;

import floetteroed.utilities.Units;
import floetteroed.utilities.math.MathHelpers;
import stockholm.ihop2.regent.demandreading.ZonalSystem;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class ZoneUsageAnalyzer extends AbstractZoneAnalyzer implements ActivityStartEventHandler, ActivityEndEventHandler {

	// -------------------- INNER CLASS --------------------

	private class ZoneActivity {

		final String zoneId;
		final double startTime_s;
		Double endTime_s = null;

		ZoneActivity(String zoneId, double startTime_s) {
			this.zoneId = zoneId;
			this.startTime_s = startTime_s;
		}
	}

	// -------------------- CONSTANTS --------------------

	private final double analysisStartTime_s;

	private final double analysisEndTime_s;

	private final Predicate<Id<Person>> personChecker;

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, ZoneActivity> personId2mostRecentActivity = new LinkedHashMap<>();

	private final Map<String, Set<Id<Person>>> zoneId2personIds = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ZoneUsageAnalyzer(final ZonalSystem zonalSystem, final Scenario scenario, final double analysisStartTime_s,
			final double analysisEndTime_s, final Predicate<Id<Person>> personChecker) {
		super(zonalSystem, scenario);
		this.analysisStartTime_s = analysisStartTime_s;
		this.analysisEndTime_s = analysisEndTime_s;
		this.personChecker = personChecker;
	}

	// -------------------- INTERNALS --------------------

	private Set<Id<Person>> getOrAddPersonIds(final String zoneId) {
		Set<Id<Person>> personIds = this.zoneId2personIds.get(zoneId);
		if (personIds == null) {
			personIds = new LinkedHashSet<>();
			this.zoneId2personIds.put(zoneId, personIds);
		}
		return personIds;
	}

	private void processCompletedActivity(final Id<Person> personId, final ZoneActivity mostRecentActivity) {
		if (MathHelpers.overlap(this.analysisStartTime_s, this.analysisEndTime_s, mostRecentActivity.startTime_s,
				mostRecentActivity.endTime_s) > 0.0) {
			this.getOrAddPersonIds(mostRecentActivity.zoneId).add(personId);
		}
	}

	void complete() {
		for (Map.Entry<Id<Person>, ZoneActivity> entry : this.personId2mostRecentActivity.entrySet()) {
			entry.getValue().endTime_s = Units.S_PER_D;
			this.processCompletedActivity(entry.getKey(), entry.getValue());
		}
		this.personId2mostRecentActivity.clear();
	}

	Map<String, Set<Id<Person>>> getZoneId2personIdsView() {
		return Collections.unmodifiableMap(this.zoneId2personIds);
	}
	
	// -------------------- IMPLEMENTATION OF *EventHandler --------------------

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if (!this.personChecker.test(event.getPersonId())) {
			return;
		}
		final String zoneId = super.getZoneId(event.getLinkId());
		if (zoneId != null) {
			this.personId2mostRecentActivity.put(event.getPersonId(), new ZoneActivity(zoneId, event.getTime()));
		}
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		if (!this.personChecker.test(event.getPersonId())) {
			return;
		}
		final String zoneId = super.getZoneId(event.getLinkId());
		if (zoneId != null) {
			final ZoneActivity mostRecentActivity;
			if (this.personId2mostRecentActivity.containsKey(event.getPersonId())) {
				mostRecentActivity = this.personId2mostRecentActivity.get(event.getPersonId());
			} else {
				mostRecentActivity = new ZoneActivity(zoneId, 0.0);
			}
			mostRecentActivity.endTime_s = event.getTime();
			this.processCompletedActivity(event.getPersonId(), mostRecentActivity);
			this.personId2mostRecentActivity.remove(event.getPersonId());
		}
	}
}
