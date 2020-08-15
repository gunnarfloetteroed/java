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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.MatsimVehicleReader;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IdentifyLineByStops {

	static Set<String> matches(Collection<String> allCandidates, Collection<String> allRequired) {
		Set<String> result = new LinkedHashSet<>();
		for (String required : allRequired) {
			for (String candidate : allCandidates) {
				if (candidate.toUpperCase().contains(required.toUpperCase())) {
					result.add(candidate);
				}
			}
		}
		return result;
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		String mode = "ferry";

		// line 80
		// final Set<String> requiredStopNames = new LinkedHashSet<>(
		// Arrays.asList(new String[] { "Nybrokajen", "Nacka strand", "Lidingö",
		// "Frihamnen" }));
		
		// line 89
		final Set<String> requiredStopNames = new LinkedHashSet<>(Arrays.asList(new String[] { "Klara Mälarstrand",
				"Lilla Essingen", "Ekensberg", "Kungshättan brygga", "Tappström" }));

		String reducedScheduleFile = "/Users/GunnarF/OneDrive - VTI/My Data/wum/data/output/transitSchedule_reduced.xml.gz";
		String reducedTransitVehiclesFile = "/Users/GunnarF/OneDrive - VTI/My Data/wum/data/output/transitVehiclesDifferentiated.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(reducedScheduleFile);
		// 2020-08-14: changed while moving to MATSim 12
		// OLD: new VehicleReaderV1(scenario.getTransitVehicles()).readFile(reducedTransitVehiclesFile);
		new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(reducedTransitVehiclesFile);

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {

			List<String> foundStopNames = new LinkedList<>();
			for (TransitRoute route : line.getRoutes().values()) {
				if (mode.equalsIgnoreCase(route.getTransportMode())) {
					for (TransitRouteStop stop : route.getStops()) {
						foundStopNames.add(stop.getStopFacility().getName());
					}
				}
			}

			Set<String> matches = matches(new LinkedHashSet<>(foundStopNames), requiredStopNames);
			if (matches.size() > 0) {
				System.out.println("Line " + line.getId() + " matches " + matches.size() + " / "
						+ requiredStopNames.size() + " : " + matches + "; full stop sequence: " + foundStopNames);
			}

		}

		System.out.println("... DONE.");
	}

}
