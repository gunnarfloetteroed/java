package stockholm.wum.creation;
///*
// * Copyright 2018 Gunnar Flötteröd
// * 
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// *
// * contact: gunnar.flotterod@gmail.com
// *
// */
//package gunnar.wum.creation;
//
//import java.time.LocalDate;
//
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.contrib.gtfs.RunGTFS2MATSim;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.geometry.CoordinateTransformation;
//import org.matsim.core.utils.geometry.transformations.TransformationFactory;
//import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
//import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
//import org.matsim.pt.utils.CreateVehiclesForSchedule;
//import org.matsim.vehicles.VehicleWriterV1;
//
//import saleem.stockholmmodel.utils.StockholmTransformationFactory;
//
///**
// *
// * @author Gunnar Flötteröd
// *
// */
public class CreateFullTransitSystem {
//
//	public static void main(String[] args) {
//
//		// input data
//
//		// String gtfsZipFile =
//		// "/Users/GunnarF/NoBackup/data-workspace/pt/data/input/sweden-20180424.zip";
//		String gtfsZipFile = "/Users/GunnarF/OneDrive - VTI/My Data/sweden/sweden-20190214.zip";
//
//		CoordinateTransformation ct = StockholmTransformationFactory.getCoordinateTransformation(
//				TransformationFactory.WGS84, StockholmTransformationFactory.WGS84_SWEREF99);
//		// CoordinateTransformation ct = new IdentityTransformation();
//		LocalDate date = LocalDate.parse("2019-02-14");
//
//		// output files
//
//		String scheduleFile = "/Users/GunnarF/OneDrive - VTI/My Data/sweden/transitSchedule-sweden-20190214.xml.gz";
//		// String networkFile = "network.xml.gz";
//		String transitVehiclesFile = "/Users/GunnarF/OneDrive - VTI/My Data/sweden/transitVehicles-20190214.xml.gz";
//
//		// Convert GTFS
//
//		 System.out.println("Commented out line to make this compile.");
//		 System.exit(0);
//		RunGTFS2MATSim.convertGtfs(gtfsZipFile, scheduleFile, date, ct, false);
//
//		// Parse the schedule again
//
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new TransitScheduleReader(scenario).readFile(scheduleFile);
//
//		// if necessary, parse in an existing network file here:
//
//		// new MatsimNetworkReader(scenario.getNetwork()).readFile("network.xml");
//
//		// Create a network around the schedule
//
//		// new
//		// CreatePseudoNetwork(scenario.getTransitSchedule(),scenario.getNetwork(),"pt_").createNetwork();
//
//		// Create simple transit vehicles
//
//		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();
//
//		// Write out network, vehicles and schedule
//
//		// new NetworkWriter(scenario.getNetwork()).write(networkFile);
//		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleFile);
//		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(transitVehiclesFile);
//
//	}
}
