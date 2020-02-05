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

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DifferentiatedPTModeReconstructor {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/2018-08-09_scenario/";

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		VehicleReaderV1 vehicleReader = new VehicleReaderV1(scenario.getTransitVehicles());
		vehicleReader.readFile(path + "/vehicles.xml");
		System.out.println("  loaded " + scenario.getTransitVehicles().getVehicles().size() + " transit vehicles");

		TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
		scheduleReader.readFile(path + "/transitSchedule.xml");
		System.out.println("  loaded " + scenario.getTransitSchedule().getTransitLines().size() + " transit lines");

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				Set<String> vehicleTypes = new LinkedHashSet<>();
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicle = scenario.getTransitVehicles().getVehicles().get(departure.getVehicleId());
					vehicleTypes.add(vehicle.getType().getId().toString());
				}
				System.out.println("line " + line.getId() + ", route " + route.getId() + ": " + route.getTransportMode()
						+ " -> " + vehicleTypes);
				if (vehicleTypes.size() != 1) {
					throw new RuntimeException("... this does not work.");
				}
				route.setTransportMode(vehicleTypes.iterator().next());
			}
		}

		TransitScheduleWriter writer = new TransitScheduleWriter(scenario.getTransitSchedule());
		writer.writeFile(path + "/transitSchedule_withDifferentiatedPT.xml");

		System.out.println("... DONE");
	}
}
