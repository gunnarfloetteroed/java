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
package stockholm.ihop4.tollzonepassagedata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import floetteroed.utilities.Units;
import floetteroed.utilities.math.BasicStatistics;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TollLinkAnalyzer implements LinkEnterEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private final Vehicle2DriverEventHandler vehicle2DriverHandler;

	private final Set<Id<Link>> tollLinkIds;

	private final Set<Id<Person>> tollLinkUserIds;
	private final Set<Id<Person>> allLinkUsers;

	private final Map<Id<Person>, List<String>> person2acts = new LinkedHashMap<>();
	private final Map<Id<Person>, List<Double>> per2start = new LinkedHashMap<>();
	private final Map<Id<Person>, List<Double>> per2end = new LinkedHashMap<>();

	public TollLinkAnalyzer(final Vehicle2DriverEventHandler vehicle2DriverHandler) {
		this.vehicle2DriverHandler = vehicle2DriverHandler;
		this.tollLinkIds = new LinkedHashSet<>();
		for (String linkStr : TollZonePassageDataSpecification.chargingPoint2link.values()) {
			this.tollLinkIds.add(Id.createLinkId(linkStr));
		}
		this.tollLinkUserIds = new LinkedHashSet<>();
		this.allLinkUsers = new LinkedHashSet<>();
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		final Id<Person> driverId = this.vehicle2DriverHandler.getDriverOfVehicle(event.getVehicleId());
		this.allLinkUsers.add(driverId);
		if (this.tollLinkIds.contains(event.getLinkId())) {
			this.tollLinkUserIds.add(driverId);

		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.add(this.person2acts, event.getPersonId(), event.getActType());
		this.add(this.per2start, event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.add(this.per2end, event.getPersonId(), event.getTime());
	}

	private <K, V> void add(Map<K, List<V>> map, K key, V value) {
		List<V> list = map.get(key);
		if (list == null) {
			list = new ArrayList<>();
			map.put(key, list);
		}
		list.add(value);
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/ihop4/2018-12-06/it.2000/";
		final String eventsFileName = path + "2000.events.xml.gz";

		final Vehicle2DriverEventHandler vehicle2driverHandler = new Vehicle2DriverEventHandler(); // null
		final TollLinkAnalyzer analyzer = new TollLinkAnalyzer(vehicle2driverHandler);

		final Config config = ConfigUtils.createConfig();

		final EventsManager events = EventsUtils.createEventsManager(config);
		events.addHandler(vehicle2driverHandler);
		events.addHandler(analyzer);

		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);

		System.out.println(
				"toll link users: " + analyzer.tollLinkUserIds.size() + " out of " + analyzer.allLinkUsers.size());

		// >>> ANALYSIS >>>

		final List<String> work_home = Arrays.asList("work", "home");
		final List<String> other_home = Arrays.asList("other", "home");
		final List<String> work_intermedhome_other_home = Arrays.asList("work", "intermediate_home", "other", "home");

		final BasicStatistics hDur_in_HWH = new BasicStatistics();
		final BasicStatistics wDur_in_HWH = new BasicStatistics();

		final BasicStatistics hDur_in_HOH = new BasicStatistics();
		final BasicStatistics oDur_in_HOH = new BasicStatistics();

		final BasicStatistics hDur_in_HWHOH = new BasicStatistics();
		final BasicStatistics wDur_in_HWHOH = new BasicStatistics();
		final BasicStatistics iHDur_in_HWHOH = new BasicStatistics();
		final BasicStatistics oDur_in_HWHOH = new BasicStatistics();

		for (Id<Person> personId : analyzer.tollLinkUserIds) {
			final List<String> acts = analyzer.person2acts.get(personId);
			if (work_home.equals(acts)) {
				/*-
				 * index:	0		1
				 * start:	work	home
				 * end:		home	work
				 */
				hDur_in_HWH.add(analyzer.per2end.get(personId).get(0)
						+ (Units.S_PER_D - analyzer.per2start.get(personId).get(1)));
				wDur_in_HWH.add(analyzer.per2end.get(personId).get(1) - analyzer.per2start.get(personId).get(0));
			} else if (other_home.equals(acts)) {
				/*-
				 * index:	0		1
				 * start:	other	home
				 * end:		home	other
				 */
				hDur_in_HOH.add(analyzer.per2end.get(personId).get(0)
						+ (Units.S_PER_D - analyzer.per2start.get(personId).get(1)));
				oDur_in_HOH.add(analyzer.per2end.get(personId).get(1) - analyzer.per2start.get(personId).get(0));
			} else if (work_intermedhome_other_home.equals(acts)) {
				/*-
				 * index:	0		1		2		3
				 * start:	work	iHome	other	home
				 * end:		home	work	iHome	other
				 */

				hDur_in_HWHOH.add(analyzer.per2end.get(personId).get(0)
						+ (Units.S_PER_D - analyzer.per2start.get(personId).get(3)));
				wDur_in_HWHOH.add(analyzer.per2end.get(personId).get(1) - analyzer.per2start.get(personId).get(0));
				iHDur_in_HWHOH.add(analyzer.per2end.get(personId).get(2) - analyzer.per2start.get(personId).get(1));
				oDur_in_HWHOH.add(analyzer.per2end.get(personId).get(3) - analyzer.per2start.get(personId).get(2));
			} else {
				System.out.println("UNKNOWN: " + acts);
			}
		}

		System.out.println();

		report("H in HWH", hDur_in_HWH);
		report("W in HWH", wDur_in_HWH);

		report("H in HOH", hDur_in_HOH);
		report("O in HOH", oDur_in_HOH);

		report("H in HW(iH)OH", hDur_in_HWHOH);
		report("W in HW(iH)OH", wDur_in_HWHOH);
		report("iH in HW(iH)OH", iHDur_in_HWHOH);
		report("O in HW(iH)OH", oDur_in_HWHOH);

		// <<< ANALYSIS <<<

		System.out.println("... DONE");
	}

	static void report(String label, BasicStatistics stats) {
		System.out.println(label);
		System.out.println("  mean:   " + stats.getAvg() / 3600);
		System.out.println("  stddev: " + stats.getStddev() / 3600);
		System.out.println("  cnt:    " + stats.size());
		System.out.println();
	}

}
