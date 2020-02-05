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
package stockholm.wum.creation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DetailPTVehicles {

	private final String transitVehicleTypeDefinitionsFileName;
	private final String scheduleFileName;
	private final String oldTransitVehiclesFileName; 
	
	public DetailPTVehicles(final String transitVehicleTypeDefinitionsFileName,
			final String scheduleFileName,
			final String vehiclesFileName) {
		this.transitVehicleTypeDefinitionsFileName = transitVehicleTypeDefinitionsFileName;
		this.scheduleFileName = scheduleFileName;
		this.oldTransitVehiclesFileName = vehiclesFileName;
	}
	
	public void run(final String newTransitVehiclesFileName) {
		final Vehicles newVehicles = VehicleUtils.createVehiclesContainer();
		{
			final VehicleReaderV1 reader = new VehicleReaderV1(newVehicles);
			reader.readFile(transitVehicleTypeDefinitionsFileName);
		}

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		{
			final TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
			scheduleReader.readFile(scheduleFileName);
			final VehicleReaderV1 vehicleReader = new VehicleReaderV1(scenario.getTransitVehicles());
			vehicleReader.readFile(oldTransitVehiclesFileName);
		}

		final VehiclesFactory vehiclesFactory = VehicleUtils.getFactory();
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				final VehicleType newVehicleType = newVehicles.getVehicleTypes()
						.get(Id.create(route.getTransportMode(), VehicleType.class));
				if (newVehicleType == null) {
					System.out.println("no vehicle type for " + route.getTransportMode());
				}
				for (Departure dpt : route.getDepartures().values()) {
					final Vehicle oldVehicle = scenario.getTransitVehicles().getVehicles().get(dpt.getVehicleId());
					final Vehicle newVehicle = vehiclesFactory.createVehicle(oldVehicle.getId(), newVehicleType);
					newVehicles.addVehicle(newVehicle);
				}
			}
		}

		final VehicleWriterV1 vehicleWriter = new VehicleWriterV1(newVehicles);
		vehicleWriter.writeFile(newTransitVehiclesFileName);
	}
	
	public static void main(String[] args) {

		System.out.println("STARTED ...");

		String transitVehicleTypeDefinitionsFileName = "/Users/GunnarF/NoBackup/data-workspace/pt/data/input/transitVehicles_only-types.xml";
		String scheduleFileName = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitSchedule_reduced.xml.gz";
		String oldTransitVehiclesFileName = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitVehicles.xml.gz";
		String newTransitVehiclesFileName = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitVehiclesDifferentiated.xml.gz";

		final Vehicles newVehicles = VehicleUtils.createVehiclesContainer();
		{
			final VehicleReaderV1 reader = new VehicleReaderV1(newVehicles);
			reader.readFile(transitVehicleTypeDefinitionsFileName);
		}

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		{
			final TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
			scheduleReader.readFile(scheduleFileName);
			final VehicleReaderV1 vehicleReader = new VehicleReaderV1(scenario.getTransitVehicles());
			vehicleReader.readFile(oldTransitVehiclesFileName);
		}

		final VehiclesFactory vehiclesFactory = VehicleUtils.getFactory();
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				final VehicleType newVehicleType = newVehicles.getVehicleTypes()
						.get(Id.create(route.getTransportMode(), VehicleType.class));
				if (newVehicleType == null) {
					System.out.println("no vehicle type for " + route.getTransportMode());
				}
				for (Departure dpt : route.getDepartures().values()) {
					final Vehicle oldVehicle = scenario.getTransitVehicles().getVehicles().get(dpt.getVehicleId());
					final Vehicle newVehicle = vehiclesFactory.createVehicle(oldVehicle.getId(), newVehicleType);
					newVehicles.addVehicle(newVehicle);
				}
			}
		}

		final VehicleWriterV1 vehicleWriter = new VehicleWriterV1(newVehicles);
		vehicleWriter.writeFile(newTransitVehiclesFileName);
		
		// vehiclesFactory.createVehicle(id, type)

		// final Set<Id<VehicleType>> oldVehTypeIds = new LinkedHashSet<>(
		// scenario.getTransitVehicles().getVehicleTypes().keySet());
		// for (Id<VehicleType> oldVehTypeId : oldVehTypeIds) {
		// scenario.getTransitVehicles().removeVehicleType(oldVehTypeId);
		// }

		System.out.println("... DONE");
		System.exit(0);

		// final String zoneShapeFileName =
		// "/Users/GunnarF/NoBackup/data-workspace/ihop2/ihop2-data/demand-input/zones_EPSG3857.shp";
		// final ZonalSystem zonalSystem = new ZonalSystem(zoneShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857);
		//
		// String fullScheduleFile =
		// "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitSchedule.xml.gz";
		// String fullTransitVehiclesFile =
		// "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitVehicles.xml.gz";
		//
		// String pointFile =
		// "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/stopPoints.xy";
		//
		// String reducedScheduleFile =
		// "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitSchedule_reduced.xml.gz";
		//
		// Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// new TransitScheduleReader(scenario).readFile(fullScheduleFile);
		// new
		// VehicleReaderV1(scenario.getTransitVehicles()).readFile(fullTransitVehiclesFile);
		//
		// final CoordinateTransformation network2zonal =
		// StockholmTransformationFactory.getCoordinateTransformation(
		// StockholmTransformationFactory.WGS84_SWEREF99,
		// StockholmTransformationFactory.WGS84_EPSG3857);
		//
		// System.out.print("Identifying contained stop facilities ... ");
		// final Set<Id<TransitStopFacility>> relevantStopFacilityIds = new
		// LinkedHashSet<>();
		// for (TransitStopFacility stopFacility :
		// scenario.getTransitSchedule().getFacilities().values()) {
		// final Coord stopCoord = network2zonal.transform(stopFacility.getCoord());
		// final Point stopPoint = MGC.xy2Point(stopCoord.getX(), stopCoord.getY());
		// if (zonalSystem.entireRegion.contains(stopPoint)) {
		// relevantStopFacilityIds.add(stopFacility.getId());
		// }
		// }
		// System.out.println(relevantStopFacilityIds.size() + " out of "
		// + scenario.getTransitSchedule().getFacilities().size() + " found.");
		//
		// // [rail, bus, ferry, Communal Taxi Service, tram, subway]
		// final Set<String> relevantModes = new LinkedHashSet<String>();
		// relevantModes.add("rail");
		// relevantModes.add("bus");
		// relevantModes.add("ferry");
		// relevantModes.add("tram");
		// relevantModes.add("subway");
		//
		// int routeCnt = 0;
		// final Set<TransitLine> reducedLines = new LinkedHashSet<>();
		// final Set<TransitRoute> reducedRoutes = new LinkedHashSet<>();
		// final Set<TransitRouteStop> reducedStops = new LinkedHashSet<>();
		// final Set<TransitRouteStop> allStops = new LinkedHashSet<>();
		// final Set<Id<TransitStopFacility>> reducedStopFacilityIds = new
		// LinkedHashSet<>();
		// for (TransitLine line :
		// scenario.getTransitSchedule().getTransitLines().values()) {
		// boolean foundRelevantRoute = false;
		// for (TransitRoute route : line.getRoutes().values()) {
		//
		// routeCnt++;
		// allStops.addAll(route.getStops());
		//
		// if (relevantModes.contains(route.getTransportMode())) {
		// boolean foundRelevantStop = false;
		// for (Iterator<TransitRouteStop> stopIt = route.getStops().iterator();
		// stopIt.hasNext()
		// && !foundRelevantStop;) {
		// final TransitRouteStop stop = stopIt.next();
		// if (relevantStopFacilityIds.contains(stop.getStopFacility().getId())) {
		// foundRelevantStop = true;
		// }
		// }
		// if (foundRelevantStop) {
		// reducedRoutes.add(route);
		// reducedStops.addAll(route.getStops());
		// for (TransitRouteStop stop : route.getStops()) {
		// reducedStopFacilityIds.add(stop.getStopFacility().getId());
		// }
		// foundRelevantRoute = true;
		// }
		// }
		//
		// }
		// if (foundRelevantRoute) {
		// reducedLines.add(line);
		// }
		// }
		// System.out.println("all relevant modes: " + relevantModes);
		// System.out.println(reducedLines.size() + " out of " +
		// scenario.getTransitSchedule().getTransitLines().size()
		// + " relevant lines");
		// System.out.println(reducedRoutes.size() + " out of " + routeCnt + " relevant
		// routes");
		// System.out.println(reducedStops.size() + " out of " + allStops.size() + "
		// relevant stops");
		//
		// final Set<TransitLine> linesToRemove = new LinkedHashSet<>(
		// scenario.getTransitSchedule().getTransitLines().values());
		// linesToRemove.removeAll(reducedLines);
		// for (TransitLine removeLine : linesToRemove) {
		// scenario.getTransitSchedule().removeTransitLine(removeLine);
		// }
		//
		// for (TransitLine line :
		// scenario.getTransitSchedule().getTransitLines().values()) {
		// final Set<TransitRoute> routesToRemove = new LinkedHashSet<>();
		// for (TransitRoute route : line.getRoutes().values()) {
		// if (!reducedRoutes.contains(route)) {
		// routesToRemove.add(route);
		// }
		// }
		// for (TransitRoute removeRoute : routesToRemove) {
		// line.removeRoute(removeRoute);
		// }
		// }
		//
		// final Set<Id<TransitStopFacility>> facilitiesToRemoveIds = new
		// LinkedHashSet<>(
		// scenario.getTransitSchedule().getFacilities().keySet());
		// facilitiesToRemoveIds.removeAll(reducedStopFacilityIds);
		// for (Id<TransitStopFacility> facilityToRemoveId : facilitiesToRemoveIds) {
		// TransitStopFacility facilityToRemove =
		// scenario.getTransitSchedule().getFacilities()
		// .get(facilityToRemoveId);
		// scenario.getTransitSchedule().removeStopFacility(facilityToRemove);
		// }
		//
		// try {
		// PrintWriter pointWriter = new PrintWriter(new FileWriter(pointFile, false));
		// for (TransitStopFacility fac :
		// scenario.getTransitSchedule().getFacilities().values()) {
		// pointWriter.println(fac.getCoord().getX() + "," + fac.getCoord().getY());
		// }
		// pointWriter.flush();
		// pointWriter.close();
		// } catch (IOException e) {
		// throw new RuntimeException(e);
		// }
		//
		// TransitScheduleWriter writer = new
		// TransitScheduleWriter(scenario.getTransitSchedule());
		// writer.writeFile(reducedScheduleFile);
		//
		// System.out.println("... DONE");

	}
}
