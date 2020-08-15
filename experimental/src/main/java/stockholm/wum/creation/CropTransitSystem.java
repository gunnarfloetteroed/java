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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.MatsimVehicleReader;

import stockholm.ihop2.regent.demandreading.ZonalSystem;
import stockholm.saleem.StockholmTransformationFactory;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CropTransitSystem {

	private final ZonalSystem zonalSystem;

	private final String fullScheduleFile;

	private final String fullTransitVehiclesFile;

	private final CoordinateTransformation network2zonal;

	public CropTransitSystem(final ZonalSystem zonalSystem, final String fullScheduleFile,
			final String fullTransitVehiclesFile, final CoordinateTransformation network2zonalTrafo) {
		this.zonalSystem = zonalSystem;
		this.fullScheduleFile = fullScheduleFile;
		this.fullTransitVehiclesFile = fullTransitVehiclesFile;
		this.network2zonal = network2zonalTrafo;
	}

	public void run(final String reducedScheduleFile, final String reducedTransitVehicleFile) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(this.fullScheduleFile);
				
		// 2020-08-14: changed while moving to MATSim 12
		// OLDnew VehicleReaderV1(scenario.getTransitVehicles()).readFile(this.fullTransitVehiclesFile);
		new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(this.fullTransitVehiclesFile);

		System.out.print("Identifying contained stop facilities ... ");
		final Set<Id<TransitStopFacility>> relevantStopFacilityIds = new LinkedHashSet<>();
		for (TransitStopFacility stopFacility : scenario.getTransitSchedule().getFacilities().values()) {
			final Coord stopCoord = this.network2zonal.transform(stopFacility.getCoord());
			final Point stopPoint = MGC.xy2Point(stopCoord.getX(), stopCoord.getY());
			if (this.zonalSystem.entireRegion.contains(stopPoint)) {
				relevantStopFacilityIds.add(stopFacility.getId());
			}
		}
		System.out.println(relevantStopFacilityIds.size() + " out of "
				+ scenario.getTransitSchedule().getFacilities().size() + " found.");

		// [rail, bus, ferry, Communal Taxi Service, tram, subway]
		final Set<String> relevantModes = new LinkedHashSet<String>();
		relevantModes.add("rail");
		relevantModes.add("bus");
		relevantModes.add("ferry");
		relevantModes.add("tram");
		relevantModes.add("subway");

		int routeCnt = 0;
		final Set<TransitLine> reducedLines = new LinkedHashSet<>();
		final Set<TransitRoute> reducedRoutes = new LinkedHashSet<>();
		final Set<TransitRouteStop> reducedStops = new LinkedHashSet<>();
		final Set<TransitRouteStop> allStops = new LinkedHashSet<>();
		final Set<Id<TransitStopFacility>> reducedStopFacilityIds = new LinkedHashSet<>();
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			boolean foundRelevantRoute = false;
			for (TransitRoute route : line.getRoutes().values()) {

				routeCnt++;
				allStops.addAll(route.getStops());

				if (relevantModes.contains(route.getTransportMode())) {
					boolean foundRelevantStop = false;
					for (Iterator<TransitRouteStop> stopIt = route.getStops().iterator(); stopIt.hasNext()
							&& !foundRelevantStop;) {
						final TransitRouteStop stop = stopIt.next();
						if (relevantStopFacilityIds.contains(stop.getStopFacility().getId())) {
							foundRelevantStop = true;
						}
					}
					if (foundRelevantStop) {
						reducedRoutes.add(route);
						reducedStops.addAll(route.getStops());
						for (TransitRouteStop stop : route.getStops()) {
							reducedStopFacilityIds.add(stop.getStopFacility().getId());
						}
						foundRelevantRoute = true;
					}
				}

			}
			if (foundRelevantRoute) {
				reducedLines.add(line);
			}
		}
		System.out.println("all relevant modes: " + relevantModes);
		System.out.println(reducedLines.size() + " out of " + scenario.getTransitSchedule().getTransitLines().size()
				+ " relevant lines");
		System.out.println(reducedRoutes.size() + " out of " + routeCnt + " relevant routes");
		System.out.println(reducedStops.size() + " out of " + allStops.size() + " relevant stops");

		final Set<TransitLine> linesToRemove = new LinkedHashSet<>(
				scenario.getTransitSchedule().getTransitLines().values());
		linesToRemove.removeAll(reducedLines);
		for (TransitLine removeLine : linesToRemove) {
			scenario.getTransitSchedule().removeTransitLine(removeLine);
		}

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			final Set<TransitRoute> routesToRemove = new LinkedHashSet<>();
			for (TransitRoute route : line.getRoutes().values()) {
				if (!reducedRoutes.contains(route)) {
					routesToRemove.add(route);
				}
			}
			for (TransitRoute removeRoute : routesToRemove) {
				line.removeRoute(removeRoute);
			}
		}

		final Set<Id<TransitStopFacility>> facilitiesToRemoveIds = new LinkedHashSet<>(
				scenario.getTransitSchedule().getFacilities().keySet());
		facilitiesToRemoveIds.removeAll(reducedStopFacilityIds);
		for (Id<TransitStopFacility> facilityToRemoveId : facilitiesToRemoveIds) {
			TransitStopFacility facilityToRemove = scenario.getTransitSchedule().getFacilities()
					.get(facilityToRemoveId);
			scenario.getTransitSchedule().removeStopFacility(facilityToRemove);
		}

		TransitScheduleWriter writer = new TransitScheduleWriter(scenario.getTransitSchedule());
		writer.writeFile(reducedScheduleFile);
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		// TODO replace the below by using an instance of this class
		
		final String zoneShapeFileName = "/Users/GunnarF/NoBackup/data-workspace/ihop2/ihop2-data/demand-input/zones_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zoneShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);

		String fullScheduleFile = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitSchedule.xml.gz";
		String fullTransitVehiclesFile = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitVehicles.xml.gz";

		String pointFile = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/stopPoints.xy";

		String reducedScheduleFile = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitSchedule_reduced.xml.gz";
		String reducedTransitVehiclesFile = "/Users/GunnarF/NoBackup/data-workspace/pt/data/output/transitVehicles_reduced.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(fullScheduleFile);

		// 2020-08-14: changed while moving to MATSim 12
		// OLD: new VehicleReaderV1(scenario.getTransitVehicles()).readFile(fullTransitVehiclesFile);
		new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(fullTransitVehiclesFile);

		final CoordinateTransformation network2zonal = StockholmTransformationFactory.getCoordinateTransformation(
				StockholmTransformationFactory.WGS84_SWEREF99, StockholmTransformationFactory.WGS84_EPSG3857);

		System.out.print("Identifying contained stop facilities ... ");
		final Set<Id<TransitStopFacility>> relevantStopFacilityIds = new LinkedHashSet<>();
		for (TransitStopFacility stopFacility : scenario.getTransitSchedule().getFacilities().values()) {
			final Coord stopCoord = network2zonal.transform(stopFacility.getCoord());
			final Point stopPoint = MGC.xy2Point(stopCoord.getX(), stopCoord.getY());
			if (zonalSystem.entireRegion.contains(stopPoint)) {
				relevantStopFacilityIds.add(stopFacility.getId());
			}
		}
		System.out.println(relevantStopFacilityIds.size() + " out of "
				+ scenario.getTransitSchedule().getFacilities().size() + " found.");

		// [rail, bus, ferry, Communal Taxi Service, tram, subway]
		final Set<String> relevantModes = new LinkedHashSet<String>();
		relevantModes.add("rail");
		relevantModes.add("bus");
		relevantModes.add("ferry");
		relevantModes.add("tram");
		relevantModes.add("subway");

		int routeCnt = 0;
		final Set<TransitLine> reducedLines = new LinkedHashSet<>();
		final Set<TransitRoute> reducedRoutes = new LinkedHashSet<>();
		final Set<TransitRouteStop> reducedStops = new LinkedHashSet<>();
		final Set<TransitRouteStop> allStops = new LinkedHashSet<>();
		final Set<Id<TransitStopFacility>> reducedStopFacilityIds = new LinkedHashSet<>();
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			boolean foundRelevantRoute = false;
			for (TransitRoute route : line.getRoutes().values()) {

				routeCnt++;
				allStops.addAll(route.getStops());

				if (relevantModes.contains(route.getTransportMode())) {
					boolean foundRelevantStop = false;
					for (Iterator<TransitRouteStop> stopIt = route.getStops().iterator(); stopIt.hasNext()
							&& !foundRelevantStop;) {
						final TransitRouteStop stop = stopIt.next();
						if (relevantStopFacilityIds.contains(stop.getStopFacility().getId())) {
							foundRelevantStop = true;
						}
					}
					if (foundRelevantStop) {
						reducedRoutes.add(route);
						reducedStops.addAll(route.getStops());
						for (TransitRouteStop stop : route.getStops()) {
							reducedStopFacilityIds.add(stop.getStopFacility().getId());
						}
						foundRelevantRoute = true;
					}
				}

			}
			if (foundRelevantRoute) {
				reducedLines.add(line);
			}
		}
		System.out.println("all relevant modes: " + relevantModes);
		System.out.println(reducedLines.size() + " out of " + scenario.getTransitSchedule().getTransitLines().size()
				+ " relevant lines");
		System.out.println(reducedRoutes.size() + " out of " + routeCnt + " relevant routes");
		System.out.println(reducedStops.size() + " out of " + allStops.size() + " relevant stops");

		final Set<TransitLine> linesToRemove = new LinkedHashSet<>(
				scenario.getTransitSchedule().getTransitLines().values());
		linesToRemove.removeAll(reducedLines);
		for (TransitLine removeLine : linesToRemove) {
			scenario.getTransitSchedule().removeTransitLine(removeLine);
		}

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			final Set<TransitRoute> routesToRemove = new LinkedHashSet<>();
			for (TransitRoute route : line.getRoutes().values()) {
				if (!reducedRoutes.contains(route)) {
					routesToRemove.add(route);
				}
			}
			for (TransitRoute removeRoute : routesToRemove) {
				line.removeRoute(removeRoute);
			}
		}

		final Set<Id<TransitStopFacility>> facilitiesToRemoveIds = new LinkedHashSet<>(
				scenario.getTransitSchedule().getFacilities().keySet());
		facilitiesToRemoveIds.removeAll(reducedStopFacilityIds);
		for (Id<TransitStopFacility> facilityToRemoveId : facilitiesToRemoveIds) {
			TransitStopFacility facilityToRemove = scenario.getTransitSchedule().getFacilities()
					.get(facilityToRemoveId);
			scenario.getTransitSchedule().removeStopFacility(facilityToRemove);
		}

		try {
			PrintWriter pointWriter = new PrintWriter(new FileWriter(pointFile, false));
			for (TransitStopFacility fac : scenario.getTransitSchedule().getFacilities().values()) {
				pointWriter.println(fac.getCoord().getX() + "," + fac.getCoord().getY());
			}
			pointWriter.flush();
			pointWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		TransitScheduleWriter writer = new TransitScheduleWriter(scenario.getTransitSchedule());
		writer.writeFile(reducedScheduleFile);

		System.out.println("... DONE");

	}

}
