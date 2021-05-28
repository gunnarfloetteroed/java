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
package stockholm.carsharing;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CarsharingSupplyGenerator {

	private static class Company {

		private final String name;
		private final Map<Id<Link>, Integer> link2oneWayVehicles = new LinkedHashMap<>();
		private final Map<Id<Link>, Integer> link2twoWayVehicles = new LinkedHashMap<>();
		private final Map<Id<Link>, Integer> link2freeParking = new LinkedHashMap<>();

		private Company(final String name) {
			this.name = name;
		}

		private Set<Id<Link>> allOneWayStationLinks() {
			final Set<Id<Link>> result = new LinkedHashSet<>(this.link2oneWayVehicles.keySet());
			result.addAll(this.link2freeParking.keySet());
			return result;
		}

		private void placeOneWayVehicles(final Id<Link> linkId, final int vehicleCnt) {
			this.link2oneWayVehicles.put(linkId, vehicleCnt);
		}

		private void placeTwoWayVehicles(final Id<Link> linkId, final int vehicleCnt) {
			this.link2twoWayVehicles.put(linkId, vehicleCnt);
		}

		private void placeFreeParking(final Id<Link> linkId, final int freeParking) {
			this.link2freeParking.put(linkId, freeParking);
		}
	}

	private final Map<String, Id<Link>> zone2link = new LinkedHashMap<>();
	private final Map<Id<Link>, String> link2zone = new LinkedHashMap<>();

	private final Map<String, Company> companies = new LinkedHashMap<>();

	private final Scenario scenario;

	CarsharingSupplyGenerator(final String networkFileName) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFileName);
		this.scenario = ScenarioUtils.loadScenario(config);
	}

	public void defineZone(final String zoneId, final String linkId) {
		this.zone2link.put(zoneId, Id.createLinkId(linkId));
		this.link2zone.put(Id.createLinkId(linkId), zoneId);
	}

	private Company getOrCreateCompany(final String companyId) {
		Company company = this.companies.get(companyId);
		if (company == null) {
			company = new Company(companyId);
			this.companies.put(companyId, company);
		}
		return company;
	}

	public void addOneWayVehicles(final String companyId, final String zoneId, final int vehicleCnt) {
		final Id<Link> linkId = this.zone2link.get(zoneId);
		this.getOrCreateCompany(companyId).placeOneWayVehicles(linkId, vehicleCnt);
	}

	public void addFreeParking(final String companyId, final String zoneId, final int freeParking) {
		final Id<Link> linkId = this.zone2link.get(zoneId);
		this.getOrCreateCompany(companyId).placeFreeParking(linkId, freeParking);
	}

	public void addTwoWayVehicles(final String companyId, final String zoneId, final int vehicleCnt) {
		final Id<Link> linkId = this.zone2link.get(zoneId);
		this.getOrCreateCompany(companyId).placeTwoWayVehicles(linkId, vehicleCnt);
	}

	private void printStation(final String type, final String companyName, final Id<Link> linkId, final int vehicleCnt,
			final Integer freeparking, final PrintWriter writer) {
		final String stationId = companyName + "_" + type + "_" + this.link2zone.get(linkId);
		final Link link = this.scenario.getNetwork().getLinks().get(linkId);
		final double x = 0.5 * (link.getToNode().getCoord().getX() + link.getFromNode().getCoord().getX());
		final double y = 0.5 * (link.getToNode().getCoord().getY() + link.getFromNode().getCoord().getY());
		writer.print("    <" + type + " id=\"" + stationId + "\" x=\"" + x + "\" y=\"" + y + "\" ");
		if (freeparking != null) {
			writer.print("freeparking=\"" + freeparking + "\" ");
		}
		writer.println(">");
		for (int vehIndex = 0; vehIndex < vehicleCnt; vehIndex++) {
			final String vehId = companyName + "_" + type + "_" + (++this.vehCnt);
			writer.println("      <vehicle type=\"car\" vehicleID=\"" + vehId + "\" />");
		}
		writer.println("    </" + type + ">");
	}

	private int vehCnt;

	public void writeStationsFile(final String stationsFileName) {
		try {
			final PrintWriter writer = new PrintWriter(stationsFileName);
			
			writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			writer.println("<!DOCTYPE companies SYSTEM \"src/main/resources/dtd/CarsharingStations.dtd\">");
			
			writer.println("<companies>");
			for (Company company : this.companies.values()) {
				writer.println("  <company name=\"" + company.name + "\">");
				this.vehCnt = 0;
				for (Map.Entry<Id<Link>, Integer> entry : company.link2twoWayVehicles.entrySet()) {
					this.printStation("twoway", company.name, entry.getKey(), entry.getValue(), null, writer);
				}
				this.vehCnt = 0;
				for (Id<Link> link : company.allOneWayStationLinks()) {
					this.printStation("oneway", company.name, link, company.link2oneWayVehicles.getOrDefault(link, 0),
							company.link2freeParking.getOrDefault(link, 0), writer);
				}
				//writer.println("    <freefloating/>");
				writer.println("  </company>");
			}
			writer.println("</companies>");
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String path = "./scenarios/EkoCS-Trans/input/";

		CarsharingSupplyGenerator gen = new CarsharingSupplyGenerator(path + "network.xml");

		final List<Link> linksList = new ArrayList<>(gen.scenario.getNetwork().getLinks().values());
		Collections.shuffle(linksList);
		
		gen.defineZone("Observatorium", "43526_SE");
		gen.defineZone("TabyPark", "84868_SW");
		gen.defineZone("KungensKurva", "77551_S");
		
		gen.addFreeParking("m", "Observatorium", 1000);
		gen.addOneWayVehicles("m", "Observatorium", 100);
		gen.addTwoWayVehicles("m", "Observatorium", 100);

		gen.addFreeParking("m", "TabyPark", 1000);
		gen.addOneWayVehicles("m", "TabyPark", 100);
		gen.addTwoWayVehicles("m", "TabyPark", 100);

		gen.addFreeParking("m", "KungensKurva", 1000);
		gen.addOneWayVehicles("m", "KungensKurva", 100);
		gen.addTwoWayVehicles("m", "KungensKurva", 100);

		gen.writeStationsFile(path + "stations.xml");

		System.out.println("... DONE");
	}
}
