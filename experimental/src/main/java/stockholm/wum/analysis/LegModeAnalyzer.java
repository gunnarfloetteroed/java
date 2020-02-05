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
package stockholm.wum.analysis;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LegModeAnalyzer implements PersonDepartureEventHandler, ActivityStartEventHandler {

	private final Map<Id<Person>, List<String>> traveler2ModeSequence = new LinkedHashMap<>();

	private final Map<List<String>, Integer> modeSequence2Cnt = new LinkedHashMap<>();

	public LegModeAnalyzer() {
	}

	public void parse(final String eventsFile) {
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		EventsUtils.readEvents(events, eventsFile);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		List<String> modeSequence = this.traveler2ModeSequence.get(event.getPersonId());
		if (modeSequence == null) {
			modeSequence = new LinkedList<>();
			this.traveler2ModeSequence.put(event.getPersonId(), modeSequence);
		}
		modeSequence.add(event.getLegMode());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!"pt interaction".equals(event.getActType())) {
			final List<String> modeSequence = this.traveler2ModeSequence.get(event.getPersonId());
			final Integer oldCnt = this.modeSequence2Cnt.get(modeSequence);
			this.modeSequence2Cnt.put(modeSequence, oldCnt == null ? 1 : oldCnt + 1);
			this.traveler2ModeSequence.remove(event.getPersonId());
		}
	}

	public String getResult() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<List<String>, Integer> entry : this.modeSequence2Cnt.entrySet()) {
			result.append(entry.getValue() + "\t" + entry.getKey() + "\n");
		}
		return result.toString();
	}

	public static void main(String[] args) {
		LegModeAnalyzer analyzer = new LegModeAnalyzer();
		analyzer.parse("/Users/GunnarF/NoBackup/data-workspace/wum/2019-05-27b/100.events.xml.gz");
		System.out.println(analyzer.getResult());
	}
}
