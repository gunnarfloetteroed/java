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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RunAnalysis {

	public static void analysis1() {
		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/results/2018-08-09_scenario/output";
		final String configFileName = path + "/output_config.xml";
		final String eventsFileName = path + "/output_events.xml.gz";

		Config config = ConfigUtils.loadConfig(configFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		EventsManager eventsManager = new EventsManagerImpl();
		// Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler();
		// eventsManager.addHandler(vehicle2Driver);
		BoardingProblemAnalyzer boarding = new BoardingProblemAnalyzer();
		eventsManager.addHandler(boarding);

		new MatsimEventsReader(eventsManager).readFile(eventsFileName);

		System.out.println("Boarding denials:        " + boarding.personId2deniedBoardingCnt.size());
		System.out.println("Earliest stuck time [h]: " + boarding.earliestStuckTime_s / 3600.0);
		System.out.println("Latest stuck time [h]:   " + boarding.latestStuckTime_s / 3600.0);
		System.out.println("Stuck modes:             " + boarding.stuckMode2cnt);
		// System.out.println("Last activities of stuck persons: ");
		// boarding.printLastActivitiesOfStuckPersons();

		Vehicles vehicles = scenario.getTransitVehicles();
		System.out.println("Number of transit vehicles: " + vehicles.getVehicles().size());

		System.out.println("Capacities per type:");
		for (VehicleType type : vehicles.getVehicleTypes().values()) {
			System.out.println("  type: " + type.getId() + ", seats: " + type.getCapacity().getSeats() + ", standing: "
					+ type.getCapacity().getStandingRoom());
		}

		int[] lastDptPerRouteCnt = new int[49];
		int[] lastDptPerLineCnt = new int[49];

		TransitSchedule schedule = scenario.getTransitSchedule();
		for (TransitLine line : schedule.getTransitLines().values()) {
			double lastLineDpt_s = Double.NEGATIVE_INFINITY;
			for (TransitRoute route : line.getRoutes().values()) {
				double lastRouteDpt_s = Double.NEGATIVE_INFINITY;
				for (Departure departure : route.getDepartures().values()) {
					lastRouteDpt_s = Math.max(lastRouteDpt_s, departure.getDepartureTime());
				}
				lastDptPerRouteCnt[MathHelpers.round(lastRouteDpt_s / 3600)]++;
				lastLineDpt_s = Math.max(lastLineDpt_s, lastRouteDpt_s);
			}
			lastDptPerLineCnt[MathHelpers.round(lastLineDpt_s / 3600)]++;
		}
		for (int h = 0; h <= 48; h++) {
			System.out.println(h + "\t" + lastDptPerRouteCnt[h] + "\t" + lastDptPerLineCnt[h]);
		}

		double allScores = 0;
		double stuckScores = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			allScores += person.getSelectedPlan().getScore();
			if (boarding.stuckPersons.contains(person.getId())) {
				stuckScores += person.getSelectedPlan().getScore();
			}
		}
		System.out.println("average population score: " + allScores / scenario.getPopulation().getPersons().size());
		System.out.println("average stuck score:      " + stuckScores / boarding.stuckPersons.size());

		System.out.println("... DONE");
	}

	public static void analysis2() {
		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/2018-08-09_scenario/";
		final String configFileName = path + "config.xml";
		final String eventsFileName = path + "output/output_events.xml.gz";

		Config config = ConfigUtils.loadConfig(configFileName);
		config.network().addParam("inputNetworkFile", "output/output_network.xml.gz");
		config.plans().addParam("inputPlansFile", "output/output_plans.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Map<Id<TransitRoute>, TransitRoute> id2transitRoute = new LinkedHashMap<>();
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				// System.out.println(route.getId() + ", " + route.getTransportMode());
				id2transitRoute.put(route.getId(), route);
			}
		}
		// System.exit(0);
		
		scenario.getTransitSchedule().getTransitLines().values().iterator().next().getRoutes().values().iterator().next().getTransportMode();
		
//		EventsManager eventsManager = new EventsManagerImpl();
//		DifferentiatedPTLinkExtractor ptVehs = new DifferentiatedPTLinkExtractor(scenario);
//		eventsManager.addHandler(ptVehs);
//
//		new MatsimEventsReader(eventsManager).readFile(eventsFileName);
//
//		System.out.println("from and to node id are equal: " + ptVehs.equalNodes);
//		System.out.println("... diferent: " + ptVehs.differentNodes);
//
//		for (Map.Entry<?, ?> entry : ptVehs.detailedPTMode2linkIds.entrySet()) {
//			System.out.println(entry);
//		}

		// Identify persons traveling across links of interest.

		final Map<String, Set<Person>> mode2users = new LinkedHashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					final Leg leg = (Leg) planElement;
					if (leg.getRoute() instanceof TransitPassengerRoute) {
						final TransitRoute route = id2transitRoute.get(((TransitPassengerRoute) leg.getRoute()).getRouteId());
						Set<Person> users = mode2users.get(route.getTransportMode());
						if (users == null) {
							users = new LinkedHashSet<>();
							mode2users.put(route.getTransportMode(), users);
						}
						users.add(person);
					}
				}
			}
		}

		for (Map.Entry<String, Set<Person>> entry: mode2users.entrySet()) {
			System.out.println(entry.getKey() + " -> " + entry.getValue().size());
		}
		
	}

	public static void main(String[] args) {
		analysis2();
	}
}
