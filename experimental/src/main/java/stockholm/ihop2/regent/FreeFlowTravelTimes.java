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
package stockholm.ihop2.regent;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FreeFlowTravelTimes {

	public FreeFlowTravelTimes() {
	}

	public static void main(String[] args) {

		// System.out.println("STARTED ...");
		//
		// final String networkFileName =
		// "./test/matsim-testrun/input/network-plain.xml";
		// final Config config = ConfigUtils.createConfig();
		// config.setParam("network", "inputNetworkFile", networkFileName);
		// final Scenario scenario = ScenarioUtils.loadScenario(config);
		//
		// final String zonesShapeFileName =
		// "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		// final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857);
		// zonalSystem.addNetwork(scenario.getNetwork(),
		// StockholmTransformationFactory.WGS84_SWEREF99);
		//
		// final int startTime_s = 0;
		// final int binSize_s = 3600;
		// final int binCnt = 1;
		// final int sampleCnt = 1;
		//
		// final TravelTime linkTTs = new TravelTime() {
		// @Override
		// public double getLinkTravelTime(Link link, double time,
		// Person person, Vehicle vehicle) {
		// return link.getLength() / link.getFreespeed();
		// }
		// };
		//
		// final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
		// scenario.getNetwork(), linkTTs, new
		// OnlyTimeDependentTravelDisutility(linkTTs),
		// // null,
		// zonalSystem, new Random(), startTime_s, binSize_s, binCnt,
		// sampleCnt);
		// travelTimeMatrices
		// .writeToFile("./test/matsim-testrun/freeflow-traveltimes.xml");
		//
		// System.out.println("... DONE");
	}

}
