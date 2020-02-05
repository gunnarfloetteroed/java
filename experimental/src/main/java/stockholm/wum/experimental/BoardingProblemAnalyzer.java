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
package stockholm.wum.experimental;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class BoardingProblemAnalyzer
		implements BoardingDeniedEventHandler, PersonStuckEventHandler, ActivityEndEventHandler {

	public double earliestStuckTime_s = Double.POSITIVE_INFINITY;
	public double latestStuckTime_s = Double.NEGATIVE_INFINITY;

	public Map<Id<Person>, Integer> personId2deniedBoardingCnt = new LinkedHashMap<>();

	public Map<String, Integer> stuckMode2cnt = new LinkedHashMap<>();

	private void incBoardingDenials(Id<Person> personId) {
		Integer val = this.personId2deniedBoardingCnt.get(personId);
		this.personId2deniedBoardingCnt.put(personId, (val == null) ? 1 : val + 1);
	}

	private void incStuckModeCnt(final String stuckMode) {
		Integer val = this.stuckMode2cnt.get(stuckMode);
		this.stuckMode2cnt.put(stuckMode, (val == null) ? 1 : val + 1);
	}

	@Override
	public void handleEvent(BoardingDeniedEvent e) {
		this.incBoardingDenials(e.getPersonId());
	}

	public Set<Id<Person>> stuckPersons = new LinkedHashSet<>();

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.earliestStuckTime_s = Math.min(event.getTime(), this.earliestStuckTime_s);
		this.latestStuckTime_s = Math.max(event.getTime(), this.latestStuckTime_s);
		this.incStuckModeCnt(event.getLegMode());
		this.stuckPersons.add(event.getPersonId());
	}

	public Map<Id<Person>, List<ActivityEndEvent>> person2ActivityEnds = new LinkedHashMap<>();

	@Override
	public void handleEvent(ActivityEndEvent event) {
		List<ActivityEndEvent> list = this.person2ActivityEnds.get(event.getPersonId());
		if (list == null) {
			list = new ArrayList<>();
			this.person2ActivityEnds.put(event.getPersonId(), list);
		}
		list.add(event);
	}

	public void printLastActivitiesOfStuckPersons() {
		for (Id<Person> personId : this.stuckPersons) {
			System.out.println("  person: " + personId);
			for (ActivityEndEvent event : this.person2ActivityEnds.get(personId)) {
				System.out.println(
						"    time[h]: " + event.getTime() / 3600.0 + "; " + event);				
			}
		}
	}
}
