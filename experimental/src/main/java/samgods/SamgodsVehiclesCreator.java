/*
 * Copyright 2021 Gunnar Flötteröd
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
package samgods;

import static java.lang.Double.parseDouble;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.Units;
import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SamgodsVehiclesCreator {

	// -------------------- INNER CLASS VehicleReader --------------------

	private class VehicleReader implements TabularFileHandler {

		private final VehicleType trainType;

		private double carryover_veh_day;

		private int trainCnt;

		VehicleReader(VehicleType trainType) {
			this.trainType = trainType;
		}

		@Override
		public void startDocument() {
		}

		@Override
		public String preprocess(String line) {
			return line;
		}

		@Override
		public void startRow(String[] row) {
			final Id<Node> fromNodeId = zoneStr2nodeIds.get(row[0]).iterator().next();
			final Id<Node> toNodeId = zoneStr2nodeIds.get(row[1]).iterator().next();
			final double veh_day = parseDouble(row[2]) / effectiveDaysPerYear + this.carryover_veh_day;
			final int intVeh_day = (int) veh_day;
			for (int veh = 0; veh < intVeh_day; veh++) {
				final Tuple<Id<Node>, Id<Node>> odNodeIds = new Tuple<>(fromNodeId, toNodeId);
				List<Vehicle> trainIds = nodeIds2trains.get(odNodeIds);
				if (trainIds == null) {
					trainIds = new ArrayList<>();
					nodeIds2trains.put(odNodeIds, trainIds);
				}
				final Vehicle train = VehicleUtils.createVehicle(
						Id.createVehicleId(this.trainType.getId() + "_" + (this.trainCnt++)), this.trainType);
				vehicles.addVehicle(train);
				trainIds.add(train);
			}
			this.carryover_veh_day = veh_day - intVeh_day;
			System.out.println(this.carryover_veh_day);
		}

		@Override
		public void endDocument() {
		}

		void run(String fileName) {
			System.out.println("Loading " + fileName);
			this.carryover_veh_day = 0.0;
			this.trainCnt = 0;
			final TabularFileParser parser = new TabularFileParser();
			parser.setDelimiterTags(new String[] { " ", ":" });
			parser.setOmitEmptyColumns(false);
			try {
				parser.parse(fileName, this);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// -------------------- VARIABLES --------------------

	private final Map<String, Set<Id<Node>>> zoneStr2nodeIds;

	private final double effectiveDaysPerYear;

	private Map<Tuple<Id<Node>, Id<Node>>, List<Vehicle>> nodeIds2trains;

	private Vehicles vehicles;

	// -------------------- CONSTRUCTION --------------------

	public SamgodsVehiclesCreator(final Map<String, Set<Id<Node>>> zoneStr2nodeIds, final double effectiveDaysPerYear) {
		this.zoneStr2nodeIds = zoneStr2nodeIds;
		this.effectiveDaysPerYear = effectiveDaysPerYear;
	}

	// -------------------- IMPLEMENTATION --------------------

	private void loadTrains(final String trainTypeIdStr, final double length_m, final double maxSpeed_km_h) {

		final VehicleType trainType = VehicleUtils.createVehicleType(Id.create(trainTypeIdStr, VehicleType.class));
		trainType.setLength(length_m);
		trainType.setWidth(4.0);
		trainType.setMaximumVelocity(maxSpeed_km_h * Units.M_S_PER_KM_H);
		trainType.setNetworkMode("car");
		trainType.setPcuEquivalents(length_m / 7.5);
		trainType.setFlowEfficiencyFactor(trainType.getPcuEquivalents());
		this.vehicles.addVehicleType(trainType);

		final VehicleReader reader = new VehicleReader(trainType);
		reader.run("./scenarios/samgods/train-ods/OD_Vhcl" + trainTypeIdStr + "_STD.314");
	}

	public void writeVehicles(final String vehiclesFileName) {
		this.vehicles = VehicleUtils.createVehiclesContainer();
		this.nodeIds2trains = new LinkedHashMap<>();

		this.loadTrains("201", 340.0, 150);
		this.loadTrains("202", 271.0, 150);
		this.loadTrains("204", 327.0, 327.0);
		this.loadTrains("205", 340.0, 327.0);
		this.loadTrains("206", 340.0, 750.0);
		this.loadTrains("207", 340.0, 380.0);
		this.loadTrains("208", 340.0, 452.0);

		final MatsimVehicleWriter vehicleWriter = new MatsimVehicleWriter(this.vehicles);
		vehicleWriter.writeFile(vehiclesFileName);

		System.out.println("Created " + this.vehicles.getVehicles().size() + " vehices.");
	}
	
	public Map<Tuple<Id<Node>, Id<Node>>, List<Vehicle>> getNodeIds2trains() {
		return this.nodeIds2trains;
	}
	
	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		final SamgodsODCreator odReader = new SamgodsODCreator("./scenarios/samgods/Nyckel_Emme_Voyager.csv",
				"./scenarios/samgods/train-ods/");

		SamgodsVehiclesCreator creator = new SamgodsVehiclesCreator(odReader.getZone2nodeIds(), 200.0);
		creator.writeVehicles("./scenarios/samgods/vehicles-test.xml");
	}
	
	
}
