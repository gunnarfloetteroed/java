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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;
import org.matsim.core.events.EventsUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class StuckInPTAnalyzer
		implements BoardingDeniedEventHandler, PersonStuckEventHandler, PersonDepartureEventHandler {

	final String eventsFileName;

	final Set<Id<Person>> denied = new LinkedHashSet<>();
	final Set<Id<Person>> stuckPersons = new LinkedHashSet<>();
	final Set<Id<Link>> stuckLinks = new LinkedHashSet<>();
	final Map<String, Integer> stuckModes2Cnt = new LinkedHashMap<>();
	final Map<Id<Link>, Integer> stuckBusPassengerLoc2Cnt = new LinkedHashMap<>();
	final Map<Id<Link>, Integer> stuckRailPassengerLoc2Cnt = new LinkedHashMap<>();
	final Set<String> legModes = new LinkedHashSet<>();

	StuckInPTAnalyzer(final String eventsFileName) {
		this.eventsFileName = eventsFileName;
	}

	@Override
	public void handleEvent(BoardingDeniedEvent e) {
		this.denied.add(e.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.stuckPersons.add(event.getPersonId());
		this.stuckLinks.add(event.getLinkId());
		this.stuckModes2Cnt.put(event.getLegMode(), this.stuckModes2Cnt.getOrDefault(event.getLegMode(), 0) + 1);
		if ("railPassenger".equals(event.getLegMode())) {
			this.stuckRailPassengerLoc2Cnt.put(event.getLinkId(),
					this.stuckRailPassengerLoc2Cnt.getOrDefault(event.getLinkId(), 0) + 1);
		} else if ("busPassenger".equals(event.getLegMode())) {
			this.stuckBusPassengerLoc2Cnt.put(event.getLinkId(),
					this.stuckBusPassengerLoc2Cnt.getOrDefault(event.getLinkId(), 0) + 1);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.legModes.add(event.getLegMode());
	}

	Set<Id<Person>> getDeniedAndStuckIDs() {
		Set<Id<Person>> result = new LinkedHashSet<>(this.denied);
		result.retainAll(this.stuckPersons);
		return result;
	}

	void run() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(this);
		EventsUtils.readEvents(eventsManager, this.eventsFileName);
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		String eventsFileName = "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-14b/2000.events.xml.gz";
		String inputPlansFile = "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-14b/2000.plans.xml.gz";
		String outputPlansFile = "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-14b/2000.stuck-plans.xml.gz";

		StuckInPTAnalyzer analyzer = new StuckInPTAnalyzer(eventsFileName);
		analyzer.run();

		System.out.println("denied:              " + analyzer.denied.size());
		System.out.println("stuck:               " + analyzer.stuckPersons.size());
		System.out.println("denied and stuck:    " + analyzer.getDeniedAndStuckIDs().size());
		System.out.println("all leg modes:       " + analyzer.legModes);
		System.out.println("stuck links:         " + analyzer.stuckLinks);
		System.out.println("stuck links:         " + analyzer.stuckLinks.size());
		System.out.println("stuck modes:         " + analyzer.stuckModes2Cnt);
		System.out.println("stuck bus loc2cnt:   " + analyzer.stuckBusPassengerLoc2Cnt);
		System.out.println("stuck train loc2cnt: " + analyzer.stuckRailPassengerLoc2Cnt);

		// Config config = ConfigUtils.createConfig();
		// config.plans().setInputFile(inputPlansFile);
		// Scenario scenario = ScenarioUtils.loadScenario(config);

		// Set<Id<Person>> stuckSample = new LinkedHashSet<>();
		// for (Id<Person> id : new FractionalIterable<>(analyzer.stuck, 0.01)) {
		// stuckSample.add(id);
		// }
		// Set<Id<Person>> nonStuckIds = new
		// LinkedHashSet<>(scenario.getPopulation().getPersons().keySet());
		// nonStuckIds.removeAll(stuckSample);
		// for (Id<Person> removeId : nonStuckIds) {
		// scenario.getPopulation().getPersons().remove(removeId);
		// }
		// PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		// writer.write(outputPlansFile);
		// System.out.println("... DONE");
	}
}
