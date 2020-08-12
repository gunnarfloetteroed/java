/*
 * Copyright 2020 Gunnar Flötteröd
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import floetteroed.utilities.Time;
import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class HumanReadableLineSchedule {

	private final TransitSchedule schedule;

	public HumanReadableLineSchedule(final String transitScheduleFile) {
		final Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile(transitScheduleFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		this.schedule = scenario.getTransitSchedule();
	}

	public List<String> getTimeTables(final String lineName) {
		final TransitLine line = this.schedule.getTransitLines().get(Id.create(lineName, TransitSchedule.class));
		final List<String> result = new ArrayList<>(line.getRoutes().size());
		for (TransitRoute route : line.getRoutes().values()) {
			final List<String> stops = new ArrayList<>(route.getStops().size());
			for (TransitRouteStop stop : route.getStops()) {
				stops.add(stop.getStopFacility().getName());
			}
			final List<List<String>> allDepartureTimes = new ArrayList<List<String>>(route.getDepartures().size());
			for (Departure departure : route.getDepartures().values()) {
				final List<String> routeDepartureTimes = new ArrayList<>();
				for (TransitRouteStop stop : route.getStops()) {
					routeDepartureTimes.add(Time
							.strFromSec(MathHelpers.round(departure.getDepartureTime() + stop.getDepartureOffset())));
				}
				allDepartureTimes.add(routeDepartureTimes);
			}
			final StringBuffer timeTable = new StringBuffer(route.getId().toString() + "\n");
			for (int i = 0; i < stops.size(); i++) {
				timeTable.append(stops.get(i));
				for (int j = 0; j < allDepartureTimes.size(); j++) {
					timeTable.append("\t" + allDepartureTimes.get(j).get(i));
				}
				timeTable.append("\n");
			}
			result.add(timeTable.toString());
		}
		return result;
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		HumanReadableLineSchedule schedule = new HumanReadableLineSchedule(
				"/Users/GunnarF/OneDrive - VTI/My Data/wum/WUM-FINAL/2019-11-07_policycase/output/output_transitSchedule.xml.gz");

		for (String timeTable : schedule.getTimeTables("malin_2b")) {
			System.out.println(timeTable);
		}

		System.out.println("... DONE");

	}

}
